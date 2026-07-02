package simulador;

import java.util.ArrayList;
import java.util.List;

public class SOMP {
    private Escalonador   escalonador;
    private Processador[] processadores;
    private int           tempoAtual;

    private java.util.Map<Integer, Mutex> tabelaMutex = new java.util.HashMap<>();
    private ArrayList<Tarefa>             listaTarefasGeral;


    public SOMP(Escalonador escalonador, int numProcessadores) {
        this.escalonador   = escalonador;
        this.processadores = new Processador[numProcessadores];

        for (int i = 0; i < numProcessadores; i++)
            this.processadores[i] = new Processador(i);

        this.tempoAtual = 0;
        this.listaTarefasGeral = new ArrayList<>();
    }


    private class TratamentoIRQ {
        Tarefa tarefa;
        IO io;
        TratamentoIRQ(Tarefa t, IO i) { this.tarefa = t; this.io = i; }
    }
    
    private List<TratamentoIRQ> filaIRQ = new ArrayList<>();


    public void registrarIRQ(Tarefa t, IO io) {
        filaIRQ.add(new TratamentoIRQ(t, io)); 
    }


    public void adicionarTarefa(Tarefa t) {
        this.listaTarefasGeral.add(t);
    }


    public int getTotalNumTarefas() {
        return listaTarefasGeral.size();
    }


    public Tarefa getTarefaByIdx(int idx) {
        if (listaTarefasGeral != null) return listaTarefasGeral.get(idx); 
        return null;
    }


    private Mutex getMutex(int mutexId) {
        tabelaMutex.putIfAbsent(mutexId, new Mutex(mutexId));
        return tabelaMutex.get(mutexId);
    }


    public boolean isTravado() {
        for (Tarefa t : listaTarefasGeral) {
            final boolean temp = !t.isFinalizada() && (
                !t.isSuspensa() || t.getTempoChegada() > tempoAtual 
            );
            if (temp) return false;
        }
        return true;
    }


    public void executar() {  
        for (TratamentoIRQ irq : filaIRQ) {   // Trata interrupções de IO                
            irq.io.setIrqTratada(true);             
            System.out.println("Tick " + tempoAtual + ": SO processou IRQ da Tarefa T" + irq.tarefa.getId());
        }
        filaIRQ.clear();                                   

        for (Tarefa t : listaTarefasGeral) {
            if (t.isFinalizada()) {
                int tempoJaExecutado       = t.getTempoExecucao() - t.getTempoRestante();  // Como terminou, tempoJaExecutado é igual à duração total da tarefa
                List<Evento> eventosFinais = t.getEventosNoTempoRelativo(tempoJaExecutado);

                for (Evento ev : eventosFinais) {
                    if (ev instanceof EventoMutex) {
                        EventoMutex evMutex = (EventoMutex) ev;
                        
                        if (!evMutex.isProcessado() && !evMutex.isLock()) {  // Se for um evento de Liberação (MU) e ainda não foi processado
                            Mutex m = getMutex(evMutex.getMutexId());
                            
                            if (m.getDonoAtual() != null && m.getDonoAtual().getId() == t.getId()) {  // Se a tarefa morta for a dona do Mutex, libera a chave
                                m.setDonoAtual(null);
                                evMutex.setProcessado(true);
                                System.out.println("Tick " + tempoAtual + ": Tarefa T" + t.getId() + " liberou Mutex " + m.getId() + " ao finalizar.");

                                if (!m.getFilaDeEspera().isEmpty()) {  // Acorda imediatamente a próxima tarefa da fila do Mutex
                                    Tarefa liberada = m.getFilaDeEspera().poll();
                                    liberada.setEsperandoMutex(false);
                                    m.setDonoAtual(liberada);
                                }
                            }
                        }
                    }
                }
            }
        }

        escalonador.limparFila();             // Reconstruir a fila
        for (Tarefa t : listaTarefasGeral) {  // Só entra na CPU se não está finalizada, suspensa, esperando Mutex OU bloqueada por IO
            if (t.getTempoChegada() <= tempoAtual && !t.isFinalizada() && !t.isSuspensa() && !t.isEsperandoMutex() && !t.isBloqueada())
                escalonador.adicionarTarefa(t);
        }
        
        for (Processador cpu : processadores) {// Preempção por tempo
            Tarefa t = cpu.getTarefaAtual();
            
            if (t != null && t.isBloqueada()) {// A tarefa só sofre preempção do Quantum se não acabou de ir para IO ou fila de Mutex
                cpu.setTarefaAtual(null); 
                cpu.resetTicksNoQuantum();

            } else if (t != null && cpu.getTicksNoQuantum() >= t.getQuantum()) {
                cpu.setTarefaAtual(null); 
                cpu.resetTicksNoQuantum();
            }
        }

        escalonador.prepararFila(processadores);  

        List<Tarefa> topTarefas = new ArrayList<>();  // Processamento Dinamico, Lógica Mutex Unificada
        
        while (topTarefas.size() < processadores.length) {
            Tarefa proxima = escalonador.obterProximaTarefa();
            if (proxima == null)
                break; 

            boolean foiBloqueada       = false;
            int     tempoJaExecutado   = proxima.getTempoExecucao() - proxima.getTempoRestante();
            List<Evento> eventosDoTick = proxima.getEventosNoTempoRelativo(tempoJaExecutado);
            List<Evento> eventosProcessados = new ArrayList<>(); // Lista para armazenar eventos processados
            for (Evento ev : eventosDoTick) {
                if (ev instanceof EventoMutex) {
                    EventoMutex evMutex = (EventoMutex) ev;
                    
                    if (evMutex.isProcessado()) 
                        continue; // Evita loop infinito no step-back
                    
                    Mutex m = getMutex(evMutex.getMutexId()); 
                    
                    if (evMutex.isLock()) { 
                        if (
                            m.isLivre() || (m.getDonoAtual() != null && 
                            m.getDonoAtual().getId() == proxima.getId())
                        ) {
                            m.setDonoAtual(proxima); 
                            evMutex.setProcessado(true);

                        } else {
                            if (!m.getFilaDeEspera().contains(proxima))
                                m.getFilaDeEspera().add(proxima);

                            proxima.setEsperandoMutex(true);
                            foiBloqueada = true;
                            break; 
                        }
                    } else { //MU UNLOCK
                        if (
                            m.getDonoAtual() != null && m.getDonoAtual().getId() == proxima.getId()
                        ) {
                            m.setDonoAtual(null); 
                            evMutex.setProcessado(true);
                            
                            if (!m.getFilaDeEspera().isEmpty()) {
                                Tarefa liberada = m.getFilaDeEspera().poll();
                                liberada.setEsperandoMutex(false);
                                m.setDonoAtual(liberada); 
                                
                                escalonador.adicionarTarefa(liberada);
                                escalonador.prepararFila(processadores); 
                            }
                        }
                    }
                }
            }
            
            if (!foiBloqueada) {
                topTarefas.add(proxima);
                if(escalonador instanceof EscalonadorPRIOPENV)
                    proxima.resetarEnvelhecimento();
            }
        }
 
        for (Processador cpu : processadores) {  // Distribuição
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual != null && topTarefas.contains(tarefaAtual)) {
                topTarefas.remove(tarefaAtual); 

            } else {
                cpu.setTarefaAtual(null); 
            }
        }
        
        for (Processador cpu : processadores) {  // Preenche os vazios
            if (cpu.idle() && !topTarefas.isEmpty())
                cpu.setTarefaAtual(topTarefas.remove(0));  
        }

        gravarHistorico();  // Grava o estado e executa o tick
        for (Processador cpu : processadores) {
            cpu.registrarOciosidade();
            cpu.executar();
        }
        
        for (Tarefa t : listaTarefasGeral) {  // Tick exclusivo do hardware de IO
            if (t.getTempoChegada() <= tempoAtual && t.isBloqueada())
                t.executarIO(this);
        }
        ++tempoAtual;
    }

    private void gravarHistorico() {
        for (Tarefa tarefa : listaTarefasGeral) {
            if (tarefa.isFinalizada()) {                         
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1, 0);
                continue;

            } else if (tarefa.isEsperandoMutex()) {              
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.EsperandoMutex, -1, 0);
                continue;
            
            } else if (tarefa.isSuspensa()) {                    
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Suspenso, -1, 0);
                continue;
            
            } else if (tarefa.isBloqueada()) { // Adicionado o estado Bloqueado do IO
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Bloqueado, -1, 0);
                continue;
            
            } else if (tarefa.getTempoChegada() > tempoAtual) {  
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1, 0);
                continue;
            }

            int cpuId = -1;  
            for (Processador proc : processadores) {
                if (proc.getTarefaAtual() != null && proc.getTarefaAtual().getId() == tarefa.getId()) {
                    cpuId = proc.getId();
                    break;
                }
            }

            if (cpuId != -1) {  
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Executando, cpuId, processadores[cpuId].getTicksNoQuantum());
            
            } else {            
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1, 0);
            }
        }
    }
    

    public int getTempoAtual() { 
        return tempoAtual; 
    }
    

    public boolean isFinalizado() {  
        for (Tarefa t : listaTarefasGeral) {
            if (!t.isFinalizada()) return false;        
        }
        return true; 
    }


    public Processador[] getProcessadores() { 
        return processadores; 
    }


    public void stepBack() {
        if (tempoAtual <= 0) return;
        --tempoAtual; 

        // Limpa qualquer interrupção (IRQ) pendente gerada no tick que estamos apagando
        filaIRQ.clear();

        for (Tarefa t : listaTarefasGeral) {
            Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tempoAtual);
            
            // --- REVERSAO DE I/O ---
            if (reg.estado == Tarefa.Estado.Bloqueado) {
                t.stepBackIO(); // Delega para a Tarefa a missão de recuar seu próprio I/O
            }

            // --- REVERSÃO DE ESPERA DE MUTEX ---
            if (reg.estado == Tarefa.Estado.EsperandoMutex) {
                Tarefa.TickSnapshot regAnterior = t.getRegistroNoTempo(tempoAtual - 1);
                
                //só tira da fila se no tick anterior ela não estava esperando mutex,
                // ou seja, se ela acabou de entrar na fila de espera do mutex
                if (regAnterior == null || regAnterior.estado != Tarefa.Estado.EsperandoMutex) {
                    int tempoRelativo = t.getTempoExecucao() - t.getTempoRestante();
                    List<Evento> eventos = t.getEventosNoTempoRelativo(tempoRelativo);
                    for (Evento ev : eventos) {
                        if (ev instanceof EventoMutex && ((EventoMutex) ev).isLock()) {
                            Mutex m = getMutex(((EventoMutex) ev).getMutexId());
                            m.getFilaDeEspera().remove(t);
                            t.setEsperandoMutex(false);
                        }
                    }
                }
            }

            // --- REVERSAO DA CPU E LOCKS/UNLOCKS ---
            if (reg.estado == Tarefa.Estado.Executando) {
                int tempoJaExecutado = t.getTempoExecucao() - t.getTempoRestante();
                
                t.setTempoRestante(t.getTempoRestante() + 1); 
                t.setFinalizada(false); 
                
                List<Evento> eventos = t.getEventosNoTempoRelativo(tempoJaExecutado);
                for (Evento ev : eventos) {
                    if (ev instanceof EventoMutex) {
                        EventoMutex evMutex = (EventoMutex) ev;
                        if (evMutex.isProcessado()) {
                            evMutex.setProcessado(false); 
                            Mutex m = getMutex(evMutex.getMutexId());
                            
                            if (evMutex.isLock()) {
                                if (m.getDonoAtual() != null && m.getDonoAtual().getId() == t.getId()) 
                                    m.setDonoAtual(null); 
                            } else {
                                Tarefa donoInvasor = m.getDonoAtual();
                                if (donoInvasor != null && donoInvasor.getId() != t.getId()) {
                                    donoInvasor.setEsperandoMutex(true);
                                    
                                    // Devolve a tarefa para o INÍCIO da fila
                                    if (m.getFilaDeEspera() instanceof java.util.LinkedList) {
                                        ((java.util.LinkedList<Tarefa>) m.getFilaDeEspera()).addFirst(donoInvasor);
                                    } else if (m.getFilaDeEspera() instanceof java.util.Deque) {
                                        ((java.util.Deque<Tarefa>) m.getFilaDeEspera()).addFirst(donoInvasor);
                                    } else {
                                        // Backup genérico caso a fila não seja Deque/LinkedList
                                        List<Tarefa> temp = new ArrayList<>(m.getFilaDeEspera());
                                        m.getFilaDeEspera().clear();
                                        m.getFilaDeEspera().add(donoInvasor);
                                        m.getFilaDeEspera().addAll(temp);
                                    }
                                }
                                m.setDonoAtual(t); 
                            }
                        }
                    }
                }
            }

            // --- REVERSÃO DE FINALIZAÇAO (Unlocks automaticos) ---
            if (reg.estado == Tarefa.Estado.Finalizado) {
                Tarefa.TickSnapshot regAnterior = t.getRegistroNoTempo(tempoAtual - 1);
                if (regAnterior != null && regAnterior.estado == Tarefa.Estado.Executando) {
                    int tempoRelativo = t.getTempoExecucao() - t.getTempoRestante(); 
                    List<Evento> eventosFinais = t.getEventosNoTempoRelativo(tempoRelativo);
                    for (Evento ev : eventosFinais) {
                        if (ev instanceof EventoMutex) {
                            EventoMutex evMutex = (EventoMutex) ev;
                            if (evMutex.isProcessado() && !evMutex.isLock()) {
                                evMutex.setProcessado(false);
                                Mutex m = getMutex(evMutex.getMutexId());
                                Tarefa donoInvasor = m.getDonoAtual();
                                if (donoInvasor != null && donoInvasor.getId() != t.getId()) {
                                    donoInvasor.setEsperandoMutex(true);
                                    
                                    // Repete a lógica de devolver ao INÍCIO da fila aqui
                                    if (m.getFilaDeEspera() instanceof java.util.LinkedList) {
                                        ((java.util.LinkedList<Tarefa>) m.getFilaDeEspera()).addFirst(donoInvasor);
                                    } else if (m.getFilaDeEspera() instanceof java.util.Deque) {
                                        ((java.util.Deque<Tarefa>) m.getFilaDeEspera()).addFirst(donoInvasor);
                                    } else {
                                        List<Tarefa> temp = new ArrayList<>(m.getFilaDeEspera());
                                        m.getFilaDeEspera().clear();
                                        m.getFilaDeEspera().add(donoInvasor);
                                        m.getFilaDeEspera().addAll(temp);
                                    }
                                }
                                m.setDonoAtual(t);
                            }
                        }
                    }
                }
            }
            
            t.apagarRegistroNoTempo(tempoAtual);
        }

        // Restaura as CPUs
        for (Processador cpu : processadores) {
            cpu.apagarRegistroOciosidade(tempoAtual); 
            cpu.setTarefaAtual(null);
            cpu.resetTicksNoQuantum(); 
        }

        // Restaura o estado anterior
        if (tempoAtual > 0) {
            int tickanterior = tempoAtual - 1;
            for (Tarefa t : listaTarefasGeral) {
                t.restaurarEnvelhecimento(tickanterior);

                Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tickanterior);
                
                // Sincroniza a flag de Espera baseada no estado passado
                t.setEsperandoMutex(reg.estado == Tarefa.Estado.EsperandoMutex);

                if (reg.estado == Tarefa.Estado.Executando) {
                    Processador cpu = processadores[reg.cpuId];
                    cpu.setTarefaAtual(t); 
                    
                    int contagemQuantum = 0;
                    for (int i = tickanterior; i >= 0; i--) {
                        Tarefa.TickSnapshot retrocesso = t.getRegistroNoTempo(i);
                        if (retrocesso.estado == Tarefa.Estado.Executando && retrocesso.cpuId == cpu.getId()) {
                            contagemQuantum++;
                        } else {
                            break; 
                        }
                    }
                    cpu.setTicksNoQuantum(contagemQuantum);
                }
            }
        } else {
            // Se voltou para o tick 0, garante que ninguém está esperando Mutex
            for (Tarefa t : listaTarefasGeral) {
                t.setEsperandoMutex(false);
            }
        }
    }
}
package simulador;

import java.util.ArrayList;
import java.util.List;

public class SOMP {
    private Escalonador escalonador;
    private Processador[] processadores;
    private int tempoAtual;
    private java.util.Map<Integer, Mutex> tabelaMutex = new java.util.HashMap<>();

    private ArrayList<Tarefa> listaTarefasGeral;

    public SOMP(Escalonador escalonador, int numProcessadores) {
        this.escalonador = escalonador;
        this.processadores = new Processador[numProcessadores];
        for (int i = 0; i < numProcessadores; i++) {
            this.processadores[i] = new Processador(i);
        }
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
        filaIRQ.add(new TratamentoIRQ(t, io)); // Hardware enfileira a IRQ
    }

    // Um novo método para receber as tarefas do LeitorConfig:
    public void adicionarTarefa(Tarefa t) {
        this.listaTarefasGeral.add(t);
    }

    // Método para o Main poder puxar a lista de tarefas e enviar para a Window:
    // public List<Tarefa> getListaTarefasGeral() {
    //     return listaTarefasGeral;
    // }

    public int getTotalNumTarefas() {
        return listaTarefasGeral.size();
    }

    public Tarefa getTarefaByIdx(int idx) {
        if (listaTarefasGeral != null)  // && id >= 0 && id < listaTarefasGeral.size())
            return listaTarefasGeral.get(idx);  // get ja tem checagem de idx
        return null;
    }

    //Também cria se não estiver criado o mutex  
    private Mutex getMutex(int mutexId) {
        tabelaMutex.putIfAbsent(mutexId, new Mutex(mutexId));
        return tabelaMutex.get(mutexId);
    }

    public boolean isTravado() {
        for (Tarefa t : listaTarefasGeral) {
            final boolean temp = !t.isFinalizada() && (
                !t.isSuspensa() || t.getTempoChegada() > tempoAtual  // Se existe alguma tarefa que não está suspensa ou que ainda não chegou no sistema, a simulação pode progredir
            );
            
            if (temp)
                return false;  
        }
        
        return true;  // Se o for terminar e não achar ninguém apto a rodar, o sistema está travado 
    }

    public void executar() {

        for (TratamentoIRQ irq : filaIRQ) {                // Processamento de Interrupções, instante imediatamente posterior
            irq.io.setIrqTratada(true);             // Informa ao IO que o SO capturou o sinal
            irq.tarefa.avancarEventoIO();                  // Acorda a tarefa
            System.out.println("Tick " + tempoAtual + ": SO processou IRQ da Tarefa T" + irq.tarefa.getId());
        }
        filaIRQ.clear();                                   // Limpa as IRQs tratadas

        for (Processador cpu : processadores) {            // Verifica quantum por causa da preempção por tempo
            Tarefa t = cpu.getTarefaAtual();
            
            if (t != null && cpu.getTicksNoQuantum() >= t.getQuantum()) {
                if (t.isBloqueada()) {
                    cpu.setTarefaAtual(null); // Sai para fazer I/O
                    cpu.resetTicksNoQuantum();
            
                } else if (cpu.getTicksNoQuantum() >= cpu.getTarefaAtual().getQuantum()) {
                    cpu.setTarefaAtual(null); // Expulsa da CPU pelo tempo
                    cpu.resetTicksNoQuantum();
                }
            }
        }

        escalonador.limparFila();                          // Reconstroi a fila
        for (Tarefa t : listaTarefasGeral) {
            if (t.getTempoChegada() <= tempoAtual && !t.isFinalizada() && !t.isSuspensa() && !t.isBloqueada())
                escalonador.adicionarTarefa(t);
        }

        escalonador.limparFila();                          // Reconstroi a fila
        for (Tarefa t : listaTarefasGeral) {
            if (t.getTempoChegada() <= tempoAtual && !t.isFinalizada() && !t.isSuspensa() && !t.isBloqueada())
                escalonador.adicionarTarefa(t);
        }
        escalonador.prepararFila(processadores);  
        

        // 3. Pega as tarefas da fila (Substituído FOR por WHILE para processar o Mutex dinamicamente)
        List<Tarefa> topTarefas = new ArrayList<>();
        
        while (topTarefas.size() < processadores.length) {
            Tarefa proxima = escalonador.obterProximaTarefa();
            if (proxima == null) {
                break; // A fila esvaziou, não há mais tarefas prontas
            }

            // --- PROCESSAMENTO DO MUTEX PARA A TAREFA SELECIONADA ---
            boolean foiBloqueada = false;
            int tempoJaExecutado = proxima.getTempoExecucao() - proxima.getTempoRestante();
            List<Evento> eventosDoTick = proxima.getEventosNoTempoRelativo(tempoJaExecutado);
            List<Evento> eventosProcessados = new ArrayList<>();

            for (Evento ev : eventosDoTick) {
                if (ev instanceof EventoMutex) {
                    EventoMutex evMutex = (EventoMutex) ev;
                    Mutex m = getMutex(evMutex.getMutexId()); // Use seu método de buscar o Mutex
                    
                    if (evMutex.isLock()) { // ML (Lock)
                        if (m.isLivre() || (m.getDonoAtual() != null && m.getDonoAtual().getId() == proxima.getId())) {
                            m.setDonoAtual(proxima); // Conseguiu a trava!
                            eventosProcessados.add(ev);
                        } else {
                            // Ocupado! A tarefa vai pra fila do Mutex e não assume a CPU
                            if (!m.getFilaDeEspera().contains(proxima)) {
                                m.getFilaDeEspera().add(proxima);
                            }
                            proxima.setEsperandoMutex(true);
                            foiBloqueada = true;
                            break; // Interrompe a leitura de eventos, ela já está bloqueada
                        }
                    } else { // MU (Unlock)
                        if (m.getDonoAtual() != null && m.getDonoAtual().getId() == proxima.getId()) {
                            m.setDonoAtual(null); // Soltou a trava!
                            eventosProcessados.add(ev);
                            
                            if (!m.getFilaDeEspera().isEmpty()) {
                                // Acorda a próxima tarefa imediatamente
                                Tarefa liberada = m.getFilaDeEspera().poll();
                                liberada.setEsperandoMutex(false);
                                m.setDonoAtual(liberada); // A liberada já ganha a trava
                                
                                // INJEÇÃO DIRETA: devolve pro escalonador pra tentar rodar NESTE tick
                                escalonador.adicionarTarefa(liberada);
                                
                                // Se o seu escalonador precisa reordenar após adição, descomente a linha abaixo:
                                // escalonador.prepararFila(processadores); 
                            }
                        }
                    }
                }
            }
            
            // Limpa os eventos executados para não repetirem no futuro
            proxima.getEventos().removeAll(eventosProcessados);
            // --- FIM DO PROCESSAMENTO DO MUTEX ---

            // Se ela passou ilesa pelos bloqueios de Mutex, entra nas topTarefas
            if (!foiBloqueada) {
                topTarefas.add(proxima);
                if(escalonador instanceof EscalonadorPRIOPENV) {
                    proxima.resetarEnvelhecimento();
                }
            }
            // Se foi bloqueada, o while apenas recomeça e puxa a próxima da fila!
        }

        // 4. Distribui as tarefas nos processadores mantendo a afinidade
        for (Processador cpu : processadores) {
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual != null && topTarefas.contains(tarefaAtual)) {
                topTarefas.remove(tarefaAtual); // Continua no mesmo processador
            } else {
                cpu.setTarefaAtual(null); // Sai do processador
            }
        }

        //Atribui as tarefas restantes
        for (Processador cpu : processadores) { 
            if (cpu.idle() && !topTarefas.isEmpty()) {
                cpu.setTarefaAtual(topTarefas.remove(0));  
            }
        }

        // 6. Grava o estado e executa o tick
        gravarHistorico();
        for (Processador cpu : processadores) {
            cpu.registrarOciosidade();
            cpu.executar();
        }

        for (Tarefa t : listaTarefasGeral) {
            if (t.getTempoChegada() <= tempoAtual && t.isBloqueada())
                t.executarIO(this);
        }

        ++tempoAtual;
    }

    private void gravarHistorico() {
        for (Tarefa tarefa : listaTarefasGeral) {
            if (tarefa.isFinalizada()) {                         // Terminou
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1, 0);
                continue;
            } else if (tarefa.isEsperandoMutex()) {              
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.EsperandoMutex, -1, 0);
            }
            if (tarefa.getTempoChegada() > tempoAtual) {  
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.NaoCriada, -1, 0);
                continue;
            
            } else if (tarefa.isFinalizada()) {                          
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1, 0);
                continue;
            
            } else if (tarefa.isSuspensa()) {                    
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Suspenso, -1, 0);
                continue;
            
            } else if (tarefa.isBloqueada()) {                   
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Bloqueado, -1, 0);
                continue;
            }

            int cpuId = -1;  // Descobre se a tarefa está em algum processador
            for (Processador proc : processadores) {
                if (
                    proc.getTarefaAtual()         != null && 
                    proc.getTarefaAtual().getId() == tarefa.getId()
                ) {
                    cpuId = proc.getId();
                    break;
                }
            }

            if (cpuId != -1) {  // Ta rodando em algum lugar
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Executando, cpuId, processadores[cpuId].getTicksNoQuantum());
            } else {            // Não ta rodando, mas já chegou e não terminou -> está esperando
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1, 0);
            }
        }
    }
    
    public int getTempoAtual() {  // Retorna o tempo real da simulação
        return tempoAtual; 
    }
    
    public boolean isFinalizado() {  // Verifica se todas as tarefas já terminaram
        for (Tarefa t : listaTarefasGeral) {
            if (!t.isFinalizada()) {
                return false;       // Se encontrar uma que não terminou, retorna falso
            }
        }
        return true; // Todas terminaram
    }

    public Processador[] getProcessadores() {
        return processadores;
    }

    public void stepBack() {
        if (tempoAtual <= 0) 
            return;

        filaIRQ.clear();                                       // Limpa interrupções que ficaram no limbo do retrocesso
        --tempoAtual;                                          //Volta 1 tick

        for (Tarefa t : listaTarefasGeral) {                   // Restaura o valor das tarefas
            Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tempoAtual);
            
            if (reg.estado == Tarefa.Estado.Executando) {
                t.setTempoRestante(t.getTempoRestante() + 1);  // Devolve o tempo que tinha gasto
                t.setFinalizada(false);            // Ressuscita a tarefa caso ela tenha morrido neste tick
            
            } else if (reg.estado == Tarefa.Estado.Bloqueado) { 
                t.reverterTickIO(); 
            }
            
            t.apagarRegistroNoTempo(tempoAtual);                // Apaga a coluna a frente
        }

        // Limpa os processadores
        // O método executar vai reconstruir a fila a partir da lista geral
        for (Processador cpu : processadores) {
            cpu.apagarRegistroOciosidade(tempoAtual); //desfaz o registro de ociosidade naquele tick
            cpu.setTarefaAtual(null);
            cpu.resetTicksNoQuantum(); 
        }

        //Reconstroi o estado exato
        if(tempoAtual > 0)
        {
            int tickanterior = tempoAtual - 1;
            // Restaura o envelhecimento das tarefas
            for (Tarefa t : listaTarefasGeral) {
                t.restaurarEnvelhecimento(tickanterior);

                Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tickanterior);
                if (reg.estado == Tarefa.Estado.Executando) {
                    Processador cpu = processadores[reg.cpuId];
                    
                    // Devolve a tarefa pra cpu
                    cpu.setTarefaAtual(t); 
                    
                    // Calcula quantos ticks seguidos essa tarefa estava rodando no passado
                    int contagemQuantum = 0;
                    for (int i = tickanterior; i >= 0; i--) {
                        Tarefa.TickSnapshot retrocesso = t.getRegistroNoTempo(i);
                        if (retrocesso.estado == Tarefa.Estado.Executando && retrocesso.cpuId == cpu.getId()) {
                            contagemQuantum++;
                        } else {
                            break; // Se não estava executando quebra a contagem
                        }
                    }
                    // Devolve os ticks do quantum para a preempção funcionar perfeitamente
                    cpu.setTicksNoQuantum(contagemQuantum);
                }
            }
        }
    }
}
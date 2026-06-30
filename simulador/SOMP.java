package simulador;

import java.util.ArrayList;
import java.util.List;

public class SOMP {
    private Escalonador escalonador;
    private Processador[] processadores;
    private int tempoAtual;
    private int quantum;

    private ArrayList<Tarefa> listaTarefasGeral;

    public SOMP(Escalonador escalonador, int numProcessadores, int quantum) {
        this.escalonador = escalonador;
        this.processadores = new Processador[numProcessadores];
        for (int i = 0; i < numProcessadores; i++) {
            this.processadores[i] = new Processador(i);
        }
        this.quantum = quantum;
        this.tempoAtual = 0;

        this.listaTarefasGeral = new ArrayList<>();
    }

    // Um novo método para receber as tarefas do LeitorConfig:
    public void adicionarTarefa(Tarefa t) {
        this.listaTarefasGeral.add(t);
    }

    public int getTotalNumTarefas() {
        return listaTarefasGeral.size();
    }

    public Tarefa getTarefaByIdx(int idx) {
        if (listaTarefasGeral != null)  // && id >= 0 && id < listaTarefasGeral.size())
            return listaTarefasGeral.get(idx);  // get ja tem checagem de idx
        return null;
    }

    public void executar() {  
        // Reconstroi a fila
        escalonador.limparFila();
        for (Tarefa t : listaTarefasGeral) {
            // Se a tarefa está pronta ainda não acabou e não está suspensa entra na fila
            if (t.getTempoChegada() <= tempoAtual && !t.isFinalizada() && !t.isSuspensa()) {
                escalonador.adicionarTarefa(t);
            }
        }

        // Verifica quantum por causa da preempção por tempo
        for (Processador cpu : processadores) {
            Tarefa t = cpu.getTarefaAtual();
            if (t != null && cpu.getTicksNoQuantum() >= quantum) {
                cpu.setTarefaAtual(null); // Expulsa da CPU
                cpu.resetTicksNoQuantum();
            }
        }

        escalonador.prepararFila(processadores, quantum);  

        // Pega as tarefas da fila pela função obterproximatarefa
        List<Tarefa> topTarefas = new ArrayList<>();
        for (int i = 0; i < processadores.length; i++) {
            Tarefa proxima = escalonador.obterProximaTarefa();
            if (proxima != null) {
                topTarefas.add(proxima);
                if(escalonador instanceof EscalonadorPRIOPENV) {
                    proxima.resetarEnvelhecimento();
                }
            }
        }

        // Distribui as tarefas nos processadores mantendo a afinidade com as tarefas
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

        // Grava o estado e executa o tick
        gravarHistorico();
        for (Processador cpu : processadores) {
            cpu.registrarOciosidade();
            cpu.executar();
        }
        ++tempoAtual;
    }

    private void gravarHistorico() {
        for (Tarefa tarefa : listaTarefasGeral) {
            if (tarefa.isFinalizada()) {                         // Terminou
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1);
                continue;
            } else if (tarefa.isSuspensa()) {                    // Suspenso
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Suspenso, -1);
                continue;
            } else if (tarefa.getTempoChegada() > tempoAtual) {  // Tarefa ainda não foi criada
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1);
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
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Executando, cpuId);
            } else {            // Não ta rodando, mas já chegou e não terminou -> está esperando
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Esperando, -1);
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
        if (tempoAtual <= 0) return;
        --tempoAtual; //Volta 1 tick

        // Restaura o valor das tarefas
        for (Tarefa t : listaTarefasGeral) {
            Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tempoAtual);
            
            if (reg.estado == Tarefa.Estado.Executando) {
                t.setTempoRestante(t.getTempoRestante() + 1); // Devolve o tempo que tinha gasto
                t.setFinalizada(false); // Ressuscita a tarefa caso ela tenha morrido neste tick
            }
            
            // Apaga a coluna a frente
            t.apagarRegistroNoTempo(tempoAtual);
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
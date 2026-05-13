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

    public void executar() {  
        // 1. RECONSTRÓI A FILA DO ZERO (Evita que as tarefas caiam num buraco negro)
        escalonador.limparFila();
        for (Tarefa t : listaTarefasGeral) {
            // Se a tarefa já chegou, não acabou e não está suspensa, ela vai para a disputa!
            if (t.getTempoChegada() <= tempoAtual && !t.isFinalizada() && !t.isSuspensa()) {
                escalonador.adicionarTarefa(t);
            }
        }

        // 2. VERIFICA O QUANTUM (Preempção por tempo)
        for (Processador cpu : processadores) {
            Tarefa t = cpu.getTarefaAtual();
            if (t != null && cpu.getTicksNoQuantum() >= quantum) {
                cpu.setTarefaAtual(null); // Expulsa da CPU (o passo 1 já garantiu que ela está na fila)
                cpu.resetTicksNoQuantum();
            }
        }

        // 3. ESCALONADOR ORGANIZA AS PRIORIDADES
        escalonador.prepararFila(processadores, quantum);  

        // 4. SOMP PEGA AS 'N' PRIMEIRAS TAREFAS
        List<Tarefa> topTarefas = new ArrayList<>();
        for (int i = 0; i < processadores.length; i++) {
            Tarefa proxima = escalonador.obterProximaTarefa();
            if (proxima != null) {
                topTarefas.add(proxima);
            }
        }

        // 5. DISTRIBUIÇÃO NAS CPUS (Afinidade e preenchimento)
        for (Processador cpu : processadores) {
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual != null && topTarefas.contains(tarefaAtual)) {
                topTarefas.remove(tarefaAtual); // Continua na mesma CPU
            } else {
                cpu.setTarefaAtual(null); // Saiu da CPU
            }
        }
        for (Processador cpu : processadores) { 
            if (cpu.idle() && !topTarefas.isEmpty()) {
                cpu.setTarefaAtual(topTarefas.remove(0));  
            }
        }

        // 6. GRAVA O ESTADO E EXECUTA O TICK
        gravarHistorico();
        for (Processador cpu : processadores) { cpu.executar(); }
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

            int cpuId = -1;  // Descobre se a tarefa está em alguma cpu
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

    public void stepBack() {
        if (tempoAtual <= 0) return;
        --tempoAtual; // A máquina do tempo: volta 1 tick

        // 1. Restaura a "Vida" matemática das tarefas
        for (Tarefa t : listaTarefasGeral) {
            Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tempoAtual);
            
            if (reg.estado == Tarefa.Estado.Executando) {
                t.setTempoRestante(t.getTempoRestante() + 1); // Devolve o tempo que tinha gasto
                t.setFinalizada(false); // Ressuscita a tarefa caso ela tenha morrido neste tick
            }
            
            // Apaga a linha gráfica do futuro
            t.apagarRegistroNoTempo(tempoAtual);
        }

        // 2. Limpa as CPUs. 
        // Não se preocupe em devolvê-las ao Escalonador agora. Quando você clicar em "Próximo Passo", 
        // o novo método executar() (ali em cima) vai reconstruir a fila perfeitamente a partir da Lista Geral!
        for (Processador cpu : processadores) {
            cpu.setTarefaAtual(null);
            cpu.resetTicksNoQuantum(); 
        }
    }
}
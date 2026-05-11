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
    public List<Tarefa> getListaTarefasGeral() {
        return listaTarefasGeral;
    }

    // Execução de 1 tick
    public void executar() {
        for (Tarefa tarefa : listaTarefasGeral) {  // Coloca tarefas que chegaram agora no escalonador
            if (tarefa.getTempoChegada() == tempoAtual)
                escalonador.adicionarTarefa(tarefa);
        }

        escalonador.executar(processadores, quantum);       // Escalonador executa seu algoritmo

        gravarHistorico();

        for (Processador cpu : processadores) {    // Executa 1 tick por processador
            cpu.executar();                       
        }

        ++tempoAtual;
    }

    private void gravarHistorico() {
        for (Tarefa tarefa : listaTarefasGeral) {
            if (tarefa.isFinalizada()) {  // Terminou
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Finalizado, -1);
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
            } else {            // Não ta rodando, mas já chegou e não terminou -> está Suspenso
                tarefa.registrarEstado(tempoAtual, Tarefa.Estado.Suspenso, -1);
            }
        }
    }
}
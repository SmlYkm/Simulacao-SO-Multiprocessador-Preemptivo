package simulador;

import java.util.ArrayList;
import java.util.List;


public class SOMP {
    private Escalonador escalonador;
    private Processador[] processadores;
    private int tempoAtual;

    private List<Tarefa> listaTarefasGeral;


    public SOMP(Escalonador escalonador, int numProcessadores) {
        this.escalonador = escalonador;
        this.processadores = new Processador[numProcessadores];
        for (int i = 0; i < numProcessadores; i++) {
            this.processadores[i] = new Processador(i);
        }
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

    public void executar() {
        // Lógica de execução do simulador
    }
}
package simulador;

import java.util.ArrayList;
// import simulador.Tarefa;

public abstract class Escalonador {
    protected ArrayList<Tarefa> tarefas;

    public Escalonador () {
        tarefas = new ArrayList<>();
    }
    
    // Coloca tarefa no final da fila
    public void adicionarTarefa(Tarefa tarefa) {
        tarefas.add(tarefa);
    }

    // Executa 1 tick, recebe lista de processadores e ve quem fica com qual tarefa
    public abstract void executar(Processador[] cpus);
}

package simulador;

import java.util.ArrayList;
// import simulador.Tarefa;

public abstract class Escalonador {
    protected ArrayList<Tarefa> listaTarefas;

    public Escalonador () {
        listaTarefas = new ArrayList<>();
    }
    
    public abstract void adicionarTarefa(Tarefa novaTarefa);
    public abstract void executar();  // Executa 1 tick
}

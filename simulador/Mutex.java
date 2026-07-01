package simulador;

import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
    private int id;
    private Tarefa donoAtual;
    private Queue<Tarefa> filaDeEspera;

    public Mutex(int id) {
        this.id = id;
        this.donoAtual = null;
        this.filaDeEspera = new LinkedList<>();
    }

    public int getId() { return id; }
    
    public Tarefa getDonoAtual() { return donoAtual; }
    
    public void setDonoAtual(Tarefa donoAtual) { this.donoAtual = donoAtual; }
    
    public Queue<Tarefa> getFilaDeEspera() { return filaDeEspera; }

    public boolean isLivre() {
        return donoAtual == null;
    }
}
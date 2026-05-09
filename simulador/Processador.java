package simulador;

public class Processador {
    private int id;
    private Tarefa tarefaAtual; // Se for null, o processador está "desligado"
    private int tempoDesligado; // Acumula os ticks em que ficou ocioso

    public Processador(int id) {
        this.id = id;
        this.tempoDesligado = 0;
        this.tarefaAtual = null;
    }
    
    // Getters e setters básicos...
    public int getId() {
        return id;
    }

    public Tarefa getTarefaAtual() {
        return tarefaAtual;
    }

    public int getTempoDesligado() {
        return tempoDesligado;
    }

    public void setTarefaAtual(Tarefa tarefaAtual) {
        this.tarefaAtual = tarefaAtual;
    }

    public void setTempoDesligado(int tempoDesligado) {
        this.tempoDesligado = tempoDesligado;
    }

}

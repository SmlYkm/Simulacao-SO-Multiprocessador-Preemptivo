package simulador;

public abstract class Evento {  //Classe que vai servir de classe abstrata para os eventos de I/O e mutex na parte B.
    protected int tempoChegada;
    protected int tempoExecucao;

    protected Evento(int tempoChegada, int tempoExecucao) {
        this.tempoChegada   = tempoChegada;
        this.tempoExecucao  = tempoExecucao;
    }

    public int getTempoChegada() {
        return tempoChegada;
    }

    public int getTempoExecucao() {
        return tempoExecucao;
    }
}
package simulador;

public abstract class Evento {  //Classe que vai servir de classe abstrata para os eventos de I/O e mutex na parte B.
    private int tempoChegada;

    protected Evento(int tempoChegada) {
        this.tempoChegada = tempoChegada;
    }

    public int getTempoChegada() {
        return tempoChegada;
    }
}
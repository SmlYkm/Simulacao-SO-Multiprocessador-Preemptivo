package simulador;
abstract class Evento {
    private int tempoChegada;
    private int tempoExecucao;

    protected Evento(int tempoChegada, int tempoExecucao) {
        this.tempoChegada = tempoChegada;
        this.tempoExecucao = tempoExecucao;
    }

    public int getTempoChegada() {
        return tempoChegada;

    }

    public int getTempoExecucao() {
        return tempoExecucao;
    }
}
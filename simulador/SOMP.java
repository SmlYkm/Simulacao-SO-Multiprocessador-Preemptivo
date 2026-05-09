package simulador;

public class SOMP {
    private Escalonador escalonador;
    private Processador[] processadores;
    private int tempoAtual;

    public SOMP(Escalonador escalonador, int numProcessadores) {
        this.escalonador = escalonador;
        this.processadores = new Processador[numProcessadores];
        for (int i = 0; i < numProcessadores; i++) {
            this.processadores[i] = new Processador(i);
        }
        this.tempoAtual = 0;
    }

    public void executar() {
        // Lógica de execução do simulador
    }
}
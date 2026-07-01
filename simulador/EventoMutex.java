package simulador;

public class EventoMutex extends Evento {
    private int mutexId;
    private boolean isLock; // true = Solicitar (ML), false = Liberar (MU)

    public EventoMutex(int instanteRelativo, int mutexId, boolean isLock) {
        super(instanteRelativo);
        this.mutexId = mutexId;
        this.isLock = isLock;
    }

    public int getMutexId() {
        return mutexId;
    }

    public boolean isLock() {
        return isLock;
    }
}
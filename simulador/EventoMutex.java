package simulador;

public class EventoMutex extends Evento {
    private int     mutexId;
    private boolean isLock; // true = Solicitar (ML), false = Liberar (MU)
    private boolean processado;

    public EventoMutex(int instanteRelativo, int mutexId, boolean isLock) {
        super(instanteRelativo);
        this.mutexId    = mutexId;
        this.isLock     = isLock;
        this.processado = false; 
    }

    
    public int getMutexId() { 
        return mutexId; 
    }


    public boolean isLock() { 
        return isLock; 
    }
    
    
    public boolean isProcessado() { 
        return processado; 
    }
    
    
    public void setProcessado(boolean processado) { 
        this.processado = processado; 
    }
}
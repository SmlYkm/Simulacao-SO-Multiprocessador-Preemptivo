package simulador;

import java.util.ArrayList;
import java.util.List;

public class Tarefa {
    private static int quantum;
    
    private int     id;
    private int     prioridadeEstatica;
    private int     prioridadeDinamica;
    private int     tempoEsperando; 
    private int     tempoExecucao;
    private int     tempoRestante;
    private int     tempoChegada;
    private double  valorSorteioAtual;
    private boolean finalizada;
    private boolean suspensa;
    private boolean envolvidaEmSorteio;
    private boolean esperandoMutex;
    private String  cor;

    private List<Evento>       eventos;
    private List<TickSnapshot> historico;

    public enum Estado {
        NaoCriada,  
        Esperando,  
        Executando, 
        Suspenso,
        EsperandoMutex,
        Finalizado,
        Bloqueado  // Tarefa na fila de dispositivos IO 
    }

    public class TickSnapshot { 
        public Estado  estado;
        public int     cpuId;
        public boolean ocorreuSorteio;
        public int     prioridadeDinamica; 
        public int     tempoEsperando; 
        public int     ticksNoQuantum; 

        public TickSnapshot(
            Estado  estado, 
            int     cpuId, 
            boolean ocorreuSorteio, 
            int     prioridadeDinamica, 
            int     tempoEsperando, 
            int     ticksNoQuantum
        ) {
            this.estado             = estado;
            this.cpuId              = cpuId;
            this.ocorreuSorteio     = ocorreuSorteio;
            this.prioridadeDinamica = prioridadeDinamica;
            this.tempoEsperando     = tempoEsperando;
            this.ticksNoQuantum     = ticksNoQuantum;
        }
    }

    public Tarefa(
        int          id, 
        String       cor, 
        int          tempoChegada, 
        int          tempoExecucao, 
        int          prioridadeEstatica, 
        List<Evento> lista_eventos
    ) {
        this.id                 = id;
        this.tempoChegada       = tempoChegada;
        this.tempoExecucao      = tempoExecucao;
        this.tempoRestante      = tempoExecucao;
        this.prioridadeEstatica = prioridadeEstatica;
        this.finalizada         = false;
        this.suspensa           = false;
        this.envolvidaEmSorteio = false;
        this.esperandoMutex     = false;
        this.cor                =  "#" + cor;
        this.eventos            = new ArrayList<>();
        
        if (lista_eventos != null)
            this.eventos.addAll(lista_eventos);

        this.historico = new ArrayList<>();
    }

    
    public int getId() { 
        return id; 
    }


    public int getprioridadeEstatica() { 
        return prioridadeEstatica; 
    }
    
    
    public int getprioridadeDinamica() { 
        return prioridadeDinamica; 
    }
    
    
    public int getTempoExecucao() { 
        return tempoExecucao; 
    }
    
    
    public int getTempoRestante() { 
        return tempoRestante; 
    }
    
    
    public int getTempoChegada() { 
        return tempoChegada; 
    }
    
    
    public double getValorSorteioAtual() { 
        return valorSorteioAtual; 
    }
    
    
    public int getQuantum() { 
        return quantum; 
    }
    
    
    public boolean isFinalizada() { 
        return finalizada; 
    }
    
    
    public boolean isSuspensa() { 
        return suspensa; 
    }
    
    
    public boolean isEnvolvidaEmSorteio() { 
        return envolvidaEmSorteio; 
    }
    
    
    public boolean isEsperandoMutex() { 
        return esperandoMutex; 
    }
    
    
    public String getCor() { 
        return cor; 
    }
    
    
    public List<Evento> getEventos() { 
        return eventos; 
    }
    

    public List<Evento> getEventosNoTempoRelativo(int tempoRelativo) {
        List<Evento> eventosDoTick = new ArrayList<>();
        
        if (this.eventos != null) {
            for (Evento ev : this.eventos) {
                if (ev.getTempoChegada() == tempoRelativo) 
                    eventosDoTick.add(ev);
            }
        }
        return eventosDoTick;
    }
    

    public boolean isBloqueada() {  // Procura pelo I/O do tick atual que não foi tratado
        int tempoDecorrido = tempoExecucao - tempoRestante;
        for (Evento ev : eventos) {
            if (ev instanceof IO) {
                IO io = (IO) ev;
                if (tempoDecorrido == io.getTempoChegada() && !io.isIrqTratada())
                    return true;
            }
        }
        return false;
    }


    public void executarIO(SOMP so) {
        int tempoDecorrido = tempoExecucao - tempoRestante;
        
        for (Evento ev : eventos) {
            if (ev instanceof IO) {
                IO io = (IO) ev;
                
                if (tempoDecorrido == io.getTempoChegada() && !io.isIrqTratada()) {
                    io.execTick(this, so);
                    return;  // Retorna pois executa 1 de cada vez
                }
            }
        }
    }
    
    
    public void setprioridadeEstatica(int prioridadeEstatica) { 
        this.prioridadeEstatica = prioridadeEstatica; 
    }
    
    
    public void setTempoRestante(int tempoRestante) { 
        this.tempoRestante = tempoRestante; 
    }
    
    
    public void setValorSorteioAtual(double valorSorteioAtual) { 
        this.valorSorteioAtual = valorSorteioAtual; 
    }
    

    public void setFinalizada(boolean finalizada) { 
        this.finalizada = finalizada; 
    }

    
    public static void setQuantum(int quantum) { 
        Tarefa.quantum = quantum; 
    }
    

    public void suspender(boolean suspensa) { 
        this.suspensa = suspensa; 
    }
    
    
    public void setEnvolvidaEmSorteio(boolean envolvidaEmSorteio) { 
        this.envolvidaEmSorteio = envolvidaEmSorteio; 
    }
    
    
    public void setEsperandoMutex(boolean esperandoMutex) { 
        this.esperandoMutex = esperandoMutex; 
    }
    

    public void setCor(String cor) { 
        this.cor = cor; 
    }
    
    
    public void adicionarEvento(Evento evento) { 
        this.eventos.add(evento); 
    }
    
    
    public void apagarRegistroNoTempo(int tempo) { 
        if(tempo >= 0 && tempo < historico.size()) 
            historico.remove(tempo); 
    }


    public void registrarEstado(int tempoAtual, Estado estado, int cpuId, int ticksNoQuantum) {
        if (this.envolvidaEmSorteio)
            System.out.println("Salvando histórico: A T" + this.id + " TEM sorteio no tick " + tempoAtual);

        while (historico.size() <= tempoAtual)
            historico.add(
                new TickSnapshot(
                    Estado.NaoCriada, 
                    -1, 
                    false, 
                    this.prioridadeDinamica, 
                    this.tempoEsperando, 
                    ticksNoQuantum
                )
            );
        
        historico.set(
            tempoAtual, 
            new TickSnapshot(
                estado, 
                cpuId, 
                this.envolvidaEmSorteio, 
                this.prioridadeDinamica, 
                this.tempoEsperando, 
                ticksNoQuantum
            )
        );
        
        this.envolvidaEmSorteio = false;
    }


    public TickSnapshot getRegistroNoTempo(int tempo) {
        if (tempo >= 0 && tempo < historico.size()) 
            return historico.get(tempo);
        
        return new TickSnapshot(
            Estado.NaoCriada, -1, false, -1, -1, -1
        ); 
    }


    public void envelhecer(int alpha) {
        tempoEsperando++;
        prioridadeDinamica = prioridadeEstatica + tempoEsperando * alpha;
    }


    public void resetarEnvelhecimento() {
        tempoEsperando     = 0;
        prioridadeDinamica = prioridadeEstatica;
    }


    public void restaurarEnvelhecimento(int tempo){
        TickSnapshot oldSnapshot = getRegistroNoTempo(tempo);
        
        if(oldSnapshot != null) {
            this.prioridadeDinamica = oldSnapshot.prioridadeDinamica;
            this.tempoEsperando = oldSnapshot.tempoEsperando;
        }
    }


    public void executar(int tempo) {
        if (tempoRestante <= 0)
            return;

        tempoRestante -= tempo;
        if (tempoRestante <= 0) {
            tempoRestante = 0;
            finalizada = true;
        }
    }


    public TickSnapshot popHistorico() { 
        if (historico.isEmpty()) 
            return null;
        return historico.remove(historico.size() - 1);
    }

    public boolean temEventoMutexNoTick(int tempoRelativo) {
        for (Evento ev : eventos) {
            if (ev instanceof EventoMutex) {
                EventoMutex em = (EventoMutex) ev;
                if (ev.getTempoChegada() == tempoRelativo && !em.isProcessado())
                    return true;
            }
        }
        return false;
    }

    public EventoMutex getEventoMutexNoTick(int tempoRelativo) {
        for (Evento ev : eventos) {
            if (ev instanceof EventoMutex) {
                EventoMutex em = (EventoMutex) ev;
                if (ev.getTempoChegada() == tempoRelativo && !em.isProcessado())
                    return em;
            }
        }
        return null;
    }
}
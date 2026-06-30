package simulador;

import java.util.ArrayList;
import java.util.List;

public class Tarefa {

    private int id;
    private int prioridadeEstatica;//PRIOP
    private int prioridadeDinamica;//PRIOPENV
    private int tempoEsperando; //PRIOENV
    private int tempoExecucao;
    private int tempoRestante;
    private int tempoChegada;
    private double valorSorteioAtual;
    private boolean finalizada;
    private boolean suspensa;
    private boolean envolvidaEmSorteio;
    private String cor;
    
    private List<Evento>       eventos;
    private List<TickSnapshot> historico;
    private int                eventoAtualIdx;

    public enum Estado {
        NaoCriada,  // SO ainda não recebeu a tarefa
        Esperando,  // SO recebeu tarefa e esta esperando para ser executada
        Executando, 
        Suspenso,   
        Finalizado,
        Bloqueado  // Tarefa na fila de dispositivos IO 
    }

    // Storage class para agregar estado e cpu de execução em dado tempo
    // Usado para manter um histórico de execução
    public class TickSnapshot { 
        public Estado estado;
        public int    cpuId;
        public boolean ocorreuSorteio;
        public int prioridadeDinamica; //PRIOPENV
        public int tempoEsperando; //PRIOPENV

        public TickSnapshot(Estado estado, int cpuId, boolean ocorreuSorteio, int prioridadeDinamica, int tempoEsperando) {
            this.estado = estado;
            this.cpuId  = cpuId;
            this.ocorreuSorteio = ocorreuSorteio;
            this.prioridadeDinamica = prioridadeDinamica;
            this.tempoEsperando = tempoEsperando;
        }
    }


    public Tarefa(int id, String cor, int tempoChegada, int tempoExecucao, int prioridadeEstatica, List<Evento> lista_eventos) {
        this.id = id;
        this.tempoChegada = tempoChegada;
        this.tempoExecucao = tempoExecucao;
        this.tempoRestante = tempoExecucao;
        this.prioridadeEstatica = prioridadeEstatica;
        this.finalizada = false;
        this.suspensa = false;
        this.envolvidaEmSorteio = false;
        this.cor =  "#" + cor;
        this.eventos = new ArrayList<>();
        if (lista_eventos != null) {
            this.eventos.addAll(lista_eventos);
        }
        this.historico = new ArrayList<>();
    }

    public int getId() { return id; }
    public int getprioridadeEstatica() { return prioridadeEstatica; }
    public int getprioridadeDinamica() { return prioridadeDinamica; }
    public int getTempoExecucao() { return tempoExecucao; }
    public int getTempoRestante() { return tempoRestante; }
    public int getTempoChegada() { return tempoChegada; }
    public double getValorSorteioAtual() { return valorSorteioAtual; }
    public boolean isFinalizada() { return finalizada; }
    public boolean isSuspensa() { return suspensa; }
    public boolean isEnvolvidaEmSorteio() { return envolvidaEmSorteio; }
    public String getCor() { return cor; }
    public List<Evento> getEventos() { return eventos; }
    
    public void setprioridadeEstatica(int prioridadeEstatica) { this.prioridadeEstatica = prioridadeEstatica; }
    public void setTempoRestante(int tempoRestante) { this.tempoRestante = tempoRestante; }
    public void setValorSorteioAtual(double valorSorteioAtual) { this.valorSorteioAtual = valorSorteioAtual; }
    public void setFinalizada(boolean finalizada) { this.finalizada = finalizada; }
    public void suspender(boolean suspensa) { this.suspensa = suspensa; }
    public void setEnvolvidaEmSorteio(boolean envolvidaEmSorteio) { this.envolvidaEmSorteio = envolvidaEmSorteio; }
    public void setCor(String cor) { this.cor = cor; }
    public void adicionarEvento(Evento evento) { this.eventos.add(evento); }
    public void apagarRegistroNoTempo(int tempo) { if(tempo >= 0 && tempo < historico.size()) historico.remove(tempo); }

    
    public boolean isBloqueada() {
        if (eventoAtualIdx >= eventos.size()) 
            return false;
        
        Evento prox = eventos.get(eventoAtualIdx);
        if (prox instanceof IO) {                               // Se for evento de IO
            int tempoDecorrido = tempoExecucao - tempoRestante; // Quantos ticks a tarefa executou na CPU
            
            return (                                            // Bloqueia se atingiu o momento de fazer o IO e esse IO ainda não terminou
                tempoDecorrido == prox.getTempoChegada() && 
                !((IO)prox).isFinalizado()
            ); 
        }

        return false;
    }


    public void executarIO() {
        if (!isBloqueada()) 
            return;
        
        IO io = (IO) eventos.get(eventoAtualIdx);
        io.execTick();
        if (io.isFinalizado())
            ++eventoAtualIdx; // Avança para a próxima requisição IO
    }


    public void reverterTickIO() {
        if (eventoAtualIdx < eventos.size()) {  // Primeiro, verifica se o IO atual estava em progresso e reverte
            Evento prox = eventos.get(eventoAtualIdx);
 
            if (prox instanceof IO) {
                IO io = (IO) prox;
 
                if (!io.isFinalizado() && io.getTempoExecutado() > 0) {
                    io.stepBack();
                    return;
                }
            }
        }
        
        
        if (eventoAtualIdx > 0) {                               // Se nenhum IO está em progresso no índice atual, pode significar que o IO terminou exatamente 
            Evento anterior = eventos.get(eventoAtualIdx - 1);  // no tick sendo desfeito, é necessario voltar o índice para reviver o I/O
            
            if (!(anterior instanceof IO)) 
                return;
            
            IO io = (IO) anterior;
            if (io.isFinalizado()) {
                io.stepBack();
                --eventoAtualIdx;                               // Volta indice
            }
        }
    }


    public void registrarEstado(int tempoAtual, Estado estado, int cpuId) {  // Método para gravar o que aconteceu neste tick

        if (this.envolvidaEmSorteio) {
            System.out.println("Salvando histórico: A T" + this.id + " TEM sorteio no tick " + tempoAtual);
        }
        
        // Preenche buracos se o tempo der saltos, por segurança
        while (historico.size() <= tempoAtual) {
            historico.add(new TickSnapshot(Estado.NaoCriada, -1, false, this.prioridadeDinamica, this.tempoEsperando));
        }
        // Grava o estado atual e se houve sorteio
        historico.set(tempoAtual, new TickSnapshot(estado, cpuId, this.envolvidaEmSorteio, this.prioridadeDinamica, this.tempoEsperando));
        
        // Reseta a flag para o próximo tick
        this.envolvidaEmSorteio = false;
    }

    public TickSnapshot getRegistroNoTempo(int tempo) {
        if (tempo >= 0 && tempo < historico.size())
            return historico.get(tempo);
        return new TickSnapshot(Estado.NaoCriada, -1, false, -1, -1); // Retorna um estado padrão se o tempo for inválido
    }

    public void envelhecer(int alpha) {
        tempoEsperando++;
        prioridadeDinamica = prioridadeEstatica + tempoEsperando * alpha;
    }

    public void resetarEnvelhecimento() {
        tempoEsperando = 0;
        prioridadeDinamica = prioridadeEstatica;
    }

    public void     restaurarEnvelhecimento(int tempo){
        TickSnapshot oldSnapshot = getRegistroNoTempo(tempo);

        if(oldSnapshot != null) {
            this.prioridadeDinamica = oldSnapshot.prioridadeDinamica;
            this.tempoEsperando = oldSnapshot.tempoEsperando;
        }
    }

    public void executar(int quantum) {
        if (tempoRestante > 0) {
            tempoRestante -= quantum;
            if (tempoRestante <= 0) {
                tempoRestante = 0;
                finalizada = true;
            }
        }
    }

    public Evento popEvento() {  // Remove ultimo evento
        if (eventos.isEmpty())
            return null;
        int idx = eventos.size() - 1;
        return eventos.remove(idx);
    }

    public TickSnapshot popHistorico() { //Remove o ultimo estado do historico
        if (historico.isEmpty())
            return null;
        int idx = historico.size() - 1;
        return historico.remove(idx);
    }

}
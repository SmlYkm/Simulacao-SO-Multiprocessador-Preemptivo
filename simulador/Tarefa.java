package simulador;

import java.util.ArrayList;
import java.util.List;

public class Tarefa {

    private int id;
    private int prioridade;
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

    public enum Estado {
        NaoCriada,  // SO ainda não recebeu a tarefa
        Esperando,  // SO recebeu tarefa e esta esperando para ser executada
        Executando, 
        Suspenso,   
        Finalizado    
    }

    // Storage class para agregar estado e cpu de execução em dado tempo
    // Usado para manter um histórico de execução
    public class TickSnapshot { 
        public Estado estado;
        public int    cpuId;
        public boolean ocorreuSorteio;

        public TickSnapshot(Estado estado, int cpuId, boolean ocorreuSorteio) {
            this.estado = estado;
            this.cpuId  = cpuId;
            this.ocorreuSorteio = ocorreuSorteio;
        }
    }


    public Tarefa(int id, String cor, int tempoChegada, int tempoExecucao, int prioridade, List<Evento> lista_eventos) {
        this.id = id;
        this.tempoChegada = tempoChegada;
        this.tempoExecucao = tempoExecucao;
        this.tempoRestante = tempoExecucao;
        this.prioridade = prioridade;
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

    // Getters e setters
    public int getId() { return id; }
    public int getPrioridade() { return prioridade; }
    public int getTempoExecucao() { return tempoExecucao; }
    public int getTempoRestante() { return tempoRestante; }
    public int getTempoChegada() { return tempoChegada; }
    public double getValorSorteioAtual() { return valorSorteioAtual; }
    public boolean isFinalizada() { return finalizada; }
    public boolean isSuspensa() { return suspensa; }
    public boolean isEnvolvidaEmSorteio() { return envolvidaEmSorteio; }
    public String getCor() { return cor; }
    public List<Evento> getEventos() { return eventos; }
    
    public void setPrioridade(int prioridade) { this.prioridade = prioridade; }
    public void setTempoRestante(int tempoRestante) { this.tempoRestante = tempoRestante; }
    public void setValorSorteioAtual(double valorSorteioAtual) { this.valorSorteioAtual = valorSorteioAtual; }
    public void setFinalizada(boolean finalizada) { this.finalizada = finalizada; }
    public void suspender(boolean suspensa) { this.suspensa = suspensa; }
    public void setEnvolvidaEmSorteio(boolean envolvidaEmSorteio) { this.envolvidaEmSorteio = envolvidaEmSorteio; }
    public void setCor(String cor) { this.cor = cor; }
    public void adicionarEvento(Evento evento) { this.eventos.add(evento); }

    // Método para o SOMP gravar o que aconteceu neste tick
    public void registrarEstado(int tempoAtual, Estado estado, int cpuId) {

        if (this.envolvidaEmSorteio) {
            System.out.println("Salvando histórico: A T" + this.id + " TEM sorteio no tick " + tempoAtual);
        }
        
        // Preenche buracos se o tempo der saltos (segurança)
        while (historico.size() <= tempoAtual) {
            historico.add(new TickSnapshot(Estado.NaoCriada, -1, false));
        }
        // Grava o estado atual e se houve sorteio
        historico.set(tempoAtual, new TickSnapshot(estado, cpuId, this.envolvidaEmSorteio));
        
        // Reseta a flag para o próximo tick
        this.envolvidaEmSorteio = false;
    }

    public TickSnapshot getRegistroNoTempo(int tempo) {
        if (tempo >= 0 && tempo < historico.size())
            return historico.get(tempo);
        return new TickSnapshot(Estado.NaoCriada, -1, false); // Retorna um estado padrão se o tempo for inválido
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

    public Evento popEvento() {  // Remove ultimo evento, como se fosse uma stack
        if (eventos.isEmpty())
            return null;
        int idx = eventos.size() - 1;
        return eventos.remove(idx);
    }

    public TickSnapshot popHistorico() {
        if (historico.isEmpty())
            return null;
        int idx = historico.size() - 1;
        return historico.remove(idx);
    }

    public TickSnapshot peekHistorico() {
        if (historico.isEmpty())
            return null;
        int idx = historico.size() - 1;
        return historico.get(idx);
    }

    public void stepBack() {
        TickSnapshot desfeito = popHistorico(); // Tira o registro do estado futuro que estamos desfazendo
        
        if (desfeito != null) {
            if (desfeito.estado == Estado.Executando)
                ++tempoRestante; // Se ela rodou nesse tick, devolvemos o tempo de execução
            
            if (tempoRestante > 0)  // Se recuperou o tempoRestante, garantimos que ela reviveu (não está mais finalizada)
                finalizada = false;
        }
    }
}
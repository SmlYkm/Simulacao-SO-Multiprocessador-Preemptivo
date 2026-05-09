package simulador;

import java.util.ArrayList;
import java.util.List;
// import java.awt.Color; 

public class Tarefa {

    private int id;
    private int prioridade;
    private int tempoExecucao;
    private int tempoRestante;
    private int tempoChegada;
    private boolean finalizada;
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

        public TickSnapshot(Estado estado, int cpuId) {
            this.estado = estado;
            this.cpuId  = cpuId;
        }
    }


    public Tarefa(int id, String cor, int tempoChegada, int tempoExecucao, int prioridade, List<Evento> lista_eventos) {
        this.id = id;
        this.tempoChegada = tempoChegada;
        this.tempoExecucao = tempoExecucao;
        this.tempoRestante = tempoExecucao;
        this.prioridade = prioridade;
        this.finalizada = false;
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
    public boolean isFinalizada() { return finalizada; }
    public String getCor() { return cor; }
    public List<Evento> getEventos() { return eventos; }

    public void setPrioridade(int prioridade) { this.prioridade = prioridade; }
    public void setTempoRestante(int tempoRestante) { this.tempoRestante = tempoRestante; }
    public void setFinalizada(boolean finalizada) { this.finalizada = finalizada; }
    public void setCor(String cor) { this.cor = cor; }
    public void adicionarEvento(Evento evento) { this.eventos.add(evento); }

    // Método para o SOMP gravar o que aconteceu neste tick
    public void registrarEstado(int tempoAtual, Estado estado, int cpuId) {
        // Preenche buracos se o tempo der saltos (segurança)
        while (historico.size() <= tempoAtual) {
            historico.add(
                new TickSnapshot(Estado.NaoCriada, -1)
            );
        }
        historico.set(tempoAtual, new TickSnapshot(estado, cpuId));
    }

    public TickSnapshot getRegistroNoTempo(int tempo) {
        if (tempo >= 0 && tempo < historico.size())
            return historico.get(tempo);
        return new TickSnapshot(Estado.NaoCriada, -1);
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

}
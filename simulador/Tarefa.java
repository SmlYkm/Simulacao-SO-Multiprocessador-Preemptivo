package simulador;

import java.util.ArrayList;
import java.util.List;



public class Tarefa {

    private int id;
    private int prioridade;
    private int tempoExecucao;
    private int tempoRestante;
    private int tempoChegada;
    private boolean finalizada;
    private String cor;

    private ArrayList<Evento> eventos;

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
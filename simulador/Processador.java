package simulador;

public class Processador {
    private int id;
    private Tarefa tarefaAtual; // Se for null, o processador está "desligado"
    private int tempoDesligado; // Acumula os ticks em que ficou ocioso
    private int ticksNoQuantum; // Contador de ticks para controle de quantum
    
    public Processador(int id) {
        this.id = id;
        this.tempoDesligado = 0;
        this.tarefaAtual = null;
    }
    
    // Getters e setters básicos...
    public int getId() { return id; }
    public Tarefa getTarefaAtual() { return tarefaAtual; }
    public int getTempoDesligado() { return tempoDesligado; }
    public int getTicksNoQuantum() { return ticksNoQuantum; }
    
    
    public void setTarefaAtual(Tarefa tarefaAtual) {
        if (this.tarefaAtual != tarefaAtual) {
            this.ticksNoQuantum = 0; 
        }
        this.tarefaAtual = tarefaAtual;
    }
    
    public void setTempoDesligado(int tempoDesligado) { this.tempoDesligado = tempoDesligado; }
    public void resetTicksNoQuantum() { this.ticksNoQuantum = 0; }


    // Executa uma unidade de tempo
    public void executar() {
        if (tarefaAtual != null) {
            tarefaAtual.executar(1);
            ++ticksNoQuantum;
            if (tarefaAtual.isFinalizada()) {   // Seta para null no caso de ja terminar a tarefa
                tarefaAtual = null;
                ticksNoQuantum = 0;
            }
        } else {
            ++tempoDesligado;
        }
    }
    
    public boolean idle() {
        return tarefaAtual == null;
    }
}

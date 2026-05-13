package simulador;

import java.util.ArrayList;
import java.util.List;

public class Processador {
    private int id;
    private Tarefa tarefaAtual; // Se for null, o processador está "desligado"
    private int ticksNoQuantum; // Contador de ticks para controle de quantum
    private List<Boolean> historicoOcioso = new ArrayList<>();// Histórico para controle de ociosidade (true = ocioso, false = ocupado)
    
    public Processador(int id) {
        this.id = id;
        this.tarefaAtual = null;
        this.ticksNoQuantum = 0;
        this.historicoOcioso = new ArrayList<>();
    }
    
    // Getters e setters básicos...
    public int getId() { return id; }
    public Tarefa getTarefaAtual() { return tarefaAtual; }
    public int getTicksNoQuantum() { return ticksNoQuantum; }
    
    
    public void setTarefaAtual(Tarefa tarefaAtual) {
        if (this.tarefaAtual != tarefaAtual) {
            this.ticksNoQuantum = 0; 
        }
        this.tarefaAtual = tarefaAtual;
    }
    
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

        }
    }
    
    public boolean idle() {
        return tarefaAtual == null;
    }

    public void stepBack() {
        if (tarefaAtual != null) {
            Tarefa.TickSnapshot atual = tarefaAtual.popHistorico();
            // Tarefa.TickSnapshot anterior = tarefaAtual.peekHistorico(); acho q n precisa  
            tarefaAtual.popEvento();  // Por consistencia
            switch (atual.estado) {  // TODO
            case NaoCriada:
                break;

            case Esperando:
                break;

            case Executando:
                break;

            case Suspenso:
                break;

            case Finalizado:
                break;
            
            default:
                break;
            }
        }
    
    }
    public void registrarOciosidade() {
            // idle() deve ser o seu método que retorna true se a tarefaAtual for null
            historicoOcioso.add(this.idle()); 
    }
    
    public void apagarRegistroOciosidade(int tempo) {
        // Remove a memória do futuro se voltarmos no tempo
        if (tempo >= 0 && tempo < historicoOcioso.size()) {
            historicoOcioso.remove(tempo);
        }
    }
    
    public int getTempoOciosoTotal() {
        // Conta quantas vezes a CPU ficou ociosa até o momento atual
        int total = 0;
        for (Boolean ocioso : historicoOcioso) {
            if (ocioso) total++;
        }
        return total;
    }
}

package simulador;

import java.util.ArrayList;

public abstract class Escalonador {
    protected ArrayList<Tarefa> tarefas;

    public Escalonador () {
        tarefas = new ArrayList<>();
    }
    
    // Coloca a tarefa no final da fila
    public void adicionarTarefa(Tarefa tarefa) {
        tarefas.add(tarefa);
    }

    // Limpa a fila atual para que o SOMP a reconstrua do zero a cada tick
    public void limparFila() {
        this.tarefas.clear();
    }

    // Método abstrato: as classes filhas (SRTF, PRIOP) definem como ordenar a lista
    public abstract void prepararFila(Processador[] cpus, int valorDoQuantum);

    // O SOMP usa este método para ir buscar as próximas tarefas mais prioritárias
    public Tarefa obterProximaTarefa() {
        if (!tarefas.isEmpty()) {
            return tarefas.remove(0); 
        }
        return null;
    }

    // Remove da fila tarefas que já terminaram
    protected void removerTarefasFinalizadas() {
        tarefas.removeIf(Tarefa::isFinalizada);
    }

    // Sorteia um valor aleatório para todas as tarefas na fila (para o Desempate)
    protected void sortearValores() {
        for (Tarefa t : tarefas) {
            t.setValorSorteioAtual(Math.random());
        }
    }

    // A lógica matemática e central de desempate
    protected int desempate(Tarefa t1, Tarefa t2, Processador[] cpus) {
        // Desempate 1: A tarefa já estava em execução imediatamente antes
        boolean t1EstavaExecutando = estavaExecutando(t1, cpus);
        boolean t2EstavaExecutando = estavaExecutando(t2, cpus);
        if (t1EstavaExecutando && !t2EstavaExecutando) return -1; // t1 ganha
        if (!t1EstavaExecutando && t2EstavaExecutando) return 1;  // t2 ganha

        // Desempate 2: Instante de ingresso 
        int compareIngresso = Integer.compare(t1.getTempoChegada(), t2.getTempoChegada());
        if (compareIngresso != 0) return compareIngresso;

        // Desempate 3: Duração da tarefa
        int compareDuracao = Integer.compare(t1.getTempoExecucao(), t2.getTempoExecucao());
        if (compareDuracao != 0) return compareDuracao;

        // Desempate 4: Sorteio
        t1.setEnvolvidaEmSorteio(true);
        t2.setEnvolvidaEmSorteio(true); 
        return Double.compare(t1.getValorSorteioAtual(), t2.getValorSorteioAtual());
    }

    // Metodo auxiliar para o Desempate 1
    protected boolean estavaExecutando(Tarefa tarefa, Processador[] cpus) { 
        for (Processador cpu : cpus) {
            if (cpu.getTarefaAtual() != null && cpu.getTarefaAtual().getId() == tarefa.getId()) {
                return true;
            }
        }
        return false;
    }
}
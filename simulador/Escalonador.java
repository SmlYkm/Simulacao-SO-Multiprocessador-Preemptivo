package simulador;

import java.util.ArrayList;
import java.util.List;

public abstract class Escalonador {
    protected ArrayList<Tarefa> tarefas;

    public Escalonador () {
        tarefas = new ArrayList<>();
    }
    
    public void adicionarTarefa(Tarefa tarefa) {  // Coloca tarefa no final da fila
        tarefas.add(tarefa);
    }

    // Executa 1 tick, recebe lista de processadores e ve quem fica com qual tarefa
    public abstract void executar(Processador[] cpus, int valorDoQuantum);
    
    public void stepBack() {  // Volta 1 tick no tempo
        // TODO
        
    }

    protected void removerTarefasFinzalidas() {
        tarefas.removeIf(Tarefa::isFinalizada);
    }

    protected void sortearValores() {
        for (Tarefa t : tarefas) {
            t.setValorSorteioAtual(Math.random());
        }
    }

    protected int desempate(Tarefa t1, Tarefa t2, Processador[] cpus) {
        // Desempate 1: A tarefa já estava executando imediatamente antes?
        boolean t1EstavaExecutando = estavaExecutando(t1, cpus);
        boolean t2EstavaExecutando = estavaExecutando(t2, cpus);
        if (t1EstavaExecutando && !t2EstavaExecutando) 
            return -1; // t1 ganha
        if (!t1EstavaExecutando && t2EstavaExecutando) 
            return 1;  // t2 ganha

        // Desempate 2: Instante de ingresso (quem chegou antes)
        int compareIngresso = Integer.compare(t1.getTempoChegada(), t2.getTempoChegada());
        if (compareIngresso != 0)
            return compareIngresso;

        // Desempate 3: Duração da tarefa (menor duração ganha)
        int compareDuracao = Integer.compare(t1.getTempoExecucao(), t2.getTempoExecucao());
        if (compareDuracao != 0)
            return compareDuracao;

        // Desempate 4: Sorteio 
        t1.setEnvolvidaEmSorteio(true);
        t2.setEnvolvidaEmSorteio(true); 
        
        return Double.compare(t1.getValorSorteioAtual(), t2.getValorSorteioAtual());
    }

    private boolean estavaExecutando(Tarefa tarefa, Processador[] cpus) {  // Método auxiliar para o Desempate 1
        for (Processador cpu : cpus) {
            if (cpu.getTarefaAtual() != null && cpu.getTarefaAtual().getId() == tarefa.getId()) {
                return true;
            }
        }
        return false;
    }

    protected void distribuirTarefas(Processador[] cpus) {
        // A) Separamos as tarefas que vão ganhar o direito de executar
        List<Tarefa> tarefasParaExecutar = new ArrayList<>();
        while (!tarefas.isEmpty() && tarefasParaExecutar.size() < cpus.length) {
            tarefasParaExecutar.add(tarefas.remove(0));
        }

        // B) Passo 1 da Distribuição: Afinidade. 
        // Se a tarefa já estava nessa CPU e continua tendo direito de executar, ela permanece
        
        for (Processador cpu : cpus) {
            Tarefa tarefaAntiga = cpu.getTarefaAtual();
            
            
            if (tarefaAntiga != null && tarefasParaExecutar.contains(tarefaAntiga)) {
                // A tarefa já estava e vai continuar rodando 
                // Removemos ela da lista de pendentes
                tarefasParaExecutar.remove(tarefaAntiga);
            } else {
                // Se a tarefa não tem mais prioridade para rodar (ou terminou), limpamos a CPU
                cpu.setTarefaAtual(null);
            }
        }

        // C) Passo 2 da Distribuição: Preencher os espaços vazios.
        // Colocamos as tarefas restantes (novas ou que subiram de prioridade) nas CPUs livres.
        for (Processador cpu : cpus) {
            if (cpu.idle() && !tarefasParaExecutar.isEmpty()) {
                cpu.setTarefaAtual(tarefasParaExecutar.remove(0));
            }
        }
    }

    protected void distribuirTarefasPreemptivo(Processador[] cpus) {
        
        List<Tarefa> tarefasParaExecutar = new ArrayList<>();  // Tarefas que vão executar neste tick
        for (Tarefa t : tarefas) {
            if (!t.isFinalizada() && !t.isSuspensa())
                tarefasParaExecutar.add(t);
            if (tarefasParaExecutar.size() == cpus.length) 
                break;  // Já selecionamos tarefas suficientes para as CPUs
        }
        
        for (Processador cpu : cpus) {  // Distribuição por afinidade e preempção
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual == null)  // A tarefa que tava la ja foi finalizada
                continue;             // ou ainda não foi atribuida uma tarefa
            
            if (!tarefasParaExecutar.contains(tarefaAtual)) {
                cpu.setTarefaAtual(null);  // Preempção, perde a cpu
            } else {
                tarefasParaExecutar.remove(tarefaAtual);  // Continua executando
            }                                            
        }

        for (Processador cpu : cpus) {  // Tarefas prioritárias que sobraram vão pra cpus livres
            if (cpu.idle() && !tarefasParaExecutar.isEmpty())
                cpu.setTarefaAtual(tarefasParaExecutar.remove(0));  // Inicio da fila -> mais prioritária
        }
    }
}

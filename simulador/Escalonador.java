package simulador;

import java.util.ArrayList;
// import simulador.Tarefa;
import java.util.List;

public abstract class Escalonador {
    protected ArrayList<Tarefa> tarefas;

    public Escalonador () {
        tarefas = new ArrayList<>();
    }
    
    // Coloca tarefa no final da fila
    public void adicionarTarefa(Tarefa tarefa) {
        tarefas.add(tarefa);
    }

    // Executa 1 tick, recebe lista de processadores e ve quem fica com qual tarefa
    public abstract void executar(Processador[] cpus, int valorDoQuantum);

    protected void removerTarefasFinzalidas() {
        tarefas.removeIf(Tarefa::isFinalizada);
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

    protected void distribuirTarefasPreemptivo(Processador[] cpus) {
        // A) Separamos as tarefas que vão ganhar o direito de executar neste tick.
        // Pegamos apenas as N primeiras (N = quantidade de CPUs), MAS SEM REMOVER da lista original.
        List<Tarefa> tarefasParaExecutar = new ArrayList<>();
        int limite = Math.min(tarefas.size(), cpus.length);
        
        for (int i = 0; i < limite; i++) {
            tarefasParaExecutar.add(tarefas.get(i));
        }
        
        // B) Passo 1 da Distribuição: Afinidade e PREEMPÇÃO.
        for (Processador cpu : cpus) {
            Tarefa tarefaAtual = cpu.getTarefaAtual();
                        
            if (tarefaAtual != null) {
                if (tarefasParaExecutar.contains(tarefaAtual)) {
                    // AFINIDADE: A tarefa já estava na CPU e continua tendo prioridade alta o suficiente.
                    // Removemos da nossa listinha TEMPORÁRIA para saber que ela já foi alocada.
                    tarefasParaExecutar.remove(tarefaAtual);
                } else {
                    // PREEMPÇÃO: A tarefa que estava rodando não está mais no "Top N" de prioridade.
                    // Ela perde a CPU! Como não a removemos da lista 'tarefas' global, ela 
                    // continuará na fila de prontos para tentar rodar no futuro.
                    cpu.setTarefaAtual(null);
                }
            }
        }

        // C) Passo 2 da Distribuição: Preencher os espaços vazios.
        // Pegamos as tarefas prioritárias que sobraram e colocamos nas CPUs que ficaram livres.
        for (Processador cpu : cpus) {
            if (cpu.idle() && !tarefasParaExecutar.isEmpty()) {
                // Aqui sim usamos remove(0), mas estamos removendo da listinha TEMPORÁRIA,
                // não da lista global 'tarefas'.
                cpu.setTarefaAtual(tarefasParaExecutar.remove(0));
            }
        }
    }
}

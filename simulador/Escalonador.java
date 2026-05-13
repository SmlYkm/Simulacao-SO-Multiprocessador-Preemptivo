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

    protected void distribuirTarefasPreemptivo(Processador[] cpus) {
        
        List<Tarefa> tarefasParaExecutar = new ArrayList<>();  // Tarefas que vão executar neste tick
        for (Tarefa t : tarefas) {                             // Partimos do presupostoque tarefas esta sortado
            if (!t.isFinalizada())
                tarefasParaExecutar.add(t);                    // Há a possibilidade de t estar suspenso, lidamos com isso depois
            if (tarefasParaExecutar.size() == cpus.length) 
                break;                                         // Ja foi as n cpus
        }
        
        for (Processador cpu : cpus) {  // Distribuição por afinidade e preempção
            Tarefa tarefaAtual = cpu.getTarefaAtual();
            if (tarefaAtual == null)  // A tarefa que tava la ja foi finalizada
                continue;             // ou ainda não foi atribuida uma tarefa
            
            if (tarefasParaExecutar.contains(tarefaAtual)) {
                tarefasParaExecutar.remove(tarefaAtual);  // Afinidade -> tarefa já estava na CPU e continua tendo prioridade alta o suficiente
            } else if (tarefaAtual.isSuspensa()) {
                continue;  // Tarefa é mais prioritária mas está suspensa
            } else {
                cpu.getTarefaAtual().suspender(true);  // Suspende tarefa porque a nova é mais prioritária
                cpu.setTarefaAtual(null);  // Preempção, perde a cpu, it's over
            }                                            
        }

        for (Processador cpu : cpus) {  // Tarefas prioritárias que sobraram vão pra cpus livres
            if (cpu.idle() && !tarefasParaExecutar.isEmpty()) {
                Tarefa t = tarefasParaExecutar.remove(0);
                if (t.isSuspensa())     // É possivel que a tarefa esteja suspensa
                    t.suspender(false);
                cpu.setTarefaAtual(t);  // Inicio da fila -> mais prioritária
            }
        }
    }
}

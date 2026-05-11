package simulador;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EscalonadorSRTF extends Escalonador {

    @Override
    public void executar(Processador[] cpus, int valorDoQuantum) {
        
        // 1. Devolver todas as tarefas atualmente nas CPUs para a lista geral do escalonador
        // Isso é necessário para avaliarmos TODAS as tarefas juntas (prontas + executando)
        for (Processador cpu : cpus) {
            Tarefa t = cpu.getTarefaAtual();
            if (t != null && !t.isFinalizada()) {
                //Se a tarefa for maior que o quantum, ela é expulsa da CPU e volta para a lista de tarefas do escalonador
                if (cpu.getTicksNoQuantum() >= valorDoQuantum) {
                    cpu.setTarefaAtual(null); // Expulsa a tarefa
                    cpu.resetTicksNoQuantum();
                }
                if (!tarefas.contains(t)) {
                    tarefas.add(t);
                }
            }
        }

        // 2. Dar um valor aleatório novo para cada tarefa neste tick
        for (Tarefa t : tarefas) {
            t.setValorSorteioAtual(Math.random());
        }

        // 3. Ordenar a lista de tarefas aplicando as regras do SRTF e os desempates
        tarefas.sort((t1, t2) -> {
            
            // Regra principal do SRTF: Menor tempo restante
            int compareTempo = Integer.compare(t1.getTempoRestante(), t2.getTempoRestante());
            if (compareTempo != 0) {
                return compareTempo;
            }

            // --- TRATAMENTO DE EMPATES ---
            
            // Desempate 1: A tarefa já estava executando imediatamente antes?
            boolean t1EstavaExecutando = estavaExecutando(t1, cpus);
            boolean t2EstavaExecutando = estavaExecutando(t2, cpus);
            if (t1EstavaExecutando && !t2EstavaExecutando) return -1; // t1 ganha
            if (!t1EstavaExecutando && t2EstavaExecutando) return 1;  // t2 ganha

            // Desempate 2: Instante de ingresso (quem chegou antes)
            int compareIngresso = Integer.compare(t1.getTempoChegada(), t2.getTempoChegada());
            if (compareIngresso != 0) {
                return compareIngresso;
            }

            // Desempate 3: Duração da tarefa (menor duração ganha)
            int compareDuracao = Integer.compare(t1.getTempoExecucao(), t2.getTempoExecucao());
            if (compareDuracao != 0) {
                return compareDuracao;
            }

            // Desempate 4: Sorteio 
            t1.setEnvolvidaEmSorteio(true);
            t2.setEnvolvidaEmSorteio(true); 
            
            return Double.compare(t1.getValorSorteioAtual(), t2.getValorSorteioAtual());
        });

        // 4. Distribuir as tarefas ordenadas pelas CPUs seguindo as regras de afinidade e preenchimento
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

    // Método auxiliar para o Desempate 1
    private boolean estavaExecutando(Tarefa tarefa, Processador[] cpus) {
        for (Processador cpu : cpus) {
            if (cpu.getTarefaAtual() != null && cpu.getTarefaAtual().getId() == tarefa.getId()) {
                return true;
            }
        }
        return false;
    }
}
package simulador;

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
        sortearValores();

        // 3. Ordenar a lista de tarefas aplicando as regras do SRTF e os desempates
        tarefas.sort((t1, t2) -> {
            
            // Regra principal do SRTF: Menor tempo restante
            int compareTempo = Integer.compare(t1.getTempoRestante(), t2.getTempoRestante());
            if (compareTempo != 0) {
                return compareTempo;
            }

            return desempate(t1, t2, cpus);
        });
        // 4. Distribuir as tarefas ordenadas pelas CPUs seguindo as regras de afinidade e preenchimento
        distribuirTarefas(cpus);

    }
}
package simulador;

public class EscalonadorSRTF extends Escalonador {

    @Override
    public void prepararFila(Processador[] cpus, int valorDoQuantum) {
        // 1. Dá um número aleatório novo para cada tarefa neste tick (usado no Desempate 4)
        sortearValores(); 

        // 2. Ordena a lista
        tarefas.sort((t1, t2) -> {
            // Regra principal do SRTF: Menor tempo restante ganha
            int compareTempo = Integer.compare(t1.getTempoRestante(), t2.getTempoRestante());
            if (compareTempo != 0) {
                return compareTempo;
            }

            // Se houver empate no tempo restante, a classe mãe resolve os desempates!
            return desempate(t1, t2, cpus);
        });
    }
}
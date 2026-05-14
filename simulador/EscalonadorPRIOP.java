package simulador;

public class EscalonadorPRIOP extends Escalonador {

    @Override
    public void prepararFila(Processador[] cpus, int valorDoQuantum) {
        // Sorteia novos valores aleatórios para o caso de precisar ir a sorteio
        sortearValores();

        // Ordena a lista
        tarefas.sort((t1, t2) -> {
            //Comparando prioridades
            int comparePrioridade = Integer.compare(t2.getPrioridade(), t1.getPrioridade());
            if (comparePrioridade != 0) {
                return comparePrioridade;
            }

            // Se houver empate na prioridade chama o desempate da classe abstrata
            return desempate(t1, t2, cpus);
        });
    }
}
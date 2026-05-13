package simulador;

public class EscalonadorPRIOP extends Escalonador {

    @Override
    public void prepararFila(Processador[] cpus, int valorDoQuantum) {
        // 1. Sorteia novos valores aleatórios para o caso de precisar ir a sorteio
        sortearValores();

        // 2. Ordena a lista
        tarefas.sort((t1, t2) -> {
            // Regra principal do PRIOP: Maior prioridade ganha (T2 compara com T1 para ser decrescente)
            int comparePrioridade = Integer.compare(t2.getPrioridade(), t1.getPrioridade());
            if (comparePrioridade != 0) {
                return comparePrioridade;
            }

            // Se houver empate na prioridade, a classe mãe resolve os desempates!
            return desempate(t1, t2, cpus);
        });
    }
}
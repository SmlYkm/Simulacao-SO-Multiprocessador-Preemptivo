package simulador;

public class EscalonadorPRIOP extends Escalonador {

    @Override
    public void prepararFila(Processador[] cpus) {
        // Sorteia novos valores aleatórios para o caso de precisar ir a sorteio
        sortearValores();

        // Ordena a lista
        tarefas.sort((t1, t2) -> {
            //Comparando prioridadeEstaticaEstaticas
            int compareprioridadeEstatica = Integer.compare(t2.getprioridadeEstatica(), t1.getprioridadeEstatica());
            if (compareprioridadeEstatica != 0) {
                return compareprioridadeEstatica;
            }

            // Se houver empate na prioridadeEstatica chama o desempate da classe abstrata
            return desempate(t1, t2, cpus);
        });
    }
}
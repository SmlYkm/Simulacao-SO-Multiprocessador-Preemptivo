package simulador;

public class EscalonadorPRIOPENV extends Escalonador {
    int alpha; // Fator de envelhecimento

    EscalonadorPRIOPENV(int alpha) {
        this.alpha = alpha;
    }

    @Override
    public void prepararFila(Processador[] cpus, int valorDoQuantum) {
        // Sorteia novos valores aleatórios para o caso de precisar ir a sorteio
        sortearValores();

        for(Tarefa t : tarefas) {
            t.envelhecer(alpha); // Envelhece cada tarefa na fila
        }

        // Ordena a lista
        tarefas.sort((t1, t2) -> {
            //Comparando prioridadeDinamica
            int compareprioridadeDinamica = Integer.compare(t2.getprioridadeDinamica(), t1.getprioridadeDinamica());
            if (compareprioridadeDinamica != 0) {
                return compareprioridadeDinamica;
            }

            int compareprioridadeEstatica = Integer.compare(t2.getprioridadeEstatica(), t1.getprioridadeEstatica());
            if (compareprioridadeEstatica != 0) {
                return compareprioridadeEstatica;
            }
            // Se houver empate na prioridadeDinamica chama o desempate da classe abstrata
            return desempate(t1, t2, cpus);
        });
    }
}
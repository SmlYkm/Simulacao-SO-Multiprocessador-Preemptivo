package simulador;

public class EscalonadorSRTF extends Escalonador {

    @Override
    public void prepararFila(Processador[] cpus) {
        // Dá um número aleatório novo para cada tarefa neste tick usado pelo sorteio
        sortearValores(); 

        // Ordena a lista
        tarefas.sort((t1, t2) -> {
            // Compara qual tem menor tempo
            int compareTempo = Integer.compare(t1.getTempoRestante(), t2.getTempoRestante());
            if (compareTempo != 0) {
                return compareTempo;
            }

            // Se houver empate no tempo restante vai no metodo de desempate da classe abstrata
            return desempate(t1, t2, cpus);
        });
    }
}
package simulador;

public class EscalonadorPRIOP extends Escalonador {
    
    @Override
    public void executar(Processador[] cpus, int valorDoQuantum) {
        removerTarefasFinzalidas();
        sortearValores();  // Usado no desempate
        tarefas.sort((t1, t2) -> {
            int diff = t2.getPrioridade() - t1.getPrioridade();
            if (diff != 0)  // Não houve empate
                return diff;
            return desempate(t1, t2, cpus);
        });

        distribuirTarefasPreemptivo(cpus);
    }
}
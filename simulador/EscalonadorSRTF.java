package simulador;

public class EscalonadorSRTF extends Escalonador {

    // Placeholder, só pra testar por enquanto 
    public void executar(Processador[] cpus) {
        for (Processador cpu : cpus) {
            if (cpu.idle() && !tarefas.isEmpty()) {
                Tarefa tempTarefa = tarefas.removeFirst();
                cpu.setTarefaAtual(tempTarefa);
            }
        }
    }
}
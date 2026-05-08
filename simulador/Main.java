package simulador;

public class Main {
    public static void main(String[] args) {
        // Inicializa a interface principal do simulador
        Window window = new Window("Simulador de Escalonamento MP");

        // Cria algumas tarefas de teste para visualizar no gráfico
        // Construtor: id, prioridade, tempoExecucao, tempoChegada, cor
        Tarefa t0 = new Tarefa(0, 1, 5, 0, "RED");
        Tarefa t1 = new Tarefa(1, 2, 8, 2, "BLUE");
        Tarefa t2 = new Tarefa(2, 1, 4, 6, "GREEN");

        // Adiciona as tarefas à janela
        window.addTask(t0);
        window.addTask(t1);
        window.addTask(t2);

        // Exibe a janela
        window.showWindow();
    }
}
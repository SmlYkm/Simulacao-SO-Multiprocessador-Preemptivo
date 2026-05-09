package simulador;

public class Main {
    public static void main(String[] args) {
        
        // 1. O Leitor lê o ficheiro txt e constrói o SOMP inteiro
        // (Certifica-te que crias um ficheiro chamado "config.txt" na pasta do teu projeto)
        SOMP sistema = LeitorConfig.carregarSimulacao("config.txt");

        if (sistema != null) {
            // 2. Inicializa a interface principal
            Window window = new Window("Simulador de Escalonamento MP");

            // 3. Pega nas tarefas que o Leitor colocou no SOMP e envia para a Janela
            for (Tarefa t : sistema.getListaTarefasGeral()) {
                window.addTask(t);
            }

            // Exibe a janela
            window.showWindow();
            
            // sistema.executar(); // Mais tarde irás chamar isto para iniciar a simulação real
        } else {
            System.out.println("Falha ao inicializar o sistema. Verifica o ficheiro de configuração.");
        }
    }
}
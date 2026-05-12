package simulador;

public class Main {
    public static void main(String[] args) {
        
        SOMP sistema = LeitorConfig.carregarSimulacao("config.txt");

        if (sistema != null) {
            // A Window agora NÃO precisa mais receber o 'sistema' (SOMP) no construtor.
            // Ela vai se comunicar com o sistema através do Controller.
            Window window = new Window("Simulador de Escalonamento MP");

            // Instancia o Controller passando o Model (sistema) e a View (window)
            SimulationController controller = new SimulationController(sistema, window);
            
            // Injeta o Controller na View para que os botões funcionem
            window.setController(controller); 

            // Exibe a janela
            window.showWindow();
            
        } else {
            System.out.println("Falha ao inicializar o sistema. Verifica o ficheiro de configuração.");
        }
    }
}
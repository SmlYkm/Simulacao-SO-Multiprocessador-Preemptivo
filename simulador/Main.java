package simulador;

import javax.swing.UIManager;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName()  // Mantém o visual nativo do sistema operacional
            );
        } catch (Exception e) {
            // Ignora e usa o padrão do Java
        }

        Window window        = new Window("Simulador de Escalonamento MP");  // Inicia a janela de forma independente
        String arquivoPadrao = "config.txt";                                        // Tenta carregar o arquivo padrão dinamicamente  
        File   file          = new File(arquivoPadrao);
        
        if (file.exists()) {
            System.out.println("Arquivo padrão encontrado. Carregando...");
            window.carregarSimulacaoPorCaminho(arquivoPadrao);
        } else {
            System.out.println("Arquivo padrão 'config.txt' não encontrado. Iniciando vazio.");
        }

        window.showWindow();
    }
}
package simulador;

import java.io.BufferedReader;
import java.io.FileReader;
import javax.swing.JOptionPane;

public class LeitorConfig {

     static SOMP carregarSimulacao(String caminhoFicheiro) {
        SOMP sistema = null;

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoFicheiro))) {
            String linha;

            // Lê a primeira linha com as configurações
            if ((linha = br.readLine()) != null) {
                // Divide a linha pelos pontos e vírgulas
                String[] config = linha.split(";"); 
                
                String algoritmo = config[0].replace("\uFEFF", "").trim(); // Remove Espaços e caracteres invisíveis 
                int quantum = Integer.parseInt(config[1].trim());
                int qtdeCpus = Integer.parseInt(config[2].trim());

                // Caso tenha menos que dois processadores gera exceção
                if (qtdeCpus < 2) {
                    // Avisa o usuário especificamente sobre este erro
                    JOptionPane.showMessageDialog(null, 
                        "O arquivo de configuração solicitou " + qtdeCpus + " processador(es).\n" +
                        "O mínimo exigido pelo projeto é 2!\n\n" +
                        "A simulação será iniciada no Modo Default.", 
                        "Quantidade de CPUs Inválida", 
                        JOptionPane.ERROR_MESSAGE);
                    
                    // Aborta a leitura e vai pra configuração default
                    throw new Exception("Quantidade de CPUs menor que o mínimo exigido.");
                }

                Escalonador escalonador = null;
                if (algoritmo.equalsIgnoreCase("SRTF")) {
                    escalonador = new EscalonadorSRTF();
                } else if (algoritmo.equalsIgnoreCase("PRIOP")) {
                    escalonador = new EscalonadorPRIOP();        
                }
                else
                {
                    escalonador = new EscalonadorSRTF(); // Padrão para evitar falhas
                }

                sistema = new SOMP(escalonador, qtdeCpus, quantum);
            }

            // Lê as tarefas e eventos.
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue; // Ignora linhas em branco

                String[] dados = linha.split(";");
                
                int id = Integer.parseInt(dados[0].trim());
                String cor = dados[1].trim();
                int ingresso = Integer.parseInt(dados[2].trim());
                int duracao = Integer.parseInt(dados[3].trim());
                int prioridade = Integer.parseInt(dados[4].trim());

                Tarefa novaTarefa = new Tarefa(id,cor, ingresso, duracao, prioridade, null); // Passa null para a lista de eventos por enquanto

                // Trata lista de eventos (não está sendo usada ainda).
                if (dados.length > 5 && !dados[5].trim().isEmpty()) {
                    String[] listaEventos = dados[5].split(",");
                    for (String strEvento : listaEventos) {
                        strEvento = strEvento.trim();
                        // Aqui vai instanciar os eventos
                        // Exemplo se for um evento de E/S
                        // novaTarefa.adicionarEvento(new EventoES(...));
                    }
                }

                // Adiciona a tarefa criada ao sistema
                sistema.adicionarTarefa(novaTarefa); 
            }

        } catch (Exception e) {
            System.err.println("Arquivo config.txt não encontrado ou inválido. Carregando padrões do sistema!");
            // Aviso para o usuário
            JOptionPane.showMessageDialog(null, 
                "Arquivo 'config.txt' não encontrado ou inválido.\nO sistema iniciará com configurações padrão (Modo Default).", 
                "Aviso de Configuração", 
                JOptionPane.WARNING_MESSAGE);

            // Configuraçao default para caso a config.txt dê erro
            sistema = new SOMP(new EscalonadorSRTF(), 2, 2); // 2 CPUs, Quantum 2
            sistema.adicionarTarefa(new Tarefa(1, "FF0000", 0, 5, 1, null)); // Tarefa Padrão
            sistema.adicionarTarefa(new Tarefa(2, "0000FF", 2, 3, 2, null)); // Tarefa Padrão
        }

        return sistema;
    }
}
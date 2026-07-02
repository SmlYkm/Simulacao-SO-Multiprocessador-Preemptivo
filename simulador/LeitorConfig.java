package simulador;

import java.io.BufferedReader;
import java.io.FileReader;
import javax.swing.JOptionPane;

public class LeitorConfig {

     static SOMP carregarSimulacao(String caminhoFicheiro) {
        SOMP sistema = null;

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoFicheiro))) {
            String linha;

            if ((linha = br.readLine()) != null) {
                String[] config    = linha.split(";"); 
                String   algoritmo = config[0].replace("\uFEFF", "").trim(); 
                Tarefa.setQuantum(
                    Integer.parseInt(config[1].trim())
                ); 
                int qtdeCpus = Integer.parseInt(config[2].trim());

                int alpha = 0; 
                if (algoritmo.equalsIgnoreCase("PRIOPENV"))
                    alpha = Integer.parseInt(config[3].trim()); 

                if (qtdeCpus < 2) {
                    JOptionPane.showMessageDialog(
                        null, 
                        "O arquivo de configuração solicitou " + qtdeCpus + " processador(es).\n" +
                        "O mínimo exigido pelo projeto é 2!\n\n" +
                        "A simulação será iniciada no Modo Default.", 
                        "Quantidade de CPUs Inválida", 
                        JOptionPane.ERROR_MESSAGE
                    );
                    throw new Exception("Quantidade de CPUs menor que o mínimo exigido.");
                }

                Escalonador escalonador = null;
                
                if (algoritmo.equalsIgnoreCase("SRTF")) {
                    escalonador = new EscalonadorSRTF();

                } else if (algoritmo.equalsIgnoreCase("PRIOP")) {
                    escalonador = new EscalonadorPRIOP();        
                
                } else if (algoritmo.equalsIgnoreCase("PRIOPENV")) {
                    escalonador = new EscalonadorPRIOPENV(alpha);
                
                } else {
                    escalonador = new EscalonadorSRTF(); 
                    JOptionPane.showMessageDialog(null, 
                        "Algoritmo inválido: " + algoritmo + "\nA simulação será iniciada no Modo Default.", 
                        "Algoritmo Inválido", JOptionPane.ERROR_MESSAGE);
                    throw new Exception("Algoritmo inválido no arquivo de configuração.");
                }

                sistema = new SOMP(escalonador, qtdeCpus);
            }

            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) 
                    continue; 

                String[] dados = linha.split(";");
                
                // Remove qualquer caractere que não seja número 
                int    id                 = Integer.parseInt(dados[0].replaceAll("\\D+", "")); 
                String cor                = dados[1].trim();
                int    ingresso           = Integer.parseInt(dados[2].trim());
                int    duracao            = Integer.parseInt(dados[3].trim());
                int    prioridadeEstatica = Integer.parseInt(dados[4].trim());

                Tarefa novaTarefa = new Tarefa(
                    id, cor, ingresso, duracao, prioridadeEstatica, null
                ); 

                for (int i = 5; i < dados.length; i++) {
                    String strEvento = dados[i].trim();
<<<<<<< HEAD
                    if (strEvento.isEmpty()) continue;
=======
                    
                    if (strEvento.isEmpty()) {
                        continue;
                    }
>>>>>>> 07c576a848852d457ce8a343478eee35566f824d

                    try {
                        if (strEvento.startsWith("I")) {  
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)[:\\-](\\d+)").matcher(strEvento);
                            if (m.find()) {
                                int inicio    = Integer.parseInt(m.group(1));
                                int duracaoIO = Math.max(1, Integer.parseInt(m.group(2)));
                                novaTarefa.adicionarEvento(new IO(inicio, duracaoIO));
                            }
                        } 
                        else if (strEvento.startsWith("ML") || strEvento.startsWith("MU")) {
<<<<<<< HEAD
                            String[] partes           = strEvento.split(":");
                            String   comando          = partes[0].trim(); 
                            int      instanteRelativo = Integer.parseInt(partes[1].trim()); 
                            boolean  isLock           = comando.startsWith("ML"); 
                            
                            // Extrai o número do Mutex (ex: "ML01" -> 1)
                            int mutexId = Integer.parseInt(comando.substring(2).trim());
                            
=======
                            String[] partes = strEvento.split(":");
                            String comando  = partes[0].trim(); 
                            int instanteRelativo = Integer.parseInt(partes[1].trim()); 
                            boolean isLock  = comando.startsWith("ML"); 
                            int mutexId     = Integer.parseInt(comando.substring(2).trim());
                            
>>>>>>> 07c576a848852d457ce8a343478eee35566f824d
                            novaTarefa.adicionarEvento(
                                new EventoMutex(instanteRelativo, mutexId, isLock)
                            );
                        }
<<<<<<< HEAD
                        
=======
>>>>>>> 07c576a848852d457ce8a343478eee35566f824d
                    } catch (Exception ex) {
                        System.err.println("Erro ao fazer o parsing do evento [" + strEvento + "] na Tarefa " + id);
                    }
                }
                
                sistema.adicionarTarefa(novaTarefa); 
            }

        } catch (Exception e) {
            System.err.println("Carregando padrões do sistema!");
            sistema = new SOMP(new EscalonadorSRTF(), 2); 
            sistema.adicionarTarefa(new Tarefa(1, "FF0000", 0, 5, 1, null)); 
            sistema.adicionarTarefa(new Tarefa(2, "0000FF", 2, 3, 2, null)); 
        
        }

        return sistema;
    }
}
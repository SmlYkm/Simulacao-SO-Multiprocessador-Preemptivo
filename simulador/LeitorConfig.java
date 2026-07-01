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
                Tarefa.setQuantum(Integer.parseInt(config[1].trim())); // Define o quantum para todas as tarefas
                int qtdeCpus = Integer.parseInt(config[2].trim());

                int alpha = 0; // Valor padrão
                if (algoritmo.equalsIgnoreCase("PRIOPENV")) {
                    alpha = Integer.parseInt(config[3].trim()); // Captura o parâmetro de envelhecimento
                }

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
                } else if (algoritmo.equalsIgnoreCase("PRIOPENV")) {
                    escalonador = new EscalonadorPRIOPENV(alpha);
                } else
                {
                    escalonador = new EscalonadorSRTF(); // Padrão para evitar falhas
                    JOptionPane.showMessageDialog(null, 
                        "O arquivo de configuração solicitou um algoritmo de escalonamento inválido: " + algoritmo + "\n\n" +
                        "A simulação será iniciada no Modo Default.", 
                        "Algoritmo de Escalonamento Inválido", 
                        JOptionPane.ERROR_MESSAGE);
                    
                    // Aborta a leitura e vai pra configuração default
                    throw new Exception("Algoritmo de escalonamento inválido no arquivo de configuração.");
                }

                sistema = new SOMP(escalonador, qtdeCpus);
            }

            // Lê as tarefas e eventos.
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue; // Ignora linhas em branco

                String[] dados = linha.split(";");
                
                int id = Integer.parseInt(dados[0].trim());
                String cor = dados[1].trim();
                int ingresso = Integer.parseInt(dados[2].trim());
                int duracao = Integer.parseInt(dados[3].trim());
                int prioridadeEstatica = Integer.parseInt(dados[4].trim());

                Tarefa novaTarefa = new Tarefa(id,cor, ingresso, duracao, prioridadeEstatica, null); // Passa null para a lista de eventos por enquanto


                // Trata lista de eventos de IO
                // Trata a lista de eventos separada por vírgulas
                if (dados.length > 5 && !dados[5].trim().isEmpty()) {
                    String[] listaEventos = dados[5].split(",");
                    

                    for (String strEvento : listaEventos) {
                        strEvento = strEvento.trim();

                        if (strEvento.startsWith("I")) { // Captura I0, IO, etc
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)[:\\-](\\d+)").matcher(strEvento);
                            if (m.find()) {
                                int inicio    = Integer.parseInt(m.group(1));
                                int duracaoIO = Integer.parseInt(m.group(2));
                                novaTarefa.adicionarEvento(new IO(inicio, duracaoIO));
                            }
                        }
                        strEvento = strEvento.trim(); // Remove espaços em branco
                        
                        try {
                            if (strEvento.contains(":")) {
                                String[] partes = strEvento.split(":");
                                String comando = partes[0].trim(); // Pega a parte antes dos ":" (Ex: "ML01" ou "I0")
                                String valores = partes[1].trim(); // Pega a parte depois dos ":" (Ex: "05" ou "05-10")

                                // 1. Processa Eventos de MUTEX (MLxx: 00 ou MUxx: 00)
                                if (comando.startsWith("ML") || comando.startsWith("MU")) {
                                    int instanteRelativo = Integer.parseInt(valores); 
                                    boolean isLock = comando.startsWith("ML"); 
                                    int mutexId = Integer.parseInt(comando.substring(2).trim());
                                    
                                    novaTarefa.adicionarEvento(new EventoMutex(instanteRelativo, mutexId, isLock));
                                } 
                                // 2. Processa Eventos de E/S (I0:xx-yy)
                                else if (comando.startsWith("I")) {
                                    // A variável 'valores' agora tem algo como "05-10"
                                    //String[] tempos = valores.split("-");
                                    
                                    //int instanteRelativo = Integer.parseInt(tempos[0].trim()); // O 'xx'
                                    //int duracao = Integer.parseInt(tempos[1].trim());          // O 'yy'
                                    
                                    //novaTarefa.adicionarEvento(new EventoES(instanteRelativo, duracao));
                                }
                            }
                        } catch (Exception ex) {
                            System.err.println("Erro ao fazer o parsing do evento [" + strEvento + "] na Tarefa " + id);
                            // Pode optar por lançar uma exceção ou ignorar o evento mal formatado
                        }
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
            sistema = new SOMP(new EscalonadorSRTF(), 2); // 2 CPUs, Quantum 2
            sistema.adicionarTarefa(new Tarefa(1, "FF0000", 0, 5, 1, null)); // Tarefa Padrão
            sistema.adicionarTarefa(new Tarefa(2, "0000FF", 2, 3, 2, null)); // Tarefa Padrão
        }

        return sistema;
    }
}
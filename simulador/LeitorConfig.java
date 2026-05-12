package simulador;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class LeitorConfig {

     static SOMP carregarSimulacao(String caminhoFicheiro) {
        SOMP sistema = null;

        try (BufferedReader br = new BufferedReader(new FileReader(caminhoFicheiro))) {
            String linha;

            // 1. Ler a PRIMEIRA LINHA (Configurações do Sistema)
            if ((linha = br.readLine()) != null) {
                // Divide a linha pelos pontos e vírgulas
                String[] config = linha.split(";"); 
                
                String algoritmo = config[0].trim();
                int quantum = Integer.parseInt(config[1].trim());
                int qtdeCpus = Integer.parseInt(config[2].trim());

                // Instancia o escalonador correto com base na string
                Escalonador escalonador = null;
                if (algoritmo.equalsIgnoreCase("SRTF")) {
                    escalonador = new EscalonadorSRTF();
                } else if (algoritmo.equalsIgnoreCase("PRIOP")) {
                    escalonador = new EscalonadorPRIOP();        
                }

                // Cria o Sistema Operativo
                sistema = new SOMP(escalonador, qtdeCpus, quantum);
                
                // DICA: Vais precisar de adicionar o 'quantum' no construtor do teu SOMP depois!
            }

            // 2. Ler as RESTANTES LINHAS (Tarefas e Eventos)
            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue; // Ignora linhas em branco

                String[] dados = linha.split(";");
                
                int id = Integer.parseInt(dados[0].trim());
                String cor = dados[1].trim();
                int ingresso = Integer.parseInt(dados[2].trim());
                int duracao = Integer.parseInt(dados[3].trim());
                int prioridade = Integer.parseInt(dados[4].trim());

                // Usa o construtor que já tens na tua classe Tarefa
                // Construtor: id, prioridade, tempoExecucao, tempoChegada, cor
                Tarefa novaTarefa = new Tarefa(id,cor, ingresso, duracao, prioridade, null); // Passa null para a lista de eventos por enquanto

                // 3. Tratar a Lista de Eventos (se existir)
                if (dados.length > 5 && !dados[5].trim().isEmpty()) {
                    String[] listaEventos = dados[5].split(",");
                    for (String strEvento : listaEventos) {
                        strEvento = strEvento.trim();
                        // Aqui vais instanciar as classes filhas de Evento.
                        // Exemplo imaginário: se for um evento de E/S
                        // novaTarefa.adicionarEvento(new EventoES(...));
                    }
                }

                // Adiciona a tarefa criada ao sistema
                sistema.adicionarTarefa(novaTarefa); 
            }

        } catch (IOException | NumberFormatException e) {
            System.err.println("Erro ao ler o ficheiro: " + e.getMessage());
        }

        return sistema;
    }
}
package simulador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

// Interface principal do Simulador.
public class Window extends JFrame {
    private final int ROW_HEIGHT  = 40;       // Altura de cada linha de tarefa
    private final int LABEL_WIDTH = 120;      // Largura da coluna de nomes (esquerda)
    private final int TICK_WIDTH  = 25;       // Largura de cada tick de tempo (eixo X)
    
    private int        currentTime = 0;
    private GanttPanel ganttPanel;
    
    private List<Tarefa>         tasksHistory = new ArrayList<>(); 
    private SimulationController controller;

    public Window(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());


        // 1. Cria o painel "Pai" que vai ficar no topo da tela
        JPanel superiorPanel = new JPanel(new BorderLayout());
        superiorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Dá um respiro nas bordas

        // 2. Cria o painel dos botões (que vai ficar alinhado à esquerda)
        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton btnBack= new JButton("(<) Retroceder");
        JButton btnStep = new JButton("Próximo Passo (>)");
        JButton btnRunAll = new JButton("Execução Completa (>>)");
        
        // Adiciona os botões no painel da esquerda
        painelBotoes.add(btnBack);
        painelBotoes.add(btnStep);
        painelBotoes.add(btnRunAll);

        // 3. Junta tudo no painel Superior
        superiorPanel.add(painelBotoes, BorderLayout.WEST);         // Botões na ESQUERDA
        superiorPanel.add(new PainelLegenda(), BorderLayout.EAST);  // Legenda na DIREITA

        // 4. Adiciona o painel superior na janela
        // (Isso substitui a forma antiga que você usava para adicionar os botões)
        this.add(superiorPanel, BorderLayout.NORTH);
        
        btnStep.addActionListener(e -> {
            if (controller != null) 
                controller.stepForward(currentTime);
            ganttPanel.revalidate();
            ganttPanel.repaint();
        });

        btnBack.addActionListener(e -> {
            if (controller != null)
                controller.stepBack();
            ganttPanel.revalidate();
            ganttPanel.repaint();
        });

        btnRunAll.addActionListener(e -> {
            if (controller != null) 
                controller.runAll();
            ganttPanel.revalidate();
            ganttPanel.repaint();
        });


        // Gráfico de Gantt
        ganttPanel             = new GanttPanel();
        JScrollPane scrollPane = new JScrollPane(ganttPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Listener para detectar cliques no nome das tarefas (Extremidade Esquerda)
        ganttPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() <= LABEL_WIDTH) {
                    int taskIndex = controller.getTotalNumTarefas() - (e.getY() - 30) / ROW_HEIGHT - 1; 
                    if (taskIndex >= 0 && taskIndex < tasksHistory.size())
                        openTaskOptions(taskIndex);
                }
            }
        });

        setLocationRelativeTo(null);
    }

    public void setController(SimulationController ctrl) {
        this.controller = ctrl;
    }

    public void setCurrentTime(int t) {
        this.currentTime = t;
    }

    public void decCurrentTime() {
        --currentTime;
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    // Método para injetar tarefas e atualizar a tela
    public void addTask(Tarefa t) {
        tasksHistory.add(t);
        ganttPanel.revalidate(); 
        ganttPanel.repaint();
    }

    // Abre menu para modificar atributos
    private void openTaskOptions(int idx) {
        Tarefa t = tasksHistory.get(idx); // Pega apenas pra ler dados pra interface
        JPopupMenu menu = new JPopupMenu("Atributos da Tarefa T" + t.getId());
        
        JMenuItem prioritySetter = new JMenuItem(new AbstractAction("Alterar Prioridade") {
            public void actionPerformed(ActionEvent e) {
                String input = JOptionPane.showInputDialog(
                    Window.this, 
                    "Nova prioridade para T" + t.getId() + ":", 
                    "Alterar Prioridade", 
                    JOptionPane.QUESTION_MESSAGE
                );

                if (input != null && !input.trim().isEmpty()) {
                    try {
                        int priority = Integer.parseInt(input.trim());
                        // Chama o controller para alterar no Model!
                        if (controller != null) controller.changeTaskPriority(idx, priority);
                        ganttPanel.repaint(); 
                    } catch (NumberFormatException ex) {
                        showError("Valor inválido! Use apenas números.");
                    }
                }
            }
        });

        String suspLabel = t.isSuspensa() ? "Retomar Tarefa" : "Suspender Tarefa";
        JMenuItem suspendTaskSetter = new JMenuItem(new AbstractAction(suspLabel) {
            public void actionPerformed(ActionEvent e) {
                if (controller != null) controller.toggleTaskSuspension(idx);
                ganttPanel.repaint(); 
            }
        });

        menu.add(prioritySetter);
        menu.add(suspendTaskSetter);
        menu.show(ganttPanel, 10, ((controller.getTotalNumTarefas() - idx - 1) * ROW_HEIGHT) + 50);
    }

    public void incrementTime() {
        ++currentTime;
        ganttPanel.revalidate();
        ganttPanel.repaint();
    }
    // Desenha listras HORIZONTAIS (Para tarefas que chegaram e estão aguardando)
    private void desenharListrasHorizontais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
        g2.setColor(cor);
        // Pula de 4 em 4 pixels no eixo Y para criar o padrão listrado
        for (int i = 2; i < height; i += 4) {
            g2.drawLine(x, y + i, x + width, y + i);
        }
    }

    // Desenha listras VERTICAIS (Para tarefas finalizadas)
    private void desenharListrasVerticais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
        g2.setColor(cor);
        // Pula de 4 em 4 pixels no eixo X para criar o padrão listrado
        for (int i = 2; i < width; i += 4) {
            g2.drawLine(x + i, y, x + i, y + height);
        }
    }

    // Painel de renderização do Gantt.
    private class GanttPanel extends JPanel {
        public GanttPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        public Dimension getPreferredSize() {
            // O gráfico cresce para a direita conforme o tempo passa
            int width  = LABEL_WIDTH + (currentTime * TICK_WIDTH) + 100;
            int height = (tasksHistory.size() * ROW_HEIGHT) + 100;
            return new Dimension(
                Math.max(width, getParent().getWidth()), 
                height
            );
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. CALCULA O LIMITE INFERIOR DAS TAREFAS
            int yBottom = 30 + (tasksHistory.size() * ROW_HEIGHT);

            // 2. DESENHA A GRADE E O NOVO EIXO X (EMBAIXO)
            for (int t = 0; t <= currentTime; ++t) {
                int x = LABEL_WIDTH + (t * TICK_WIDTH);
                
                // Linha vertical do grid (vai até um pouco abaixo da última tarefa)
                g2.setColor(new Color(230, 230, 230));
                g2.drawLine(x, 0, x, yBottom + 5); 
                
                // Desenha os números de tempo EMBAIXO das tarefas
                if (t % 5 == 0) {
                    g2.setColor(Color.DARK_GRAY);
                    // O "yBottom + 20" coloca os números logo abaixo da linha final
                    g2.drawString(String.valueOf(t), x - 5, yBottom + 20); 
                }
            }

            // 3. DESENHA A LINHA HORIZONTAL FINAL (Eixo X base)
            g2.setColor(Color.BLACK); // Ou Color.DARK_GRAY se preferir mais suave
            g2.drawLine(0, yBottom, getWidth(), yBottom);

            // 4. REPOSICIONA O RODAPÉ DE CPUS (Mais para baixo ainda)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            int rodapeY = yBottom + 50; // Espaço para não encavalar com os números
            String textoRodape = "Tempo Ocioso das CPUs: ";
            if (controller != null) {
                Processador[] cpus_simulacao = controller.getProcessadores();
                for (int p = 0; p < cpus_simulacao.length; p++) {
                    textoRodape += "[P" + p + "]: " + cpus_simulacao[p].getTempoOciosoTotal() + " ticks     ";
                }
            }
            g2.drawString(textoRodape, 15, rodapeY);

            // 5. COLUNA ESQUERDA (Fundo e linha divisória)
            g2.setColor(new Color(245, 245, 245));
            g2.fillRect(0, 0, LABEL_WIDTH, yBottom); // Fundo agora vai só até a linha base
            g2.setColor(Color.BLACK);
            g2.drawLine(LABEL_WIDTH, 0, LABEL_WIDTH, yBottom + 5);

            // 6. DESENHO DAS TAREFAS (Mantém a mesma lógica)
            for (int i = 0; i < tasksHistory.size(); ++i) {
                int y    = 30 + ((tasksHistory.size() - 1 - i) * ROW_HEIGHT);
                Tarefa t = tasksHistory.get(i);
                
                // Desenha o nome da tarefa
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString("Tarefa T" + t.getId(), 15, y + 25);
                
                // Desenha a linha separadora horizontal de cada tarefa
                g2.setColor(new Color(220, 220, 220));
                g2.drawLine(0, y + ROW_HEIGHT, getWidth(), y + ROW_HEIGHT);
                
                // Desenho do bloco da tarefa tick a tick
                for (int tick = 0; tick < currentTime; ++tick) {
                    
                    // Pula os ticks antes da tarefa chegar
                    if (tick < t.getTempoChegada()) {
                        continue; 
                    }

                    Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tick);
                    int x = LABEL_WIDTH + (tick * TICK_WIDTH);

                    if (reg.estado == Tarefa.Estado.Executando) {
                        g2.setColor(Color.decode(t.getCor()));
                        g2.fillRect(x, y + 5, TICK_WIDTH, 30);
                        
                        g2.setColor(Color.WHITE); 
                        g2.setFont(new Font("Arial", Font.BOLD, 12));
                        g2.drawString("P" + reg.cpuId, x + 5, y + 25);
    
                    } else if (reg.estado == Tarefa.Estado.Suspenso) {
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y + 5, TICK_WIDTH, 30);
                        
                    } else if (reg.estado == Tarefa.Estado.Esperando) {
                        desenharListrasHorizontais(g2, x, y + 5, TICK_WIDTH, 30, new Color(180, 180, 180));
                        g2.setColor(new Color(200, 200, 200)); 
                        g2.drawRect(x, y + 5, TICK_WIDTH - 1, 30 - 1);
                        
                    } else if (reg.estado == Tarefa.Estado.Finalizado) {
                        desenharListrasVerticais(g2, x, y + 5, TICK_WIDTH, 30, new Color(200, 200, 200));
                        g2.setColor(new Color(220, 220, 220)); 
                        g2.drawRect(x, y + 5, TICK_WIDTH - 1, 30 - 1);
                    }

                    if (reg.ocorreuSorteio) {
                        g2.setColor(Color.RED);
                        g2.fillOval(x + TICK_WIDTH - 12, y + 3, 10, 10);
                        
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 9));
                        g2.drawString("S", x + TICK_WIDTH - 9, y + 11);
                    }
                }
            }
        }

        // Método auxiliar: Listras Horizontais (Espera)
        private void desenharListrasHorizontais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
            g2.setColor(cor);
            for (int i = 2; i < height; i += 4) {
                g2.drawLine(x, y + i, x + width, y + i);
            }
        }

        // Método auxiliar: Listras Verticais (Finalizada)
        private void desenharListrasVerticais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
            g2.setColor(cor);
            for (int i = 2; i < width; i += 4) {
                g2.drawLine(x + i, y, x + i, y + height);
            }
        }

    }

    public void showWindow() { 
        setVisible(true); 
    }

    // Classe que desenha a legenda na barra de botões
    class PainelLegenda extends JPanel {
        public PainelLegenda() {
            // Define o tamanho fixo ideal para caber as 4 legendas
            setPreferredSize(new Dimension(240, 40)); 
            setOpaque(false); // Fundo transparente para combinar com a janela
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 0; // Ponto de partida
            
            // 1. SORTEIO
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Sorteio", x, 12);
            
            g2.setColor(Color.RED);
            g2.fillOval(x + 10, 18, 14, 14); // Círculo Vermelho
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString("S", x + 14, 29); // Letra S
            
            x += 50; // Avança para o lado
            
            // 2. PRONTA (Espera - Listras Horizontais)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Pronta", x, 12);
            
            g2.setColor(new Color(200, 200, 200));
            g2.drawRect(x + 8, 18, 14, 14);
            g2.setColor(new Color(180, 180, 180));
            for(int i = 2; i < 14; i += 3) g2.drawLine(x + 8, 18 + i, x + 22, 18 + i);
            
            x += 55;
            
            // 3. FINALIZADA (Morta - Listras Verticais)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Finalizada", x, 12);
            
            g2.setColor(new Color(220, 220, 220));
            g2.drawRect(x + 15, 18, 14, 14);
            g2.setColor(new Color(200, 200, 200));
            for(int i = 2; i < 14; i += 3) g2.drawLine(x + 15 + i, 18, x + 15 + i, 32);
            
            x += 65;
            
            // 4. SUSPENSA (Bloco Preto)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Suspensa", x, 12);
            
            g2.setColor(Color.BLACK);
            g2.fillRect(x + 16, 18, 14, 14);
        }
    }
}
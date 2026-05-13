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

        // Painel Superior: Opções Globais
        JPanel  controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Botões de controle de tempo
        JButton btnBack      = new JButton("(<) Retroceder");
        JButton btnStep      = new JButton("Próximo Passo (>)");
        JButton btnRunAll    = new JButton("Execução Completa (>>)");

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

        // Adicionar os botões ao painel
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlPanel.add(btnBack);
        controlPanel.add(btnStep);
        controlPanel.add(btnRunAll);
        
        add(controlPanel, BorderLayout.NORTH);

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

            // Desenhar Linhas de grade e header de tempo
            g2.setColor(new Color(230, 230, 230));
            for (int t = 0; t <= currentTime; ++t) {
                int x = LABEL_WIDTH + (t * TICK_WIDTH);
                
                g2.drawLine(x, 0, x, getHeight());
                g2.setColor(Color.DARK_GRAY);
                
                if (t % 5 == 0)
                    g2.drawString(String.valueOf(t), x - 5, 20);
            }


            // Coluna Esquerda = nome das tarefas e fundo
            g2.setColor(new Color(245, 245, 245));
            g2.fillRect(0, 0, LABEL_WIDTH, getHeight());
            g2.setColor(Color.BLACK);
            g2.drawLine(LABEL_WIDTH, 0, LABEL_WIDTH, getHeight());

            for (int i = 0; i < tasksHistory.size(); ++i) {
                int y    = 30 + ((tasksHistory.size() - 1 - i) * ROW_HEIGHT);
                Tarefa t = tasksHistory.get(i);
                
                // Desenha o nome da tarefa
                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString("Tarefa T" + t.getId(), 15, y + 25);
                
                // Desenha a linha separadora horizontal
                g2.setColor(new Color(220, 220, 220));
                g2.drawLine(0, y + ROW_HEIGHT, getWidth(), y + ROW_HEIGHT);
                
                // Desenho do bloco da tarefa tick a tick pelo histórico
                for (int tick = 0; tick < currentTime; ++tick) {

                    if (tick < t.getTempoChegada()) {
                        continue; 
                    }

                    Tarefa.TickSnapshot reg = t.getRegistroNoTempo(tick);
                    int x = LABEL_WIDTH + (tick * TICK_WIDTH);

                    if (reg.estado == Tarefa.Estado.Executando) {
                        // Desenha o bloco com a cor customizada da tarefa
                        g2.setColor(Color.decode(t.getCor()));
                        g2.fillRect(x, y + 5, TICK_WIDTH, 30);
                        
                        // Escreve o ID do processador por cima do bloco
                        g2.setColor(Color.WHITE); // Texto branco para contraste
                        g2.setFont(new Font("Arial", Font.BOLD, 12));
                        g2.drawString("P" + reg.cpuId, x + 5, y + 25);
    
                    } else if (reg.estado == Tarefa.Estado.Suspenso) {
                        // Tarefa suspensa = Bloco Preto
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y + 5, TICK_WIDTH, 30);
                        
                    } else if (reg.estado == Tarefa.Estado.Esperando) {
                        // NOVA LÓGICA: Tarefa aguardando = Listras Horizontais
                        desenharListrasHorizontais(g2, x, y + 5, TICK_WIDTH, 30, new Color(180, 180, 180));
                        g2.setColor(new Color(200, 200, 200)); // Borda cinza
                        g2.drawRect(x, y + 5, TICK_WIDTH - 1, 30 - 1);
                        
                    } else if (reg.estado == Tarefa.Estado.Finalizado) {
                        // NOVA LÓGICA: Tarefa morta = Listras Verticais
                        desenharListrasVerticais(g2, x, y + 5, TICK_WIDTH, 30, new Color(200, 200, 200));
                        g2.setColor(new Color(220, 220, 220)); // Borda mais clara
                        g2.drawRect(x, y + 5, TICK_WIDTH - 1, 30 - 1);
                    }

                    // Verifica se ocorreu sorteio
                    if (reg.ocorreuSorteio) {
                        // Desenha um círculo Vermelho
                        g2.setColor(Color.RED);
                        g2.fillOval(x + TICK_WIDTH - 12, y + 3, 10, 10);
                        
                        // Desenha um "S" Branco e legível dentro
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Arial", Font.BOLD, 9));
                        g2.drawString("S", x + TICK_WIDTH - 9, y + 11);
                    }
                    // Se o estado for NaoCriada a tela não desenha nada
                }
            }
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            int rodapeY = 30 + (tasksHistory.size() * ROW_HEIGHT) + 30;
            String textoRodape = "Tempo Ocioso das CPUs: ";
            if (controller != null) {
                Processador[] cpusSimulacao = controller.getProcessadores();
                for (int p = 0; p < cpusSimulacao.length; p++) {
                    textoRodape += "[P" + p + "]: " + cpusSimulacao[p].getTempoOciosoTotal() + " ticks     ";
                }
            }
            g2.drawString(textoRodape, 15, rodapeY);
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
}
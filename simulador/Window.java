package simulador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

// Interface principal do Simulador
// Model View Controller pattern
//   SOMP                 = Model
//   Window               = View
//   SimulationController = Controller
public class Window extends JFrame {
    private final int ROW_HEIGHT  = 40;   // Altura de cada linha de tarefa
    private final int LABEL_WIDTH = 120;  // Largura da coluna de nomes (esquerda)
    private final int TICK_WIDTH  = 25;   // Largura de cada tick de tempo (eixo X)
    
    private int        currentTime = 0;
    private GanttPanel ganttPanel;
    
    private List<Tarefa>         tasksHistory = new ArrayList<>(); 
    private SimulationController controller;  

    public Window(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        // Painel Superior: Estrutura que segura os botões na esquerda e a legenda na direita
        JPanel superiorPanel = new JPanel(new BorderLayout());
        superiorPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT)); 
        
        JButton btnLoadFile  = new JButton("Carregar Arquivo");
        JButton btnBack      = new JButton("(<) Retroceder");
        JButton btnStep      = new JButton("Próximo Passo (>)");
        JButton btnRunAll    = new JButton("Execução Completa (>>)");
        
        btnLoadFile.addActionListener(e -> abrirSeletorDeArquivo());

        btnStep.addActionListener(e -> { 
            if (controller != null) 
                controller.stepForward(currentTime);
            ganttPanel.revalidate();
            ganttPanel.repaint();
            if (controller != null && controller.isSimulacaoFinalizada()) { 
                gerarImagemDoGantt();
                System.out.println("Simulação concluída passo a passo. Imagem gerada!");
            }
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
            if (controller != null && controller.isSimulacaoFinalizada()) {
                gerarImagemDoGantt();
                System.out.println("Execução completa finalizada. Imagem gerada!");
            }
        });

        painelBotoes.add(btnLoadFile);
        painelBotoes.add(new JSeparator(SwingConstants.VERTICAL));
        painelBotoes.add(btnBack);
        painelBotoes.add(btnStep);
        painelBotoes.add(btnRunAll);

        superiorPanel.add(painelBotoes, BorderLayout.WEST);         // Botões na esquerda
        superiorPanel.add(new PainelLegenda(), BorderLayout.EAST);  // Legenda na direita

        this.add(superiorPanel, BorderLayout.NORTH); 

        // Gráfico de Gantt
        ganttPanel             = new GanttPanel();
        JScrollPane scrollPane = new JScrollPane(ganttPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Listener para detectar cliques no nome das tarefas
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

    private void abrirSeletorDeArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecione o arquivo de configuração");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        int userSelection = fileChooser.showOpenDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
            boolean sucesso     = carregarSimulacaoPorCaminho(selectedPath);
            if (sucesso) {
                JOptionPane.showMessageDialog(
                    this, 
                    "Arquivo carregado com sucesso!", 
                    "Sucesso", 
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }

    public boolean carregarSimulacaoPorCaminho(String caminhoFicheiro) {  
        SOMP novoSistema = LeitorConfig.carregarSimulacao(caminhoFicheiro);
        if (novoSistema != null) {
            tasksHistory.clear();
            currentTime = 0;
            
            SimulationController novoController = new SimulationController(novoSistema, this);
            this.setController(novoController);
            
            if (ganttPanel != null) { 
                ganttPanel.revalidate();
                ganttPanel.repaint();
            }
            return true;
        } else {
            // O próprio LeitorConfig já disparou o Pop-up visual se não houver CPUs suficientes.
            // Aqui apenas garantimos que a interface não quebre.
            return false;
        }
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

    public void addTask(Tarefa t) {  
        tasksHistory.add(t);
        ganttPanel.revalidate(); 
        ganttPanel.repaint();
    }

    private void openTaskOptions(int idx) { 
        Tarefa t = tasksHistory.get(idx);  
        JPopupMenu menu = new JPopupMenu("Atributos da Tarefa T" + t.getId());
        
        JMenuItem prioritySetter = new JMenuItem(new AbstractAction("Alterar prioridadeEstatica") {
            public void actionPerformed(ActionEvent e) {  // Abre modal para digitar nova prioridadeEstatica
                String input = JOptionPane.showInputDialog(
                    Window.this, 
                    "Nova prioridadeEstatica para T" + t.getId() + ":", 
                    "Alterar prioridadeEstatica", 
                    JOptionPane.QUESTION_MESSAGE
                );

                if (input != null && !input.trim().isEmpty()) { 
                    try {
                        int priority = Integer.parseInt(input.trim());
                        if (controller != null) 
                            controller.changeTaskPriority(idx, priority);
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
                if (controller != null) 
                    controller.toggleTaskSuspension(idx);
                ganttPanel.repaint(); 
            }
        });

        menu.add(prioritySetter);
        menu.add(suspendTaskSetter);
        menu.show(
            ganttPanel, 
            10, 
            ((controller.getTotalNumTarefas() - idx - 1) * ROW_HEIGHT) + 50
        );
    }


    public void incrementTime() {
        ++currentTime;
        ganttPanel.revalidate();
        ganttPanel.repaint();
    }

    
    private class GanttPanel extends JPanel { 
        public GanttPanel() {
            setBackground(Color.WHITE);
        }

        @Override
        public Dimension getPreferredSize() {
            int width  = LABEL_WIDTH + (currentTime * TICK_WIDTH) + 100; 
            int height = (tasksHistory.size() * ROW_HEIGHT)       + 100;
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

            int yBottom = 30 + (tasksHistory.size() * ROW_HEIGHT);  // Limite inferior das tarefas

            for (int t = 0; t <= currentTime; ++t) { 
                int x = LABEL_WIDTH + (t * TICK_WIDTH);
                
                g2.setColor(new Color(230, 230, 230));
                g2.drawLine(x, 0, x, yBottom + 5); 
                
                if (t % 5 == 0) { 
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(String.valueOf(t), x - 5, yBottom + 20); 
                }
            }

            g2.setColor(Color.BLACK); 
            g2.drawLine(0, yBottom, getWidth(), yBottom);

            g2.setColor(new Color(245, 245, 245)); 
            g2.fillRect(0, 0, LABEL_WIDTH, yBottom);
            g2.setColor(Color.BLACK);
            g2.drawLine(LABEL_WIDTH, 0, LABEL_WIDTH, yBottom + 5);

            for (int i = 0; i < tasksHistory.size(); ++i) { 
                int y    = 30 + ((tasksHistory.size() - 1 - i) * ROW_HEIGHT);
                Tarefa t = tasksHistory.get(i);
                
                g2.setColor(Color.BLACK); 
                g2.setFont(new Font("Arial", Font.PLAIN, 12));
                g2.drawString("Tarefa T" + t.getId(), 15, y + 25);
                
                g2.setColor(new Color(220, 220, 220)); 
                g2.drawLine(0, y + ROW_HEIGHT, getWidth(), y + ROW_HEIGHT);
                
                for (int tick = 0; tick < currentTime; ++tick) { 
                    if (tick < t.getTempoChegada()) 
                        continue; 

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
                        
                    } else if (reg.estado == Tarefa.Estado.Bloqueado) { // IO -> laranja
                        g2.setColor(Color.ORANGE);
                        g2.fillRect(x, y + 5, TICK_WIDTH, 30);
                        
                        g2.setColor(Color.WHITE); 
                        g2.setFont(new Font("Arial", Font.BOLD, 10));
                        g2.drawString("I/O", x + 3, y + 25);

                    } else if (reg.estado == Tarefa.Estado.EsperandoMutex) { 
                        // Cor laranja/avermelhada quadriculada para espera de Mutex
                        desenharPreenchimentoQuadriculado(g2, x, y + 5, TICK_WIDTH, 30, new Color(255, 165, 0));
                        g2.setColor(new Color(255, 140, 0)); 
                        g2.drawRect(x, y + 5, TICK_WIDTH - 1, 30 - 1);
                    }else if (reg.estado == Tarefa.Estado.Esperando) { 
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

            // Rodapé com Proteção contra Null
            g2.setColor(Color.BLACK); 
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            int rodapeY = yBottom + 50; 
            String textoRodape = "Tempo Ocioso das CPUs: ";
            
            if (controller != null) {
                Processador[] cpus_simulacao = controller.getProcessadores();
                if (cpus_simulacao != null) { 
                    for (int p = 0; p < cpus_simulacao.length; p++) {
                        textoRodape += "[P" + p + "]: " + cpus_simulacao[p].getTempoOciosoTotal() + " ticks     ";
                    }
                }
            }
            g2.drawString(textoRodape, LABEL_WIDTH + 15, rodapeY);
        }

        private void desenharListrasHorizontais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
            g2.setColor(cor);
            for (int i = 2; i < height; i += 4) {
                g2.drawLine(x, y + i, x + width, y + i);
            }
        }

        private void desenharListrasVerticais(Graphics2D g2, int x, int y, int width, int height, Color cor) {
            g2.setColor(cor);
            for (int i = 2; i < width; i += 4) {
                g2.drawLine(x + i, y, x + i, y + height);
            }
        }
        
        private void desenharPreenchimentoQuadriculado(Graphics2D g2, int x, int y, int width, int height, Color cor) {
            g2.setColor(cor);
            // Linhas Verticais
            for (int i = 2; i < width; i += 4) {
                g2.drawLine(x + i, y, x + i, y + height);
            }
            // Linhas Horizontais
            for (int i = 2; i < height; i += 4) {
                g2.drawLine(x, y + i, x + width, y + i);
            }
        }
    }

    public void showWindow() { 
        setVisible(true); 
    }

    class PainelLegenda extends JPanel { 
        public PainelLegenda() {
            setPreferredSize(new Dimension(320, 40)); 
            setOpaque(false); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 0; 
            
            g2.setColor(Color.BLACK);  
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Sorteio", x, 12);
            
            g2.setColor(Color.RED);
            g2.fillOval(x + 10, 18, 14, 14); 
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.drawString("S", x + 14, 29); 
            
            x += 50; 
            
            g2.setColor(Color.BLACK);  
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Pronta", x, 12);
            
            g2.setColor(new Color(200, 200, 200));
            g2.drawRect(x + 8, 18, 14, 14);
            g2.setColor(new Color(180, 180, 180));
            for(int i = 2; i < 14; i += 3) 
                g2.drawLine(x + 8, 18 + i, x + 22, 18 + i);
            
            x += 55;
            
            g2.setColor(Color.BLACK);  
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Finalizada", x, 12);
            
            g2.setColor(new Color(220, 220, 220));
            g2.drawRect(x + 15, 18, 14, 14);
            g2.setColor(new Color(200, 200, 200));
            for(int i = 2; i < 14; i += 3) 
                g2.drawLine(x + 15 + i, 18, x + 15 + i, 32);
            
            x += 65;
            
            g2.setColor(Color.BLACK);  
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Suspensa", x, 12);
            
            g2.setColor(Color.BLACK);
            g2.fillRect(x + 16, 18, 14, 14);

            x += 65;

            g2.setColor(Color.BLACK);  
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.drawString("Bloqueada", x, 12);
            
            g2.setColor(Color.ORANGE);
            g2.fillRect(x + 16, 18, 14, 14);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.drawString("I/O", x + 17, 29);
        }
    }

    private void gerarImagemDoGantt() {
        // 1. Calcula o tamanho necessário (Garante largura mínima para caber a legenda)
        int width = Math.max(ganttPanel.getPreferredSize().width, LABEL_WIDTH + 300);
        int ganttHeight = ganttPanel.getPreferredSize().height;
        int legendHeight = 60; // Espaço extra no topo da imagem para a legenda
        int totalHeight = ganttHeight + legendHeight;

        Dimension tamanhoOriginal = ganttPanel.getSize(); 
        ganttPanel.setSize(new Dimension(width, ganttHeight)); 

        BufferedImage imagem = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB); 
        Graphics2D g2 = imagem.createGraphics(); 
        
        // Pinta o fundo inteiro de branco
        g2.setColor(Color.WHITE); 
        g2.fillRect(0, 0, width, totalHeight);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // --- NOVIDADE: DESENHANDO A LEGENDA NA IMAGEM ---
        PainelLegenda legenda = new PainelLegenda();
        legenda.setSize(300, 40); 
        
        // Cria uma cópia da "caneta" e move ela para o topo (alinhada com o gráfico)
        Graphics2D g2Legenda = (Graphics2D) g2.create();
        g2Legenda.translate(LABEL_WIDTH, 15); 
        legenda.paint(g2Legenda);
        g2Legenda.dispose(); // Descarta essa caneta
        // ------------------------------------------------

        // --- DESENHANDO O GANTT LOGO ABAIXO ---
        // Cria outra cópia da caneta e empurra ela para baixo, para não rabiscar em cima da legenda
        Graphics2D g2Gantt = (Graphics2D) g2.create();
        g2Gantt.translate(0, legendHeight); 
        ganttPanel.paint(g2Gantt); 
        g2Gantt.dispose();
        // --------------------------------------

        // Devolve o tamanho original pro painel voltar a caber no ScrollPane
        ganttPanel.setSize(tamanhoOriginal); 

        File pasta = new File("imagens"); 
        if (!pasta.exists()) {
            pasta.mkdir();
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File arquivoSaida = new File(pasta, "simulacao_" + timestamp + ".png");

        try { 
            ImageIO.write(imagem, "png", arquivoSaida);
            System.out.println("Imagem completa gerada com sucesso: " + arquivoSaida.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar a imagem: " + e.getMessage());
        }
    }
}
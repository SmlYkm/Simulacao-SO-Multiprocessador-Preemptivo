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
    
    private int currentTime = 0;
    private GanttPanel ganttPanel;
    private List<Object> tasksHistory = new ArrayList<>(); // Substituir Object por TCB/Tarefa

    public Window(String title) {
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLayout(new BorderLayout());

        // Painel Superior: Opções Globais
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAdd      = new JButton("Adicionar Tarefa");
        JButton btnCPU      = new JButton("Config. CPUs");
        JButton btnImport   = new JButton("Importar Config");
        JButton btnStep     = new JButton("Próximo Passo (>)");
        
        controlPanel.add(btnAdd);
        controlPanel.add(btnCPU);
        controlPanel.add(btnImport);
        controlPanel.add(new JSeparator(SwingConstants.VERTICAL));
        controlPanel.add(btnStep);
        add(controlPanel, BorderLayout.NORTH);

        // Gráfico de Gantt
        ganttPanel = new GanttPanel();
        JScrollPane scrollPane = new JScrollPane(ganttPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);

        // Listener para detectar cliques no nome das tarefas (Extremidade Esquerda)
        ganttPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() <= LABEL_WIDTH) {
                    int taskIndex = (e.getY() - 30) / ROW_HEIGHT; // 30px
                    if (taskIndex >= 0 && taskIndex < tasksHistory.size()) {
                        openTaskOptions(taskIndex);
                    }
                }
            }
        });

        setLocationRelativeTo(null);
    }

    
    // Abre menu para modificar atributos
    private void openTaskOptions(int taskIndex) {
        JPopupMenu menu = new JPopupMenu("Atributos da Tarefa T" + taskIndex);
        menu.add(new JMenuItem("Alterar Prioridade"));
        menu.add(new JMenuItem("Suspender Tarefa"));
        menu.add(new JMenuItem("Forçar Execução"));
        menu.show(ganttPanel, 10, (taskIndex * ROW_HEIGHT) + 50);
    }

    public void incrementTime() {
        ++currentTime;
        ganttPanel.repaint();
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
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Desenhar Linhas de grade e header de tempo
            g2.setColor(new Color(230, 230, 230));
            for (int t = 0; t <= currentTime; t++) {
                int x = LABEL_WIDTH + (t * TICK_WIDTH);
                g2.drawLine(x, 0, x, getHeight());
                g2.setColor(Color.DARK_GRAY);
                if (t % 5 == 0) 
                    g2.drawString(String.valueOf(t), x - 5, 20);
                g2.setColor(new Color(230, 230, 230));
            }

            // Coluna Esquerda = nome das tarefas
            g2.setColor(new Color(245, 245, 245));
            g2.fillRect(0, 0, LABEL_WIDTH, getHeight());
            g2.setColor(Color.BLACK);
            g2.drawLine(LABEL_WIDTH, 0, LABEL_WIDTH, getHeight());

            for (int i = 0; i < tasksHistory.size(); i++) {
                int y = 30 + (i * ROW_HEIGHT);
                g2.drawString("Tarefa T" + i, 15, y + 25);
                g2.drawLine(0, y + ROW_HEIGHT, getWidth(), y + ROW_HEIGHT);
                
                // Exemplo de desenho de bloco
                // g2.setColor(Color.BLUE);
                // g2.fillRect(LABEL_WIDTH + (startTick * TICK_WIDTH), y + 5, duration * TICK_WIDTH, 30);
            }
        }
    }

    public void showWindow() { 
        setVisible(true); 
    }
}
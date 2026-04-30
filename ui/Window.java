package ui;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class Window {
    private int time = 0;
    private JFrame frame = new JFrame();
    
    private List<JButton> tasksList = new ArrayList<>();


    public Window(String windowName, int windowWidth, int windowHeight) {
        frame.setTitle(windowName);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(windowWidth, windowHeight);

    }

    public Window() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    public void incrementTime() {
        ++time;
    }

    public void decrementTime() {
        --time;
    }

    public int getTime() {
        return time;
    }

    public void addButton(JButton button) {
        tasksList.add(button); // Atualiza a estrutura de dados
        frame.add(button);     // Atualiza a tela (JFrame)
        refreshUI();           // Avisa o SO para redesenhar
    }

    public void destroyButton(String buttonId) {
        Iterator<JButton> it = tasksList.iterator();

        while (it.hasNext()) {
            JButton btnAtual = it.next(); 
            // Procura o botão pelo texto (ID)
            if (btnAtual.getText().equals(buttonId)) {        
                it.remove();            // Remove da lista na memória
                frame.remove(btnAtual); // Remove visualmente da tela
                refreshUI();            // Avisa o SO para redesenhar
                break;                  // Se já achou e apagou, sai do loop
            }
        }
    }

    private void refreshUI() {
        frame.revalidate(); 
        frame.repaint();    
    }
    
    public void show() {
        frame.setVisible(true);
    }
}
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants; // Imported WindowConstants

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Window with Button");
        
        // Updated to use static access from WindowConstants
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JButton button = new JButton("Click Me");
        frame.add(button);

        frame.setVisible(true);
    }
}
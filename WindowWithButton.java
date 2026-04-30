import javax.swing.JButton;
import javax.swing.JFrame;
//askjdhfaçlksdfh
public class WindowWithButton {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Window with Button");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);

        JButton button = new JButton("Click Me");
        frame.add(button);

        frame.setVisible(true);
    }
}
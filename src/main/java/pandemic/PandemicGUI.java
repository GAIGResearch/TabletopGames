package pandemic;

import components.Board;
import utilities.BoardView;
import javax.swing.*;

class PandemicGUI extends JFrame {

    PandemicGUI(Board board) {
        JComponent comp = new BoardView(board, "data/pandemicBackground.jpg");
        getContentPane().add(comp);

        // Frame properties
        pack();
        this.setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        repaint();
    }
}

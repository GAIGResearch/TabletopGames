package utilities;

import components.Board;
import components.BoardNode;

import javax.swing.*;
import java.awt.*;

public class BoardView extends JComponent {
    private Image background;
    private Board board;
    private int width;
    private int height;

    public BoardView (Board b, String backgroundPath) {
        this.board = b;
        this.background = ImageIO.GetInstance().getImage(backgroundPath);
        width = background.getWidth(null);
        height = background.getHeight(null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawBoard((Graphics2D) g);
    }

    private void drawBoard(Graphics2D g) {
        // Draw board background
        g.drawImage(background, 0, 0, null, null);

        // Draw nodes
        int size = 6;
        for (BoardNode b : board.getBoardNodes()) {
            Vector2D pos = b.getPosition();
            g.fillOval(pos.getX() - size/2, pos.getY() - size/2, size, size);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}

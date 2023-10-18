package games.resistance.gui;


import javax.swing.*;
import java.awt.*;

public class ResBoardView extends JPanel {

    private Image backgroundImage;

    public ResBoardView(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int imgWidth = backgroundImage.getWidth(this);
        int imgHeight = backgroundImage.getHeight(this);

        // Calculate the position to center the image
        int x = (getWidth() - imgWidth) / 2;
        int y = (getHeight() - imgHeight) / 2;

        g.drawImage(backgroundImage, x, y, imgWidth, imgHeight, this);
    }

}

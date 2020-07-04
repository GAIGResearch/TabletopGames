package games.coltexpress.gui;

import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressTrainView extends JComponent {

    List<Compartment> train;
    int width, height;
    String dataPath;

    public ColtExpressTrainView(List<Compartment> train, int width, int height, String dataPath) {
        this.train = train;
        this.width = width;
        this.height = height;
        this.dataPath = dataPath;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawTrain((Graphics2D) g, new Rectangle(0, 0, width, height));
        // TODO: round cards
    }

    public void drawTrain(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        for (int i = 0; i < train.size(); i++) {
            // Draw background car
            Image car;
            if (i == 0) {
                car = ImageIO.GetInstance().getImage(dataPath + "locomotive.png");
            } else {
                car = ImageIO.GetInstance().getImage(dataPath + "wagon.png");
            }
            g.drawImage(car, rect.x + i*trainCarWidth, rect.y, trainCarWidth, trainCarHeight, null);

            // TODO
            Compartment c = train.get(i);
            // c.lootInside
            // c.lootOnTop
            // c.playersInsideCompartment
            // c.playersOnTopOfCompartment
            // c.containsMarshal
        }
    }

    private void drawLoot(Graphics2D g, Loot loot, Rectangle r, boolean visible) {
        Image lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

}

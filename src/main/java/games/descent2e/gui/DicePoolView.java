package games.descent2e.gui;

import games.descent2e.DescentGameState;
import games.descent2e.DescentParameters;
import games.descent2e.components.DescentDice;
import games.descent2e.components.DicePool;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;

import static gui.AbstractGUIManager.defaultItemSize;

public class DicePoolView extends JComponent {
    DicePool dicePool;
    String imgPath;
    int width = 70, height = 200;

    public DicePoolView(DicePool dicePool, DescentGameState dgs) {
        this.dicePool = dicePool;
        this.imgPath = ((DescentParameters)dgs.getGameParameters()).dataPath + "/img/";
    }

    @Override
    protected void paintComponent(Graphics gg) {
        Graphics2D g = (Graphics2D) gg;

        // todo rerolls?

        // Draw dice colors
        int idx = 0;
        int pad = 5;
        int x = pad;
        int y = 20 + pad;
        for (DescentDice die : dicePool.getComponents()) {
            Image toDraw = ImageIO.GetInstance().getImage(imgPath + "dice/" + die.getColour().name().toLowerCase() + ".png");
            g.drawImage(toDraw, x, y, defaultItemSize / 3, defaultItemSize / 3, null);
            x += defaultItemSize/3 + pad;
            if (x + defaultItemSize/3 >= width) {
                x = pad;
                y += defaultItemSize/3;
            }
            idx++;
        }
        x = pad;
        y += defaultItemSize;

        if (dicePool.hasRolled()) {
            // Draw results:
            int h = g.getFontMetrics().getHeight();
            g.setColor(Color.white);
            // Damage
            Image toDraw = ImageIO.GetInstance().getImage(imgPath + "tokens/Heart_L.png");
            g.drawImage(toDraw, x, y - h*2/3, defaultItemSize / 3, defaultItemSize / 3, null);
            g.drawString(""+dicePool.getDamage(), x + defaultItemSize/3 + pad, y);
            y += defaultItemSize/3 + pad;
            // Shields
            toDraw = ImageIO.GetInstance().getImage(imgPath + "tokens/Shield_L.png");
            g.drawImage(toDraw, x, y - h*2/3, defaultItemSize / 3, defaultItemSize / 3, null);
            g.drawString(""+dicePool.getShields(), x + defaultItemSize/3 + pad, y);
            y += defaultItemSize/3 + pad;
            // Surges
            toDraw = ImageIO.GetInstance().getImage(imgPath + "tokens/Surge_L.png");
            g.drawImage(toDraw, x, y - h*2/3, defaultItemSize / 3, defaultItemSize / 3, null);
            g.drawString(""+dicePool.getSurge(), x + defaultItemSize/3 + pad, y);
            y += defaultItemSize/3 + pad;
            // Range
            g.drawString("R: "+dicePool.getRange(), x, y);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    public void update(DicePool pool) {
        this.dicePool = pool;
    }
}

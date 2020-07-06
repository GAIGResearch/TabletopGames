package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.RoundCard;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;

import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressTrainView extends JComponent {

    List<Compartment> train;
    int width, height;
    String dataPath;
    HashMap<Integer, ColtExpressTypes.CharacterType> characters;
    ColtExpressGameState cegs;

    public ColtExpressTrainView(List<Compartment> train, String dataPath,
                                HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        this.train = train;
        int nCars = train.size();
        this.width = (trainCarWidth*2/3)*(nCars-1) + trainCarWidth;
        this.height = trainCarHeight+ceCardHeight;
        this.dataPath = dataPath;
        this.characters = characters;
    }

    @Override
    protected void paintComponent(Graphics g) {
        drawTrain((Graphics2D) g, new Rectangle(0, 0, width, height));

        // TODO: round cards, hidden unless already past or current
        Deck<RoundCard> rounds = cegs.getRounds();
    }

    public void drawTrain(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        int x = rect.x;
        int bottomOffset = trainCarHeight/5;
        int y = rect.y + playerSize - bottomOffset;
        for (int i = 0; i < train.size(); i++) {
            // Draw background car
            Image car;
            int carWidth;
            if (i == train.size()-1) {
                car = ImageIO.GetInstance().getImage(dataPath + "locomotive.png");
                carWidth = trainCarWidth;
            } else {
                car = ImageIO.GetInstance().getImage(dataPath + "wagon.png");
                carWidth = (int)(2/3.0*trainCarWidth);
            }
            g.drawImage(car, x, y, trainCarWidth, trainCarHeight, null);

            // Draw contents in car
            Compartment c = train.get(i);

            int spaceWidth = trainCarWidth*2/3-trainCarWidth/15;
            int lootAreaWidth = spaceWidth/2;
            int x1 = x + spaceWidth/2 + 5;
            int offset;
            // Loot inside
            if (c.lootInside.getSize() > 0) {
                offset = lootAreaWidth / (c.lootInside.getSize()+1);
                for (int j = 0; j < c.lootInside.getSize(); j++) {
                    drawLoot(g, c.lootInside.get(j), new Rectangle(x1 + offset * j, y+trainCarHeight - lootSize - bottomOffset, lootSize, lootSize));
                }
            }
            // Loot on top
            if (c.lootOnTop.getSize() > 0) {
                offset = lootAreaWidth / (c.lootOnTop.getSize()+1);
                for (int j = 0; j < c.lootOnTop.getSize(); j++) {
                    drawLoot(g, c.lootOnTop.get(j), new Rectangle(x1 + offset * j, 0, lootSize, lootSize));
                }
            }
            // Players + marshal
            x1 = x + 5;
            int j = 0;
            if (c.playersInsideCompartment.size() > 0 || c.containsMarshal) {
                offset = lootAreaWidth / (c.playersInsideCompartment.size() + 2 + (c.containsMarshal ? 1 : 0));
                for (int p : c.playersInsideCompartment) {
                    drawPlayer(g, p, new Rectangle(x1 + offset * j, y + trainCarHeight - playerSize - bottomOffset, playerSize, playerSize));
                    j++;
                }
                if (c.containsMarshal) {
                    drawPlayer(g, -1, new Rectangle(x1 + offset * j, y + trainCarHeight - playerSize - bottomOffset, playerSize, playerSize));
                }
            }
            if (c.playersOnTopOfCompartment.size() > 0) {
                offset = lootAreaWidth / (c.playersOnTopOfCompartment.size()+2);
                j = 0;
                for (int p : c.playersOnTopOfCompartment) {
                    drawPlayer(g, p, new Rectangle(x1 + offset * j, 0, playerSize, playerSize));
                    j++;
                }
            }

            x += carWidth;
        }
    }

    private void drawLoot(Graphics2D g, Loot loot, Rectangle r) {
        Image lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
    }

    private void drawPlayer(Graphics2D g, int p, Rectangle r) {
        Image playerFace;
        if (p == -1) {
            playerFace = ImageIO.GetInstance().getImage(dataPath + "characters/Marshal.png");
        } else {
            playerFace = ImageIO.GetInstance().getImage(dataPath + "characters/" + characters.get(p).name() + ".png");
        }
        g.drawImage(playerFace, r.x, r.y, r.width, r.height, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(ColtExpressGameState cegs) {
        this.cegs = cegs;
    }
}

package games.coltexpress.gui;

import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.components.Compartment;
import games.coltexpress.components.Loot;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;

import static games.coltexpress.gui.ColtExpressGUIManager.*;

public class ColtExpressTrainView extends JComponent {

    double scale = 1.0;
    int panX, panY;

    // List of compartments in the train
    List<Compartment> train;
    // Width and height of view
    int width, height;
    // Path to assets
    String dataPath;
    // List of player characters, mapping to player ID
    HashMap<Integer, ColtExpressTypes.CharacterType> characters;
    // Current game state
    ColtExpressGameState cegs;
    // Bottom offset based on asset (to make characters and loot appear inside train)
    int bottomOffset = (int)(trainCarHeight/5. * scale);

    public ColtExpressTrainView(List<Compartment> train, String dataPath,
                                HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        this.train = train;
        int nCars = train.size();
        this.width = trainCarWidth*3/2*nCars;
        this.height = (int)((trainCarHeight+ playerSize - bottomOffset + 20) * 1.5);
        this.dataPath = dataPath;
        this.characters = characters;

        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                // Rotated down, zoom out
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Middle (wheel) click, pan around
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panX += (int) (scale * (end.x - start.x));
                    panY += (int) (scale * (end.y - start.y));
                    start = null;
                }
            }
        });
    }

    private void updateScale(double scale) {
        this.scale = scale;
        bottomOffset = (int)(trainCarHeight/5. * scale);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Draw train
        drawTrain((Graphics2D) g, new Rectangle(panX, panY, (int)(width*scale), (int)(height*scale)));
    }

    /**
     * Draws the train compartments, including players, marshal and loot present
     * @param g - Graphics object
     * @param rect - rectangle to draw train in
     */
    public void drawTrain(Graphics2D g, Rectangle rect) {
        int size = g.getFont().getSize();
        int x = rect.x;
        int y = rect.y + (int)(playerSize*scale) - bottomOffset;
        for (int i = 0; i < train.size(); i++) {
            // Draw background car
            Image car;
            int carWidth;
            if (i == train.size()-1) {
                car = ImageIO.GetInstance().getImage(dataPath + "locomotive.png");
                carWidth = (int)(trainCarWidth*scale);
            } else {
                car = ImageIO.GetInstance().getImage(dataPath + "wagon.png");
                carWidth = (int)(trainCarWidth*scale);
            }
            g.drawImage(car, x, y, (int)(trainCarWidth*3/2*scale), (int)(trainCarHeight*scale), null);

            // Draw contents in car
            Compartment c = train.get(i);

            int spaceWidth = (int)((trainCarWidth-trainCarWidth/15)*scale);
            int lootAreaWidth = spaceWidth/2;
            int x1 = x + spaceWidth/2 + 5;
            int offset;
            // Loot inside
            if (c.lootInside.getSize() > 0) {
                offset = lootAreaWidth / (c.lootInside.getSize()+1);
                for (int j = 0; j < c.lootInside.getSize(); j++) {
                    drawLoot(g, c.lootInside.get(j), new Rectangle(x1 + offset * j, (int)(y+trainCarHeight*scale - lootSize*scale - bottomOffset), (int)(lootSize*scale), (int)(lootSize*scale)));
                }
            }
            // Loot on top
            if (c.lootOnTop.getSize() > 0) {
                offset = lootAreaWidth / (c.lootOnTop.getSize()+1);
                for (int j = 0; j < c.lootOnTop.getSize(); j++) {
                    drawLoot(g, c.lootOnTop.get(j), new Rectangle(x1 + offset * j, (int)(playerSize*scale - lootSize*scale), (int)(lootSize*scale), (int)(lootSize*scale)));
                }
            }
            // Players + marshal
            x1 = x + 5;
            int j = 0;
            if (c.playersInsideCompartment.size() > 0 || c.containsMarshal) {
                offset = lootAreaWidth / (c.playersInsideCompartment.size() + 2 + (c.containsMarshal ? 1 : 0));
                for (int p : c.playersInsideCompartment) {
                    drawPlayer(g, p, new Rectangle(x1 + offset * j, (int)(y + trainCarHeight*scale - playerSize*scale - bottomOffset), (int)(playerSize*scale), (int)(playerSize*scale)));
                    j++;
                }
                if (c.containsMarshal) {
                    drawPlayer(g, -1, new Rectangle(x1 + offset * j, (int)(y + trainCarHeight*scale - playerSize*scale - bottomOffset), (int)(playerSize*scale), (int)(playerSize*scale)));
                }
            }
            if (c.playersOnTopOfCompartment.size() > 0) {
                offset = lootAreaWidth / (c.playersOnTopOfCompartment.size()+2);
                j = 0;
                for (int p : c.playersOnTopOfCompartment) {
                    drawPlayer(g, p, new Rectangle(x1 + offset * j, 0, (int)(playerSize*scale), (int)(playerSize*scale)));
                    j++;
                }
            }

            x += carWidth;
        }
    }

    /**
     * Draws an item of loot.
     * @param g - Graphics object
     * @param loot - loot to draw.
     * @param r - rectangle to draw loot in.
     */
    private void drawLoot(Graphics2D g, Loot loot, Rectangle r) {
        Image lootFace = ImageIO.GetInstance().getImage(dataPath + loot.getLootType().name() + "_behind.png");
        g.drawImage(lootFace, r.x, r.y, r.width, r.height, null);
        // Draw component ID for identification in actions available
//        Font f = g.getFont();
//        g.setFont(new Font(f.getName(), Font.PLAIN, 8));
//        g.setColor(Color.black);
//        g.drawString(loot.getComponentID()+"", r.x, r.y + 15);
//        g.setFont(f);
    }

    /**
     * Draws a player pawn.
     * @param g - Graphics object
     * @param p - player ID to draw
     * @param r - rectangle to draw token in.
     */
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

    /**
     * Updates information based on current game state.
     * @param cegs - current game state.
     */
    public void update(ColtExpressGameState cegs) {
        this.cegs = cegs;
    }
}

package games.terraformingmars.gui;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static core.AbstractGUI.*;
import static games.terraformingmars.gui.Utils.*;

public class TMPlayerView extends ComponentView {

    int player;
    TMGameState gs;

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;
    Image production;

    int fontSize = 16;
    int offsetX = 10;
    int spacing = 10;

    // TODO: standard actions with resources
    // Player choice cards
    // Tags cards played
    // Effects cards played
    // Actions cards played
    // Number of cards played by type
    // Milestones available with progress, highlight claimed
    // Awards available with progress, highlight claimed

    public TMPlayerView(TMGameState gs, int player) {
        super(gs.getBoard(), 0, 0);
        this.gs = gs;
        this.player = player;

        width = defaultItemSize * 2 * TMTypes.Resource.nPlayerBoardRes() + offsetX*2;
        height = defaultItemSize * 2 + offsetX*2;

        rects = new Rectangle[gs.getBoard().getWidth() * gs.getBoard().getHeight()];
        highlight = new ArrayList<>();

        production = ImageIO.GetInstance().getImage("data/terraformingmars/images/misc/production.png");

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    // Left click, highlight cell
                    for (Rectangle r: rects) {
                        if (r != null && r.contains(e.getPoint())) {
                            highlight.clear();
                            highlight.add(r);
                            break;
                        }
                    }
                } else {
                    // Remove highlight
                    highlight.clear();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;

        g.setFont(new Font("Prototype", Font.BOLD, fontSize));

        // Draw resources and production
        int k = 0;
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            if (res.isPlayerBoardRes()) {
                // Rect containing resource image, count next to it. Next line: prod background with prod count
                Image resImg = ImageIO.GetInstance().getImage(res.getImagePath());
                drawImage(g, resImg, offsetX + spacing / 5 + k * defaultItemSize * 2, offsetX + spacing / 5, defaultItemSize, defaultItemSize);
                drawShadowStringCentered(g, "" + gs.getPlayerResources()[player].get(res).getValue(),
                        new Rectangle2D.Double(offsetX + spacing / 5. + defaultItemSize + k * defaultItemSize * 2, offsetX + spacing / 5., defaultItemSize, defaultItemSize));
                drawImage(g, production, offsetX + spacing / 5 + defaultItemSize / 2 + k * defaultItemSize * 2, offsetX + spacing / 5 + defaultItemSize, defaultItemSize, defaultItemSize);
                drawShadowStringCentered(g, "" + gs.getPlayerProduction()[player].get(res).getValue(),
                        new Rectangle2D.Double(offsetX + spacing / 5. + defaultItemSize / 2. + k * defaultItemSize * 2, offsetX + spacing / 5. + defaultItemSize, defaultItemSize, defaultItemSize));

                g.setColor(playerColors[player]);
                g.drawRect(offsetX + k * defaultItemSize * 2, offsetX, defaultItemSize * 2, defaultItemSize * 2);
                k++;
            }
        }

        if (highlight.size() > 0) {
            g.setColor(Color.green);
            Stroke s = g.getStroke();
            g.setStroke(new BasicStroke(3));

            Rectangle r = highlight.get(0);
            g.drawRect(r.x, r.y, r.width, r.height);
            g.setStroke(s);
        }
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(TMGameState gs) {
        this.gs = gs;
    }
}

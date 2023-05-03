package games.terraformingmars.gui;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.Award;
import games.terraformingmars.components.Milestone;
import gui.views.ComponentView;
import utilities.ImageIO;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import static games.terraformingmars.gui.TMBoardView.playerColors;
import static games.terraformingmars.gui.TMGUI.focusPlayer;
import static gui.AbstractGUIManager.defaultItemSize;
import static utilities.GUIUtils.drawImage;
import static utilities.GUIUtils.drawShadowStringCentered;

public class TMPlayerView extends ComponentView {

    TMGameState gs;

    Rectangle[] rects;  // Used for highlights + action trimming
    ArrayList<Rectangle> highlight;

    Image background;
    Image production;

    int fontSize = 16;
    int offsetX = 10;
    int spacing = 10;

    public TMPlayerView(TMGameState gs, int player) {
        super(gs.getBoard(), 0, 0);
        this.gs = gs;

        width = defaultItemSize * 2 * TMTypes.Resource.nPlayerBoardRes() + offsetX*2;
        height = defaultItemSize * 2 + offsetX*8 + defaultItemSize*5/3;

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

        g.setFont(TMGUI.defaultFont);

        // Draw resources and production
        int k = 0;
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            if (res.isPlayerBoardRes()) {
                // Rect containing resource image, count next to it. Next line: prod background with prod count
                Image resImg = ImageIO.GetInstance().getImage(res.getImagePath());
                drawImage(g, resImg, offsetX + spacing / 5 + k * defaultItemSize * 2, offsetX + spacing / 5, defaultItemSize, defaultItemSize);
                drawShadowStringCentered(g, "" + gs.getPlayerResources()[focusPlayer].get(res).getValue(),
                        new Rectangle(offsetX + spacing / 5 + defaultItemSize + k * defaultItemSize * 2, offsetX + spacing / 5, defaultItemSize, defaultItemSize));
                drawImage(g, production, offsetX + spacing / 5 + defaultItemSize / 2 + k * defaultItemSize * 2, offsetX + spacing / 5 + defaultItemSize, defaultItemSize, defaultItemSize);
                drawShadowStringCentered(g, "" + gs.getPlayerProduction()[focusPlayer].get(res).getValue(),
                        new Rectangle(offsetX + spacing / 5 + defaultItemSize / 2 + k * defaultItemSize * 2, offsetX + spacing / 5 + defaultItemSize, defaultItemSize, defaultItemSize),
                        Color.white, Color.black);

                g.setColor(playerColors[focusPlayer]);
                g.drawRect(offsetX + k * defaultItemSize * 2, offsetX, defaultItemSize * 2, defaultItemSize * 2);
                k++;
            }
        }

        // Draw tags from cards played, tag + count
        k = 0;
        int startX = offsetX;
        int startY = offsetX*2 + defaultItemSize*2;
        drawShadowStringCentered(g, "Tags played:", new Rectangle(startX, startY, defaultItemSize*2, defaultItemSize/3), null, null, 12);
        startX += defaultItemSize*2;
        for (TMTypes.Tag t: TMTypes.Tag.values()) {
            int nCards = gs.getPlayerCardsPlayedTags()[focusPlayer].get(t).getValue();
            Image img = ImageIO.GetInstance().getImage(t.getImagePath());
            drawImage(g, img, startX + k*spacing/2 + k*2*defaultItemSize/3, startY, defaultItemSize/3, defaultItemSize/3);
            drawShadowStringCentered(g, "" + nCards,
                    new Rectangle(startX + k*spacing/2 + k*2*defaultItemSize/3 + defaultItemSize/3, startY, defaultItemSize/3, defaultItemSize/3));
            k++;
        }

        // Draw number of cards played by type
        k = 0;
        startX = offsetX;
        startY = offsetX*3 + defaultItemSize*2 + defaultItemSize/3;
        Font f = g.getFont();
        g.setFont(new Font(f.getName(), f.getStyle(), 12));
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        String text = "Card types played:";
        drawShadowStringCentered(g, text, new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize/3), null, null, 12);
        startX += metrics.stringWidth(text) + spacing*2;
        for (TMTypes.CardType t: TMTypes.CardType.values()) {
            if (t.isPlayableStandard()) {
                int nCards = gs.getPlayerCardsPlayedTypes()[focusPlayer].get(t).getValue();
                text = t.name() + ": " + nCards;
                drawShadowStringCentered(g, text,
                        new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize / 3), t.getColor(), null, 12);
                startX += metrics.stringWidth(text) + spacing*2;
                k++;
            }
        }

        // Draw number of tiles by type
        k = 0;
        startX = offsetX;
        startY = offsetX*4 + defaultItemSize*2 + 2*defaultItemSize/3;
        drawShadowStringCentered(g, "Tiles placed:", new Rectangle(startX, startY, defaultItemSize*2, defaultItemSize/3), null, null, 12);
        startX += defaultItemSize*2;
        for (TMTypes.Tile t: TMTypes.Tile.values()) {
            int nTiles = gs.getPlayerTilesPlaced()[focusPlayer].get(t).getValue();
            Image img = ImageIO.GetInstance().getImage(t.getImagePath());
            drawImage(g, img, startX + k*spacing/2 + k*2*defaultItemSize/3, startY, defaultItemSize/3, defaultItemSize/3);
            drawShadowStringCentered(g, "" + nTiles,
                    new Rectangle(startX + k*spacing/2 + k*2*defaultItemSize/3 + defaultItemSize/3, startY, defaultItemSize/3, defaultItemSize/3));
            k++;
        }

        // Draw milestones with progress, highlight claimed
        k = 0;
        startX = offsetX;
        startY = offsetX*5 + defaultItemSize*2 + defaultItemSize;
        text = "Milestones:";
        drawShadowStringCentered(g, text, new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize/3), null, null, 12);
        startX += metrics.stringWidth(text) + spacing;
        for (Milestone m: gs.getMilestones()) {
            Color color = TMGUI.fontColor;
            if (m.isClaimed()) color = playerColors[m.claimed];

            int progress = m.checkProgress(gs, focusPlayer);
            text = m.getComponentName() + ": " + progress + "/" + m.min;
            drawShadowStringCentered(g, text,
                    new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize / 3), color, null, 12);
            startX += metrics.stringWidth(text) + spacing;
            k++;
        }
        g.setFont(f);

        k = 0;
        startX = offsetX;
        startY = offsetX*6 + defaultItemSize*2 + defaultItemSize*4/3;
        text = "Awards:";
        drawShadowStringCentered(g, text, new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize/3), null, null, 12);
        startX += metrics.stringWidth(text) + spacing;
        for (Award a: gs.getAwards()) {
            Color color = TMGUI.fontColor;
            if (a.isClaimed()) color = playerColors[a.claimed];

            int progress = a.checkProgress(gs, focusPlayer);
            text = a.getComponentName() + ": " + progress;
            drawShadowStringCentered(g, text,
                    new Rectangle(startX, startY, metrics.stringWidth(text), defaultItemSize / 3), color, null, 12);
            startX += metrics.stringWidth(text) + spacing;
            k++;
        }
        g.setFont(f);
    }

    public ArrayList<Rectangle> getHighlight() {
        return highlight;
    }

    public void update(TMGameState gs) {
        this.gs = gs;
    }
}

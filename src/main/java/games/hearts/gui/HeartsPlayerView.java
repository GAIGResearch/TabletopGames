package games.hearts.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.hearts.HeartsGameState;



import java.awt.*;

import static games.hearts.gui.HeartsGUIManager.*;

public class HeartsPlayerView extends HeartsDeckView {
    int playerID;
    int Points;

    int border = 5;
    int borderBottom = 20;

    HeartsDeckView playerHandView;

    public HeartsPlayerView(Deck<FrenchCard>d, int playerID, String dataPath){
        super(d, dataPath,false);
        this.width = playerWidth + border*2;
        this.height = playerHeight + border + borderBottom;
        this.playerID = playerID;
    }

    @Override
    protected void paintComponent(Graphics g){
        drawDeck((Graphics2D) g, new Rectangle(border, border, playerWidth, cardHeight));
        g.setColor(Color.black);
        int gap = 20;
        g.drawString(Points + " points", border+playerWidth/2 - 20, border+cardHeight + 10 + gap);
    }

    @Override
    public Dimension getPreferredSize(){ return new Dimension(width, height); }

    public void update(HeartsGameState hgs){
        Points = hgs.getPlayerPoints(playerID);
        this.setDeck(hgs.getPlayerDecks().get(playerID));
        repaint();
    }


}
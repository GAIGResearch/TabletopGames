package games.hearts.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.hearts.HeartsGameState;



import java.awt.*;

public class HeartsPlayerTrickView extends HeartsDeckView {
    int playerID;
    int Points;

    int border = 5;
    int borderBottom = 20;

    HeartsDeckView playerHandView;

    public HeartsPlayerTrickView(Deck<FrenchCard>d, int playerID, String dataPath){
        super(d, dataPath,false);
        this.width = HeartsGUIManager.playerWidth + border*2;
        this.height = HeartsGUIManager.playerHeight + border + borderBottom;
        this.playerID = playerID;
    }

    @Override
    protected void paintComponent(Graphics g){
        drawDeck((Graphics2D) g, new Rectangle(border, border, HeartsGUIManager.playerWidth, HeartsGUIManager.cardHeight));
        g.setColor(Color.black);
        int gap = 20;
        g.drawString("Trick Deck", border+ HeartsGUIManager.playerWidth/2 - 20, border+ HeartsGUIManager.cardHeight + 10 + gap);
    }

    @Override
    public Dimension getPreferredSize(){ return new Dimension(width, height); }

    public void update(HeartsGameState hgs){
        Points = hgs.getPlayerPoints(playerID);
        this.setDeck(hgs.getPlayerTrickDecks().get(playerID));
        repaint();
    }


}

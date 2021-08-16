package games.blackjack.gui;

import core.components.Deck;
import games.blackjack.BlackjackGameState;
import core.components.FrenchCard;

import java.awt.*;

import static games.blackjack.gui.BlackjackGUIManager.*;

public class BlackjackPlayerView extends BlackjackDeckView{
    int playerID;
    int Points;

    int border = 5;
    int borderBottom = 20;

    public BlackjackPlayerView(Deck<FrenchCard>d, int playerID, String dataPath){
        super(d, dataPath);
        this.width = playerWidth + border*2;
        this.height = playerHeight + border + borderBottom;
        this.playerID = playerID;
    }

    @Override
    protected void paintComponent(Graphics g){
        drawDeck((Graphics2D) g, new Rectangle(border, border, playerWidth, cardHeight));
        g.setColor(Color.black);
        g.drawString(Points + " points", border+playerWidth/2 - 20, border+cardHeight + 10);
    }

    @Override
    public Dimension getPreferredSize(){ return new Dimension(width, height); }

    public void update(BlackjackGameState gamestate){
        this.component = gamestate.getPlayerDecks().get(playerID);
        Points = gamestate.calculatePoints(playerID);
    }
}

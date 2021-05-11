package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

import javax.swing.*;

import java.awt.*;

import static games.sushigo.gui.SGGUI.*;


public class SGPlayerView extends JComponent {

    // ID of player showing
    int playerId;
    // Number of points player has
    int nPoints;
    SGDeckView playerHandView;
    SGCardView playerCardView;
    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;


    int offset = 30;

    SGGameState gs;

    public SGPlayerView(Deck<SGCard> deck, Deck<SGCard> playDeck, int playerId, int humanId, String dataPath)
    {
        this.width = playerAreaWidth + border*20;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new SGDeckView(humanId, deck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.playerCardView = new SGCardView(humanId, playDeck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle r = new Rectangle(5, 100, 60, 85);
        playerHandView.drawDeck((Graphics2D) g);
        //playerCardView.drawDeck((Graphics2D) g);

        //playerCardView.drawComponent((Graphics2D) g, new Rectangle(5, 100, 60, 85), new SGCard(SGCard.SGCardType.Maki_2, 0), true);

        for (int i = 0; i < gs.getPlayerFields().get(playerId).getSize(); i++){
            playerCardView.drawComponent((Graphics2D) g, r, gs.getPlayerFields().get(playerId).get(i), true);

            r.x += offset;
        }



        g.setColor(Color.black);
        g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border + SGCardHeight+10);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(SGGameState gameState)
    {
        gs = gameState;
        playerHandView.updateComponent(gameState.getPlayerDecks().get(playerId));
        playerCardView.updateComponent(gameState.getPlayerFields().get(playerId));
        nPoints = gameState.getPlayerScore()[playerId];
    }
}

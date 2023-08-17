package games.monopolydeal.gui;

import core.components.Deck;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;
import games.uno.UnoGameState;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.monopolydeal.gui.MonopolyDealGUIManager.*;

public class MonopolyDealPlayerHandView extends JComponent {

    int playerId;

    MonopolyDealDeckView playerHandView;

    //Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    public MonopolyDealPlayerHandView(Deck<MonopolyDealCard> d, int playerId, Set<Integer> humanId, String dataPath){
        this.width = playerAreaWidth + border*2;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new MonopolyDealDeckView(playerId, d, true, dataPath, new Rectangle(border,border,playerAreaWidth, MonopolyDealCardHeight ));
    }

    @Override
    protected void paintComponent(Graphics g) {
        playerHandView.drawDeck((Graphics2D) g);
        g.setColor(Color.black);
//        g.drawString(nPoints + " points", border+playerAreaWidth/2 - 20, border+MonopolyDealCardHeight + 10);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(MonopolyDealGameState gameState) {
        playerHandView.updateComponent(gameState.getPlayerHand(playerId));
//        nPoints = gameState.getPlayerScore()[playerId];
    }
}

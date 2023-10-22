package games.monopolydeal.gui;

import core.CoreConstants;
import core.components.Deck;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.cards.MonopolyDealCard;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.monopolydeal.gui.MonopolyDealGUIManager.*;

public class MonopolyDealCardsView extends JComponent {

    int playerId;

    MonopolyDealDeckView playerHandView;

    //Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    public MonopolyDealCardsView(Deck<MonopolyDealCard> d, int playerId, Set<Integer> humanId, String dataPath, MonopolyDealGUIType type){
        switch (type) {
            case Hand:
                this.width = playerAreaWidth + border * 2;
                this.height = (playerAreaHeight + borderBottom * 2)/2;
                this.playerId = playerId;
                this.playerHandView = new MonopolyDealDeckView(humanId.iterator().next(), d, true, dataPath, new Rectangle(border, borderBottom, playerAreaWidth, MonopolyDealCardHeight/2), MonopolyDealCardWidth/2, MonopolyDealCardHeight/2);
                break;
            case Property:
                this.width = MonopolyDealCardWidth/5;
                this.height = MonopolyDealCardHeight/5;
                this.playerId = playerId;
                this.playerHandView = new MonopolyDealDeckView(playerId, d, true, dataPath, new Rectangle(0, 0, MonopolyDealCardWidth/4, MonopolyDealCardHeight/5), MonopolyDealCardWidth/5, MonopolyDealCardHeight/5);
                break;
        }
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
    public void clear(){
        playerHandView.updateComponent(new Deck<MonopolyDealCard>("PropertySet", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));
    }
    public void updateProperty(MonopolyDealGameState gameState, int playerId, int propertyIndex){
        playerHandView.updateComponent(gameState.getPropertySets(playerId)[propertyIndex]);
    }
}

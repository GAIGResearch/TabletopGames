package games.sushigo.gui;

import core.components.Deck;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import javax.swing.*;

import java.awt.*;
import java.util.Set;

import static games.sushigo.gui.SGGUIManager.*;


public class SGPlayerView extends JComponent {

    // ID of player showing
    int playerId;
    // Number of points player has
    SGDeckView playerHandView;
    SGDeckView playedCardsView;
    JLabel pointsText;

    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    SGGameState gs;

    public SGPlayerView(Deck<SGCard> deck, Deck<SGCard> playDeck, int playerId, Set<Integer> humanId, String dataPath)
    {
        this.width = playerAreaWidth + border*20;
        this.height = playerAreaHeight + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new SGDeckView(playerId, deck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.playedCardsView = new SGDeckView(playerId, playDeck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.pointsText = new JLabel(0 + " points");
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(playerHandView);
        add(playedCardsView);
        add(pointsText);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(SGGameState gameState)
    {
        gs = gameState;
        playerHandView.updateComponent(gameState.getPlayerHands().get(playerId));
        playedCardsView.updateComponent(gameState.getPlayedCards().get(playerId));
        this.pointsText.setText(gameState.getPlayerScore()[playerId].getValue() + " points");
    }
}

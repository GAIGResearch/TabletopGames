package games.loveletter.gui;

import core.components.Deck;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.loveletter.cards.LoveLetterCard;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.loveletter.gui.LoveLetterGUIManager.*;


public class LoveLetterPlayerView extends JPanel {

    // ID of player
    int playerId;
    // ID of human looking
    Set<Integer> humanId;
    // Number of points player has
    int nPoints, nPointsWin;

    // Border offsets
    int border = 5;
    int borderBottom = 25;
    int buffer = 10;
    int width, height;

    LoveLetterDeckView handCards;
    LoveLetterDeckView discardCards;

    public LoveLetterPlayerView(Deck<LoveLetterCard> hand, Deck<LoveLetterCard> discard, int playerId, Set<Integer> humanId, String dataPath) {
        JLabel label1 = new JLabel("Player hand:");
        JLabel label2 = new JLabel("Discards:");
        handCards = new LoveLetterDeckView(playerId, hand, false, dataPath,
                new Rectangle(0, 0, playerAreaWidth / 2 - border * 2, llCardHeight));
        discardCards = new LoveLetterDeckView(playerId, discard, true, dataPath,
                new Rectangle(0, 0, playerAreaWidth / 2 - border * 2, llCardHeight));
        JPanel wrap = new JPanel();
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setOpaque(false);
        wrap.add(label1);
        wrap.add(handCards);
        JPanel wrap2 = new JPanel();
        wrap2.setLayout(new BoxLayout(wrap2, BoxLayout.Y_AXIS));
        wrap2.setOpaque(false);
        wrap2.add(label2);
        wrap2.add(discardCards);

        this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.add(wrap);
        this.add(wrap2);

        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + border + borderBottom*2;
        this.humanId = humanId;
        this.playerId = playerId;
    }

    /**
     * Draws the player's hand and their number of points.
     *
     * @param g - Graphics object.
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Draw affection tokens
        g.setColor(Color.black);
        g.drawString(nPoints + "/" + nPointsWin + " affection tokens", border + buffer, llCardHeight + borderBottom*2);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information
     *
     * @param gameState - current game state
     */
    public void update(LoveLetterGameState gameState, boolean front) {
        Deck<LoveLetterCard> hands = gameState.getPlayerHandCards().get(playerId);
        Deck<LoveLetterCard> discards = gameState.getPlayerDiscardCards().get(playerId);
        LoveLetterParameters params = (LoveLetterParameters) gameState.getGameParameters();
        handCards.updateComponent(hands);
        discardCards.updateComponent(discards);

        if (front) {
            handCards.setFront(true);
            this.setFocusable(true);

            String text = "<html>";
            for (LoveLetterCard llc : hands.getComponents()) {
                text += llc.cardType.getCardText(params);
                text += "<br/>";
            }
            text += "</html>";
            handCards.setToolTipText(text);

        } else {
            handCards.setFront(false);
            this.setFocusable(false);

            handCards.setToolTipText("");
        }

        String text = "<html>";
        for (LoveLetterCard llc : discards.getComponents()) {
            text += llc.cardType.getCardText(params);
            text += "<br/>";
        }
        text += "</html>";
        discardCards.setToolTipText(text);

        nPoints = gameState.getAffectionTokens()[playerId];
        nPointsWin = (gameState.getNPlayers() == 2? params.nTokensWin2 : gameState.getNPlayers() == 3? params.nTokensWin3 : params.nTokensWin4);

    }
}

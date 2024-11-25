package games.seasaltpaper.gui;

import core.components.Deck;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import javax.swing.*;
import java.awt.*;

import static games.seasaltpaper.gui.SSPGUIManager.playerAreaHeight;
import static games.seasaltpaper.gui.SSPGUIManager.playerAreaWidth;


public class SSPPlayerView extends JPanel {
    int playerId;
    // Number of points player has
    SSPDeckView playerHandView;
    SSPDeckView playerDiscardView;
    JLabel pointsText;

    // Border offsets
    int border = 5;
    int borderBottom = 20;
    int width, height;

    SeaSaltPaperGameState gs;

    public SSPPlayerView(Deck<SeaSaltPaperCard> playerHand, Deck<SeaSaltPaperCard> playerDiscard, int playerId, String dataPath)
    {
//        this.width = playerAreaWidth + border*20;
//        this.height = playerAreaHeight*2 + border + borderBottom;
        this.playerId = playerId;
        this.playerHandView = new SSPDeckView(playerId, playerHand, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.playerDiscardView = new SSPDeckView(playerId, playerDiscard, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
//        this.pointsText = new JLabel(0 + " points");
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//        this.setLayout(new FlowLayout());

        JPanel wrapperHand = new JPanel();
        wrapperHand.setLayout(new BoxLayout(wrapperHand, BoxLayout.X_AXIS));
//        wrapperHand.add(new JLabel(new ImageIcon(dataPath + "hand.png")));
        wrapperHand.add(playerHandView);
        wrapperHand.setOpaque(false);

        JPanel wrapperPlayed = new JPanel();
        wrapperPlayed.setLayout(new BoxLayout(wrapperPlayed, BoxLayout.X_AXIS));
//        JLabel playedLabel = new JLabel(new ImageIcon(dataPath + "play.png"));
//        wrapperPlayed.add(playedLabel);
        wrapperPlayed.add(playerDiscardView);
        wrapperPlayed.setOpaque(false);

//        playerHandView.setOpaque(false);
//        playerDiscardView.setOpaque(false);
//        pointsText.setOpaque(false);

//        add(playerHandView);
//        add(playerDiscardView);
//        add(pointsText);
        add(wrapperPlayed);
        add(wrapperHand);
        setBackground(Color.WHITE);
    }

//    @Override
//    public Dimension getPreferredSize() {
//        return new Dimension(width, height);
//    }

    public void update(SeaSaltPaperGameState gameState)
    {
        gs = gameState;
        playerHandView.updateComponent(gameState.getPlayerHands().get(playerId));
        playerDiscardView.updateComponent(gameState.getPlayerDiscards().get(playerId));
//        this.pointsText.setText(gameState.getPlayerScore()[playerId].getValue() + " points");
    }
}

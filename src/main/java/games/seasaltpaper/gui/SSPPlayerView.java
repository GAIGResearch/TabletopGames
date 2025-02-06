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

    public SSPPlayerView(SeaSaltPaperGameState gameState, Deck<SeaSaltPaperCard> playerHand, Deck<SeaSaltPaperCard> playerDiscard, int playerId, String dataPath)
    {
        this.gs = gameState;
        this.width = playerAreaWidth + border*20;
        this.height = playerAreaHeight*2 + borderBottom + border + borderBottom;
        this.playerId = playerId;

        this.playerHandView = new SSPDeckView(playerId, playerHand, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));  // todo only visible if player is human or always fully observable
        this.playerDiscardView = new SSPDeckView(playerId, playerDiscard, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.pointsText = new JLabel(0 + " points");
        this.pointsText.setOpaque(false);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(pointsText);
        add(playerHandView);
        add(playerDiscardView);
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        this.pointsText.setText(gs.getGameScore(playerId) + " points");
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}

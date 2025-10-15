package games.gofish.gui;

import core.components.Deck;
import core.components.FrenchCard;
import games.gofish.GoFishGameState;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static games.gofish.gui.GoFishGUIManager.*;

public class GoFishPlayerView extends JComponent {

    int playerId;
    GoFishDeckView playerHandView;
    int booksCount = 0;
    int border = 5;
    int width, height;

    public GoFishPlayerView(Deck<FrenchCard> hand, int playerId, Set<Integer> humanIds) {
        this.playerId = playerId;
        this.width = playerAreaWidth + border * 2;
        this.height = playerAreaHeight + border + 60; // Extra space for books

        // Player hand
        this.playerHandView = new GoFishDeckView(
                playerId,
                hand,
                true,
                null,
                new Rectangle(border, border + 20, playerAreaWidth, cardHeight)
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw hand
        playerHandView.drawDeck((Graphics2D) g);

        // Draw labels
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Hand:", border, border + 15);

        // Draw books info
        g.drawString("Books: " + booksCount, border, height - 25);

        // Draw small book indicators
        g.setColor(new Color(0, 150, 0));
        for (int i = 0; i < booksCount; i++) {
            int x = border + (i * 25);
            int y = height - 20;
            g.fillRect(x, y, 20, 15);
            g.setColor(Color.WHITE);
            g.drawString("" + (i + 1), x + 7, y + 12);
            g.setColor(new Color(0, 150, 0));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(GoFishGameState gameState) {
        playerHandView.updateComponent(gameState.getPlayerHands().get(playerId));

        // Update books count from game state
        Deck<FrenchCard> books = gameState.getPlayerBooks().get(playerId);
        this.booksCount = books.getSize() / 4;

        repaint();
    }
}
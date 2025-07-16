package games.diamant.gui;

import games.diamant.DiamantGameState;
import games.diamant.cards.DiamantCard;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

public class DiamantBoardView extends JComponent {

    static final int CARD_WIDTH = 70;
    static final int CARD_HEIGHT = 100;
    static final int CARD_SPACING = 20;
    static final int PLAYER_CIRCLE_DIAM = 18;
    static final int CAMPSITE_X = 40;
    static final int CAMPSITE_Y = 220;
    static final int CAMPSITE_WIDTH = 120;
    static final int CAMPSITE_HEIGHT = 80;
    static final Color[] PLAYER_COLOURS = new Color[]{Color.GREEN, Color.RED, Color.BLUE, Color.YELLOW, Color.MAGENTA, Color.ORANGE, Color.CYAN, Color.PINK};

    List<DiamantCard> path;
    List<Integer> gemsOnPath;
    List<Integer> playersInCave;
    int nCavesLeft;
    Map<DiamantCard.HazardType, Integer> discardedHazards;
    int nPlayers;

    public DiamantBoardView(DiamantGameState state) {
        setPreferredSize(new Dimension(800, 400));
        update(state);
    }

    public synchronized void update(DiamantGameState state) {
        path = state.getPath().getComponents();
        gemsOnPath = state.getGemsOnPathList();
        playersInCave = new ArrayList<>(state.getPlayersInCave());
        nPlayers = state.getNPlayers();
        int totalCaves = ((games.diamant.DiamantParameters) state.getGameParameters()).nCaves;
        nCavesLeft = totalCaves - state.getRoundCounter();

        // Count discarded hazards by type
        discardedHazards = new EnumMap<>(DiamantCard.HazardType.class);
        for (DiamantCard.HazardType ht : DiamantCard.HazardType.values()) {
            if (ht != DiamantCard.HazardType.None) {
                discardedHazards.put(ht, 0);
            }
        }
        for (int i = 0; i < state.getDiscardDeck().getSize(); i++) {
            DiamantCard c = state.getDiscardDeck().get(i);
            if (c.getCardType() == DiamantCard.DiamantCardType.Hazard) {
                DiamantCard.HazardType ht = c.getHazardType();
                discardedHazards.put(ht, discardedHazards.get(ht) + 1);
            }
        }
        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setFont(new Font("Arial", Font.BOLD, 16));

        // Calculate how many cards fit per row
        int availableWidth = getWidth() - 220; // leave space for campsite and margin
        int cardsPerRow = Math.max(1, availableWidth / (CARD_WIDTH + CARD_SPACING));
        int startX = 200;
        int startY = 60;
        int rowSpacing = 30;

        int pathSize = path.size();
        for (int i = 0; i < pathSize; i++) {
            // The oldest card (last in main deck, index 0 in path) is at the top left, newest at the end
            int cardIdx = pathSize - 1 - i;
            int row = i / cardsPerRow;
            int col = i % cardsPerRow;
            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY + row * (CARD_HEIGHT + rowSpacing);

            // Correct mapping: gemsOnPath is reversed relative to path
            int gemsIdx = gemsOnPath.size() - 1 - cardIdx;
            int gems = (gemsIdx >= 0 && gemsIdx < gemsOnPath.size()) ? gemsOnPath.get(gemsIdx) : 0;

            drawCard(g, path.get(cardIdx), cardX, cardY, gems);

            // Draw player circles on the most recent card (rightmost, last card drawn, i==pathSize-1)
            if (i == pathSize - 1 && pathSize > 0) {
                int circleY = cardY + CARD_HEIGHT + 10;
                int circleX = cardX + 5;
                int offset = 0;
                for (int p = 0; p < nPlayers; p++) {
                    if (playersInCave.contains(p)) {
                        g.setColor(PLAYER_COLOURS[p % PLAYER_COLOURS.length]);
                        g.fillOval(circleX + offset, circleY, PLAYER_CIRCLE_DIAM, PLAYER_CIRCLE_DIAM);
                        g.setColor(Color.BLACK);
                        g.drawOval(circleX + offset, circleY, PLAYER_CIRCLE_DIAM, PLAYER_CIRCLE_DIAM);
                        offset += PLAYER_CIRCLE_DIAM + 4;
                    }
                }
            }
        }
        // Draw campsite area
        g.setColor(new Color(230, 230, 200));
        g.fillRect(CAMPSITE_X, CAMPSITE_Y, CAMPSITE_WIDTH, CAMPSITE_HEIGHT);
        g.setColor(Color.BLACK);
        g.drawRect(CAMPSITE_X, CAMPSITE_Y, CAMPSITE_WIDTH, CAMPSITE_HEIGHT);
        g.drawString("Campsite", CAMPSITE_X + 20, CAMPSITE_Y + 20);
        // Draw player circles in campsite for those not in cave
        int campCircleX = CAMPSITE_X + 10;
        int campCircleY = CAMPSITE_Y + 35;
        int campOffset = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (!playersInCave.contains(p)) {
                g.setColor(PLAYER_COLOURS[p % PLAYER_COLOURS.length]);
                g.fillOval(campCircleX + campOffset, campCircleY, PLAYER_CIRCLE_DIAM, PLAYER_CIRCLE_DIAM);
                g.setColor(Color.BLACK);
                g.drawOval(campCircleX + campOffset, campCircleY, PLAYER_CIRCLE_DIAM, PLAYER_CIRCLE_DIAM);
                campOffset += PLAYER_CIRCLE_DIAM + 4;
            }
        }
        // Draw caves left
        g.setColor(Color.BLACK);
        g.drawString("Caves left: " + nCavesLeft, 30, 40);

        // Draw discarded hazards
        g.drawString("Discarded Hazards:", 30, 340);
        int hazardY = 360;
        int hazardX = 30;
        for (DiamantCard.HazardType ht : DiamantCard.HazardType.values()) {
            if (ht == DiamantCard.HazardType.None) continue;
            g.drawString(ht.name() + ": " + discardedHazards.get(ht), hazardX, hazardY);
            hazardY += 20;
        }
    }

    private void drawCard(Graphics2D g, DiamantCard card, int x, int y, int gemsOnCard) {
        // Card background
        if (card.getCardType() == DiamantCard.DiamantCardType.Relic) {
            g.setColor(new Color(255, 215, 0)); // gold
        } else if (card.getCardType() == DiamantCard.DiamantCardType.Hazard) {
            g.setColor(new Color(255, 180, 180)); // light red for hazards
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12);

        // Card type and value
        if (card.getCardType() == DiamantCard.DiamantCardType.Treasure) {
            g.setColor(new Color(220, 180, 60));
            g.fillOval(x + 10, y + 10, 20, 20);
            g.setColor(Color.BLACK);
            g.drawString("" + card.getValue(), x + 14, y + 25);
        } else if (card.getCardType() == DiamantCard.DiamantCardType.Relic) {
            String label = "Relic";
            g.setColor(Color.BLACK);
            // Draw "Relic" label at the top, centered
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g.drawString(label, x + (CARD_WIDTH - labelWidth) / 2, y + 30);
            // Draw value centered in the card
            String valueStr = String.valueOf(card.getValue());
            int valueWidth = fm.stringWidth(valueStr);
            g.drawString(valueStr, x + (CARD_WIDTH - valueWidth) / 2, y + CARD_HEIGHT / 2 + fm.getAscent() / 2);
        } else if (card.getCardType() == DiamantCard.DiamantCardType.Hazard) {
            String label = card.getHazardType().name();
            g.setColor(Color.RED.darker());
            g.drawString(label, x + 10, y + 25);
        }
        // Gems on path (not card value, but gems left on this card)
        g.setColor(Color.BLUE);
        g.drawString("Gems: " + gemsOnCard, x + 10, y + CARD_HEIGHT - 15);
    }
}

package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressParameters;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import utilities.ImageIO;
import utilities.Utils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;
import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressPlayerView extends JComponent {

    // Width and height of display area
    int width, height;
    // ID of player this is showing
    int playerId;

    // Hand of player
    ColtExpressDeckView<ColtExpressCard> playerHand;
    // Loot of player
    ColtExpressDeckView<Loot> playerLoot;
    // Character of player
    ColtExpressTypes.CharacterType playerCard;
    // Deck of player
    Deck<ColtExpressCard> playerDeck;
    // Bullets left for player
    int bulletsLeft;

    // Path to assets
    String dataPath;
    // Image for back of cards
    Image cardBack;

    // End of game sum of points gained
    int lootSum = -1;
    // True if this player was the best shooter
    boolean bestShooter;
    // Shooter award, added to total points
    int shooterReward;
    // True if game has ended (showing end game stats)
    boolean gameEnd;

    public ColtExpressPlayerView(int playerId, String dataPath,
                                 HashMap<Integer, ColtExpressTypes.CharacterType> characters) {
        this.playerId = playerId;
        this.dataPath = dataPath;
        this.width = playerAreaWidth;
        this.height = playerAreaHeight;
        playerHand = new ColtExpressDeckView(null, true, dataPath, characters);
        playerLoot = new ColtExpressDeckView(null, false, dataPath, characters);
        playerCard = characters.get(playerId);
        cardBack = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }

    /**
     * Draws player character card, loot, bullets left, points total (on game end, plus a * symbol if best shooter),
     * player deck and player hand.
     * @param g - Graphics object.
     */
    protected void paintComponent(Graphics g) {
        // Draw player card
        Image card = ImageIO.GetInstance().getImage(dataPath + "characters/" + playerCard.name() + "Card.png");
        g.drawImage(card, 0, 0, ceCardWidth, ceCardHeight, null);
        g.setColor(Color.black);
        g.drawString(playerCard.name(), ceCardWidth/2-20, ceCardHeight-5);

        // Draw loot, bullets left, points total if game end
        playerLoot.drawDeck((Graphics2D) g, new Rectangle(ceCardWidth + 10, 0, defaultItemSize*2, defaultItemSize), false);
        g.setColor(Color.black);
        g.drawString("Bullets left: " + bulletsLeft, ceCardWidth + defaultItemSize*2 + 15, ceCardHeight*2/3);
        if (gameEnd) {
            if (lootSum == -1) {
                lootSum = 0;
                for (Loot loot: ((Deck<Loot>)playerLoot.getComponent()).getComponents()) {
                    lootSum += loot.getValue();
                }
                if (bestShooter) {
                    lootSum += shooterReward;
                }
            }
            String endResult = "";
            if (bestShooter) {
                endResult += "* ";
            }
            endResult += "Total points: " + lootSum;
            g.drawString(endResult, ceCardWidth + defaultItemSize*2 + 15, ceCardHeight/3);
        }

        // Draw player deck
        g.drawImage(cardBack, 0, ceCardHeight + 5, ceCardWidth, ceCardHeight, null);
        g.drawString("" + playerDeck.getSize(), 10, ceCardHeight*2 - 5);

        // Draw player hand
        playerHand.drawDeck((Graphics2D) g, new Rectangle(ceCardWidth + 30, ceCardHeight + 5, width-ceCardWidth*2-30, ceCardHeight), false);
    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Updates information based on current game state.
     * @param gameState - current game state.
     * @param humanID - ID of human player
     */
    public void update(ColtExpressGameState gameState, int humanID) {
        playerDeck = gameState.getPlayerDecks().get(playerId);
        playerHand.updateComponent(gameState.getPlayerHandCards().get(playerId));
        playerLoot.updateComponent(gameState.getLoot(playerId));
        if (gameState.getGameStatus() == Utils.GameResult.GAME_END && !gameEnd) {
            gameEnd = true;
            playerLoot.setFront(true);
            bestShooter = gameState.getBestShooters().contains(playerId);
            shooterReward = ((ColtExpressParameters)gameState.getGameParameters()).shooterReward;
        }
        bulletsLeft = gameState.getBulletsLeft()[playerId];

        if (playerId == gameState.getCurrentPlayer() && ALWAYS_DISPLAY_CURRENT_PLAYER
                || playerId == humanID
                || ALWAYS_DISPLAY_FULL_OBSERVABLE) {
            playerHand.setFront(true);
        } else {
            playerHand.setFront(false);
        }
    }
}

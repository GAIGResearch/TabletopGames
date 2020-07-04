package games.coltexpress.gui;

import core.components.Deck;
import games.coltexpress.ColtExpressGameState;
import games.coltexpress.ColtExpressTypes;
import games.coltexpress.cards.ColtExpressCard;
import games.coltexpress.components.Loot;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

import static core.CoreConstants.ALWAYS_DISPLAY_CURRENT_PLAYER;
import static core.CoreConstants.ALWAYS_DISPLAY_FULL_OBSERVABLE;
import static games.coltexpress.gui.ColtExpressGUI.*;

public class ColtExpressPlayerView extends JComponent {

    int width, height;
    int playerId;

    ColtExpressDeckView<ColtExpressCard> playerHand;
    ColtExpressDeckView<Loot> playerLoot;
    ColtExpressTypes.CharacterType playerCard;
    Deck<ColtExpressCard> playerDeck;
    int bulletsLeft;
    String dataPath;

    Image cardBack;

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


    protected void paintComponent(Graphics g) {
        // Draw player card + loot + bullets left on same line
        Image card = ImageIO.GetInstance().getImage(dataPath + "characters/" + playerCard.name() + "Card.png");
        g.drawImage(card, 0, 0, ceCardWidth, ceCardHeight, null);
        g.setColor(Color.black);
        g.drawString(playerCard.name(), ceCardWidth/2-20, ceCardHeight-5);

        // Draw loot, bullets left
        playerLoot.drawDeck((Graphics2D) g, new Rectangle(ceCardWidth + 10, 0, defaultItemSize*2, defaultItemSize));
        g.setColor(Color.black);
        g.drawString("Bullets left: " + bulletsLeft, ceCardWidth + defaultItemSize*2 + 15, ceCardHeight*2/3);

        // Draw player deck
        g.drawImage(cardBack, 0, ceCardHeight + 5, ceCardWidth, ceCardHeight, null);
        g.drawString("" + playerDeck.getSize(), 10, ceCardHeight*2 - 5);

        // Draw player hand
        playerHand.drawDeck((Graphics2D) g, new Rectangle(ceCardWidth + 20, ceCardHeight + 5, width-ceCardWidth*2-20, ceCardHeight));

    }

    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public void update(ColtExpressGameState gameState, int humanID) {
        playerDeck = gameState.getPlayerDecks().get(playerId);
        playerHand.updateComponent(gameState.getPlayerHandCards().get(playerId));
        playerLoot.updateComponent(gameState.getLoot(playerId));
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

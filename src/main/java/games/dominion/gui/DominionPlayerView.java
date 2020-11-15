package games.dominion.gui;
import games.dominion.*;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import static games.dominion.gui.DominionGUI.*;

public class DominionPlayerView extends JComponent {

    DominionDeckView playerHand;
    DominionDeckView playerDiscard;
    DominionDeckView playerDraw;
    DominionDeckView playerTableau;

    // Width and height of display area
    int width, height;
    // ID of player this is showing
    int playerId;

    int actions;
    int buys;
    int spendAvailable;

    // Path to assets
    String dataPath;
    // Image for back of cards
    Image cardBack;

    // Border offsets
    int border = 5;
    int borderBottom = 20;

    public DominionPlayerView(int playerId, String dataPath) {
        this.playerId = playerId;
        this.dataPath = dataPath;
        this.width = playerAreaWidth + border*2;
        this.height = playerAreaHeight + borderBottom + border;
        playerHand = new DominionDeckView(null, true, dataPath);
        playerDiscard = new DominionDeckView(null, true, dataPath);
        playerDraw = new DominionDeckView(null, false, dataPath);
        playerTableau = new DominionDeckView(null, true, dataPath);

        cardBack = ImageIO.GetInstance().getImage(dataPath + "CardBack.png");
    }
}

package games.resistance.gui;

import core.AbstractGameState;
import core.components.Deck;
import core.properties.PropertyString;
import games.pandemic.PandemicGameState;
import games.resistance.ResGameState;
import games.resistance.components.ResPlayerCards;
import gui.views.CardView;
import gui.views.DeckView;
import utilities.ImageIO;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static core.CoreConstants.imgHash;
import static games.resistance.gui.ResGUIManager.ResPlayerCardsHeight;
import static games.resistance.gui.ResGUIManager.ResPlayerCardsWidth;

public class ResBoardView extends JComponent {


    private Image backgroundImage;


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int imgWidth = backgroundImage.getWidth(this);
        int imgHeight = backgroundImage.getHeight(this);

        // Calculate the position to center the image
        int x = (getWidth() - imgWidth) / 2;
        int y = (getHeight() - imgHeight) / 2;

        g.drawImage(backgroundImage, x, y, imgWidth, imgHeight, this);
    }
}

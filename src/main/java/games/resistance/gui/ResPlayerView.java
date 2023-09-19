package games.resistance.gui;

import core.components.Deck;
import games.resistance.ResGameState;
import games.resistance.components.ResPlayerCards;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

import javax.swing.*;
import java.awt.*;

import static games.resistance.gui.ResGUIManager.playerAreaHeight;
import static games.resistance.gui.ResGUIManager.playerAreaWidth;


public class ResPlayerView extends JComponent {


    int playerId;

    ResDeckView playerHandView;

    JLabel pointsText;


    // Border offsets
    int border = 5;

    int width, height;

    ResGameState gs;

    public ResPlayerView(Deck<ResPlayerCards> deck, int playerId, int humanId, String dataPath)
    {
        this.width = playerAreaWidth;
        this.height = playerAreaHeight;
        this.playerId = playerId;
        this.playerHandView = new ResDeckView(humanId, deck, true, dataPath, new Rectangle(border, border, playerAreaWidth, playerAreaHeight));
        this.pointsText = new JLabel(0 + " points");

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(playerHandView);
        add(pointsText);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    public void update(ResGameState gameState)
    {
        gs = gameState;
        playerHandView.updateComponent(gameState.getPlayerHandCards().get(playerId));

    }
}

package games.resistance.gui;

import games.resistance.components.ResPlayerCards;
import core.components.Deck;
import games.resistance.ResGameState;

import javax.swing.*;
import java.awt.*;


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
        this.width = ResGUIManager.playerAreaWidth;
        this.height = ResGUIManager.playerAreaHeight;
        this.playerId = playerId;
        this.playerHandView = new ResDeckView(humanId, deck, true, dataPath, new Rectangle(border, border, ResGUIManager.playerAreaWidth, ResGUIManager.playerAreaHeight));
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

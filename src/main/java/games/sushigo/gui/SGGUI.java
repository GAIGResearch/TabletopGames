package games.sushigo.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class SGGUI extends AbstractGUI {
    public SGGUI(Game game, ActionController ac) {
        super(ac, 15);
        //Set width and hight
        this.width = 1280;
        this.height = 720;

        if(game != null)
        {
            AbstractGameState gameState = game.getGameState();
            if(gameState != null)
            {
                JPanel mainGameArea = new JPanel();
                mainGameArea.setLayout(new BorderLayout());

                // Top area will show state information
                JPanel infoPanel = createGameStateInfoPanel("Sushi GO", gameState, width, defaultInfoPanelHeight);
                // Bottom area will show actions available
                JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight, false);

                // Add all views to frame
                getContentPane().add(mainGameArea, BorderLayout.CENTER);
                getContentPane().add(infoPanel, BorderLayout.NORTH);
                getContentPane().add(actionPanel, BorderLayout.SOUTH);
            }

        }

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if(gameState != null)
        {
            // Update actions
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }
}

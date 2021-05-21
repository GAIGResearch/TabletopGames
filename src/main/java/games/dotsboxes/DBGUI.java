package games.dotsboxes;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import gui.ScreenHighlight;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;

public class DBGUI extends AbstractGUI {
    DBGridBoardView view;

    public DBGUI(AbstractGameState gameState, ActionController ac) {
        this(gameState, ac, defaultDisplayWidth, defaultDisplayHeight);
    }

    public DBGUI(AbstractGameState gameState, ActionController ac,
                 int displayWidth, int displayHeight) {
        super(ac, 100);
        this.width = displayWidth;
        this.height = displayHeight;

        view = new DBGridBoardView(((DBGameState)gameState));

        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, true);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState, boolean actionTaken) {
        if (gameState != null) {
            view.updateGameState(((DBGameState)gameState));
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

}

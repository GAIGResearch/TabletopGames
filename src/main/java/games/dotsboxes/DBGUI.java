package games.dotsboxes;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.components.Component;
import core.components.Deck;
import games.GameType;
import gui.views.AreaView;
import gui.views.CardView;
import gui.views.ComponentView;
import players.human.ActionController;
import players.human.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class DBGUI extends AbstractGUI {
    DBGridBoardView view;
    int width, height;

    public DBGUI(AbstractGameState gameState, ActionController ac) {
        this(gameState, ac, defaultDisplayWidth, defaultDisplayHeight);
    }

    public DBGUI(AbstractGameState gameState, ActionController ac,
                 int displayWidth, int displayHeight) {
        super(ac, 100);
        this.width = displayWidth;
        this.height = displayHeight;

        view = new DBGridBoardView(((DBGameState)gameState).grid);

        JPanel infoPanel = createGameStateInfoPanel("Dots and Boxes", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            view.updateComponent(((DBGameState)gameState).grid);
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20);
    }
}

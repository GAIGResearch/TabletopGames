package gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import games.GameType;
import players.ActionController;
import players.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class PrototypeGUI extends AbstractGUI {
    JComponent view;
    int width = 500, height = 400, actionPanelHeight = 200;

    public PrototypeGUI(GameType game, AbstractGameState gameState, ActionController ac, int maxActionSpace) {
        super(ac, maxActionSpace);

        if (gameState != null) {
            view = new AreaView(gameState.getAllComponents(), width, height);
        } else {
            view = new JPanel();
        }
        JPanel infoPanel = new JPanel();
        if (game != null && gameState != null) {
            infoPanel = createGameStateInfoPanel(game.name(), gameState);
        }
        JComponent actionPanel = createActionPanel(new Collection[0], width, actionPanelHeight);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (view instanceof AreaView) {
                ((AreaView) view).updateArea(gameState.getAllComponents());
            } else {
                view = new AreaView(gameState.getAllComponents(), width, height);
            }
            if (player instanceof HumanGUIPlayer) {
                updateActionButtons(player, gameState);
            }
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height + actionPanelHeight + 100);
    }
}

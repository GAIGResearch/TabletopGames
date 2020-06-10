package games.descent.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.components.Component;
import core.components.Deck;
import games.descent.DescentGameState;
import gui.views.AreaView;
import gui.views.CardView;
import gui.views.ComponentView;
import players.ActionController;
import players.HumanGUIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class DescentGUI extends AbstractGUI {
    JComponent view;
    int width, height;

    public DescentGUI(AbstractGameState gameState, ActionController ac) {
        super(ac, 1);  // TODO: calculate/approximate max action space

        DescentGameState dgs = (DescentGameState) gameState;

        if (gameState != null) {
            view = new DescentGridBoardView(dgs.getMasterBoard(), dgs.getMasterGraph());
            width = view.getPreferredSize().width;
            height = view.getPreferredSize().height;
        } else {
            view = new JPanel();
        }
        JPanel infoPanel = createGameStateInfoPanel("Descent", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new Collection[0], width, defaultActionPanelHeight);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(infoPanel);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(actionPanel, BorderLayout.SOUTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        if (gameState != null) {
            if (view instanceof AreaView) {
                ((AreaView) view).updateComponent(gameState.getAllComponents());
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
        return new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight);
    }
}

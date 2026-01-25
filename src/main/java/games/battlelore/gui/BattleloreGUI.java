package games.battlelore.gui;

import gui.AbstractGUIManager;
import gui.GamePanel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.battlelore.BattleloreGameState;
import gui.IScreenHighlight;
import players.human.ActionController;
import javax.swing.*;
import java.awt.*;
import java.util.Set;

public class BattleloreGUI extends AbstractGUIManager {
    BattleloreBoardView view;

    public BattleloreGUI(GamePanel parent, Game game, ActionController ac, Set<Integer> humanId) {
        super(parent, game, ac, humanId);

        if (game == null) {
            return;
        }

        BattleloreGameState gameState = (BattleloreGameState) game.getGameState();
        view = new BattleloreBoardView(gameState.getBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize *  gameState.getBoard().getWidth() * 2);
        this.height = defaultItemSize * gameState.getBoard().getHeight() * 2;


        JPanel infoPanel = createGameStateInfoPanel("Battlelore", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanelOpaque(new IScreenHighlight[0], width, defaultActionPanelHeight, false);

        parent.setLayout(new BorderLayout());
        parent.add(view, BorderLayout.CENTER);
        parent.add(infoPanel, BorderLayout.NORTH);
        parent.add(actionPanel, BorderLayout.SOUTH);
        parent.setPreferredSize(new Dimension(width, height + defaultActionPanelHeight + defaultInfoPanelHeight + defaultCardHeight + 20));
        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

    }

    @Override
    public int getMaxActionSpace() {
        return 25;
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        view.updateComponent(((BattleloreGameState)gameState).getBoard());
    }
}

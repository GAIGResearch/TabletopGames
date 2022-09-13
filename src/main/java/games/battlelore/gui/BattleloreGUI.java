package games.battlelore.gui;

import gui.AbstractGUIManager;
import gui.GamePanel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.battlelore.BattleloreGameState;
import gui.ScreenHighlight;
import players.human.ActionController;
import javax.swing.*;
import java.awt.*;

public class BattleloreGUI extends AbstractGUIManager {
    BattleloreBoardView view;

    public BattleloreGUI(GamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 25);

        if (game == null) {
            return;
        }

        BattleloreGameState gameState = (BattleloreGameState) game.getGameState();
        view = new BattleloreBoardView(gameState.getBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize *  gameState.getBoard().getWidth() * 2);
        this.height = defaultItemSize * gameState.getBoard().getHeight() * 2;


        JPanel infoPanel = createGameStateInfoPanel("Battlelore", gameState, width, defaultInfoPanelHeight);
        JComponent actionPanel = createActionPanel(new ScreenHighlight[0], width, defaultActionPanelHeight, false);

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
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        view.updateComponent(((BattleloreGameState)gameState).getBoard());
    }
}

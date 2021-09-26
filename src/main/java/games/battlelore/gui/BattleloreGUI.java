package games.battlelore.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.battlelore.BattleloreGameState;
import players.human.ActionController;
import javax.swing.*;
import java.awt.*;

public class BattleloreGUI extends AbstractGUI {
    BattleloreBoardView view;

    public BattleloreGUI(Game game, ActionController ac) {
        super(ac, 25);

        if (game == null) {
            return;
        }

        BattleloreGameState gameState = (BattleloreGameState) game.getGameState();
        view = new BattleloreBoardView(gameState.getBoard());

        // Set width/height of display
        this.width = defaultItemSize * gameState.getBoard().getWidth() * 2;
        this.height = defaultItemSize * gameState.getBoard().getHeight() * 2;

        JPanel infoPanel = createGameStateInfoPanel("Battlelore", gameState, width, defaultInfoPanelHeight);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        view.updateComponent(((BattleloreGameState)gameState).getBoard());
        repaint();
    }
}

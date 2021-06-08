package games.battlelore.gui;

import core.AbstractGUI;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import games.battlelore.BattleloreGameState;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.gui.TTTBoardView;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;

public class BattleloreGUI extends AbstractGUI
{
    BattleloreBoardView view;

    public BattleloreGUI(Game game, ActionController ac)
    {
        super(ac, 1);
        if (game == null)
        {
            return;
        }

        BattleloreGameState gameState = (BattleloreGameState) game.getGameState();
        view = new BattleloreBoardView(gameState.getBoard());

        // Set width/height of display
        this.width = defaultItemSize * gameState.getBoard().getWidth();
        this.height = defaultItemSize * gameState.getBoard().getHeight();

        JPanel infoPanel = createGameStateInfoPanel("Battlelore", gameState, width, defaultInfoPanelHeight);

        getContentPane().add(view, BorderLayout.CENTER);
        getContentPane().add(infoPanel, BorderLayout.NORTH);

        setFrameProperties();
    }

    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState)
    {

    }
}

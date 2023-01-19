package games.tictactoe.gui;

import core.Game;
import games.tictactoe.TicTacToeGameState;
import glgui.GLGUIManager;
import glgui.GLGamePanel;
import players.human.ActionController;

public class TicTacToeGLGUIManager extends GLGUIManager {


    TTTGLBoardView boardView;

    public TicTacToeGLGUIManager(GLGamePanel parent, Game game, ActionController ac) {
        super(parent, ac, 1);
        if (game == null) return;

        TicTacToeGameState gameState = (TicTacToeGameState) game.getGameState();
        boardView = new TTTGLBoardView(gameState.getGridBoard());

        // Set width/height of display
        this.width = Math.max(defaultDisplayWidth, defaultItemSize * gameState.getGridBoard().getWidth());
        this.height = defaultItemSize * gameState.getGridBoard().getHeight();
    }
}

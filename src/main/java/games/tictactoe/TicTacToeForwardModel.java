package games.tictactoe;

import core.actions.IAction;
import core.components.Grid;
import core.AbstractGameState;
import core.ForwardModel;
import utilities.Utils;


public class TicTacToeForwardModel extends ForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        ((TicTacToeGameState)firstState).grid = new Grid<>(tttgp.gridWidth, tttgp.gridHeight, ' ');
    }

    @Override
    public void next(AbstractGameState currentState, IAction action) {
        action.execute(currentState);
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) currentState.getGameParameters();
        if (currentState.getTurnOrder().getRoundCounter() == (tttgp.gridWidth * tttgp.gridHeight)) {
            currentState.setGameStatus(Utils.GameResult.GAME_END);
        }

        checkGameEnd((TicTacToeGameState) currentState);
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    /**
     * Checks if the game ended.
     * // TODO: very hard-coded, should work for other value for widths/heights of grid.
     * @param gameState - game state to check game end.
     */
    private void checkGameEnd(TicTacToeGameState gameState){
        Grid<Character> grid = gameState.getGrid();

        //check rows
        for (int x = 0; x < grid.getWidth(); x++){
            if (grid.getElement(x, 0).equals(grid.getElement(x, 1)) &&
                    grid.getElement(x, 0).equals(grid.getElement(x, 2)) &&
                    !grid.getElement(x, 0).equals(' ')){
                gameState.registerWinner(grid.getElement(x, 0));
                return;
            }
        }

        // check columns
        for (int y = 0; y < grid.getHeight(); y++){
            if (grid.getElement(0, y).equals(grid.getElement(1, y)) &&
                    grid.getElement(0, y).equals(grid.getElement(2, y)) &&
                    !grid.getElement(0, y).equals(' ')){
                gameState.registerWinner(grid.getElement(0, y));
                return;
            }
        }

        //check diagonals
        if (grid.getElement(0, 0).equals(grid.getElement(1, 1)) &&
                grid.getElement(0, 0).equals(grid.getElement(2, 2)) &&
                !grid.getElement(1, 1).equals(' ')){
            gameState.registerWinner(grid.getElement(1, 1));
            return;
        }

        if (grid.getElement(0, 2).equals(grid.getElement(1, 1)) &&
                grid.getElement(0, 2).equals(grid.getElement(2, 0)) &&
                !grid.getElement(0, 2).equals(' ')){
            gameState.registerWinner(grid.getElement(1, 1));
        }
    }
}

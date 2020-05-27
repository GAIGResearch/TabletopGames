package games.tictactoe;

import core.actions.AbstractAction;
import core.components.GridBoard;
import core.AbstractGameState;
import core.AbstractForwardModel;
import utilities.Utils;


public class TicTacToeForwardModel extends AbstractForwardModel {

    @Override
    public void setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        ((TicTacToeGameState)firstState).gridBoard = new GridBoard<>(tttgp.gridWidth, tttgp.gridHeight, Character.class, ' ');
    }

    @Override
    public void next(AbstractGameState currentState, AbstractAction action) {
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
        GridBoard<Character> gridBoard = gameState.getGridBoard();

        //check rows
        for (int x = 0; x < gridBoard.getWidth(); x++){
            if (gridBoard.getElement(x, 0).equals(gridBoard.getElement(x, 1)) &&
                    gridBoard.getElement(x, 0).equals(gridBoard.getElement(x, 2)) &&
                    !gridBoard.getElement(x, 0).equals(' ')){
                gameState.registerWinner(gridBoard.getElement(x, 0));
                return;
            }
        }

        // check columns
        for (int y = 0; y < gridBoard.getHeight(); y++){
            if (gridBoard.getElement(0, y).equals(gridBoard.getElement(1, y)) &&
                    gridBoard.getElement(0, y).equals(gridBoard.getElement(2, y)) &&
                    !gridBoard.getElement(0, y).equals(' ')){
                gameState.registerWinner(gridBoard.getElement(0, y));
                return;
            }
        }

        //check diagonals
        if (gridBoard.getElement(0, 0).equals(gridBoard.getElement(1, 1)) &&
                gridBoard.getElement(0, 0).equals(gridBoard.getElement(2, 2)) &&
                !gridBoard.getElement(1, 1).equals(' ')){
            gameState.registerWinner(gridBoard.getElement(1, 1));
            return;
        }

        if (gridBoard.getElement(0, 2).equals(gridBoard.getElement(1, 1)) &&
                gridBoard.getElement(0, 2).equals(gridBoard.getElement(2, 0)) &&
                !gridBoard.getElement(0, 2).equals(' ')){
            gameState.registerWinner(gridBoard.getElement(1, 1));
        }
    }
}

package games.tictactoe;

import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.AbstractGameState;
import core.ForwardModel;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends ForwardModel {

    /**
     * Creates a new FM object with a given random seed.
     *
     * @param seed - random seed or this forward model.
     */
    protected TicTacToeForwardModel(long seed) {
        super(seed);
    }

    @Override
    public void setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        ((TicTacToeGameState)firstState).gridBoard = new GridBoard<>(tttgp.gridWidth, tttgp.gridHeight, Character.class, ' ');
    }

    @Override
    public List<AbstractAction> computeAvailableActions(AbstractGameState gameState) {
        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);

        for (int x = 0; x < tttgs.gridBoard.getWidth(); x++){
            for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                if (tttgs.gridBoard.getElement(x, y) == ' ')
                    actions.add(new SetGridValueAction(tttgs.gridBoard.getComponentID(), x, y, player == 0 ? 'x' : 'o'));
            }
        }
        return actions;
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
                registerWinner(gameState, gridBoard.getElement(x, 0));
                return;
            }
        }

        // check columns
        for (int y = 0; y < gridBoard.getHeight(); y++){
            if (gridBoard.getElement(0, y).equals(gridBoard.getElement(1, y)) &&
                    gridBoard.getElement(0, y).equals(gridBoard.getElement(2, y)) &&
                    !gridBoard.getElement(0, y).equals(' ')){
                registerWinner(gameState, gridBoard.getElement(0, y));
                return;
            }
        }

        //check diagonals
        if (gridBoard.getElement(0, 0).equals(gridBoard.getElement(1, 1)) &&
                gridBoard.getElement(0, 0).equals(gridBoard.getElement(2, 2)) &&
                !gridBoard.getElement(1, 1).equals(' ')){
            registerWinner(gameState, gridBoard.getElement(1, 1));
            return;
        }

        if (gridBoard.getElement(0, 2).equals(gridBoard.getElement(1, 1)) &&
                gridBoard.getElement(0, 2).equals(gridBoard.getElement(2, 0)) &&
                !gridBoard.getElement(0, 2).equals(' ')){
            registerWinner(gameState, gridBoard.getElement(1, 1));
        }
    }

    @Override
    public void endGame(AbstractGameState gameState) {
        gameState.setGameStatus(Utils.GameResult.GAME_DRAW);
        Arrays.fill(gameState.getPlayerResults(), Utils.GameResult.GAME_DRAW);
    }

    /**
     * Inform the game this player has won.
     * @param winnerSymbol - which player won.
     */
    public void registerWinner(TicTacToeGameState gameState, char winnerSymbol){
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        if (winnerSymbol == 'o'){
            gameState.setPlayerResult(Utils.GameResult.GAME_WIN, 1);
            gameState.setPlayerResult(Utils.GameResult.GAME_LOSE, 0);
        } else {
            gameState.setPlayerResult(Utils.GameResult.GAME_WIN, 0);
            gameState.setPlayerResult(Utils.GameResult.GAME_LOSE, 1);
        }
    }

}

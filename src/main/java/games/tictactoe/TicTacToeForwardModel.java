package games.tictactoe;

import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.AbstractGameState;
import core.AbstractForwardModel;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends AbstractForwardModel {

    /**
     * Creates a new FM object with a given random seed.
     *
     * @param seed - random seed or this forward model.
     */
    protected TicTacToeForwardModel(long seed) {
        super(seed);
    }

    /**
     * Constructor for copies, leaves random generator null.
     */
    public TicTacToeForwardModel() { }

    @Override
    public void setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        ((TicTacToeGameState)firstState).gridBoard = new GridBoard<>(tttgp.gridSize, tttgp.gridSize, Character.class, ' ');
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
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
    protected AbstractForwardModel getCopy() {
        return new TicTacToeForwardModel();
    }

    @Override
    public void next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) currentState.getGameParameters();
        if (currentState.getTurnOrder().getRoundCounter() == (tttgp.gridSize * tttgp.gridSize)) {
            currentState.setGameStatus(Utils.GameResult.GAME_END);
            return;
        }

        if (checkGameEnd((TicTacToeGameState) currentState)) {
            return;
        }
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    /**
     * Checks if the game ended.
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(TicTacToeGameState gameState){
        GridBoard<Character> gridBoard = gameState.getGridBoard();

        // Check columns
        for (int x = 0; x < gridBoard.getWidth(); x++){
            Character c = gridBoard.getElement(x, 0);
            if (c != ' ') {
                boolean win = true;
                for (int y = 1; y < gridBoard.getHeight(); y++) {
                    if (gridBoard.getElement(x, y) != c) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    registerWinner(gameState, c);
                    return true;
                }
            }
        }

        // Check rows
        for (int y = 0; y < gridBoard.getHeight(); y++){
            Character c = gridBoard.getElement(0, y);
            if (c != ' ') {
                boolean win = true;
                for (int x = 1; x < gridBoard.getWidth(); x++) {
                    if (gridBoard.getElement(x, y) != c) {
                        win = false;
                        break;
                    }
                }
                if (win) {
                    registerWinner(gameState, c);
                    return true;
                }
            }
        }

        // Check diagonals
        // Primary
        Character c = gridBoard.getElement(0, 0);
        if (c != ' ') {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                if (gridBoard.getElement(i, i) != c) {
                    win = false;
                }
            }
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }

        // Secondary
        c = gridBoard.getElement(gridBoard.getWidth()-1, 0);
        if (c != ' ') {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                if (gridBoard.getElement(gridBoard.getWidth()-1-i, i) != c) {
                    win = false;
                }
            }
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }
        boolean tie = true;
        for (Character[] row: gridBoard.getGridValues()){
            for (Character field: row){
                if (field.equals(' ')){
                    tie = false;
                }
            }
        }
        if (tie){
            gameState.setGameStatus(Utils.GameResult.GAME_DRAW);
            Arrays.fill(gameState.getPlayerResults(), Utils.GameResult.GAME_DRAW);
        }

        return tie;
    }

    @Override
    public void endGame(AbstractGameState gameState) {
        System.out.println(Arrays.toString(gameState.getPlayerResults()));
    }

    /**
     * Inform the game this player has won.
     * @param winnerSymbol - which player won.
     */
    public void registerWinner(TicTacToeGameState gameState, char winnerSymbol){
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        int winningPlayer = gameState.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(Utils.GameResult.GAME_WIN, winningPlayer);
        gameState.setPlayerResult(Utils.GameResult.GAME_LOSE, 1-winningPlayer);
    }
}

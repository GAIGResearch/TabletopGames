package games.tictactoe;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.components.Token;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        int gridSize = tttgp.gridSize;
        TicTacToeGameState state = (TicTacToeGameState) firstState;
        state.gridBoard = new GridBoard<>(gridSize, gridSize, new Token(TicTacToeConstants.emptyCell));
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);

        if (gameState.isNotTerminal())
            for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
                for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                    if (tttgs.gridBoard.getElement(x, y).getTokenType().equals(TicTacToeConstants.emptyCell))
                        actions.add(new SetGridValueAction<>(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player)));
                }
            }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new TicTacToeForwardModel();
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) currentState.getGameParameters();
        int gridSize = tttgp.gridSize;
        if (currentState.getTurnOrder().getRoundCounter() == (gridSize * gridSize)) {
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
     *
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(TicTacToeGameState gameState) {
        GridBoard<Token> gridBoard = gameState.getGridBoard();

        // Check columns
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            Token c = gridBoard.getElement(x, 0);
            if (!c.getTokenType().equals(TicTacToeConstants.emptyCell)) {
                boolean win = true;
                for (int y = 1; y < gridBoard.getHeight(); y++) {
                    Token o = gridBoard.getElement(x, y);
                    if (o.getTokenType().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
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
        for (int y = 0; y < gridBoard.getHeight(); y++) {
            Token c = gridBoard.getElement(0, y);
            if (!c.getTokenType().equals(TicTacToeConstants.emptyCell)) {
                boolean win = true;
                for (int x = 1; x < gridBoard.getWidth(); x++) {
                    Token o = gridBoard.getElement(x, y);
                    if (o.getTokenType().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
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
        Token c = gridBoard.getElement(0, 0);
        if (!c.getTokenType().equals(TicTacToeConstants.emptyCell)) {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                Token o = gridBoard.getElement(i, i);
                if (o.getTokenType().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
                    win = false;
                }
            }
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }

        // Secondary
        c = gridBoard.getElement(gridBoard.getWidth() - 1, 0);
        if (!c.getTokenType().equals(TicTacToeConstants.emptyCell)) {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                Token o = gridBoard.getElement(gridBoard.getWidth() - 1 - i, i);
                if (o.getTokenType().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
                    win = false;
                }
            }
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }
        boolean tie = gridBoard.getComponents().stream().noneMatch(t -> t.getTokenType().equals(TicTacToeConstants.emptyCell));

        if (tie) {
            gameState.setGameStatus(Utils.GameResult.DRAW);
            Arrays.fill(gameState.getPlayerResults(), Utils.GameResult.DRAW);
        }

        return tie;
    }

    @Override
    protected void endGame(AbstractGameState gameState) {
        if (gameState.getCoreGameParameters().verbose) {
            System.out.println(Arrays.toString(gameState.getPlayerResults()));
        }
    }

    /**
     * Inform the game this player has won.
     *
     * @param winnerSymbol - which player won.
     */
    private void registerWinner(TicTacToeGameState gameState, Token winnerSymbol) {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        int winningPlayer = TicTacToeConstants.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(Utils.GameResult.WIN, winningPlayer);
        gameState.setPlayerResult(Utils.GameResult.LOSE, 1 - winningPlayer);
    }
}

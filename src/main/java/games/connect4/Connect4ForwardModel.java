package games.connect4;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.components.Token;
import utilities.Pair;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Connect4ForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        Connect4GameParameters c4gp = (Connect4GameParameters) firstState.getGameParameters();
        int gridSize = c4gp.gridSize;
        Connect4GameState state = (Connect4GameState) firstState;
        state.gridBoard = new GridBoard<>(gridSize, gridSize, new Token(Connect4Constants.emptyCell));
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        Connect4GameState c4gs = (Connect4GameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);

        if (gameState.isNotTerminal())
            for (int x = 0; x < c4gs.gridBoard.getWidth(); x++) {
                int y = c4gs.gridBoard.getHeight() - 1; // this is bottom of column
                boolean end = false;
                while(!end)
                {
                    boolean newCol = false;
                    if (c4gs.gridBoard.getElement(x, y).getTokenType().equals(Connect4Constants.emptyCell)) {
                        actions.add(new SetGridValueAction<>(c4gs.gridBoard.getComponentID(), x, y, Connect4Constants.playerMapping.get(player)));
                        newCol = true;
                    }

                    //Stop when reaching top (column is full) or on finding the first empty cell (action available)
                    end = (--y <  0 || newCol);
                }
            }
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new Connect4ForwardModel();
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);
        Connect4GameParameters c4gp = (Connect4GameParameters) currentState.getGameParameters();
        int gridSize = c4gp.gridSize;
        if (currentState.getTurnOrder().getRoundCounter() == (gridSize * gridSize)) {
            currentState.setGameStatus(Utils.GameResult.GAME_END);
            return;
        }

        if (checkGameEnd((Connect4GameState) currentState)) {
            return;
        }
        currentState.getTurnOrder().endPlayerTurn(currentState);
    }

    /**
     * Checks if the game ended.
     *
     * @param gameState - game state to check game end.
     */
    private boolean checkGameEnd(Connect4GameState gameState) {
        GridBoard<Token> gridBoard = gameState.getGridBoard();
        Connect4GameParameters c4gp = (Connect4GameParameters) gameState.getGameParameters();
        boolean gap = false;
        LinkedList<Pair<Integer, Integer>> winning = new LinkedList<>();;

        // Check columns
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            int count = 0;
            String lastToken = null;
            winning.clear();
            for (int y = gridBoard.getHeight() - 1; y >= 0; y--) {
                Token c = gridBoard.getElement(x, y);
                if (c.getTokenType().equals(Connect4Constants.emptyCell)) {
                    count = 0;
                    lastToken = null;
                    winning.clear();
                    gap = true;
                } else if (lastToken == null || !lastToken.equals(c.getTokenType())) {
                    winning.clear();
                    count = 1;
                    lastToken = c.getTokenType();
                    winning.add(new Pair<>(x, y));
                } else {
                    {
                        count++;
                        winning.add(new Pair<>(x, y));
                        if (count == c4gp.winCount) {
                            registerWinner(gameState, c, winning);
                            return true;
                        }
                    }
                    lastToken = c.getTokenType();
                }
            }
        }

        // Check rows
        for (int y = gridBoard.getHeight() - 1; y >= 0; y--) {
            int count = 0;
            String lastToken = null;
            winning.clear();
            for (int x = 0; x < gridBoard.getWidth(); x++) {
                Token c = gridBoard.getElement(x, y);
                if (c.getTokenType().equals(Connect4Constants.emptyCell)) {
                    count = 0;
                    lastToken = null;
                    winning.clear();
                } else if (lastToken == null || !lastToken.equals(c.getTokenType())) {
                    winning.clear();
                    count = 1;
                    lastToken = c.getTokenType();
                    winning.add(new Pair<>(x, y));
                } else {
                    {
                        count++;
                        winning.add(new Pair<>(x, y));
                        if (count == c4gp.winCount) {
                            registerWinner(gameState, c, winning);
                            return true;
                        }
                    }
                    lastToken = c.getTokenType();
                }
            }
        }

        //Check main diagonals (from col 0)
        for (int y = gridBoard.getHeight() - 1; y >= 0; y--) {
            if (checkMainDiagonals(gameState, 0, y))
                return true;

        }

        //Check main and inverse diagonals (from row 0)
        for (int x = 1; x < gridBoard.getWidth(); x++) {
            if (checkMainDiagonals(gameState, x, gridBoard.getHeight() - 1))
                return true;
            if (checkInvDiagonals(gameState, x, gridBoard.getHeight() - 1))
                return true;
        }

        //Check inv diagonals (from last column)
        for (int y = gridBoard.getHeight() - 2; y >= 0; y--) { //height -1 is checked in previous loop
            if (checkInvDiagonals(gameState, gridBoard.getWidth()-1, y))
                return true;
        }

        if (!gap) { //tie
            gameState.setGameStatus(Utils.GameResult.DRAW);
            Arrays.fill(gameState.getPlayerResults(), Utils.GameResult.DRAW);
            return true;
        }

        return false;
    }


    private boolean checkMainDiagonals(Connect4GameState gameState, int xStart, int yStart)
    {
        GridBoard<Token> gridBoard = gameState.getGridBoard();
        Connect4GameParameters c4gp = (Connect4GameParameters) gameState.getGameParameters();
        int count = 0;
        String lastToken = null;
        LinkedList<Pair<Integer, Integer>> winning = new LinkedList<>();

        for (int x = xStart, y = yStart; x < gridBoard.getWidth() && y >=0; x++, y--) {
            Token c = gridBoard.getElement(x, y);

            if (c.getTokenType().equals(Connect4Constants.emptyCell)) {
                count = 0;
                lastToken = null;
                winning.clear();
            } else if (lastToken == null || !lastToken.equals(c.getTokenType())) {
                winning.clear();
                count = 1;
                lastToken = c.getTokenType();
                winning.add(new Pair<>(x, y));
            } else {
                count++;
                winning.add(new Pair<>(x, y));
                if (count == c4gp.winCount) {
                    registerWinner(gameState, c, winning);
                    return true;
                }
                lastToken = c.getTokenType();
            }
        }
        return false;
    }

    private boolean checkInvDiagonals(Connect4GameState gameState, int xStart, int yStart)
    {
        GridBoard<Token> gridBoard = gameState.getGridBoard();
        Connect4GameParameters c4gp = (Connect4GameParameters) gameState.getGameParameters();
        int count = 0;
        String lastToken = null;
        LinkedList<Pair<Integer, Integer>> winning = new LinkedList<>();

        for (int x = xStart, y = yStart; x >= 0 && y >= 0; x--, y--) {
            Token c = gridBoard.getElement(x, y);

            if (c.getTokenType().equals(Connect4Constants.emptyCell)) {
                count = 0;
                lastToken = null;
                winning.clear();
            } else if (lastToken == null || !lastToken.equals(c.getTokenType())) {
                winning.clear();
                count = 1;
                lastToken = c.getTokenType();
                winning.add(new Pair<>(x, y));
            } else {
                count++;
                winning.add(new Pair<>(x, y));
                if (count == c4gp.winCount) {
                    registerWinner(gameState, c, winning);
                    return true;
                }
                lastToken = c.getTokenType();
            }
        }
        return false;
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
    private void registerWinner(Connect4GameState gameState, Token winnerSymbol, LinkedList<Pair<Integer, Integer>> winPos) {
        gameState.setGameStatus(Utils.GameResult.GAME_END);
        int winningPlayer = Connect4Constants.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(Utils.GameResult.WIN, winningPlayer);
        gameState.setPlayerResult(Utils.GameResult.LOSE, 1 - winningPlayer);
        gameState.registerWinningCells(winPos);
    }
}
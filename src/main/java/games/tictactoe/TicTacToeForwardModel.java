package games.tictactoe;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.GridBoard;
import core.interfaces.ITreeActionSpace;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends StandardForwardModel implements ITreeActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        int gridSize = tttgp.gridSize;
        TicTacToeGameState state = (TicTacToeGameState) firstState;
        state.gridBoard = new GridBoard(gridSize, gridSize, new BoardNode(TicTacToeConstants.emptyCell));
    }


    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, ActionSpace.Default);
    }

    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();

        if (gameState.isNotTerminal()){
            // Normal action space
            for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
                for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                    if (tttgs.gridBoard.getElement(x, y).getComponentName().equals(TicTacToeConstants.emptyCell)) {
                        actions.add(new SetGridValueAction(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player).getComponentID()));
                    }
                }
            }
        }
        return actions;
        }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (checkAndProcessGameEnd((TicTacToeGameState) currentState)) {
            return;
        }
        endPlayerTurn(currentState);
    }

    /**
     * Checks if the game ended.
     *
     * @param gameState - game state to check game end.
     */
    private boolean checkAndProcessGameEnd(TicTacToeGameState gameState) {
        GridBoard gridBoard = gameState.getGridBoard();

        // Check columns
        for (int x = 0; x < gridBoard.getWidth(); x++) {
            BoardNode c = gridBoard.getElement(x, 0);
            if (c != null && !c.getComponentName().equals(TicTacToeConstants.emptyCell)) {
                boolean win = true;
                for (int y = 1; y < gridBoard.getHeight(); y++) {
                    BoardNode o = gridBoard.getElement(x, y);
                    if (o.getComponentName().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
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
            BoardNode c = gridBoard.getElement(0, y);
            if (!c.getComponentName().equals(TicTacToeConstants.emptyCell)) {
                boolean win = true;
                for (int x = 1; x < gridBoard.getWidth(); x++) {
                    BoardNode o = gridBoard.getElement(x, y);
                    if (o.getComponentName().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
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
        BoardNode c = gridBoard.getElement(0, 0);
        if (!c.getComponentName().equals(TicTacToeConstants.emptyCell)) {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                BoardNode o = gridBoard.getElement(i, i);
                if (o.getComponentName().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
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
        if (!c.getComponentName().equals(TicTacToeConstants.emptyCell)) {
            boolean win = true;
            for (int i = 1; i < gridBoard.getWidth(); i++) {
                BoardNode o = gridBoard.getElement(gridBoard.getWidth() - 1 - i, i);
                if (o.getComponentName().equals(TicTacToeConstants.emptyCell) || !o.equals(c)) {
                    win = false;
                }
            }
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }
        boolean tie = gridBoard.getComponents().stream().noneMatch(t -> t.getComponentName().equals(TicTacToeConstants.emptyCell));

        if (tie) {
            gameState.setGameStatus(CoreConstants.GameResult.DRAW_GAME);
            Arrays.fill(gameState.getPlayerResults(), CoreConstants.GameResult.DRAW_GAME);
        }

        return tie;
    }

    /**
     * Inform the game this player has won.
     *
     * @param winnerSymbol - which player won.
     */
    private void registerWinner(TicTacToeGameState gameState, BoardNode winnerSymbol) {
        gameState.setGameStatus(CoreConstants.GameResult.GAME_END);
        int winningPlayer = TicTacToeConstants.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(CoreConstants.GameResult.WIN_GAME, winningPlayer);
        gameState.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 1 - winningPlayer);
    }

    public ActionTreeNode initActionTree(AbstractGameState gameState){
        int gridSize = ((TicTacToeGameState) gameState).gridBoard.getWidth();
        ActionTreeNode root = new ActionTreeNode(0, "root");
        for (int x = 0; x < gridSize; x++) {
            ActionTreeNode xNode = root.addChild(0, "X" + x);
            for (int y = 0; y < gridSize; y++) {
                xNode.addChild(0, "Y" + y);
            }
        }
        return root;
    }
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState){
        root.resetTree();

        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        int player = tttgs.getCurrentPlayer();
        for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
            ActionTreeNode xNode = root.findChildrenByName("X" + x);
            for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                ActionTreeNode yNode = xNode.findChildrenByName("Y" + y);
                if (tttgs.gridBoard.getElement(x, y).getComponentName().equals(TicTacToeConstants.emptyCell)) {
                    xNode.setValue(1); // make sure that we set parent available
                    yNode.setAction(new SetGridValueAction(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player).getComponentID()));
                }
            }
        }
        return root;
    }
}

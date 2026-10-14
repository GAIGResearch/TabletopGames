package games.tictactoe;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.grid.SparseGrid;
import core.components.grid.SquareCoord;
import core.interfaces.ITreeActionSpace;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends StandardForwardModel implements ITreeActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TicTacToeGameState state = (TicTacToeGameState) firstState;
        state.gridBoard = new SparseGrid<>("TicTacToe Board");
        state.gridSize = ((TicTacToeGameParameters) state.getGameParameters()).gridSize;
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
            for (int x = 0; x < tttgs.getGridSize(); x++) {
                for (int y = 0; y < tttgs.getGridSize(); y++) {
                    if (!tttgs.gridBoard.contains(new SquareCoord(x, y))) {
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
        SparseGrid<SquareCoord, BoardNode> gridBoard = gameState.getGridBoard();
        int n = gameState.getGridSize();

        // each line is given by its first square and the step along it: the columns, the rows and the two diagonals
        List<int[]> lines = new ArrayList<>();
        for (int x = 0; x < n; x++)
            lines.add(new int[]{x, 0, 0, 1});
        for (int y = 0; y < n; y++)
            lines.add(new int[]{0, y, 1, 0});
        lines.add(new int[]{0, 0, 1, 1});
        lines.add(new int[]{n - 1, 0, -1, 1});

        for (int[] line : lines) {
            BoardNode c = gridBoard.get(new SquareCoord(line[0], line[1]));
            if (c == null) continue;
            boolean win = true;
            for (int i = 1; i < n && win; i++)
                win = c.equals(gridBoard.get(new SquareCoord(line[0] + i * line[2], line[1] + i * line[3])));
            if (win) {
                registerWinner(gameState, c);
                return true;
            }
        }

        boolean tie = gridBoard.size() == n * n;
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
        int gridSize = ((TicTacToeGameState) gameState).getGridSize();
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
        for (int x = 0; x < tttgs.getGridSize(); x++) {
            ActionTreeNode xNode = root.findChildrenByName("X" + x);
            for (int y = 0; y < tttgs.getGridSize(); y++) {
                ActionTreeNode yNode = xNode.findChildrenByName("Y" + y);
                if (!tttgs.gridBoard.contains(new SquareCoord(x, y))) {
                    xNode.setValue(1); // make sure that we set parent available
                    yNode.setAction(new SetGridValueAction(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player).getComponentID()));
                }
            }
        }
        return root;
    }
}

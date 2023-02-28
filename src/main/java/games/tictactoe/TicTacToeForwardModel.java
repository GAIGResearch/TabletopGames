package games.tictactoe;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.GridBoard;
import core.components.Token;
import core.interfaces.IOrderedActionSpace;
import utilities.ActionTreeNode;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TicTacToeForwardModel extends StandardForwardModel implements IOrderedActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        TicTacToeGameParameters tttgp = (TicTacToeGameParameters) firstState.getGameParameters();
        int gridSize = tttgp.gridSize;
        root = generateActionTree(gridSize);
        TicTacToeGameState state = (TicTacToeGameState) firstState;
        state.gridBoard = new GridBoard<>(gridSize, gridSize, new Token(TicTacToeConstants.emptyCell));
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        root.resetTree();
        TicTacToeGameState tttgs = (TicTacToeGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();

        if (gameState.isNotTerminal())
            for (int x = 0; x < tttgs.gridBoard.getWidth(); x++) {
                ActionTreeNode xNode = root.findChildrenByName("X" + x);
                for (int y = 0; y < tttgs.gridBoard.getHeight(); y++) {
                    ActionTreeNode yNode = xNode.findChildrenByName("Y" + y);
                    if (tttgs.gridBoard.getElement(x, y).getTokenType().equals(TicTacToeConstants.emptyCell)) {
                        actions.add(new SetGridValueAction<>(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player)));
                        xNode.setValue(1); // make sure that we set parent available
                        yNode.setAction(new SetGridValueAction<>(tttgs.gridBoard.getComponentID(), x, y, TicTacToeConstants.playerMapping.get(player)));
                    }
                }
            }
//        System.out.println(Arrays.toString(root.getActionMask()));
//        System.out.println(root.getActionMaskNames());
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
    private void registerWinner(TicTacToeGameState gameState, Token winnerSymbol) {
        gameState.setGameStatus(CoreConstants.GameResult.GAME_END);
        int winningPlayer = TicTacToeConstants.playerMapping.indexOf(winnerSymbol);
        gameState.setPlayerResult(CoreConstants.GameResult.WIN_GAME, winningPlayer);
        gameState.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, 1 - winningPlayer);
    }

    @Override
    public int getActionSpace() {
        return root.getSubNodes();
    }

    public ActionTreeNode generateActionTree(int gridSize){
        root = new ActionTreeNode(0, "root");
        for (int x = 0; x < gridSize; x++) {
            ActionTreeNode xNode = root.addChild(0, "X" + x);
            for (int y = 0; y < gridSize; y++) {
                xNode.addChild(0, "Y" + y);
            }
        }
        return root;
    }

    public List<Object> getTreeShape() {
        return root.getTreeShape();
    }

    @Override
    public int[] getFixedActionSpace() {
        return new int[0];
    }

    @Override
    public int[] getActionMask(AbstractGameState gameState) {
        return root.getActionMask();
    }

    @Override
    public void nextPython(AbstractGameState state, int actionID) {
        // Manual conversion from actionID to actionVector
        int gridSize = ((TicTacToeGameParameters) state.getGameParameters()).gridSize;
        int[] actionVector = new int[2]; // we have 2 levels in the tree
        actionVector[0] = (actionID-gridSize)/gridSize;
        actionVector[1] = (actionID-gridSize)%gridSize;
        nextPython(state, actionVector);
    }

    public void nextPython(AbstractGameState state, int[] actionVector) {
        ActionTreeNode node = root;
        for (int i = 0; i < actionVector.length; i++) {
            node = node.getChildren().get(actionVector[i]);
        }
        AbstractAction action = node.getAction();
        next(state, action);

    }
}

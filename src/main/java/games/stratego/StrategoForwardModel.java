package games.stratego;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.GridBoard;
import core.interfaces.IOrderedActionSpace;
import games.stratego.actions.Move;
import games.stratego.components.Piece;
import utilities.ActionTreeNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class StrategoForwardModel extends StandardForwardModel implements IOrderedActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        StrategoParams params = (StrategoParams) firstState.getGameParameters();
        StrategoGameState state = (StrategoGameState) firstState;
        state.gridBoard = new GridBoard<>(params.gridSize, params.gridSize);
        Random random = new Random(params.getRandomSeed());

        StrategoConstants.PieceSetups[] setups = StrategoConstants.PieceSetups.values();
        StrategoConstants.PieceSetups RedSetup = setups[random.nextInt(setups.length)];
        StrategoConstants.PieceSetups BlueSetup = setups[random.nextInt(setups.length)];

        ArrayList<Piece> RedPieces = RedSetup.getRedSetup();
        ArrayList<Piece> BluePieces = BlueSetup.getBlueSetup();

        root = generateActionTree(params.gridSize, RedPieces.size());
        leaves = root.getLeafNodes();

        for (Piece piece : RedPieces){
            piece.setOwnerId(0);
            state.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
        }
        for (Piece piece : BluePieces){
            piece.setOwnerId(1);
            state.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
        }

        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        StrategoGameState state = (StrategoGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();
        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(player);
        List<Piece> pieces = state.gridBoard.getComponents();
        state.getObservationVector();


        if (pieces.isEmpty()){
            throw new AssertionError("Error: No Pieces Found");
//            state.setGameStatus(Utils.GameResult.GAME_END);
 //           return actions;
        }

        for (Piece piece : pieces){
            if (piece != null){
                if (piece.getPieceAlliance() == playerAlliance) {
                    Collection<Move> moves = piece.calculateMoves(state);
                    actions.addAll(moves);
                }
            }
        }
        return actions;
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.getGameStatus() == CoreConstants.GameResult.GAME_END){
            return;
        }
        StrategoGameState sgs = (StrategoGameState) currentState;

        endPlayerTurn(sgs);

        List<AbstractAction> actions = _computeAvailableActions(sgs);
        if (actions.isEmpty()){
            // If the player can't take any actions, they lose
            sgs.setGameStatus(CoreConstants.GameResult.GAME_END);
            sgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, sgs.getCurrentPlayer());
            sgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 1-sgs.getCurrentPlayer());
        } else {
            if (sgs.getRoundCounter() >= ((StrategoParams)sgs.getGameParameters()).maxRounds) {
                // Max rounds reached, draw
                sgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                sgs.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, sgs.getCurrentPlayer());
                sgs.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1-sgs.getCurrentPlayer());
            }
        }
    }

    private ActionTreeNode generateActionTree(int gridSize, int noUnits) {
        root = new ActionTreeNode(0, "root");

        // Tree Structure
        // 0 - Root
        // 1 - Unit (0 - noUnits)
        // 2 - Action (Move / Attack)
        // 3 - Direction (North / South / East / West)
        // 4 - Distance (1 - gridsize) (Only for scout)

        for (int i = 0; i < noUnits; i++) {
            root.addChild(0, "unit" + i);
        }

        for (ActionTreeNode unit : root.getChildren()) {

            // Attack Sub Tree
            ActionTreeNode attack = unit.addChild(0, "attack");
            attack.addChild(0, "north");
            attack.addChild(0, "south");
            attack.addChild(0, "east");
            attack.addChild(0, "west");

            // Move Sub Tree
            ActionTreeNode move = unit.addChild(0, "move");
            move.addChild(0, "north");
            move.addChild(0, "south");
            move.addChild(0, "east");
            move.addChild(0, "west");

            // For scout (unlimited moves)
            for (ActionTreeNode child : move.getChildren()) {
                for (int i = 0; i < gridSize; i++) {
                    child.addChild(0, Integer.toString(i+1));
                }
            }
        }

        return root;
    }

    @Override
    public int getActionSpace() {
        return leaves.size();
    }

    @Override
    public int[] getFixedActionSpace() {
        return new int[0];
    }

    @Override
    public int[] getActionMask(AbstractGameState gameState) {
        return leaves.stream()
                .mapToInt(ActionTreeNode::getValue)
                .toArray();
    }

    @Override
    public void nextPython(AbstractGameState state, int actionID) {
        ActionTreeNode node = leaves.get(actionID);

        int distance;
        int direction;


        //
    }
}

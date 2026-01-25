package games.stratego;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.BoardNode;
import core.components.GridBoard;
import core.interfaces.ITreeActionSpace;
import games.stratego.actions.AttackMove;
import games.stratego.actions.Move;
import games.stratego.actions.NormalMove;
import games.stratego.actions.DeepMove;
import games.stratego.components.Piece;
import utilities.ActionTreeNode;
import utilities.Distance;
import games.stratego.metrics.StrategoMetrics;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.List;

public class StrategoForwardModel extends StandardForwardModel implements ITreeActionSpace {

    public enum EndCondition {
        FLAG_CAPTURE,
        NO_MOVES_LEFT,
        MAX_TURNS
    }

    @Override
    protected void _setup(AbstractGameState firstState) {
        StrategoParams params = (StrategoParams) firstState.getGameParameters();
        StrategoGameState state = (StrategoGameState) firstState;
        state.gridBoard = new GridBoard(params.gridSize, params.gridSize);

        StrategoConstants.PieceSetups[] setups = StrategoConstants.PieceSetups.values();
        StrategoConstants.PieceSetups RedSetup = setups[state.getRnd().nextInt(setups.length)];
        StrategoConstants.PieceSetups BlueSetup = setups[state.getRnd().nextInt(setups.length)];

        ArrayList<Piece> RedPieces = RedSetup.getRedSetup();
        ArrayList<Piece> BluePieces = BlueSetup.getBlueSetup();

        for (Piece piece : RedPieces){
            piece.setOwnerId(0);
            state.gridBoard.setElement(piece.getPiecePosition().getX(), piece.getPiecePosition().getY(), piece.copy());
        }
        for (Piece piece : BluePieces){
            piece.setOwnerId(1);
            state.gridBoard.setElement(piece.getPiecePosition().getX(), piece.getPiecePosition().getY(), piece.copy());
        }

        state.setFirstPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        StrategoGameState state = (StrategoGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();
        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(player);
        List<BoardNode> pieces = state.gridBoard.getComponents();

        if (pieces.isEmpty()){
            throw new AssertionError("Error: No Pieces Found");
//            state.setGameStatus(Utils.GameResult.GAME_END);
            //           return actions;
        }

        for (BoardNode bn : pieces){
            Piece piece = (Piece) bn;
            if (piece != null){
                if (piece.getPieceAlliance() == playerAlliance) {

                    List<AbstractAction> pieceActions = piece.calculateMoves(state, actionSpace);
                    if (pieceActions.size() == 0) continue;
                    if (actionSpace.structure == ActionSpace.Structure.Deep) {
                        // Single action to choose the piece, then move for piece is selected sequentially
                        if (actionSpace.context == ActionSpace.Context.Dependent) {
                            actions.add(new DeepMove(player, piece.getPiecePosition(), actionSpace));
                        } else {
                            actions.add(new DeepMove(player, piece.getComponentID(), actionSpace));
                        }
                    } else {
                        actions.addAll(pieceActions);
                    }
                }
            }
        }
    return actions;
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, ActionSpace.Default);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.getGameStatus() == CoreConstants.GameResult.GAME_END || currentState.isActionInProgress()){
            return;
        }

        StrategoGameState sgs = (StrategoGameState) currentState;
        endPlayerTurn(sgs);

        List<AbstractAction> actions = _computeAvailableActions(sgs, currentState.getCoreGameParameters().actionSpace);
        if (actions.isEmpty()){
            sgs.logEvent(StrategoMetrics.StrategoEvent.EndCondition, EndCondition.NO_MOVES_LEFT.name() + ":" + sgs.getCurrentPlayer());
            // If the player can't take any actions, they lose
            _computeAvailableActions(sgs, currentState.getCoreGameParameters().actionSpace);
            sgs.setGameStatus(CoreConstants.GameResult.GAME_END);
            sgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, sgs.getCurrentPlayer());
            sgs.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 1-sgs.getCurrentPlayer());
        } else {
            if (sgs.getTurnCounter() >= ((StrategoParams)sgs.getGameParameters()).maxRounds) {
                sgs.logEvent(StrategoMetrics.StrategoEvent.EndCondition, EndCondition.MAX_TURNS.name());
                // Max rounds reached, draw
                sgs.setGameStatus(CoreConstants.GameResult.GAME_END);
                sgs.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, sgs.getCurrentPlayer());
                sgs.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1-sgs.getCurrentPlayer());
            }
        }
    }

    private String getDirection(Vector2D pos1, Vector2D pos2) {
        if (pos1.getX() == pos2.getX()) {
            if (pos1.getY() > pos2.getY()) {
                return "north";
            } else {
                return "south";
            }
        } else {
            if (pos1.getX() > pos2.getX()) {
                return "west";
            } else {
                return "east";
            }
        }
    }

    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState){
        root.resetTree();
        StrategoGameState state = (StrategoGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = state.getCurrentPlayer();
        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(player);
        List<BoardNode> pieces = state.gridBoard.getComponents();
        int c = 0;
        for (BoardNode bn : pieces){
            Piece piece = (Piece) bn;
            if (piece != null){
                if (piece.getPieceAlliance() == playerAlliance) {
                    List<AbstractAction> moves = piece.calculateMoves(state, ActionSpace.Default);
//                    List<AbstractAction> moves = piece.calculateMoves(state, state.getCoreGameParameters().actionSpace);
                    actions.addAll(moves);

                    // Player unit on position
                    ActionTreeNode pos = root.getChildren().get(c);
                    pos.setValue(1);

                    // Valid moves have been generated
                    // Encode them into tree
                    if (moves.size() > 0) {
                        for (AbstractAction move : moves) {
                            // Chooses between attack and move
                            ActionTreeNode actionNode = move instanceof NormalMove
                                    ? pos.getChildren().get(1) : pos.getChildren().get(0);
                            actionNode.setValue(1);

                            // Gets direction of move
                            String direction = getDirection(piece.getPiecePosition(), ((Move)move).to(state));
                            ActionTreeNode directionNode = null;
                            switch (direction) {
                                case "north":
                                    directionNode = actionNode.getChildren().get(0);
                                    directionNode.setValue(1);
                                    break;
                                case "south":
                                    directionNode = actionNode.getChildren().get(1);
                                    directionNode.setValue(1);
                                    break;
                                case "east":
                                    directionNode = actionNode.getChildren().get(2);
                                    directionNode.setValue(1);
                                    break;
                                case "west":
                                    directionNode = actionNode.getChildren().get(3);
                                    directionNode.setValue(1);
                                    break;
                            }

                            // If move is a normal move, action is stored in child due to scouts extra movement
                            if (move instanceof NormalMove) {
                                int distanceIndex = (int) Distance.manhattan_distance(piece.getPiecePosition(), ((Move)move).to(state)) - 1;
                                assert directionNode != null;
                                directionNode.getChildren().get(distanceIndex).setAction(move);
                            }

                            // If move in an action move, action is stored in direction node
                            else if (move instanceof AttackMove) {
                                directionNode.setAction(move);
                            }
                        }
                    }
                }
            }
            c++;
        }
        return root;
    }

    public ActionTreeNode initActionTree(AbstractGameState state) {
        StrategoGameState sgs = (StrategoGameState) state;
        int gridSize = ((StrategoParams) sgs.getGameParameters()).gridSize;
        root = new ActionTreeNode(0, "root");

        // Tree Structure
        // 0 - Root
        // 1 - Position (0 - noPositions)
        // 2 - Action (Move / Attack)
        // 3 - Direction (North / South / East / West)
        // 4 - Distance (1 - gridsize) (Only for scout)

        for (int i = 0; i < gridSize*gridSize; i++) {
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

}

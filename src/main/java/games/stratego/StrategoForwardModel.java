package games.stratego;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import games.stratego.actions.Move;
import games.stratego.components.Piece;
import utilities.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StrategoForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        StrategoParams params = (StrategoParams) firstState.getGameParameters();
        StrategoGameState state = (StrategoGameState) firstState;
        state.gridBoard = new GridBoard<>(params.gridSize, params.gridSize);

        ArrayList<StrategoConstants.PieceSetups> setups = StrategoConstants.returnPieceSetups();
        StrategoConstants.PieceSetups RedSetup = setups.get((int) (Math.random()*setups.size()));
        StrategoConstants.PieceSetups BlueSetup = setups.get((int) (Math.random()*setups.size()));

        ArrayList<Piece> RedPieces = RedSetup.getRedSetup();
        ArrayList<Piece> BluePieces = BlueSetup.getBlueSetup();

        for (Piece piece : RedPieces){
            Piece copy = piece.copy();
            state.gridBoard.setElement(copy.getPiecePosition()[0], copy.getPiecePosition()[1], copy);
        }
        for (Piece piece : BluePieces){
            Piece copy = piece.copy();
            state.gridBoard.setElement(copy.getPiecePosition()[0], copy.getPiecePosition()[1], copy);
        }

        for (Piece piece : state.gridBoard.getComponents()){
            if (piece != null){
                if (piece.getPieceAlliance() == Piece.Alliance.RED){
                    piece.setOwnerId(0);
                } else if (piece.getPieceAlliance() == Piece.Alliance.BLUE){
                    piece.setOwnerId(1);
                }
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        StrategoGameState state = (StrategoGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getTurnOrder().getCurrentPlayer(gameState);
        String playerAlliance = StrategoConstants.playerMapping.get(player);
        List<Piece> pieces = state.gridBoard.getComponents();
        if (pieces.isEmpty()){
            System.out.println("Error: No Pieces Found");
            state.setGameStatus(Utils.GameResult.GAME_END);
        }

        for (Piece piece : pieces){
            if (piece != null){
                Piece _piece = state.gridBoard.getElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1]);
                if (_piece != piece){
                    System.out.println("Board and piece inconsistency:" + piece + ",(" + piece.getPiecePosition()[0] + "," + piece.getPiecePosition()[1] + "), " +
                            _piece);
                }

                if (piece.getPieceAlliance().toString().equals(playerAlliance)){
                    Collection<Move> moves = piece.calculateMoves(state);
                    actions.addAll(moves);
                }
            }
        }
        if (actions.isEmpty()){
            state.setGameStatus(Utils.GameResult.GAME_END);
            state.setPlayerResult(Utils.GameResult.LOSE, state.getCurrentPlayer());
            state.setPlayerResult(Utils.GameResult.WIN, 1-state.getCurrentPlayer());
        }
        return actions;
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        boolean valid = action.execute(currentState);
        if (!valid){
            System.out.println("Invalid move");
        }

        if (currentState.getGameStatus() == Utils.GameResult.GAME_END){
            return;
        }

        currentState.getTurnOrder().endPlayerTurn(currentState);

        List<AbstractAction> actions = _computeAvailableActions(currentState);
        if (actions.isEmpty()){
            currentState.setGameStatus(Utils.GameResult.GAME_END);
            currentState.setPlayerResult(Utils.GameResult.LOSE, currentState.getCurrentPlayer());
            currentState.setPlayerResult(Utils.GameResult.WIN, 1-currentState.getCurrentPlayer());
        }
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new StrategoForwardModel();
    }
}

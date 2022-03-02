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
import java.util.Random;

public class StrategoForwardModel extends AbstractForwardModel {

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

        for (Piece piece : RedPieces){
            piece.setOwnerId(0);
            state.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
        }
        for (Piece piece : BluePieces){
            piece.setOwnerId(1);
            state.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
        }

        state.getTurnOrder().setStartingPlayer(0);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        StrategoGameState state = (StrategoGameState) gameState;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gameState.getCurrentPlayer();
        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(player);
        List<Piece> pieces = state.gridBoard.getComponents();

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
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        action.execute(currentState);

        if (currentState.getGameStatus() == Utils.GameResult.GAME_END){
            return;
        }

        currentState.getTurnOrder().endPlayerTurn(currentState);

        List<AbstractAction> actions = _computeAvailableActions(currentState);
        if (actions.isEmpty()){
            // If the player can't take any actions, they lose
            currentState.setGameStatus(Utils.GameResult.GAME_END);
            currentState.setPlayerResult(Utils.GameResult.LOSE, currentState.getCurrentPlayer());
            currentState.setPlayerResult(Utils.GameResult.WIN, 1-currentState.getCurrentPlayer());
        } else {
            if (currentState.getTurnOrder().getRoundCounter() >= ((StrategoParams)currentState.getGameParameters()).maxRounds) {
                // Max rounds reached, draw
                currentState.setGameStatus(Utils.GameResult.GAME_END);
                currentState.setPlayerResult(Utils.GameResult.DRAW, currentState.getCurrentPlayer());
                currentState.setPlayerResult(Utils.GameResult.DRAW, 1-currentState.getCurrentPlayer());
            }
        }
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new StrategoForwardModel();
    }
}

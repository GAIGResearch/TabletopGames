package games.stratego;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.GridBoard;
import games.stratego.actions.DeepMove;
import games.stratego.actions.Move;
import games.stratego.components.Piece;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class StrategoForwardModel extends StandardForwardModel {

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
        List<Piece> pieces = state.gridBoard.getComponents();

        if (pieces.isEmpty()){
            throw new AssertionError("Error: No Pieces Found");
//            state.setGameStatus(Utils.GameResult.GAME_END);
            //           return actions;
        }

        for (Piece piece : pieces){
            if (piece != null){
                if (piece.getPieceAlliance() == playerAlliance) {
                    if (actionSpace.structure == ActionSpace.Structure.Deep) {
                        if (actionSpace.context == ActionSpace.Context.Dependent) {
                            actions.add(new DeepMove(player, piece.getPiecePosition(), actionSpace));
                        } else {
                            actions.add(new DeepMove(player, piece.getComponentID(), actionSpace));
                        }
                    } else {
                        actions.addAll(piece.calculateMoves(state, actionSpace));
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
}

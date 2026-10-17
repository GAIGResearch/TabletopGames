package games.stratego;

import core.AbstractGameState;
import core.CoreConstants;
import core.components.BoardNode;
import core.interfaces.IStateHeuristic;
import games.stratego.components.Piece;

public class StrategoHeuristic implements IStateHeuristic {

    double bombValue = 0.0;
    double flagValue = 1.0;

    double maxRankSum = -1;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        StrategoGameState state = (StrategoGameState) gs;
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];

        if (maxRankSum == -1) {
            StrategoParams params = (StrategoParams) state.getGameParameters();
            maxRankSum = 0;
            for (int i = 0; i < params.pieceSetupCount.length; i++) {
                maxRankSum += (i+1)*params.pieceSetupCount[i];
            }
            maxRankSum += bombValue * params.pieceSetupNBombs;
            maxRankSum += flagValue * params.pieceSetupNFlags;
        }

        if (!gs.isNotTerminal())
            return playerResult.value;

        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(playerId);
        double sumP = 0.0;
        double sumOpp = 0.0;

        for (BoardNode p : state.gridBoard.getComponents()){
            Piece piece = (Piece) p;
            if (piece != null){
                double pieceValue = piece.getPieceRank();
                if (piece.getPieceType() == Piece.PieceType.BOMB) pieceValue = bombValue;
                else if (piece.getPieceType() == Piece.PieceType.FLAG) pieceValue = flagValue;
                if (playerAlliance == piece.getPieceAlliance()) {
                    sumP += pieceValue;
                } else{
                    sumOpp += pieceValue;
                }
            }
        }
        double playerScore = sumP/maxRankSum;
        double oppScore = sumOpp/maxRankSum;

        return playerScore - oppScore;
    }

    // no settings (any fields are fixed, or worked out from the game): any two are the same heuristic, so that one
    // read back from JSON equals the one written
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode();
    }
}

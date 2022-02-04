package games.stratego;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.stratego.components.Piece;
import utilities.Utils;

import java.util.ArrayList;

public class StrategoHeuristic implements IStateHeuristic {

    double bombValue = 0.0;
    double flagValue = 1.0;
    double pieceValueMultiplier = 0.1;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        StrategoGameState state = (StrategoGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if(playerResult == Utils.GameResult.LOSE) {
            return -1;
        }
        if(playerResult == Utils.GameResult.WIN) {
            return 1;
        }

        Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(playerId);
        ArrayList<Double> playerRanks = new ArrayList<>();
        ArrayList<Double> opponentRanks = new ArrayList<>();
        double pieceValue;

        for (Piece piece : state.gridBoard.getComponents()){
            if (piece != null){
                if (piece.getPieceType() == Piece.PieceType.BOMB) pieceValue = bombValue;
                else if (piece.getPieceType() == Piece.PieceType.FLAG) pieceValue = flagValue;
                else pieceValue = piece.getPieceRank() * pieceValueMultiplier;
                if (playerAlliance == piece.getPieceAlliance()) {
                    playerRanks.add(pieceValue);
                } else{
                    opponentRanks.add(pieceValue);
                }
            }
        }
        double sum = 0.0;
        for (double item : playerRanks){
            sum += item;
        }
        double avgPlayerRank = sum/playerRanks.size();
        sum=0.0;
        for (double item : opponentRanks){
            sum += item;
        }
        double avgOpponentRank = sum/opponentRanks.size();

        return avgPlayerRank - avgOpponentRank;
    }


}

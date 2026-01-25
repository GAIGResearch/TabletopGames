package games.stratego;

import core.AbstractGameState;
import core.components.BoardNode;
import core.interfaces.IStateFeatureVector;
import games.stratego.components.Piece;

import java.util.ArrayList;
import java.util.List;

public class StrategoFeatures implements IStateFeatureVector {

    @Override
    public String[] names() {
        /* */
        return new String[100];
    }


    // Gets the observartion vector
    // Index = position on board
    // Value = Piece Type
    @Override
    public double[] doubleVector(AbstractGameState state, int playerID) {
        /* Scheme
        * 1 Unknown Player Piece (I don't think this ever happens)
        * 2 - 13 Player Piece Type
        * -1 Unknown Opponent Piece
        * -2 - -13 Opponent Piece Type
        * 0 Empty Space */
        StrategoGameState sgs = (StrategoGameState) state;
        List<BoardNode> pieces = sgs.gridBoard.getComponents();
        List<Double> values = new ArrayList<>();
        int changeSignRed = playerID == 0 ? 1 : -1;
        int changeSignBlue = playerID == 0 ? -1 : 1;

        for (BoardNode bn : pieces) {
            Piece piece = (Piece) bn;
            if (piece != null) {

                // Player is Red
                if (playerID == 0) {

                    // Player Pieces
                    if (piece.getPieceAlliance() == Piece.Alliance.RED) {
                        values.add((double) ((piece.getPieceType().ordinal() + 1)));
                    }

                    // Opponent Piece is known
                    else if (piece.getPieceAlliance() == Piece.Alliance.BLUE && piece.isPieceKnown()) {
                        values.add((double) ((piece.getPieceType().ordinal() + 1) * changeSignBlue));
                    }

                    // Enemy Unknown
                    else {
                        values.add(-1.0);
                    }
                }

                // Player is Blue
                else if (playerID == 1) {
                    if (piece.getPieceAlliance() == Piece.Alliance.BLUE) {
                        values.add((double) ((piece.getPieceType().ordinal() + 1)));
                    }

                    // Opponent Piece is known
                    else if (piece.getPieceAlliance() == Piece.Alliance.RED && piece.isPieceKnown()) {
                        values.add((double) ((piece.getPieceType().ordinal() + 1) * changeSignRed));
                    }

                    // Enemy Unknown
                    else {
                        values.add(-1.0);
                    }
                }
            }
            // Empty Space
            else {
                values.add(0.0);
            }
        }
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }

}

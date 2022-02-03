package games.stratego.actions;

import core.AbstractGameState;
import core.components.GridBoard;
import games.stratego.StrategoGameState;
import games.stratego.components.Piece;

public class NormalMove extends Move{

    public NormalMove(int movedPieceID, int[] destinationCoordinate) {
        super(movedPieceID, destinationCoordinate);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Piece movedPiece = (Piece) gs.getComponentById(movedPieceID);
        GridBoard<Piece> board = ((StrategoGameState)gs).getGridBoard();
        board.setElement(movedPiece.getPiecePosition()[0], movedPiece.getPiecePosition()[1], null);
        board.setElement(destinationCoordinate[0], destinationCoordinate[1], movedPiece);
        movedPiece.setPiecePosition(destinationCoordinate);
        movedPiece.changePieceKnownFlag(true);
        return true;
    }

    @Override
    public NormalMove copy() {
        return new NormalMove(movedPieceID, destinationCoordinate.clone());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Move (" + movedPieceID + " [" + destinationCoordinate[0] + "," + destinationCoordinate[1] + "])";
    }

}
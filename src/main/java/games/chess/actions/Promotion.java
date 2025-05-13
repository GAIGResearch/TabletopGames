package games.chess.actions;
<<<<<<< HEAD
import java.util.Objects;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.chess.components.ChessPiece;
import games.chess.ChessGameState;
=======


import java.util.List;
import java.util.Objects;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MoveAction;



import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.chess.components.ChessPiece;
import games.chess.ChessGameState;
import games.chess.actions.MovePiece;
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
import games.chess.components.ChessPiece.ChessPieceType;

public class Promotion extends AbstractAction {
    private final int startX;
    private final int startY;

    private final int targetX;
    private final int targetY;
    private final ChessPieceType newPieceType;


    public Promotion(int startX, int startY, int targetX, int targetY, ChessPieceType newPieceType) {
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.newPieceType = newPieceType;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        MovePiece move = new MovePiece(startX, startY, targetX, targetY);
        if (!move.execute(gs)) {
<<<<<<< HEAD
            throw new IllegalStateException("Promotion failed: MovePiece execution failed.");
=======
            return false;
>>>>>>> 9d59845f675b7ddaa58e9adb5ed3781d501f1f5c
        }
        ChessGameState chessGameState = (ChessGameState) gs;

        ChessPiece piece = chessGameState.getPiece(targetX, targetY);
        if (piece != null && piece.getChessPieceType() == ChessPieceType.PAWN) {
            piece.setChessPieceType(newPieceType);
        }
        return true;
    }

    @Override
    public Promotion copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Promotion)) return false;
        Promotion other = (Promotion) obj;
        return startX == other.startX && startY == other.startY && targetX == other.targetX && targetY == other.targetY && newPieceType == other.newPieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startX, startY, targetX, targetY, newPieceType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Promotion: " + startX + "," + startY + " to " + targetX + "," + targetY + " to " + newPieceType;
    }

    public String getChessNotation() {
        return targetX + targetY + "=" + newPieceType.toString().charAt(0);


    }
}

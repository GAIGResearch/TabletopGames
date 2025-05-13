package games.chess.actions;
import java.util.Objects;
import core.AbstractGameState;
import core.actions.AbstractAction;
import games.chess.components.ChessPiece;
import games.chess.ChessGameState;
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
            throw new IllegalStateException("Promotion failed: MovePiece execution failed.");
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

package games.chess.components;



import core.components.Component;
import core.CoreConstants;

public class ChessBoard extends Component {

    private ChessPiece[][] board = new ChessPiece[8][8]; // 8x8 chess board
    private int size = 8; // Standard chess board size

    public ChessBoard() {
        super(CoreConstants.ComponentType.BOARD, "ChessBoard");
    }


    public ChessBoard(ChessPiece[][] board, int componentId) {
        super(CoreConstants.ComponentType.BOARD, "ChessBoard", componentId);
        this.board = board;
    }


    @Override
    public Component copy() {
        ChessPiece[][] newBoard = new ChessPiece[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (this.board[i][j] != null) {
                    newBoard[i][j] = this.board[i][j].copy(); 
                }
            }
        }
        return new ChessBoard(newBoard, this.getComponentID());
    }

    @Override
    public int hashCode() {
        int result = 17;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                result = 31 * result + (board[i][j] != null ? board[i][j].hashCode() : 0);
            }
        }
        return result;
    }

    public void setPiece(int x, int y, ChessPiece piece) {
        board[x][y] = piece; // Set the piece at the specified coordinates
        if (piece != null) {
            piece.setPosition(x, y); // Update the piece's position
        }
    }
    public ChessPiece getPiece(int x, int y) {
        return board[x][y]; // Get the piece at the specified coordinates
    }
    public ChessPiece[][] getBoard() {
        return board; // Get the entire board
    }
}
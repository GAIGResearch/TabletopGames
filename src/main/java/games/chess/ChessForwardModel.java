package games.chess;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.chess.ChessGameState;
import games.chess.actions.MovePiece;
import games.chess.components.ChessPiece;
import games.poker.actions.Check;

import static utilities.Utils.indexOf;
import static utilities.Utils.rotateImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>The forward model contains all the game rules and logic. It is mainly responsible for declaring rules for:</p>
 * <ol>
 *     <li>Game setup</li>
 *     <li>Actions available to players in a given game state</li>
 *     <li>Game events or rules applied after a player's action</li>
 *     <li>Game end</li>
 * </ol>
 */
public class ChessForwardModel extends StandardForwardModel {

    /**
     * Initializes all variables in the given game state. Performs initial game setup according to game rules, e.g.:
     * <ul>
     *     <li>Sets up decks of cards and shuffles them</li>
     *     <li>Gives player cards</li>
     *     <li>Places tokens on boards</li>
     *     <li>...</li>
     * </ul>
     *
     * @param firstState - the state to be modified to the initial game state.
     */
    @Override
    protected void _setup(AbstractGameState firstState) {

        ChessGameState chessState = (ChessGameState) firstState;
        
        
        //Add pieces to the board
        for (int i = 0; i < 8; i++) {
            chessState.setPiece(i, 1, new ChessPiece(ChessPiece.ChessPieceType.PAWN, 0, false));
            chessState.setPiece(i, 6, new ChessPiece(ChessPiece.ChessPieceType.PAWN, 1, false));
        }
        chessState.setPiece(0, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 0, false));
        chessState.setPiece(1, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 0, false));
        chessState.setPiece(2, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 0, false));
        chessState.setPiece(3, 0, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, 0, false));
        chessState.setPiece(4, 0, new ChessPiece(ChessPiece.ChessPieceType.KING, 0, false));
        chessState.setPiece(5, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 0, false));
        chessState.setPiece(6, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 0, false));
        chessState.setPiece(7, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 0, false));
        chessState.setPiece(0, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 1, false));
        chessState.setPiece(1, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 1, false));
        chessState.setPiece(2, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 1, false));
        chessState.setPiece(3, 7, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, 1, false));
        chessState.setPiece(4, 7, new ChessPiece(ChessPiece.ChessPieceType.KING, 1, false));
        chessState.setPiece(5, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 1, false));
        chessState.setPiece(6, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 1, false));
        chessState.setPiece(7, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 1, false));

        //Set the rest of the board to null (empty squares)
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                chessState.setPiece(j, i, null);
            }
        }
        //Update piece lists
        chessState.updatePieceLists();


        chessState.halfMoveClock = 0;
        chessState.repetitionCount = 0;
        



    }

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        List<AbstractAction> actions = new ArrayList<>();
        ChessGameState chessState = (ChessGameState) gameState;
        int playerId = chessState.getCurrentPlayer();
        List<ChessPiece> pieces = chessState.getPieces(playerId);
        for (ChessPiece piece : pieces) {
            actions.addAll(computeAvailableActionsPiece(chessState, piece));
        }


        return actions;
    }

    protected List<AbstractAction> computeAvailableActionsPiece(ChessGameState chessState, ChessPiece piece) {
        List<AbstractAction> actions = new ArrayList<>();
        HashMap<ChessPiece, int[]> pieceToPosition = chessState.getPieceToPosition();
        int[] position = pieceToPosition.get(piece);
        int x = position[0];
        int y = position[1];
        int playerId = piece.getOwnerId();
        int opponentId = 1-playerId; // Assuming two players with IDs 0 and 1
        ChessPiece.ChessPieceType type = piece.getChessPieceType();
        switch (type) {
            case KING:
                // King can move one square in any direction
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue; // Skip the current position
                        int newX = x + dx;
                        int newY = y + dy;
                        if (isWithinBounds(newX, newY) && !isOccupiedBy(chessState, newX, newY, playerId) && !isCellThreatened(chessState, newX, newY, playerId)) {
                            actions.add(new MovePiece(x, y, newX, newY));
                        }
                    }
                }
                break;
            case PAWN:
                // Pawn can move one square forward, or two squares forward if it hasn't moved yet
                int direction = (playerId == 0) ? 1 : -1; // White moves up, Black moves down
                int newX = x;
                int newY = y + direction;
                if (isWithinBounds(newX, newY) && !isOccupiedBy(chessState, newX, newY, playerId)) {
                    actions.add(new MovePiece(x, y, newX, newY));
                }
                // Check for double move
                if (!piece.getMoved()) {
                    newY = y + 2 * direction;
                    if (isWithinBounds(newX, newY) && !isOccupiedBy(chessState, newX, newY, playerId)) {
                        actions.add(new MovePiece(x, y, newX, newY));
                    }
                }
                // Check for captures
                newX = x - 1;
                newY = y + direction;
                if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY, opponentId)) {
                    actions.add(new MovePiece(x, y, newX, newY));
                }
                newX = x + 1;
                if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY, opponentId)) {
                    actions.add(new MovePiece(x, y, newX, newY));
                }
                break;

            // TODO: Implement movement logic for each piece type
            default:
                break;
        }
        return actions;
    }

    protected boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    protected boolean isOccupiedBy(ChessGameState chessState, int x, int y, int playerId) {

        ChessPiece piece = chessState.getPiece(x, y);
        if (piece == null) {
            return false;
        }
        // Check if the piece belongs to the same player
        if (piece.getOwnerId() == playerId) {
            return true; // The cell is occupied by the player's own piece
        } else if (piece.getOwnerId() != playerId) {
            return false; // The cell is occupied by an opponent's piece
        }
        //Raise an error if the piece is not owned by either player or null
        throw new IllegalStateException("Piece is not owned by either player or null: " + piece);
    }
    protected boolean isCellThreatened(ChessGameState chessState, int x, int y, int playerId) {
        // Check if the cell is threatened by any opponent piece
        return false; // TODO: Implement threat detection logic
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        ChessGameState chessState = (ChessGameState) currentState;

        endPlayerTurn(chessState);
    }
}

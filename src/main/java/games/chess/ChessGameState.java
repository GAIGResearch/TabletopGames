package games.chess;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import games.chess.components.ChessBoard;
import games.GameType;
import games.chess.actions.MovePiece;
import games.chess.components.ChessPiece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ChessGameState extends AbstractGameState {

    ChessBoard board = new ChessBoard();
    //List of white pieces
    List<ChessPiece> whitePieces = new ArrayList<>();
    //List of black pieces
    List<ChessPiece> blackPieces = new ArrayList<>();
    //Game state counts 
    Map<Integer, Integer> gameStateCounts = new HashMap<>();

    //Number of moves without a pawn move or capture
    int halfMoveClock = 0;


    
    

    public ChessGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);

    }

    @Override
    protected GameType _getGameType() {
        return GameType.Chess;
    }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(board);
        return components;
    }

    @Override
    protected ChessGameState _copy(int playerId) {
        ChessGameState copy = new ChessGameState(getGameParameters(), getNPlayers());
        copy.whitePieces = new ArrayList<>();
        copy.whitePieces.addAll(whitePieces.stream().map(ChessPiece::copy).toList());
        copy.blackPieces = new ArrayList<>();
        copy.blackPieces.addAll(blackPieces.stream().map(ChessPiece::copy).toList());
        copy.gameStateCounts = new HashMap<>(gameStateCounts);
        copy.halfMoveClock = halfMoveClock;
        copy.board = (ChessBoard) board.copy();
        return copy;
    }


    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // Simple value for each piece on the board, weighted by its type. Find the difference between the two players.
            double playerScore = 0.0; 
            double opponentScore = 0.0;
            for (ChessPiece piece : getPlayerPieces(1 - playerId)) {   
                ChessPiece.ChessPieceType type = piece.getChessPieceType();
                switch (type) {
                    case PAWN:
                        opponentScore += 1;
                        break;
                    case KNIGHT:
                    case BISHOP:
                        opponentScore += 3;
                        break;
                    case ROOK:
                        opponentScore += 5;
                        break;
                    case QUEEN:
                        opponentScore += 9;
                        break;
                    case KING:
                        opponentScore += 0;
                        break;
                }
            }
            for (ChessPiece piece : getPlayerPieces(playerId)) {
                ChessPiece.ChessPieceType type = piece.getChessPieceType();
                switch (type) {
                    case PAWN:
                        playerScore += 1;
                        break;
                    case KNIGHT:
                    case BISHOP:
                        playerScore += 3;
                        break;
                    case ROOK:
                        playerScore += 5;
                        break;
                    case QUEEN:
                        playerScore += 9;
                        break;
                    case KING:
                        playerScore += 0;
                        break;
                }
            }
            //Reward check
            if (isInCheck(1-playerId)) {
                playerScore += 1;
            }
            //Penalize oppenent king movement choices
            int opponentKingActions = computeAvailableActionsKing(getKingPosition(1-playerId)[0], getKingPosition(1-playerId)[1], 1-playerId).size();
            playerScore -= opponentKingActions * 0.2; // TODO: Tune this value to be more or less punishing

            return (playerScore - opponentScore)/100; // TODO: Normalize this value to be between 0 and 1
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    @Override
    public double getGameScore(int playerId) {
        if (isNotTerminal()) {
            return 0;
        } else {
            return (double) (getPlayerResults()[playerId].value+1) / 2.0;
        }
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ChessGameState that &&
                super.equals(o) &&
                this.halfMoveClock == that.halfMoveClock &&
                this.whitePieces.equals(that.whitePieces) &&
                this.blackPieces.equals(that.blackPieces) &&
                this.gameStateCounts.equals(that.gameStateCounts) &&
                this.board.equals(that.board);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), halfMoveClock, whitePieces, blackPieces, gameStateCounts, board);
    }

    public ChessBoard getBoard() {
        return board;
    }

    public void setPiece(int x, int y, ChessPiece piece) {
        board.setPiece(x, y, piece);
        if (piece != null) {
            if (piece.getOwnerId() == 0) {
                whitePieces.add(piece);
            } else if (piece.getOwnerId() == 1) {
                blackPieces.add(piece);
            } else {
                throw new IllegalArgumentException("Invalid player ID: " + piece.getOwnerId());
            }
        }
    }
    public ChessPiece getPiece(int x, int y) {
        return board.getPiece(x, y);
    }
    public List<ChessPiece> getPlayerPieces(int playerId) {
        if (playerId == 0) {
            return whitePieces;
        } else if (playerId == 1) {
            return blackPieces;
        } else {
            throw new IllegalArgumentException("Invalid player ID: " + playerId);
        }
    }

    public void deletePiece(ChessPiece piece) {
        if (piece == null) {
            throw new IllegalArgumentException("Trying to delete a null piece");
        }
        int[] position = piece.getPosition();
        board.setPiece(position[0], position[1], null); // Remove the piece from the board
        if (piece.getOwnerId() == 0) {
            whitePieces.remove(piece);
        } else if (piece.getOwnerId() == 1) {
            blackPieces.remove(piece);
        }
    }

    public void updatePiecePosition(ChessPiece piece, int x, int y) {
        deletePiece(piece); // Remove the piece from its original position
        piece.setPosition(x, y); // Update the piece's position
        setPiece(x, y, piece); // Move the piece to the target position
    }


    public int[] getKingPosition(int playerId) {
        for (ChessPiece piece : getPlayerPieces(playerId)) {
            if (piece.getChessPieceType() == ChessPiece.ChessPieceType.KING) {
                return piece.getPosition();
            }
        }
        throw new IllegalArgumentException("King not found for player " + playerId);
    }

    //A few duplicate methods from the forward model to use in the heuristic function. TODO: decide wheter to have them here or in the forward model, might want to use legal moves in the future.
    public boolean isInCheck(int playerId) {
        // Check if the player's king is in check
        int[] kingPosition = this.getKingPosition(playerId);
        int kingX = kingPosition[0];
        int kingY = kingPosition[1];
        // Check if any opponent piece can attack the king's position
        return isCellThreatened(kingX, kingY, 1-playerId);
    }

    public int isOccupiedBy(int x, int y) {
        ChessPiece piece = getPiece(x, y);
        if (piece == null) {
            return -1;
        }
        // Check if the piece belongs to the same player
        return piece.getOwnerId(); // Return the player ID of the piece
    }

    public boolean isCellThreatened(int x, int y, int playerId) {
        // Check if the cell is threatened by any piece from playerId
        for (ChessPiece piece : getPlayerPieces(playerId)) {
            int[] position = piece.getPosition();
            int pieceX = position[0];
            int pieceY = position[1];
            
            boolean canMove = true;
            ChessPiece.ChessPieceType type = piece.getChessPieceType();
            switch (type) {
                case KING:
                    // King can move one square in any direction
                    if (Math.abs(pieceX - x) <= 1 && Math.abs(pieceY - y) <= 1) {
                        return true;
                    }
                    break;
                case PAWN:
                    // Pawn can attack diagonally
                    int direction = (playerId == 0) ? 1 : -1; // White moves up, Black moves down
                    if ((x == pieceX - 1 || x == pieceX + 1) && y == pieceY + direction) {
                        return true; // Pawn can attack diagonally
                    }
                    break;
                case ROOK:
                    // Rook can move any number of squares horizontally or vertically but we need to check for obstacles
                    // Check if the rook is in the same row or column as the target cell
                    if (pieceX == x || pieceY == y) {
                        // Check if there are no pieces between the rook and the target cell
                        int stepX = (x - pieceX) == 0 ? 0 : (x - pieceX) / Math.abs(x - pieceX);
                        int stepY = (y - pieceY) == 0 ? 0 : (y - pieceY) / Math.abs(y - pieceY);
                        if (stepX == 0 && stepY == 0) {
                            canMove = false; // Cannot move to the same cell (it would be captured)
                        }
                        for (int i = 1; i < Math.max(Math.abs(x - pieceX), Math.abs(y - pieceY)); i++) {
                            int checkX = pieceX + i * stepX;
                            int checkY = pieceY + i * stepY;
                            if (isOccupiedBy(checkX, checkY) != -1) {
                                canMove = false;
                                break; // There is a piece in the way, so the rook cannot attack
                            }
                        }
                        if (canMove) {
                            return true; // Rook can attack the target cell
                        }
                    }
                    break;
                case BISHOP:
                    // Bishop can move any number of squares diagonally
                    if (Math.abs(pieceX - x) == Math.abs(pieceY - y) && pieceX != x) {
                        // Check if there are no pieces between the bishop and the target cell
                        int stepX = (x - pieceX) / Math.abs(x - pieceX);
                        int stepY = (y - pieceY) / Math.abs(y - pieceY);
                        for (int i = 1; i < Math.abs(x - pieceX); i++) {
                            int checkX = pieceX + i * stepX;
                            int checkY = pieceY + i * stepY;
                            if (isOccupiedBy(checkX, checkY) != -1) {
                                canMove = false;
                                break; // There is a piece in the way, so the bishop cannot attack
                            }
                        }
                        if (canMove) {
                            return true; // Bishop can attack the target cell
                        }
                    }
                    break;
                case QUEEN:
                    // Queen can move like both a rook and a bishop
                    if (pieceX == x || pieceY == y || (Math.abs(pieceX - x) == Math.abs(pieceY - y) && pieceX != x)) {
                        // Check if there are no pieces between the queen and the target cell
                        int stepX = (x - pieceX) == 0 ? 0 : (x - pieceX) / Math.abs(x - pieceX);
                        int stepY = (y - pieceY) == 0 ? 0 : (y - pieceY) / Math.abs(y - pieceY);
                        if (stepX == 0 && stepY == 0) {
                            canMove = false; // Cannot move to the same cell (it would be captured)
                        }
                        for (int i = 1; i < Math.max(Math.abs(x - pieceX), Math.abs(y - pieceY)); i++) {
                            int checkX = pieceX + i * stepX;
                            int checkY = pieceY + i * stepY;
                            if (isOccupiedBy(checkX, checkY) != -1) {
                                canMove = false;
                                break;
                            }
                        }
                        if (canMove) {
                            return true; // Queen can attack the target cell
                        }
                    }
                    break;
                case KNIGHT:
                    // Knight can move in an "L" shape: two squares in one direction and one square perpendicular
                    if ((Math.abs(pieceX - x) == 2 && Math.abs(pieceY - y) == 1) || (Math.abs(pieceX - x) == 1 && Math.abs(pieceY - y) == 2)) {
                        return true;
                    }
                    break;
            }
        }

        return false; 
    }

    protected List<AbstractAction> computeAvailableActionsKing(int x, int y, int playerId) {
        List<AbstractAction> actions = new ArrayList<>();
        int newX, newY;
        // King can move one square in any direction
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip the current position
                newX = x + dx;
                newY = y + dy;
                MovePiece move = new MovePiece(x, y, newX, newY);
                if (isWithinBounds(newX, newY) && isOccupiedBy(newX, newY) != playerId && !isCellThreatened(newX, newY, 1-playerId)) {
                    actions.add(move);
                }
            }
        }
        return actions;
    }

    public boolean isWithinBounds(int x, int y) {

        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    public String getBoardString() {
        StringBuilder sb = new StringBuilder();
        ChessPiece[][] boardArray = board.getBoard();
        for (int i = 0; i < boardArray.length; i++) {
            sb.append(8-i).append(" ");
            for (int j = 0; j < boardArray[i].length; j++) {
                if (boardArray[j][7-i] != null) {
                    sb.append(boardArray[j][7-i].toString()).append(" ");
                } else {
                    sb.append("-- ");
                }
            }
            sb.append("\n");
        }
        sb.append("  a  b  c  d  e  f  g  h\n");
        return sb.toString();
    }
    public void incrementHalfMoveClock() {
        halfMoveClock++;
    }
    public void resetHalfMoveClock() {
        halfMoveClock = 0;
    }
    public boolean AddCheckRepetitionCount() {
        // Check if the current board state has been seen before
        int boardHash = Objects.hash(board.hashCode(), getCurrentPlayer());
        if (gameStateCounts.containsKey(boardHash)) {
            gameStateCounts.put(boardHash, gameStateCounts.get(boardHash) + 1);
            if (gameStateCounts.get(boardHash) >= 3) { 
                return true; // Draw by repetition
            }
        } else {
            gameStateCounts.put(boardHash, 1); // Add the new board state to the map
        }
        return false; // No draw by repetition
    }
    
    public String getChessCoordinates(int x, int y) {
        // Convert the coordinates to chess notation (e.g., a1, b2, etc.)
        char file = (char) ('a' + x);
        char rank = (char) ('1' + y);
        return "" + file + rank;
    }

    public void resetEnPassant() {
        for (ChessPiece piece : getPlayerPieces(getCurrentPlayer())){
            if (piece.getChessPieceType() == ChessPiece.ChessPieceType.PAWN) {
                piece.setEnPassant(false); // Reset en passant for all pawns
                board.setPiece(piece.getPosition()[0], piece.getPosition()[1], piece); // Update the board with the new piece state
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Chess Game State Hash:").append(hashCode()).append("\n");
        sb.append("White Pieces: ").append(whitePieces.hashCode()).append("\n");
        sb.append("Black Pieces: ").append(blackPieces.hashCode()).append("\n");
        sb.append("Game State Counts: ").append(gameStateCounts.hashCode()).append("\n");
        sb.append("Half Move Clock: ").append(halfMoveClock).append("\n");
        sb.append("Board:\n").append(board.hashCode()).append("\n");
        return sb.toString();
    }
}

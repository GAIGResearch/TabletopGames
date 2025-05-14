package games.chess;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.CoreConstants;
import games.chess.actions.Castle;
import games.chess.actions.EnPassant;
import games.chess.actions.MovePiece;
import games.chess.actions.Promotion;
import games.chess.components.ChessPiece;


import java.util.ArrayList;

import java.util.List;


public class ChessForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {

        ChessGameState chessState = (ChessGameState) firstState;
        chessState.blackPieces.clear();
        chessState.whitePieces.clear(); 
        
        
        //Add pieces to the board
        for (int i = 0; i < 8; i++) {
            chessState.setPiece(i, 1, new ChessPiece(ChessPiece.ChessPieceType.PAWN, 0, i,1, ChessPiece.MovedState.NOT_MOVED));
            chessState.setPiece(i, 6, new ChessPiece(ChessPiece.ChessPieceType.PAWN, 1, i,6, ChessPiece.MovedState.NOT_MOVED));
        }
        chessState.setPiece(0, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 0, 0, 0, ChessPiece.MovedState.NOT_MOVED));
        chessState.setPiece(1, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 0, 1, 0, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(2, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 0, 2, 0, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(3, 0, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, 0, 3, 0, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(4, 0, new ChessPiece(ChessPiece.ChessPieceType.KING, 0, 4, 0, ChessPiece.MovedState.NOT_MOVED));
        chessState.setPiece(5, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 0, 5, 0, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(6, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 0, 6, 0, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(7, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 0, 7, 0, ChessPiece.MovedState.NOT_MOVED));
        chessState.setPiece(0, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 1, 0, 7, ChessPiece.MovedState.NOT_MOVED));
        chessState.setPiece(1, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 1, 1, 7, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(2, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 1, 2, 7, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(3, 7, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, 1, 3, 7, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(4, 7, new ChessPiece(ChessPiece.ChessPieceType.KING, 1, 4, 7, ChessPiece.MovedState.NOT_MOVED));
        chessState.setPiece(5, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, 1, 5, 7, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(6, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, 1, 6, 7, ChessPiece.MovedState.NOT_RELEVANT));
        chessState.setPiece(7, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, 1, 7, 7, ChessPiece.MovedState.NOT_MOVED));
        chessState.AddCheckRepetitionCount();

        chessState.halfMoveClock = 0;
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
        for (ChessPiece piece : chessState.getPlayerPieces(playerId)) {
            actions.addAll(computeAvailableActionsPiece(chessState, piece));
        }


        return actions;
    }


    protected List<AbstractAction> computeAvailableActionsPiece(ChessGameState chessState, ChessPiece piece) {
        List<AbstractAction> actions = new ArrayList<>();
        int[] position = piece.getPosition();
        int x = position[0];
        int y = position[1];
        int playerId = piece.getOwnerId();
        ChessPiece.ChessPieceType type = piece.getChessPieceType();
        switch (type) {
            case KING:
                actions.addAll(computeAvailableActionsKing(chessState, x, y, playerId));
                break;
            case PAWN:
                actions.addAll(computeAvailableActionsPawn(chessState, x, y, playerId));
                break;
            case ROOK:
                actions.addAll(computeAvailableActionsRook(chessState, x, y, playerId));
                break;
            case BISHOP:
                actions.addAll(computeAvailableActionsBishop(chessState, x, y, playerId));
                break;
            case QUEEN:
                // Queen can move like both a rook and a bishop
                actions.addAll(computeAvailableActionsRook(chessState, x, y, playerId));
                actions.addAll(computeAvailableActionsBishop(chessState, x, y, playerId));
                break;
            case KNIGHT:
                actions.addAll(computeAvailableActionsKnight(chessState, x, y, playerId));
                break;
        }
        return actions;
    }

    protected boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    protected int isOccupiedBy(ChessGameState chessState, int x, int y) {

        ChessPiece piece = chessState.getPiece(x, y);
        if (piece == null) {
            return -1;
        }
        // Check if the piece belongs to the same player
        return piece.getOwnerId(); // Return the player ID of the piece
    }
    protected boolean isCellThreatened(ChessGameState chessState, int x, int y, int playerId) {
        // Check if the cell is threatened by any piece from playerId
        for (ChessPiece piece : chessState.getPlayerPieces(playerId)) {
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
                            if (isOccupiedBy(chessState, checkX, checkY) != -1) {
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
                            if (isOccupiedBy(chessState, checkX, checkY) != -1) {
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
                    if (pieceX == x || pieceY == y || ((Math.abs(pieceX - x) == Math.abs(pieceY - y) && pieceX != x))) {
                        // Check if there are no pieces between the queen and the target cell
                        int stepX = (x - pieceX) == 0 ? 0 : (x - pieceX) / Math.abs(x - pieceX);
                        int stepY = (y - pieceY) == 0 ? 0 : (y - pieceY) / Math.abs(y - pieceY);
                        if (stepX == 0 && stepY == 0) {
                            canMove = false; // Cannot move to the same cell (it would be captured)
                        }
                        for (int i = 1; i < Math.max(Math.abs(x - pieceX), Math.abs(y - pieceY)); i++) {
                            int checkX = pieceX + i * stepX;
                            int checkY = pieceY + i * stepY;
                            if (isOccupiedBy(chessState, checkX, checkY) != -1) {
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

    protected List<AbstractAction> computeAvailableActionsKing(ChessGameState chessState, int x, int y, int playerId) {
        List<AbstractAction> actions = new ArrayList<>();
        int newX, newY;
        // King can move one square in any direction
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip the current position
                newX = x + dx;
                newY = y + dy;
                MovePiece move = new MovePiece(x, y, newX, newY);
                if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) != playerId && !CheckAfterMove(chessState, move)) {
                    actions.add(move);
                }
            }
        }
        //Castling logic
        // Check if the king and rook have not moved yet, there are no pieces between them, the king is not in check, and the squares the king moves through are not attacked
        // Check for castling to the left (queenside)
        ChessPiece kingChessPiece = chessState.getPiece(x, y);

        ChessPiece rookChessPiece = chessState.getPiece(0, y);
        if (kingChessPiece.getMoved() == ChessPiece.MovedState.NOT_MOVED && rookChessPiece != null && rookChessPiece.getOwnerId() == playerId && rookChessPiece.getMoved() == ChessPiece.MovedState.NOT_MOVED) {
            Castle move = new Castle(Castle.CastleType.QUEEN_SIDE);
            if (isOccupiedBy(chessState, 1, y) == -1 && isOccupiedBy(chessState, 2, y) == -1 && isOccupiedBy(chessState, 3, y) == -1 && !isInCheck(chessState, playerId) && !isCellThreatened(chessState, x-2, y, 1-playerId) && !isCellThreatened(chessState, x-1, y, 1-playerId)) {
                actions.add(move);
            }
        }
        // Check for castling to the right (kingside)
        rookChessPiece = chessState.getPiece(7, y);
        if (kingChessPiece.getMoved() == ChessPiece.MovedState.NOT_MOVED && rookChessPiece != null && rookChessPiece.getOwnerId() == playerId && rookChessPiece.getMoved() == ChessPiece.MovedState.NOT_MOVED) {
            Castle move = new Castle(Castle.CastleType.KING_SIDE);
            if (isOccupiedBy(chessState, 5, y) == -1 && isOccupiedBy(chessState, 6, y) == -1 && !isInCheck(chessState, playerId) && !isCellThreatened(chessState, x+2, y, 1-playerId) && !isCellThreatened(chessState, x+1, y, 1-playerId)) {
                actions.add(move);
            }
        }

        
        return actions;
    }

    protected List<AbstractAction> computeAvailableActionsPawn(ChessGameState chessState, int x, int y, int playerId) {
        List<AbstractAction> actions = new ArrayList<>();
        int newX, newY;
        ChessPiece enPassantTarget;
        AbstractAction move;
        // Pawn can move one square forward, or two squares forward if it hasn't moved yet
        int direction = (playerId == 0) ? 1 : -1; // White moves up, Black moves down
        newX = x;
        newY = y + direction;
        move = new MovePiece(x, y, newX, newY);
        if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) == -1 && !CheckAfterMove(chessState, move)) {
            //check if the pawn is on the last row for promotion
            if (newY == 0 || newY == 7) {
                // Pawn can be promoted to any piece type (except king)
                for (ChessPiece.ChessPieceType type : ChessPiece.ChessPieceType.values()) {
                    if (type != ChessPiece.ChessPieceType.KING) {
                        actions.add(new Promotion(x, y, newX, newY, type));
                    }
                }
            } else {
                actions.add(move);
            }
        }
        // Check for null piece
        if (chessState.getPiece(x, y) == null) {
            return actions; // No piece to move, return empty actions
        }


        // Check for double move
        if (chessState.getPiece(x, y).getMoved() == ChessPiece.MovedState.NOT_MOVED) {
            newY = y + 2 * direction;
            move = new MovePiece(x, y, newX, newY);
            if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) == -1 && isOccupiedBy(chessState, x, y + direction) == -1 && !CheckAfterMove(chessState, move)) {
                actions.add(move);
            }
        }
        // Check for captures
        newX = x - 1;
        newY = y + direction;
        move = new MovePiece(x, y, newX, newY);
        if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) == 1-playerId && !CheckAfterMove(chessState, move)) {
            //Check for promotion
            if (newY == 0 || newY == 7) {
                // Pawn can be promoted to any piece type (except king)
                for (ChessPiece.ChessPieceType type : ChessPiece.ChessPieceType.values()) {
                    if (type != ChessPiece.ChessPieceType.KING) {
                        actions.add(new Promotion(x, y, newX, newY, type));
                    }
                }
            } else
                actions.add(new MovePiece(x, y, newX, newY));
        }
        //Enpassant logic
        enPassantTarget = isWithinBounds(newX, y) ? chessState.getPiece(newX, y) : null;
        move = new EnPassant(x, y, newX);
        if (isWithinBounds(newX, newY) && enPassantTarget != null && enPassantTarget.getChessPieceType() == ChessPiece.ChessPieceType.PAWN &&
         enPassantTarget.getEnPassant() && enPassantTarget.getOwnerId() == 1-playerId  && !CheckAfterMove(chessState, move)) {
            actions.add(move);
        }


        newX = x + 1;
        move = new MovePiece(x, y, newX, newY);
        if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) == 1-playerId && !CheckAfterMove(chessState, move)) {
            //Check for promotion
            if (newY == 0 || newY == 7) {
                // Pawn can be promoted to any piece type (except king)
                for (ChessPiece.ChessPieceType type : ChessPiece.ChessPieceType.values()) {
                    if (type != ChessPiece.ChessPieceType.KING) {
                        actions.add(new Promotion(x, y, newX, newY, type));
                    }
                }
            } else
                actions.add(new MovePiece(x, y, newX, newY));
        }

        //Enpassant logic
        enPassantTarget = isWithinBounds(newX, y) ? chessState.getPiece(newX, y) : null;
        move = new EnPassant(x, y, newX);
        if (isWithinBounds(newX, newY) && enPassantTarget != null && enPassantTarget.getChessPieceType() == ChessPiece.ChessPieceType.PAWN &&
         enPassantTarget.getEnPassant() && enPassantTarget.getOwnerId() == 1-playerId  && !CheckAfterMove(chessState, move)) {
            actions.add(move);
        }

        return actions;
    }

    protected List<MovePiece> computeAvailableActionsRook(ChessGameState chessState, int x, int y, int playerId) {
        List<MovePiece> actions = new ArrayList<>();
        int newX, newY;
        // Rook can move any number of squares horizontally or vertically, but we need to check for obstacles
        // Horizontal moves
        // Right direction
        for (int i = 1; i < 8-x; i++) {
            newX = x + i;
            newY = y;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Left direction
        for (int i = 1; i <= x; i++) {
            newX = x - i;
            newY = y;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Forward direction
        for (int i = 1; i < 8-y; i++) {
            newX = x;
            newY = y + i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Backward direction
        for (int i = 1; i <= y; i++) {
            newX = x;
            newY = y - i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        return actions;
    }

    protected List<MovePiece> computeAvailableActionsBishop(ChessGameState chessState, int x, int y, int playerId) {
        List<MovePiece> actions = new ArrayList<>();
        int newX, newY;
        // Bishop can move any number of squares diagonally, but we need to check for obstacles
        // Diagonal moves
        // Forward-right direction
        for (int i = 1; i < 8-x && i < 8-y; i++) {
            newX = x + i;
            newY = y + i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Forward-left direction
        for (int i = 1; i <= x && i < 8-y; i++) {
            newX = x - i;
            newY = y + i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Backward-right direction
        for (int i = 1; i < 8-x && i <= y; i++) {
            newX = x + i;
            newY = y - i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        // Backward-left direction
        for (int i = 1; i <= x && i <= y; i++) {
            newX = x - i;
            newY = y - i;
            MovePiece move = new MovePiece(x, y, newX, newY);
            boolean check = CheckAfterMove(chessState, move);
            int occupiedBy = isOccupiedBy(chessState, newX, newY);
            if (occupiedBy == -1 && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
            } else if (occupiedBy == playerId) {
                break; // Stop if blocked by own piece
            } else if (occupiedBy == 1-playerId && !check) {
                actions.add(new MovePiece(x, y, newX, newY));
                break; // Stop after capturing
            } else if (occupiedBy == 1-playerId && check) {
                break; // Can't capture because it leaves the king in check. Stop looking further (blocked by enemy piece)
            }
        }
        return actions;
    }   

    protected List<MovePiece> computeAvailableActionsKnight(ChessGameState chessState, int x, int y, int playerId) {
        List<MovePiece> actions = new ArrayList<>();
        int newX, newY;
        // Knight can move in an "L" shape: two squares in one direction and one square perpendicular
        int[][] moves = {
                {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
        };
        for (int[] move : moves) {
            newX = x + move[0];
            newY = y + move[1];
            MovePiece movePiece = new MovePiece(x, y, newX, newY);
            // Check if the new position is within bounds and not occupied by own piece
            if (isWithinBounds(newX, newY) && isOccupiedBy(chessState, newX, newY) != playerId && !CheckAfterMove(chessState, movePiece)) {
                actions.add(movePiece);
            }
        }
        return actions;
    }

    protected boolean CheckAfterMove(ChessGameState chessState, AbstractAction action) {
        
        // Check if any opponent piece can attack the king's position after a move
        int playerId = chessState.getCurrentPlayer();
        ChessGameState nextGameState = chessState._copy(playerId);
        action.execute(nextGameState);
        int[] kingPosition = nextGameState.getKingPosition(playerId);
        int kingX = kingPosition[0];
        int kingY = kingPosition[1];
        return isCellThreatened(nextGameState, kingX, kingY, 1-playerId);
    }

    protected boolean isInCheck(AbstractGameState gameState, int playerId) {
        // Check if the player's king is in check
        ChessGameState chessState = (ChessGameState) gameState;
        int[] kingPosition = chessState.getKingPosition(playerId);
        int kingX = kingPosition[0];
        int kingY = kingPosition[1];
        // Check if any opponent piece can attack the king's position
        return isCellThreatened(chessState, kingX, kingY, 1-playerId);
    }

    protected void checkGameEnd(ChessGameState chessState) {

        // Check if the game is over (checkmate or stalemate)
        List<AbstractAction> availableActions = computeAvailableActions(chessState);
        ChessParameters chessParameters = (ChessParameters) chessState.getGameParameters();
        if (availableActions.isEmpty() && chessState.isNotTerminal()) {
            // No available actions, check for stalemate or checkmate
            if (isInCheck(chessState, chessState.getCurrentPlayer())) {
                // Checkmate
                chessState.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, chessState.getCurrentPlayer());
                chessState.setPlayerResult(CoreConstants.GameResult.WIN_GAME, 1 - chessState.getCurrentPlayer());
                // System.out.println("Checkmate! Player " + chessState.getCurrentPlayer() + " loses.");
            } else {
                // Stalemate
                chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, chessState.getCurrentPlayer());
                chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1 - chessState.getCurrentPlayer());
                // System.out.println("Stalemate!");
            }
            chessState.setGameStatus(CoreConstants.GameResult.GAME_END);
        }
        //Check half-move clock. If it reaches 50, the game is drawn if a player claims so, and is automatically drawn at 75. 
        //The claim is not implemented, and we will just end the game at 50 moves, i.e. two moves per player for a total of 100.
        //Order here is important because checkmate takes precedence over 50-move rule. 
        if (chessState.isNotTerminal() && chessState.halfMoveClock >= 100) {
            chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, chessState.getCurrentPlayer());
            chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1 - chessState.getCurrentPlayer());
            chessState.setGameStatus(CoreConstants.GameResult.GAME_END);
            // System.out.println("Draw by 50-move rule");
        }


        //Check draw by repetition. The game is drawn if a board position is repeated drawByRepetition times. Default is 3, if set to 0, it is disabled.
        if (chessState.isNotTerminal() && chessParameters.drawByRepetition != 0) {
            if (chessState.AddCheckRepetitionCount()) {
                chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, chessState.getCurrentPlayer());
                chessState.setPlayerResult(CoreConstants.GameResult.DRAW_GAME, 1 - chessState.getCurrentPlayer());
                chessState.setGameStatus(CoreConstants.GameResult.GAME_END);
                // System.out.println("Draw by "+ chessParameters.drawByRepetition + "-fold repetition");
            }
        }
        //TODO: Check for insufficient material
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        ChessGameState chessState = (ChessGameState) currentState;
        endPlayerTurn(chessState);
        if (chessState.getCurrentPlayer() == 0) {
            endRound(chessState);
        }
        chessState.resetEnPassant();

        //Print board state
        // System.out.println("Available actions: " + computeAvailableActions(chessState) + " for player " + chessState.getCurrentPlayer());
        // System.out.println(chessState.getBoardString());
        // System.out.println("Turn number: " + chessState.getTurnCounter() + " Half-move clock: " + chessState.halfMoveClock);
        if (chessState.isNotTerminal()) {
            checkGameEnd(chessState);
        } 
    }

    @Override
    public void endGame(AbstractGameState gameState) {
        ChessGameState chessState = (ChessGameState) gameState;
        chessState.setGameStatus(CoreConstants.GameResult.GAME_END);
    }
}

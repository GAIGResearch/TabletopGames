package games.chess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.codahale.metrics.Slf4jReporter;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.chess.actions.Castle;
import games.chess.actions.EnPassant;
import games.chess.actions.MovePiece;
import games.chess.actions.Castle.CastleType;
import games.chess.components.ChessPiece;

public class ActionTests {
    ChessForwardModel fm = new ChessForwardModel();
    Game game = GameType.Chess.createGameInstance(2, new ChessParameters());

    @Test
    public void InitialActionsTest() {
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //20 possible moves, 16 pawns and 4 knights
        // White player
        fm.computeAvailableActions(state);
        assertEquals(20, fm.computeAvailableActions(state).size());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        // Black player
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(20, fm.computeAvailableActions(state).size());
    }

    @Test 
    public void KingCastlingTest() {
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //Setup the board for castling (move pawn, bishop and knight)
        //Pawns
        fm.next(state, new MovePiece(4,1, 4, 3));
        fm.next(state, new MovePiece(4,6, 4, 4));
        //Bishops
        fm.next(state, new MovePiece(5,0, 2, 3));
        fm.next(state, new MovePiece(5,7, 2, 4));
        //Knights
        fm.next(state, new MovePiece(6,0, 5, 2));
        fm.next(state, new MovePiece(6,7, 5, 5));
        //Castle
        // Check castle is in available actions
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new Castle(CastleType.KING_SIDE))));
        fm.next(state, new Castle(CastleType.KING_SIDE));
        //Check the king and rook positions
        int[] kingPos = {6, 0};
        ChessPiece king = state.getPiece(kingPos[0], kingPos[1]);
        ChessPiece rook = state.getPiece(kingPos[0] - 1, kingPos[1]);
        assertEquals(king.getChessPieceType(), ChessPiece.ChessPieceType.KING);
        assertEquals(rook.getChessPieceType(), ChessPiece.ChessPieceType.ROOK);
        assertEquals(king.getOwnerId(), 0);
        assertEquals(rook.getOwnerId(), 0);
        assertEquals(king.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(kingPos[0], 6);
        assertEquals(kingPos[1], 0);
        assertEquals(rook.getPosition()[0], 5);
        assertEquals(rook.getPosition()[1], 0);
        //Black player
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new Castle(CastleType.KING_SIDE))));

        fm.next(state, new Castle(CastleType.KING_SIDE));
        //Check the king and rook positions
        kingPos = new int[]{6, 7};
        king = state.getPiece(kingPos[0], kingPos[1]);
        rook = state.getPiece(kingPos[0] - 1, kingPos[1]);
        assertEquals(king.getChessPieceType(), ChessPiece.ChessPieceType.KING);
        assertEquals(rook.getChessPieceType(), ChessPiece.ChessPieceType.ROOK);
        assertEquals(king.getOwnerId(), 1);
        assertEquals(rook.getOwnerId(), 1);
        assertEquals(king.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(kingPos[0], 6);
        assertEquals(kingPos[1], 7);
        assertEquals(rook.getPosition()[0], 5);
        assertEquals(rook.getPosition()[1], 7);
    }

    @Test
    public void QueenCastlingTest() {
        List<AbstractAction> actions;
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //Setup the board for castling (move pawn, bishop, knight and queen)
        //Pawns
        fm.next(state, new MovePiece(3,1, 3, 3));
        fm.next(state, new MovePiece(3,6, 3, 4));
        fm.next(state, new MovePiece(4,1, 4, 3));
        fm.next(state, new MovePiece(4,6, 4, 4));
        //Bishops
        fm.next(state, new MovePiece(2,0, 5, 3));
        fm.next(state, new MovePiece(2,7, 5, 4));
        //Knights
        fm.next(state, new MovePiece(1,0, 2, 2));
        fm.next(state, new MovePiece(1,7, 2, 5));

        actions = fm.computeAvailableActions(state);
        // Check castle is not in available actions for white
        assertTrue(actions.stream().noneMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE))));
        fm.next(state, new MovePiece(3,0, 7, 3));
        // Check castle is not in available actions for black
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().noneMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE))));
        fm.next(state, new MovePiece(3,7, 7, 4));
        // Check castle is in available actions for white
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().noneMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE))));// The way to castle is threatened by the queen
        fm.next(state, new MovePiece(7, 3, 7, 2));// Move the queen to a different position
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE)))); // The way to castle is not threatened anymore
        fm.next(state, new MovePiece(7, 4, 7, 5)); // Move the queen to a different position
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE)))); // The way to castle is not threatened anymore

        fm.next(state, new Castle(CastleType.QUEEN_SIDE));
        //Check the king and rook positions
        int[] kingPos = {2, 0};
        ChessPiece king = state.getPiece(kingPos[0], kingPos[1]);
        ChessPiece rook = state.getPiece(kingPos[0] + 1, kingPos[1]);
        assertEquals(king.getChessPieceType(), ChessPiece.ChessPieceType.KING);
        assertEquals(rook.getChessPieceType(), ChessPiece.ChessPieceType.ROOK);
        assertEquals(king.getOwnerId(), 0);
        assertEquals(rook.getOwnerId(), 0);
        assertEquals(king.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getPosition()[0], 3);
        assertEquals(rook.getPosition()[1], 0);
        //Black player
        actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new Castle(CastleType.QUEEN_SIDE))));
        
        fm.next(state, new Castle(CastleType.QUEEN_SIDE));

        kingPos = new int[]{2, 7};
        king = state.getPiece(kingPos[0], kingPos[1]);
        rook = state.getPiece(kingPos[0] + 1, kingPos[1]);
        assertEquals(king.getChessPieceType(), ChessPiece.ChessPieceType.KING);
        assertEquals(rook.getChessPieceType(), ChessPiece.ChessPieceType.ROOK);
        assertEquals(king.getOwnerId(), 1);
        assertEquals(rook.getOwnerId(), 1);
        assertEquals(king.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getMoved(), ChessPiece.MovedState.MOVED);
        assertEquals(rook.getPosition()[0], 3);
        assertEquals(rook.getPosition()[1], 7);
    }
    @Test
    public void EnPassantTest() {
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //Setup the board for en passant
        //Pawns
        fm.next(state, new MovePiece(4,1, 4, 3));
        fm.next(state, new MovePiece(4,6, 4, 5));
        fm.next(state, new MovePiece(4,3, 4, 4));
        fm.next(state, new MovePiece(3,6, 3, 4));

        //En Passant
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().anyMatch(e -> e.equals(new EnPassant(4, 4, 3)))); // Check en passant is in available actions
        fm.next(state, new EnPassant(4, 4, 3)); // Execute en passant
        //Check the pawn positions
        ChessPiece pawn = state.getPiece(3, 5);
        assertEquals(pawn.getChessPieceType(), ChessPiece.ChessPieceType.PAWN);
        assertEquals(pawn.getOwnerId(), 0);
        assertEquals(state.getPiece(4, 4), null); // Check the pawn at (4, 4) is removed
    }
    @Test
    public void drawByRepetitionTest() {
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //Setup the board for draw by repetition 
        fm.next(state, new MovePiece(1,0, 2, 2)); // Move knight
        fm.next(state, new MovePiece(1,7, 2, 5)); // Move knight
        fm.next(state, new MovePiece(2,2, 1, 0)); // Move knight back
        fm.next(state, new MovePiece(2,5, 1, 7)); // Move knight back
        fm.next(state, new MovePiece(1,0, 2, 2)); // Move knight
        fm.next(state, new MovePiece(1,7, 2, 5)); // Move knight
        fm.next(state, new MovePiece(2,2, 1, 0)); // Move knight back
        fm.next(state, new MovePiece(2,5, 1, 7)); // Move knight back

        //Check game is over
        assertTrue(state.isGameOver());
    }

    @Test
    public void FoolsMateTest() {
        ChessGameState state = (ChessGameState) game.getGameState().copy();
        //Setup the board for Fools Mate
        //Pawns
        fm.next(state, new MovePiece(5,1, 5, 2)); // Move pawn
        fm.next(state, new MovePiece(4,6, 4, 5)); // Move pawn
        fm.next(state, new MovePiece(6,1, 6, 3)); // Move pawn
        fm.next(state, new MovePiece(3,7, 7, 3)); // Move pawn
        //Check game is over
        assertTrue(state.isGameOver());
    }
}

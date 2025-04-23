package games.chess;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import games.chess.ChessGameState;
import games.chess.actions.MovePiece;
import games.chess.components.ChessPiece;

import java.util.ArrayList;
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
            chessState.setPiece(i, 1, new ChessPiece(ChessPiece.ChessPieceType.PAWN, ChessPiece.ChessPieceColor.WHITE, false));
            chessState.setPiece(i, 6, new ChessPiece(ChessPiece.ChessPieceType.PAWN, ChessPiece.ChessPieceColor.BLACK, false));
        }
        chessState.setPiece(0, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(1, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(2, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(3, 0, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(4, 0, new ChessPiece(ChessPiece.ChessPieceType.KING, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(5, 0, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(6, 0, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(7, 0, new ChessPiece(ChessPiece.ChessPieceType.ROOK, ChessPiece.ChessPieceColor.WHITE, false));
        chessState.setPiece(0, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(1, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(2, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(3, 7, new ChessPiece(ChessPiece.ChessPieceType.QUEEN, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(4, 7, new ChessPiece(ChessPiece.ChessPieceType.KING, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(5, 7, new ChessPiece(ChessPiece.ChessPieceType.BISHOP, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(6, 7, new ChessPiece(ChessPiece.ChessPieceType.KNIGHT, ChessPiece.ChessPieceColor.BLACK, false));
        chessState.setPiece(7, 7, new ChessPiece(ChessPiece.ChessPieceType.ROOK, ChessPiece.ChessPieceColor.BLACK, false));

        //Set the rest of the board to null (empty squares)
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                chessState.setPiece(j, i, new ChessPiece(ChessPiece.ChessPieceType.NONE, ChessPiece.ChessPieceColor.NONE, false));
            }
        }



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
        actions.add(new MovePiece(0,1,0,2)); // TODO: remove this line, just an example action
        // TODO: create action classes for the current player in the given game state and add them to the list. Below just an example that does nothing, remove.
        




        return actions;
    }
}

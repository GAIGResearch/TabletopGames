package games.chess;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import games.GameType;
import games.chess.components.ChessPiece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.Objects;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.s;

/**
 * <p>The game state encapsulates all game information. It is a data-only class, with game functionality present
 * in the Forward Model or actions modifying the state of the game.</p>
 * <p>Most variables held here should be {@link Component} subclasses as much as possible.</p>
 * <p>No initialisation or game logic should be included here (not in the constructor either). This is all handled externally.</p>
 * <p>Computation may be included in functions here for ease of access, but only if this is querying the game state information.
 * Functions on the game state should never <b>change</b> the state of the game.</p>
 */
public class ChessGameState extends AbstractGameState {

    ChessPiece[][] pieces = new ChessPiece[8][8];
    GridBoard board = new GridBoard(pieces);
    //Mapping from piece to its position on the board
    HashMap<ChessPiece, int[]> pieceToPosition = new HashMap<>();
    //List of white pieces
    List<ChessPiece> whitePieces = new ArrayList<>();
    //List of black pieces
    List<ChessPiece> blackPieces = new ArrayList<>();

    //Number of moves without a pawn move or capture
    int halfMoveClock = 0;
    //Number of repetitions of the same position
    int repetitionCount = 0;

    
    

    /**
     * @param gameParameters - game parameters.
     * @param nPlayers       - number of players in the game
     */
    public ChessGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    /**
     * @return the enum value corresponding to this game, declared in {@link GameType}.
     */
    @Override
    protected GameType _getGameType() {
        return GameType.Chess;
    }

    /**
     * Returns all Components used in the game and referred to by componentId from actions or rules.
     * This method is called after initialising the game state, so all components will be initialised already.
     *
     * @return - List of Components in the game.
     */
    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.add(board);
        return components;
    }

    /**
     * <p>Create a deep copy of the game state containing only those components the given player can observe.</p>
     * <p>If the playerID is NOT -1 and If any components are not visible to the given player (e.g. cards in the hands
     * of other players or a face-down deck), then these components should instead be randomized (in the previous examples,
     * the cards in other players' hands would be combined with the face-down deck, shuffled together, and then new cards drawn
     * for the other players). This process is also called 'redeterminisation'.</p>
     * <p>There are some utilities to assist with this in utilities.DeterminisationUtilities. One firm is guideline is
     * that the standard random number generator from getRnd() should not be used in this method. A separate Random is provided
     * for this purpose - redeterminisationRnd.
     *  This is to avoid this RNG stream being distorted by the number of player actions taken (where those actions are not themselves inherently random)</p>
     * <p>If the playerID passed is -1, then full observability is assumed and the state should be faithfully deep-copied.</p>
     *
     * <p>Make sure the return type matches the class type, and is not AbstractGameState.</p>
     *
     *
     * @param playerId - player observing this game state.
     */
    @Override
    protected ChessGameState _copy(int playerId) {
        ChessGameState copy = new ChessGameState(getGameParameters(), getNPlayers());
        copy.whitePieces = new ArrayList<>(whitePieces);
        copy.blackPieces = new ArrayList<>(blackPieces);
        copy.halfMoveClock = halfMoveClock;
        copy.repetitionCount = repetitionCount;
        copy.pieces = new ChessPiece[8][8]; //Is this really the best way to do this? Ew...
        for (int i = 0; i < pieces.length; i++) {
            for (int j = 0; j < pieces[i].length; j++) {
                if (pieces[i][j] != null) {
                    copy.pieces[i][j] = pieces[i][j].copy();
                } else {
                    copy.pieces[i][j] = null;
                }
            }
        }
        copy.board = new GridBoard(copy.pieces);
        copy.pieceToPosition = new HashMap<>(pieceToPosition.size());
        for (ChessPiece piece : pieceToPosition.keySet()) {
            copy.pieceToPosition.put(piece.copy(), pieceToPosition.get(piece).clone());
        }
        copy.pieceToPosition = pieceToPosition;

        return copy;
    }

    /**
     * @param playerId - player observing the state.
     * @return a score for the given player approximating how well they are doing (e.g. how close they are to winning
     * the game); a value between 0 and 1 is preferred, where 0 means the game was lost, and 1 means the game was won.
     */
    @Override
    protected double _getHeuristicScore(int playerId) {
        if (isNotTerminal()) {
            // TODO calculate an approximate value
            return 0;
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return getPlayerResults()[playerId].value;
        }
    }

    /**
     * @param playerId - player observing the state.
     * @return the true score for the player, according to the game rules. May be 0 if there is no score in the game.
     */
    @Override
    public double getGameScore(int playerId) {
        // TODO: What is this player's score (if any)?
        return 0;
    }

    @Override
    protected boolean _equals(Object o) {
        // TODO: compare all variables in the state
        return o instanceof ChessGameState that &&
                super.equals(o) &&
                this.halfMoveClock == that.halfMoveClock &&
                this.repetitionCount == that.repetitionCount &&
                this.board.equals(that.board);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), halfMoveClock, repetitionCount, board);
    }

    public GridBoard getBoard() {
        return board;
    }

    public void setPiece(int x, int y, ChessPiece piece) {
        pieces[x][y] = piece;
        if (piece != null) {
            pieceToPosition.put(piece, new int[]{x, y});
        }
    }
    public ChessPiece getPiece(int x, int y) {
        return pieces[x][y];
    }
    public List<ChessPiece> getPieces(int playerId) {
        if (playerId == 0) {
            return whitePieces;
        } else if (playerId == 1) {
            return blackPieces;
        } else {
            throw new IllegalArgumentException("Invalid player ID: " + playerId);
        }
    }
    public List<ChessPiece> getWhitePieces() {
        return whitePieces;
    }
    public List<ChessPiece> getBlackPieces() {
        return blackPieces;
    }
    public HashMap<ChessPiece, int[]> getPieceToPosition() {
        return pieceToPosition;
    }
    public void updatePieceLists()
    {
        whitePieces.clear();
        blackPieces.clear();
        pieceToPosition.clear();
        for (int i = 0; i < pieces.length; i++) {
            for (int j = 0; j < pieces[i].length; j++) {
                ChessPiece piece = pieces[i][j];
                if (piece != null) {
                    if (piece.getOwnerId() == 0) {
                        whitePieces.add(piece);
                    } else if (piece.getOwnerId() == 1) {
                        blackPieces.add(piece);
                    }
                    pieceToPosition.put(piece, new int[]{i, j});
                }
            }
        }
        return;
    }



    // TODO: Consider the methods below for possible implementation
    // TODO: These all have default implementations in AbstractGameState, so are not required to be implemented here.
    // TODO: If the game has 'teams' that win/lose together, then implement the next two nethods.
    /**
     * Returns the number of teams in the game. The default is to have one team per player.
     * If the game does not have 'teams' that win/lose together, then ignore these two methods.
     */
   // public int getNTeams();
    /**
     * Returns the team number the specified player is on.
     */
    //public int getTeam(int player);

    // TODO: If your game has multiple special tiebreak options, then implement the next two methods.
    // TODO: The default is to tie-break on the game score (if this is the case, ignore these)
    // public double getTiebreak(int playerId, int tier);
    // public int getTiebreakLevels();


    // TODO: If your game does not have a score of any type, and is an 'insta-win' type game which ends
    // TODO: as soon as a player achieves a winning condition, and has some bespoke method for determining 1st, 2nd, 3rd etc.
    // TODO: Then you *may* want to implement:.
    //public int getOrdinalPosition(int playerId);

    // TODO: Review the methods below...these are all supported by the default implementation in AbstractGameState
    // TODO: So you do not (and generally should not) implement your own versions - take advantage of the framework!
    // public Random getRnd() returns a Random number generator for the game. This will be derived from the seed
    // in game parameters, and will be updated correctly on a reset

    // Ths following provide access to the id of the current player; the first player in the Round (if that is relevant to a game)
    // and the current Turn and Round numbers.
    // public int getCurrentPlayer()
    // public int getFirstPlayer()
    // public int getRoundCounter()
    // public int getTurnCounter()
    // also make sure you check out the standard endPlayerTurn() and endRound() methods in StandardForwardModel

    // This method can be used to log a game event (e.g. for something game-specific that you want to include in the metrics)
    // public void logEvent(IGameEvent...)
}

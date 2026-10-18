package games.stratego;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.BoardNode;
import core.components.Component;
import core.components.GridBoard;
import games.GameType;
import games.stratego.components.Piece;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StrategoGameState extends AbstractGameState{
    GridBoard gridBoard;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public StrategoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Stratego;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{ add(gridBoard);}};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        StrategoGameState s = new StrategoGameState(gameParameters.copy(), 2);
        s.gridBoard = gridBoard.copy();

        if (playerId != -1 && getCoreGameParameters().partialObservable){
            // the opponent's unknown pieces, row by row, are dealt their types again at random
            Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(playerId);
            List<Piece> hidden = new ArrayList<>();
            for (Piece p : getPieces()) {
                if (p.getPieceAlliance() != playerAlliance && !p.isPieceKnown()) {
                    hidden.add(p);
                }
            }
            List<Piece.PieceType> pieceTypesHidden = new ArrayList<>();
            for (Piece p : hidden) {
                pieceTypesHidden.add(p.getPieceType());
            }
            for (Piece p : hidden) {
                Piece.PieceType hiddenPieceType = pieceTypesHidden.remove(redeterminisationRnd.nextInt(pieceTypesHidden.size()));
                s.setPiece(p.getPiecePosition(), p.partialCopy(hiddenPieceType));
            }
        }
        return s;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new StrategoHeuristic().evaluateState(this, playerId);
    }

    /**
     * GameScore has no meaning in Stratego. This will always return zero for any non-terminal game state.
     */
    @Override
    public double getGameScore(int playerId) {
        return playerResults[playerId].value;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof StrategoGameState other) {
            return gridBoard.equals(other.gridBoard);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return gridBoard.hashCode();
    }

    public GridBoard getGridBoard() {
        return gridBoard;
    }

    /**
     * The pieces on the board, row by row.
     */
    public Collection<Piece> getPieces() {
        List<Piece> retValue = new ArrayList<>();
        for (BoardNode node : gridBoard.asMap().values())
            retValue.add((Piece) node);
        return retValue;
    }

    /**
     * The piece at the position, or null if there is none.
     */
    public Piece getPiece(Vector2D position) {
        return (Piece) gridBoard.get(position);
    }

    /**
     * The piece on the board with the componentID, or null if it has been taken.
     */
    public Piece getPiece(int componentID) {
        // not getComponentById, which may hold an earlier version of the piece
        for (Piece p : getPieces())
            if (p.getComponentID() == componentID)
                return p;
        return null;
    }

    /**
     * Puts the piece at the position, or empties it if the piece is null.
     */
    public void setPiece(Vector2D position, Piece piece) {
        gridBoard.set(position, piece);
    }

    @Override
    protected List<Integer> _getUnknownComponentsIds(int playerId) {
        ArrayList<Integer> pieceList = new ArrayList<>();

        for (Piece piece : getPieces()){
            if (playerId != -1){
                Piece.Alliance playerAlliance = StrategoConstants.playerMapping.get(playerId);
                if (playerAlliance != piece.getPieceAlliance() && !piece.isPieceKnown()) {
                    pieceList.add(piece.getComponentID());
                }
            }
        }
        return pieceList;
    }

    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }


}

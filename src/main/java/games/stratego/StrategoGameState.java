package games.stratego;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.turnorders.AlternatingTurnOrder;
import games.GameType;
import games.stratego.components.Piece;

import java.util.ArrayList;
import java.util.List;

public class StrategoGameState extends AbstractGameState {
    GridBoard<Piece> gridBoard;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     */
    public StrategoGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers), GameType.Stratego);
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{ add(gridBoard);}};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        StrategoGameState s = new StrategoGameState(gameParameters.copy(), getNPlayers());
        s.gridBoard = gridBoard.emptyCopy();

        for (Piece piece : gridBoard.getComponents()){
            if (piece != null){
                if (playerId != -1){
                    String playerAlliance = StrategoConstants.playerMapping.get(playerId);
                    if (playerAlliance.equals(piece.getPieceAlliance().toString()) || piece.getPieceKnownFlag()) {
                        s.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
                    } else{
                        Piece.PieceType hiddenPieceType = getHiddenPieceType(playerId);
                        s.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.partialCopy(hiddenPieceType));
                    }
                } else{
                    s.gridBoard.setElement(piece.getPiecePosition()[0], piece.getPiecePosition()[1], piece.copy());
                }
            }
        }
        return s;
    }

    protected Piece.PieceType getHiddenPieceType(int ownerID){
        ArrayList<Piece.PieceType> hiddenPieces = new ArrayList<>();
        for (Piece piece : this.gridBoard.getComponents()){
            if (piece != null){
                if (piece.getOwnerId() != ownerID){
                    hiddenPieces.add(piece.getPieceType());
                }
            }
        }
        return hiddenPieces.get((int) (Math.random()*hiddenPieces.size()));
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return new StrategoHeuristic().evaluateState(this, playerId);
    }

    @Override
    public double getGameScore(int playerId) {
        return 0;
    }

    @Override
    protected void _reset() {
        gridBoard = null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }


    public GridBoard<Piece> getGridBoard() {
        return gridBoard;
    }

    @Override
    protected List<Integer> _getUnknownComponentsIds(int playerId) {
        ArrayList<Integer> pieceList = new ArrayList<>();

        for (Piece piece : gridBoard.getComponents()){
            if (piece != null){
                if (playerId != -1){
                    String playerAlliance = StrategoConstants.playerMapping.get(playerId);
                    if (!playerAlliance.equals(piece.getPieceAlliance().toString())) {
                        pieceList.add(piece.getComponentID());
                    }
                }
            }
        }
        return pieceList;
    }

    public void printToConsole() {
        System.out.println(gridBoard.toString());
    }
}

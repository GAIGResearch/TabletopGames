package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.catan.CatanConstants.HEX_SIDES;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class DeepPlaceSettlementThenRoad extends PlaceSettlementWithRoad implements IExtendedSequence {
    boolean executed;

    public DeepPlaceSettlementThenRoad(int x, int y, int vertex, int player) {
        super(x, y, vertex, -1, player);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        BuildSettlement buildSettlement = new BuildSettlement(x,y,vertex,player,true);
        buildSettlement.execute(gs);
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        List<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;
        CatanTile[][] board = gs.getBoard();
        CatanTile tile = board[x][y];
        int[][] coords = tile.getNeighboursOnVertex(vertex);
        actions.add(new BuildRoad(x, y, vertex, player, true));
        for (int[] neighbour: coords) {
            actions.add(new BuildRoad(neighbour[0], neighbour[1], (vertex+2)%HEX_SIDES, player, true));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        executed = true;
        CatanParameters cp = (CatanParameters) state.getGameParameters();

        // players get the resources in the second round after the settlements they placed
        if (state.getRoundCounter() == 1){
            CatanGameState cgs = ((CatanGameState)state);
            CatanTile[][] board = cgs.getBoard();
            // in the second round players get the resources from the tiles around the settlement
            ArrayList<CatanTile> tiles = new ArrayList<>();
            CatanTile tile = cgs.getBoard()[x][y];
            // next step is to find the tiles around the settlement
            int[][] neighbourCoords =  tile.getNeighboursOnVertex(((BuildRoad)action).edge);
            tiles.add(tile);
            tiles.add(board[neighbourCoords[0][0]][neighbourCoords[0][1]]);
            tiles.add(board[neighbourCoords[1][0]][neighbourCoords[1][1]]);

            for (CatanTile t: tiles){
                CatanParameters.Resource res = cp.productMapping.get(t.getTileType());
                if (res!=null){
                    cgs.getPlayerResources(player).get(res).increment();
                    cgs.getResourcePool().get(res).decrement();
                    if (state.getCoreGameParameters().verbose) {
                        System.out.println("At setup Player " + player + " got " + res);
                    }
                }
            }
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public DeepPlaceSettlementThenRoad copy() {
        DeepPlaceSettlementThenRoad copy = new DeepPlaceSettlementThenRoad(x, y, vertex, player);
        copy.executed = executed;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepPlaceSettlementThenRoad)) return false;
        if (!super.equals(o)) return false;
        DeepPlaceSettlementThenRoad that = (DeepPlaceSettlementThenRoad) o;
        return executed == that.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), executed);
    }

    @Override
    public String toString() {
        return String.format("DeepPlaceSettlementThenRoad: x=%d y=%d player=%d",x,y,player);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

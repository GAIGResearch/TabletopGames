package games.catan.actions.setup;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.grid.HexCoord;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.actions.build.BuildRoad;
import games.catan.actions.build.BuildSettlement;
import games.catan.components.CatanTile;

import java.util.ArrayList;
import java.util.Objects;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class PlaceSettlementWithRoad extends AbstractAction {
    public final HexCoord hex;
    public final int vertex, edge;
    public final int player;

    public PlaceSettlementWithRoad(HexCoord hex, int vertex, int edge, int player) {
        this.hex = hex;
        this.vertex = vertex;
        this.edge = edge;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = ((CatanGameState)gs);
        BuildSettlement buildSettlement  = new BuildSettlement(hex, vertex, player, true);
        int edgeID = cgs.getTile(hex).getEdgeID(edge);
        BuildRoad buildRoad = new BuildRoad(hex, edge, player, true, edgeID);
        CatanParameters cp = (CatanParameters) gs.getGameParameters();

        if (buildSettlement.execute(gs) && buildRoad.execute(gs)){
            // players get the resources in the second round after the settlements they placed
            if (gs.getRoundCounter() == 1){
                // in the second round players get the resources from the tiles around the settlement
                ArrayList<CatanTile> tiles = new ArrayList<CatanTile>();
                CatanTile tile = cgs.getTile(hex);
                tiles.add(tile);
                for (HexCoord neighbour : tile.getNeighboursOnVertex(vertex))
                    tiles.add(cgs.getTile(neighbour));

                for (CatanTile t: tiles){
                    CatanParameters.Resource res = cp.productMapping.get(t.getTileType());
                    if (res!=null){
                        cgs.getPlayerResources(player).get(res).increment();
                        cgs.getResourcePool().get(res).decrement();
                        if (gs.getCoreGameParameters().verbose) {
                            System.out.println("Setup: p" + player + " got " + res + " (last settle)");
                        }
                    }
                }
            }
            return true;
        } else {
            throw new AssertionError("Could not execute chosen settlement and road build");
        }
    }

    @Override
    public PlaceSettlementWithRoad copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceSettlementWithRoad)) return false;
        PlaceSettlementWithRoad that = (PlaceSettlementWithRoad) o;
        return hex.equals(that.hex) && vertex == that.vertex && edge == that.edge && player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hex, vertex, edge, player);
    }

    @Override
    public String toString() {
        return String.format("p%d settles (hex=%s vertex=%d) and builds road (edge=%d)", player, hex, vertex, edge);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

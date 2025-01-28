package games.catan.actions.setup;

import core.AbstractGameState;
import core.actions.AbstractAction;
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
    public final int x;
    public final int y;
    public final int vertex, edge;
    public final int player;

    public PlaceSettlementWithRoad(int x, int y, int vertex, int edge, int player) {
        this.x = x;
        this.y = y;
        this.vertex = vertex;
        this.edge = edge;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = ((CatanGameState)gs);
        BuildSettlement buildSettlement  = new BuildSettlement(x,y,vertex,player,true);
        int edgeID = cgs.getBoard()[x][y].getEdgeIDs()[edge];
        BuildRoad buildRoad = new BuildRoad(x,y,edge,player,true, edgeID);
        CatanParameters cp = (CatanParameters) gs.getGameParameters();

        if (buildSettlement.execute(gs) && buildRoad.execute(gs)){
            // players get the resources in the second round after the settlements they placed
            if (gs.getRoundCounter() == 1){
                CatanTile[][] board = cgs.getBoard();
                // in the second round players get the resources from the tiles around the settlement
                ArrayList<CatanTile> tiles = new ArrayList<CatanTile>();
                CatanTile tile = cgs.getBoard()[buildSettlement.x][buildSettlement.y];
                // next step is to find the tiles around the settlement
                int[][] neighbourCoords =  tile.getNeighboursOnVertex(buildSettlement.vertex);
                tiles.add(tile);
                tiles.add(board[neighbourCoords[0][0]][neighbourCoords[0][1]]);
                tiles.add(board[neighbourCoords[1][0]][neighbourCoords[1][1]]);

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
        return x == that.x && y == that.y && vertex == that.vertex && edge == that.edge && player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, vertex, edge, player);
    }

    @Override
    public String toString() {
        return String.format("p%d settles (x=%d y=%d vertex=%d) and builds road (edge=%d)",player,x,y,vertex,edge);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

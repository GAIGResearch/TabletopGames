package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;

import java.util.ArrayList;
import java.util.Objects;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class PlaceSettlementWithRoad extends AbstractAction {
    public final int x;
    public final int y;
    public final int i;
    public final int player;

    public PlaceSettlementWithRoad(int x, int y, int i, int player) {
        this.x = x;
        this.y = y;
        this.i = i;
        this.player = player;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        BuildSettlement buildSettlement  = new BuildSettlement(x,y,i,player,true);
        BuildRoad buildRoad = new BuildRoad(x,y,i,player,true);
        CatanParameters cp = (CatanParameters) gs.getGameParameters();

        if (buildSettlement.execute(gs) && buildRoad.execute(gs)){
            // players get the resources in the second round after the settlements they placed
            if (gs.getRoundCounter() == 1){
                CatanGameState cgs = ((CatanGameState)gs);
                CatanTile[][] board = cgs.getBoard();
                // in the second round players get the resources from the tiles around the settlement
                ArrayList<CatanTile> tiles = new ArrayList<CatanTile>();
                CatanTile tile = cgs.getBoard()[buildSettlement.x][buildSettlement.y];
                // next step is to find the tiles around the settlement
                int[][] neighbourCoords =  CatanTile.getNeighboursOnVertex(tile, buildSettlement.vertex);
                tiles.add(tile);
                tiles.add(board[neighbourCoords[0][0]][neighbourCoords[0][1]]);
                tiles.add(board[neighbourCoords[1][0]][neighbourCoords[1][1]]);

                for (CatanTile t: tiles){
                    CatanParameters.Resource res = cp.productMapping.get(t.getTileType());
                    if (res!=null){
                        cgs.getPlayerResources(player).get(res).increment();
                        cgs.getResourcePool().get(res).decrement();
                        if (gs.getCoreGameParameters().verbose) {
                            System.out.println("At setup Player " + player + " got " + res);
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
        return x == that.x && y == that.y && i == that.i && player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, i, player);
    }

    @Override
    public String toString() {
        return String.format("PlaceSettlementWithRoad: x=%d y=%d i=%d player=%d",x,y,i,player);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Edge;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.Objects;

public class BuildRoad extends AbstractAction {
    public final int x;
    public final int y;
    public final int edge;
    public final int playerID;
    public final boolean free;

    public BuildRoad(int x, int y, int edge, int playerID, boolean free) {
        this.x = x;
        this.y = y;
        this.edge = edge;
        this.playerID = playerID;
        this.free = free;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        CatanTile[][] board = cgs.getBoard();
        if (board[x][y].getRoads()[edge].getOwner() == -1) {
            if (((Counter) cgs.getComponentActingPlayer(CatanConstants.roadCounterHash)).isMaximum()) {
                throw new AssertionError("No more roads to build for player " + gs.getCurrentPlayer());
            }
            ((Counter) cgs.getComponentActingPlayer(CatanConstants.roadCounterHash)).increment(1);
            // only take resources after set up and not with road building card
            if (!free) {
                if (!CatanGameState.spendResources(cgs, CatanParameters.costMapping.get("road"))) {
                    throw new AssertionError("Player " + gs.getCurrentPlayer() + " cannot afford this road");
                }
            }
            board[this.x][this.y].addRoad(edge, playerID);

            // find the road in the graph and set the owner to playerID
            Graph<Settlement, Road> graph = cgs.getGraph();
            Settlement settl1 = board[x][y].getSettlements()[edge];
            Settlement settl2 = board[x][y].getSettlements()[(edge+1)%6];
            // update the placed road in the graph in both directions not just on the board
            for (Edge<Settlement, Road> e: graph.getEdges(settl1)){
                if (e.getDest().equals(settl2)){
                    e.getValue().setOwner(playerID);
                }
            }
            for (Edge<Settlement, Road> e: graph.getEdges(settl2)){
                if (e.getDest().equals(settl1)){
                    e.getValue().setOwner(playerID);
                }
            }
            return true;
        } else {
            throw new AssertionError("Road already owned: " + this.toString());
        }
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BuildRoad) {
            BuildRoad otherAction = (BuildRoad) other;
            return x == otherAction.x && y == otherAction.y && edge == otherAction.edge && playerID == otherAction.playerID && free == otherAction.free;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, edge, playerID, free);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Buildroad in x=" + x + " y=" + y + " edge=" + edge + " free = " + free;
    }



    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getEdge() {
        return edge;
    }

    public int getPlayerID() {
        return playerID;
    }

}

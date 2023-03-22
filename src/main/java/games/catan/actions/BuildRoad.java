package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;
import games.catan.components.Edge;
import games.catan.components.Graph;
import games.catan.components.Building;

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
        CatanParameters cp = (CatanParameters) gs.getGameParameters();
        CatanTile[][] board = cgs.getBoard();
        if (board[x][y].getRoads()[edge].getOwnerId() == -1) {
            Counter roadTokens = cgs.getPlayerTokens().get(playerID).get(CatanParameters.ActionType.Road);
            if (roadTokens.isMaximum()) {
                throw new AssertionError("No more roads to build for player " + gs.getCurrentPlayer());
            }
            roadTokens.increment();
            // only take resources after set up and not with road building card
            if (!free) {
                if (!cgs.spendResourcesIfPossible(cp.costMapping.get(CatanParameters.ActionType.Road), playerID)) {
                    throw new AssertionError("Player " + playerID + " cannot afford this road");
                }
            }
            board[x][y].addRoad(edge, playerID);

            // find the road in the graph and set the owner to playerID
            Graph graph = cgs.getGraph();
            Building settl1 = board[x][y].getSettlements()[edge];
            Building settl2 = board[x][y].getSettlements()[(edge+1)%6];
            // update the placed road in the graph in both directions not just on the board
            for (Edge e: graph.getEdges(settl1)){
                if (e.getDest().equals(settl2)){
                    e.getRoad().setOwnerId(playerID);
                }
            }
            for (Edge e: graph.getEdges(settl2)){
                if (e.getDest().equals(settl1)){
                    e.getRoad().setOwnerId(playerID);
                }
            }

            // Check longest road
            int new_length = cgs.getRoadDistance(x, y, edge);
            cgs.getRoadLengths()[playerID] = new_length;
            if (new_length > cgs.getLongestRoadLength()) {
                cgs.setLongestRoadLength(new_length);
                // add points for longest road and set the new road in gamestate
                if (cgs.getLongestRoadOwner() >= 0) {
                    // in this case the longest road was already claimed
                    cgs.addScore(cgs.getLongestRoadOwner(), -cp.longest_road_value);
                }
                cgs.addScore(playerID, cp.longest_road_value);
                cgs.setLongestRoadOwner(playerID);
                if (gs.getCoreGameParameters().verbose) {
                    System.out.println("Player " + playerID + " has the longest road with length " + new_length);
                }
            }
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Calculated road length: " + new_length);
            }
            return true;
        } else {
            throw new AssertionError("Road already owned: " + this);
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
        return "Build Road at x=" + x + " y=" + y + " edge=" + edge + " free = " + free;
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

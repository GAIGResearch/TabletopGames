package games.catan.actions.build;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;
import core.components.Edge;

import java.util.Objects;

import static games.catan.stats.CatanMetrics.CatanEvent.LongestRoadSteal;

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
        Edge edgeObj = cgs.getRoad(board[x][y], edge, edge);
        if (edgeObj.getOwnerId() == -1) {
            Counter roadTokens = cgs.getPlayerTokens().get(playerID).get(BuyAction.BuyType.Road);
            if (roadTokens.isMaximum()) {
                return false;  // TODO investigate why this is reached
//                throw new AssertionError("No more roads to build for player " + gs.getCurrentPlayer());
            }

            // only take resources after set up and not with road building card
            if (!free) {
                if (!cgs.spendResourcesIfPossible(cp.costMapping.get(BuyAction.BuyType.Road), playerID)) {
                    throw new AssertionError("Player " + playerID + " cannot afford this road");
                }
            }
            roadTokens.increment();
            edgeObj.setOwnerId(playerID);

            // Check longest road
            int new_length = cgs.getRoadDistance(x, y, edge);
            cgs.getRoadLengths()[playerID] = new_length;
            if (new_length > cgs.getLongestRoadLength() && new_length > cp.min_longest_road) {
                cgs.setLongestRoadLength(new_length);
                // add points for longest road and set the new road in gamestate
                if (cgs.getLongestRoadOwner() >= 0) {
                    // in this case the longest road was already claimed
                    cgs.logEvent(LongestRoadSteal, String.valueOf(playerID));
                    cgs.addScore(cgs.getLongestRoadOwner(), -cp.longest_road_value);
                }
                cgs.addScore(playerID, cp.longest_road_value);
                cgs.setLongestRoadOwner(playerID);
                if (gs.getCoreGameParameters().verbose) {
                    System.out.println("Player " + playerID + " has the longest road with length " + new_length);
                }
            }
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Calculated road length p" + playerID + ": " + new_length);
            }
            return true;
        } else {
            throw new AssertionError("Road already owned: " + this);
        }
    }

    @Override
    public BuildRoad copy() {
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
        return "p" + playerID + " Buy:Road (x=" + x + " y=" + y + " edge=" + edge + ")";
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

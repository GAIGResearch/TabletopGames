package games.catan.actions.robber;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.Objects;

import static core.CoreConstants.DefaultGamePhase.Main;

/**
 * Player may steal a resource from a player when moving a robber or playing a knight card
 */
public class StealResource extends AbstractAction {
    public final int playerID;
    public final int targetPlayerID;

    public StealResource(int playerID, int targetPlayerID){
        this.playerID = playerID;
        this.targetPlayerID = targetPlayerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        int nResTarget = cgs.getNResourcesInHand(targetPlayerID);
        if (nResTarget == 0){
            cgs.setGamePhase(Main);
            return false;
        }
        int cardIndex = cgs.getRnd().nextInt(nResTarget);
        CatanParameters.Resource resource = cgs.pickResourceFromHand(targetPlayerID, cardIndex);
        cgs.getPlayerResources(playerID).get(resource).increment();
        cgs.getPlayerResources(targetPlayerID).get(resource).decrement();
        cgs.setGamePhase(Main);
        return true;
    }

    @Override
    public StealResource copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StealResource)) return false;
        StealResource that = (StealResource) o;
        return playerID == that.playerID && targetPlayerID == that.targetPlayerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, targetPlayerID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " steals a resource from p" + targetPlayerID;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

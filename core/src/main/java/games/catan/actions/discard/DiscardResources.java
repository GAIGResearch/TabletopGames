package games.catan.actions.discard;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.Arrays;
import java.util.Objects;

/* Takes in a list of resources to be discarded from the player's hand*/
public class DiscardResources extends AbstractAction {
    public final CatanParameters.Resource[] resourcesToDiscard;
    public final int playerID;

    public DiscardResources(CatanParameters.Resource[] resourcesToDiscard, int playerID){
        this.resourcesToDiscard = resourcesToDiscard;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        for (CatanParameters.Resource resource: resourcesToDiscard){
            cgs.getPlayerResources(playerID).get(resource).decrement();
            cgs.getResourcePool().get(resource).increment();
        }
        return true;
    }

    @Override
    public DiscardResources copy() {
        return new DiscardResources(resourcesToDiscard.clone(), playerID);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof DiscardResources){
            DiscardResources otherAction = (DiscardResources)other;
            return Arrays.equals(otherAction.resourcesToDiscard, resourcesToDiscard) && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int returnVal = Objects.hash(playerID);
        return returnVal + 13 * Arrays.hashCode(resourcesToDiscard);

    }

    @Override
    public String toString() {
        return "p" + playerID + " discards: " + Arrays.toString(resourcesToDiscard);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

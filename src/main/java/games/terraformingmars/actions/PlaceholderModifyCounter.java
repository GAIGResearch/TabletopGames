package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.ModifyCounter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.Objects;

public class PlaceholderModifyCounter extends TMModifyCounter {
    TMTypes.Resource resource;
    boolean production;

    public PlaceholderModifyCounter(int change, TMTypes.Resource resource, boolean production, boolean free) {
        super(-1, change, free);
        this.resource = resource;
        this.production = production;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Find and set ID of counter for current player
        TMGameState ggs = (TMGameState)gs;
        int activePlayer = gs.getCurrentPlayer();
        if (production) {
            counterID = ggs.getPlayerProduction()[activePlayer].get(resource).getComponentID();
        } else {
            counterID = ggs.getPlayerResources()[activePlayer].get(resource).getComponentID();
        }
        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceholderModifyCounter)) return false;
        if (!super.equals(o)) return false;
        PlaceholderModifyCounter that = (PlaceholderModifyCounter) o;
        return production == that.production &&
                resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, production);
    }
}

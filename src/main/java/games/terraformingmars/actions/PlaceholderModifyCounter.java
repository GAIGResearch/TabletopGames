package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.ModifyCounter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.Objects;

public class PlaceholderModifyCounter extends TMModifyCounter {
    public TMTypes.Resource resource;
    public boolean production;

    public PlaceholderModifyCounter(int player, int change, TMTypes.Resource resource, boolean production, boolean free) {
        super(player,-1, change, free);
        this.resource = resource;
        this.production = production;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        TMGameState ggs = (TMGameState)gs;
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        if (production) {
            counterID = ggs.getPlayerProduction()[player].get(resource).getComponentID();
        } else {
            counterID = ggs.getPlayerResources()[player].get(resource).getComponentID();
        }
        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceholderModifyCounter)) return false;
        if (!super.equals(o)) return false;
        PlaceholderModifyCounter that = (PlaceholderModifyCounter) o;
        return production == that.production && player == that.player && resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, production, player);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Modify player " + resource + (production? " production " : "") + " by " + change;
    }

    @Override
    public String toString() {
        return "Modify player " + resource + (production? " production " : "") + " by " + change;
    }
}

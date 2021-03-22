package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ModifyCounter;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlaceholderModifyCounter extends TMModifyCounter implements IExtendedSequence {
    public TMTypes.Resource resource;
    public boolean production;
    public int targetPlayer;

    public PlaceholderModifyCounter(int player, int change, TMTypes.Resource resource, boolean production, boolean free) {
        super(player,-1, change, free);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = player;
    }

    public PlaceholderModifyCounter(int player, int targetPlayer, Integer change, TMTypes.Resource resource, boolean production, boolean free) {
        super(player,-1, change, free);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = targetPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        TMGameState ggs = (TMGameState)gs;
        if (targetPlayer == -2) {
            // Player chooses who this applies to
            gs.setActionInProgress(this);
            return true;
        } else {
            if (targetPlayer == -1 && player != -1) {
                targetPlayer = player;
            } else {
                // current player
                targetPlayer = gs.getCurrentPlayer();
                player = targetPlayer;
            }
            if (production) {
                counterID = ggs.getPlayerProduction()[targetPlayer].get(resource).getComponentID();
            } else {
                counterID = ggs.getPlayerResources()[targetPlayer].get(resource).getComponentID();
            }
            return super.execute(gs);
        }
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

    @Override
    public PlaceholderModifyCounter copy() {
        return new PlaceholderModifyCounter(player, targetPlayer, change, resource, production, free);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++) {
            // TODO: check if can be done for player
            actions.add(new PlaceholderModifyCounter(player, targetPlayer, change, resource, production, true));
        }
        if (actions.size() == 0) {
            actions.add(new TMAction(player));  // TODO: this should not happen
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        PlaceholderModifyCounter a = (PlaceholderModifyCounter) action;
        targetPlayer = a.targetPlayer;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return targetPlayer != -1;
    }
}

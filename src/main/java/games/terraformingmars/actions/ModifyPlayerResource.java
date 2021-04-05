package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.ResourceRequirement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class ModifyPlayerResource extends TMModifyCounter implements IExtendedSequence {
    public TMTypes.Resource resource;
    public boolean production;
    public int targetPlayer;
    public HashSet<Integer> targetPlayerOptions;

    public TMTypes.Resource counterResource;  // if not null, player chooses how much of resource to decrease in order to increase counterResource by same amount
    public boolean counterResourceProduction;
    public TMTypes.Tag tagToCount;  // change = number of these tags played by the player instead
    public TMTypes.Tile tileToCount;  // change = number of these tiles placed instead
    public boolean any;  // tiles or tags by all players (if false, own cards only)
    public boolean opponents;  // tiles or tags by opponent players (if false, own cards only)
    public boolean onMars;  // tiles placed on mars only?

    public boolean complete;

    public ModifyPlayerResource(int player, int change, TMTypes.Resource resource, boolean production) {
        // Used for other free effects
        super(-1, change, true);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = player;
        this.player = player;
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs(change), production, player, -1));
        }
    }

    public ModifyPlayerResource(TMTypes.StandardProject standardProject, int cost, int player, int change, TMTypes.Resource resource) {
        // Used for standard project definition
        super(standardProject, -1, change, false);
        this.resource = resource;
        this.production = true;
        this.targetPlayer = player;
        this.player = player;
        this.setActionCost(TMTypes.Resource.MegaCredit, cost, -1);
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs(change), true, player, -1));
        }
    }

    public ModifyPlayerResource(int player, int targetPlayer, double change, TMTypes.Resource resource, boolean production,
                                TMTypes.Tag tagToCount, TMTypes.Tile tileToCount, boolean any, boolean opponents, boolean onMars,
                                TMTypes.Resource counterResource, boolean counterResourceProduction,
                                boolean free) {
        // Copy constructor, used in extended sequence
        super(-1, change, free);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = targetPlayer;
        this.player = player;
        this.tagToCount = tagToCount;
        this.tileToCount = tileToCount;
        this.any = any;
        this.opponents = opponents;
        this.onMars = onMars;
        this.counterResource = counterResource;
        this.counterResourceProduction = counterResourceProduction;
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs((int)change), production, targetPlayer, -1));
        }
    }

    public ModifyPlayerResource(int player, int targetPlayer, double change, TMTypes.Resource resource, boolean production,
                                boolean free) {
        // Used for parsing, other properties set individually
        super(-1, change, free);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = targetPlayer;
        this.player = player;
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs((int)change), production, targetPlayer, -1));
        }
    }

    @Override
    public boolean _execute(TMGameState gs) {
        if (!complete && (targetPlayer == -2 || counterResource != null)) {
            // Player chooses who this applies to, or how much to decrease resource to increase counterResource by same amount
            gs.setActionInProgress(this);
            return true;
        } else {
            // Just execute
            if (targetPlayer == -1) {
                if (player != -1) {
                    targetPlayer = player;
                } else {
                    // current player
                    targetPlayer = gs.getCurrentPlayer();
                    player = targetPlayer;
                }
            }
            if (production) {
                counterID = gs.getPlayerProduction()[targetPlayer].get(resource).getComponentID();
            } else {
                counterID = gs.getPlayerResources()[targetPlayer].get(resource).getComponentID();
            }
            if (tagToCount != null) {
                if (any || opponents) {
                    int count = 0;
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        if (opponents && i == player) continue;
                        count += gs.getPlayerCardsPlayedTags()[i].get(tagToCount).getValue();
                    }
                    change *= count;
                } else {
                    change *= gs.getPlayerCardsPlayedTags()[player].get(tagToCount).getValue();
                }
            } else if (tileToCount != null) {
                if (onMars) {
                    int count = 0;
                    for (int i = 0; i < gs.getBoard().getHeight(); i++) {
                        for (int j = 0; j < gs.getBoard().getHeight(); j++) {
                            TMMapTile mt = gs.getBoard().getElement(j, i);
                            if (mt != null && mt.getTilePlaced() == tileToCount) {
                                if (any) count ++;
                                else if (opponents && mt.getOwner() != player || !opponents && mt.getOwner() == player) count ++;
                            }
                        }
                    }
                    change *= count;
                } else {
                    if (any || opponents) {
                        int count = 0;
                        for (int i = 0; i < gs.getNPlayers(); i++) {
                            if (opponents && i == player) continue;
                            count += gs.getPlayerTilesPlaced()[i].get(tileToCount).getValue();
                        }
                        change *= count;
                    } else {
                        change *= gs.getPlayerTilesPlaced()[player].get(tileToCount).getValue();
                    }
                }
            }
            if (counterResource != null) {
                // Increase by abs change
                Counter c;
                if (counterResourceProduction) {
                    c = gs.getPlayerProduction()[targetPlayer].get(counterResource);
                } else {
                    c = gs.getPlayerResources()[targetPlayer].get(counterResource);
                }
                c.increment(Math.abs((int)change));
                gs.getPlayerResourceIncreaseGen()[targetPlayer].put(counterResource, true);
            }
            if (change > 0) {
                gs.getPlayerResourceIncreaseGen()[targetPlayer].put(resource, true);
            }
            return super._execute(gs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifyPlayerResource)) return false;
        if (!super.equals(o)) return false;
        ModifyPlayerResource that = (ModifyPlayerResource) o;
        return production == that.production && targetPlayer == that.targetPlayer && counterResourceProduction == that.counterResourceProduction && any == that.any && opponents == that.opponents && onMars == that.onMars && resource == that.resource && counterResource == that.counterResource && tagToCount == that.tagToCount && tileToCount == that.tileToCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, production, targetPlayer, counterResource, counterResourceProduction, tagToCount, tileToCount, any, opponents, onMars);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Modify p" + targetPlayer + " " + resource + (production? " production " : "") + " by " + change;
    }

    @Override
    public String toString() {
        return "Modify p" + targetPlayer + " " + resource + (production? " production " : "") + " by " + change;
    }

    @Override
    public ModifyPlayerResource copy() {
        return this;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (targetPlayer == -2) {
            // Choose a player
            if (targetPlayerOptions != null) {
                for (int i: targetPlayerOptions) {
                    if (i == -1) {
                        actions.add(new TMAction(player));  // Pass
                        continue;
                    }
                    ModifyPlayerResource a = new ModifyPlayerResource(player, i, change, resource, production, tagToCount, tileToCount,
                            any, opponents, onMars, counterResource, counterResourceProduction, true);
                    a.complete = true;
                    if (a.canBePlayed((TMGameState) state)) {
                        actions.add(a);
                    }
                }
            } else {
                for (int i = 0; i < state.getNPlayers(); i++) {
                    ModifyPlayerResource a = new ModifyPlayerResource(player, i, change, resource, production, tagToCount, tileToCount,
                            any, opponents, onMars, counterResource, counterResourceProduction, true);
                    a.complete = true;
                    if (a.canBePlayed((TMGameState) state)) {
                        actions.add(a);
                    }
                }
            }
        } else if (counterResource != null) {
            // Choose amount
            TMGameState gs = (TMGameState)state;
            Counter c;
            if (production) {
                c = gs.getPlayerProduction()[player].get(resource);
            } else {
                c = gs.getPlayerResources()[player].get(resource);
            }
            int max = (c.getMinimum() < 0? c.getValue() + Math.abs(c.getMinimum()) : c.getValue());
            for (int i = 0; i < max; i++) {
                ModifyPlayerResource a = new ModifyPlayerResource(player, targetPlayer, -i, resource, production, tagToCount, tileToCount,
                        any, opponents, onMars, counterResource, counterResourceProduction, true);
                a.complete = true;
            }
        }

        if (actions.size() == 0) {
            actions.add(new TMAction(player)); // Should not happen
        }

        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        complete = true;  // Only 1 step
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

}

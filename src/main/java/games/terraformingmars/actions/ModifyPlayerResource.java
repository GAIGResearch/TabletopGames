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

    transient public boolean complete;

    public ModifyPlayerResource() { super(); } // This is needed for JSON Deserializer

    public ModifyPlayerResource(int player, int change, TMTypes.Resource resource, boolean production) {
        // Used for other free effects
        super(-1, change, true);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = player;
        this.player = player;
        if (change < 0 && production) {
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
        if (change < 0 && production) {
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
        if (change < 0 && production) {
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
            if (targetPlayer == -1 || targetPlayer == -2) { // TODO: why targetplayer remains -2 when in this branch?
                if (player != -1 && player != -2) {
                    targetPlayer = player;
                } else {
                    // current player
                    targetPlayer = gs.getCurrentPlayer();
                    player = targetPlayer;
                }
            } else if (targetPlayer == -3) {
                // It's -3 in solo play when action is just counted as "done" to the neutral player
                return super._execute(gs);
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
                            TMMapTile mt = (TMMapTile) gs.getBoard().getElement(j, i);
                            if (mt != null && mt.getTilePlaced() == tileToCount) {
                                if (any) count ++;
                                else if (opponents && mt.getOwnerId() != player || !opponents && mt.getOwnerId() == player) count ++;
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
                c.increment((int)(-1 * change));
                if (-1 * change > 0 && !counterResourceProduction) {
                    gs.getPlayerResourceIncreaseGen()[targetPlayer].put(counterResource, true);
                }
            }
            if (change > 0 && !production) {
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
        return production == that.production && targetPlayer == that.targetPlayer && counterResourceProduction == that.counterResourceProduction && any == that.any && opponents == that.opponents && onMars == that.onMars && complete == that.complete && resource == that.resource && Objects.equals(targetPlayerOptions, that.targetPlayerOptions) && counterResource == that.counterResource && tagToCount == that.tagToCount && tileToCount == that.tileToCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, production, targetPlayer, targetPlayerOptions, counterResource, counterResourceProduction, tagToCount, tileToCount, any, opponents, onMars, complete);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        String s = "Modify ";
        if (targetPlayer == -1) {
            s += "your ";
        } else if (targetPlayer == -2) {
            s += "any ";
        } else {
            s += "p" + targetPlayer + " ";
        }
        s += resource + (production? " production " : " ");
        if (counterResource != null) {
            if (!complete) s += "by -X and " + counterResource + (counterResourceProduction? " production " : " ") + "by X";
            else s += "by " + change + " and " + counterResource + (counterResourceProduction? " production " : " ") + "by " + (-1 * change);
        } else {
            s += "by " + change;
        }
        if (tagToCount != null) {
            s += " for each " + tagToCount + (any? " ever played" : opponents? " opponents played" : " you played");
        }
        if (tileToCount != null) {
            s += " for each " + tileToCount + (onMars? " (on Mars)" : "") + (any? " ever played" : opponents? " opponents played" : " you played");
        }
        return s;
    }

    @Override
    public ModifyPlayerResource _copy() {
        ModifyPlayerResource copy = new ModifyPlayerResource(player, targetPlayer, change, resource, production, tagToCount, tileToCount, any, opponents, onMars, counterResource, counterResourceProduction, freeActionPoint);
        copy.counterID = counterID;
        if (targetPlayerOptions != null) {
            copy.targetPlayerOptions = new HashSet<>(targetPlayerOptions);
        }
        copy.complete = complete;
        return copy;
    }

    @Override
    public ModifyPlayerResource copy() {
        return (ModifyPlayerResource) super.copy();
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        TMGameState gs = (TMGameState)state;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (targetPlayer == -2) {
            Counter c = gs.getPlayerResources()[player].get(resource);
            double max = -1*Math.min(Math.abs(change),(c.getMinimum() < 0? c.getValue() + Math.abs(c.getMinimum()) : c.getValue()));
            // Choose a player
            if (targetPlayerOptions != null) {
                for (int i: targetPlayerOptions) {
                    if (i == -1) {
                        actions.add(new TMAction(player));  // Pass
                        continue;
                    }
                    if (!production && change < 0) {
                        for (double k = max; k < 0; k++) {
                            createActions((TMGameState) state, actions, i, k);
                        }
                    } else {
                        createActions((TMGameState) state, actions, i, change);
                    }
                }
            } else {
                for (int i = 0; i < state.getNPlayers(); i++) {
                    if (!production && change < 0) {
                        for (double k = max; k < 0; k++) {
                            createActions((TMGameState) state, actions, i, k);
                        }
                    } else {
                        createActions((TMGameState) state, actions, i, change);
                    }
                }
                if (state.getNPlayers() == 1) {
                    if (!production && change < 0) {
                        for (double k = max; k < 0; k++) {
                            createActions((TMGameState) state, actions, -3, k);
                        }
                    } else {
                        createActions((TMGameState) state, actions, -3, change);
                    }
                }
            }
        } else if (counterResource != null) {
            // Choose amount
            Counter c;
            if (production) {
                c = gs.getPlayerProduction()[player].get(resource);
            } else {
                c = gs.getPlayerResources()[player].get(resource);
            }
            int max = (c.getMinimum() < 0? c.getValue() + Math.abs(c.getMinimum()) : c.getValue());
            for (int i = 0; i <= max; i++) {
                ModifyPlayerResource a = new ModifyPlayerResource(player, targetPlayer, -i, resource, production, tagToCount, tileToCount,
                        any, opponents, onMars, counterResource, counterResourceProduction, true);
                a.complete = true;
                actions.add(a);
            }
        }

        if (actions.size() == 0) {
            actions.add(new TMAction(player)); // Should not happen
        }

        return actions;
    }

    private void createActions(TMGameState state, ArrayList<AbstractAction> actions, int i, double k) {
        ModifyPlayerResource a = new ModifyPlayerResource(player, i, k, resource, production, tagToCount, tileToCount,
                any, opponents, onMars, counterResource, counterResourceProduction, true);
        a.complete = true;
        if (a.canBePlayed(state)) {
            actions.add(a);
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        complete = true;  // Only 1 step
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return complete;
    }

}

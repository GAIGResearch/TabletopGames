package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameParameters;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.ResourceRequirement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModifyPlayerResource extends TMModifyCounter implements IExtendedSequence {
    public TMTypes.Resource resource;
    public boolean production;
    public int targetPlayer;

    public TMTypes.Tag tagToCount;  // change = number of these tags played by the player instead
    public TMTypes.Tile tileToCount;  // change = number of these tiles placed instead
    public boolean any;  // tiles or tags by all players (if false, own cards only)
    public boolean opponents;  // tiles or tags by opponent players (if false, own cards only)
    public boolean onMars;  // tiles placed on mars only?

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

    public ModifyPlayerResource(int player, int targetPlayer, int change, TMTypes.Resource resource, boolean production,
                                TMTypes.Tag tagToCount, TMTypes.Tile tileToCount, boolean any, boolean opponents, boolean onMars,
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
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs(change), production, targetPlayer, -1));
        }
    }

    public ModifyPlayerResource(int player, int targetPlayer, int change, TMTypes.Resource resource, boolean production,
                                boolean free) {
        // Used for parsing, other properties set individually
        super(-1, change, free);
        this.resource = resource;
        this.production = production;
        this.targetPlayer = targetPlayer;
        this.player = player;
        if (change < 0) {
            this.requirements.add(new ResourceRequirement(resource, Math.abs(change), production, targetPlayer, -1));
        }
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
            if (tagToCount != null) {
                if (any || opponents) {
                    int count = 0;
                    for (int i = 0; i < gs.getNPlayers(); i++) {
                        if (opponents && i == player) continue;
                        count += ggs.getPlayerCardsPlayedTags()[i].get(tagToCount).getValue();
                    }
                    change *= count;
                } else {
                    change *= ggs.getPlayerCardsPlayedTags()[player].get(tagToCount).getValue();
                }
            } else if (tileToCount != null) {
                if (onMars) {
                    int count = 0;
                    for (int i = 0; i < ggs.getBoard().getHeight(); i++) {
                        for (int j = 0; j < ggs.getBoard().getHeight(); j++) {
                            TMMapTile mt = ggs.getBoard().getElement(j, i);
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
                            count += ggs.getPlayerTilesPlaced()[i].get(tileToCount).getValue();
                        }
                        change *= count;
                    } else {
                        change *= ggs.getPlayerTilesPlaced()[player].get(tileToCount).getValue();
                    }
                }
            }
            if (change > 0) {
                ggs.getPlayerResourceIncreaseGen()[targetPlayer].put(resource, true);
            }
            return super.execute(gs);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModifyPlayerResource)) return false;
        if (!super.equals(o)) return false;
        ModifyPlayerResource that = (ModifyPlayerResource) o;
        return production == that.production && targetPlayer == that.targetPlayer && any == that.any && opponents == that.opponents && onMars == that.onMars && resource == that.resource && tagToCount == that.tagToCount && tileToCount == that.tileToCount;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), resource, production, targetPlayer, tagToCount, tileToCount, any, opponents, onMars);
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
        for (int i = 0; i < state.getNPlayers(); i++) {
            TMAction a = new ModifyPlayerResource(player, i, change, resource, production, tagToCount, tileToCount, any, opponents, onMars, true);
            if (a.canBePlayed((TMGameState) state)) {
                actions.add(a);
            }
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
        ModifyPlayerResource a = (ModifyPlayerResource) action;
        targetPlayer = a.targetPlayer;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return targetPlayer != -2;
    }

}

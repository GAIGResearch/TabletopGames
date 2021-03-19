package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.effects.Effect;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.Objects;

public class TMAction extends AbstractAction {
    public final boolean free;
    public int player;
    public final boolean pass;

    public Requirement<TMGameState> requirement;
    public boolean played;

    public TMTypes.ActionType actionType;
    public TMTypes.StandardProject standardProject;

    public TMAction(TMTypes.ActionType actionType, int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
        this.actionType = actionType;
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
    }

    public TMAction(int player) {
        this.player = player;
        this.free = false;
        this.pass = true;
    }

    public TMAction(int player, boolean free) {
        this.player = player;
        this.free = free;
        this.pass = false;
    }

    public TMAction(int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
    }

    public TMAction(TMTypes.ActionType actionType, int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
        this.actionType = actionType;
    }

    public TMAction(TMTypes.StandardProject project, int player, boolean free, Requirement requirement) {
        this.player = player;
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
        this.actionType = TMTypes.ActionType.StandardProject;
        this.standardProject = project;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        TMGameState gs = (TMGameState) gameState;
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        if (!free) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken(gs, this, player);
        }
        played = true;

        // Check persisting effects for all players
        for (int i = 0; i < gs.getNPlayers(); i++) {
            for (Effect e: gs.getPlayerPersistingEffects()[i]) {
                e.execute(gs, this, i);
            }
        }

        // Check if player has Card resources, transform those to cards into hand
        Counter c = gs.getPlayerResources()[player].get(TMTypes.Resource.Card);
        int nCards = c.getValue();
        if (nCards > 0) {
            for (int i = 0; i < nCards; i++) {
                gs.getPlayerHands()[player].add(gs.getProjectCards().pick(0));
            }
            c.setValue(0);
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // TODO
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMAction)) return false;
        TMAction tmAction = (TMAction) o;
        return free == tmAction.free && player == tmAction.player && pass == tmAction.pass && played == tmAction.played && Objects.equals(requirement, tmAction.requirement) && actionType == tmAction.actionType && standardProject == tmAction.standardProject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(free, player, pass, requirement, played, actionType, standardProject);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return actionType != null? actionType.name() : "Pass";
    }

    @Override
    public String toString() {
        return actionType != null? actionType.name() : "Pass";
    }

    public static Pair<TMAction, String> parseAction(TMGameState gameState, String encoding) {

        // Third element is effect
        TMAction effect = null;
        String effectString = "";
        int player = gameState.getCurrentPlayer();

        if (encoding.contains("inc") || encoding.contains("dec")) {
            // Increase/Decrease counter action
            String[] split2 = encoding.split("-");
            // Find how much
            int increment = Integer.parseInt(split2[2]);
            if (encoding.contains("dec")) increment *= -1;

            effectString = split2[1];

            // Find which counter
            Counter which = gameState.stringToGPCounter(split2[1]);

            if (which == null) {
                // A resource or production instead
                String resString = split2[1].split("prod")[0];
                TMTypes.Resource res = Utils.searchEnum(TMTypes.Resource.class, resString);
                effect = new PlaceholderModifyCounter(player, increment, res, split2[1].contains("prod"),true);
            } else {
                // A global counter (temp, oxygen, oceantiles)
                effect = new TMModifyCounter(player, which.getComponentID(), increment, true);
            }
        } else if (encoding.contains("placetile")) {
            // equals("placetile:ocean:ocean")
            // PlaceTile action
            String[] split2 = encoding.split("/");
            // split2[1] is type of tile to place
            TMTypes.Tile toPlace = Utils.searchEnum(TMTypes.Tile.class, split2[1]);
            // split2[2] is where to place it. can be a map tile, or a city name.
            TMTypes.MapTileType where = Utils.searchEnum(TMTypes.MapTileType.class, split2[2]);
            HashSet<Vector2D> legalPositions = new HashSet<>();
            for (int i = 0; i < gameState.getBoard().getHeight(); i++) {
                for (int j = 0; j < gameState.getBoard().getWidth(); j++) {
                    TMMapTile mt = gameState.getBoard().getElement(j, i);
                    if (mt != null) {
                        if (where != null && mt.getTileType() == where || where == null && mt.getComponentName().equalsIgnoreCase(split2[2])) {
                            legalPositions.add(new Vector2D(j, i));
                        }
                    }
                }
            }
            effect = new PlaceTile(player, toPlace, legalPositions, true);  // Extended sequence, will ask player where to put it
            effectString = split2[1];
        }
        return new Pair<>(effect, effectString);
    }
}

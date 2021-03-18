package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTurnOrder;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import games.terraformingmars.rules.requirements.Requirement;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.HashSet;
import java.util.Objects;

public class TMAction extends AbstractAction {
    public Requirement<TMGameState> requirement;
    final boolean free;
    public final boolean pass;
    public boolean played;

    public TMAction(boolean free) {
        this.free = free;
        this.pass = false;
    }

    public TMAction() {
        this.free = false;
        this.pass = true;
    }

    public TMAction(boolean free, Requirement requirement) {
        this.free= free;
        this.pass = false;
        this.requirement = requirement;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (!free) {
            ((TMTurnOrder)gs.getTurnOrder()).registerActionTaken((TMGameState) gs, this);
        }
        played = true;
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TMAction)) return false;
        TMAction tmAction = (TMAction) o;
        return free == tmAction.free && pass == tmAction.pass && played == tmAction.played && Objects.equals(requirement, tmAction.requirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requirement, free, pass, played);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "Pass";
    }

    public void setPlayed(boolean played) {
        this.played = played;
    }

    public boolean isPlayed() {
        return played;
    }

    public static Pair<TMAction, String> parseAction(TMGameState gameState, String encoding) {

        // Third element is effect
        TMAction effect = null;
        String effectString = "";

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
                TMTypes.Resource res = TMTypes.Resource.valueOf(resString);
                effect = new PlaceholderModifyCounter(increment, res, split2[1].contains("prod"),true);
            } else {
                // A global counter (temp, oxygen, oceantiles)
                effect = new TMModifyCounter(which.getComponentID(), increment, true);
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
            effect = new PlaceTile(toPlace, legalPositions, true);  // Extended sequence, will ask player where to put it
            effectString = split2[1];
        }
        return new Pair<>(effect, effectString);
    }
}

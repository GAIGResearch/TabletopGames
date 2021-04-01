package games.terraformingmars.rules.requirements;

import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMMapTile;
import utilities.Group;

import java.awt.*;
import java.util.HashMap;

import static games.terraformingmars.actions.PlaceTile.*;

public class AdjacencyRequirement implements Requirement<Group<TMGameState, TMMapTile, Integer>> {

    public HashMap<TMTypes.Tile, Integer> tileTypes;
    public boolean owned;
    public boolean noneAdjacent;
    public boolean reversed;

    public AdjacencyRequirement() {}
    public AdjacencyRequirement(HashMap<TMTypes.Tile, Integer> tileTypes) {
        this.tileTypes = tileTypes;
    }

    @Override
    public boolean testCondition(Group<TMGameState, TMMapTile, Integer> o) {
        if (owned) return isAdjacentToPlayerOwnedTiles(o.a, o.b, o.c);
        if (noneAdjacent) {
            boolean isAdjacent = isAdjacentToAny(o.a, o.b);
            if (reversed) return isAdjacent;
            else return !isAdjacent;
        }
        if (tileTypes != null) {
            for (TMTypes.Tile t: tileTypes.keySet()) {
                int count = nAdjacentTiles(o.a, o.b, t);
                if (!reversed && count < tileTypes.get(t)) return false;
                if (reversed && count >= tileTypes.get(t)) return false;
            }
        }
        return true;
    }

    @Override
    public boolean isMax() {
        return false;
    }

    @Override
    public boolean appliesWhenAnyPlayer() {
        return false;
    }

    @Override
    public String getDisplayText(TMGameState gs) {
        return null;
    }

    @Override
    public String getReasonForFailure(TMGameState gs) {
        return null;
    }

    @Override
    public Image[] getDisplayImages() {
        return null;
    }

    @Override
    public String toString() {
        return "Adjacency";
    }
}

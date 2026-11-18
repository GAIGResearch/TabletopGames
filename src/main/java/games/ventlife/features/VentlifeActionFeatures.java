package games.ventlife.features;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IActionFeatureVector;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.actions.*;
import games.ventlife.components.*;

/**
 * Species-agnostic features of a Ventlife action. The target is the hex a creature is placed on or moved to; for a
 * tile, only the level it will sit at and the creatures it covers are given. Features that do not apply to the action
 * are 0, false or null.
 */
public class VentlifeActionFeatures implements IActionFeatureVector {

    public enum ActionType {DRAFT, TILE_SEAFLOOR, TILE_PLATEAU, PLACE, DISPLACE, STOP}

    private static final String[] names = {"actionType", "targetLevel", "targetIsEdge", "targetTerrain",
            "ownAdjacent", "oppAdjacent", "emptyAdjacent", "ownCovered", "oppCovered", "exposedAfter"};

    private static final Class<?>[] types = {ActionType.class, Integer.class, Boolean.class, Terrain.class,
            Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Boolean.class};

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public Class<?>[] types() {
        return types;
    }

    @Override
    public double[] doubleVector(AbstractAction action, AbstractGameState state, int playerID) {
        throw new UnsupportedOperationException("Use featureVector: actionType and targetTerrain are enums");
    }

    @Override
    public Object[] featureVector(AbstractAction action, AbstractGameState gs, int playerID) {
        VentlifeGameState state = (VentlifeGameState) gs;
        Object[] retValue = {null, 0, false, null, 0, 0, 0, 0, 0, false};

        Hex target = null;
        Species species = null;
        switch (action) {
            case DraftSpecies d -> retValue[0] = ActionType.DRAFT;
            case StopPlacing s -> retValue[0] = ActionType.STOP;
            case PlaceCreature p -> {
                retValue[0] = ActionType.PLACE;
                target = p.hex;
                species = p.species;
            }
            case Displace d -> {
                retValue[0] = ActionType.DISPLACE;
                target = d.to;
                species = d.creature.species();
            }
            case PlaceTile t -> {
                retValue[0] = state.getCell(t.smoker) == null ? ActionType.TILE_SEAFLOOR : ActionType.TILE_PLATEAU;
                retValue[1] = state.getLevel(t.smoker) + 1;
                int own = 0, opp = 0;
                for (Hex h : VentlifeUtils.tileHexes(t.smoker, t.orientation)) {
                    Creature c = state.getCreature(h);
                    // Tube Worms under the new Black Smoker climb onto it rather than being covered
                    if (c == null || (h.equals(t.smoker) && c.species() == Species.TUBE_WORM))
                        continue;
                    if (c.owner() == playerID)
                        own += c.count();
                    else
                        opp += c.count();
                }
                retValue[7] = own;
                retValue[8] = opp;
            }
            default -> throw new IllegalArgumentException("Unexpected action " + action);
        }

        if (target != null) {
            retValue[1] = state.getLevel(target);
            retValue[2] = state.isEdge(target);
            retValue[3] = state.getCell(target).terrain();
            int own = 0, opp = 0, empty = 0;
            for (Hex n : target.neighbours()) {
                Creature c = state.getCreature(n);
                if (c == null) {
                    if (state.isEmptyHex(n))
                        empty++;
                } else if (c.owner() == playerID)
                    own += c.count();
                else
                    opp += c.count();
            }
            retValue[4] = own;
            retValue[5] = opp;
            retValue[6] = empty;
            retValue[9] = VentlifeUtils.isExposed(state, target, species);
        }
        return retValue;
    }
}

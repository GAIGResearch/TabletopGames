package games.ventlife.features;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.*;

import java.util.*;

/**
 * Species-agnostic features of a Ventlife state from one player's perspective. Only the player's own tile is
 * looked at, so the features are the same in every redeterminisation.
 */
public class VentlifeStateFeatures implements IStateFeatureVector {

    private static final String[] names = {"score", "lead", "progress", "bestSpeciesScore", "creaturesOnField",
            "oppCreaturesOnField", "speciesExhausted", "speciesPresent", "pointsPerCreature", "exposedCreatures",
            "meanHeightScoringLevel", "emptyHexes", "fieldSize", "maxLevel", "raisedHexes", "edgeHexes",
            "crabPlateausLed", "largestShoal"};

    @Override
    public String[] names() {
        return names;
    }

    @Override
    public double[] doubleVector(AbstractGameState gs, int playerID) {
        VentlifeGameState state = (VentlifeGameState) gs;
        double[] retValue = new double[names.length];

        retValue[0] = state.getGameScore(playerID);
        retValue[1] = state.getLead(playerID);
        int unplaced = state.getDrawDeck().getSize();
        for (int p = 0; p < state.getNPlayers(); p++)
            unplaced += state.getHand(p).getSize();
        retValue[2] = (double) state.getTilesPlaced() / (state.getTilesPlaced() + unplaced);
        retValue[3] = state.getTiebreak(playerID, 1);

        // creatures
        int[] onField = new int[state.getNPlayers()];
        Set<Species> present = EnumSet.noneOf(Species.class);
        Set<Hex> fish = new HashSet<>();
        int exposed = 0, heightScorers = 0, heightScorerLevels = 0;
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
            Creature c = e.getValue();
            onField[c.owner()] += c.count();
            if (c.owner() != playerID)
                continue;
            Hex hex = e.getKey();
            present.add(c.species());
            if (VentlifeUtils.isExposed(state, hex, c.species()))
                exposed += c.count();
            // the species whose score rises with the level of their own hex
            if (c.species() == Species.TUBE_WORM || c.species() == Species.YETI_CRAB) {
                heightScorers += c.count();
                heightScorerLevels += c.count() * state.getLevel(hex);
            }
            if (c.species() == Species.EELPOUT_FISH)
                fish.add(hex);
        }
        retValue[4] = onField[playerID];
        for (int p = 0; p < onField.length; p++)
            if (p != playerID)
                retValue[5] = Math.max(retValue[5], onField[p]);
        for (Species s : state.getSpeciesInPlay())
            if (state.getSupply(playerID, s) == 0)
                retValue[6]++;
        retValue[7] = present.size();
        retValue[8] = onField[playerID] == 0 ? 0 : retValue[0] / onField[playerID];
        retValue[9] = exposed;
        retValue[10] = heightScorers == 0 ? 0 : (double) heightScorerLevels / heightScorers;

        // the field
        for (Map.Entry<Hex, HexCell> e : state.getField().entrySet()) {
            Hex hex = e.getKey();
            int level = e.getValue().level();
            if (!state.getCreatures().containsKey(hex))
                retValue[11]++;
            retValue[13] = Math.max(retValue[13], level);
            if (level >= 2)
                retValue[14]++;
            if (state.isEdge(hex))
                retValue[15]++;
        }
        retValue[12] = state.getField().size();

        // groups
        for (Set<Hex> plateau : VentlifeUtils.plateaus(state)) {
            int[] crabs = VentlifeUtils.crabsOn(state, plateau);
            if (crabs[playerID] > 0 && crabs[playerID] == Arrays.stream(crabs).max().orElse(0))
                retValue[16]++;
        }
        for (Set<Hex> shoal : VentlifeUtils.connectedGroups(fish))
            retValue[17] = Math.max(retValue[17], shoal.size());

        return retValue;
    }
}

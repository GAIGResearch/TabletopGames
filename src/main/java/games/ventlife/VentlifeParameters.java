package games.ventlife;

import evaluation.optimisation.TunableParameters;
import games.ventlife.components.Species;
import games.ventlife.components.Terrain;
import games.ventlife.components.VentTile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Ventlife. The defaults are the rules of Rulebook 3.0 (Regular Game), with the decisions recorded in
 * claude_game_creator/Ventlife_plan.txt. The tile distribution is read from tileFile.
 */
public class VentlifeParameters extends TunableParameters<VentlifeParameters> {

    public enum SpeciesSelection {RANDOM, FIRST_GAME, DRAFT}

    /** The species of the First Game. */
    public static final List<Species> FIRST_GAME_SPECIES = List.of(Species.TUBE_WORM, Species.VENT_SHRIMP,
            Species.VOLCANO_SNAIL, Species.YETI_CRAB);

    // the tile kinds and their quantities by player count, loaded from tileFile; derived from that parameter, so
    // not part of equals
    private record TileKind(Terrain left, Terrain right, int[] quantity) {
    }
    private List<TileKind> tileKinds;
    private String loadedTileFile;

    public String tileFile = "data/ventlife/tiles.json";
    public SpeciesSelection speciesSelection = SpeciesSelection.RANDOM;
    public int nSpecies = 4;
    public int tokensPerSpecies = 6;
    // the game ends at the end of the round in which a player has used every token of this many species (0: never)
    public int exhaustedSpeciesToEnd = 2;
    // only Tube Worms and Shrimp may be on a Black Smoker (the Habitat Guide); if false, only Fish are kept off them
    public boolean smokersOnlyForWormsAndShrimp = true;
    public int lowVentBonus = 1;
    public int maxSnailsPerTurn = 3;
    public int snailPointsPerHeightEdge = 1;
    public int shrimpPointsOneTerrain = 2;
    public int shrimpPointsBothTerrains = 5;
    // a shoal of n Fish (n >= 2) scores shoalPoints * (n - 1)
    public int shoalPoints = 5;
    public int octopusPointsPerSpecies = 2;
    public int spongePointsPerTerrain = 2;

    public VentlifeParameters() {
        addTunableParameter("tileFile", "data/ventlife/tiles.json");
        addTunableParameter("speciesSelection", SpeciesSelection.RANDOM, Arrays.asList(SpeciesSelection.values()));
        addTunableParameter("nSpecies", 4, Arrays.asList(3, 4, 5));
        addTunableParameter("tokensPerSpecies", 6, Arrays.asList(4, 5, 6, 7, 8));
        addTunableParameter("exhaustedSpeciesToEnd", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("smokersOnlyForWormsAndShrimp", true, Arrays.asList(true, false));
        addTunableParameter("lowVentBonus", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("maxSnailsPerTurn", 3, Arrays.asList(1, 2, 3));
        addTunableParameter("snailPointsPerHeightEdge", 1);
        addTunableParameter("shrimpPointsOneTerrain", 2);
        addTunableParameter("shrimpPointsBothTerrains", 5);
        addTunableParameter("shoalPoints", 5);
        addTunableParameter("octopusPointsPerSpecies", 2);
        addTunableParameter("spongePointsPerTerrain", 2);
        _reset();
    }

    @Override
    public void _reset() {
        tileFile = (String) getParameterValue("tileFile");
        speciesSelection = (SpeciesSelection) getParameterValue("speciesSelection");
        nSpecies = (int) getParameterValue("nSpecies");
        tokensPerSpecies = (int) getParameterValue("tokensPerSpecies");
        exhaustedSpeciesToEnd = (int) getParameterValue("exhaustedSpeciesToEnd");
        smokersOnlyForWormsAndShrimp = (boolean) getParameterValue("smokersOnlyForWormsAndShrimp");
        lowVentBonus = (int) getParameterValue("lowVentBonus");
        maxSnailsPerTurn = (int) getParameterValue("maxSnailsPerTurn");
        snailPointsPerHeightEdge = (int) getParameterValue("snailPointsPerHeightEdge");
        shrimpPointsOneTerrain = (int) getParameterValue("shrimpPointsOneTerrain");
        shrimpPointsBothTerrains = (int) getParameterValue("shrimpPointsBothTerrains");
        shoalPoints = (int) getParameterValue("shoalPoints");
        octopusPointsPerSpecies = (int) getParameterValue("octopusPointsPerSpecies");
        spongePointsPerTerrain = (int) getParameterValue("spongePointsPerTerrain");
        if (tileKinds == null || !tileFile.equals(loadedTileFile)) {
            tileKinds = loadTiles(tileFile);
            loadedTileFile = tileFile;
        }
    }

    private static List<TileKind> loadTiles(String fileName) {
        JSONObject json = JSONUtils.loadJSONFile(fileName);
        List<TileKind> retValue = new ArrayList<>();
        for (Object o : (JSONArray) json.get("tiles")) {
            JSONObject t = (JSONObject) o;
            JSONArray q = (JSONArray) t.get("quantity");
            int[] quantity = new int[q.size()];
            for (int i = 0; i < quantity.length; i++)
                quantity[i] = ((Long) q.get(i)).intValue();
            retValue.add(new TileKind(Terrain.fromLabel((String) t.get("left")),
                    Terrain.fromLabel((String) t.get("right")), quantity));
        }
        return retValue;
    }

    /**
     * The tiles in play for the player count: for each kind, the quantities of every column from 2 players up to
     * nPlayers.
     */
    public List<VentTile> tilesFor(int nPlayers) {
        List<VentTile> retValue = new ArrayList<>();
        for (TileKind k : tileKinds) {
            int n = 0;
            for (int col = 0; col <= nPlayers - 2 && col < k.quantity.length; col++)
                n += k.quantity[col];
            for (int i = 0; i < n; i++)
                retValue.add(new VentTile(k.left, k.right));
        }
        return retValue;
    }

    /**
     * The points for one shoal of Fish.
     */
    public int shoalScore(int size) {
        return size < 2 ? 0 : shoalPoints * (size - 1);
    }

    @Override
    protected VentlifeParameters _copy() {
        return new VentlifeParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof VentlifeParameters;
    }

    @Override
    public VentlifeParameters instantiate() {
        return this;
    }
}

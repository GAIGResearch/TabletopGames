package games.terraformingmars;

import core.AbstractParameters;

import java.util.HashMap;

public class TMGameParameters extends AbstractParameters {

    int boardSize = 9;
    String boardPath = "data/terraformingmars/board.json";
//    String projectsPath = "data/terraformingmars/projects.json";
    String projectsPath = "data/terraformingmars/htmlproj.json";
    String corpsPath = "data/terraformingmars/corporations.json";

    HashMap<TMTypes.Resource, Integer> minimumProduction = new HashMap<TMTypes.Resource, Integer>() {{
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            if (res == TMTypes.Resource.MegaCredit) put(res, -5);
            else put(res, 0);
        }
    }};
    HashMap<TMTypes.Resource, Integer> startingResources = new HashMap<TMTypes.Resource, Integer>() {{
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            put(res, 0);
        }
        put(TMTypes.Resource.TR, 20);
        put(TMTypes.Resource.MegaCredit, 500);  // TODO Test
    }};
    HashMap<TMTypes.Resource, Integer> startingProduction = new HashMap<TMTypes.Resource, Integer>() {{
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            put(res, 1);
        }
    }};
    int maxPoints = 500;

    int[] temperatureScales = new int[] {-30, -28, -26, -24, -22, -20, -18, -16, -14, -12, -10, -8, -6, -4, -2, 0, 2, 4, 6, 8};
    int[] oxygenScales = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};

    int nOceanTiles = 9;
    int projectPurchaseCost = 3;
    int nCorpChoiceStart = 2;
    int nProjectsStart = 10;
    int nProjectsResearch = 4;
    int nActionsPerPlayer = 2;

    public TMGameParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return new TMGameParameters(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    public HashMap<TMTypes.Resource, Integer> getMinimumProduction() {
        return minimumProduction;
    }

    public HashMap<TMTypes.Resource, Integer> getStartingProduction() {
        return startingProduction;
    }

    public HashMap<TMTypes.Resource, Integer> getStartingResources() {
        return startingResources;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public int getnCorpChoiceStart() {
        return nCorpChoiceStart;
    }

    public int getnOceanTiles() {
        return nOceanTiles;
    }

    public int getProjectPurchaseCost() {
        return projectPurchaseCost;
    }

    public int[] getOxygenScales() {
        return oxygenScales;
    }

    public int[] getTemperatureScales() {
        return temperatureScales;
    }

    public String getBoardPath() {
        return boardPath;
    }

    public String getCorpsPath() {
        return corpsPath;
    }

    public String getProjectsPath() {
        return projectsPath;
    }

    public int getnProjectsResearch() {
        return nProjectsResearch;
    }

    public int getnProjectsStart() {
        return nProjectsStart;
    }
}

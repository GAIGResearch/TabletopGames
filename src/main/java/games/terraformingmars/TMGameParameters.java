package games.terraformingmars;

import core.AbstractParameters;

import java.util.HashMap;
import java.util.HashSet;

import static games.terraformingmars.TMTypes.Expansion.*;

public class TMGameParameters extends AbstractParameters {

    int boardSize = 9;
    HashSet<TMTypes.Expansion> expansions = new HashSet<TMTypes.Expansion>() {{ add(CorporateEra); }};  // Elysium, Hellas and Venus compiling, but not fully parsed yet
    int soloTR = 14;
    int soloMaxGen = 14;
    int soloCities = 2;

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
//        put(TMTypes.Resource.MegaCredit, 500);  // TODO Test
    }};
    HashMap<TMTypes.Resource, Integer> startingProduction = new HashMap<TMTypes.Resource, Integer>() {{
        for (TMTypes.Resource res: TMTypes.Resource.values()) {
            if (res.isPlayerBoardRes()) {
                put(res, 1);
            }
        }
    }};
    int maxPoints = 500;
    int maxCards = 250;  // TODO based on expansions

    int projectPurchaseCost = 3;
    int nCorpChoiceStart = 2;
    int nProjectsStart = 10;
    int nProjectsResearch = 4;
    int nActionsPerPlayer = 2;
    int nMCGainedOcean = 2;

    // steel and titanium to MC rate
    double nSteelMC = 2;
    double nTitaniumMC = 3;

    // standard projects
    int nGainCardDiscard = 1;
    int nCostSPEnergy = 11;
    int nCostSPTemp = 14;
    int nCostSPOcean = 18;
    int nCostSPGreenery = 23;
    int nCostSPCity = 25;
    int nSPCityMCGain = 1;
    int nCostVenus = 15;

    // Resource actions
    int nCostGreeneryPlant = 8;
    int nCostTempHeat = 8;

    // Milestones, awards
    int[] nCostMilestone = new int[] {8, 8, 8};
    int[] nCostAwards = new int[] {8, 14, 20};
    int nPointsMilestone = 5;
    int nPointsAwardFirst = 5;
    int nPointsAwardSecond = 2;

    @Override
    protected AbstractParameters _copy() {
        return new TMGameParameters();
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

    public int getProjectPurchaseCost() {
        return projectPurchaseCost;
    }

    public int getnProjectsResearch() {
        return nProjectsResearch;
    }

    public int getnProjectsStart() {
        return nProjectsStart;
    }

    public HashSet<TMTypes.Expansion> getExpansions() {
        return expansions;
    }

    public int getnMCGainedOcean() {
        return nMCGainedOcean;
    }

    public int[] getnCostMilestone() {
        return nCostMilestone;
    }

    public int[] getnCostAwards() {
        return nCostAwards;
    }

    public double getnSteelMC() {
        return nSteelMC;
    }

    public double getnTitaniumMC() {
        return nTitaniumMC;
    }

    public int getnActionsPerPlayer() {
        return nActionsPerPlayer;
    }

    public int getnCostGreeneryPlant() {
        return nCostGreeneryPlant;
    }

    public int getnCostSPCity() {
        return nCostSPCity;
    }

    public int getnCostSPEnergy() {
        return nCostSPEnergy;
    }

    public int getnCostSPGreenery() {
        return nCostSPGreenery;
    }

    public int getnCostSPOcean() {
        return nCostSPOcean;
    }

    public int getnCostSPTemp() {
        return nCostSPTemp;
    }

    public int getnCostTempHeat() {
        return nCostTempHeat;
    }

    public int getnGainCardDiscard() {
        return nGainCardDiscard;
    }

    public int getnPointsAwardFirst() {
        return nPointsAwardFirst;
    }

    public int getnPointsAwardSecond() {
        return nPointsAwardSecond;
    }

    public int getnPointsMilestone() {
        return nPointsMilestone;
    }

    public int getnSPCityMCGain() {
        return nSPCityMCGain;
    }

    public int getSoloCities() {
        return soloCities;
    }

    public int getSoloMaxGen() {
        return soloMaxGen;
    }

    public int getSoloTR() {
        return soloTR;
    }
}

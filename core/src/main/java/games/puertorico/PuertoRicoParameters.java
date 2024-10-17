package games.puertorico;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

public class PuertoRicoParameters extends TunableParameters {

    public int numCornPlantations = 10;
    public int numIndigoPlantations = 12;
    public int numSugarPlantations = 11;
    public int numTobaccoPlantations = 9;
    public int numCoffeePlantations = 8;
    public int extraVisiblePlantations = 1;

    public int numCorn = 10;
    public int numIndigo = 11;
    public int numSugar = 11;
    public int numTobacco = 9;
    public int numCoffee = 9;

    public int plantationSlotsOnBoard = 12;
    public int townGridWidth = 4;
    public int townGridHeight = 3;
    public int townSlotsOnBoard = townGridWidth * townGridHeight;
    public int quarries = 8;
    public int numSmallProductionBuildings = 4;
    public int numLargeProductionBuildings = 3;
    public int numOtherBuildings = 2;
    public int numVictoryBuildings = 1;

    public int customsHouseDenominator = 4;
    public int fortressDenominator = 3;

    public int[] factoryBonuses = new int[]{0, 0, 1, 2, 3, 5};

    public int marketCapacity = 4;
    public int[][] shipCapacities = new int[][]{
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0},
            {4, 5, 6},
            {5, 6, 7},
            {6, 7, 8}
    };
    public int[] totalColonists = new int[]{0, 0, 0, 55, 75, 95};
    public int[] totalVP = new int[]{0, 0, 0, 75, 100, 122};
    // indexed by number of players, and then player number
    public int[][] startingDoubloons = new int[][]{
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0},
            {2, 2, 2, 0, 0},
            {2, 2, 2, 2, 0},
            {2, 2, 2, 2, 2}
    };


    public PuertoRicoParameters() {
       // TODO: Once we decide which parameters we want to tune
    }

    @Override
    public void _reset() {
        // TODO: Once we decide which parameters we want to tune
    }

    @Override
    protected AbstractParameters _copy() {
        return new PuertoRicoParameters();
        // TODO: Once we decide which parameters we want to tune
    }

    @Override
    protected boolean _equals(Object o) {
        return o == this;
    }

    @Override
    public PuertoRicoParameters instantiate() {
        return this;
    }
}

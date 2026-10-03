package games.risk;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Risk. The defaults are the World Domination rules of the 1993 Parker Brothers rulebook.
 * The board - territories, connections, continents and their bonuses - comes from mapFile (see RiskMap).
 */
public class RiskParameters extends TunableParameters<RiskParameters> {

    // the board, loaded from mapFile; derived from that parameter, so not part of equals
    private RiskMap map;

    public String mapFile = "data/risk/worldMap.json";
    public int startArmies3 = 35;
    public int startArmies4 = 30;
    public int startArmies5 = 25;
    public int startArmies6 = 20;
    public boolean randomTerritoryDeal = false;
    public int territoriesPerArmy = 3;
    public int minReinforcements = 3;
    public int maxAttackDice = 3;
    public int maxDefendDice = 2;
    public boolean allowBlitz = true;
    public boolean defenderChoosesDice = false;
    public int nWildCards = 2;
    // the values of the first sets traded in, in order (the parameter is a comma-separated list)
    public int[] tradeValues = {4, 6, 8, 10, 12, 15};
    public int tradeValueIncrement = 5;
    public int territoryBonus = 2;
    public int handLimit = 5;
    public int eliminationTradeLimit = 6;
    // expert rules (pdf p.15)
    public boolean linearTradeValues = false;
    public boolean fortifyAlongPath = false;
    // 0 for no limit
    public int maxArmiesPerTerritory = 0;
    // Secret Mission (pdf p.13-14); the territories are then always dealt at random
    public boolean secretMission = false;

    public RiskParameters() {
        addTunableParameter("mapFile", "data/risk/worldMap.json");
        addTunableParameter("startArmies3", 35, Arrays.asList(25, 30, 35, 40));
        addTunableParameter("startArmies4", 30, Arrays.asList(20, 25, 30, 35));
        addTunableParameter("startArmies5", 25, Arrays.asList(20, 25, 30));
        addTunableParameter("startArmies6", 20, Arrays.asList(15, 20, 25));
        addTunableParameter("randomTerritoryDeal", false);
        addTunableParameter("territoriesPerArmy", 3, Arrays.asList(2, 3, 4));
        addTunableParameter("minReinforcements", 3, Arrays.asList(1, 2, 3, 4));
        addTunableParameter("maxAttackDice", 3);
        addTunableParameter("maxDefendDice", 2);
        addTunableParameter("allowBlitz", true);
        addTunableParameter("defenderChoosesDice", false);
        addTunableParameter("nWildCards", 2, Arrays.asList(0, 1, 2, 3, 4));
        addTunableParameter("tradeValues", "4,6,8,10,12,15");
        addTunableParameter("tradeValueIncrement", 5, Arrays.asList(1, 2, 3, 5));
        addTunableParameter("territoryBonus", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("handLimit", 5);
        addTunableParameter("eliminationTradeLimit", 6);
        addTunableParameter("linearTradeValues", false);
        addTunableParameter("fortifyAlongPath", false);
        addTunableParameter("maxArmiesPerTerritory", 0, Arrays.asList(0, 8, 12, 20));
        addTunableParameter("secretMission", false);
        addTunableParameter("maxRounds", 100, Arrays.asList(50, 100, 200));
        _reset();
    }

    @Override
    public void _reset() {
        mapFile = (String) getParameterValue("mapFile");
        if (map == null || !map.fileName.equals(mapFile))
            map = new RiskMap(mapFile);
        startArmies3 = (int) getParameterValue("startArmies3");
        startArmies4 = (int) getParameterValue("startArmies4");
        startArmies5 = (int) getParameterValue("startArmies5");
        startArmies6 = (int) getParameterValue("startArmies6");
        randomTerritoryDeal = (boolean) getParameterValue("randomTerritoryDeal");
        territoriesPerArmy = (int) getParameterValue("territoriesPerArmy");
        minReinforcements = (int) getParameterValue("minReinforcements");
        maxAttackDice = (int) getParameterValue("maxAttackDice");
        maxDefendDice = (int) getParameterValue("maxDefendDice");
        allowBlitz = (boolean) getParameterValue("allowBlitz");
        defenderChoosesDice = (boolean) getParameterValue("defenderChoosesDice");
        nWildCards = (int) getParameterValue("nWildCards");
        tradeValues = Arrays.stream(((String) getParameterValue("tradeValues")).split(","))
                .mapToInt(v -> Integer.parseInt(v.trim())).toArray();
        tradeValueIncrement = (int) getParameterValue("tradeValueIncrement");
        territoryBonus = (int) getParameterValue("territoryBonus");
        handLimit = (int) getParameterValue("handLimit");
        eliminationTradeLimit = (int) getParameterValue("eliminationTradeLimit");
        linearTradeValues = (boolean) getParameterValue("linearTradeValues");
        fortifyAlongPath = (boolean) getParameterValue("fortifyAlongPath");
        maxArmiesPerTerritory = (int) getParameterValue("maxArmiesPerTerritory");
        secretMission = (boolean) getParameterValue("secretMission");
        // the framework's round limit, which StandardForwardModel.endRound applies
        setMaxRounds((int) getParameterValue("maxRounds"));
    }

    /**
     * The armies each player counts out at the start of the game.
     */
    public int startingArmies(int nPlayers) {
        return switch (nPlayers) {
            case 3 -> startArmies3;
            case 4 -> startArmies4;
            case 5 -> startArmies5;
            case 6 -> startArmies6;
            default -> throw new IllegalArgumentException("Risk is for 3 to 6 players, not " + nPlayers);
        };
    }

    /**
     * The armies for the k-th set traded in the game (k from 1).
     */
    public int tradeValue(int k) {
        // the expert rule: one more for each set
        if (linearTradeValues)
            return tradeValues[0] + k - 1;
        if (k <= tradeValues.length)
            return tradeValues[k - 1];
        // after the listed values, tradeValueIncrement more for each further set
        return tradeValues[tradeValues.length - 1] + (k - tradeValues.length) * tradeValueIncrement;
    }

    public RiskMap getMap() {
        return map;
    }

    @Override
    protected RiskParameters _copy() {
        return new RiskParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof RiskParameters;
    }

    @Override
    public RiskParameters instantiate() {
        return this;
    }
}

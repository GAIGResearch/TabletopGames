package games.wonders7;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.wonders7.cards.Wonder7Board;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Wonders7GameParameters extends TunableParameters {
    public List<Wonder7Board.Wonder> wonders = new ArrayList<>();
    public int nWonderCardsPerPlayer = 7;
    public int nCostNeighbourResource = 2;
    public int nCostDiscountedResource = 1;
    public int nCoinsDiscard = 3;
    public int startingCoins = 3;

    // Card Parameters
    public int rawMaterialLow = 1;
    public int rawMaterialHigh = 2;
    public int manufacturedMaterial = 1;

    public int victoryLow = 3;
    public int victoryMed = 4;
    public int victoryHigh = 5;
    public int victoryVeryHigh = 6;
    public int victoryPantheon = 7;
    public int victoryPalace = 8;

    public int tavernMoney = 5;
    public int wildcardProduction = 1;
    public int commercialMultiplierLow = 1;
    public int commercialMultiplierMed = 2;
    public int commercialMultiplierHigh = 3;

    public int militaryLow = 1;
    public int militaryMed = 2;
    public int militaryHigh = 3;

    public int scienceCompass = 1;
    public int scienceTablet = 1;
    public int scienceCog = 1;

    public int guildMultiplierLow = 1;
    public int guildMultiplierMed = 2;
    public int builderMultiplier = 1;
    public int decoratorVictoryPoints = 7;

    public static final String[] DEFAULT_WONDERS = new String[]{
            Wonder7Board.Wonder.TheColossusOfRhodes.toString(),
            Wonder7Board.Wonder.TheLighthouseOfAlexandria.toString(),
            Wonder7Board.Wonder.TheTempleOfArtemisInEphesus.toString(),
            Wonder7Board.Wonder.TheHangingGardensOfBabylon.toString(),
            Wonder7Board.Wonder.TheStatueOfZeusInOlympia.toString(),
            Wonder7Board.Wonder.TheMausoleumOfHalicarnassus.toString(),
            Wonder7Board.Wonder.ThePyramidsOfGiza.toString()
    };

    // if either wonder or card distribution seeds are set to something other than -1,
    // then this seed is fixed. The game random seed will be used in all cases where these are -1 (the default)
    public int wonderShuffleSeed = -1;
    public int cardShuffleSeed = -1;

    public Wonders7GameParameters() {
        addTunableParameter("nWonderCardsPerPlayer", 7, Arrays.asList(3, 5, 7, 9, 11));
        addTunableParameter("nCostNeighbourResource", 2, Arrays.asList(0, 1, 2, 3, 4, 5));
        addTunableParameter("nCostDiscountedResource", 1, Arrays.asList(0, 1, 2, 3, 4, 5));
        addTunableParameter("nCoinsDiscard", 3, Arrays.asList(0, 1, 2, 3, 4, 5));
        addTunableParameter("startingCoins", 3, Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
        addTunableParameter("wonderShuffleSeed", -1);
        addTunableParameter("cardShuffleSeed", -1);
        addStaticParameter("wonders", Arrays.asList(DEFAULT_WONDERS));

        // Parameters for cards
        addTunableParameter("rawMaterialLow", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("rawMaterialHigh", 2, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("manufacturedMaterial", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("victoryLow", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("victoryMed", 4, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("victoryHigh", 5, Arrays.asList(3, 4, 5, 6, 7));
        addTunableParameter("victoryVeryHigh", 6, Arrays.asList(3, 4, 5, 6, 7));
        addTunableParameter("victoryPantheon", 7, Arrays.asList(5, 6, 7, 8, 9));
        addTunableParameter("victoryPalace", 8, Arrays.asList(6, 7, 8, 9, 10));
        addTunableParameter("tavernMoney", 5, Arrays.asList(3, 4, 5, 6, 7));
        addTunableParameter("wildcardProduction", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("commercialMultiplierLow", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("commercialMultiplierMed", 2, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("commercialMultiplierHigh", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("militaryLow", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("militaryMed", 2, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("militaryHigh", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("scienceCompass", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("scienceTablet", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("scienceCog", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("guildMultiplierLow", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("guildMultiplierMed", 2, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("builderMultiplier", 1, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("decoratorVictoryPoints", 7, Arrays.asList(5, 6, 7, 8, 9));


        _reset();
    }

    @Override
    public void _reset() {
        nWonderCardsPerPlayer = (int) getParameterValue("nWonderCardsPerPlayer");
        nCostNeighbourResource = (int) getParameterValue("nCostNeighbourResource");
        nCostDiscountedResource = (int) getParameterValue("nCostDiscountedResource");
        nCoinsDiscard = (int) getParameterValue("nCoinsDiscard");
        startingCoins = (int) getParameterValue("startingCoins");
        wonderShuffleSeed = (int) getParameterValue("wonderShuffleSeed");
        cardShuffleSeed = (int) getParameterValue("cardShuffleSeed");

        wonders.clear();
        List<Object> wondersJSON = (List<Object>) getParameterValue("wonders");
        for (Object o: wondersJSON) {
            Wonder7Board.Wonder wonder = Wonder7Board.Wonder.valueOf((String) o);
            wonders.add(wonder);
        }

        rawMaterialLow = (int) getParameterValue("rawMaterialLow");
        rawMaterialHigh = (int) getParameterValue("rawMaterialHigh");
        manufacturedMaterial = (int) getParameterValue("manufacturedMaterial");
        victoryLow = (int) getParameterValue("victoryLow");
        victoryMed = (int) getParameterValue("victoryMed");
        victoryHigh = (int) getParameterValue("victoryHigh");
        victoryVeryHigh = (int) getParameterValue("victoryVeryHigh");
        victoryPantheon = (int) getParameterValue("victoryPantheon");
        victoryPalace = (int) getParameterValue("victoryPalace");
        tavernMoney = (int) getParameterValue("tavernMoney");
        wildcardProduction = (int) getParameterValue("wildcardProduction");
        commercialMultiplierLow = (int) getParameterValue("commercialMultiplierLow");
        commercialMultiplierMed = (int) getParameterValue("commercialMultiplierMed");
        commercialMultiplierHigh = (int) getParameterValue("commercialMultiplierHigh");
        militaryLow = (int) getParameterValue("militaryLow");
        militaryMed = (int) getParameterValue("militaryMed");
        militaryHigh = (int) getParameterValue("militaryHigh");
        scienceCompass = (int) getParameterValue("scienceCompass");
        scienceTablet = (int) getParameterValue("scienceTablet");
        scienceCog = (int) getParameterValue("scienceCog");
        guildMultiplierLow = (int) getParameterValue("guildMultiplierLow");
        guildMultiplierMed = (int) getParameterValue("guildMultiplierMed");
        builderMultiplier = (int) getParameterValue("builderMultiplier");
        decoratorVictoryPoints = (int) getParameterValue("decoratorVictoryPoints");

    }

    @Override
    protected AbstractParameters _copy() {
        return new Wonders7GameParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return (o instanceof Wonders7GameParameters);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Wonders7, new Wonders7ForwardModel(), new Wonders7GameState(this, GameType.Wonders7.getMinPlayers()));
    }
}
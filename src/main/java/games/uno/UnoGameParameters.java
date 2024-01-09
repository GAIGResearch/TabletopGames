package games.uno;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.Objects;


public class UnoGameParameters extends TunableParameters {
    public String dataPath = "data/uno/";

    // CLASSIC scoring only gives points to the winning player equal to the cards held by their opponents
    // INCREMENTAL scores the points held in ones own hand, and the winner is the lowest score at the end
    // CHALLENGE is the 'recommended' variant. It is as INCREMENTAL, but players are eliminated once
    // they reach 500 points
    public enum UnoScoring {
        CLASSIC, CHALLENGE, INCREMENTAL
    }

    public UnoScoring scoringMethod = UnoScoring.INCREMENTAL;
    public int nCardsPerPlayer = 7;
    public int nNumberCards = 10;
    public int nWildCards = 4;
    public int nSkipCards = 2;
    public int nReverseCards = 2;
    public int nDrawCards = 2;
    public int[] specialDrawCards = new int[]{2};  // DrawTwo card
    public int[] specialWildDrawCards = new int[]{0, 4};  // Wild, WildDrawFour card
    public static String[] colors = new String[]{
            "Red",
            "Blue",
            "Green",
            "Yellow"
    };

    public int nReversePoints = 20;
    public int nDraw2Points = 20;
    public int nSkipPoints = 20;
    public int nWildPoints = 50;
    public int nWildDrawPoints = 50;
    public int nWinPoints = 500;
    public int maxTurnsPerRound = 300;

    public UnoGameParameters() {
        addTunableParameter("nCardsPerPlayer", 7, Arrays.asList(5,7,10,15));
        addTunableParameter("nNumberCards", 10, Arrays.asList(5,10,15));
        addTunableParameter("nWildCards", 4, Arrays.asList(1,2,4,6,8,10));
        addTunableParameter("nSkipCards", 2, Arrays.asList(1,2,4,6,8,10));
        addTunableParameter("nReverseCards", 2, Arrays.asList(1,2,4,6,8,10));
        addTunableParameter("nDrawCards", 2, Arrays.asList(1,2,4,6,8,10));
        addTunableParameter("nReversePoints", 20, Arrays.asList(5,10,20,30,50));
        addTunableParameter("nDraw2Points", 20, Arrays.asList(5,10,20,30,50));
        addTunableParameter("nSkipPoints", 20, Arrays.asList(5,10,20,30,50));
        addTunableParameter("nWildPoints", 50, Arrays.asList(20,30,50,80,100));
        addTunableParameter("nWildDrawPoints", 50, Arrays.asList(20,30,50,80,100));
        addTunableParameter("nWinPoints", 500, Arrays.asList(50,100,250,500));
        addTunableParameter("scoringMethod", UnoScoring.INCREMENTAL, Arrays.asList(UnoScoring.values()));
        _reset();
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nNumberCards = (int) getParameterValue("nNumberCards");
        nWildCards = (int) getParameterValue("nWildCards");
        nSkipCards = (int) getParameterValue("nSkipCards");
        nReverseCards = (int) getParameterValue("nReverseCards");
        nDrawCards = (int) getParameterValue("nDrawCards");
        nReversePoints = (int) getParameterValue("nReversePoints");
        nDraw2Points = (int) getParameterValue("nDraw2Points");
        nSkipPoints = (int) getParameterValue("nSkipPoints");
        nWildPoints = (int) getParameterValue("nWildPoints");
        nWildDrawPoints = (int) getParameterValue("nWildDrawPoints");
        nWinPoints = (int) getParameterValue("nWinPoints");
        scoringMethod = (UnoScoring) getParameterValue("scoringMethod");
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        UnoGameParameters ugp = new UnoGameParameters();
        ugp.specialDrawCards = specialDrawCards.clone();
        ugp.specialWildDrawCards = specialWildDrawCards.clone();
        return ugp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnoGameParameters)) return false;
        if (!super.equals(o)) return false;
        UnoGameParameters that = (UnoGameParameters) o;
        return nCardsPerPlayer == that.nCardsPerPlayer &&
                nNumberCards == that.nNumberCards &&
                nWildCards == that.nWildCards &&
                nSkipCards == that.nSkipCards &&
                nReverseCards == that.nReverseCards &&
                nDrawCards == that.nDrawCards &&
                nReversePoints == that.nReversePoints &&
                nDraw2Points == that.nDraw2Points &&
                nSkipPoints == that.nSkipPoints &&
                nWildPoints == that.nWildPoints &&
                nWildDrawPoints == that.nWildDrawPoints &&
                nWinPoints == that.nWinPoints &&
                scoringMethod == that.scoringMethod &&
                Objects.equals(dataPath, that.dataPath) &&
                Arrays.equals(specialDrawCards, that.specialDrawCards) &&
                Arrays.equals(specialWildDrawCards, that.specialWildDrawCards) &&
                Arrays.equals(colors, that.colors);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, nCardsPerPlayer, nNumberCards, nWildCards, nSkipCards, nReverseCards, nDrawCards, nReversePoints, nDraw2Points, nSkipPoints, nWildPoints, nWildDrawPoints, nWinPoints, scoringMethod);
        result = 31 * result + Arrays.hashCode(specialDrawCards);
        result = 31 * result + Arrays.hashCode(specialWildDrawCards);
        result = 31 * result + Arrays.hashCode(colors);
        return result;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Uno, new UnoForwardModel(), new UnoGameState(this, GameType.Uno.getMinPlayers()));
    }
}

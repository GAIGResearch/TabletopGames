package games.hanabi;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;


import java.util.Arrays;
import java.util.Objects;

public class HanabiParameters extends TunableParameters {
    public int nNumberCards = 5;

    int hintCounter = 8;
    int failCounter = 3;
    int nHandCards = 5;
    int nCards1 = 3;
    int nCards2= 2;
    int nCards3 = 2;
    int nCards4 = 2;
    int nCards5 = 1;

    public String dataPath = "data/hanabi/";
    public HanabiParameters(){
        addTunableParameter("nHandCards", 5, Arrays.asList(3,5,7,10));
        addTunableParameter("hintCounter", 8, Arrays.asList(3,5,8,10));
        addTunableParameter("failCounter", 3, Arrays.asList(3,5,7,10));
        addTunableParameter("nCards1", 3, Arrays.asList(1,2,3,5,7,10));
        addTunableParameter("nCards2", 2, Arrays.asList(1,2,3,5,7,10));
        addTunableParameter("nCards3", 2, Arrays.asList(1,2,3,5,7,10));
        addTunableParameter("nCards4", 2, Arrays.asList(1,2,3,5,7,10));
        addTunableParameter("nCards5", 1, Arrays.asList(1,2,3,5,7,10));
    }

    @Override
    public void _reset() {
        nHandCards = (int) getParameterValue("nHandCards");
        hintCounter = (int) getParameterValue("hintCounter");
        failCounter = (int) getParameterValue("failCounter");
        nCards1 = (int) getParameterValue("nCards1");
        nCards2 = (int) getParameterValue("nCards2");
        nCards3 = (int) getParameterValue("nCards3");
        nCards4 = (int) getParameterValue("nCards4");
        nCards5 = (int) getParameterValue("nCards5");

    }

    @Override
    protected AbstractParameters _copy() {
        HanabiParameters copy = new HanabiParameters();
        copy.nNumberCards = nNumberCards;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HanabiParameters)) return false;
        HanabiParameters that = (HanabiParameters) o;

        return nNumberCards == that.nNumberCards &&
                hintCounter == that.hintCounter &&
                failCounter == that.failCounter &&
                nHandCards == that.nHandCards &&
                nCards1 == that.nCards1 &&
                nCards2 == that.nCards2 &&
                nCards3 == that.nCards3 &&
                nCards4 == that.nCards4 &&
                nCards5 == that.nCards5 &&
                Objects.equals(dataPath, that.dataPath);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath, nNumberCards, hintCounter, failCounter, nHandCards, nCards1, nCards2, nCards3, nCards4, nCards5);
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Hanabi, new HanabiForwardModel(), new HanabiGameState(this, GameType.Hanabi.getMinPlayers()));
    }
}


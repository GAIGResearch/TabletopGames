package games.gofish;

import evaluation.optimisation.TunableParameters;

public class GoFishParameters extends TunableParameters {

    // Game-specific parameters
    public int startingHandSize = 5;
    public boolean continueFishingOnSuccess = false;
    public boolean continueOnDrawingSameRank = true;

    public GoFishParameters() {
        setMaxRounds(500);
        addTunableParameter("startingHandSize", 5);
        addTunableParameter("continueFishingOnSuccess", false);
        addTunableParameter("continueOnDrawingSameRank", true);
    }
    @Override
    public void _reset() {
        startingHandSize = (int) getParameterValue("startingHandSize");
        continueFishingOnSuccess = (boolean) getParameterValue("continueFishingOnSuccess");
        continueOnDrawingSameRank = (boolean) getParameterValue("continueOnDrawingSameRank");
    }

    @Override
    protected GoFishParameters _copy() {
        GoFishParameters copy = new GoFishParameters();
        copy.startingHandSize = this.startingHandSize;
        return copy;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoFishParameters that)) return false;
        return this.startingHandSize == that.startingHandSize;
    }

    @Override
    public GoFishParameters instantiate() {
        return this;
    }


}

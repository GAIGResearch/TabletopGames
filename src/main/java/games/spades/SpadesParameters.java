package games.spades;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.Objects;

public class SpadesParameters extends TunableParameters<SpadesParameters> {
    
    public int winningScore = 500;
    public int sandbagsPerPenalty = 10;
    public int sandbagsRandPenalty = 100;
    public int nilBonusPoints = 100;
    public int nilPenaltyPoints = 100;
    public int blindNilBonusPoints = 200;
    public int blindNilPenaltyPoints = 200;
    public int pointsPerTrick = 10;
    
    public int maxBid = 13;
    
    public boolean allowBlindNil = false;
    public boolean allowNilOverbid = false; // Nil can be bid even if team has >= 500 points
    
    public SpadesParameters() {
        super();
        // Use maxRounds to end by score comparison; disable TIMEOUT endings
        setMaxRounds(30);
        setTimeoutRounds(-1);
        addTunableParameter("winningScore", 500);
        addTunableParameter("sandbagsPerPenalty", 10);
        addTunableParameter("sandbagsRandPenalty", 100);
        addTunableParameter("nilBonusPoints", 100);
        addTunableParameter("nilPenaltyPoints", 200);
        addTunableParameter("blindNilBonusPoints", 200);
        addTunableParameter("blindNilPenaltyPoints", 200);
        addTunableParameter("maxBid", 13);
        addTunableParameter("allowBlindNil", false);
        addTunableParameter("allowNilOverbid", false);
        addTunableParameter("pointsPerTrick", 10);
    }

    @Override
    public void _reset() {
        winningScore = (int) getParameterValue("winningScore");
        sandbagsPerPenalty = (int) getParameterValue("sandbagsPerPenalty");
        sandbagsRandPenalty = (int) getParameterValue("sandbagsRandPenalty");
        nilBonusPoints = (int) getParameterValue("nilBonusPoints");
        nilPenaltyPoints = (int) getParameterValue("nilPenaltyPoints");
        blindNilBonusPoints = (int) getParameterValue("blindNilBonusPoints");
        blindNilPenaltyPoints = (int) getParameterValue("blindNilPenaltyPoints");
        maxBid = (int) getParameterValue("maxBid");
        allowBlindNil = (boolean) getParameterValue("allowBlindNil");
        allowNilOverbid = (boolean) getParameterValue("allowNilOverbid");
        pointsPerTrick = (int) getParameterValue("pointsPerTrick");
    }
    
    public SpadesParameters(long seed) {
        super();
        setRandomSeed(seed);
        setMaxRounds(30);
        setTimeoutRounds(-1);
    }
    
    @Override
    protected AbstractParameters _copy() {
        return new SpadesParameters();
    }
    
    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        return o instanceof SpadesParameters;
    }

    @Override
    public SpadesParameters instantiate() {
        return this;
    }


}
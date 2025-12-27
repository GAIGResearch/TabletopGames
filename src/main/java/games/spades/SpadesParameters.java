package games.spades;

import core.AbstractParameters;

import java.util.Objects;

public class SpadesParameters extends AbstractParameters {
    
    public final int winningScore = 500;
    public final int sandbagsPerPenalty = 10;
    public final int sandbagsRandPenalty = 100;
    public final int nilBonusPoints = 100;
    public final int nilPenaltyPoints = 100;
    public final int blindNilBonusPoints = 200;
    public final int blindNilPenaltyPoints = 200;
    
    public final int maxBid = 13;
    
    public boolean allowBlindNil = false;
    public boolean allowNilOverbid = false; // Nil can be bid even if team has >= 500 points
    
    public SpadesParameters() {
        super();
        // Use maxRounds to end by score comparison; disable TIMEOUT endings
        setMaxRounds(30);
        setTimeoutRounds(-1);
    }
    
    public SpadesParameters(long seed) {
        super();
        setRandomSeed(seed);
        setMaxRounds(30);
        setTimeoutRounds(-1);
    }
    
    @Override
    protected AbstractParameters _copy() {
        SpadesParameters copy = new SpadesParameters(getRandomSeed());
        copy.allowBlindNil = this.allowBlindNil;
        copy.allowNilOverbid = this.allowNilOverbid;
        return copy;
    }
    
    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpadesParameters)) return false;
        if (!super.equals(o)) return false;
        
        SpadesParameters that = (SpadesParameters) o;
        return allowBlindNil == that.allowBlindNil &&
               allowNilOverbid == that.allowNilOverbid;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), allowBlindNil, allowNilOverbid);
    }
} 
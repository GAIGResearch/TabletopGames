package games.dicemonastery;

import evaluation.TunableParameters;

import java.util.Arrays;
import java.util.Objects;

public class DiceMonasteryParams extends TunableParameters {
    public DiceMonasteryParams(long seed) {
        super(seed);
        addTunableParameter("YEARS", 4);
        addTunableParameter("COST_PER_TREASURE_VP", 4);
    }

    public int YEARS = 4;
    public int[] BONUS_TOKENS_PER_PLAYER = {0, 0, 1, 2, 2};
    public int COST_PER_TREASURE_VP = 4;

    @Override
    public void _reset() {
        YEARS = (int) getParameterValue("YEARS");
        COST_PER_TREASURE_VP = (int) getParameterValue("COST_PER_TREASURE_VP");
    }

    @Override
    public DiceMonasteryParams instantiate() {
        return this;
    }

    @Override
    protected DiceMonasteryParams _copy() {
        DiceMonasteryParams retValue = new DiceMonasteryParams(System.currentTimeMillis());
        retValue.YEARS = YEARS;
        retValue.BONUS_TOKENS_PER_PLAYER = BONUS_TOKENS_PER_PLAYER.clone();
        retValue.COST_PER_TREASURE_VP = COST_PER_TREASURE_VP;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryParams) {
            DiceMonasteryParams other = (DiceMonasteryParams) o;
            return other.YEARS == YEARS && other.COST_PER_TREASURE_VP == COST_PER_TREASURE_VP
                    && Arrays.equals(other.BONUS_TOKENS_PER_PLAYER, BONUS_TOKENS_PER_PLAYER);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(YEARS, COST_PER_TREASURE_VP) + 71 * Arrays.hashCode(BONUS_TOKENS_PER_PLAYER);
    }


}

package games.dicemonastery;

import evaluation.TunableParameters;

import java.util.Arrays;
import java.util.Objects;

public class DiceMonasteryParams extends TunableParameters {
    public DiceMonasteryParams(long seed) {
        super(seed);
        addTunableParameter("YEARS", 4);
    }

    public int YEARS = 4;
    public int[] BONUS_TOKENS_PER_PLAYER = {0, 0, 1, 2, 2};

    @Override
    public void _reset() {
        YEARS = (int) getParameterValue("YEARS");
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
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryParams) {
            DiceMonasteryParams other = (DiceMonasteryParams) o;
            return other.YEARS == YEARS
                    && Arrays.equals(other.BONUS_TOKENS_PER_PLAYER, BONUS_TOKENS_PER_PLAYER);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(YEARS) + 71 * Arrays.hashCode(BONUS_TOKENS_PER_PLAYER);
    }


}

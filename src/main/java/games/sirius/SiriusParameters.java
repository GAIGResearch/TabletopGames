package games.sirius;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.Objects;

public class SiriusParameters extends TunableParameters {

    public int hyperAmmonia = 3;
    public int superAmmonia = 5;
    public int ammonia = 22;
    public int cardsPerEmptyMoon = 2;
    public int cardsPerNonEmptyMoon = 1;
    public int[] ammoniaTrack = new int[] {
            0,
            0, 0, 0, 0, 2,
            0, 0, 0, 0, 3,
            0, 0, 0, 0, 4,
            0, 0, 0, 0, 5,
            0, 0, 0, 0, 6
    };
    public int[] contrabandTrack = new int[] {
            0,
            0, 0, 0, 0, 0, 2,
            0, 0, 0, 0, 0, 3,
            0, 0, 0, 0, 0, 4,
            0, 0, 0, 0, 0, 5,
            0, 0, 0, 0, 0, 6
    };

    public SiriusParameters() {
        super();
        addTunableParameter("ammonia", 22);
        addTunableParameter("superAmmonia", 5);
        addTunableParameter("hyperAmmonia", 3);
    }

    @Override
    public void _reset() {
        ammonia = (int) getParameterValue("ammonia");
        superAmmonia = (int) getParameterValue("superAmmonia");
        hyperAmmonia = (int) getParameterValue("hyperAmmonia");
    }

    @Override
    protected AbstractParameters _copy() {
        return this; // TODO
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SiriusParameters) {
            SiriusParameters other = (SiriusParameters) o;
            return ammonia == other.ammonia && superAmmonia == other.superAmmonia && hyperAmmonia == other.hyperAmmonia;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ammonia, superAmmonia, hyperAmmonia);
    }

    @Override
    public Object instantiate() {
        return this;
    }

}

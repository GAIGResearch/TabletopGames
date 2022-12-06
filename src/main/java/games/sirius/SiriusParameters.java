package games.sirius;

import core.AbstractParameters;
import evaluation.TunableParameters;

import java.util.Objects;

public class SiriusParameters extends TunableParameters {

    public int hyperAmmonia = 3;
    public int superAmmonia = 5;
    public int ammonia = 22;
    public int brokenContraband = 9;
    public int contraband = 9;
    public int glowingContraband = 9;
    public int cardsPerEmptyMoon = 2;
    public int cardsPerNonEmptyMoon = 1;
    public int[] ammoniaTrack = new int[] {
            0, 0, 0, 0, 2,
            0, 0, 0, 0, 3,
            0, 0, 0, 0, 4,
            0, 0, 0, 0, 5,
            0, 0, 0, 0, 6
    };
    public int[] contrabandTrack = new int[] {
            0, 0, 0, 0, 2, 0,
            0, 0, 0, 0, 0, 0,
            3, 0, 0, 0, 0, 0,
            0, 0, 4, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            5, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            6
    };

    public SiriusParameters() {
        super();
        addTunableParameter("ammonia", 22);
        addTunableParameter("superAmmonia", 5);
        addTunableParameter("hyperAmmonia", 3);
        addTunableParameter("contraband", 11);
        addTunableParameter("brokenContraband", 11);
        addTunableParameter("glowingContraband", 11);
    }

    @Override
    public void _reset() {
        ammonia = (int) getParameterValue("ammonia");
        superAmmonia = (int) getParameterValue("superAmmonia");
        hyperAmmonia = (int) getParameterValue("hyperAmmonia");
        contraband = (int) getParameterValue("contraband");
        brokenContraband = (int) getParameterValue("brokenContraband");
        glowingContraband = (int) getParameterValue("glowingContraband");
    }

    @Override
    protected AbstractParameters _copy() {
        return this; // TODO
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof SiriusParameters) {
            SiriusParameters other = (SiriusParameters) o;
            return ammonia == other.ammonia && superAmmonia == other.superAmmonia && hyperAmmonia == other.hyperAmmonia &&
                    contraband == other.contraband && brokenContraband == other.brokenContraband && other.glowingContraband == glowingContraband &&
                    other.cardsPerEmptyMoon == cardsPerEmptyMoon && other.cardsPerNonEmptyMoon == cardsPerNonEmptyMoon;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ammonia, superAmmonia, hyperAmmonia, contraband, brokenContraband, glowingContraband,
                cardsPerEmptyMoon, cardsPerNonEmptyMoon);
    }

    @Override
    public Object instantiate() {
        return this;
    }

}

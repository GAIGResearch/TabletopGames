package games.president;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Rules of President, as described at https://www.pagat.com/climbing/president.html: deals continue until a player
 * reaches 11 points. data/president/President_Valet.json gives the RECYCLE code at
 * https://mgoadric.github.io/valet/post/president.html, a single deal.
 */
public class PresidentParameters extends TunableParameters<PresidentParameters> {

    // points for going out first (the President) and second (the Vice-President); everyone else scores 0
    public int presidentPoints = 2;
    public int vicePresidentPoints = 1;
    // 1 = a single deal (RECYCLE). Otherwise deals continue until a player's score reaches this (pagat: 11).
    public int targetScore = 11;
    // the number of cards the Scum gives the President, and the President gives back, before each later deal
    public int exchangeCards = 1;

    public PresidentParameters() {
        super();
        addTunableParameter("presidentPoints", 2);
        addTunableParameter("vicePresidentPoints", 1);
        // a safety limit on the number of deals when playing to a target score
        setMaxRounds(100);
        addTunableParameter("targetScore", 11, Arrays.asList(1, 5, 11));
        addTunableParameter("exchangeCards", 1, Arrays.asList(0, 1, 2));
    }

    @Override
    public void _reset() {
        presidentPoints = (int) getParameterValue("presidentPoints");
        vicePresidentPoints = (int) getParameterValue("vicePresidentPoints");
        targetScore = (int) getParameterValue("targetScore");
        exchangeCards = (int) getParameterValue("exchangeCards");
    }

    @Override
    protected AbstractParameters _copy() {
        return new PresidentParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof PresidentParameters;
    }

    @Override
    public PresidentParameters instantiate() {
        return this;
    }
}

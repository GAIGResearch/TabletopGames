package games.skitgubbe;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Rules of Skitgubbe. The defaults follow the RECYCLE code at https://mgoadric.github.io/valet/post/skittgube.html,
 * with the variants of https://www.pagat.com/beating/skitgubbe.html as options.
 */
public class SkitgubbeParameters extends TunableParameters<SkitgubbeParameters> {

    // the number of cards dealt to each player, and the hand size kept up by drawing in phase one
    public int handSize = 3;
    // a safeguard against an endless phase two, as cards may be picked up voluntarily: the game ends after this many
    // phase-two actions
    public int maxPhaseTwoActions = 1000;
    // false (RECYCLE): after a completed phase-two trick the next player leads. true (pagat): the player who
    // completed it leads, unless they are out of cards
    public boolean completerLeads = false;
    // false (RECYCLE): players on equal scores share a place. true: they are ranked by the order they went out
    public boolean exitOrderTiebreak = false;

    public SkitgubbeParameters() {
        super();
        addTunableParameter("handSize", 3, Arrays.asList(2, 3, 4));
        addTunableParameter("maxPhaseTwoActions", 1000);
        addTunableParameter("completerLeads", false, Arrays.asList(false, true));
        addTunableParameter("exitOrderTiebreak", false, Arrays.asList(false, true));
        _reset();
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        maxPhaseTwoActions = (int) getParameterValue("maxPhaseTwoActions");
        completerLeads = (boolean) getParameterValue("completerLeads");
        exitOrderTiebreak = (boolean) getParameterValue("exitOrderTiebreak");
    }

    @Override
    protected AbstractParameters _copy() {
        return new SkitgubbeParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof SkitgubbeParameters;
    }

    @Override
    public SkitgubbeParameters instantiate() {
        return this;
    }
}

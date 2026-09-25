package games.rummy;

import core.AbstractParameters;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Rules of Rummy. The defaults follow the RECYCLE code at https://mgoadric.github.io/valet/post/rummy.html (Block
 * Rummy, ending when the draw deck is empty), with the deal sizes for more than two players from
 * https://www.pagat.com/rummy/rummy.html.
 */
public class RummyParameters extends TunableParameters<RummyParameters> {

    // the cards dealt to each player, by the number of players
    public int cardsFor2Players = 10;
    public int cardsFor3To4Players = 7;
    public int cardsFor5To6Players = 6;
    // a deal ends after this many turns as if the draw deck were empty, since players could otherwise take the
    // discard back and forth for ever
    public int maxTurnsPerDeal = 200;
    // 0 = a single deal (RECYCLE). Otherwise deals continue until a player's score reaches this (pagat).
    public int targetScore = 0;

    public RummyParameters() {
        super();
        addTunableParameter("cardsFor2Players", 10);
        addTunableParameter("cardsFor3To4Players", 7);
        addTunableParameter("cardsFor5To6Players", 6);
        addTunableParameter("maxTurnsPerDeal", 200);
        addTunableParameter("targetScore", 0, Arrays.asList(0, 50, 100));
        // a safety limit on the number of deals when playing to a target score
        setMaxRounds(100);
    }

    public int handSize(int nPlayers) {
        if (nPlayers <= 2) return cardsFor2Players;
        if (nPlayers <= 4) return cardsFor3To4Players;
        return cardsFor5To6Players;
    }

    @Override
    public void _reset() {
        cardsFor2Players = (int) getParameterValue("cardsFor2Players");
        cardsFor3To4Players = (int) getParameterValue("cardsFor3To4Players");
        cardsFor5To6Players = (int) getParameterValue("cardsFor5To6Players");
        maxTurnsPerDeal = (int) getParameterValue("maxTurnsPerDeal");
        targetScore = (int) getParameterValue("targetScore");
    }

    @Override
    protected AbstractParameters _copy() {
        return new RummyParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof RummyParameters;
    }

    @Override
    public RummyParameters instantiate() {
        return this;
    }
}

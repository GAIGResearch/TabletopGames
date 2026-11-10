package games.perudo;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Perudo (the Asmodée rules, as on Board Game Arena: https://en.doc.boardgamearena.com/Gamehelpdudo).
 */
public class PerudoParameters extends TunableParameters<PerudoParameters> {

    // The face that shows a Paco (the bird on the "1" face), wild in a normal round
    public static final int PACO = 1;

    // Dice each player starts with
    public int nDicePerPlayer = 5;
    // Faces on each die, numbered 1 (Paco) to nFaces
    public int nFaces = 6;
    // Asmodée: the round after a player first drops to one die is a Palifico round (Pacos not wild, face fixed)
    public boolean palifico = false;
    // Calza: the player to act may call the bid exactly right, gaining a die if so and losing one if not
    public boolean calza = false;
    // Limits on the bids offered, to keep the action space small. On each face a bid may be for at most maxRaise
    // more dice than the lowest legal bid on that face, or for up to maxBidPercent of the dice on the table if that
    // is more; and for no fewer than minBidPercent of the dice on the table. In a normal round a Paco counts as two
    // dice for these limits. minBidPercent 0, with maxRaise at least the dice on the table, offers every legal bid.
    public int maxRaise = 2;
    public int minBidPercent = 25;
    public int maxBidPercent = 50;

    public PerudoParameters() {
        addTunableParameter("nDicePerPlayer", 5, Arrays.asList(3, 4, 5, 6));
        addTunableParameter("nFaces", 6);
        addTunableParameter("palifico", false, Arrays.asList(false, true));
        addTunableParameter("calza", false, Arrays.asList(false, true));
        addTunableParameter("maxRaise", 2, Arrays.asList(1, 2, 3, 4));
        addTunableParameter("minBidPercent", 25, Arrays.asList(0, 15, 25, 33));
        addTunableParameter("maxBidPercent", 50, Arrays.asList(33, 50, 67, 100));
        _reset();
    }

    @Override
    public void _reset() {
        nDicePerPlayer = (int) getParameterValue("nDicePerPlayer");
        nFaces = (int) getParameterValue("nFaces");
        palifico = (boolean) getParameterValue("palifico");
        calza = (boolean) getParameterValue("calza");
        maxRaise = (int) getParameterValue("maxRaise");
        minBidPercent = (int) getParameterValue("minBidPercent");
        maxBidPercent = (int) getParameterValue("maxBidPercent");
    }

    @Override
    protected PerudoParameters _copy() {
        return new PerudoParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof PerudoParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public PerudoParameters instantiate() {
        return this;
    }
}

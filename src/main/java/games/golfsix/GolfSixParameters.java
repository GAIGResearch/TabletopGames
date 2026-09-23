package games.golfsix;

import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Six-card Golf (as described at https://www.pagat.com/draw/golf.html).
 */
public class GolfSixParameters extends TunableParameters<GolfSixParameters> {

    // The grid is COLUMNS columns of two cards: positions 0 to COLUMNS-1 are the top row, and position p + COLUMNS
    // is below position p
    public static final int COLUMNS = 3;
    public static final int GRID_SIZE = 2 * COLUMNS;

    // The number of deals in a game; scores are added up over all of them
    public int nDeals = 1;
    // Whether, once a player's last card is face-up, each other player has one more turn before the deal is scored
    public boolean finalTurns = false;
    // The cards each player turns face-up before play starts
    public int initialFaceUp = 2;
    // A deal is scored once every player has had this many turns in it, even if nobody has all their cards face-up
    public int maxTurnsPerPlayer = 50;
    public int aceValue = 1;
    public int twoValue = -2;
    public int courtValue = 10;   // Jacks and Queens
    public int kingValue = 0;

    public GolfSixParameters() {
        addTunableParameter("nDeals", 1, Arrays.asList(1, 3, 9, 18));
        addTunableParameter("finalTurns", false, Arrays.asList(false, true));
        addTunableParameter("initialFaceUp", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("maxTurnsPerPlayer", 50);
        addTunableParameter("aceValue", 1);
        addTunableParameter("twoValue", -2);
        addTunableParameter("courtValue", 10);
        addTunableParameter("kingValue", 0);
    }

    @Override
    public void _reset() {
        nDeals = (int) getParameterValue("nDeals");
        finalTurns = (boolean) getParameterValue("finalTurns");
        initialFaceUp = (int) getParameterValue("initialFaceUp");
        maxTurnsPerPlayer = (int) getParameterValue("maxTurnsPerPlayer");
        aceValue = (int) getParameterValue("aceValue");
        twoValue = (int) getParameterValue("twoValue");
        courtValue = (int) getParameterValue("courtValue");
        kingValue = (int) getParameterValue("kingValue");
    }

    /**
     * The points a card scores when it is not in a column pair.
     */
    public int cardValue(FrenchCard card) {
        return switch (card.type) {
            case Ace -> aceValue;
            case Jack, Queen -> courtValue;
            case King -> kingValue;
            default -> card.number == 2 ? twoValue : card.number;
        };
    }

    @Override
    protected GolfSixParameters _copy() {
        return new GolfSixParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof GolfSixParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public GolfSixParameters instantiate() {
        return this;
    }
}

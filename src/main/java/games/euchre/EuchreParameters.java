package games.euchre;

import evaluation.optimisation.TunableParameters;
import games.tricktaking.ITrickTakingParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Euchre (North American rules, as described at https://www.pagat.com/euchre/euchre.html).
 */
public class EuchreParameters extends TunableParameters<EuchreParameters> implements ITrickTakingParameters {

    // The game ends after the deal in which a team reaches this score. At 1 the game is a single deal, since one team
    // scores in every deal; pagat plays to 10
    public int targetScore = 1;

    // Cards dealt to each player; with 4 players, 4 of the 24 cards are left in the kitty
    public int handSize = 5;

    // The lowest number in the deck: 9 to Ace in each suit
    public int lowestCard = 9;

    // Points to the makers for 3 or 4 tricks, for all 5 (a march), and for a march by a player going alone
    public int pointsMade = 1;
    public int pointsMarch = 2;
    public int pointsAloneMarch = 4;

    // Points to the defenders when the makers take fewer than 3 tricks (they are euchred)
    public int pointsEuchred = 2;

    // Whether the dealer takes the up-card and discards when their partner has called the up-card's suit alone,
    // though the dealer will sit out the play (RECYCLE's rule)
    public boolean sittingOutDealerPicksUp = true;

    // Whether players remember the suits others have failed to follow (used when redeterminising)
    public boolean rememberVoids = true;

    public EuchreParameters() {
        addTunableParameter("targetScore", 1, Arrays.asList(1, 5, 10));
        addTunableParameter("handSize", 5);
        addTunableParameter("lowestCard", 9);
        addTunableParameter("pointsMade", 1);
        addTunableParameter("pointsMarch", 2);
        addTunableParameter("pointsAloneMarch", 4);
        addTunableParameter("pointsEuchred", 2);
        addTunableParameter("sittingOutDealerPicksUp", true, List.of(false, true));
        addTunableParameter("rememberVoids", true, List.of(false, true));
    }

    @Override
    public void _reset() {
        targetScore = (int) getParameterValue("targetScore");
        handSize = (int) getParameterValue("handSize");
        lowestCard = (int) getParameterValue("lowestCard");
        pointsMade = (int) getParameterValue("pointsMade");
        pointsMarch = (int) getParameterValue("pointsMarch");
        pointsAloneMarch = (int) getParameterValue("pointsAloneMarch");
        pointsEuchred = (int) getParameterValue("pointsEuchred");
        sittingOutDealerPicksUp = (boolean) getParameterValue("sittingOutDealerPicksUp");
        rememberVoids = (boolean) getParameterValue("rememberVoids");
    }

    @Override
    public boolean rememberVoids() {
        return rememberVoids;
    }

    @Override
    protected EuchreParameters _copy() {
        return new EuchreParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof EuchreParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public EuchreParameters instantiate() {
        return this;
    }
}

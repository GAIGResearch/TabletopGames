package games.crazyeights;

import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Crazy Eights (Basic Game, as described at https://www.pagat.com/eights/crazy8s.html).
 * All rule constants should be read from here rather than hard-coded in the state or forward model.
 */
public class CZEParameters extends TunableParameters<CZEParameters> {

    // Cards dealt to each player (Pagat: five each, or seven each with only two players)
    public int nCardsPerPlayer = 5;
    public int nCardsPerPlayerTwoPlayers = 7;

    // Penalty points for cards left in hand at the end of the hand
    public int eightPenalty = 50;
    public int pictureCardPenalty = 10;  // Jack, Queen, King
    public int acePenalty = 1;           // FrenchCard numbers Aces as 14, so needs special-casing
    // Spot cards (2-10) score their face value

    // If the starter card turned up is an Eight, this is the suit to match (as in the RECYCLE reference code),
    // unless dealerNominatesStarterSuit is true.
    public FrenchCard.Suite starterEightSuit = FrenchCard.Suite.Hearts;

    // Pagat: if the starter card is an Eight, the dealer (the last player) nominates the suit before play begins
    public boolean dealerNominatesStarterSuit = false;

    public CZEParameters() {
        addTunableParameter("nCardsPerPlayer", 5, Arrays.asList(3, 4, 5, 6, 7));
        addTunableParameter("nCardsPerPlayerTwoPlayers", 7, Arrays.asList(5, 6, 7, 8, 9, 10));
        addTunableParameter("eightPenalty", 50, Arrays.asList(20, 30, 50));
        addTunableParameter("pictureCardPenalty", 10);
        addTunableParameter("acePenalty", 1);
        addTunableParameter("starterEightSuit", FrenchCard.Suite.Hearts, List.of(FrenchCard.Suite.values()));
        addTunableParameter("dealerNominatesStarterSuit", false, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        nCardsPerPlayer = (int) getParameterValue("nCardsPerPlayer");
        nCardsPerPlayerTwoPlayers = (int) getParameterValue("nCardsPerPlayerTwoPlayers");
        eightPenalty = (int) getParameterValue("eightPenalty");
        pictureCardPenalty = (int) getParameterValue("pictureCardPenalty");
        acePenalty = (int) getParameterValue("acePenalty");
        starterEightSuit = (FrenchCard.Suite) getParameterValue("starterEightSuit");
        dealerNominatesStarterSuit = (boolean) getParameterValue("dealerNominatesStarterSuit");
    }

    public int cardsToDeal(int nPlayers) {
        return nPlayers == 2 ? nCardsPerPlayerTwoPlayers : nCardsPerPlayer;
    }

    @Override
    protected CZEParameters _copy() {
        return new CZEParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof CZEParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public CZEParameters instantiate() {
        return this;
    }
}

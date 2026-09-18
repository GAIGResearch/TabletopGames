package games.crazyeights;

import core.AbstractParameters;
import core.components.FrenchCard;

import java.util.Objects;

/**
 * Parameters for Crazy Eights (Basic Game, as described at https://www.pagat.com/eights/crazy8s.html).
 * All rule constants should be read from here rather than hard-coded in the state or forward model.
 */
public class CZEParameters extends AbstractParameters {

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

    public int cardsToDeal(int nPlayers) {
        return nPlayers == 2 ? nCardsPerPlayerTwoPlayers : nCardsPerPlayer;
    }

    @Override
    protected AbstractParameters _copy() {
        CZEParameters copy = new CZEParameters();
        copy.nCardsPerPlayer = nCardsPerPlayer;
        copy.nCardsPerPlayerTwoPlayers = nCardsPerPlayerTwoPlayers;
        copy.eightPenalty = eightPenalty;
        copy.pictureCardPenalty = pictureCardPenalty;
        copy.acePenalty = acePenalty;
        copy.starterEightSuit = starterEightSuit;
        copy.dealerNominatesStarterSuit = dealerNominatesStarterSuit;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CZEParameters that)) return false;
        return nCardsPerPlayer == that.nCardsPerPlayer &&
                nCardsPerPlayerTwoPlayers == that.nCardsPerPlayerTwoPlayers &&
                eightPenalty == that.eightPenalty &&
                pictureCardPenalty == that.pictureCardPenalty &&
                acePenalty == that.acePenalty &&
                starterEightSuit == that.starterEightSuit &&
                dealerNominatesStarterSuit == that.dealerNominatesStarterSuit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nCardsPerPlayer, nCardsPerPlayerTwoPlayers,
                eightPenalty, pictureCardPenalty, acePenalty, starterEightSuit, dealerNominatesStarterSuit);
    }
}

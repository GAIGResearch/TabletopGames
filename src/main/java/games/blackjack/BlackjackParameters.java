package games.blackjack;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Blackjack (https://www.pagat.com/banking/blackjack.html). The defaults follow pagat, over a session
 * of 10 hands with 100 chips each. data/blackjack/Blackjack_Valet.json gives the Valet variant: 10 chips, even bets
 * from 2 to 10, any winning 21 paid 2:1, a single hand, no doubling and no splitting.
 */
public class BlackjackParameters extends TunableParameters<BlackjackParameters> {

    // Chips each player starts with
    public int startingChips = 100;

    // Bets must be even (so that insurance, half the bet, and a 3:2 natural payout are whole chips).
    // A player may bet any even amount from minBet to the smaller of maxBet and their chips.
    public int minBet = 2;
    public int maxBet = 20;

    // A winning 21 is paid payout21 : 1 (floor(bet x payout21) won) instead of 1:1. With payout21NaturalOnly only a
    // natural gets it, paid as soon as the dealer is known not to have Blackjack (Pagat, the default);
    // otherwise any winning 21 does, at settlement (RECYCLE).
    public double payout21 = 1.5;
    public boolean payout21NaturalOnly = true;

    // Hands played in a game. The game ends sooner if nobody has the chips for the minimum bet
    public int nHands = 10;

    // The dealer draws on a soft 17 (otherwise stands on every 17)
    public boolean dealerHitsSoft17 = false;

    // A player may double down on their first two cards (not a natural): double the bet, take one card, and stop
    public boolean doubleDown = true;

    // A player may split a pair of the same rank, up to maxHandsAfterSplit hands in all. Split Aces take one card
    // each; there is no doubling after a split, and a 21 after a split is not a natural
    public boolean splitting = true;
    public int maxHandsAfterSplit = 4;

    public BlackjackParameters() {
        addTunableParameter("startingChips", 100, Arrays.asList(10, 20, 50, 100));
        addTunableParameter("minBet", 2);
        addTunableParameter("maxBet", 20, Arrays.asList(10, 20, 50));
        addTunableParameter("payout21", 1.5, Arrays.asList(1.0, 1.2, 1.5, 2.0));
        addTunableParameter("payout21NaturalOnly", true, List.of(false, true));
        addTunableParameter("nHands", 10, Arrays.asList(1, 3, 5, 10));
        addTunableParameter("dealerHitsSoft17", false, List.of(false, true));
        addTunableParameter("doubleDown", true, List.of(false, true));
        addTunableParameter("splitting", true, List.of(false, true));
        addTunableParameter("maxHandsAfterSplit", 4, Arrays.asList(2, 3, 4));
    }

    /**
     * The chips won by a winning 21 on a bet of this size (payout21 : 1, rounded down), not counting the bet itself.
     */
    public int winningsOn21(int bet) {
        return (int) Math.floor(bet * payout21);
    }

    /**
     * An upper bound on the chips a player can hold at the end of the game: the starting chips plus, for each hand,
     * the most one hand can win. That is the largest stake (maxBet on every split hand, or doubled) paid at the better
     * of 1:1 and payout21, plus insurance on half of maxBet paid 2:1.
     */
    public int maxChips() {
        int maxStake = maxBet * Math.max(splitting ? maxHandsAfterSplit : 1, doubleDown ? 2 : 1);
        int maxWinPerHand = (int) Math.ceil(maxStake * Math.max(1.0, payout21)) + maxBet;
        return startingChips + nHands * maxWinPerHand;
    }

    @Override
    public void _reset() {
        startingChips = (int) getParameterValue("startingChips");
        minBet = (int) getParameterValue("minBet");
        maxBet = (int) getParameterValue("maxBet");
        payout21 = (double) getParameterValue("payout21");
        payout21NaturalOnly = (boolean) getParameterValue("payout21NaturalOnly");
        nHands = (int) getParameterValue("nHands");
        dealerHitsSoft17 = (boolean) getParameterValue("dealerHitsSoft17");
        doubleDown = (boolean) getParameterValue("doubleDown");
        splitting = (boolean) getParameterValue("splitting");
        maxHandsAfterSplit = (int) getParameterValue("maxHandsAfterSplit");
    }

    @Override
    protected BlackjackParameters _copy() {
        return new BlackjackParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof BlackjackParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public BlackjackParameters instantiate() {
        return this;
    }
}

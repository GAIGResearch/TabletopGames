package games.leducpoker;

import core.AbstractParameters;
import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static core.components.FrenchCard.FrenchCardType.*;

/**
 * Parameters for Leduc Poker. The defaults are standard Leduc Hold'em (Southey et al. 2005);
 * highCardUsesBoard is the high-card rule of the Valet RECYCLE code.
 */
public class LeducPokerParameters extends TunableParameters<LeducPokerParameters> {

    /** The ranks in the deck, lowest first. */
    public static final List<FrenchCard.FrenchCardType> RANKS = List.of(Jack, Queen, King);
    /** The suits in the deck: one card of each rank per suit. */
    public static final List<FrenchCard.Suite> SUITS = List.of(FrenchCard.Suite.Spades, FrenchCard.Suite.Hearts);

    public int nHands = 1;
    public int ante = 1;
    public int firstRoundRaise = 2;
    public int secondRoundRaise = 4;
    public int maxRaisesPerRound = 2;
    public boolean highCardUsesBoard = false;

    public LeducPokerParameters() {
        addTunableParameter("nHands", 1, Arrays.asList(1, 5, 10, 20, 50));
        addTunableParameter("ante", 1, Arrays.asList(1, 2));
        addTunableParameter("firstRoundRaise", 2, Arrays.asList(1, 2, 4));
        addTunableParameter("secondRoundRaise", 4, Arrays.asList(2, 4, 8));
        addTunableParameter("maxRaisesPerRound", 2, Arrays.asList(1, 2, 3, 4));
        addTunableParameter("highCardUsesBoard", false, Arrays.asList(false, true));
    }

    @Override
    public void _reset() {
        nHands = (int) getParameterValue("nHands");
        ante = (int) getParameterValue("ante");
        firstRoundRaise = (int) getParameterValue("firstRoundRaise");
        secondRoundRaise = (int) getParameterValue("secondRoundRaise");
        maxRaisesPerRound = (int) getParameterValue("maxRaisesPerRound");
        highCardUsesBoard = (boolean) getParameterValue("highCardUsesBoard");
    }

    /**
     * The raise amount in the given betting round (0 for the first, 1 for the second).
     */
    public int raiseAmount(int bettingRound) {
        return bettingRound == 0 ? firstRoundRaise : secondRoundRaise;
    }

    /**
     * The most one player can put in the pot in one hand.
     */
    public int maxContribution() {
        // the ante, then every raise allowed in both betting rounds
        return ante + maxRaisesPerRound * (firstRoundRaise + secondRoundRaise);
    }

    /**
     * Every card in the deck.
     */
    public List<FrenchCard> deckCards() {
        List<FrenchCard> cards = new ArrayList<>();
        for (FrenchCard.Suite suit : SUITS)
            for (FrenchCard.FrenchCardType rank : RANKS)
                cards.add(new FrenchCard(rank, suit));
        return cards;
    }

    @Override
    protected AbstractParameters _copy() {
        return new LeducPokerParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof LeducPokerParameters;
    }

    @Override
    public LeducPokerParameters instantiate() {
        return this;
    }
}

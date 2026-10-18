package games.schwimmen;

import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

/**
 * Parameters for Schwimmen (https://mgoadric.github.io/valet/post/schwimmen.html, summarised from
 * https://www.pagat.com/commerce/schwimmen.html). The defaults play pagat's game of chips over many deals;
 * data/schwimmen/Schwimmen_Valet.json gives Valet's single deal, won by the best hand.
 */
public class SchwimmenParameters extends TunableParameters<SchwimmenParameters> {

    // The suits from lowest to highest, for breaking ties between equal totals
    public static final List<FrenchCard.Suite> SUIT_ORDER = List.of(FrenchCard.Suite.Diamonds, FrenchCard.Suite.Hearts,
            FrenchCard.Suite.Spades, FrenchCard.Suite.Clubs);

    // Cards in each hand, in the extra hand and on the table
    public int handSize = 3;

    // Safeguard: a deal ends, and is scored as if a player had closed, once every player has had this many turns
    public int maxCircuitsPerDeal = 10;

    // Values of the special hands
    public double threeOfAKindValue = 30.5;
    public double threeAcesValue = 32.0;

    // The single-suit total that ends the deal at once (an Ace and two ten-point cards)
    public int schnauzTotal = 31;

    // If true, pagat's game: the worst hand of each deal loses a chip, and the last player left wins
    public boolean livesGame = true;
    public int startingChips = 3;

    // Safeguard: the most deals in a game
    public int maxDeals = 100;

    public SchwimmenParameters() {
        addTunableParameter("handSize", 3);
        addTunableParameter("maxCircuitsPerDeal", 10, Arrays.asList(3, 5, 10, 20));
        addTunableParameter("threeOfAKindValue", 30.5);
        addTunableParameter("threeAcesValue", 32.0);
        addTunableParameter("schnauzTotal", 31);
        addTunableParameter("livesGame", true, List.of(false, true));
        addTunableParameter("startingChips", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("maxDeals", 100);
        _reset();
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        maxCircuitsPerDeal = (int) getParameterValue("maxCircuitsPerDeal");
        threeOfAKindValue = (double) getParameterValue("threeOfAKindValue");
        threeAcesValue = (double) getParameterValue("threeAcesValue");
        schnauzTotal = (int) getParameterValue("schnauzTotal");
        livesGame = (boolean) getParameterValue("livesGame");
        startingChips = (int) getParameterValue("startingChips");
        maxDeals = (int) getParameterValue("maxDeals");
        setMaxRounds(maxDeals);
    }

    /**
     * The value of a card towards a single-suit total.
     */
    public int cardValue(FrenchCard card) {
        // FrenchCard numbers the Ace 14 and the court cards 11-13
        return switch (card.type) {
            case Ace -> 11;
            case King, Queen, Jack -> 10;
            default -> card.number;
        };
    }

    @Override
    protected SchwimmenParameters _copy() {
        return new SchwimmenParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof SchwimmenParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public SchwimmenParameters instantiate() {
        return this;
    }
}

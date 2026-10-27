package games.scopa;

import core.components.TarotCard;
import evaluation.optimisation.TunableParameters;

import java.util.Arrays;
import java.util.List;

/**
 * <p>Parameters for Scopa. The defaults follow Pagat's rules (https://www.pagat.com/fishing/scopa.html): deals until a
 * player reaches 11, and a redeal when the table starts with three or more Kings. data/scopa/Scopa_Valet.json gives
 * the RECYCLE code on the Valet page (https://mgoadric.github.io/valet/post/scopa.html): one deal.</p>
 */
public class ScopaParameters extends TunableParameters<ScopaParameters> {

    // Cards dealt to each player at a time, and face up to the table at the start of a deal
    public int handSize = 3;
    public int tableSize = 4;

    // The game ends after the deal in which a player's total reaches this; 0 plays a single deal (RECYCLE)
    public int targetScore = 11;

    // Whether a deal that puts three or more Kings on the table is thrown in and dealt again (pagat)
    public boolean redealOnKings = true;

    public ScopaParameters() {
        addTunableParameter("handSize", 3);
        addTunableParameter("tableSize", 4);
        addTunableParameter("targetScore", 11, Arrays.asList(0, 11, 21));
        addTunableParameter("redealOnKings", true, List.of(false, true));
        _reset();
    }

    @Override
    public void _reset() {
        handSize = (int) getParameterValue("handSize");
        tableSize = (int) getParameterValue("tableSize");
        targetScore = (int) getParameterValue("targetScore");
        redealOnKings = (boolean) getParameterValue("redealOnKings");
    }

    public static final List<TarotCard.Suit> SUITS =
            List.of(TarotCard.Suit.Swords, TarotCard.Suit.Batons, TarotCard.Suit.Cups, TarotCard.Suit.Coins);
    public static final TarotCard SETTEBELLO = new TarotCard(TarotCard.Suit.Coins, 7);

    public static int captureValue(TarotCard card) {
        return switch (card.number) {
            case TarotCard.KNAVE -> 8;
            case TarotCard.CAVALIER -> 9;
            case TarotCard.KING -> 10;
            default -> card.number;
        };
    }

    public static int primieraValue(TarotCard card) {
        return switch (card.number) {
            case 7 -> 21;
            case 6 -> 18;
            case 1 -> 16;
            case 5 -> 15;
            case 4 -> 14;
            case 3 -> 13;
            case 2 -> 12;
            default -> 10;
        };
    }

    @Override
    protected ScopaParameters _copy() {
        return new ScopaParameters();  // TunableParameters.copy() copies the parameter values
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ScopaParameters;  // TunableParameters.equals() compares the parameter values
    }

    @Override
    public ScopaParameters instantiate() {
        return this;
    }
}

package games.hareandtortoise;

import evaluation.optimisation.TunableParameters;
import games.hareandtortoise.components.HareCard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.hareandtortoise.SquareType.*;

/**
 * Parameters for Hare and Tortoise. The defaults are the rules of the 1978 Ravensburger edition (David Parlett).
 */
public class HareAndTortoiseParameters extends TunableParameters<HareAndTortoiseParameters> {

    /**
     * The 1978 Ravensburger board: index 0 is START, 1-63 the squares, and 64 HOME.
     */
    public static final SquareType[] BOARD = {
            START,
            HARE, CARROT, HARE, THREE, CARROT, HARE, LETTUCE, TORTOISE, FOUR, TWO,                  // 1-10
            TORTOISE, THREE, CARROT, HARE, TORTOISE, FLAG, TWO, FOUR, TORTOISE, THREE,              // 11-20
            CARROT, LETTUCE, TWO, TORTOISE, HARE, CARROT, FOUR, THREE, TWO, TORTOISE,               // 21-30
            HARE, FLAG, CARROT, HARE, TWO, THREE, TORTOISE, CARROT, HARE, CARROT,                   // 31-40
            TWO, LETTUCE, TORTOISE, THREE, FOUR, HARE, TWO, FLAG, CARROT, TORTOISE,                 // 41-50
            HARE, THREE, TWO, FOUR, CARROT, TORTOISE, LETTUCE, HARE, CARROT, TWO,                   // 51-60
            HARE, LETTUCE, HARE,                                                                    // 61-63
            HOME
    };

    public static final int HOME_SQUARE = BOARD.length - 1;

    public int startCarrots = 65;
    public int startLettuces = 3;
    public int carrotsPerChew = 10;
    public int carrotsPerRacePosition = 10;
    public int carrotsPerTortoiseStep = 10;
    public int homeCarrotsPerRacePosition = 10;
    public int nFallBackOnePosition = 2;
    public int nLastTurnFree = 2;
    public int nDrawOrDiscard10 = 2;
    public int nLeapAheadOnePosition = 1;
    public int nNextCarrotSquare = 1;
    public int nPreviousCarrotSquare = 1;
    public int nAnotherTurn = 1;
    public int nMissATurn = 1;
    public int nChewALettuce = 1;

    public HareAndTortoiseParameters() {
        addTunableParameter("startCarrots", 65, Arrays.asList(45, 55, 65, 75, 95));
        addTunableParameter("startLettuces", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("carrotsPerChew", 10, Arrays.asList(5, 10, 15));
        addTunableParameter("carrotsPerRacePosition", 10, Arrays.asList(5, 10, 15));
        addTunableParameter("carrotsPerTortoiseStep", 10, Arrays.asList(5, 10, 15));
        addTunableParameter("homeCarrotsPerRacePosition", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("nFallBackOnePosition", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("nLastTurnFree", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("nDrawOrDiscard10", 2, Arrays.asList(0, 1, 2, 3));
        addTunableParameter("nLeapAheadOnePosition", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nNextCarrotSquare", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nPreviousCarrotSquare", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nAnotherTurn", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nMissATurn", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("nChewALettuce", 1, Arrays.asList(0, 1, 2));
        addTunableParameter("maxRounds", 300);
        _reset();
    }

    @Override
    public void _reset() {
        startCarrots = (int) getParameterValue("startCarrots");
        startLettuces = (int) getParameterValue("startLettuces");
        carrotsPerChew = (int) getParameterValue("carrotsPerChew");
        carrotsPerRacePosition = (int) getParameterValue("carrotsPerRacePosition");
        carrotsPerTortoiseStep = (int) getParameterValue("carrotsPerTortoiseStep");
        homeCarrotsPerRacePosition = (int) getParameterValue("homeCarrotsPerRacePosition");
        nFallBackOnePosition = (int) getParameterValue("nFallBackOnePosition");
        nLastTurnFree = (int) getParameterValue("nLastTurnFree");
        nDrawOrDiscard10 = (int) getParameterValue("nDrawOrDiscard10");
        nLeapAheadOnePosition = (int) getParameterValue("nLeapAheadOnePosition");
        nNextCarrotSquare = (int) getParameterValue("nNextCarrotSquare");
        nPreviousCarrotSquare = (int) getParameterValue("nPreviousCarrotSquare");
        nAnotherTurn = (int) getParameterValue("nAnotherTurn");
        nMissATurn = (int) getParameterValue("nMissATurn");
        nChewALettuce = (int) getParameterValue("nChewALettuce");
        // the framework's round limit, which StandardForwardModel.endRound applies
        setMaxRounds((int) getParameterValue("maxRounds"));
    }

    /**
     * The carrots it costs to move the given number of squares forwards: 1 + 2 + ... + distance (the Race Card).
     */
    public int moveCost(int distance) {
        return distance * (distance + 1) / 2;
    }

    /**
     * The hare cards in the pile at the start of the game, unshuffled.
     */
    public List<HareCard> hareCards() {
        List<HareCard> cards = new ArrayList<>();
        addCards(cards, HareCard.Type.FALL_BACK_ONE_POSITION, nFallBackOnePosition);
        addCards(cards, HareCard.Type.LAST_TURN_FREE, nLastTurnFree);
        addCards(cards, HareCard.Type.DRAW_OR_DISCARD_10, nDrawOrDiscard10);
        addCards(cards, HareCard.Type.LEAP_AHEAD_ONE_POSITION, nLeapAheadOnePosition);
        addCards(cards, HareCard.Type.NEXT_CARROT_SQUARE, nNextCarrotSquare);
        addCards(cards, HareCard.Type.PREVIOUS_CARROT_SQUARE, nPreviousCarrotSquare);
        addCards(cards, HareCard.Type.ANOTHER_TURN, nAnotherTurn);
        addCards(cards, HareCard.Type.MISS_A_TURN, nMissATurn);
        addCards(cards, HareCard.Type.CHEW_A_LETTUCE, nChewALettuce);
        return cards;
    }

    private static void addCards(List<HareCard> cards, HareCard.Type type, int n) {
        for (int i = 0; i < n; i++)
            cards.add(new HareCard(type));
    }

    @Override
    protected HareAndTortoiseParameters _copy() {
        return new HareAndTortoiseParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof HareAndTortoiseParameters;
    }

    @Override
    public HareAndTortoiseParameters instantiate() {
        return this;
    }
}

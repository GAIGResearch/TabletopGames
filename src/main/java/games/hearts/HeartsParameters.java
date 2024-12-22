package games.hearts;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import core.components.FrenchCard;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.*;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link evaluation.optimisation.TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class HeartsParameters extends AbstractParameters {
    public String dataPath = "data/FrenchCards/";
    public final int shootTheMoon = 26;
    public final int heartCard = 1;
    // Could be expanded if needed to a whole sequence of 'special cards, with special scores'
    public final FrenchCard qosCard = new FrenchCard(FrenchCard.FrenchCardType.Queen, FrenchCard.Suite.Spades);
    public final FrenchCard startingCard = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 2);
    public final int queenOfSpades = 13;
    public final int cardsPassedPerRound = 3;
    public final int matchScore = 50;

    // Number of cards per player - index to array is nPlayers
    public final int[] numberOfCardsPerPlayer = new int[]{0, 0, 0,
            17, 13, 10, 8, 7};

    Map<Integer, List<FrenchCard>> cardsToRemove = new HashMap<>();

    public HeartsParameters() {
        cardsToRemove.put(3, Collections.singletonList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2)));
        cardsToRemove.put(4, Collections.emptyList());
        cardsToRemove.put(5, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Spades, 2)));
        cardsToRemove.put(6, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 4)));
        cardsToRemove.put(7, Arrays.asList(new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 2),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Diamonds, 3),
                new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Clubs, 3)));
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        HeartsParameters hgp = new HeartsParameters();
        hgp.dataPath = dataPath;
        return hgp;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HeartsParameters)) return false;
        if (!super.equals(o)) return false;
        HeartsParameters that = (HeartsParameters) o;
        return Objects.equals(dataPath, that.dataPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dataPath);
    }

}

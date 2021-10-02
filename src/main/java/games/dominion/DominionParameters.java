package games.dominion;

import core.*;
import games.dominion.cards.*;

import java.util.*;

public class DominionParameters extends AbstractParameters {

    List<CardType> cardsUsed = new ArrayList<>();
    String dataPath = "data/dominion/";

    public final int HAND_SIZE = 5;
    public final int PILES_EXHAUSTED_FOR_GAME_END = 3;
    public final int KINGDOM_CARDS_OF_EACH_TYPE = 10;
    public final int CURSE_CARDS_PER_PLAYER = 10;
    public final int STARTING_COPPER = 7;
    public final int STARTING_ESTATES = 3;
    public final int COPPER_SUPPLY = 32;
    public final int SILVER_SUPPLY = 40;
    public final int GOLD_SUPPLY = 30;
    public final int[] VICTORY_CARDS_PER_PLAYER = {-1, -1, 8, 12, 12}; // 2-4 players only


    public DominionParameters(long seed) {
        super(seed);
    }

    public DominionParameters(long seed, String[] cards) {
        super(seed);
        for (String cardName : cards) {
            try {
                cardsUsed.add(CardType.valueOf(cardName));
            } catch (Exception e) {
                System.out.println("Error initialising Parameters with " + cardName);
                System.out.println(e.getMessage());
            }
        }
    }

    public static DominionParameters firstGame(long seed) {
        DominionParameters retValue = new DominionParameters(seed);
        retValue.cardsUsed.add(CardType.CELLAR);
        retValue.cardsUsed.add(CardType.MARKET);
        retValue.cardsUsed.add(CardType.MERCHANT);
        retValue.cardsUsed.add(CardType.MILITIA);
        retValue.cardsUsed.add(CardType.MINE);
        retValue.cardsUsed.add(CardType.MOAT);
        retValue.cardsUsed.add(CardType.REMODEL);
        retValue.cardsUsed.add(CardType.SMITHY);
        retValue.cardsUsed.add(CardType.VILLAGE);
        retValue.cardsUsed.add(CardType.WORKSHOP);
        // Note that the three Victory cards and three Treasure cards are always included
        return retValue;
    }

    public static DominionParameters sizeDistortion(long seed) {
        DominionParameters retValue = new DominionParameters(seed);
        retValue.cardsUsed.add(CardType.ARTISAN);
        retValue.cardsUsed.add(CardType.BANDIT);
        retValue.cardsUsed.add(CardType.BUREAUCRAT);
        retValue.cardsUsed.add(CardType.CHAPEL);
        retValue.cardsUsed.add(CardType.FESTIVAL);
        retValue.cardsUsed.add(CardType.GARDENS);
        retValue.cardsUsed.add(CardType.SENTRY);
        retValue.cardsUsed.add(CardType.THRONE_ROOM);
        retValue.cardsUsed.add(CardType.WITCH);
        retValue.cardsUsed.add(CardType.CURSE);
        retValue.cardsUsed.add(CardType.WORKSHOP);
        return retValue;
    }

    public static DominionParameters improvements(long seed) {
        DominionParameters retValue = new DominionParameters(seed);
        retValue.cardsUsed.add(CardType.ARTISAN);
        retValue.cardsUsed.add(CardType.CELLAR);
        retValue.cardsUsed.add(CardType.MARKET);
        retValue.cardsUsed.add(CardType.MERCHANT);
        retValue.cardsUsed.add(CardType.MINE);
        retValue.cardsUsed.add(CardType.MOAT);
        retValue.cardsUsed.add(CardType.MONEYLENDER);
        retValue.cardsUsed.add(CardType.POACHER);
        retValue.cardsUsed.add(CardType.REMODEL);
        retValue.cardsUsed.add(CardType.WITCH);
        retValue.cardsUsed.add(CardType.CURSE);
        // Note that the three Victory cards and three Treasure cards are always included
        return retValue;
    }


    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected AbstractParameters _copy() {
        return this;
        // currently parameters are immutable
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DominionParameters) {
            DominionParameters dp = (DominionParameters) o;
            return dp.cardsUsed.equals(cardsUsed);
        }
        return false;
    }

    public String getDataPath() {
        return dataPath;
    }
}

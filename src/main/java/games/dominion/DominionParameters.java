package games.dominion;

import core.*;
import games.dominion.cards.*;

import java.util.*;

public class DominionParameters extends AbstractParameters {

    Map<CardType, Integer> cardsUsed = new HashMap<>();
    String dataPath = "data/dominion/";

    public DominionParameters(long seed) {
        super(seed);
    }

    public DominionParameters(long seed, String[] cards, Map<String, Integer> overrideCount) {
        super(seed);
        for (String cardName : cards) {
            try {
                int number = overrideCount.getOrDefault(cardName, 10);
                cardsUsed.put(CardType.valueOf(cardName), number);
            } catch (Exception e) {
                System.out.println("Error initialising Parameters with " + cardName);
                System.out.println(e.getMessage());
            }
        }
    }

    public static DominionParameters firstGame(long seed) {
        DominionParameters retValue = new DominionParameters(seed);
        retValue.cardsUsed.put(CardType.CELLAR, 10);
        retValue.cardsUsed.put(CardType.MARKET, 10);
        retValue.cardsUsed.put(CardType.MERCHANT, 10);
        retValue.cardsUsed.put(CardType.MILITIA, 10);
        retValue.cardsUsed.put(CardType.MINE, 10);
        retValue.cardsUsed.put(CardType.MOAT, 10);
        retValue.cardsUsed.put(CardType.REMODEL, 10);
        retValue.cardsUsed.put(CardType.SMITHY, 10);
        retValue.cardsUsed.put(CardType.VILLAGE, 10);
        retValue.cardsUsed.put(CardType.WORKSHOP, 10);
        // Note that the three Victory cards and three Treasure cards are always included
        return retValue;
    }

    public static DominionParameters improvements(long seed) {
        DominionParameters retValue = new DominionParameters(seed);
        retValue.cardsUsed.put(CardType.ARTISAN, 10);
        retValue.cardsUsed.put(CardType.CELLAR, 10);
        retValue.cardsUsed.put(CardType.MARKET, 10);
        retValue.cardsUsed.put(CardType.MERCHANT, 10);
        retValue.cardsUsed.put(CardType.MINE, 10);
        retValue.cardsUsed.put(CardType.MOAT, 10);
        retValue.cardsUsed.put(CardType.MONEYLENDER, 10);
        retValue.cardsUsed.put(CardType.POACHER, 10);
        retValue.cardsUsed.put(CardType.REMODEL, 10);
        //      retValue.cardsUsed.put(CardType.WITCH, 10);
        //    retValue.cardsUsed.put(CardType.CURSE, 30);
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

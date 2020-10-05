package games.dominion;

import core.*;
import games.dominion.cards.*;

import java.util.*;

public class DominionParameters extends AbstractParameters {

    Set<CardType> cardsUsed = new HashSet<>();

    public DominionParameters(long seed) {
        super(seed);
    }

    public DominionParameters(long seed, String... cards) {
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
}

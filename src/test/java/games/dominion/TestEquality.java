package games.dominion;

import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class TestEquality {


    @Test
    public void dominionCards() {
        DominionCard moat1 = DominionCard.create(CardType.MOAT);
        DominionCard moat2 = DominionCard.create(CardType.MOAT);
        assertEquals(moat1, moat2);
        assertNotSame(moat1, moat2);

    }
}

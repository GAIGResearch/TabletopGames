package games.dominion.test;

import games.dominion.cards.*;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class EqualityTests {


    @Test
    public void dominionCards() {
        DominionCard moat1 = DominionCard.create(CardType.MOAT);
        DominionCard moat2 = DominionCard.create(CardType.MOAT);
        assertEquals(moat1, moat2);
        assertNotSame(moat1, moat2);

    }
}

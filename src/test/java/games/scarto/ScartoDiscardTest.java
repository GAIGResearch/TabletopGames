package games.scarto;

import core.components.TarotCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static games.scarto.ScartoTestUtils.*;
import static org.junit.Assert.*;

/**
 * The Discard action is identified by its card alone.
 */
public class ScartoDiscardTest {

    @Test
    public void discardsOfTheSameCardAreEqualWithTheSameHash() {
        assertEquals(new Discard(cup(3)), new Discard(cup(3)));
        assertEquals(new Discard(cup(3)).hashCode(), new Discard(cup(3)).hashCode());
        assertEquals(new Discard(cup(3)), new Discard(cup(3)).copy());
    }

    @Test
    public void discardsOfDifferentCardsAreNotEqual() {
        assertNotEquals(new Discard(cup(3)), new Discard(cup(4)));
        assertNotEquals(new Discard(cup(3)), new Discard(coin(3)));
        // nor is a Discard a PlayCard of the same card
        assertNotEquals(new Discard(cup(3)), new PlayCard<>(cup(3)));
    }

    @Test
    public void aDiscardStoresOnlyItsCard() {
        List<Field> fields = Arrays.stream(Discard.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers())).toList();
        assertEquals(1, fields.size());
        assertEquals(TarotCard.class, fields.get(0).getType());
    }
}

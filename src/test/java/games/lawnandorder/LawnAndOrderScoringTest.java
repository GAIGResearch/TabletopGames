package games.lawnandorder;

import games.lawnandorder.components.LawnCard;
import org.junit.Test;

import java.util.List;

import static games.lawnandorder.LawnAndOrderTestUtils.card;
import static games.lawnandorder.LawnAndOrderUtils.categoryScore;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static games.lawnandorder.components.LawnCard.Category.*;
import static org.junit.Assert.assertEquals;

/**
 * Scoring each category of a lawn, with the default parameters.
 */
public class LawnAndOrderScoringTest {

    LawnAndOrderParameters params = new LawnAndOrderParameters();

    @Test
    public void rulebookScoringExample() {
        List<LawnCard> lawn = List.of(card(ORNAMENT, PINK, OVERSIZED), card(ORNAMENT, PINK, PLASTIC),
                card(ORNAMENT, YELLOW, OVERSIZED), card(ORNAMENT, YELLOW, ILLUMINATED));
        assertEquals("four Ornaments", 4, categoryScore(lawn, TYPE, params));
        assertEquals("two Pink (1) + two Yellow (1)", 2, categoryScore(lawn, COLOUR, params));
        assertEquals("two Oversized (1); Plastic and Illuminated alone (0)", 1, categoryScore(lawn, FEATURE, params));
    }

    @Test
    public void singleCardsAndAnEmptyLawnScoreNothing() {
        List<LawnCard> lawn = List.of(card(ORNAMENT, RED, OVERSIZED), card(FURNITURE, YELLOW, ILLUMINATED),
                card(STRUCTURE, PINK, PLASTIC), card(WATER_FEATURE, BLUE, REPURPOSED));
        for (LawnCard.Category c : LawnCard.Category.values()) {
            assertEquals(0, categoryScore(lawn, c, params));
            assertEquals(0, categoryScore(List.of(), c, params));
        }
    }

    @Test
    public void groupsOfThreeFiveAndSix() {
        List<LawnCard> three = List.of(card(WATER_FEATURE, RED, OVERSIZED), card(WATER_FEATURE, RED, ILLUMINATED),
                card(WATER_FEATURE, RED, PLASTIC));
        assertEquals("three Water Features", 2, categoryScore(three, TYPE, params));
        assertEquals("three Red", 2, categoryScore(three, COLOUR, params));
        assertEquals("three different features", 0, categoryScore(three, FEATURE, params));

        List<LawnCard> five = List.of(card(WATER_FEATURE, RED, OVERSIZED), card(WATER_FEATURE, RED, ILLUMINATED),
                card(WATER_FEATURE, RED, PLASTIC), card(WATER_FEATURE, RED, REPURPOSED), card(WATER_FEATURE, BLUE, OVERSIZED));
        assertEquals("five Water Features", 7, categoryScore(five, TYPE, params));
        assertEquals("four Red (4) + one Blue (0)", 4, categoryScore(five, COLOUR, params));
        assertEquals("two Oversized (1), the others alone", 1, categoryScore(five, FEATURE, params));

        List<LawnCard> six = List.of(card(WATER_FEATURE, RED, OVERSIZED), card(WATER_FEATURE, RED, ILLUMINATED),
                card(WATER_FEATURE, RED, PLASTIC), card(WATER_FEATURE, RED, REPURPOSED), card(WATER_FEATURE, BLUE, OVERSIZED),
                card(WATER_FEATURE, BLUE, ILLUMINATED));
        assertEquals("six Water Features", 10, categoryScore(six, TYPE, params));
        assertEquals("four Red (4) + two Blue (1)", 5, categoryScore(six, COLOUR, params));
        assertEquals("two Oversized (1) + two Illuminated (1)", 2, categoryScore(six, FEATURE, params));
    }

    @Test
    public void aGroupOfSevenScoresAsSix() {
        List<LawnCard> lawn = List.of(card(ORNAMENT, RED, OVERSIZED), card(ORNAMENT, RED, ILLUMINATED),
                card(ORNAMENT, RED, PLASTIC), card(ORNAMENT, RED, REPURPOSED), card(FURNITURE, RED, OVERSIZED),
                card(FURNITURE, RED, ILLUMINATED), card(FURNITURE, RED, PLASTIC));
        assertEquals("seven Red score as six", 10, categoryScore(lawn, COLOUR, params));
        assertEquals("four Ornaments (4) + three Furniture (2)", 6, categoryScore(lawn, TYPE, params));
        assertEquals("two each of Oversized, Illuminated, Plastic (1 each), one Repurposed", 3,
                categoryScore(lawn, FEATURE, params));
    }
}

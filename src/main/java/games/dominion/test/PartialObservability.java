package games.dominion.test;


import core.AbstractPlayer;
import core.components.*;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.cards.*;
import org.junit.*;

import static org.junit.Assert.*;

import java.util.*;
import static java.util.stream.Collectors.*;

public class PartialObservability {

    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    DominionGame game = new DominionGame(players, DominionParameters.firstGame(System.currentTimeMillis()));
    DominionGameState state = (DominionGameState) game.getGameState();


    @Before
    public void setup() {
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 1, DeckType.HAND);
        state.addCard(CardType.WOODCUTTER, 2, DeckType.HAND);
        state.addCard(CardType.PROVINCE, 3, DeckType.HAND);
        state.addCard(CardType.MARKET, 0, DeckType.DISCARD);
        state.addCard(CardType.MARKET, 0, DeckType.DRAW);
        state.addCard(CardType.SILVER, 0, DeckType.DRAW);
        state.addCard(CardType.FESTIVAL, 1, DeckType.DISCARD);
        state.addCard(CardType.COPPER, 1, DeckType.DISCARD);
        state.addCard(CardType.ESTATE, 1, DeckType.DISCARD);
        state.addCard(CardType.SILVER, 1, DeckType.DISCARD);
        state.addCard(CardType.SILVER, 2, DeckType.TABLE);
        state.addCard(CardType.GOLD, 2, DeckType.TABLE);
        state.addCard(CardType.MARKET, 2, DeckType.TABLE);
    }

    @Test
    public void ownHandIsUnchanged() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertEquals(fullCopy.getDeck(DeckType.HAND, 0), state.getDeck(DeckType.HAND, 0));
        assertEquals(myCopy.getDeck(DeckType.HAND, 0), state.getDeck(DeckType.HAND, 0));
    }

    @Test
    public void handsOfOtherPlayersAreChanged(){
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertFalse(myCopy.getDeck(DeckType.HAND, 1).equals(state.getDeck(DeckType.HAND, 1)));
        assertFalse(myCopy.getDeck(DeckType.HAND, 2).equals(state.getDeck(DeckType.HAND, 2)));
        assertFalse(myCopy.getDeck(DeckType.HAND, 3).equals(state.getDeck(DeckType.HAND, 3)));

        assertEquals(fullCopy.getDeck(DeckType.HAND, 1), state.getDeck(DeckType.HAND, 1));
        assertEquals(fullCopy.getDeck(DeckType.HAND, 2), state.getDeck(DeckType.HAND, 2));
        assertEquals(fullCopy.getDeck(DeckType.HAND, 3), state.getDeck(DeckType.HAND, 3));
    }

    @Test
    public void allDiscardsAreUnchanged() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertEquals(myCopy.getDeck(DeckType.DISCARD, 0), state.getDeck(DeckType.DISCARD, 0));
        assertEquals(myCopy.getDeck(DeckType.DISCARD, 1), state.getDeck(DeckType.DISCARD, 1));
        assertEquals(myCopy.getDeck(DeckType.DISCARD, 2), state.getDeck(DeckType.DISCARD, 2));
        assertEquals(myCopy.getDeck(DeckType.DISCARD, 3), state.getDeck(DeckType.DISCARD, 3));

        assertEquals(fullCopy.getDeck(DeckType.DISCARD, 0), state.getDeck(DeckType.DISCARD, 0));
        assertEquals(fullCopy.getDeck(DeckType.DISCARD, 1), state.getDeck(DeckType.DISCARD, 1));
        assertEquals(fullCopy.getDeck(DeckType.DISCARD, 2), state.getDeck(DeckType.DISCARD, 2));
        assertEquals(fullCopy.getDeck(DeckType.DISCARD, 3), state.getDeck(DeckType.DISCARD, 3));
    }

    @Test
    public void ownDrawPileIsShuffled() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertFalse(myCopy.getDeck(DeckType.DRAW, 0).equals(state.getDeck(DeckType.DRAW, 0)));
        assertEquals(fullCopy.getDeck(DeckType.DRAW, 0), state.getDeck(DeckType.DRAW, 0));

        assertEquals(myCopy.getDeck(DeckType.DRAW, 0).getSize(), state.getDeck(DeckType.DRAW, 0).getSize());

        // but the drawpile should have exactly the same cards in it
        Map<CardType, Long> originalCards = state.getDeck(DeckType.DRAW, 0).stream().collect(groupingBy(DominionCard::cardType, counting()));
        Map<CardType, Long> myCopyCards = state.getDeck(DeckType.DRAW, 0).stream().collect(groupingBy(DominionCard::cardType, counting()));
        assertEquals(originalCards, myCopyCards);
    }

    @Test
    public void otherPlayersHandsAndDrawPilesAreShuffled() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        for (int playerId = 1; playerId < 4; playerId++) {
            assertFalse(myCopy.getDeck(DeckType.DRAW, playerId).equals(state.getDeck(DeckType.DRAW, playerId)));
            assertEquals(fullCopy.getDeck(DeckType.DRAW, playerId), state.getDeck(DeckType.DRAW, playerId));
            assertEquals(myCopy.getDeck(DeckType.DRAW, playerId).getSize(), state.getDeck(DeckType.DRAW, playerId).getSize());

            // but the drawpile and hand together should have exactly the same cards in them
            Deck<DominionCard> allCards = state.getDeck(DeckType.HAND, playerId).copy();
            allCards.add(state.getDeck(DeckType.DRAW, playerId));
            Map<CardType, Long> originalCards = allCards.stream().collect(groupingBy(DominionCard::cardType, counting()));

            Deck<DominionCard> copyCards = myCopy.getDeck(DeckType.HAND, playerId).copy();
            copyCards.add(myCopy.getDeck(DeckType.DRAW, playerId));
            Map<CardType, Long> myCopyCards = copyCards.stream().collect(groupingBy(DominionCard::cardType, counting()));
            assertEquals(originalCards, myCopyCards);
        }
    }

    @Test
    public void tableauIsUnchanged() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertEquals(myCopy.getDeck(DeckType.TABLE, 2), state.getDeck(DeckType.TABLE, 2));
        assertEquals(fullCopy.getDeck(DeckType.TABLE, 2), state.getDeck(DeckType.TABLE, 2));
    }
}

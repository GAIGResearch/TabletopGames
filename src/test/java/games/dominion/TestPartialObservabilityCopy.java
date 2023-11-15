package games.dominion;


import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.actions.MoatReaction;
import games.dominion.actions.MoveCard;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static games.dominion.cards.CardType.*;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.junit.Assert.*;

public class TestPartialObservabilityCopy {

    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    Game game = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(new DominionFGParameters(), players.size()));
    DominionGameState state = (DominionGameState) game.getGameState();
    DominionForwardModel fm = new DominionForwardModel();

    @Before
    public void setup() {
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 1, DeckType.HAND);
        state.addCard(CardType.WORKSHOP, 2, DeckType.HAND);
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
    public void handsOfOtherPlayersAreChanged() {
        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState fullCopy = (DominionGameState) state.copy();
        assertFalse(myCopy.getDeck(DeckType.HAND, 1).equals(state.getDeck(DeckType.HAND, 1)));
        assertFalse(myCopy.getDeck(DeckType.HAND, 2).equals(state.getDeck(DeckType.HAND, 2)));
        assertFalse(myCopy.getDeck(DeckType.HAND, 3).equals(state.getDeck(DeckType.HAND, 3)));

        assertEquals(fullCopy.getDeck(DeckType.HAND, 1), state.getDeck(DeckType.HAND, 1));
        assertEquals(fullCopy.getDeck(DeckType.HAND, 2), state.getDeck(DeckType.HAND, 2));
        assertEquals(fullCopy.getDeck(DeckType.HAND, 3), state.getDeck(DeckType.HAND, 3));

        PartialObservableDeck<DominionCard> fullCopyHand = (PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.HAND, 1);
        assertTrue(fullCopyHand.getDeckVisibility()[1]);
        IntStream.of(0, 2, 3).forEach(i -> assertFalse(fullCopyHand.getDeckVisibility()[i]));

        PartialObservableDeck<DominionCard> partCopyHand = (PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.HAND, 1);
        assertTrue(partCopyHand.getDeckVisibility()[1]);
        IntStream.of(0, 2, 3).forEach(i -> assertFalse(partCopyHand.getDeckVisibility()[i]));
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
            assertEquals(state.getDeck(DeckType.HAND, playerId).getSize(), myCopy.getDeck(DeckType.HAND, playerId).getSize());

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

    @Test
    public void revealedCardInHandDoesNotChange() {
        state.addCard(MOAT, 0, DeckType.HAND);
        MoatReaction moatReaction = new MoatReaction(0);
        fm.next(state, moatReaction);

        PartialObservableDeck<DominionCard> playerHand = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.HAND, 0);
        checkVisibilityExpectations(playerHand);

        DominionGameState myCopy = (DominionGameState) state.copy(0);
        DominionGameState theirCopy = (DominionGameState) state.copy(1);
        DominionGameState fullCopy = (DominionGameState) state.copy();

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.HAND, 0));
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.HAND, 0));
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.HAND, 0));
    }

    private void checkVisibilityExpectations(PartialObservableDeck<DominionCard> playerHand) {
        for (int p = 0; p < 4; p++) {
            assertTrue(playerHand.getVisibilityForPlayer(0, p));
            for (int j = 1; j < 4; j++)
                if (p != 0)
                    assertFalse(playerHand.getVisibilityForPlayer(j, p));
        }
        assertEquals(MOAT, playerHand.peek(0).cardType());
    }

    private void checkVisibilityExpectations(PartialObservableDeck<DominionCard> deck, int cardIndex, boolean[] visibility, CardType card) {
        for (int p = 0; p < 4; p++) {
          //  System.out.printf("Player: %d, index: %d\n", p, cardIndex);
            assertEquals(visibility[p], deck.getVisibilityForPlayer(cardIndex, p));
            if (visibility[p] && card!= null)
                assertEquals(card, deck.peek(cardIndex).cardType());
        }
    }

    @Test
    public void movingACardPubliclyRetainsVisibilityForAnyObserver() {
        state.addCard(CardType.SILVER, 2, DeckType.HAND);
        MoveCard move = new MoveCard(SILVER, 2, DeckType.HAND, 2, DeckType.DRAW, true);
        fm.next(state, move);

        PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, 2);
        checkVisibilityExpectations(drawDeck, 0, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations(drawDeck, 1, new boolean[]{false, false, false, false}, null);

        DominionGameState myCopy = (DominionGameState) state.copy(2);
        DominionGameState theirCopy = (DominionGameState) state.copy(1);
        DominionGameState fullCopy = (DominionGameState) state.copy();

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{true, true, true, true}, SILVER);

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, false, false, false}, null);
    }

    @Test
    public void reshufflingUnknownCardsInADeckOnlyShufflesVisibilitiesThatWeDoNotShare() {
        state.addCard(CardType.SILVER, 2, DeckType.DRAW);
        state.addCard(CardType.MARKET, 2, DeckType.DRAW);
        state.addCard(CardType.GOLD, 2, DeckType.DRAW);
        state.addCard(CardType.SMITHY, 2, DeckType.DRAW);
        PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, 2);
        drawDeck.setVisibilityOfComponent(1, new boolean[]{false, true, true, false});
        drawDeck.setVisibilityOfComponent(3, new boolean[]{true, true, true, true});
        drawDeck.setVisibilityOfComponent(4, new boolean[]{true, false, true, false});
        drawDeck.setVisibilityOfComponent(6, new boolean[]{false, false, true, false});

        DominionGameState myCopy = (DominionGameState) state.copy(2);
        DominionGameState theirCopy = (DominionGameState) state.copy(1);
        DominionGameState fullCopy = (DominionGameState) state.copy();

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, true, true, false}, GOLD);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 2, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 3, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 4, new boolean[]{true, false, true, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 5, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) myCopy.getDeck(DeckType.DRAW, 2), 6, new boolean[]{false, false, true, false}, null);

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, true, true, false}, GOLD);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 2, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 3, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 4, new boolean[]{true, false, true, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 5, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) fullCopy.getDeck(DeckType.DRAW, 2), 6, new boolean[]{false, false, true, false}, null);

        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 0, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 1, new boolean[]{false, true, true, false}, GOLD);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 2, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 3, new boolean[]{true, true, true, true}, SILVER);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 4, new boolean[]{true, false, true, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 5, new boolean[]{false, false, false, false}, null);
        checkVisibilityExpectations((PartialObservableDeck<DominionCard>) theirCopy.getDeck(DeckType.DRAW, 2), 6, new boolean[]{false, false, true, false}, null);


    }

}

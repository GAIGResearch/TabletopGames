package games.dominion.test;

import core.AbstractPlayer;
import core.actions.DoNothing;
import core.components.*;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.actions.BuyCard;
import games.dominion.cards.*;
import org.junit.*;

import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.Collectors;

public class CoreGameLoop {

    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    DominionGame game = new DominionGame(players, DominionParameters.firstGame(System.currentTimeMillis()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void initialHandDealIsCorrect() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 4; i++) {
            assertEquals(5, state.getDeck(DeckType.HAND, i).getSize());
            assertEquals(5, state.getDeck(DeckType.DRAW, i).getSize());
            assertEquals(0, state.getDeck(DeckType.DISCARD, i).getSize());
            Deck<DominionCard> allCards = new Deck<>("test");
            allCards.add(state.getDeck(DeckType.HAND, i));
            allCards.add(state.getDeck(DeckType.DRAW, i));
            assertEquals(7, allCards.stream().filter(c -> c.cardType() == CardType.COPPER).count());
            assertEquals(3, allCards.stream().filter(c -> c.cardType() == CardType.ESTATE).count());
        }
    }

    @Test
    public void purchaseOptionsAreCorrect() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGameState.DominionGamePhase.Buy);
        for (int i = 0; i < 10; i++) {
            state.spend(state.availableSpend(0));
            state.spend(-i);
            assertEquals(i, state.availableSpend(0));
            fm.computeAvailableActions(state);
            Set<CardType> availableCards = state.getActions().stream()
                    .filter(a -> a instanceof BuyCard)
                    .map(a -> (BuyCard) a)
                    .map(a -> a.cardType)
                    .collect(Collectors.toSet());

            switch (i) {
                case 9:
                case 8:
                    assertTrue(availableCards.contains(CardType.PROVINCE));
                case 7:
                case 6:
                    assertTrue(availableCards.contains(CardType.GOLD));
                case 5:
                    assertTrue(availableCards.contains(CardType.DUCHY));
                case 4:
                case 3:
                    assertTrue(availableCards.contains(CardType.SILVER));
                case 2:
                    assertTrue(availableCards.contains(CardType.ESTATE));
                case 1:
                case 0:
                    assertTrue(availableCards.contains(CardType.COPPER));
                    assertTrue(state.getActions().contains(new DoNothing()));
            }
        }
    }

    @Test
    public void endOfRoundCleanUpAsExpected() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGameState.DominionGamePhase.Buy);
        state.endOfTurn(0);
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(DominionGameState.DominionGamePhase.Play, state.getGamePhase());
    }

    @Test
    public void buyingACard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGameState.DominionGamePhase.Buy);
        int silverAvailable = state.cardsOfType(CardType.SILVER, 0, DeckType.SUPPLY);
        BuyCard newBuy = new BuyCard(CardType.SILVER, 0);
        state.spend(-1); // to guarantee they can afford it
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.ALL));
        newBuy.execute(state);
        assertEquals(1, state.cardsOfType(CardType.SILVER, 0, DeckType.ALL));
        assertEquals(7, state.cardsOfType(CardType.COPPER, 0, DeckType.ALL));
        assertEquals(3, state.cardsOfType(CardType.ESTATE, 0, DeckType.ALL));
        assertEquals(1, state.cardsOfType(CardType.SILVER, 0, DeckType.DISCARD));
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.HAND));
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.DRAW));
        assertEquals(silverAvailable - 1, state.cardsOfType(CardType.SILVER, 0, DeckType.SUPPLY));
        assertEquals(0, state.buysLeft());
    }

    @Test
    public void provinceGameEndCondition() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 12; i++) {
            assertFalse(state.gameOver());
            state.removeCardFromTable(CardType.PROVINCE);
        }
        assertTrue(state.gameOver());
    }

    @Test
    public void threePilesEmptyGameEndCondition() {
        DominionGameState state = (DominionGameState) game.getGameState();
        CardType[] cardsToRemove = {CardType.CELLAR, CardType.VILLAGE, CardType.SMITHY};
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < cardsToRemove.length; j++) {
                assertFalse(state.gameOver());
                state.removeCardFromTable(cardsToRemove[j]);
            }
        }
        assertTrue(state.gameOver());
    }

}

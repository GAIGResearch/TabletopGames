package games.dominion;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.DominionGameState.DominionGamePhase;
import games.dominion.actions.BuyCard;
import games.dominion.actions.EndPhase;
import games.dominion.actions.SimpleAction;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestCoreGameLoop {

    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    Game game = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(new DominionFGParameters(), players.size()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void initialHandDealIsCorrect() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 4; i++) {
            assertEquals(5, state.getDeck(DeckType.HAND, i).getSize());
            assertEquals(5, state.getDeck(DeckType.DRAW, i).getSize());
            assertEquals(0, state.getDeck(DeckType.DISCARD, i).getSize());
            Deck<DominionCard> allCards = new Deck<>("test", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
            allCards.add(state.getDeck(DeckType.HAND, i));
            allCards.add(state.getDeck(DeckType.DRAW, i));
            assertEquals(7, allCards.stream().filter(c -> c.cardType() == CardType.COPPER).count());
            assertEquals(3, allCards.stream().filter(c -> c.cardType() == CardType.ESTATE).count());
        }
    }

    @Test
    public void purchaseOptionsAreCorrect() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGamePhase.Buy);
        for (int i = 0; i < 10; i++) {
            state.spend(state.getAvailableSpend(0));
            state.spend(-i);
            assertEquals(i, state.getAvailableSpend(0));
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            Set<CardType> availableCards = actions.stream()
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
                    assertTrue(fm.computeAvailableActions(state).contains(new EndPhase(DominionGamePhase.Buy)));
            }
        }
    }

    @Test
    public void endOfTurnCleanUpAsExpected() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGamePhase.Buy);
        state.addCard(CardType.COPPER, 0, DeckType.TABLE);
        state.addCard(CardType.COPPER, 1, DeckType.TABLE);
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new EndPhase(DominionGamePhase.Buy));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 1).getSize());
        assertEquals(6, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
    }

    @Test
    public void endOfRoundCleanUpAsExpected() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int p = 0; p < 4; p++) {
            state.setGamePhase(DominionGamePhase.Buy);
            BuyCard newBuy = new BuyCard(CardType.MOAT, p);
            fm.next(state, newBuy);
        }
        for (int p=0; p < 4; p++) {
            assertEquals(5, state.getDeck(DeckType.HAND, p).getSize());
            assertEquals(0, state.getDeck(DeckType.DRAW, p).getSize());
            assertEquals(0, state.getDeck(DeckType.TABLE, p).getSize());
            assertEquals(6, state.getDeck(DeckType.DISCARD, p).getSize());
            assertEquals(DominionGamePhase.Play, state.getGamePhase());
        }
    }
    private void moveForwardToNextPlayer(DominionGameState state) {
        int startingPlayer = state.getCurrentPlayer();
        while (state.getCurrentPlayer() == startingPlayer)
            fm.next(state, new EndPhase((DominionGamePhase) state.getGamePhase()));
    }

    @Test
    public void reshuffleOfDiscardIntoDeck() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 4; i++)
            moveForwardToNextPlayer(state);
        assertEquals(0, state.getCurrentPlayer());
        state.setGamePhase(DominionGamePhase.Buy);
        BuyCard newBuy = new BuyCard(CardType.MOAT, 0);
        fm.next(state, newBuy);
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
    }

    @Test
    public void buyingACard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGamePhase.Buy);
        int silverAvailable = state.cardsOfType(CardType.SILVER, 0, DeckType.SUPPLY);
        BuyCard newBuy = new BuyCard(CardType.SILVER, 0);
        state.spend(-1); // to guarantee they can afford it
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.ALL));
        fm.computeAvailableActions(state);
        fm.next(state, newBuy);
        assertEquals(1, state.cardsOfType(CardType.SILVER, 0, DeckType.ALL));
        assertEquals(7, state.cardsOfType(CardType.COPPER, 0, DeckType.ALL));
        assertEquals(3, state.cardsOfType(CardType.ESTATE, 0, DeckType.ALL));
        assertEquals(1, state.cardsOfType(CardType.SILVER, 0, DeckType.DISCARD));
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.HAND));
        assertEquals(0, state.cardsOfType(CardType.SILVER, 0, DeckType.DRAW));
        assertEquals(silverAvailable - 1, state.cardsOfType(CardType.SILVER, 0, DeckType.SUPPLY));
        assertEquals(1, state.getBuysLeft());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
    }

    @Test
    public void canBuyMoreThanOneCard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setGamePhase(DominionGamePhase.Buy);
        BuyCard newBuy = new BuyCard(CardType.COPPER, 0);
        state.changeBuys(3);
        for (int i = 0; i < 4; i++) {
            assertEquals(4 - i, state.getBuysLeft());
            assertEquals(i, state.cardsOfType(CardType.COPPER, 0, DeckType.DISCARD));
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertTrue(actions.contains(new EndPhase(DominionGamePhase.Buy)));
            assertTrue(actions.contains(newBuy));
            fm.computeAvailableActions(state);
            fm.next(state, newBuy);
        }
        assertEquals(1, state.getBuysLeft());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(11, state.cardsOfType(CardType.COPPER, 0, DeckType.ALL));
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
            for (CardType type : cardsToRemove) {
                assertFalse(state.gameOver());
                state.removeCardFromTable(type);
            }
        }
        assertTrue(state.gameOver());
    }

    @Test
    public void canPlayACardFromHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertTrue(actions.contains(new EndPhase(DominionGamePhase.Play)));
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertTrue(actions.contains(new EndPhase(DominionGamePhase.Play)));
        assertTrue(actions.contains(new SimpleAction(CardType.VILLAGE, 0)));
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(actions.contains(new EndPhase(DominionGamePhase.Play)));
        assertTrue(actions.contains(new SimpleAction(CardType.VILLAGE, 0)));
        assertTrue(actions.contains(new SimpleAction(CardType.SMITHY, 0)));
    }

    @Test
    public void cannotPlayAnActionCardDuringBuyPhase() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        state.setGamePhase(DominionGamePhase.Buy);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new EndPhase(DominionGamePhase.Buy)));
        assertFalse(actions.contains(new SimpleAction(CardType.VILLAGE, 0)));
    }

    @Test
    public void cannotPlayACardWithNoActionsLeft() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        (new SimpleAction(CardType.SMITHY, 0)).execute(state);
        assertEquals(0, state.getActionsLeft());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertTrue(actions.contains(new EndPhase(DominionGamePhase.Play)));
    }

    @Test
    public void playingActionCardMovesItToTableau() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        fm.computeAvailableActions(state);
        fm.next(state, new SimpleAction(CardType.VILLAGE, 0));
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
    }

    @Test
    public void playingLastActionMovesToBuyPhase() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        fm.computeAvailableActions(state);
        fm.next(state, new SimpleAction(CardType.SMITHY, 0));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void drawingAVisibleCardLeavesInvisibleCardOnDeck() {
        DominionGameState state = (DominionGameState) game.getGameState();
        PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, 2);
        drawDeck.setVisibilityOfComponent(0, 1, true);
        drawDeck.setVisibilityOfComponent(0, 2, true);

        drawDeck.draw();
        for (int i = 0; i < 4; i++) {
            assertFalse(drawDeck.getVisibilityForPlayer(0, i));
        }

    }
}

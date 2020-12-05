package games.dominion.test;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.PartialObservableDeck;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.actions.*;
import games.dominion.cards.*;
import games.dominion.DominionGameState.*;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

public class BaseActionCards {

    Random rnd = new Random(373);
    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    DominionGame game = new DominionGame(players, DominionParameters.firstGame(System.currentTimeMillis()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void village() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction village = new Village(0);
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        fm.next(state, village);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(2, state.actionsLeft());
    }

    @Test
    public void smithy() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction smithy = new Smithy(0);
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        fm.next(state, smithy);
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.actionsLeft());
    }

    @Test
    public void laboratory() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction laboratory = new Laboratory(0);
        state.addCard(CardType.LABORATORY, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        fm.next(state, laboratory);
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.actionsLeft());
    }

    @Test
    public void woodcutter() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction woodcutter = new Woodcutter(0);
        state.addCard(CardType.WOODCUTTER, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(1, state.buysLeft());
        int money = state.availableSpend(0);
        fm.next(state, woodcutter);
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.actionsLeft());
        assertEquals(money + 2, state.availableSpend(0));
    }

    @Test
    public void market() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction market = new Market(0);
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(1, state.buysLeft());
        fm.next(state, market);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.actionsLeft());
        int money = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(money + 1, state.availableSpend(0));
    }

    @Test
    public void festival() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction festival = new Festival(0);
        state.addCard(CardType.FESTIVAL, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(1, state.buysLeft());
        int money = state.availableSpend(0);
        fm.next(state, festival);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(2, state.actionsLeft());
        assertEquals(money + 2, state.availableSpend(0));
    }

    @Test
    public void cellarBase() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction cellar = new Cellar(0);
        state.addCard(CardType.CELLAR, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to ensure we have at least one ESTATE and one COPPER
        fm.next(state, cellar);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(state.currentActionInProgress(), cellar);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.actionsLeft());

        List<AbstractAction> cellarActions = fm.computeAvailableActions(state);
        assertEquals(3, cellarActions.size());
        assertTrue(cellarActions.contains(new DiscardCard(CardType.ESTATE, 0)));
        assertTrue(cellarActions.contains(new DiscardCard(CardType.COPPER, 0)));
        assertTrue(cellarActions.contains(new DoNothing()));
    }

    @Test
    public void cellarDiscardsAndDraws() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction cellar = new Cellar(0);
        state.addCard(CardType.CELLAR, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to ensure we have at least one ESTATE and one COPPER
        fm.next(state, cellar);

        fm.next(state, new DiscardCard(CardType.ESTATE, 0));
        fm.next(state, new DiscardCard(CardType.COPPER, 0));
        fm.next(state, new DiscardCard(CardType.COPPER, 0));
        assertEquals(3, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());

        fm.next(state, new DoNothing());
        assertEquals(3, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(2, state.getDeck(DeckType.DRAW, 0).getSize());
        assertNull(state.currentActionInProgress());

        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new EndPhase(), nextActions.get(0));
    }

    @Test
    public void militiaCausesAllOtherPlayersToDiscardDownToFive() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.endOfTurn(0);
        state.endOfTurn(1);
        assertEquals(2, state.getCurrentPlayer());
        DominionAction militia = new Militia(2);
        state.addCard(CardType.MILITIA, 2, DeckType.HAND);
        for (int i = 0; i < 4; i++) {
            if (i != 2) assertEquals(5, state.getDeck(DeckType.HAND, i).getSize());
        }
        int start = state.availableSpend(2);
        fm.next(state, militia);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(start + 2, state.availableSpend(2));
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            assertTrue(actionsAvailable.stream().allMatch(a -> a instanceof DiscardCard));
            fm.next(state, actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
        } while (state.getCurrentPlayer() != 2);
        for (int i = 0; i < 4; i++) {
            if (i != 2) assertEquals(3, state.getDeck(DeckType.HAND, i).getSize());
        }
    }

    @Test
    public void militiaSkipsPlayersWithThreeOrFewerCards() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.endOfTurn(0);
        state.endOfTurn(1);
        state.endOfTurn(2);
        assertEquals(3, state.getCurrentPlayer());
        DominionAction militia = new Militia(3);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(2, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(2, DeckType.HAND, 0, DeckType.DISCARD);
        assertEquals(2, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 2).getSize());

        fm.next(state, militia);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(militia, state.currentActionInProgress());
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            assertTrue(actionsAvailable.stream().allMatch(a -> a instanceof DiscardCard));
            fm.next(state, actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
        } while (state.getCurrentPlayer() != 3);
        assertEquals(2, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 2).getSize());
    }

    @Test
    public void militiaDoesNothingIfAllPlayersHaveThreeOrFewerCards() {
        DominionGameState state = (DominionGameState) game.getGameState();
        assertEquals(0, state.getCurrentPlayer());
        DominionAction militia = new Militia(0);
        state.addCard(CardType.MILITIA, 0, DeckType.HAND);
        for (int i = 1; i < 4; i++) {
            state.drawCard(i, DeckType.HAND, i, DeckType.DISCARD);
            state.drawCard(i, DeckType.HAND, i, DeckType.DISCARD);
        }
        assertEquals(3, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 2).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 3).getSize());

        fm.next(state, militia);
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());

    }

    @Test
    public void moat() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction moat = new Moat(0);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        fm.next(state, moat);
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.actionsLeft());
        assertFalse(state.isDefended(0));
    }

    @Test
    public void moatDefendsAgainstMilitia() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.endOfTurn(0);
        state.endOfTurn(1);
        state.endOfTurn(2);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        DominionAction militia = new Militia(3);

        fm.next(state, militia);

        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
        assertEquals(2, actionsAvailable.size());
        assertTrue(actionsAvailable.contains(new DoNothing()));
        assertTrue(actionsAvailable.contains(new MoatReaction(0)));

        assertFalse(state.isDefended(0));
        fm.next(state, new MoatReaction(0));
        assertTrue(state.isDefended(0));
        PartialObservableDeck<DominionCard> hand = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.HAND, 0);
        assertEquals(CardType.MOAT, hand.get(0).cardType());
        for (int i = 1; i < 3; i++) {
            assertFalse(state.isDefended(i));
            assertTrue(hand.getVisibilityForPlayer(0, i));
        }

        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void notRevealingMoatDoesNotDefendAgainstMilitia() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.endOfTurn(0);
        state.endOfTurn(1);
        state.endOfTurn(2);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        DominionAction militia = new Militia(3);

        fm.next(state, militia);

        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
        assertEquals(2, actionsAvailable.size());
        assertTrue(actionsAvailable.contains(new DoNothing()));
        assertTrue(actionsAvailable.contains(new MoatReaction(0)));

        assertFalse(state.isDefended(0));
        fm.next(state, new DoNothing());
        PartialObservableDeck<DominionCard> hand = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.HAND, 0);
        assertEquals(CardType.MOAT, hand.get(0).cardType());
        for (int i = 0; i < 3; i++) {
            assertFalse(state.isDefended(i));
            if (i != 0)
                assertFalse(hand.getVisibilityForPlayer(0, i));
        }
        assertEquals(0, state.getCurrentPlayer());
        actionsAvailable = fm.computeAvailableActions(state);
        assertTrue(actionsAvailable.stream().allMatch(a -> a instanceof DiscardCard));
        // and now we have to discard cards
    }

    @Test
    public void moatDefendsAgainstMilitiaII() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.endOfTurn(0);
        state.endOfTurn(1);
        assertEquals(2, state.getCurrentPlayer());
        DominionAction militia = new Militia(2);
        state.addCard(CardType.MILITIA, 2, DeckType.HAND);
        state.addCard(CardType.MOAT, 1, DeckType.HAND);

        fm.next(state, militia);
        assertEquals(3, state.getCurrentPlayer());
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            Optional<AbstractAction> moatReaction = actionsAvailable.stream().filter(a -> a instanceof MoatReaction).findFirst();
            AbstractAction chosen = moatReaction.orElseGet(() -> actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
            fm.next(state, chosen);
        } while (state.getCurrentPlayer() != 2);
        assertEquals(3, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(5, state.getDeck(DeckType.HAND, 2).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 3).getSize());
    }

    @Test
    public void moatDefenceStatusEndsWithTurn() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setDefended(3);
        assertTrue(state.isDefended(3));
        state.endOfTurn(0);
        for (int i = 0; i < 4; i++) {
            assertFalse(state.isDefended(i));
        }
    }

    @Test
    public void allPlayersDefendingAgainstMilitiaMovesProgressOn() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MOAT, 1, DeckType.HAND);
        state.addCard(CardType.MOAT, 2, DeckType.HAND);
        state.addCard(CardType.MOAT, 3, DeckType.HAND);
        state.addCard(CardType.MILITIA, 0, DeckType.HAND);
        DominionAction militia = new Militia(0);

        fm.next(state, militia);
        for (int i = 0; i < 3; i++) {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            AbstractAction moatReaction = actionsAvailable.stream().filter(a -> a instanceof MoatReaction).findFirst().get();
            fm.next(state, moatReaction);
        }
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void remodelForcesATrash() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertTrue(actions.stream().allMatch(a -> a instanceof TrashCard));
        assertEquals(3, actions.size()); // COPPER, GOLD, ESTATE

        fm.next(state, new TrashCard(CardType.ESTATE, 0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(1, state.getDeck(DeckType.TRASH, -1).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
    }


    @Test
    public void remodelWithNoCardsInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.getDeck(DeckType.HAND, 0).clear();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(1, actions.size());
        assertEquals(new DoNothing(), actions.get(0));

        fm.next(state, actions.get(0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void remodelBuyOptionsCorrectGivenTrashedCard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);
        int availableSpend = state.availableSpend(0);
        fm.next(state, new TrashCard(CardType.ESTATE, 0));

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof GainCard));
        assertTrue(actions.stream().allMatch(a -> ((GainCard)a).cardType.getCost() <= 4));
        Set<CardType> allCards = state.cardsToBuy();
        Set<CardType> allGainable = actions.stream().map( a -> ((GainCard)a).cardType).collect(toSet());
        allCards.removeAll(allGainable);
        assertTrue(allCards.stream().allMatch(c -> c.getCost() >= 5));

        fm.next(state, new GainCard(CardType.SILVER, 0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(availableSpend, state.availableSpend(0));
    }

    @Test
    public void merchantWithNoSilverInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.DISCARD);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(treasureValue, state.availableSpend(0));

        fm.next(state, new EndPhase());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue, state.availableSpend(0));
        assertEquals(1, state.buysLeft());
    }

    @Test
    public void merchantWithOneSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(treasureValue, state.availableSpend(0));

        fm.next(state, new EndPhase());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 1, state.availableSpend(0));
        assertEquals(1, state.buysLeft());
    }

    @Test
    public void merchantWithTwoSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(treasureValue, state.availableSpend(0));

        fm.next(state, new EndPhase());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 1, state.availableSpend(0));
        assertEquals(1, state.buysLeft());
    }

    @Test
    public void merchantsWithTwoSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        assertEquals(9, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.actionsLeft());
        assertEquals(treasureValue, state.availableSpend(0));

        fm.next(state, new EndPhase());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 2, state.availableSpend(0));
        assertEquals(1, state.buysLeft());
    }

    @Test
    public void workshop() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.WORKSHOP, 0, DeckType.HAND);
        Workshop workshop = new Workshop(0);

        int startSpend = state.availableSpend(0);
        fm.next(state, workshop);
        assertEquals(0, state.actionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(workshop.executionComplete(state));
        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertTrue(availableActions.stream().allMatch(a -> a instanceof GainCard));
        assertEquals(11, availableActions.size()); // COPPER, SILVER, ESTATE, CELLAR, MOAT, MERCHANT, VILLAGE, WORKSHOP, MILITIA, REMODEL, SMITHY
        availableActions.forEach(a -> {
                    GainCard gc = (GainCard) a;
                    assertTrue(gc.cardType.getCost() <= 4);
                    assertEquals(DeckType.DISCARD, gc.destinationDeck);
                }
        );
        fm.next(state, availableActions.get(3));
        assertEquals(0, state.actionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(workshop.executionComplete(state));
        assertEquals(startSpend, state.availableSpend(0));
        assertEquals(1, state.buysLeft());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void mine() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Mine mine = new Mine(0);

        int startSpend = state.availableSpend(0);
        fm.next(state, mine);
        assertEquals(0, state.actionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Play, state.getGamePhase());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new TrashCard(CardType.COPPER, 0)));
        assertTrue(availableActions.contains(new TrashCard(CardType.SILVER, 0)));

        fm.next(state, new TrashCard(CardType.SILVER, 0));
        assertFalse(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Play, state.getGamePhase());
        availableActions = fm.computeAvailableActions(state);
        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new GainCard(CardType.COPPER, 0, DeckType.HAND)));
        assertTrue(availableActions.contains(new GainCard(CardType.SILVER, 0, DeckType.HAND)));
        assertTrue(availableActions.contains(new GainCard(CardType.GOLD, 0, DeckType.HAND)));

        fm.next(state, new GainCard(CardType.GOLD, 0, DeckType.HAND));
        assertEquals(startSpend + 1, state.availableSpend(0));
        assertTrue(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void mineWithNoTreasure() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        do { // remove all COPPER
            state.getDeck(DeckType.HAND, 0).remove(DominionCard.create(CardType.COPPER));
        } while (state.getDeck(DeckType.HAND,0).stream().anyMatch(c -> c.cardType() == CardType.COPPER));

        Mine mine = new Mine(0);

        fm.next(state, mine);
        assertEquals(0, state.actionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.availableSpend(0));
    }
}

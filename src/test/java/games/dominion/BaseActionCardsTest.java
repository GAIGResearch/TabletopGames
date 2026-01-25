package games.dominion;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.PartialObservableDeck;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.DominionGameState.DominionGamePhase;
import games.dominion.actions.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import games.wonders7.actions.PlayCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static core.CoreConstants.GameResult.GAME_END;
import static games.dominion.DominionGameState.DominionGamePhase.Buy;
import static games.dominion.DominionGameState.DominionGamePhase.Play;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class BaseActionCardsTest {

    Random rnd = new Random(373);
    List<AbstractPlayer> players = Arrays.asList(new TestPlayer(),
            new TestPlayer(),
            new TestPlayer(),
            new TestPlayer());

    Game game = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(new DominionFGParameters(), players.size()));
    Game gameImprovements = new Game(GameType.Dominion, players, new DominionForwardModel(), new DominionGameState(DominionParameters.improvements(), players.size()));
    DominionForwardModel fm = new DominionForwardModel();

    @Test
    public void village() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction village = new SimpleAction(CardType.VILLAGE, 0);
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        fm.next(state, village);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(2, state.getActionsLeft());
    }

    @Test
    public void smithy() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction smithy = new SimpleAction(CardType.SMITHY, 0);
        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        fm.next(state, smithy);
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.getActionsLeft());
    }

    @Test
    public void laboratory() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction laboratory = new SimpleAction(CardType.LABORATORY, 0);
        state.addCard(CardType.LABORATORY, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        fm.next(state, laboratory);
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.getActionsLeft());
    }

    @Test
    public void market() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction market = new SimpleAction(CardType.MARKET, 0);
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(1, state.getBuysLeft());
        fm.next(state, market);
        assertEquals(Play, state.getGamePhase());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        int money = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(money + 1, state.getAvailableSpend(0));
    }

    @Test
    public void festival() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction festival = new SimpleAction(CardType.FESTIVAL, 0);
        state.addCard(CardType.FESTIVAL, 0, DeckType.HAND);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(1, state.getBuysLeft());
        int money = state.getAvailableSpend(0);
        fm.next(state, festival);
        assertEquals(Play, state.getGamePhase());
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(2, state.getActionsLeft());
        assertEquals(money + 2, state.getAvailableSpend(0));
    }

    @Test
    public void cellarBase() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction cellar = new Cellar(0);
        state.addCard(CardType.CELLAR, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to ensure we have at least one ESTATE and one COPPER
        fm.next(state, cellar);
        assertEquals(Play, state.getGamePhase());
        assertEquals(state.currentActionInProgress(), cellar);
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(1, state.getActionsLeft());

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
        assertEquals(new EndPhase(Play), nextActions.get(0));
    }

    @Test
    public void militiaCausesAllOtherPlayersToDiscardDownToFive() {
        DominionGameState state = (DominionGameState) game.getGameState();
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        assertEquals(2, state.getCurrentPlayer());
        DominionAction militia = new Militia(2);
        state.addCard(CardType.MILITIA, 2, DeckType.HAND);
        for (int i = 0; i < 4; i++) {
            if (i != 2) assertEquals(5, state.getDeck(DeckType.HAND, i).getSize());
        }
        int start = state.getAvailableSpend(2);
        fm.next(state, militia);
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(start + 2, state.getAvailableSpend(2));
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
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
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
        DominionAction moat = new SimpleAction(CardType.MOAT, 0);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        fm.next(state, moat);
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(0, state.getActionsLeft());
        assertFalse(state.isDefended(0));
    }

    private void moveForwardToNextPlayer(DominionGameState state) {
        int startingPlayer = state.getCurrentPlayer();
        while (state.getCurrentPlayer() == startingPlayer)
            fm.next(state, new EndPhase((DominionGamePhase) state.getGamePhase()));
    }

    @Test
    public void moatDefendsAgainstMilitia() {
        DominionGameState state = (DominionGameState) game.getGameState();
        moveForwardToNextPlayer(state);
        moveForwardToNextPlayer(state);
        moveForwardToNextPlayer(state);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        assertEquals(3, state.getCurrentPlayer());
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
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
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
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
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
        moveForwardToNextPlayer(state);
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
            assertTrue(state.isDefended(i + 1));
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
        assertEquals(Play, state.getGamePhase());
        assertTrue(actions.stream().allMatch(a -> a instanceof TrashCard));
        assertEquals(3, actions.size()); // COPPER, GOLD, ESTATE

        fm.next(state, new TrashCard(CardType.ESTATE, 0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
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

        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void remodelBuyOptionsCorrectGivenTrashedCard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);
        int availableSpend = state.getAvailableSpend(0);
        fm.next(state, new TrashCard(CardType.ESTATE, 0));

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.stream().allMatch(a -> a instanceof GainCard));
        assertTrue(actions.stream().allMatch(a -> ((GainCard) a).cardType.cost <= 4));
        List<CardType> allCards = state.getCardsToBuy();
        List<CardType> allGainable = actions.stream().map(a -> ((GainCard) a).cardType).collect(toList());
        allCards.removeAll(allGainable);
        assertTrue(allCards.stream().allMatch(c -> c.cost >= 5));

        fm.next(state, new GainCard(CardType.SILVER, 0));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(availableSpend, state.getAvailableSpend(0));
    }

    @Test
    public void remodelPossibleWithNoBuyableCards() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        // now remove all cost 2 cards
        while (state.getCardsToBuy().contains(CardType.ESTATE))
            state.removeCardFromTable(CardType.ESTATE);
        while (state.getCardsToBuy().contains(CardType.COPPER))
            state.removeCardFromTable(CardType.COPPER);
        while (state.getCardsToBuy().contains(CardType.MOAT))
            state.removeCardFromTable(CardType.MOAT);
        while (state.getCardsToBuy().contains(CardType.CELLAR))
            state.removeCardFromTable(CardType.CELLAR);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(Play, state.getGamePhase());
        assertTrue(actions.stream().allMatch(a -> a instanceof TrashCard));
        assertEquals(2, actions.size()); // COPPER, ESTATE

        fm.next(state, new TrashCard(CardType.COPPER, 0));
        actions = fm.computeAvailableActions(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, actions.size());
        assertEquals(new DoNothing(), actions.get(0));
        assertTrue(state.isActionInProgress());
        fm.next(state, new DoNothing());
        assertFalse(state.isActionInProgress());
        // at this point the game is over because we set three stacks to have no cards
        assertEquals(GAME_END, state.getGameStatus());
    }

    @Test
    public void merchantWithNoSilverInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.DISCARD);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(Play, state.getGamePhase());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(treasureValue, state.getAvailableSpend(0));

        fm.next(state, new EndPhase(Play));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue, state.getAvailableSpend(0));
        assertEquals(1, state.getBuysLeft());
    }

    @Test
    public void merchantWithOneSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);

        fm.next(state, merchant);
        int treasureValue = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(Play, state.getGamePhase());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(treasureValue, state.getAvailableSpend(0));

        fm.next(state, new EndPhase(Play));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 1, state.getAvailableSpend(0));
        assertEquals(1, state.getBuysLeft());
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
        assertEquals(Play, state.getGamePhase());
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(treasureValue, state.getAvailableSpend(0));

        fm.next(state, new EndPhase(Play));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 1, state.getAvailableSpend(0));
        assertEquals(1, state.getBuysLeft());
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
        assertEquals(Play, state.getGamePhase());
        assertEquals(9, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertEquals(treasureValue, state.getAvailableSpend(0));

        fm.next(state, new EndPhase(Play));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(treasureValue + 2, state.getAvailableSpend(0));
        assertEquals(1, state.getBuysLeft());
    }

    @Test
    public void workshop() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.WORKSHOP, 0, DeckType.HAND);
        Workshop workshop = new Workshop(0);

        int startSpend = state.getAvailableSpend(0);
        fm.next(state, workshop);
        assertEquals(0, state.getActionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(workshop.executionComplete(state));
        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertTrue(availableActions.stream().allMatch(a -> a instanceof GainCard));
        assertEquals(11, availableActions.size()); // COPPER, SILVER, ESTATE, CELLAR, MOAT, MERCHANT, VILLAGE, WORKSHOP, MILITIA, REMODEL, SMITHY
        availableActions.forEach(a -> {
                    GainCard gc = (GainCard) a;
                    assertTrue(gc.cardType.cost <= 4);
                    assertEquals(DeckType.DISCARD, gc.destinationDeck);
                }
        );
        fm.next(state, availableActions.get(3));
        assertEquals(0, state.getActionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(workshop.executionComplete(state));
        assertEquals(startSpend, state.getAvailableSpend(0));
        assertEquals(1, state.getBuysLeft());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void mine() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Mine mine = new Mine(0);

        int startSpend = state.getAvailableSpend(0);
        fm.next(state, mine);
        assertEquals(0, state.getActionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(mine.executionComplete(state));
        assertEquals(Play, state.getGamePhase());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new TrashCard(CardType.COPPER, 0)));
        assertTrue(availableActions.contains(new TrashCard(CardType.SILVER, 0)));

        fm.next(state, new TrashCard(CardType.SILVER, 0));
        assertFalse(mine.executionComplete(state));
        assertEquals(Play, state.getGamePhase());
        availableActions = fm.computeAvailableActions(state);
        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new GainCard(CardType.COPPER, 0, DeckType.HAND)));
        assertTrue(availableActions.contains(new GainCard(CardType.SILVER, 0, DeckType.HAND)));
        assertTrue(availableActions.contains(new GainCard(CardType.GOLD, 0, DeckType.HAND)));

        fm.next(state, new GainCard(CardType.GOLD, 0, DeckType.HAND));
        assertEquals(startSpend + 1, state.getAvailableSpend(0));
        assertTrue(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void mineWithNoTreasure() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        do { // remove all COPPER
            state.getDeck(DeckType.HAND, 0).remove(DominionCard.create(CardType.COPPER));
        } while (state.getDeck(DeckType.HAND, 0).stream().anyMatch(c -> c.cardType() == CardType.COPPER));

        Mine mine = new Mine(0);

        fm.next(state, mine);
        assertEquals(0, state.getActionsLeft());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(mine.executionComplete(state));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getAvailableSpend(0));
    }

    @Test
    public void artisan() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.ARTISAN, 0, DeckType.HAND);
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to make sure there is one
        Artisan artisan = new Artisan(0);

        fm.next(state, artisan);
        List<AbstractAction> availableActions = fm.computeAvailableActions(state);

        assertTrue(availableActions.stream().allMatch(a -> a instanceof GainCard));
        assertTrue(availableActions.stream().allMatch(a -> ((GainCard) a).cardType.cost <= 5));
        assertTrue(availableActions.contains(new GainCard(CardType.MINE, 0, DeckType.HAND)));
        assertFalse(artisan.executionComplete(state));

        fm.next(state, new GainCard(CardType.MINE, 0, DeckType.HAND));
        availableActions = fm.computeAvailableActions(state);

        assertTrue(availableActions.stream().allMatch(a -> a instanceof MoveCard));
        assertEquals(4, availableActions.size());
        assertTrue(availableActions.contains(new MoveCard(CardType.MINE, 0, DeckType.HAND, 0, DeckType.DRAW, false)));
        assertTrue(availableActions.contains(new MoveCard(CardType.COPPER, 0, DeckType.HAND, 0, DeckType.DRAW, false)));
        assertTrue(availableActions.contains(new MoveCard(CardType.ESTATE, 0, DeckType.HAND, 0, DeckType.DRAW, false)));
        assertTrue(availableActions.contains(new MoveCard(CardType.VILLAGE, 0, DeckType.HAND, 0, DeckType.DRAW, false)));
        assertFalse(artisan.executionComplete(state));

        fm.next(state, new MoveCard(CardType.MINE, 0, DeckType.HAND, 0, DeckType.DRAW, false));
        assertTrue(artisan.executionComplete(state));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());

        PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, 0);
        assertTrue(drawDeck.getVisibilityOfComponent(0)[0]); // player 0 can see the card; no-one else can
        for (int i = 1; i < 4; i++) assertFalse(drawDeck.getVisibilityOfComponent(0)[i]);
        for (int i = 0; i < 4; i++) assertFalse(drawDeck.getVisibilityOfComponent(1)[i]);

        DominionGameState copyState = (DominionGameState) state.copy(0);
        PartialObservableDeck<DominionCard> copyDrawDeck = (PartialObservableDeck<DominionCard>) copyState.getDeck(DeckType.DRAW, 0);
        assertTrue(copyDrawDeck.getVisibilityOfComponent(0)[0]); // player 0 can see the card; no-one else can
        for (int i = 1; i < 4; i++) assertFalse(copyDrawDeck.getVisibilityOfComponent(0)[i]);
        for (int i = 0; i < 4; i++) assertFalse(copyDrawDeck.getVisibilityOfComponent(1)[i]);

        copyState = (DominionGameState) state.copy(1);
        copyDrawDeck = (PartialObservableDeck<DominionCard>) copyState.getDeck(DeckType.DRAW, 0);
        assertTrue(copyDrawDeck.getVisibilityOfComponent(0)[0]); // player 0 can see the card; no-one else can
        // we know they know what it is, even though we don't
        for (int i = 1; i < 4; i++) assertFalse(copyDrawDeck.getVisibilityOfComponent(0)[i]);
        for (int i = 0; i < 4; i++) assertFalse(copyDrawDeck.getVisibilityOfComponent(1)[i]);
    }

    @Test
    public void moneylender() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MONEYLENDER, 0, DeckType.HAND);
        Moneylender moneylender = new Moneylender(0);
        int startSpend = state.getAvailableSpend(0);
        long copperInHand = state.getDeck(DeckType.HAND, 0).stream()
                .filter(c -> c.cardType() == CardType.COPPER).count();
        fm.next(state, moneylender);
        assertEquals(startSpend + 2, state.getAvailableSpend(0));
        assertEquals(copperInHand - 1L, state.getDeck(DeckType.HAND, 0).stream()
                .filter(c -> c.cardType() == CardType.COPPER).count());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(1, state.getBuysLeft());
        assertEquals(CardType.COPPER, state.getDeck(DeckType.TRASH, -1).get(0).cardType());
    }

    @Test
    public void moneylenderWithoutCopper() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MONEYLENDER, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        do { // remove all COPPER
            state.getDeck(DeckType.HAND, 0).remove(DominionCard.create(CardType.COPPER));
        } while (state.getDeck(DeckType.HAND, 0).stream().anyMatch(c -> c.cardType() == CardType.COPPER));
        Moneylender moneylender = new Moneylender(0);

        fm.next(state, moneylender);
        assertEquals(2, state.getAvailableSpend(0));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        assertEquals(1, state.getBuysLeft());
    }

    @Test
    public void poacherWithNoEmptyPiles() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.POACHER, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.DRAW);
        Poacher poacher = new Poacher(0);
        int startSpend = state.getAvailableSpend(0);
        fm.next(state, poacher);

        assertEquals(startSpend + 1, state.getAvailableSpend(0));
        assertEquals(1, state.getActionsLeft());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getBuysLeft());
        assertFalse(state.isActionInProgress());
    }

    @Test
    public void poacherWithTwoEmptyPiles() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 10; i++) {
            state.removeCardFromTable(CardType.VILLAGE);
            state.removeCardFromTable(CardType.MILITIA);
        }
        state.addCard(CardType.POACHER, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to guarantee one
        Poacher poacher = new Poacher(0);

        fm.next(state, poacher);
        int startSpend = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(startSpend + 1, state.getAvailableSpend(0));
        assertEquals(1, state.getActionsLeft());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(4, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getBuysLeft());
        assertTrue(state.isActionInProgress());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new DiscardCard(CardType.COPPER, 0)));
        assertTrue(availableActions.contains(new DiscardCard(CardType.ESTATE, 0)));

        fm.next(state, new DiscardCard(CardType.COPPER, 0));
        assertFalse(poacher.executionComplete(state));
        assertEquals(Play, state.getGamePhase());
        fm.next(state, new DiscardCard(CardType.ESTATE, 0));
        int finalSpend = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertTrue(poacher.executionComplete(state));
        assertEquals(Play, state.getGamePhase());
        assertEquals(finalSpend + 1, state.getAvailableSpend(0));
        assertEquals(1, state.getActionsLeft());
    }


    @Test
    public void poacherWithTwoEmptyPilesAndOneCardInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        for (int i = 0; i < 10; i++) {
            state.removeCardFromTable(CardType.VILLAGE);
            state.removeCardFromTable(CardType.MILITIA);
        }
        for (int i = 0; i < 5; i++)
            state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.addCard(CardType.POACHER, 0, DeckType.HAND);
        assertEquals(1, state.getDeck(DeckType.HAND, 0).getSize());
        Poacher poacher = new Poacher(0);

        fm.next(state, poacher);
        assertEquals(1, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(4, state.getDeck(DeckType.DRAW, 0).getSize());
        assertTrue(state.isActionInProgress());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(1, availableActions.size());

        fm.next(state, availableActions.get(0));
        assertTrue(poacher.executionComplete(state));
        assertEquals(Play, state.getGamePhase());
        availableActions = fm.computeAvailableActions(state);
        assertEquals(1, availableActions.size());
        assertEquals(new EndPhase(Play), availableActions.get(0));
    }


    @Test
    public void witch() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.WITCH, 0, DeckType.HAND);
        Witch witch = new Witch(0);
        assertEquals(30, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));
        assertTrue(fm.computeAvailableActions(state).contains(witch));

        fm.next(state, witch);

        assertEquals(27, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));
        for (int i = 1; i < 4; i++)
            assertEquals(1, state.cardsOfType(CardType.CURSE, i, DeckType.DISCARD));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void witchWithAMoatAndOneCurse() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.WITCH, 0, DeckType.HAND);
        state.addCard(CardType.MOAT, 1, DeckType.HAND);

        Witch witch = new Witch(0);
        assertEquals(30, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));
        for (int i = 0; i < 29; i++)
            state.removeCardFromTable(CardType.CURSE);
        assertEquals(1, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));

        fm.next(state, witch);
        assertEquals(1, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));
        assertTrue(fm.computeAvailableActions(state).contains(new MoatReaction(1)));
        fm.next(state, new MoatReaction(1));
        assertEquals(0, state.cardsOfType(CardType.CURSE, -1, DeckType.SUPPLY));
        assertEquals(1, state.cardsOfType(CardType.CURSE, 2, DeckType.DISCARD));
        assertEquals(0, state.cardsOfType(CardType.CURSE, 1, DeckType.DISCARD));
        assertEquals(0, state.cardsOfType(CardType.CURSE, 3, DeckType.DISCARD));

        assertEquals(0, state.getCurrentPlayer());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void chapel() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.CHAPEL, 0, DeckType.HAND);
        Chapel chapel = new Chapel(0);

        fm.next(state, chapel);

        fm.next(state, new TrashCard(CardType.COPPER, 0));
        fm.next(state, new TrashCard(CardType.COPPER, 0));
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(2, state.getDeck(DeckType.TRASH, -1).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertFalse(chapel.executionComplete(state));
        assertEquals(chapel, state.currentActionInProgress());

        fm.next(state, new DoNothing());
        assertTrue(chapel.executionComplete(state));
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertNull(state.currentActionInProgress());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void harbingerWithNoDiscard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.HARBINGER, 0, DeckType.HAND);
        Harbinger harbinger = new Harbinger(0);

        fm.next(state, harbinger);

        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(1, state.getActionsLeft());
        assertNull(state.currentActionInProgress());
        assertEquals(Play, state.getGamePhase());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(1, availableActions.size());
        assertEquals(new EndPhase(Play), availableActions.get(0));
    }

    @Test
    public void harbingerWithDiscard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.HARBINGER, 0, DeckType.HAND);
        state.addCard(CardType.HARBINGER, 0, DeckType.DISCARD);
        state.addCard(CardType.SILVER, 0, DeckType.DISCARD);

        Harbinger harbinger = new Harbinger(0);

        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(2, state.getDeck(DeckType.DISCARD, 0).getSize());
        fm.next(state, harbinger);

        assertFalse(harbinger.executionComplete(state));
        assertEquals(4, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(harbinger, state.currentActionInProgress());
        List<AbstractAction> availableActions = fm.computeAvailableActions(state);

        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new DoNothing()));
        assertTrue(availableActions.contains(new MoveCard(CardType.HARBINGER, 0, DeckType.DISCARD, 0, DeckType.DRAW, false)));
        assertTrue(availableActions.contains(new MoveCard(CardType.SILVER, 0, DeckType.DISCARD, 0, DeckType.DRAW, false)));

        fm.next(state, new MoveCard(CardType.SILVER, 0, DeckType.DISCARD, 0, DeckType.DRAW, false));
        assertTrue(harbinger.executionComplete(state));
        assertNull(state.currentActionInProgress());

        assertEquals(1, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(CardType.SILVER, state.getDeck(DeckType.DRAW, 0).peek().cardType());
        PartialObservableDeck<DominionCard> drawDeck = (PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, 0);
        assertTrue(drawDeck.getVisibilityForPlayer(0, 0));
        for (int i = 1; i < 4; i++)
            assertFalse(drawDeck.getVisibilityForPlayer(0, i));

        assertEquals(Play, state.getGamePhase());

        availableActions = fm.computeAvailableActions(state);
        assertEquals(1, availableActions.size());
        assertEquals(new EndPhase(Play), availableActions.get(0));
    }

    @Test
    public void throneRoomWithMarket() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        assertEquals(1, state.getActionsLeft());
        assertEquals(throneRoom, state.currentActionInProgress());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(DominionCard.create(CardType.MARKET).getAction(0), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(1, state.getActionsLeft());
        assertEquals(2, state.getBuysLeft());
        assertEquals(2, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());

        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(DominionCard.create(CardType.MARKET).getAction(0, true), nextActions.get(0));
        assertNotEquals(DominionCard.create(CardType.MARKET).getAction(0, false), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(2, state.getActionsLeft());  // we used our action on th eThrone Room, and then each Market gives +1 Action
        assertEquals(3, state.getBuysLeft());
        assertEquals(2, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
        assertFalse(state.isActionInProgress());
    }


    @Test
    public void throneRoomWithNoActions() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);
        assertEquals(0, state.getActionsLeft());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void throneRoomWithWorkshop() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.WORKSHOP, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        assertEquals(throneRoom, state.currentActionInProgress());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new Workshop(0), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(new Workshop(0), state.currentActionInProgress());
        assertEquals(0, state.getActionsLeft());
        assertEquals(2, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.HAND, 0).getSize());

        nextActions = fm.computeAvailableActions(state);
        assertEquals(11, nextActions.size()); // COPPER, SILVER, ESTATE, CELLAR, MOAT, MERCHANT, VILLAGE, WORKSHOP, MILITIA, REMODEL, SMITHY
        nextActions.forEach(a -> {
                    GainCard gc = (GainCard) a;
                    assertTrue(gc.cardType.cost <= 4);
                    assertEquals(DeckType.DISCARD, gc.destinationDeck);
                }
        );

        fm.next(state, new GainCard(CardType.SILVER, 0));
        assertEquals(throneRoom, state.currentActionInProgress());

        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new Workshop(0, true), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(new Workshop(0, true), state.currentActionInProgress());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(11, nextActions.size());

        fm.next(state, new GainCard(CardType.SILVER, 0));
        assertFalse(state.isActionInProgress());

        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void throneRoomWithMerchant() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        assertEquals(throneRoom, state.currentActionInProgress());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(new Merchant(0), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(throneRoom, state.currentActionInProgress());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(new Merchant(0, true), nextActions.get(0));
        assertEquals(1, state.getActionsLeft());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());

        fm.next(state, nextActions.get(0));
        assertFalse(state.isActionInProgress());
        assertEquals(Play, state.getGamePhase());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(2, state.getActionsLeft());
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());

        fm.next(state, new EndPhase(Play));
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
        int treasureInHand = state.getDeck(DeckType.HAND, 0).sumInt(DominionCard::treasureValue);
        assertEquals(treasureInHand + 2, state.getAvailableSpend(0));
    }

    @Test
    public void throneRoomWithThroneRoomWithSingleMarket() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(throneRoom, state.currentActionInProgress());
        assertSame(throneRoom, state.currentActionInProgress());
        assertEquals(2, nextActions.size());
        assertEquals(DominionCard.create(CardType.THRONE_ROOM).getAction(0), nextActions.get(0));
        assertEquals(DominionCard.create(CardType.MARKET).getAction(0), nextActions.get(1));

        fm.next(state, nextActions.get(0));
        assertEquals(1, state.getActionsLeft());
        assertNotSame(throneRoom, state.currentActionInProgress());
        assertTrue(state.currentActionInProgress() instanceof ThroneRoom);
        // we now have the second throne room controlling the action flow
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        fm.next(state, nextActions.get(0)); // EnthroneMarket - I
        fm.next(state, fm.computeAvailableActions(state).get(0)); // EnthroneMarket - II
        fm.next(state, fm.computeAvailableActions(state).get(0)); // ThroneRoom for a second time
        // we now have no actions for second ThroneRoom - so we should move to buy phase immediately
        assertEquals(Play, state.getGamePhase());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new EndPhase(Play), nextActions.get(0)); // EnthroneMarket - I

        assertEquals(3, state.getBuysLeft());
        assertEquals(3, state.getDeck(DeckType.TABLE, 0).getSize());
        assertEquals(7, state.getDeck(DeckType.HAND, 0).getSize());
    }


    @Test
    public void throneRoomWithThroneRoomAndThenNoCard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);
        // one ThroneRoom on table, one in hand
        assertEquals(1, state.getDeck(DeckType.HAND, 0).stream().filter(c -> c.cardType() == CardType.THRONE_ROOM).count());
        assertEquals(1, state.getDeck(DeckType.TABLE, 0).stream().filter(c -> c.cardType() == CardType.THRONE_ROOM).count());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(DominionCard.create(CardType.THRONE_ROOM).getAction(0, false), nextActions.get(0));

        fm.next(state, nextActions.get(0));
        assertEquals(0, state.getDeck(DeckType.HAND, 0).stream().filter(c -> c.cardType() == CardType.THRONE_ROOM).count());
        assertEquals(2, state.getDeck(DeckType.TABLE, 0).stream().filter(c -> c.cardType() == CardType.THRONE_ROOM).count());
        // playing the second throne room - with no actions left should give us a single Pass action
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new EndPhase(Play), nextActions.get(0));
        fm.next(state, nextActions.get(0)); // EndPhase
        assertFalse(state.isActionInProgress());
        assertEquals(DominionGamePhase.Buy, state.getGamePhase());
    }

    @Test
    public void bandit() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BANDIT, 0, DeckType.HAND);
        // we then put a Silver and Gold on one player, nothing on another, a Copper and Silver on a third and a Moat on the fourth
        state.addCard(CardType.SILVER, 1, DeckType.DRAW);
        state.addCard(CardType.GOLD, 1, DeckType.DRAW);
        state.addCard(CardType.GOLD, 2, DeckType.DRAW);
        state.addCard(CardType.SILVER, 2, DeckType.DRAW);
        state.addCard(CardType.COPPER, 2, DeckType.DRAW);
        state.addCard(CardType.SILVER, 3, DeckType.DRAW);
        state.addCard(CardType.MOAT, 3, DeckType.HAND);

        Bandit bandit = new Bandit(0);
        fm.next(state, bandit);
        assertEquals(bandit, state.currentActionInProgress());
        assertEquals(CardType.GOLD, state.getDeck(DeckType.DISCARD, 0).peek().cardType());
        assertEquals(1, state.getTotal(0, c -> c.cardType().equals(CardType.GOLD) ? 1 : 0));
        assertEquals(1, state.getTotal(1, c -> c.cardType().equals(CardType.GOLD) ? 1 : 0));

        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(2, nextActions.size());
        assertTrue(nextActions.contains(new TrashCard(CardType.SILVER, 1, DeckType.DISCARD)));
        assertTrue(nextActions.contains(new TrashCard(CardType.GOLD, 1, DeckType.DISCARD)));
        fm.next(state, new TrashCard(CardType.GOLD, 1, DeckType.DISCARD));

        assertEquals(1, state.getTotal(0, c -> c.cardType().equals(CardType.GOLD) ? 1 : 0));
        assertEquals(0, state.getTotal(1, c -> c.cardType().equals(CardType.GOLD) ? 1 : 0));
        assertEquals(2, state.getCurrentPlayer());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new TrashCard(CardType.SILVER, 2, DeckType.DISCARD), nextActions.get(0));
        fm.next(state, new TrashCard(CardType.SILVER, 2, DeckType.DISCARD));

        assertEquals(3, state.getCurrentPlayer());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(2, nextActions.size());
        assertTrue(nextActions.contains(new DoNothing()));
        assertTrue(nextActions.contains(new MoatReaction(3)));

        fm.next(state, new MoatReaction(3));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());

        assertEquals(1, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.DISCARD, 1).getSize());
        assertEquals(1, state.getDeck(DeckType.DISCARD, 2).getSize());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 3).getSize());
        assertEquals(2, state.getDeck(DeckType.TRASH, -1).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 1).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 2).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 3).getSize());
    }

    @Test
    public void banditWithOneCardInDrawDeck() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BANDIT, 0, DeckType.HAND);
        for (int i = 0; i < 5; i++)
            state.drawCard(1, DeckType.DRAW, 1, DeckType.DISCARD);
        state.addCard(CardType.SILVER, 1, DeckType.DRAW);
        // p1 now has just one (unique) card in their Draw pile.
        assertEquals(1, state.getDeck(DeckType.DRAW, 1).getSize());
        assertEquals(5, state.getDeck(DeckType.DISCARD, 1).getSize());

        Bandit bandit = new Bandit(0);
        fm.next(state, bandit);

        assertEquals(1, state.getCurrentPlayer());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertTrue(nextActions.contains(new TrashCard(CardType.SILVER, 1, DeckType.DISCARD)));
        fm.next(state, new TrashCard(CardType.SILVER, 1, DeckType.DISCARD));

        assertEquals(1, state.getDeck(DeckType.DISCARD, 1).getSize());
        assertEquals(1, state.getDeck(DeckType.TRASH, 1).getSize());
        assertEquals(4, state.getDeck(DeckType.DRAW, 1).getSize());
    }


    @Test
    public void throneRoomWithBandit() {
        // for this we'll give everyone a SILVER or two. Then check that these have all been trashed, and the right
        // number of cards are in DRAW and DISCARD decks
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BANDIT, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        // we then put a Silver and Gold on one player, nothing on another, a Copper and Silver on a third and a Moat on the fourth
        state.addCard(CardType.SILVER, 1, DeckType.DRAW);
        state.addCard(CardType.SILVER, 2, DeckType.DRAW);
        state.addCard(CardType.COPPER, 2, DeckType.DRAW);
        state.addCard(CardType.SILVER, 2, DeckType.DRAW);
        state.addCard(CardType.SILVER, 3, DeckType.DRAW);
        state.addCard(CardType.MOAT, 3, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        do {
            AbstractAction next = fm.computeAvailableActions(state).get(0);
            fm.next(state, next);
        } while (state.isActionInProgress());

        assertEquals(2, state.getTotal(0, DeckType.DISCARD, c -> c.cardType() == CardType.GOLD ? 1 : 0));
        assertEquals(0, state.getTotal(1, c -> c.cardType() == CardType.SILVER ? 1 : 0));
        assertEquals(0, state.getTotal(2, c -> c.cardType() == CardType.SILVER ? 1 : 0));
        assertEquals(1, state.getTotal(3, c -> c.cardType() == CardType.SILVER ? 1 : 0));
        assertEquals(2, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.DISCARD, 1).getSize());
        assertEquals(2, state.getDeck(DeckType.DRAW, 1).getSize());
        assertEquals(2, state.getDeck(DeckType.DISCARD, 2).getSize());
        assertEquals(4, state.getDeck(DeckType.DRAW, 2).getSize());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 3).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 3).getSize());
    }

    @Test
    public void throneRoomWithMilitiaAndMoat() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MILITIA, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.MOAT, 3, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        do {
            AbstractAction next = fm.computeAvailableActions(state).get(0);
            fm.next(state, next);
        } while (state.isActionInProgress());

        assertEquals(3, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 2).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 3).getSize());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.isEmpty());
        assertEquals(Buy, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }


    @Test
    public void throneRoomWithMilitiaAndFollowOnCardsThatCannotBeUsed() {
        DominionGameState state = (DominionGameState) game.getGameState();
        do {
            AbstractAction next = fm.computeAvailableActions(state).get(0);
            fm.next(state, next);
        } while (state.getCurrentPlayer() == 0);

        state.addCard(CardType.THRONE_ROOM, 1, DeckType.HAND);
        state.addCard(CardType.MOAT, 1, DeckType.HAND);
        state.addCard(CardType.VILLAGE, 1, DeckType.HAND);
        state.addCard(CardType.MILITIA, 1, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(1);
        fm.next(state, throneRoom);

        do {
            AbstractAction next = fm.computeAvailableActions(state).get(0);
            fm.next(state, next);
        } while (state.isActionInProgress());

        assertEquals(3, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 2).getSize());
        assertEquals(3, state.getDeck(DeckType.HAND, 3).getSize());

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.isEmpty());
        assertEquals(Buy, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void bureaucrat() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BUREAUCRAT, 0, DeckType.HAND);
        // in the default set up most players will have at least one ESTATE
        // we ensure that player 2 has no Estates, and that player 3 has an ESTATE and a DUCHY
        state.addCard(CardType.ESTATE, 1, DeckType.HAND);
        while (state.getDeck(DeckType.HAND, 2).stream().anyMatch(DominionCard::isVictoryCard)) {
            state.getDeck(DeckType.HAND, 2).remove(DominionCard.create(CardType.ESTATE));
        }
        int p2HandSize = state.getDeck(DeckType.HAND, 2).getSize();
        state.addCard(CardType.ESTATE, 3, DeckType.HAND);
        state.addCard(CardType.DUCHY, 3, DeckType.HAND);

        Bureaucrat bureaucrat = new Bureaucrat(0);
        fm.next(state, bureaucrat);
        assertEquals(bureaucrat, state.currentActionInProgress());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(CardType.SILVER, state.getDeck(DeckType.DRAW, 0).peek().cardType());

        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new MoveCard(CardType.ESTATE, 1, DeckType.HAND, 1, DeckType.DRAW, true), nextActions.get(0));
        fm.next(state, nextActions.get(0));

        assertEquals(2, state.getCurrentPlayer());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(1, nextActions.size());
        assertEquals(new RevealHand(2), nextActions.get(0));
        fm.next(state, nextActions.get(0));

        assertEquals(3, state.getCurrentPlayer());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(2, nextActions.size());
        assertTrue(nextActions.contains(new MoveCard(CardType.ESTATE, 3, DeckType.HAND, 3, DeckType.DRAW, true)));
        assertTrue(nextActions.contains(new MoveCard(CardType.DUCHY, 3, DeckType.HAND, 3, DeckType.DRAW, true)));
        fm.next(state, nextActions.get(0));

        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isActionInProgress());

        // also need to check that card put on Draw pile is visible (either by attacker or victims)
        for (int pDeck = 0; pDeck < 4; pDeck++) {
            for (int pObs = 0; pObs < 4; pObs++) {
                boolean visibility = ((PartialObservableDeck<DominionCard>) state.getDeck(DeckType.DRAW, pDeck))
                        .getVisibilityForPlayer(0, pObs);
                //            System.out.printf("Deck=%d, Obs=%d : %s%n", pDeck, pObs, visibility);
                if (pDeck != 2)
                    assertTrue(visibility);
                else
                    assertFalse(visibility);
            }
        }
        // and that player 2's hand is fully visible
        for (int i = 0; i < state.getDeck(DeckType.HAND, 2).getSize(); i++)
            for (int j = 0; j < state.getNPlayers(); j++)
                assertTrue(((PartialObservableDeck<DominionCard>) state.getDeck(DeckType.HAND, 2)).getVisibilityForPlayer(i, j));

        // then check that we have correct card counts
        assertEquals(5, state.getDeck(DeckType.HAND, 1).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 1).getSize());
        assertEquals(p2HandSize, state.getDeck(DeckType.HAND, 2).getSize());
        assertEquals(5, state.getDeck(DeckType.DRAW, 2).getSize());
        assertEquals(6, state.getDeck(DeckType.HAND, 3).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 3).getSize());
        assertEquals(CardType.ESTATE, state.getDeck(DeckType.DRAW, 1).peek().cardType());
        assertEquals(CardType.DUCHY, state.getDeck(DeckType.DRAW, 3).peek().cardType());
    }

    @Test
    public void sentryWithNoneKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        Sentry sentry = new Sentry(0);
        fm.next(state, sentry);

        assertEquals(sentry, state.currentActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(1, state.getActionsLeft());
        assertEquals(8, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(2, state.getDeck(DeckType.DRAW, 0).getSize());
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        assertEquals(3, nextActions.size());
        CardType firstCard = state.getDeck(DeckType.HAND, 0).get(1).cardType();
        CardType secondCard = state.getDeck(DeckType.HAND, 0).get(0).cardType();
        assertEquals(new TrashCard(secondCard, 0), nextActions.get(0));
        assertEquals(new DiscardCard(secondCard, 0), nextActions.get(1));
        assertEquals(new DoNothing(), nextActions.get(2));

        fm.next(state, nextActions.get(0));
        nextActions = fm.computeAvailableActions(state);
        assertEquals(new TrashCard(firstCard, 0), nextActions.get(0));
        assertEquals(new DiscardCard(firstCard, 0), nextActions.get(1));
        assertEquals(new DoNothing(), nextActions.get(2));
        fm.next(state, nextActions.get(1));

        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(2, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TRASH, 0).getSize());
        assertEquals(1, fm.computeAvailableActions(state).size());
    }

    @Test
    public void sentryWithOneKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        Sentry sentry = new Sentry(0);
        fm.next(state, sentry);

        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        CardType secondCard = state.getDeck(DeckType.HAND, 0).get(1).cardType();
        fm.next(state, nextActions.get(0));
        nextActions = fm.computeAvailableActions(state);
        fm.next(state, nextActions.get(2));

        assertFalse(state.isActionInProgress());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(3, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(1, state.getDeck(DeckType.TRASH, 0).getSize());
        assertEquals(secondCard, state.getDeck(DeckType.DRAW, 0).peek().cardType());
        assertEquals(1, fm.computeAvailableActions(state).size());
    }

    @Test
    public void sentryWithBothKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.DRAW);
        state.addCard(CardType.SILVER, 0, DeckType.DRAW);
        Sentry sentry = new Sentry(0);
        fm.next(state, sentry);

        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        CardType firstCard = state.getDeck(DeckType.HAND, 0).get(1).cardType();
        CardType secondCard = state.getDeck(DeckType.HAND, 0).get(0).cardType();
        assertEquals(CardType.SILVER, firstCard);
        assertNotSame(CardType.SILVER, secondCard);
        fm.next(state, nextActions.get(2));
        nextActions = fm.computeAvailableActions(state);
        fm.next(state, nextActions.get(2));

        assertTrue(state.isActionInProgress());
        nextActions = fm.computeAvailableActions(state);
        assertEquals(2, nextActions.size());
        assertEquals(new CompositeAction(new MoveCard(secondCard, 0, DeckType.HAND, 0, DeckType.DRAW, false),
                        new MoveCard(firstCard, 0, DeckType.HAND, 0, DeckType.DRAW, false)),
                nextActions.get(0));
        assertEquals(new CompositeAction(new MoveCard(firstCard, 0, DeckType.HAND, 0, DeckType.DRAW, false),
                        new MoveCard(secondCard, 0, DeckType.HAND, 0, DeckType.DRAW, false)),
                nextActions.get(1));
        fm.next(state, nextActions.get(0));

        assertEquals(0, state.getCurrentPlayer());
        assertEquals(6, state.getDeck(DeckType.HAND, 0).getSize());
        assertEquals(6, state.getDeck(DeckType.DRAW, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.DISCARD, 0).getSize());
        assertEquals(0, state.getDeck(DeckType.TRASH, 0).getSize());
        assertEquals(firstCard, state.getDeck(DeckType.DRAW, 0).peek().cardType());
        assertEquals(secondCard, state.getDeck(DeckType.DRAW, 0).peek(1).cardType());
        assertEquals(1, fm.computeAvailableActions(state).size());
    }
}

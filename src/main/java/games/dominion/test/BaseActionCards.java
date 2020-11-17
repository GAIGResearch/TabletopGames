package games.dominion.test;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.*;
import games.dominion.DominionConstants.*;
import games.dominion.actions.*;
import games.dominion.cards.*;
import games.dominion.DominionGameState.*;
import org.junit.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BaseActionCards {


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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
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
        fm.computeAvailableActions(state);
        fm.next(state, cellar);

        fm.computeAvailableActions(state);
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
}

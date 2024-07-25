package games.dominion;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.GameType;
import games.dominion.DominionConstants.DeckType;
import games.dominion.actions.*;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestBaseActionCardsWithCopy {

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
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        fm.next(state, village);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void smithy() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction smithy =  new SimpleAction(CardType.SMITHY, 0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();

        state.addCard(CardType.SMITHY, 0, DeckType.HAND);
        fm.next(state, smithy);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void laboratory() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        DominionAction laboratory =  new SimpleAction(CardType.LABORATORY, 0);
        state.addCard(CardType.LABORATORY, 0, DeckType.HAND);
        fm.next(state, laboratory);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void market() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();

        DominionAction market = new SimpleAction(CardType.MARKET, 0);
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        fm.next(state, market);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void festival() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        DominionAction festival = new SimpleAction(CardType.FESTIVAL, 0);
        state.addCard(CardType.FESTIVAL, 0, DeckType.HAND);
        fm.next(state, festival);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void cellarBase() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        DominionAction cellar = new Cellar(0);
        state.addCard(CardType.CELLAR, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to ensure we have at least one ESTATE and one COPPER
        fm.next(state, cellar);

        fm.computeAvailableActions(state);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void cellarDiscardsAndDraws() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction cellar = new Cellar(0);
        state.addCard(CardType.CELLAR, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to ensure we have at least one ESTATE and one COPPER
        fm.next(state, cellar);

        fm.next(state, new DiscardCard(CardType.ESTATE, 0));

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();

        fm.next(state, new DiscardCard(CardType.COPPER, 0));
        fm.next(state, new DiscardCard(CardType.COPPER, 0));

        fm.next(state, new DoNothing());

        fm.computeAvailableActions(state);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void militiaCausesAllOtherPlayersToDiscardDownToFive() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        DominionAction militia = new Militia(2);
        state.addCard(CardType.MILITIA, 2, DeckType.HAND);
        fm.next(state, militia);
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            fm.next(state, actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
        } while (state.getCurrentPlayer() != 2);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void militiaSkipsPlayersWithThreeOrFewerCards() {
        DominionGameState state = (DominionGameState) game.getGameState();
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        DominionAction militia = new Militia(3);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(0, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(2, DeckType.HAND, 0, DeckType.DISCARD);
        state.drawCard(2, DeckType.HAND, 0, DeckType.DISCARD);

        fm.next(state, militia);
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            fm.next(state, actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
        } while (state.getCurrentPlayer() != 3);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void militiaDoesNothingIfAllPlayersHaveThreeOrFewerCards() {
        DominionGameState state = (DominionGameState) game.getGameState();
        DominionAction militia = new Militia(0);
        state.addCard(CardType.MILITIA, 0, DeckType.HAND);
        for (int i = 1; i < 4; i++) {
            state.drawCard(i, DeckType.HAND, i, DeckType.DISCARD);
            state.drawCard(i, DeckType.HAND, i, DeckType.DISCARD);
        }

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();

        fm.next(state, militia);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void moat() {
        DominionGameState state = (DominionGameState) game.getGameState();
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        DominionAction moat =  new SimpleAction(CardType.MOAT, 0);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        fm.next(state, moat);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void moatDefendsAgainstMilitia() {
        DominionGameState state = (DominionGameState) game.getGameState();
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        state.addCard(CardType.MOAT, 0, DeckType.HAND);
        state.addCard(CardType.MILITIA, 3, DeckType.HAND);
        DominionAction militia = new Militia(3);
        fm.next(state, militia);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        fm.next(state, new MoatReaction(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
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
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new DoNothing());
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void moatDefendsAgainstMilitiaII() {
        DominionGameState state = (DominionGameState) game.getGameState();
        fm.endPlayerTurn(state);
        fm.endPlayerTurn(state);
        DominionAction militia = new Militia(2);
        state.addCard(CardType.MILITIA, 2, DeckType.HAND);
        state.addCard(CardType.MOAT, 1, DeckType.HAND);

        fm.next(state, militia);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        assertEquals(3, state.getCurrentPlayer());
        do {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            Optional<AbstractAction> moatReaction = actionsAvailable.stream().filter(a -> a instanceof MoatReaction).findFirst();
            AbstractAction chosen = moatReaction.orElseGet(() -> actionsAvailable.get(rnd.nextInt(actionsAvailable.size())));
            fm.next(state, chosen);
            assertEquals(startHash, copy.hashCode());
            assertFalse(startHash == state.hashCode());
        } while (state.getCurrentPlayer() != 2);
    }
    private void moveForwardToNextPlayer(DominionGameState state) {
        int startingPlayer = state.getCurrentPlayer();
        while (state.getCurrentPlayer() == startingPlayer)
            fm.next(state, new EndPhase((DominionGameState.DominionGamePhase) state.getGamePhase()));
    }

    @Test
    public void moatDefenceStatusEndsWithTurn() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.setDefended(3);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());
        moveForwardToNextPlayer(state);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
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

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        for (int i = 0; i < 3; i++) {
            List<AbstractAction> actionsAvailable = fm.computeAvailableActions(state);
            AbstractAction moatReaction = actionsAvailable.stream().filter(a -> a instanceof MoatReaction).findFirst().get();
            fm.next(state, moatReaction);
            assertEquals(startHash, copy.hashCode());
            assertFalse(startHash == state.hashCode());
        }
    }

    @Test
    public void remodelForcesATrash() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new TrashCard(CardType.ESTATE, 0));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void remodelWithNoCardsInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.getDeck(DeckType.HAND, 0).clear();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());
        fm.next(state, remodel);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void remodelBuyOptionsCorrectGivenTrashedCard() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.REMODEL, 0, DeckType.HAND);
        state.addCard(CardType.GOLD, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND);
        Remodel remodel = new Remodel(0);
        fm.next(state, remodel);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new TrashCard(CardType.ESTATE, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new GainCard(CardType.SILVER, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void merchantWithNoSilverInHand() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.DISCARD);
        Merchant merchant = new Merchant(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, merchant);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new EndPhase(DominionGameState.DominionGamePhase.Play));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void merchantWithOneSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, merchant);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new EndPhase(DominionGameState.DominionGamePhase.Play));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void merchantWithTwoSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, merchant);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new EndPhase(DominionGameState.DominionGamePhase.Play));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void merchantsWithTwoSilver() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Merchant merchant = new Merchant(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, merchant);
        fm.next(state, merchant);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new EndPhase(DominionGameState.DominionGamePhase.Play));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void workshop() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.WORKSHOP, 0, DeckType.HAND);
        Workshop workshop = new Workshop(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, workshop);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        fm.next(state, availableActions.get(3));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void mine() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        Mine mine = new Mine(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, mine);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new TrashCard(CardType.SILVER, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

        fm.next(state, new GainCard(CardType.GOLD, 0, DeckType.HAND));

        assertEquals(startHash, copy.hashCode());
        assertEquals(midHash, midCopy.hashCode());
    }

    @Test
    public void mineWithNoTreasure() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MINE, 0, DeckType.HAND);
        do { // remove all COPPER
            state.getDeck(DeckType.HAND, 0).remove(DominionCard.create(CardType.COPPER));
        } while (state.getDeck(DeckType.HAND, 0).stream().anyMatch(c -> c.cardType() == CardType.COPPER));

        Mine mine = new Mine(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, mine);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void artisan() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.ARTISAN, 0, DeckType.HAND);
        state.addCard(CardType.VILLAGE, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.HAND); // to make sure there is one
        Artisan artisan = new Artisan(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());
        fm.next(state, artisan);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new GainCard(CardType.MINE, 0, DeckType.HAND));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

        fm.next(state, new MoveCard(CardType.MINE, 0, DeckType.HAND, 0, DeckType.DRAW, false));
        assertEquals(startHash, copy.hashCode());
        assertEquals(midHash, midCopy.hashCode());
    }

    @Test
    public void moneylender() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MONEYLENDER, 0, DeckType.HAND);
        Moneylender moneylender = new Moneylender(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, moneylender);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
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
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, moneylender);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void poacherWithNoEmptyPiles() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.POACHER, 0, DeckType.HAND);
        state.addCard(CardType.ESTATE, 0, DeckType.DRAW);
        Poacher poacher = new Poacher(0);
        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());
        fm.next(state, poacher);
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
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

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, poacher);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new DiscardCard(CardType.COPPER, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

        fm.next(state, new DiscardCard(CardType.ESTATE, 0));

        assertEquals(startHash, copy.hashCode());
        assertEquals(midHash, midCopy.hashCode());
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
        Poacher poacher = new Poacher(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, poacher);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        fm.next(state, availableActions.get(0));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void witch() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.WITCH, 0, DeckType.HAND);
        Witch witch = new Witch(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());
        fm.next(state, witch);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
    }

    @Test
    public void witchWithAMoatAndOneCurse() {
        DominionGameState state = (DominionGameState) gameImprovements.getGameState();
        state.addCard(CardType.WITCH, 0, DeckType.HAND);
        state.addCard(CardType.MOAT, 1, DeckType.HAND);

        Witch witch = new Witch(0);
        for (int i = 0; i < 29; i++)
            state.removeCardFromTable(CardType.CURSE);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, witch);
        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new MoatReaction(1));
        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void throneRoomWithMarket() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, throneRoom);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

    }

    @Test
    public void throneRoomWithWorkshop() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.WORKSHOP, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        fm.next(state, new Workshop(0));
        fm.next(state, new GainCard(CardType.SILVER, 0));

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new Workshop(0, true));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new GainCard(CardType.SILVER, 0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void throneRoomWithMerchant() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.MERCHANT, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void throneRoomWithThroneRoomWithSingleMarket() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.MARKET, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        state.addCard(CardType.THRONE_ROOM, 0, DeckType.HAND);
        ThroneRoom throneRoom = new ThroneRoom(0);
        fm.next(state, throneRoom);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        // we now have the second throne room controlling the action flow

        fm.next(state, fm.computeAvailableActions(state).get(0)); // EnthroneMarket - I

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0)); // EnthroneMarket - II

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0)); // ThroneRoom for a second time

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void banditI() {
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

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, bandit);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new TrashCard(CardType.GOLD, 1, DeckType.DISCARD));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void banditII() {
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
        fm.next(state, new TrashCard(CardType.GOLD, 1, DeckType.DISCARD));

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, new TrashCard(CardType.SILVER, 2, DeckType.DISCARD));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new MoatReaction(3));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void bureaucratI() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BUREAUCRAT, 0, DeckType.HAND);
        // in the default set up most players will have at least one ESTATE
        // we ensure that player 2 has no Estates, and that player 3 has an ESTATE and a DUCHY
        state.addCard(CardType.ESTATE, 1, DeckType.HAND);
        while (state.getDeck(DeckType.HAND, 2).stream().anyMatch(DominionCard::isVictoryCard)) {
            state.getDeck(DeckType.HAND, 2).remove(DominionCard.create(CardType.ESTATE));
        }
        state.addCard(CardType.ESTATE, 3, DeckType.HAND);
        state.addCard(CardType.DUCHY, 3, DeckType.HAND);

        Bureaucrat bureaucrat = new Bureaucrat(0);
        fm.next(state, bureaucrat);
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, nextActions.get(0));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);
        nextActions = fm.computeAvailableActions(state);

        fm.next(state, nextActions.get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

        nextActions = fm.computeAvailableActions(state);
        fm.next(state, nextActions.get(0));
    }

    @Test
    public void bureaucratII() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.BUREAUCRAT, 0, DeckType.HAND);
        // in the default set up most players will have at least one ESTATE
        // we ensure that player 2 has no Estates, and that player 3 has an ESTATE and a DUCHY
        state.addCard(CardType.ESTATE, 1, DeckType.HAND);
        while (state.getDeck(DeckType.HAND, 2).stream().anyMatch(DominionCard::isVictoryCard)) {
            state.getDeck(DeckType.HAND, 2).remove(DominionCard.create(CardType.ESTATE));
        }
        state.addCard(CardType.ESTATE, 3, DeckType.HAND);
        state.addCard(CardType.DUCHY, 3, DeckType.HAND);

        Bureaucrat bureaucrat = new Bureaucrat(0);
        fm.next(state, bureaucrat);
        List<AbstractAction> nextActions = fm.computeAvailableActions(state);
        fm.next(state, nextActions.get(0));

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        nextActions = fm.computeAvailableActions(state);

        fm.next(state, nextActions.get(0));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        nextActions = fm.computeAvailableActions(state);
        fm.next(state, nextActions.get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void sentryWithNoneKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        Sentry sentry = new Sentry(0);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, sentry);

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void sentryWithOneKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        Sentry sentry = new Sentry(0);
        fm.next(state, sentry);

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(2));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void sentryWithBothKept() {
        DominionGameState state = (DominionGameState) game.getGameState();
        state.addCard(CardType.SENTRY, 0, DeckType.HAND);
        state.addCard(CardType.SILVER, 0, DeckType.DRAW);
        state.addCard(CardType.SILVER, 0, DeckType.DRAW);
        Sentry sentry = new Sentry(0);
        fm.next(state, sentry);

        fm.next(state, fm.computeAvailableActions(state).get(2));

        int startHash = state.hashCode();
        DominionGameState copy = (DominionGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(2));

        int midHash = state.hashCode();
        DominionGameState midCopy = (DominionGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }
}

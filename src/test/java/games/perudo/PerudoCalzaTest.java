package games.perudo;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.perudo.actions.Bid;
import games.perudo.actions.Calza;
import games.perudo.actions.Dudo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Calza (PerudoParameters.calza): when it is offered, whose dice it changes, and who starts the next round.
 * Unit tests execute Calza on an arranged direct state; flow tests drive factory games with fm.next.
 */
public class PerudoCalzaTest {

    PerudoGameState state;
    AbstractForwardModel fm;

    /**
     * Direct 3-player state with the given parameters; player 1 (after the bidder, player 0) is to act. Dice: player 0 three (a Paco, two fours), player 1 four (a two, three, four, five), player 2 three
     * (a Paco, a four, a six) = 10 dice. Towards fours: fours 2 + 1 + 1 = 4, Pacos 1 + 0 + 1 = 2, so 6 count.
     */
    private void arrange(PerudoParameters params) {
        params.setRandomSeed(7);
        state = newState(params, 3);
        fm = new PerudoForwardModel();
        setAllDice(state,
                faces(1, 0, 0, 2, 0, 0),
                faces(0, 1, 1, 1, 1, 0),
                faces(1, 0, 0, 1, 0, 1));
        state.setTurnOwner(1);
    }

    private void start(int nPlayers, PerudoParameters params) {
        Game g = newGame(nPlayers, 21, params);
        state = (PerudoGameState) g.getGameState();
        fm = g.getForwardModel();
    }

    /** The actions that are not bids. */
    private static Set<AbstractAction> challenges(List<AbstractAction> actions) {
        Set<AbstractAction> s = new HashSet<>();
        for (AbstractAction a : actions)
            if (!(a instanceof Bid)) s.add(a);
        return s;
    }

    private void assertBidCleared() {
        assertFalse(state.hasBid());
        assertEquals(0, state.getBidFace());
        assertEquals(-1, state.getBidder());
    }

    // ---------------------------------------------------------------- when Calza is offered

    @Test
    public void withCalzaOffCalzaIsNeverOfferedAfterABid() {
        arrange(new PerudoParameters());
        state.setBid(6, 4, 0);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(new Dudo()), challenges(actions));
        assertFalse(actions.contains(new Calza()));
    }

    @Test
    public void withCalzaOnCalzaIsOfferedAlongsideDudoAfterABid() {
        arrange(calzaParams());
        state.setBid(6, 4, 0);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(new Dudo(), new Calza()), challenges(actions));
        // offered once only
        assertEquals(1, actions.stream().filter(a -> a instanceof Calza).count());
    }

    @Test
    public void withCalzaOnCalzaIsOfferedAfterAPacoBidToo() {
        arrange(calzaParams());
        state.setBid(2, 1, 0);
        assertEquals(Set.of(new Dudo(), new Calza()), challenges(fm.computeAvailableActions(state)));
    }

    @Test
    public void withCalzaOnTheOpeningOffersOnlyBids() {
        arrange(calzaParams());
        // no bid yet: 10 dice, opening 1..10 dice of faces 2..6 = 50 bids, no Dudo, no Calza
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new HashSet<>(bidRange(1, 10, 2, 6)), new HashSet<>(actions));
        assertEquals(50, actions.size());
    }

    @Test
    public void calzaIsNotOfferedInAPalificoRound() {
        arrange(calzaPalificoParams());
        state.setPalificoRound(true);
        state.setPalificoUsed(1, true);
        state.setBid(2, 4, 0);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(Set.of(new Dudo()), challenges(actions));
    }

    @Test
    public void withCalzaAndPalificoOnCalzaIsOfferedInANormalRound() {
        arrange(calzaPalificoParams());
        state.setBid(6, 4, 0);
        assertFalse(state.isPalificoRound());
        assertEquals(Set.of(new Dudo(), new Calza()), challenges(fm.computeAvailableActions(state)));
    }

    // ---------------------------------------------------------------- Calza.execute

    @Test
    public void addDieGivesThePlayerOneMoreDie() {
        arrange(calzaParams());
        state.addDie(0);
        // 3 + 1 = 4; total 10 + 1 = 11; the others unchanged
        assertEquals(4, state.getNDice(0));
        assertEquals(4, state.getNDice(1));
        assertEquals(3, state.getNDice(2));
        assertEquals(11, state.getTotalDice());
    }

    @Test
    public void anExactCalzaGivesTheCallerADieCountingPacosAsWild() {
        arrange(calzaParams());
        state.setBid(6, 4, 0);       // fours 4 + Pacos 2 = 6 == 6 (without wild Pacos only 4: wrong)
        new Calza().execute(state);
        assertEquals(4 + 1, state.getNDice(1));
        assertEquals(3, state.getNDice(0));   // the bidder unchanged
        assertEquals(3, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void anExactCalzaOnPacosCountsOnlyThePacos() {
        arrange(calzaParams());
        state.setBid(2, 1, 0);       // Pacos 1 + 0 + 1 = 2 == 2
        new Calza().execute(state);
        assertEquals(4 + 1, state.getNDice(1));
        assertEquals(3, state.getNDice(0));
        assertEquals(3, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
    }

    @Test
    public void aCalzaWhenMoreDiceMatchThanBidCostsTheCallerADie() {
        arrange(calzaParams());
        state.setBid(5, 4, 0);       // 6 > 5: not exact
        new Calza().execute(state);
        assertEquals(4 - 1, state.getNDice(1));
        assertEquals(3, state.getNDice(0));   // the bidder unchanged although the bid was right
        assertEquals(3, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void aCalzaWhenFewerDiceMatchThanBidCostsTheCallerADie() {
        arrange(calzaParams());
        state.setBid(7, 4, 0);       // 6 < 7: not exact
        new Calza().execute(state);
        assertEquals(4 - 1, state.getNDice(1));
        assertEquals(3, state.getNDice(0));   // the bidder unchanged although the bid was wrong
        assertEquals(3, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void anExactCalzaByACallerWithAllTheirDiceGainsNothing() {
        arrange(calzaParams());
        state.setDice(1, faces(0, 1, 1, 1, 1, 1));   // 5 dice = nDicePerPlayer; still 1 four, so 6 count
        state.setBid(6, 4, 0);
        new Calza().execute(state);
        assertEquals(5, state.getNDice(1));
        assertEquals(3, state.getNDice(0));
        assertEquals(3, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
        assertBidCleared();
    }

    @Test
    public void calzaRevealsTheDiceAsAGameEvent() {
        arrange(calzaParams());
        List<Event> events = new ArrayList<>();
        state.addListener(new IGameListener() {
            public void onEvent(Event event) { events.add(event); }
            public void report() { }
            public void setGame(Game game) { }
            public Game getGame() { return null; }
        });
        state.setBid(6, 4, 0);
        new Calza().execute(state);
        assertTrue("no GAME_EVENT logged for the reveal",
                events.stream().anyMatch(e -> e.type == Event.GameEvent.GAME_EVENT));
    }

    // ---------------------------------------------------------------- flow (fm.next)

    @Test
    public void afterAnExactCalzaTheCallerStartsTheNextRoundWithTheDieGained() {
        start(3, calzaParams());
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 4, 0),
                faces(0, 0, 0, 5, 0, 0));
        fm.next(state, new Bid(5, 6));   // sixes 5 + Pacos 0 = 5
        assertTrue(fm.computeAvailableActions(state).contains(new Calza()));
        fm.next(state, new Calza());     // player 1: exact, 4 -> 5
        assertEquals(0 + 1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(5, state.getNDice(0));
        assertEquals(5, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertFalse(state.hasBid());
        // new round opening: 15 dice, faces 2..6, no Calza or Dudo
        assertEquals(new HashSet<>(bidRange(1, 15, 2, 6)), new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void afterAWrongCalzaTheCallerStartsTheNextRoundADieDown() {
        start(3, calzaParams());
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 4, 0),
                faces(0, 0, 0, 5, 0, 0));
        fm.next(state, new Bid(4, 6));   // sixes 5 > 4: right, but not exact
        fm.next(state, new Calza());     // player 1: 4 -> 3; player 0 keeps 5
        assertEquals(0 + 1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(5, state.getNDice(0));
        assertEquals(3, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
    }

    @Test
    public void aWrongCalzaOnTheLastDieEliminatesTheCallerAndTheNextPlayerStillInStarts() {
        start(4, calzaParams());
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 3),
                faces(0, 1, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 5, 0));
        eliminate(state, 2, 0);
        fm.next(state, new Bid(2, 6));   // sixes 3 + Pacos 0 = 3 != 2
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Calza());     // player 1 loses their only die
        assertTrue(state.isNotTerminal());
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(0, state.getNDice(1));
        assertEquals(0, state.getEliminationRound(1));
        // player 1 is out and player 2 already out: player 3 starts round 1
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(0 + 1, state.getRoundCounter());
        assertEquals(3, state.getNDice(0));
        assertEquals(5, state.getNDice(3));
    }

    @Test
    public void aWrongCalzaByTheLastButOnePlayerEndsTheGameWithTheBidderWinning() {
        start(2, calzaParams());
        setAllDice(state, faces(0, 0, 0, 0, 0, 3), faces(0, 1, 0, 0, 0, 0));
        fm.next(state, new Bid(2, 6));   // sixes 3 != 2
        // Calza is allowed with only two players
        assertTrue(fm.computeAvailableActions(state).contains(new Calza()));
        fm.next(state, new Calza());
        assertFalse(state.isNotTerminal());
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals(2, state.getOrdinalPosition(1));
    }

    // ---------------------------------------------------------------- with Palifico (flow)

    @Test
    public void aWrongCalzaToOneDieStartsTheCallersPalificoRoundWithoutCalza() {
        start(3, calzaPalificoParams());
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 2, 0, 0, 0, 0),
                faces(0, 0, 0, 5, 0, 0));
        fm.next(state, new Bid(4, 6));   // sixes 5 + Pacos 0 = 5 != 4
        fm.next(state, new Calza());     // player 1: 2 -> 1, the first time at one die
        assertEquals(1, state.getNDice(1));
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(1));
        assertFalse(state.hasHadPalifico(0));
        // within the Palifico round, after player 1 opens, player 2 may Dudo but not Calza
        fm.next(state, new Bid(1, 3));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(Set.of(new Dudo()), challenges(fm.computeAvailableActions(state)));
    }

    @Test
    public void aCalzaGainNeverStartsAPalificoRound() {
        start(3, calzaPalificoParams());
        // arranged: player 1 at one die without having had Palifico (not reachable from the start with palifico
        // on); the trigger looks at the dice after the Calza, 1 + 1 = 2
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 1, 0, 0, 0, 0),
                faces(0, 0, 0, 5, 0, 0));
        fm.next(state, new Bid(5, 6));   // sixes 5 + Pacos 0 = 5 == 5
        fm.next(state, new Calza());
        assertEquals(2, state.getNDice(1));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertFalse(state.hasHadPalifico(1));
    }

    @Test
    public void aPlayerBackToTwoDiceByCalzaGetsNoSecondPalificoWhenDroppingToOneAgain() {
        start(3, calzaPalificoParams());

        // round 0: player 0 (2 dice) bids wrongly, Dudo drops them to one die -> their Palifico round
        setAllDice(state,
                faces(0, 2, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 5, 0));
        fm.next(state, new Bid(3, 2));   // twos 2 + Pacos 0 = 2 < 3
        fm.next(state, new Dudo());
        assertEquals(1, state.getRoundCounter());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        assertEquals(0, state.getCurrentPlayer());

        // round 1 (Palifico): player 0's bid is right, player 1's Dudo costs player 1 a die
        setAllDice(state,
                faces(0, 0, 1, 0, 0, 0),
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 5, 0));
        fm.next(state, new Bid(1, 3));   // threes 1 >= 1
        fm.next(state, new Dudo());      // player 1: 5 -> 4
        assertEquals(2, state.getRoundCounter());
        assertFalse(state.isPalificoRound());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(4, state.getNDice(1));

        // round 2: player 0 Calzas exactly and is back to 2 dice
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 1),
                faces(1, 1, 1, 1, 0, 0),
                faces(0, 0, 0, 0, 3, 2));
        fm.next(state, new Bid(1, 2));   // player 1
        fm.next(state, new Bid(4, 6));   // player 2; sixes 1 + 0 + 2 = 3, Pacos 0 + 1 + 0 = 1: 4 count
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state).contains(new Calza()));
        fm.next(state, new Calza());     // exact: player 0 1 -> 2
        assertEquals(3, state.getRoundCounter());
        assertEquals(2, state.getNDice(0));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));

        // round 3: player 0 Calzas wrongly and drops to one die again: a player has only one Palifico round
        setAllDice(state,
                faces(0, 2, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 4, 0),
                faces(0, 0, 0, 0, 0, 5));
        fm.next(state, new Bid(1, 3));   // player 0
        fm.next(state, new Bid(2, 3));   // player 1
        fm.next(state, new Bid(3, 3));   // player 2; threes 0 + Pacos 0 = 0 != 3
        fm.next(state, new Calza());     // player 0: 2 -> 1
        assertEquals(4, state.getRoundCounter());
        assertEquals(1, state.getNDice(0));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        assertFalse(state.hasHadPalifico(1));
        assertFalse(state.hasHadPalifico(2));
    }
}

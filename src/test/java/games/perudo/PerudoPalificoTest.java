package games.perudo;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import games.perudo.actions.Bid;
import games.perudo.actions.Dudo;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static games.perudo.PerudoParameters.PACO;
import static games.perudo.PerudoTestUtils.*;
import static org.junit.Assert.*;

/**
 * Palifico (PerudoParameters.palifico): when a Palifico round starts, and the bids and Dudo within one.
 * Unit tests arrange a Palifico round on a direct state; flow tests drive factory games with fm.next.
 */
public class PerudoPalificoTest {

    PerudoGameState state;
    AbstractForwardModel fm;

    private void start(int nPlayers, PerudoParameters params) {
        Game g = params == null ? newGame(nPlayers, 21) : newGame(nPlayers, 21, params);
        state = (PerudoGameState) g.getGameState();
        fm = g.getForwardModel();
    }

    /**
     * Direct 3-player state, palifico on, in player 0's Palifico round (player 0 holds one die, a Paco).
     * Per face over all 11 dice: Pacos 1 + 1 + 2 = 4, twos 1, threes 1, fours 2 + 1 = 3, fives 1, sixes 1.
     */
    private void arrangePalificoRound() {
        PerudoParameters params = palificoParams();
        params.setRandomSeed(3);
        state = newState(params, 3);
        fm = new PerudoForwardModel();
        setAllDice(state,
                faces(1, 0, 0, 0, 0, 0),
                faces(1, 1, 0, 2, 1, 0),
                faces(2, 0, 1, 1, 0, 1));
        state.setPalificoRound(true);
        state.setPalificoUsed(0, true);
    }

    private static Set<AbstractAction> asSet(List<AbstractAction> actions) {
        return new HashSet<>(actions);
    }

    private static Set<AbstractAction> withDudo(Set<Bid> bids) {
        Set<AbstractAction> s = new HashSet<>(bids);
        s.add(new Dudo());
        return s;
    }

    /**
     * Player 0 holds two twos, player 1 five sixes and player 2 five fives (12 dice). Player 0 bids three twos
     * (2 < 3) and player 1 calls Dudo.
     */
    private void dropPlayer0ToOneDie() {
        setAllDice(state,
                faces(0, 2, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 5, 0));
        fm.next(state, new Bid(3, 2));
        fm.next(state, new Dudo());     // player 1 calls rightly; player 0 loses a die and has 1 left
    }

    // ---------------------------------------------------------------- triggering the Palifico round (flow)

    @Test
    public void withPalificoOffADropToOneDieStartsANormalRound() {
        start(3, null);
        dropPlayer0ToOneDie();
        assertEquals(1, state.getNDice(0));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertFalse(state.hasHadPalifico(0));
        // 1 + 5 + 5 = 11 dice; normal opening: 1..11 dice of faces 2..6, no Pacos: 11 * 5 = 55 bids
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new HashSet<>(bidRange(1, 11, 2, 6)), asSet(actions));
        assertEquals(55, actions.size());
    }

    @Test
    public void withPalificoOnTheRoundAfterADropToOneDieIsPalificoStartedByThatPlayer() {
        start(3, palificoParams());
        dropPlayer0ToOneDie();
        assertEquals(1, state.getNDice(0));
        assertEquals(0 + 1, state.getRoundCounter());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        assertFalse(state.hasHadPalifico(1));
        assertFalse(state.hasHadPalifico(2));
        // 1 + 5 + 5 = 11 dice; Palifico opening: 1..11 dice of any face 1..6: 11 * 6 = 66 bids, Pacos included
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new HashSet<>(bidRange(1, 11, 1, 6)), asSet(actions));
        assertEquals(66, actions.size());
    }

    @Test
    public void aCallerDroppingToOneDieStartsTheirPalificoRound() {
        start(3, palificoParams());
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 0, 2),
                faces(0, 0, 0, 0, 5, 0));
        fm.next(state, new Bid(3, 6));  // sixes 5 + 2 = 7 >= 3: a correct bid
        fm.next(state, new Dudo());     // player 1 calls wrongly and drops to 1 die
        assertEquals(1, state.getNDice(1));
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(1));
        assertFalse(state.hasHadPalifico(0));
    }

    @Test
    public void aTwoPlayerGameStillGetsAPalificoRound() {
        start(2, palificoParams());
        setAllDice(state, faces(0, 2, 0, 0, 0, 0), faces(0, 0, 0, 0, 0, 3));
        fm.next(state, new Bid(3, 2));  // twos 2 < 3: wrong
        fm.next(state, new Dudo());     // player 0 drops to 1 die
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        // 1 + 3 = 4 dice: 4 * 6 = 24 opening bids
        assertEquals(new HashSet<>(bidRange(1, 4, 1, 6)), asSet(fm.computeAvailableActions(state)));
    }

    @Test
    public void palificoRoundPlaysThroughWithoutWildPacosAndTheNextRoundIsNormal() {
        start(3, palificoParams());
        dropPlayer0ToOneDie();
        assertTrue(state.isPalificoRound());
        // re-arrange the re-rolled dice: Pacos 4, fours 3 (as in arrangePalificoRound); 11 dice
        setAllDice(state,
                faces(1, 0, 0, 0, 0, 0),
                faces(1, 1, 0, 2, 1, 0),
                faces(2, 0, 1, 1, 0, 1));
        fm.next(state, new Bid(2, 4));                     // player 0 opens on fours
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(withDudo(bidRange(3, 11, 4, 4)), asSet(actions));   // fours only, more of them
        assertTrue(pacoBids(actions).isEmpty());
        fm.next(state, new Bid(4, 4));                     // player 1
        fm.next(state, new Dudo());                        // player 2: fours 3 < 4 (Pacos not wild) -> player 1 loses
        assertEquals(1, state.getNDice(0));
        assertEquals(5 - 1, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        assertFalse(state.hasHadPalifico(1));
    }

    @Test
    public void theRoundAfterAPalificoRoundIsNormalEvenWithTheOneDiePlayerStillOnOneDie() {
        start(3, palificoParams());
        // arranged: player 0's Palifico round, player 0 holds one six
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 1),
                faces(0, 5, 0, 0, 0, 0),
                faces(0, 0, 5, 0, 0, 0));
        state.setPalificoRound(true);
        state.setPalificoUsed(0, true);
        fm.next(state, new Bid(1, 6));  // sixes 1 >= 1: correct
        fm.next(state, new Dudo());     // player 1 calls wrongly: 5 -> 4 dice
        assertEquals(1, state.getNDice(0));
        assertEquals(4, state.getNDice(1));
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
        // normal opening again: 1 + 4 + 5 = 10 dice, faces 2..6: 50 bids, no Pacos
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(new HashSet<>(bidRange(1, 10, 2, 6)), asSet(actions));
        assertEquals(50, actions.size());
    }

    @Test
    public void aPlayerWhoHasHadPalificoDoesNotGetASecondOne() {
        start(3, palificoParams());
        // arranged (a second drop to one die needs Calza): player 0 has had Palifico and holds 2 dice; this is
        // player 2's Palifico round
        setAllDice(state,
                faces(0, 2, 0, 0, 0, 0),
                faces(0, 0, 0, 0, 0, 5),
                faces(0, 0, 0, 0, 0, 1));
        state.setPalificoUsed(0, true);
        state.setPalificoUsed(2, true);
        state.setPalificoRound(true);
        fm.next(state, new Bid(3, 2));  // twos 2 < 3: wrong
        fm.next(state, new Dudo());     // player 0 drops to 1 die again
        assertEquals(1, state.getNDice(0));
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(0));
    }

    @Test
    public void anotherPlayersFirstDropToOneDieAfterAPalificoRoundTriggersTheirs() {
        start(3, palificoParams());
        // arranged: player 0's Palifico round; player 1 holds 2 sixes
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 1),
                faces(0, 0, 0, 0, 0, 2),
                faces(1, 1, 1, 1, 1, 0));
        state.setPalificoRound(true);
        state.setPalificoUsed(0, true);
        fm.next(state, new Bid(3, 6));  // sixes 1 + 2 = 3 >= 3: correct
        fm.next(state, new Dudo());     // player 1 calls wrongly and drops to 1 die
        assertEquals(1, state.getNDice(1));
        assertEquals(1, state.getCurrentPlayer());
        assertTrue(state.isPalificoRound());
        assertTrue(state.hasHadPalifico(1));
        assertTrue(state.hasHadPalifico(0));
        assertFalse(state.hasHadPalifico(2));
    }

    @Test
    public void anEliminationTriggersNoPalificoRound() {
        start(3, palificoParams());
        // arranged: a Palifico round in which player 0 holds one six and has not had Palifico themselves, so only
        // the "exactly one die" condition keeps the elimination from triggering one
        setAllDice(state,
                faces(0, 0, 0, 0, 0, 1),
                faces(0, 5, 0, 0, 0, 0),
                faces(0, 0, 5, 0, 0, 0));
        state.setPalificoRound(true);
        fm.next(state, new Bid(2, 6));  // sixes 1 < 2: wrong
        fm.next(state, new Dudo());     // player 0 loses their last die and is out
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(1, state.getCurrentPlayer());
        assertFalse(state.isPalificoRound());
        assertFalse(state.hasHadPalifico(0));
        assertFalse(state.hasHadPalifico(1));
    }

    // ---------------------------------------------------------------- bidding in a Palifico round (unit)

    @Test
    public void palificoOpeningIsEveryBidOnEveryFaceIncludingPacos() {
        arrangePalificoRound();
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // 11 dice x 6 faces = 66 bids, no Dudo
        assertEquals(new HashSet<>(bidRange(1, 11, 1, 6)), asSet(actions));
        assertEquals(66, actions.size());
    }

    @Test
    public void palificoRaisesKeepTheFaceAndRaiseTheQuantity() {
        arrangePalificoRound();
        state.setBid(4, 3, 0);
        state.setTurnOwner(1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // only 5..11 threes and Dudo: no higher face at 4, no other face at 5+, no Pacos
        assertEquals(withDudo(bidRange(5, 11, 3, 3)), asSet(actions));
        assertEquals(7 + 1, actions.size());
    }

    @Test
    public void palificoRaisesFromAPacoBidAreMorePacosOnly() {
        arrangePalificoRound();
        state.setBid(3, PACO, 0);
        state.setTurnOwner(1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        // 4..11 Pacos and Dudo; not 7+ of another face (2 * 3 + 1) as in a normal round
        assertEquals(withDudo(bidRange(4, 11, PACO, PACO)), asSet(actions));
        assertEquals(8 + 1, actions.size());
    }

    @Test
    public void palificoBidOfEveryDieOnTheFaceLeavesOnlyDudo() {
        arrangePalificoRound();
        state.setBid(11, 3, 0);
        state.setTurnOwner(1);
        assertEquals(List.of(new Dudo()), fm.computeAvailableActions(state));
    }

    @Test
    public void pacosAreNotWildInAPalificoRound() {
        arrangePalificoRound();
        // per face: Pacos 4, twos 1, threes 1, fours 3, fives 1, sixes 1
        int[] expected = {4, 1, 1, 3, 1, 1};
        for (int f = 1; f <= 6; f++)
            assertEquals("face " + f, expected[f - 1], state.countMatching(f));
        // the same dice in a normal round: fours 3 + 4 Pacos = 7
        state.setPalificoRound(false);
        assertEquals(3 + 4, state.countMatching(4));
        assertEquals(4, state.countMatching(PACO));
    }

    @Test
    public void palificoDudoCostsTheBidderWhenOnlyWildPacosWouldMakeTheBid() {
        arrangePalificoRound();
        state.setBid(5, 4, 1);     // fours 3 < 5 (with wild Pacos 7 >= 5 would cost the caller)
        state.setTurnOwner(2);
        new Dudo().execute(state);
        assertEquals(1, state.getNDice(0));
        assertEquals(5 - 1, state.getNDice(1));
        assertEquals(5, state.getNDice(2));
        assertEquals(1, state.getNextRoundStarter());
    }

    @Test
    public void palificoDudoOnAPacoBidCountsThePacos() {
        arrangePalificoRound();
        state.setBid(4, PACO, 1);  // Pacos 4 >= 4: correct, the caller loses
        state.setTurnOwner(2);
        new Dudo().execute(state);
        assertEquals(5, state.getNDice(1));
        assertEquals(5 - 1, state.getNDice(2));
        assertEquals(2, state.getNextRoundStarter());
    }
}

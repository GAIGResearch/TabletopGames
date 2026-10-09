package games.toads;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.toads.actions.PlayDefenderCards;
import games.toads.actions.PlayFieldCard;
import games.toads.actions.PlayFlankCard;
import games.toads.components.ToadCard;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static games.toads.ToadConstants.ToadGamePhase.PLAY;
import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * A Battle in War of the Toads has two steps. The Attacker plays their face-up card; then, at the same time, the
 * Attacker chooses their hidden card and the Defender both their face-up and their hidden card. The choices in that
 * second step can arrive one at a time or as one joint action, and an observation hides the opponent's choices in
 * that step but keeps our own.
 */
public class ToadSimultaneousTest {

    ToadParameters params;
    ToadGameState state;
    ToadForwardModel fm;

    @Before
    public void setUp() {
        params = tacticsParams(933);
        params.setParameterValue("useTactics", false);
        fm = new ToadForwardModel();
        state = newState(params, fm);
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
    }

    private ToadCard card(int player, int index) {
        return state.getPlayerHand(player).get(index);
    }

    @Test
    public void attackerAloneOpensTheBattle() {
        assertEquals(List.of(0), state.getCurrentSimultaneousPlayers());
        assertEquals(0, state.getAttacker());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertTrue(actions.stream().allMatch(a -> a instanceof PlayFieldCard pfc && pfc.playerId == 0));
    }

    @Test
    public void attackerHiddenCardAndBothDefenderCardsAreChosenTogether() {
        fm.next(state, new PlayFieldCard(0, card(0, 0)));
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());
        assertEquals(0, state.getAttacker());
        // the Attacker keeps the turn
        assertEquals(0, state.getCurrentPlayer());

        List<AbstractAction> attacker = fm.computeAvailableActions(state, null, 0);
        assertEquals(3, attacker.size());
        assertTrue(attacker.stream().allMatch(a -> a instanceof PlayFlankCard pfc && pfc.playerId == 0));

        List<AbstractAction> defender = fm.computeAvailableActions(state, null, 1);
        assertEquals(4 * 3, defender.size());
        assertTrue(defender.stream().allMatch(a -> a instanceof PlayDefenderCards pdc && pdc.playerId == 1));
        for (ToadCard field : state.getPlayerHand(1))
            for (ToadCard flank : state.getPlayerHand(1))
                if (field != flank)
                    assertTrue(defender.contains(new PlayDefenderCards(1, field, flank)));
    }

    @Test
    public void actingSetShrinksAsPlayersChoose() {
        fm.next(state, new PlayFieldCard(0, card(0, 0)));

        // the Defender may choose first
        fm.next(state, new PlayDefenderCards(1, card(1, 0), card(1, 1)));
        assertEquals(List.of(0), state.getCurrentSimultaneousPlayers());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(fm.computeAvailableActions(state, null, 1).isEmpty());

        fm.next(state, new PlayFlankCard(0, card(0, 0)));
        // the Battle is resolved, and the Defender attacks next
        assertEquals(1, state.getBattlesFought());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getAttacker());
        assertEquals(List.of(1), state.getCurrentSimultaneousPlayers());
        assertNull(state.getFieldCard(0));
        assertNull(state.getFieldCard(1));
    }

    @Test
    public void jointActionEqualsSequentialApplicationInEitherOrder() {
        List<ToadGameState> results = new ArrayList<>();
        for (int order = 0; order < 3; order++) {
            ToadGameState s = newState(params, fm);
            fm.next(s, new PlayFieldCard(0, s.getPlayerHand(0).get(0)));
            AbstractAction flank = new PlayFlankCard(0, s.getPlayerHand(0).get(1));
            AbstractAction defence = new PlayDefenderCards(1, s.getPlayerHand(1).get(2), s.getPlayerHand(1).get(3));
            switch (order) {
                case 0 -> {
                    fm.next(s, flank);
                    fm.next(s, defence);
                }
                case 1 -> {
                    fm.next(s, defence);
                    fm.next(s, flank);
                }
                default -> fm.next(s, new SimultaneousAction(Map.of(0, flank, 1, defence)));
            }
            results.add(s);
        }
        for (ToadGameState s : results) {
            ToadGameState expected = results.get(0);
            assertEquals(1, s.getBattlesFought());
            assertEquals(1, s.getCurrentPlayer());
            assertEquals(1, s.getAttacker());
            for (int p = 0; p < 2; p++) {
                assertEquals(expected.getScoreInBattle(0, p), s.getScoreInBattle(0, p));
                assertEquals(names(expected.getPlayerHand(p).getComponents()), names(s.getPlayerHand(p).getComponents()));
                assertEquals(names(expected.getDiscards(p).getComponents()), names(s.getDiscards(p).getComponents()));
            }
        }
    }

    private static List<String> names(List<ToadCard> cards) {
        return cards.stream().map(ToadCard::getComponentName).sorted().toList();
    }

    @Test
    public void defenderDoesNotSeeTheAttackersHiddenCard() {
        ToadCard aField = card(0, 0);
        fm.next(state, new PlayFieldCard(0, aField));
        ToadCard aFlank = card(0, 1);
        fm.next(state, new PlayFlankCard(0, aFlank));
        assertEquals(1, state.getCurrentPlayer());

        ToadGameState seenBy1 = (ToadGameState) state.copy(1);
        assertEquals(aField, seenBy1.getFieldCard(0));
        assertNull(seenBy1.getHiddenFlankCard(0));
        assertEquals(List.of(0, 1), seenBy1.getCurrentSimultaneousPlayers());
        assertEquals(1, seenBy1.getCurrentPlayer());
        assertEquals(4 * 3, fm.computeAvailableActions(seenBy1).size());

        // the Attacker still sees their own hidden card, and waits on the Defender
        ToadGameState seenBy0 = (ToadGameState) state.copy(0);
        assertEquals(aFlank, seenBy0.getHiddenFlankCard(0));
        assertEquals(List.of(1), seenBy0.getCurrentSimultaneousPlayers());

        // and the master state is untouched
        assertEquals(aFlank, state.getHiddenFlankCard(0));
        assertEquals(List.of(1), state.getCurrentSimultaneousPlayers());
    }

    @Test
    public void attackerDoesNotSeeTheDefendersCards() {
        fm.next(state, new PlayFieldCard(0, card(0, 0)));
        ToadCard dField = card(1, 0);
        ToadCard dFlank = card(1, 1);
        fm.next(state, new PlayDefenderCards(1, dField, dFlank));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, state.getPlayerHand(1).getSize());  // the face-up card has left the hand; the hidden one has not

        ToadGameState seenBy0 = (ToadGameState) state.copy(0);
        assertNull(seenBy0.getFieldCard(1));
        assertNull(seenBy0.getHiddenFlankCard(1));
        // the face-up card is back in the (redeterminised) hand
        assertEquals(4, seenBy0.getPlayerHand(1).getSize());
        assertEquals(List.of(0, 1), seenBy0.getCurrentSimultaneousPlayers());
        assertEquals(0, seenBy0.getCurrentPlayer());

        // the Defender still sees their own cards, and waits on the Attacker
        ToadGameState seenBy1 = (ToadGameState) state.copy(1);
        assertEquals(dField, seenBy1.getFieldCard(1));
        assertEquals(dFlank, seenBy1.getHiddenFlankCard(1));
        assertEquals(List.of(0), seenBy1.getCurrentSimultaneousPlayers());
        assertTrue(fm.computeAvailableActions(seenBy1, null, 1).isEmpty());

        // and the master state is untouched
        assertEquals(dField, state.getFieldCard(1));
        assertEquals(dFlank, state.getHiddenFlankCard(1));
    }

    @Test
    public void aTurnIsABattleAndARoundIsAWar() {
        // with the opening return and the recycle option on, so that every phase is exercised
        params.setParameterValue("openingReturn", true);
        params.setParameterValue("discardOption", true);
        state = newState(params, fm);
        Random rnd = new Random(42);
        int steps = 0;
        while (state.isNotTerminal()) {
            // the turn moves on only once a Battle has been fought (and its post-battle actions are done),
            // and starts again from 0 with each new War of four Battles
            if (state.getGamePhase() != ToadConstants.ToadGamePhase.POST_BATTLE)
                assertEquals("step " + steps, state.getBattlesFought(), state.getTurnCounter() + 4 * state.getRoundCounter());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())));
            steps++;
        }
        assertEquals(8, state.getBattlesFought());
        assertEquals(1, state.getRoundCounter());
    }

    @Test
    public void randomPlayersFinishTheGameAndHistoryRecordsThePlayer() {
        for (boolean openingReturn : List.of(true, false)) {
            for (long seed = 10; seed < 15; seed++) {
                ToadParameters p = new ToadParameters();
                p.setRandomSeed(seed);
                p.setParameterValue("openingReturn", openingReturn);
                Game g = GameType.WarOfTheToads.createGameInstance(2, seed, p);
                g.reset(List.of(new RandomPlayer(new Random(seed)), new RandomPlayer(new Random(seed + 1))));
                g.run();
                ToadGameState s = (ToadGameState) g.getGameState();
                assertFalse(s.isNotTerminal());
                int defences = 0;
                for (Pair<Integer, AbstractAction> h : s.getHistory()) {
                    if (h.b instanceof PlayFieldCard pfc) assertEquals((int) h.a, pfc.playerId);
                    if (h.b instanceof PlayFlankCard pfc) assertEquals((int) h.a, pfc.playerId);
                    if (h.b instanceof PlayDefenderCards pdc) {
                        assertEquals((int) h.a, pdc.playerId);
                        defences++;
                    }
                }
                // four Battles in each of two Wars
                assertEquals(8, defences);
            }
        }
    }
}

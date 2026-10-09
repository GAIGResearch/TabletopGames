package games.lawnandorder;

import core.actions.AbstractAction;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.Pass;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.lawnandorder.LawnAndOrderGameState.Decision.*;
import static games.lawnandorder.LawnAndOrderGameState.Phase.CONTINUE_OR_PASS;
import static games.lawnandorder.LawnAndOrderGameState.PlayerStatus.*;
import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * The simultaneous choices: which actions each player has in each step, what an action does on its own, and how the
 * turn is handed round the players still to choose before the step is resolved.
 */
public class LawnAndOrderActionsTest {

    LawnAndOrderForwardModel fm;
    LawnAndOrderGameState state;

    @Before
    public void setup() {
        fm = new LawnAndOrderForwardModel();
        state = newState(3, 5);
    }

    private Set<AbstractAction> playEachCardOf(int player) {
        return state.getHand(player).getComponents().stream().map(c -> new PlayObject(player, c)).collect(Collectors.toSet());
    }

    @Test
    public void eachActivePlayerMayPlayAnyCardInHand() {
        for (int p = 0; p < 3; p++) {
            Set<AbstractAction> expected = playEachCardOf(p);
            assertEquals("five distinct cards", 5, expected.size());
            assertEquals(expected, actionsFor(fm, state, p));
        }
        // the one-argument version is for the current player
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(playEachCardOf(0), new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void playingACardPlacesItFaceDownAndLeavesThePlayerNothingToChoose() {
        LawnCard chosen = state.getHand(0).get(2);
        int agendaSize = state.getAgenda().getSize();
        fm.next(state, new PlayObject(0, chosen));

        assertEquals(List.of(chosen), state.getChosenCard(0).getComponents());
        assertEquals(4, state.getHand(0).getSize());
        assertFalse(state.getHand(0).contains(chosen));
        assertEquals(Set.of(), actionsFor(fm, state, 0));
        assertEquals(playEachCardOf(1), actionsFor(fm, state, 1));
        // nothing is revealed until every active player has chosen
        assertEquals(0, state.getLawn(0).getSize());
        assertEquals(0, state.getRevealedRules().getSize());
        assertEquals(agendaSize, state.getAgenda().getSize());
        assertEquals(List.of(1, 2), state.getCurrentSimultaneousPlayers());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void passedAndBustedPlayersHaveNoActions() {
        state.status[1] = PASSED;
        state.status[2] = CEASE_AND_DESIST;
        assertEquals(playEachCardOf(0), actionsFor(fm, state, 0));
        assertEquals(Set.of(), actionsFor(fm, state, 1));
        assertEquals(Set.of(), actionsFor(fm, state, 2));
        assertEquals(List.of(0), state.getCurrentSimultaneousPlayers());

        state.setGamePhase(CONTINUE_OR_PASS);
        assertEquals(Set.of(new Continue(0), new Pass(0)), actionsFor(fm, state, 0));
        assertEquals(Set.of(), actionsFor(fm, state, 1));
        assertEquals(Set.of(), actionsFor(fm, state, 2));
    }

    @Test
    public void continueOrPassIsOfferedOnlyToActivePlayersYetToDecide() {
        state.setGamePhase(CONTINUE_OR_PASS);
        for (int p = 0; p < 3; p++)
            assertEquals(Set.of(new Continue(p), new Pass(p)), actionsFor(fm, state, p));
        state.decisions[1] = CONTINUE;
        state.status[2] = PASSED;
        assertEquals(Set.of(new Continue(0), new Pass(0)), actionsFor(fm, state, 0));
        assertEquals(Set.of(), actionsFor(fm, state, 1));
        assertEquals(Set.of(), actionsFor(fm, state, 2));
    }

    @Test
    public void continueAndPassOnlyRecordTheDecisionUntilEveryoneHasDecided() {
        state.setGamePhase(CONTINUE_OR_PASS);
        int deckSize = state.getDrawDeck().getSize();

        fm.next(state, new Continue(0));
        assertEquals(CONTINUE, state.getDecision(0));
        assertEquals("the card is drawn when the step is resolved", 5, state.getHand(0).getSize());
        assertEquals(deckSize, state.getDrawDeck().getSize());
        assertEquals(1, state.getCurrentPlayer());

        fm.next(state, new Pass(1));
        assertEquals(PASS, state.getDecision(1));
        assertEquals("the pass takes effect when the step is resolved", ACTIVE, state.getStatus(1));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(List.of(2), state.getCurrentSimultaneousPlayers());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
    }

    @Test
    public void theTurnPassesUpwardFromTheCurrentPlayerAndWrapsRound() {
        state = newState(4, 5);
        setAgendaTop(state, rule(REPURPOSED));  // a Standard Rule: at most 1 citation on a 1-card lawn, no bust
        state.setTurnOwner(2);
        LawnCard[] cards = new LawnCard[4];
        for (int p = 0; p < 4; p++)
            cards[p] = state.getHand(p).get(0);

        fm.next(state, new PlayObject(2, cards[2]));
        assertEquals(3, state.getCurrentPlayer());
        fm.next(state, new PlayObject(3, cards[3]));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new PlayObject(0, cards[0]));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getLawn(0).getSize());

        fm.next(state, new PlayObject(1, cards[1]));
        // resolved: every card on its lawn, and the turn with a player who is to decide
        for (int p = 0; p < 4; p++)
            assertEquals(List.of(cards[p]), state.getLawn(p).getComponents());
        assertEquals(CONTINUE_OR_PASS, state.getGamePhase());
        assertEquals(List.of(0, 1, 2, 3), state.getPlayersStillToChoose());
        assertTrue(state.getPlayersStillToChoose().contains(state.getCurrentPlayer()));
    }

    @Test
    public void theTurnSkipsPlayersWhoAreNotChoosing() {
        state = newState(4, 5);
        state.status[3] = PASSED;
        state.setTurnOwner(1);
        fm.next(state, new PlayObject(1, state.getHand(1).get(0)));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new PlayObject(2, state.getHand(2).get(0)));
        assertEquals("player 3 has passed", 0, state.getCurrentPlayer());
    }
}

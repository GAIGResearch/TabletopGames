package games.klaverjassen;

import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.klaverjassen.actions.ChooseTrump;
import games.tricktaking.PlayCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.klaverjassen.KlaverjassenTestUtils.*;
import static games.tricktaking.TrickTakingTestUtils.cardsOf;
import static org.junit.Assert.*;

/**
 * The first decision of a hand: the player on the dealer's left chooses trumps, then leads the first trick.
 */
public class KlaverjassenTrumpChoiceTest {

    KlaverjassenGameState state;
    KlaverjassenForwardModel fm;

    @Before
    public void setup() {
        state = newState(42);
        fm = new KlaverjassenForwardModel();
    }

    private static Set<AbstractAction> allTrumpChoices() {
        return Set.of(new ChooseTrump(Hearts), new ChooseTrump(Diamonds), new ChooseTrump(Clubs), new ChooseTrump(Spades));
    }

    private Set<AbstractAction> playsOfWholeHand(int player) {
        Set<AbstractAction> s = new HashSet<>();
        for (FrenchCard c : state.getPlayerHand(player).getComponents())
            s.add(new PlayCard(c));
        return s;
    }

    @Test
    public void theChooserMustChooseOneOfTheFourSuitsBeforeAnyCardIsPlayed() {
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(allTrumpChoices(), new HashSet<>(actions));
        assertEquals("no duplicate actions", 4, actions.size());
    }

    @Test
    public void choosingTrumpsSetsTheSuitAndTheChooserLeadsWithAnyCard() {
        List<FrenchCard> hand0 = cardsOf(state.getPlayerHand(0));
        fm.next(state, new ChooseTrump(Hearts));

        assertEquals(Hearts, state.getTrumpSuit());
        // no turn change: the chooser leads the first trick, ordered with Hearts as trumps
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(new KlaverjassenCardOrder(Hearts), state.getCurrentTrick().getOrder());
        assertEquals(hand0, cardsOf(state.getPlayerHand(0)));
        for (int p = 0; p < 4; p++)
            assertEquals(8, state.getPlayerHand(p).getSize());

        // the leader may lead any of their 8 cards; trumps are not offered again
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(playsOfWholeHand(0), new HashSet<>(actions));
        assertEquals(8, actions.size());
        assertEquals(0, state.getHandPoints(0) + state.getHandPoints(1));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void eachSuitCanBeChosenAndGivesItsOwnCardOrder() {
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            KlaverjassenGameState s = newState(5);
            fm.next(s, new ChooseTrump(suit));
            assertEquals(suit, s.getTrumpSuit());
            assertEquals(new KlaverjassenCardOrder(suit), s.getCurrentTrick().getOrder());
            assertEquals(0, s.getCurrentPlayer());
        }
    }

    @Test
    public void inALaterHandTheChooserOnTheNewDealersLeftChoosesAndLeads() {
        // the deal passes left: dealer 0, so player 1 chooses trumps and leads
        fm.endRound(state, 1);
        fm.deal(state);
        assertEquals(1, state.getTrumpChooser());   // arrangement guard
        assertEquals(1, state.getCurrentPlayer());

        assertEquals(allTrumpChoices(), new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new ChooseTrump(Spades));
        assertEquals(Spades, state.getTrumpSuit());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(new KlaverjassenCardOrder(Spades), state.getCurrentTrick().getOrder());
        assertEquals(playsOfWholeHand(1), new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void chooseTrumpExecutedDirectlyReplacesTheEmptyTrick() {
        // the setup trick has no trumps yet; ChooseTrump gives a new trick with the chosen order
        assertEquals(new KlaverjassenCardOrder(null), state.getCurrentTrick().getOrder());
        assertTrue(new ChooseTrump(Clubs).execute(state));
        assertEquals(Clubs, state.getTrumpSuit());
        assertEquals(new KlaverjassenCardOrder(Clubs), state.getCurrentTrick().getOrder());
        assertEquals(0, state.getCurrentTrick().getLeader());
        assertEquals(new ArrayList<>(), cardsOf(state.getCurrentTrick()));
    }
}

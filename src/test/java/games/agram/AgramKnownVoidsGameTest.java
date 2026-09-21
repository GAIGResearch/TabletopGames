package games.agram;

import core.AbstractForwardModel;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.FrenchCard.Suite.*;
import static games.agram.AgramTestUtils.*;
import static org.junit.Assert.*;

/**
 * Known voids recorded through real games driven by fm.next, and respected by
 * redeterminised copies.
 */
public class AgramKnownVoidsGameTest {

    private static void assertVoids(AgramGameState state, Set<?>... expected) {
        for (int p = 0; p < expected.length; p++)
            assertEquals("player " + p, expected[p], state.getKnownVoids().get(p));
    }

    @Test
    public void voidsAccumulateOverAThreeTrickGameAndLeadersNeverRecordOne() {
        Game game = newGame(3, 5);
        AgramGameState state = (AgramGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveHand(state, 0, "4H", "6C", "8D");
        giveHand(state, 1, "5C", "7D", "3S");
        giveHand(state, 2, "9H", "AC", "10S");
        assertVoids(state, Set.of(), Set.of(), Set.of());

        // trick 1, led by player 0 in Hearts: player 1 cannot follow; player 2's Nine of Hearts wins
        playCards(state, fm, "4H", "5C");
        assertVoids(state, Set.of(), Set.of(Hearts), Set.of());
        fm.next(state, play("9H"));
        assertVoids(state, Set.of(), Set.of(Hearts), Set.of());
        assertEquals(2, state.getCurrentPlayer());

        // trick 2, led by player 2 in Clubs (a different suit from trick 1 - still no void for the leader);
        // player 0 follows, player 1 (last to play) cannot; the Ace of Clubs wins
        playCards(state, fm, "AC", "6C", "7D");
        assertVoids(state, Set.of(), Set.of(Hearts, Clubs), Set.of());
        assertEquals(2, state.getCurrentPlayer());

        // trick 3, led by player 2 in Spades: player 0 cannot follow, player 1 follows
        playCards(state, fm, "10S", "8D", "3S");
        assertFalse(state.isNotTerminal());
        assertVoids(state, Set.of(Spades), Set.of(Hearts, Clubs), Set.of());
    }

    @Test
    public void randomGamesRecordExactlyTheFailuresToFollowSuitAndCopiesRespectThem() {
        int voidsRecorded = 0, constrainedCopies = 0;
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            for (long seed = 1; seed <= 5; seed++) {
                Game game = newGame(nPlayers, seed);
                AgramGameState state = (AgramGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String label = nPlayers + " players, seed " + seed;

                // the voids expected from the rules: the suit led, for each follower who played another suit
                List<Set<FrenchCard.Suite>> expected = new ArrayList<>();
                for (int p = 0; p < nPlayers; p++)
                    expected.add(EnumSet.noneOf(FrenchCard.Suite.class));

                int steps = 0;
                while (state.isNotTerminal() && steps++ < 100) {
                    int player = state.getCurrentPlayer();
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    PlayCard chosen = (PlayCard) actions.get(rnd.nextInt(actions.size()));
                    FrenchCard.Suite lead = state.getCurrentTrick().getLeadSuit();
                    if (lead != null && chosen.card.suite != lead && expected.get(player).add(lead))
                        voidsRecorded++;
                    fm.next(state, chosen);

                    String at = label + ", step " + steps;
                    for (int p = 0; p < nPlayers; p++)
                        assertEquals(at + ", player " + p, expected.get(p), state.getKnownVoids().get(p));
                    // the recorded voids are true: nobody holds a card of a suit they are known to be void in
                    assertHandsRespectKnownVoids(at, state, state);

                    if (state.isNotTerminal()) {
                        int observer = state.getCurrentPlayer();
                        AgramGameState copy = (AgramGameState) state.copy(observer);
                        assertEquals(at, cardsOf(state.getPlayerHands().get(observer)),
                                cardsOf(copy.getPlayerHands().get(observer)));
                        assertHandsRespectKnownVoids(at + ", redeterminised for " + observer, copy, state);
                        for (int p = 0; p < nPlayers; p++)
                            if (p != observer && !expected.get(p).isEmpty() && copy.getPlayerHands().get(p).getSize() > 0)
                                constrainedCopies++;
                    }
                }
                assertFalse(label + ": game did not end within 100 actions", state.isNotTerminal());
            }
        }
        assertTrue("no player ever failed to follow suit", voidsRecorded > 0);
        assertTrue("no redeterminised copy had a known void to respect", constrainedCopies > 0);
    }
}

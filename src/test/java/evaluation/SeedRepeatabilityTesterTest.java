package evaluation;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static org.junit.Assert.*;

public class SeedRepeatabilityTesterTest {

    /**
     * Chooses by a counter shared between all instances, so a second run of the game (with new agents)
     * carries on from where the first left off, whatever the seed.
     */
    private static class SharedCounterPlayer extends AbstractPlayer {
        static int counter = 0;

        SharedCounterPlayer() {
            super(null, "SharedCounter");
        }

        @Override
        public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
            return possibleActions.get(counter++ % possibleActions.size());
        }

        @Override
        public AbstractPlayer copy() {
            return new SharedCounterPlayer();
        }
    }

    @Test
    public void ticTacToeWithDefaultMCTSIsRepeatable() {
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.TicTacToe, 2, 42);
        assertTrue(result.toString(), result.repeatable());
        assertEquals(2, result.actions().size());
        assertFalse(result.actions().get(0).isEmpty());
    }

    @Test
    public void loveLetterWithRandomPlayersIsRepeatable() {
        // random players ignore the (unseeded) redeterminisation of hidden information in their observations
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.LoveLetter, 3, 123, RandomPlayer::new, 3);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void randomPlayersAreReseeded() {
        // RandomPlayer's own Random is seeded from the clock, so this relies on the tester re-seeding it
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.Dominion, 2, 7, RandomPlayer::new, 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void rootWithRandomPlayersIsRepeatable() {
        // the board's nodes and edges were held in hash collections keyed by component ID
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.Root, 3, 1, RandomPlayer::new, 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void saboteurWithRandomPlayersIsRepeatable() {
        // the deck was built from hash maps keyed by arrays, so in a different order for each new set of parameters
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.Saboteur, 3, 1, RandomPlayer::new, 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void loveLetterIsRepeatableAcrossLaunches() {
        // the deck was built by iterating over a HashMap keyed by card type, whose order changed between JVM launches
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.checkAcrossLaunches(GameType.LoveLetter, 3, 1, "random", -1, 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void dominionIsRepeatableAcrossLaunches() {
        // the cards to buy came from a HashMap keyed by card type
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.checkAcrossLaunches(GameType.Dominion, 3, 1, "random", -1, 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void cantStopWithMCTSIsRepeatable() {
        // the dice rolled on the states MCTS searches came from the unseeded redeterminisationRnd
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.CantStop, 3, 42,
                SeedRepeatabilityTester.agentFactory("", 200), 2);
        assertTrue(result.toString(), result.repeatable());
    }

    @Test
    public void divergenceIsReported() {
        SeedRepeatabilityTester.Result result = SeedRepeatabilityTester.check(GameType.TicTacToe, 2, 42, SharedCounterPlayer::new, 2);
        assertFalse(result.repeatable());
        assertEquals(1, result.divergingRun());
        assertTrue(result.firstDivergence() >= 0);
        assertTrue(result.toString().contains("NOT repeatable"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void sharedAgentIsRejected() {
        AbstractPlayer shared = new RandomPlayer();
        SeedRepeatabilityTester.check(GameType.TicTacToe, 2, 42, () -> shared, 2);
    }
}

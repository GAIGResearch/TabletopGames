package games.diamant;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import games.diamant.cards.DiamantCard;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Diamant as a simultaneous-move game: the acting set is everyone in the cave who has not yet
 * chosen, choices can arrive one at a time or as one joint action, players who have left the cave
 * are never asked, and an observation hides the other players' choices but keeps our own.
 */
public class DiamantSimultaneousTests {

    private static Game newGame(long seed) {
        Game g = GameType.Diamant.createGameInstance(4, seed);
        g.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        return g;
    }

    /** Put treasures on top of the main deck so that the next few turns cannot end the cave. */
    private static void safeDraws(DiamantGameState state, int n) {
        for (int i = 0; i < n; i++)
            state.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Treasure, DiamantCard.HazardType.None, 4));
    }

    @Test
    public void actingSetShrinksAsPlayersChooseAndResetsOnResolution() {
        Game g = newGame(1);
        DiamantGameState state = (DiamantGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        safeDraws(state, 2);

        assertEquals(List.of(0, 1, 2, 3), state.getCurrentSimultaneousPlayers());
        assertEquals(0, state.getCurrentPlayer());

        fm.next(state, new ExitFromCave(0));
        assertEquals(List.of(1, 2, 3), state.getCurrentSimultaneousPlayers());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.actionsPlayed.size());
        assertTrue("turn resolved early", state.playerInCave.get(0));

        fm.next(state, new ContinueInCave(1));
        assertEquals(List.of(2, 3), state.getCurrentSimultaneousPlayers());
        assertEquals(2, state.getCurrentPlayer());

        fm.next(state, new ContinueInCave(2));
        fm.next(state, new ContinueInCave(3));

        // resolved: player 0 has left, choices cleared, and the new turn belongs to those in the cave
        assertEquals(0, state.actionsPlayed.size());
        assertEquals(List.of(1, 2, 3), state.getPlayersInCave());
        assertEquals(List.of(1, 2, 3), state.getCurrentSimultaneousPlayers());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void playersOutOfTheCaveNeverGetTheTurn() {
        Game g = newGame(2);
        DiamantGameState state = (DiamantGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        safeDraws(state, 4);
        int cave = state.nCave;

        fm.next(state, new ExitFromCave(0));
        fm.next(state, new ContinueInCave(1));
        fm.next(state, new ExitFromCave(2));
        fm.next(state, new ContinueInCave(3));
        assertEquals(List.of(1, 3), state.getPlayersInCave());

        // three more turns, always taking whoever holds the turn; only 1 and 3 are ever asked
        for (int turn = 0; turn < 3; turn++) {
            assertEquals(List.of(1, 3), state.getCurrentSimultaneousPlayers());
            for (int i = 0; i < 2; i++) {
                int p = state.getCurrentPlayer();
                assertTrue("player " + p + " has left the cave but holds the turn", List.of(1, 3).contains(p));
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertEquals(List.of(new ContinueInCave(p), new ExitFromCave(p)), actions);
                fm.next(state, new ContinueInCave(p));
            }
            assertEquals(cave, state.nCave);
        }
        // and a departed player has no decision to make
        assertTrue(fm.computeAvailableActions(state, null, 0).isEmpty());
    }

    @Test
    public void jointActionEqualsSequentialApplication() {
        Game g1 = newGame(3);
        Game g2 = newGame(3);
        DiamantGameState joint = (DiamantGameState) g1.getGameState();
        DiamantGameState sequential = (DiamantGameState) g2.getGameState();
        safeDraws(joint, 1);
        safeDraws(sequential, 1);
        assertEquals(joint.getPath().getSize(), sequential.getPath().getSize());

        Map<Integer, AbstractAction> choices = new HashMap<>();
        choices.put(0, new ExitFromCave(0));
        choices.put(1, new ExitFromCave(1));
        choices.put(2, new ContinueInCave(2));
        choices.put(3, new ContinueInCave(3));
        g1.getForwardModel().next(joint, new SimultaneousAction(choices));
        for (int p = 0; p < 4; p++) {
            assertEquals(p, sequential.getCurrentPlayer());
            g2.getForwardModel().next(sequential, choices.get(p));
        }

        for (DiamantGameState s : List.of(joint, sequential)) {
            assertEquals(0, s.actionsPlayed.size());
            assertEquals(List.of(2, 3), s.getPlayersInCave());
            assertEquals(List.of(2, 3), s.getCurrentSimultaneousPlayers());
            assertEquals(2, s.getPath().getSize());
        }
        for (int p = 0; p < 4; p++) {
            assertEquals("hand " + p, sequential.hands.get(p).getValue(), joint.hands.get(p).getValue());
            assertEquals("chest " + p, sequential.treasureChests.get(p).getValue(), joint.treasureChests.get(p).getValue());
        }
        assertEquals(sequential.gemsOnPath, joint.gemsOnPath);
        assertEquals(sequential.getTurnCounter() > 0, joint.getTurnCounter() > 0);
    }

    @Test
    public void observationKeepsOwnChoiceHidesOthersAndOwnsTheTurn() {
        Game g = newGame(4);
        DiamantGameState state = (DiamantGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        assertTrue(state.getCoreGameParameters().partialObservable);

        fm.next(state, new ExitFromCave(0));
        fm.next(state, new ContinueInCave(1));
        assertEquals(2, state.getCurrentPlayer());

        // a player who has chosen sees their own choice, and nobody else's
        DiamantGameState seenBy1 = (DiamantGameState) state.copy(1);
        assertEquals(Set.of(1), seenBy1.actionsPlayed.keySet());
        assertEquals(new ContinueInCave(1), seenBy1.actionsPlayed.get(1));
        assertEquals(1, seenBy1.getCurrentPlayer());
        assertEquals(List.of(0, 2, 3), seenBy1.getCurrentSimultaneousPlayers());

        // a player yet to choose sees no choices at all, and everyone still to move
        DiamantGameState seenBy2 = (DiamantGameState) state.copy(2);
        assertEquals(0, seenBy2.actionsPlayed.size());
        assertEquals(2, seenBy2.getCurrentPlayer());
        assertEquals(List.of(0, 1, 2, 3), seenBy2.getCurrentSimultaneousPlayers());
        assertEquals(List.of(new ContinueInCave(2), new ExitFromCave(2)), fm.computeAvailableActions(seenBy2));

        // the full copy is untouched
        DiamantGameState full = (DiamantGameState) state.copy();
        assertEquals(Set.of(0, 1), full.actionsPlayed.keySet());
        assertEquals(2, full.getCurrentPlayer());
        assertEquals(List.of(2, 3), full.getCurrentSimultaneousPlayers());

        // the master state is unchanged by all of that
        assertEquals(Set.of(0, 1), state.actionsPlayed.keySet());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void newCaveStartsWithAPlayerWhoWasOutsideTheOldOne() {
        Game g = newGame(5);
        DiamantGameState state = (DiamantGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        int cave = state.nCave;

        // player 1 leaves on a safe draw, so that players 0, 2 and 3 are in the cave
        safeDraws(state, 1);
        fm.next(state, new ContinueInCave(0));
        fm.next(state, new ExitFromCave(1));
        fm.next(state, new ContinueInCave(2));
        fm.next(state, new ContinueInCave(3));
        assertEquals(List.of(0, 2, 3), state.getPlayersInCave());
        assertEquals(cave, state.nCave);
        // the turn cycles on from player 3, who chose last, to player 0
        assertEquals(0, state.getCurrentPlayer());

        // now a second Snake on the path collapses the cave when everyone continues
        state.path.add(new DiamantCard(DiamantCard.DiamantCardType.Hazard, DiamantCard.HazardType.Snakes, 0));
        state.gemsOnPath.add(0);
        state.mainDeck.add(new DiamantCard(DiamantCard.DiamantCardType.Hazard, DiamantCard.HazardType.Snakes, 0));
        for (int i = 0; i < 3; i++)
            fm.next(state, new ContinueInCave(state.getCurrentPlayer()));

        assertEquals(cave + 1, state.nCave);
        assertEquals(List.of(0, 1, 2, 3), state.getPlayersInCave());
        assertEquals(List.of(0, 1, 2, 3), state.getCurrentSimultaneousPlayers());
        // the turn goes to player 1, who was outside when the cave collapsed, not to one of the
        // players who chose to continue (see DiamantForwardModel.firstPlayerOfNextCave)
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnCounter());
    }

    @Test
    public void randomPlayersFinishTheGameAndNobodyActsFromOutsideTheCave() {
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            for (long seed = 10; seed < 13; seed++) {
                Game g = GameType.Diamant.createGameInstance(nPlayers, seed);
                List<AbstractPlayer> players = new ArrayList<>();
                for (int p = 0; p < nPlayers; p++) players.add(new RandomPlayer(new Random(seed + p)));
                g.reset(players);
                g.run();
                DiamantGameState state = (DiamantGameState) g.getGameState();
                assertFalse(state.isNotTerminal());
                int actions = 0;
                for (Pair<Integer, AbstractAction> h : state.getHistory()) {
                    int actor;
                    if (h.b instanceof ContinueInCave c) actor = c.playerId;
                    else if (h.b instanceof ExitFromCave e) actor = e.playerId;
                    else throw new AssertionError("unexpected action in history: " + h.b);
                    assertEquals("history records the wrong player", (int) h.a, actor);
                    actions++;
                }
                assertTrue(actions > 0);
            }
        }
    }
}

package games.hearts;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.hearts.actions.Pass;
import games.hearts.actions.Play;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static games.hearts.HeartsGameState.Phase.PASSING;
import static games.hearts.HeartsGameState.Phase.PLAYING;
import static org.junit.Assert.*;

/**
 * Hearts mixes a simultaneous phase (passing) with a sequential one (trick play). Passing goes one
 * card at a time, everyone at once; choices can arrive one at a time or as one joint action; an
 * observation hides which cards the others committed but not how many, and makes the observer the
 * turn owner only while passing lasts. There is no passing at all in every fourth round.
 */
public class HeartsSimultaneousTests {

    private static Game newGame(int nPlayers, long seed) {
        Game g = GameType.Hearts.createGameInstance(nPlayers, seed);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++) players.add(new RandomPlayer(new Random(seed + p)));
        g.reset(players);
        return g;
    }

    private static List<String> names(Deck<FrenchCard> hand) {
        List<String> retValue = new ArrayList<>();
        for (FrenchCard c : hand.getComponents()) retValue.add(c.toString());
        return retValue;
    }

    private static FrenchCard firstCardOf(HeartsGameState s, int player) {
        return s.getPlayerDecks().get(player).get(0);
    }

    /** One passing sub-turn: everyone commits the first card of their hand, in the order the turn goes round. */
    private static void passSubTurn(Game g, HeartsGameState s) {
        for (int i = 0; i < s.getNPlayers(); i++) {
            int p = s.getCurrentPlayer();
            g.getForwardModel().next(s, new Pass(p, firstCardOf(s, p)));
        }
    }

    private static int holderOfStartingCard(HeartsGameState s) {
        FrenchCard startingCard = ((HeartsParameters) s.getGameParameters()).startingCard;
        for (int p = 0; p < s.getNPlayers(); p++)
            if (s.getPlayerDecks().get(p).contains(startingCard)) return p;
        throw new AssertionError("nobody holds " + startingCard);
    }

    private static int playerOf(AbstractAction a) {
        if (a instanceof Pass pass) return pass.playerID;
        if (a instanceof Play play) return play.playerID;
        throw new AssertionError("unexpected action " + a);
    }

    @Test
    public void actingSetIsEveryoneStillToCommitThisSubTurn() {
        Game g = newGame(4, 1);
        HeartsGameState s = (HeartsGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        assertEquals(PASSING, s.getGamePhase());
        assertEquals(List.of(0, 1, 2, 3), s.getCurrentSimultaneousPlayers());
        assertEquals(0, s.getCurrentPlayer());
        assertEquals(13, fm.computeAvailableActions(s, null, 2).size());

        fm.next(s, new Pass(0, firstCardOf(s, 0)));
        assertEquals(List.of(1, 2, 3), s.getCurrentSimultaneousPlayers());
        assertEquals(1, s.getCurrentPlayer());
        // player 0 has committed this sub-turn and has nothing to do until the next
        assertTrue(fm.computeAvailableActions(s, null, 0).isEmpty());
        assertEquals(13, fm.computeAvailableActions(s, null, 1).size());

        fm.next(s, new Pass(1, firstCardOf(s, 1)));
        fm.next(s, new Pass(2, firstCardOf(s, 2)));
        assertEquals(List.of(3), s.getCurrentSimultaneousPlayers());
        assertEquals(3, s.getCurrentPlayer());
        fm.next(s, new Pass(3, firstCardOf(s, 3)));

        // the second sub-turn: everyone again, and the turn cycles on from player 3 to player 0
        assertEquals(PASSING, s.getGamePhase());
        assertEquals(List.of(0, 1, 2, 3), s.getCurrentSimultaneousPlayers());
        assertEquals(0, s.getCurrentPlayer());
        for (int p = 0; p < 4; p++) {
            assertEquals(1, s.pendingPasses.get(p).size());
            assertEquals(12, s.getPlayerDecks().get(p).getSize());
        }

        passSubTurn(g, s);
        assertEquals(PASSING, s.getGamePhase());
        passSubTurn(g, s);

        // resolved: cards handed on, and trick play belongs to the holder of the 2 of clubs alone
        assertEquals(PLAYING, s.getGamePhase());
        int holder = holderOfStartingCard(s);
        assertEquals(holder, s.getCurrentPlayer());
        assertEquals(List.of(holder), s.getCurrentSimultaneousPlayers());
        for (int p = 0; p < 4; p++) {
            assertTrue(s.pendingPasses.get(p).isEmpty());
            assertEquals(13, s.getPlayerDecks().get(p).getSize());
        }
        List<AbstractAction> actions = fm.computeAvailableActions(s);
        assertEquals(1, actions.size());
        assertEquals(new Play(holder, ((HeartsParameters) s.getGameParameters()).startingCard), actions.get(0));
        assertTrue(fm.computeAvailableActions(s, null, (holder + 1) % 4).isEmpty());
    }

    @Test
    public void jointPassesEqualSequentialPasses() {
        Game g1 = newGame(4, 2);
        Game g2 = newGame(4, 2);
        HeartsGameState joint = (HeartsGameState) g1.getGameState();
        HeartsGameState sequential = (HeartsGameState) g2.getGameState();
        for (int p = 0; p < 4; p++)
            assertEquals(names(sequential.getPlayerDecks().get(p)), names(joint.getPlayerDecks().get(p)));

        for (int subTurn = 0; subTurn < 3; subTurn++) {
            Map<Integer, AbstractAction> choices = new HashMap<>();
            for (int p = 0; p < 4; p++) choices.put(p, new Pass(p, firstCardOf(joint, p)));
            g1.getForwardModel().next(joint, new SimultaneousAction(choices));
            for (int p = 0; p < 4; p++) {
                assertEquals(p, sequential.getCurrentPlayer());
                g2.getForwardModel().next(sequential, new Pass(p, firstCardOf(sequential, p)));
            }
        }

        for (HeartsGameState s : List.of(joint, sequential)) {
            assertEquals(PLAYING, s.getGamePhase());
            for (int p = 0; p < 4; p++) assertTrue(s.pendingPasses.get(p).isEmpty());
        }
        for (int p = 0; p < 4; p++)
            assertEquals("hand " + p, names(sequential.getPlayerDecks().get(p)), names(joint.getPlayerDecks().get(p)));
        assertEquals(sequential.getCurrentPlayer(), joint.getCurrentPlayer());
        assertEquals(holderOfStartingCard(sequential), joint.getCurrentPlayer());
    }

    @Test
    public void observationKeepsOwnPassesHidesOthersAndOwnsTheTurnOnlyWhilePassing() {
        Game g = newGame(4, 3);
        HeartsGameState s = (HeartsGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        assertTrue(s.getCoreGameParameters().partialObservable);

        fm.next(s, new Pass(0, firstCardOf(s, 0)));
        FrenchCard passedBy1 = firstCardOf(s, 1);
        fm.next(s, new Pass(1, passedBy1));
        assertEquals(2, s.getCurrentPlayer());
        List<FrenchCard> allCards = allCards(s);

        // a player who has committed sees their own card, and only the number the others committed
        HeartsGameState seenBy1 = (HeartsGameState) s.copy(1);
        assertEquals(List.of(passedBy1), seenBy1.pendingPasses.get(1));
        for (int p = 0; p < 4; p++) {
            assertEquals("pending " + p, s.pendingPasses.get(p).size(), seenBy1.pendingPasses.get(p).size());
            assertEquals("hand " + p, s.getPlayerDecks().get(p).getSize(), seenBy1.getPlayerDecks().get(p).getSize());
        }
        assertEquals(names(s.getPlayerDecks().get(1)), names(seenBy1.getPlayerDecks().get(1)));
        assertEquals(1, seenBy1.getCurrentPlayer());
        assertEquals(List.of(2, 3), seenBy1.getCurrentSimultaneousPlayers());
        assertTrue(fm.computeAvailableActions(seenBy1).isEmpty());
        // nothing has been lost or duplicated in the reshuffle
        List<FrenchCard> seenCards = allCards(seenBy1);
        assertEquals(allCards.size(), seenCards.size());
        assertTrue(seenCards.containsAll(allCards));

        // a player yet to commit is the current player of their own observation, with a full hand of passes
        HeartsGameState seenBy2 = (HeartsGameState) s.copy(2);
        assertEquals(2, seenBy2.getCurrentPlayer());
        assertEquals(List.of(2, 3), seenBy2.getCurrentSimultaneousPlayers());
        List<AbstractAction> actions = fm.computeAvailableActions(seenBy2);
        assertEquals(13, actions.size());
        for (AbstractAction a : actions) assertEquals(2, ((Pass) a).playerID);

        // the full copy hides nothing
        HeartsGameState full = (HeartsGameState) s.copy();
        assertEquals(s.pendingPasses, full.pendingPasses);
        assertEquals(2, full.getCurrentPlayer());
        // and the master state is untouched
        assertEquals(2, s.getCurrentPlayer());
        assertEquals(List.of(passedBy1), s.pendingPasses.get(1));

        // through to trick play: now every seat sees the real current player, not themselves
        fm.next(s, new Pass(2, firstCardOf(s, 2)));
        fm.next(s, new Pass(3, firstCardOf(s, 3)));
        passSubTurn(g, s);
        passSubTurn(g, s);
        assertEquals(PLAYING, s.getGamePhase());
        Random rnd = new Random(11);
        for (int i = 0; i < 6; i++) {
            int current = s.getCurrentPlayer();
            for (int seat = 0; seat < 4; seat++) {
                HeartsGameState obs = (HeartsGameState) s.copy(seat);
                assertEquals("seat " + seat, current, obs.getCurrentPlayer());
                assertEquals(List.of(current), obs.getCurrentSimultaneousPlayers());
                assertEquals(seat == current ? fm.computeAvailableActions(s).size() : 0,
                        fm.computeAvailableActions(obs, null, seat).size());
            }
            List<AbstractAction> legal = fm.computeAvailableActions(s);
            fm.next(s, legal.get(rnd.nextInt(legal.size())));
        }
    }

    private static List<FrenchCard> allCards(HeartsGameState s) {
        List<FrenchCard> retValue = new ArrayList<>(s.getDrawDeck().getComponents());
        for (int p = 0; p < s.getNPlayers(); p++) {
            retValue.addAll(s.getPlayerDecks().get(p).getComponents());
            retValue.addAll(s.pendingPasses.get(p));
        }
        return retValue;
    }

    @Test
    public void noPassingInTheFourthRoundAndEveryActionIsByTheCurrentPlayer() {
        boolean reachedFourthRound = false;
        for (int nPlayers = 3; nPlayers <= 7; nPlayers++) {
            for (long seed = 10; seed < 15; seed++) {
                Game g = newGame(nPlayers, seed);
                HeartsGameState s = (HeartsGameState) g.getGameState();
                AbstractForwardModel fm = g.getForwardModel();
                Random rnd = new Random(seed);
                int round = 0;
                assertEquals(PASSING, s.getGamePhase());
                while (s.isNotTerminal()) {
                    if (s.getRoundCounter() != round) {
                        round = s.getRoundCounter();
                        if (round % 4 == 3) {
                            assertEquals("round " + round + " should not pass", PLAYING, s.getGamePhase());
                            assertEquals(holderOfStartingCard(s), s.getCurrentPlayer());
                            reachedFourthRound = true;
                        } else {
                            assertEquals("round " + round + " should pass", PASSING, s.getGamePhase());
                        }
                    }
                    int p = s.getCurrentPlayer();
                    List<Integer> acting = s.getCurrentSimultaneousPlayers();
                    assertTrue(acting.contains(p));
                    if (s.getGamePhase() == PLAYING) assertEquals(List.of(p), acting);
                    List<AbstractAction> actions = fm.computeAvailableActions(s);
                    assertFalse(actions.isEmpty());
                    AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
                    assertEquals(p, playerOf(chosen));
                    assertEquals(s.getGamePhase() == PASSING, chosen instanceof Pass);
                    fm.next(s, chosen);
                }
            }
        }
        assertTrue("no game reached a fourth round", reachedFourthRound);
    }

    @Test
    public void randomPlayersFinishTheGameThroughTheGameLoop() {
        for (int nPlayers = 3; nPlayers <= 5; nPlayers++) {
            Game g = newGame(nPlayers, 20 + nPlayers);
            g.run();
            HeartsGameState s = (HeartsGameState) g.getGameState();
            assertFalse(s.isNotTerminal());
            int passes = 0;
            for (Pair<Integer, AbstractAction> h : s.getHistory()) {
                assertEquals("history records the wrong player", (int) h.a, playerOf(h.b));
                if (h.b instanceof Pass) passes++;
            }
            // three cards per player in every passing round
            assertEquals(0, passes % (3 * nPlayers));
            assertTrue(passes > 0);
        }
    }
}

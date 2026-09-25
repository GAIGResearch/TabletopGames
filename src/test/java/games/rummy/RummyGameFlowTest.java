package games.rummy;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import games.rummy.actions.LayOff;
import games.rummy.actions.Meld;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.*;
import static games.rummy.RummyTestUtils.*;
import static games.rummy.actions.LayOff.Position.HIGH;
import static org.junit.Assert.*;

/**
 * Real games driven with fm.next: scripted walk-throughs (drawing from each source; melding, laying off and
 * discarding; going out by a meld), and seeded games played to the end with every kind of action.
 */
public class RummyGameFlowTest {

    @Test
    public void twoTurnsDrawingFromEachSource() {
        Game g = newGame(2, 5);
        RummyGameState state = (RummyGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        FrenchCard firstDiscard = state.getDiscardPile().peek();

        // player 0 draws from the draw deck and discards the card drawn
        FrenchCard drawn = state.getDrawDeck().peek();
        fm.next(state, new DrawCard(false));
        assertEquals(11, state.getPlayerHand(0).getSize());
        assertEquals(31 - 1, state.getDrawDeck().getSize());
        assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Discard(drawn));
        assertEquals(10, state.getPlayerHand(0).getSize());
        assertEquals(List.of(drawn, firstDiscard), state.getDiscardPile().getComponents());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertEquals(1, state.getTurnCounter());

        // player 1 takes that card, may not discard it, and discards another
        fm.next(state, new DrawCard(true));
        assertEquals(drawn, state.getTakenCard());
        assertEquals(List.of(firstDiscard), state.getDiscardPile().getComponents());
        List<AbstractAction> discards = fm.computeAvailableActions(state).stream()
                .filter(a -> a instanceof Discard).toList();
        assertEquals(10, discards.size());   // 11 cards less the taken one
        assertFalse(discards.contains(new Discard(drawn)));
        FrenchCard other = state.getPlayerHand(1).getComponents().stream().filter(c -> !c.equals(drawn)).findFirst().orElseThrow();
        fm.next(state, new Discard(other));
        assertEquals(List.of(other, firstDiscard), state.getDiscardPile().getComponents());
        assertTrue(state.getPlayerHand(1).contains(drawn));
        assertNull(state.getTakenCard());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertEquals(2, state.getTurnCounter());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void aTurnWithAMeldALayOffAndADiscard() {
        Game g = newGame(2, 7);
        RummyGameState state = (RummyGameState) g.getGameState();
        RummyForwardModel fm = (RummyForwardModel) g.getForwardModel();
        giveHand(state, 0, "5H", "6H", "7H", "9S", "9D", "9C", "KC", "2D", "JD", "4C");
        // player 1: ranks all different, and no three of a suit in sequence
        giveHand(state, 1, "AS", "3D", "6S", "8C", "10D", "QC", "KS", "8H", "4S", "JH");
        setDrawDeck(state, "4H", "8D", "10S", "2C", "5S");
        setDiscardPile(state, "3S");

        // player 0 draws the 4H, and may meld the runs 4-5-6, 5-6-7 or 4-5-6-7 of Hearts, or the set of 9s
        fm.next(state, new DrawCard(false));
        assertEquals(Set.of(meld("4H", "5H", "6H"), meld("5H", "6H", "7H"), meld("4H", "5H", "6H", "7H"),
                        meld("9D", "9C", "9S")),
                actionSet(state, fm).stream().filter(a -> a instanceof Meld).collect(Collectors.toSet()));
        fm.next(state, meld("4H", "5H", "6H"));
        // the 7H now fits above the 6H; the 9s may not be melded in the same turn
        assertEquals(union(discards("7H", "9S", "9D", "9C", "KC", "2D", "JD", "4C"), layOff("7H", HIGH)),
                actionSet(state, fm));
        fm.next(state, layOff("7H", HIGH));
        fm.next(state, new Discard(card("9S")));

        assertEquals(List.of(cards("4H", "5H", "6H", "7H")),
                state.getMelds().stream().map(Deck::getComponents).toList());
        assertEquals(Set.copyOf(cards("9D", "9C", "KC", "2D", "JD", "4C")), setOf(state.getPlayerHand(0)));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
        assertFalse(state.hasMeldedThisTurn());
        assertEquals(1, state.getTurnCounter());

        // player 1 draws the 8D (a set 8D 8H 8C) and lays the 8H off onto player 0's run, which breaks up the set
        fm.next(state, new DrawCard(false));
        assertTrue(actionSet(state, fm).containsAll(Set.of(meld("8D", "8H", "8C"), layOff("8H", HIGH))));
        fm.next(state, layOff("8H", HIGH));
        assertEquals(cards("4H", "5H", "6H", "7H", "8H"), meldCards(state, 0));
        assertEquals(discards("AS", "3D", "6S", "8C", "10D", "QC", "KS", "4S", "JH", "8D"), actionSet(state, fm));
        fm.next(state, new Discard(card("8D")));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(2, state.getTurnCounter());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void aPlayerGoesOutByMeldingTheTakenDiscard() {
        Game g = newGame(2, 8);
        RummyGameState state = (RummyGameState) g.getGameState();
        RummyForwardModel fm = (RummyForwardModel) g.getForwardModel();
        giveHand(state, 0, "9D", "9C", "9S", "QC", "3H");
        giveHand(state, 1, "AS", "3D", "6S", "8C", "10D", "QD", "KS", "2H", "4S", "JC");
        setDrawDeck(state, "KC", "5D", "7S", "2C", "10H");
        setDiscardPile(state, "3S");

        // player 0 draws the KC, melds the 9s and discards the 3H, keeping QC KC
        fm.next(state, new DrawCard(false));
        fm.next(state, meld("9S", "9D", "9C"));
        fm.next(state, new Discard(card("3H")));
        // player 1 draws the 5D and discards the JC
        fm.next(state, new DrawCard(false));
        fm.next(state, new Discard(card("JC")));
        // player 0 takes the JC and melds J-Q-K of Clubs, emptying the hand: no discard
        fm.next(state, new DrawCard(true));
        assertTrue(actionSet(state, fm).contains(meld("JC", "QC", "KC")));
        fm.next(state, meld("JC", "QC", "KC"));

        assertFalse(state.isNotTerminal());
        assertEquals(List.of(cards("9D", "9C", "9S"), cards("JC", "QC", "KC")),
                state.getMelds().stream().map(Deck::getComponents).toList());
        assertEquals(0, state.getPlayerHand(0).getSize());
        assertEquals(3, state.getTurnCounter());
        assertNull(state.getTakenCard());
        assertFalse(state.hasMeldedThisTurn());
        // the game ended on the empty hand, with 7S 2C 10H still on top of the draw deck
        assertEquals(cards("7S", "2C", "10H"), state.getDrawDeck().peek(0, 3));
        // player 1: AS 3D 6S 8C 10D QD KS 2H 4S 5D = 1 + 3 + 6 + 8 + 10 + 10 + 10 + 2 + 4 + 5 = 59
        assertEquals(0.0, state.getGameScore(0), 0.0);
        assertEquals(-59.0, state.getGameScore(1), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    /** What happened in a random game. */
    private record Played(int turns, int melds, int layOffs, boolean wentOut) {
    }

    /** Plays a seeded game to the end, checking the rules at each step and the results at the end. */
    private Played playRandomGame(Game g, long seed, int maxTurns, boolean allActions) {
        RummyGameState state = (RummyGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        int n = state.getNPlayers();
        int handSize = ((RummyParameters) state.getGameParameters()).handSize(n);
        int[] putDown = new int[n];   // cards each player has melded or laid off
        Random rnd = new Random(seed);
        int steps = 0, turns = 0, discardPileTakes = 0, melds = 0, layOffs = 0;
        boolean wentOut = false;
        AbstractAction last = null;
        while (state.isNotTerminal() && steps++ < 2000) {
            assertAllCardsPresent(state);
            for (RummyMeld m : state.getMelds())
                assertTrue("not a valid meld on the table: " + m.getComponents(), isMeldByRule(m.getComponents()));
            int player = state.getCurrentPlayer();
            Deck<FrenchCard> hand = state.getPlayerHand(player);
            List<AbstractAction> all = fm.computeAvailableActions(state);
            if (state.getGamePhase() == RummyGameState.Phase.DRAW) {
                // a turn starts only while the draw deck has cards and the turn cap is not reached
                assertTrue("draw deck empty at the start of a turn", state.getDrawDeck().getSize() > 0);
                assertTrue("a turn beyond the cap", turns < maxTurns);
                Set<AbstractAction> draws = state.getDiscardPile().getSize() > 0
                        ? Set.of(new DrawCard(false), new DrawCard(true)) : Set.of(new DrawCard(false));
                assertEquals(draws, new HashSet<>(all));
                assertEquals("the player to act", turns % n, player);
                assertNull(state.getTakenCard());
                assertFalse(state.hasMeldedThisTurn());
                for (int p = 0; p < n; p++)
                    assertEquals("hand size of " + p, handSize - putDown[p], state.getPlayerHand(p).getSize());
            } else {
                long nDiscards = all.stream().filter(a -> a instanceof Discard).count();
                // the taken card is barred only while it is still in hand, not after it was melded or laid off
                boolean takenBarred = state.getTakenCard() != null && hand.contains(state.getTakenCard()) && hand.getSize() > 1;
                assertEquals(hand.getSize() - (takenBarred ? 1 : 0), nDiscards);
                for (AbstractAction a : all) {
                    if (a instanceof Meld m) {
                        assertFalse("a second meld offered", state.hasMeldedThisTurn());
                        assertTrue("meld cards not in hand: " + m, hand.getComponents().containsAll(m.cards));
                        assertTrue("not a valid meld: " + m, isMeldByRule(m.cards));
                    } else if (a instanceof LayOff l) {
                        assertTrue("lay-off card not in hand: " + l, hand.contains(l.card));
                    } else {
                        assertTrue("unexpected action in PLAY: " + a, a instanceof Discard);
                    }
                }
            }
            AbstractAction chosen;
            if (allActions) {
                // a kind of action (draw, meld, lay-off, discard) uniformly among those available, then an action of
                // that kind, so melds and lay-offs are taken often
                List<Class<?>> kinds = all.stream().<Class<?>>map(Object::getClass).distinct().toList();
                Class<?> kind = kinds.get(rnd.nextInt(kinds.size()));
                List<AbstractAction> ofKind = all.stream().filter(kind::isInstance).toList();
                chosen = ofKind.get(rnd.nextInt(ofKind.size()));
            } else {
                // draws and discards only, so no hand empties
                List<AbstractAction> basic = all.stream().filter(a -> a instanceof DrawCard || a instanceof Discard).toList();
                chosen = basic.get(rnd.nextInt(basic.size()));
            }
            if (chosen.equals(new DrawCard(true))) discardPileTakes++;
            if (chosen instanceof Meld m) {
                melds++;
                putDown[player] += m.cards.size();
            }
            if (chosen instanceof LayOff) {
                layOffs++;
                putDown[player]++;
            }
            int turnCounter = state.getTurnCounter();
            fm.next(state, chosen);
            boolean handEmptied = state.getPlayerHand(player).getSize() == 0;
            if (chosen instanceof Discard || handEmptied) {
                turns++;
                assertEquals("the turn counter", turnCounter + 1, state.getTurnCounter());
            } else if (!(chosen instanceof DrawCard)) {
                // a meld or lay-off with cards left: the same player goes on
                assertEquals(RummyGameState.Phase.PLAY, state.getGamePhase());
                assertEquals(player, state.getCurrentPlayer());
                assertTrue(state.isNotTerminal());
            }
            if (handEmptied) {
                wentOut = true;
                assertFalse("the game goes on after a hand empties", state.isNotTerminal());
            }
            last = chosen;
        }
        assertFalse("the game did not end within 2000 actions", state.isNotTerminal());
        assertTrue("guard: the discard pile was never taken", discardPileTakes > 0);
        assertAllCardsPresent(state);
        assertFalse("the game ended at a draw", last instanceof DrawCard);
        assertTrue("ended with cards to draw, before the cap and with no empty hand",
                wentOut || state.getDrawDeck().getSize() == 0 || turns == maxTurns);

        // results: the fewest points in hand wins, shared if tied
        int[] points = new int[n];
        int fewest = Integer.MAX_VALUE;
        for (int p = 0; p < n; p++) {
            points[p] = pointsByRule(state.getPlayerHand(p));
            fewest = Math.min(fewest, points[p]);
            assertEquals(-points[p], state.getGameScore(p), 0.0);
        }
        int atFewest = 0;
        for (int p = 0; p < n; p++) if (points[p] == fewest) atFewest++;
        for (int p = 0; p < n; p++) {
            GameResult expected = points[p] != fewest ? LOSE_GAME : atFewest > 1 ? DRAW_GAME : WIN_GAME;
            assertEquals("result of player " + p, expected, state.getPlayerResults()[p]);
        }
        return new Played(turns, melds, layOffs, wentOut);
    }

    @Test
    public void twoPlayerGamesArePlayedToTheEndWithMeldsAndLayOffs() {
        int melds = 0, layOffs = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Played played = playRandomGame(newGame(2, seed), seed, 200, true);
            melds += played.melds();
            layOffs += played.layOffs();
            // unless a hand empties, at least 31 draws empty the draw deck of 52 - 20 - 1 = 31 cards
            assertTrue("seed " + seed + ": " + played.turns() + " turns", played.wentOut() || played.turns() >= 31);
        }
        assertTrue("guard: no meld was laid down in 5 games", melds > 0);
        assertTrue("guard: no card was laid off in 5 games", layOffs > 0);
    }

    @Test
    public void aFourPlayerGameIsPlayedToTheEnd() {
        Played played = playRandomGame(newGame(4, 12), 12, 200, true);
        assertTrue(played.wentOut() || played.turns() >= 52 - 28 - 1);
    }

    @Test
    public void aSixPlayerGameIsPlayedToTheEnd() {
        Played played = playRandomGame(newGame(6, 13), 13, 200, true);
        assertTrue(played.wentOut() || played.turns() >= 52 - 36 - 1);
    }

    @Test
    public void aGameStopsAtTheTurnCap() {
        RummyParameters params = new RummyParameters();
        params.setParameterValue("maxTurnsPerDeal", 10);
        Game g = newGame(2, 14, params);
        // draws and discards only, so no hand empties before the cap
        Played played = playRandomGame(g, 14, 10, false);
        assertEquals(10, played.turns());
        RummyGameState state = (RummyGameState) g.getGameState();
        // at most 10 of the 31 cards drawn
        assertTrue(state.getDrawDeck().getSize() >= 21);
    }
}

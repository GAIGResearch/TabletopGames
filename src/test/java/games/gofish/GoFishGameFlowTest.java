package games.gofish;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.gofish.actions.GoFishAsk;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;

import static core.CoreConstants.GameResult.*;
import static games.gofish.GoFishTestUtils.*;
import static org.junit.Assert.*;

/**
 * Real games driven by fm.next: scripted sequences of turns (the rules; what each ask reveals and the known voids),
 * and seeded random games played to the end.
 */
public class GoFishGameFlowTest {

    @Test
    public void scriptedTurnsWithAsksDrawsAndABook() {
        Game game = newGame(3, 17);
        GoFishGameState state = (GoFishGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        giveHand(state, 0, card("5H"), card("9C"));
        giveHand(state, 1, card("5S"), card("5C"), card("KD"));
        giveHand(state, 2, card("9D"), card("7H"), card("2C"));
        stackDrawDeck(state, card("8S"), card("9S"), card("9H"));
        int deck = 52 - 2 - 3 - 3;   // 44
        assertEquals(deck, state.drawDeck.getSize());
        assertEquals(0, state.getCurrentPlayer());

        // 1. P0 asks P1 for 5s: gets 5S 5C and goes again
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(cards("5H", "9C", "5S", "5C"), cardSet(state.playerHands.get(0)));
        assertEquals(cards("KD"), cardSet(state.playerHands.get(1)));
        assertEquals(0, state.getCurrentPlayer());

        // 2. P0 asks P2 for 5s: Go fish, draws the 8S, turn to P1
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(cards("5H", "9C", "5S", "5C", "8S"), cardSet(state.playerHands.get(0)));
        assertEquals(deck - 1, state.drawDeck.getSize());
        assertEquals(1, state.getCurrentPlayer());

        // 3. P1 holds only a King: it may ask P0 or P2 for Kings, nothing else
        assertEquals(Set.of(new GoFishAsk(0, 13), new GoFishAsk(2, 13)),
                new HashSet<>(fm.computeAvailableActions(state)));
        fm.next(state, new GoFishAsk(2, 13));    // Go fish: draws the 9S, turn to P2
        assertEquals(cards("KD", "9S"), cardSet(state.playerHands.get(1)));
        assertEquals(2, state.getCurrentPlayer());

        // 4. P2 asks P0 for 9s: gets the 9C, goes again
        fm.next(state, new GoFishAsk(0, 9));
        assertEquals(cards("9D", "7H", "2C", "9C"), cardSet(state.playerHands.get(2)));
        assertEquals(cards("5H", "5S", "5C", "8S"), cardSet(state.playerHands.get(0)));
        assertEquals(2, state.getCurrentPlayer());

        // 5. P2 asks P1 for 9s: gets the 9S, goes again (three 9s, no book yet)
        fm.next(state, new GoFishAsk(1, 9));
        assertEquals(cards("9D", "7H", "2C", "9C", "9S"), cardSet(state.playerHands.get(2)));
        assertEquals(cards("KD"), cardSet(state.playerHands.get(1)));
        assertEquals(0, state.playerBooks.get(2).getSize());
        assertEquals(2, state.getCurrentPlayer());

        // 6. P2 asks P0 for 7s: Go fish, draws the 9H - the fourth 9, laid down at once. Not the rank asked for,
        //    so the turn passes to the left, from P2 round to P0
        fm.next(state, new GoFishAsk(0, 7));
        assertEquals(cards("9D", "9C", "9S", "9H"), cardSet(state.playerBooks.get(2)));
        assertEquals(cards("7H", "2C"), cardSet(state.playerHands.get(2)));
        assertEquals(1.0, state.getGameScore(2), 0.0);
        assertEquals(deck - 3, state.drawDeck.getSize());
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomGamesConserveCardsAndEndAsSoonAsAHandOrTheDrawDeckIsEmpty() {
        int games = 0;
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 5; seed++) {
                Game game = newGame(nPlayers, seed);
                GoFishGameState state = (GoFishGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String id = nPlayers + " players, seed " + seed;
                int steps = 0;
                while (state.isNotTerminal() && steps++ < 1000) {
                    // while the game runs, no hand and not the draw deck may be empty
                    assertTrue(id + ": draw deck empty in a running game", state.drawDeck.getSize() > 0);
                    for (int p = 0; p < nPlayers; p++) {
                        assertTrue(id + ": hand " + p + " empty in a running game", state.playerHands.get(p).getSize() > 0);
                        double h = state.getHeuristicScore(p);
                        assertTrue(id + ": heuristic " + h, h >= 0.0 && h <= 1.0);
                    }
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    assertAllCardsPresent(state);
                    assertBooksWellFormed(state);
                }
                assertFalse(id + ": game did not end within 1000 actions", state.isNotTerminal());
                boolean emptyHand = false;
                for (int p = 0; p < nPlayers; p++)
                    if (state.playerHands.get(p).getSize() == 0) emptyHand = true;
                assertTrue(id + ": ended with no empty hand and cards to draw", emptyHand || state.drawDeck.getSize() == 0);

                // result oracle: most books wins; a tie for most is a draw for those tied; everyone else loses
                int most = 0;
                for (int p = 0; p < nPlayers; p++) most = Math.max(most, state.playerBooks.get(p).getSize() / 4);
                int nMost = 0;
                for (int p = 0; p < nPlayers; p++) if (state.playerBooks.get(p).getSize() / 4 == most) nMost++;
                for (int p = 0; p < nPlayers; p++) {
                    int books = state.playerBooks.get(p).getSize() / 4;
                    assertEquals(id + ": score of " + p, books, state.getGameScore(p), 0.0);
                    CoreConstants.GameResult expected = books < most ? LOSE_GAME : (nMost > 1 ? DRAW_GAME : WIN_GAME);
                    assertEquals(id + ": result of " + p, expected, state.getPlayerResults()[p]);
                }
                games++;
            }
        }
        assertEquals(25, games);
    }

    @Test
    public void scriptedTurnsRevealCardsAndRecordKnownVoidsThatConstrainRedeterminisation() {
        Game game = newGame(3, 17);
        GoFishGameState state = (GoFishGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        GoFishKnownVoids kv = state.getKnownVoids();
        giveHand(state, 0, card("5H"), card("9C"));
        giveHand(state, 1, card("5S"), card("5C"), card("KD"));
        giveHand(state, 2, card("9D"), card("7H"), card("2C"));
        stackDrawDeck(state, card("8S"), card("KS"), card("9S"));
        int deck = 52 - 2 - 3 - 3;   // 44
        assertEquals(deck, state.drawDeck.getSize());
        assertEquals(0, state.getCurrentPlayer());

        // 1. P0 asks P1 for 5s: gets 5S 5C, goes again. The 5H (rule 1) and the 5S 5C (rule 2) are shown; P1 void in 5
        fm.next(state, new GoFishAsk(1, 5));
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(3, visibleToAllOfRank(state, 0, 5));
        assertVisibleOnlyToOwner(state, 0, card("9C"));
        assertEquals(Set.of(5), kv.get(1));

        // 2. P0 asks P2 for 5s: Go fish. Nothing more shown (a 5 is already shown); the 8S drawn is private.
        //    P2 void in 5; P0 drew, so P0's voids (none) are forgotten. Turn to P1
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(3, visibleToAllOfRank(state, 0, 5));
        assertVisibleOnlyToOwner(state, 0, card("8S"));
        assertVisibleOnlyToOwner(state, 0, card("9C"));
        assertEquals(Set.of(5), kv.get(2));
        assertEquals(Set.of(), kv.get(0));

        // 3. P1 asks P2 for Kings: Go fish, draws the KS - the rank asked for: shown, and P1 goes again. The KD is
        //    shown too (rule 1). P2 void in 5 and K; P1 drew, so P1's void in 5 is forgotten
        fm.next(state, new GoFishAsk(2, 13));
        assertEquals(1, state.getCurrentPlayer());
        assertVisibleToAll(state, 1, card("KD"));
        assertVisibleToAll(state, 1, card("KS"));
        assertEquals(Set.of(5, 13), kv.get(2));
        assertEquals(Set.of(), kv.get(1));

        // 4. P1 asks P0 for Kings: Go fish, draws the 9S (private), turn to P2. P0 void in K
        fm.next(state, new GoFishAsk(0, 13));
        assertEquals(2, state.getCurrentPlayer());
        assertVisibleOnlyToOwner(state, 1, card("9S"));
        assertVisibleToAll(state, 1, card("KD"));
        assertVisibleToAll(state, 1, card("KS"));
        assertEquals(Set.of(13), kv.get(0));
        assertEquals(Set.of(), kv.get(1));

        // 5. P2 asks P0 for 9s: gets the 9C, goes again. The 9D and 9C are shown. P0 void in K and 9; P2 keeps its
        //    voids in 5 and K (the cards received are public)
        fm.next(state, new GoFishAsk(0, 9));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(cards("5H", "5S", "5C", "8S"), cardSet(state.playerHands.get(0)));
        assertEquals(cards("9D", "7H", "2C", "9C"), cardSet(state.playerHands.get(2)));
        assertVisibleToAll(state, 2, card("9D"));
        assertVisibleToAll(state, 2, card("9C"));
        assertVisibleOnlyToOwner(state, 2, card("7H"));
        assertEquals(Set.of(13, 9), kv.get(0));
        assertEquals(Set.of(5, 13), kv.get(2));
        assertEquals(deck - 3, state.drawDeck.getSize());
        assertTrue(state.isNotTerminal());

        // From P1's view: P0's one hidden card (the 8S) is never a K or 9 and P2's two hidden cards (7H 2C) are
        // never a 5 or K. Unconstrained, P2's 2 slots would take one of the 3 hidden 5s/Ks (5D KH KC) in ~1 copy in 8
        for (int i = 0; i < 200; i++) {
            GoFishGameState copy = (GoFishGameState) state.copy(1);
            for (FrenchCard c : copy.playerHands.get(0).getComponents())
                assertTrue("copy " + i + ": P0 holds " + c, c.number != 13 && c.number != 9);
            for (FrenchCard c : copy.playerHands.get(2).getComponents())
                assertTrue("copy " + i + ": P2 holds " + c, c.number != 5 && c.number != 13);
            assertEquals("copy " + i + ": P0's shown 5s", 3, visibleToAllOfRank(copy, 0, 5));
            assertEquals(cardSet(state.playerHands.get(1)), cardSet(copy.playerHands.get(1)));
        }
    }

    @Test
    public void inRandomGamesKnownVoidsAreAlwaysTrueAndCopiesRespectThem() {
        int voidsSeen = 0;
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 3; seed++) {
                Game game = newGame(nPlayers, seed);
                GoFishGameState state = (GoFishGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String id = nPlayers + " players, seed " + seed;
                int steps = 0;
                while (state.isNotTerminal() && steps++ < 1000) {
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));
                    for (int p = 0; p < nPlayers; p++) {
                        Set<Integer> voids = state.getKnownVoids().get(p);
                        voidsSeen += voids.size();
                        for (FrenchCard c : state.playerHands.get(p).getComponents())
                            assertFalse(id + ", step " + steps + ": P" + p + " holds " + c + " but is known void in it",
                                    voids.contains(c.number));
                    }
                    if (!state.isNotTerminal()) break;
                    int observer = state.getCurrentPlayer();
                    GoFishGameState copy = (GoFishGameState) state.copy(observer);
                    assertAllCardsPresent(copy);
                    for (int p = 0; p < nPlayers; p++) {
                        assertEquals(id + ": hand size " + p, state.playerHands.get(p).getSize(), copy.playerHands.get(p).getSize());
                        for (FrenchCard c : copy.playerHands.get(p).getComponents())
                            assertFalse(id + ", step " + steps + ": copy for P" + observer + " gives P" + p + " " + c
                                    + " though known void in it", state.getKnownVoids().get(p).contains(c.number));
                    }
                    assertEquals(id + ": observer's own hand", state.playerHands.get(observer).getComponents(),
                            copy.playerHands.get(observer).getComponents());
                }
                assertFalse(id + ": game did not end within 1000 actions", state.isNotTerminal());
            }
        }
        assertTrue("some known voids were recorded (else the checks above are vacuous)", voidsSeen > 0);
    }

    @Test
    public void playUntilAllBooksScriptedTurnsPlayOnPastEmptyHandsAndTheEmptyDrawDeck() {
        Game game = newGame(3, 17, playUntilAllBooks());
        GoFishGameState state = (GoFishGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        assertTrue(((GoFishParameters) state.getGameParameters()).playUntilAllBooks);
        // kept ranks 5, 9, K; the other 10 ranks booked: P0 3, P1 4, P2 3 (40 cards). Hands 2 + 1 + 4, draw deck 5
        giveBooks(state, 0, 2, 3, 4);
        giveBooks(state, 1, 6, 7, 8, 10);
        giveBooks(state, 2, 11, 12, 14);
        giveHand(state, 0, card("5H"), card("9H"));
        giveHand(state, 1, card("5S"));
        giveHand(state, 2, card("9S"), card("9C"), card("KH"), card("KS"));
        stackDrawDeck(state, card("9D"), card("KC"), card("5C"), card("5D"), card("KD"));
        assertEquals(5, state.drawDeck.getSize());
        assertAllCardsPresent(state);
        assertEquals(0, state.getCurrentPlayer());

        // 1. P0 asks P1 for 5s: gets the 5S, P1's last card. The game goes on; P0 goes again
        fm.next(state, new GoFishAsk(1, 5));
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(0, state.playerHands.get(1).getSize());

        // 2. P0 asks P2 for 5s: Go fish, draws the 9D; the turn passes to P1, who has no cards and draws the KC
        fm.next(state, new GoFishAsk(2, 5));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(cards("5H", "5S", "9H", "9D"), cardSet(state.playerHands.get(0)));
        assertEquals(cards("KC"), cardSet(state.playerHands.get(1)));
        assertVisibleOnlyToOwner(state, 1, card("KC"));
        assertEquals(5 - 2, state.drawDeck.getSize());

        // 3. P1 asks P2 for Kings: gets the KH KS, goes again
        fm.next(state, new GoFishAsk(2, 13));
        assertEquals(1, state.getCurrentPlayer());
        // 4. P1 asks P0 for Kings: Go fish, draws the 5C; the turn passes to P2
        fm.next(state, new GoFishAsk(0, 13));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(cards("KC", "KH", "KS", "5C"), cardSet(state.playerHands.get(1)));

        // 5. P2 asks P0 for 9s: gets the 9H 9D and books the 9s, emptying P2's hand; P2 goes again, so draws the 5D
        fm.next(state, new GoFishAsk(0, 9));
        assertEquals(2, state.getCurrentPlayer());
        assertEquals((3 + 1) * 4, state.playerBooks.get(2).getSize());
        assertEquals(cards("5D"), cardSet(state.playerHands.get(2)));
        assertEquals(5 - 4, state.drawDeck.getSize());

        // 6. P2 asks P0 for 5s: gets the 5H 5S, P0's last cards. P1 and P2 hold cards: the game goes on
        fm.next(state, new GoFishAsk(0, 5));
        assertTrue(state.isNotTerminal());
        assertEquals(0, state.playerHands.get(0).getSize());
        assertEquals(2, state.getCurrentPlayer());

        // 7. P2 asks P1 for 5s: gets the 5C and books the 5s, emptying P2's hand; P2 goes again and draws the KD,
        //    the last card
        fm.next(state, new GoFishAsk(1, 5));
        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(cards("KD"), cardSet(state.playerHands.get(2)));
        assertEquals(0, state.drawDeck.getSize());
        assertEquals("P0 has no cards: not a target", Set.of(new GoFishAsk(1, 13)),
                new HashSet<>(fm.computeAvailableActions(state)));

        // 8. P2 asks P1 for Kings: gets the KC KH KS and books the Kings. No-one holds cards: the game is over
        fm.next(state, new GoFishAsk(1, 13));
        assertFalse(state.isNotTerminal());
        assertAllCardsPresent(state);
        // books 3, 4, 3 + 3 = 6: P2 wins
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void playUntilAllBooksRandomGamesKeepTheInvariantsAndEndWhenAtMostOnePlayerHoldsCards() {
        int games = 0, ruleEnds = 0, maxRoundEnds = 0;
        int emptyDeckGoFish = 0, emptyHandDraws = 0, skips = 0;
        for (int nPlayers = 2; nPlayers <= 6; nPlayers++) {
            for (long seed = 1; seed <= 3; seed++) {
                Game game = newGame(nPlayers, seed, playUntilAllBooks());
                GoFishGameState state = (GoFishGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                int maxRounds = state.getGameParameters().getMaxRounds();
                Random rnd = new Random(seed);
                String id = nPlayers + " players, seed " + seed;
                int steps = 0;
                while (state.isNotTerminal() && steps++ < 50000) {
                    int actor = state.getCurrentPlayer();
                    // while the game runs, the player to act has cards and so does at least one other player
                    assertTrue(id + ": current player " + actor + " has no cards", state.playerHands.get(actor).getSize() > 0);
                    int holders = 0;
                    for (int p = 0; p < nPlayers; p++) {
                        if (state.playerHands.get(p).getSize() > 0) holders++;
                        double h = state.getHeuristicScore(p);
                        assertTrue(id + ": heuristic " + h, h >= 0.0 && h <= 1.0);
                    }
                    assertTrue(id + ": " + holders + " players hold cards in a running game", holders >= 2);

                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    GoFishAsk ask = (GoFishAsk) actions.get(rnd.nextInt(actions.size()));
                    boolean goFish = !state.playerHasRank(ask.targetPlayer, ask.rank);
                    int deckBefore = state.drawDeck.getSize();
                    if (goFish && deckBefore == 0) emptyDeckGoFish++;
                    fm.next(state, ask);
                    // every card drawn beyond the asker's Go fish card was drawn by an empty-handed player
                    emptyHandDraws += deckBefore - state.drawDeck.getSize() - (goFish && deckBefore > 0 ? 1 : 0);
                    int now = state.getCurrentPlayer();
                    if (state.isNotTerminal() && now != actor && now != (actor + 1) % nPlayers) skips++;

                    assertAllCardsPresent(state);
                    assertBooksWellFormed(state);
                    for (int p = 0; p < nPlayers; p++)
                        for (FrenchCard c : state.playerHands.get(p).getComponents())
                            assertFalse(id + ", step " + steps + ": P" + p + " holds " + c + " but is known void in it",
                                    state.getKnownVoids().get(p).contains(c.number));
                    if (!state.isNotTerminal()) break;
                    int observer = state.getCurrentPlayer();
                    GoFishGameState copy = (GoFishGameState) state.copy(observer);
                    assertAllCardsPresent(copy);
                    for (int p = 0; p < nPlayers; p++)
                        for (FrenchCard c : copy.playerHands.get(p).getComponents())
                            assertFalse(id + ", step " + steps + ": copy for P" + observer + " gives P" + p + " " + c
                                    + " though known void in it", state.getKnownVoids().get(p).contains(c.number));
                    assertEquals(id + ": observer's own hand", state.playerHands.get(observer).getComponents(),
                            copy.playerHands.get(observer).getComponents());
                }
                assertFalse(id + ": game did not end within 50000 actions", state.isNotTerminal());

                int holders = 0, books = 0;
                for (int p = 0; p < nPlayers; p++) {
                    if (state.playerHands.get(p).getSize() > 0) holders++;
                    books += state.playerBooks.get(p).getSize() / 4;
                }
                if (holders <= 1) ruleEnds++;                       // 13 books means no-one holds cards
                else {
                    assertEquals(id + ": ended with " + holders + " holders and " + books + " books, before maxRounds",
                            maxRounds, state.getRoundCounter());
                    maxRoundEnds++;
                }

                // result oracle: most books wins; a tie for most is a draw for those tied; everyone else loses
                int most = 0;
                for (int p = 0; p < nPlayers; p++) most = Math.max(most, state.playerBooks.get(p).getSize() / 4);
                int nMost = 0;
                for (int p = 0; p < nPlayers; p++) if (state.playerBooks.get(p).getSize() / 4 == most) nMost++;
                for (int p = 0; p < nPlayers; p++) {
                    int b = state.playerBooks.get(p).getSize() / 4;
                    CoreConstants.GameResult expected = b < most ? LOSE_GAME : (nMost > 1 ? DRAW_GAME : WIN_GAME);
                    assertEquals(id + ": result of " + p, expected, state.getPlayerResults()[p]);
                }
                games++;
            }
        }
        assertEquals(15, games);
        assertEquals(games, ruleEnds + maxRoundEnds);
        assertTrue("most games end by the rules, not maxRounds: " + ruleEnds + " of " + games, ruleEnds * 2 > games);
        // the variant's special situations must occur, else the checks above say little about them
        assertTrue("some Go fish on an empty draw deck", emptyDeckGoFish > 0);
        assertTrue("some empty-handed player drew at the start of a turn", emptyHandDraws > 0);
        assertTrue("some empty-handed player was skipped", skips > 0);
    }
}

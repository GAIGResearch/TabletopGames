package games.schwimmen;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.schwimmen.actions.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.LongFunction;

import static core.CoreConstants.GameResult.*;
import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * The chips game (livesGame = true) through the forward model: each way a deal ends is scored and the next deal
 * starts (dealer rotation, dealing only to players still in, play from the new dealer's left), swimming and
 * dropping out, players who are out skipped in turns / pass counts / the close round, the end of the game (last
 * player in, all dropping out together, maxDeals), and random games to the end.
 * <p>
 * Standard 3-player position (dealer 2, player 0 to play): 16 hearts, 9 diamonds, 10 clubs (Q D and 10 C tie at 10;
 * clubs is the higher) - player 1 is the unique worst hand. The fourth hand, when used, is 21 clubs.
 */
public class SchwimmenChipsGameFlowTest {

    static final String[] TABLE = h("AH", "7C", "10D");
    static final String[] HAND0 = h("7H", "9H", "KS");   // 7 + 9 = 16 hearts
    static final String[] HAND1 = h("8C", "9D", "7S");   // 9 diamonds - the worst
    static final String[] HAND2 = h("QD", "10C", "8S");  // 10 (diamonds and clubs)
    static final String[] HAND3 = h("AC", "KC", "7D");   // 11 + 10 = 21 clubs

    SchwimmenForwardModel fm;

    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
    }

    /**
     * Runs the scenario on successive ordinary seeds until the next deal it reaches is ordinary too: a Schnauz or
     * Feuer dealt in the new deal would be scored at once and start yet another deal (roundCounter 2). The new deal
     * is shuffled inside the forward model, so no arrangement can fix it. Fails after 30 seeds.
     */
    private SchwimmenGameState withOrdinaryNextDeal(int nPlayers, LongFunction<SchwimmenGameState> scenario) {
        long seed = -1;
        for (int tries = 0; tries < 30; tries++) {
            seed = ordinarySeed(nPlayers, seed + 1);
            SchwimmenGameState state = scenario.apply(seed);
            if (state.getRoundCounter() <= 1) return state;
        }
        fail("every next deal in 30 seeds held a Schnauz or Feuer");
        return null;
    }

    private SchwimmenGameState livesState(SchwimmenParameters params, int nPlayers, long seed) {
        return newState(params, nPlayers, seed, fm);
    }

    private void passAndClose(SchwimmenGameState state) {
        fm.next(state, new Pass());
        fm.next(state, new Close(true));
    }

    /** The standard 3-player position, 3 chips each: player 0 passes and closes, players 1 and 2 pass. */
    private SchwimmenGameState closedStandardDeal(SchwimmenParameters params, long seed) {
        SchwimmenGameState state = livesState(params, 3, seed);
        arrangePlay(state, 0, TABLE, HAND0, HAND1, HAND2);
        setChips(state, 3, 3, 3);
        passAndClose(state);
        fm.next(state, new Pass());
        fm.next(state, new Pass());
        return state;
    }

    /** Asserts the position straight after a new deal: the dealer to choose, cards only for players still in. */
    private void assertNewDeal(SchwimmenGameState state, int dealer) {
        int n = state.getNPlayers(), in = 0;
        assertTrue("the game goes on", state.isNotTerminal());
        assertEquals("dealer", dealer, state.getDealer());
        assertEquals("the dealer's choice comes first", dealer, state.getCurrentPlayer());
        assertTrue(state.isDealerChoicePending());
        assertEquals(-1, state.getCloser());
        assertEquals(0, state.getConsecutivePasses());
        assertFalse(state.isActionInProgress());
        for (int p = 0; p < n; p++) {
            int expected = state.isInGame(p) ? 3 : 0;
            if (state.isInGame(p)) in++;
            assertEquals("hand of player " + p, expected, state.getPlayerHand(p).getSize());
        }
        assertEquals(3, state.getExtraHand().getSize());
        assertEquals(0, state.getTable().getSize());
        assertEquals(0, state.getDiscardPile().getSize());
        assertEquals("draw deck: 32 - 3 x " + in + " - 3", 32 - 3 * in - 3, state.getDrawDeck().getSize());
        assertAllCardsPresent(state);
    }

    // ---- a deal ended by a close ----

    @Test
    public void aClosedDealCostsTheWorstHandAChipAndTheNextDealStarts() {
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> closedStandardDeal(livesParams(), seed));
        // player 1 (9 diamonds) loses one chip: 3 - 1 = 2
        assertEquals(3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals("one deal played", 1, state.getRoundCounter());
        // the deal passes from player 2 to player 0 on the left
        assertNewDeal(state, 0);
        assertEquals("the dealer's choice", List.of(new ChooseHand(false), new ChooseHand(true)),
                fm.computeAvailableActions(state));
        fm.next(state, new ChooseHand(false));
        assertEquals("play starts on the new dealer's left", 1, state.getCurrentPlayer());
        assertEquals(11, fm.computeAvailableActions(state).size());
    }

    @Test
    public void aSchnauzOrFeuerDealtInTheNextDealIsScoredAtOnce() {
        // the next deal is shuffled inside the forward model, so search seeds for one whose next deal gives a player
        // Schnauz or Feuer; after every seed no player may be left holding one while the game goes on
        boolean found = false;
        long seed = -1;
        for (int tries = 0; tries < 2000 && !found; tries++) {
            seed = ordinarySeed(3, seed + 1);
            SchwimmenGameState state = closedStandardDeal(livesParams(), seed);
            if (state.isNotTerminal())
                assertFalse("seed " + seed + ": a special hand left in play", anyPlayerSpecial(state));
            if (state.getRoundCounter() >= 2) {
                found = true;
                // the closed deal cost player 1 a chip, and the special deal cost at least one more chip in all
                int total = state.getChips(0) + state.getChips(1) + state.getChips(2);
                assertTrue("seed " + seed + ": chips " + total, total <= 9 - 1 - 1);
            }
        }
        assertTrue("no seed dealt a Schnauz or Feuer in the next deal", found);
    }

    @Test
    public void aSwimmingPlayerWithTheWorstHandDropsOutAndIsDealtNoCards() {
        // 4 players, dealer 3, player 1 swimming (0 chips) with the worst hand
        SchwimmenGameState state = withOrdinaryNextDeal(4, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 4, seed);
            arrangePlay(s, 0, TABLE, HAND0, HAND1, HAND2, HAND3);
            setChips(s, 3, 0, 3, 3);
            passAndClose(s);
            for (int i = 0; i < 3; i++)
                fm.next(s, new Pass());
            return s;
        });
        // player 1 drops out with 3 players left in: -1 - 3 = -4
        assertEquals(-4, state.getChips(1));
        assertFalse(state.isInGame(1));
        assertEquals(3, state.getChips(0));
        assertEquals(3, state.getChips(2));
        assertEquals(3, state.getChips(3));
        // the deal passes from player 3 to player 0; player 1 gets no cards
        assertNewDeal(state, 0);
        fm.next(state, new ChooseHand(false));
        assertEquals("player 1 is out, so player 2 plays first", 2, state.getCurrentPlayer());
    }

    @Test
    public void theDealAndTheCloseRoundSkipAPlayerWhoIsOut() {
        // 4 players, dealer 3, player 0 out (-4). Player 1 closes; players 2 and 3 have their last turns, and then
        // play would return to player 1 past player 0: the deal ends. Player 3 (9 diamonds) is worst.
        SchwimmenGameState state = withOrdinaryNextDeal(4, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 4, seed);
            arrangePlay(s, 1, TABLE, h(), HAND3, HAND0, HAND1);
            setChips(s, -4, 3, 3, 3);
            passAndClose(s);
            assertEquals(2, s.getCurrentPlayer());
            fm.next(s, new Pass());
            assertEquals(3, s.getCurrentPlayer());
            fm.next(s, new Pass());
            return s;
        });
        assertEquals(1, state.getRoundCounter());
        assertEquals(-4, state.getChips(0));
        assertEquals(3, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals("3 - 1", 2, state.getChips(3));
        // the dealer's left is player 0, who is out: the deal passes to player 1
        assertNewDeal(state, 1);
        fm.next(state, new ChooseHand(false));
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void turnsAndTheCountOfPassesSkipAPlayerWhoIsOut() {
        // 4 players, player 1 out: three passes by the three players still in replace the table
        SchwimmenGameState state = livesState(livesParams(), 4, ordinarySeed(4, 0));
        arrangePlay(state, 0, TABLE, HAND0, h(), HAND2, HAND3);
        setChips(state, 3, -4, 3, 3);
        state.dealer = 3;
        List<FrenchCard> nextTable = new ArrayList<>(state.getDrawDeck().getComponents().subList(0, 3));

        takeTurn(state, fm, new Pass());
        assertEquals("player 1 is out", 2, state.getCurrentPlayer());
        takeTurn(state, fm, new Pass());
        assertEquals(3, state.getCurrentPlayer());
        takeTurn(state, fm, new Pass());
        // n = 3 players still in have passed
        assertEquals(nextTable, state.getTable().getComponents());
        assertEquals(cards(TABLE), state.getDiscardPile().getComponents());
        assertEquals(0, state.getConsecutivePasses());
        assertEquals(0, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());
    }

    // ---- the other ways a deal ends ----

    @Test
    public void feuerCostsEveryOtherPlayerAChipAndASwimmingPlayerDropsOut() {
        // player 0 exchanges into three Aces; player 1 (30 spades, not the worst) loses a chip too; player 2
        // (24 hearts) is swimming and drops out with 2 players left in: -1 - 2 = -3
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 3, seed);
            arrangePlay(s, 0, h("AC", "8D", "9D"), h("AH", "AD", "7C"), h("KS", "QS", "JS"), h("7H", "8H", "9H"));
            setChips(s, 3, 3, 0);
            fm.next(s, new ExchangeOne(card("7C"), card("AC")));
            return s;
        });
        assertEquals("the Feuer holder keeps their chips", 3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(-3, state.getChips(2));
        assertEquals(1, state.getRoundCounter());
        // the deal passes from player 2 to player 0
        assertNewDeal(state, 0);
        fm.next(state, new ChooseHand(false));
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void aSchnauzByExchangeIsScoredAndTheNextDealStarts() {
        // player 0 exchanges into A K Q of hearts = 31; player 1 (9 diamonds) is worst
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 3, seed);
            arrangePlay(s, 0, h("QH", "7D", "10D"), h("AH", "KH", "7C"), HAND1, HAND2);
            setChips(s, 3, 3, 3);
            fm.next(s, new ExchangeOne(card("7C"), card("QH")));
            return s;
        });
        assertEquals(3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals(1, state.getRoundCounter());
        assertNewDeal(state, 0);
    }

    @Test
    public void aSchnauzDealtIsScoredAndTheNextDealStarts() {
        // straight after the deal (dealer 2): player 0 was dealt A K Q of hearts = 31; player 1 is worst
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 3, seed);
            arrangeDeal(s, h("7H", "9H", "KS"), h("AH", "KH", "QH"), HAND1, HAND2);
            setChips(s, 3, 3, 3);
            assertTrue(fm.endDealIfSpecialHand(s));
            return s;
        });
        assertEquals(3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals(1, state.getRoundCounter());
        assertNewDeal(state, 0);
    }

    @Test
    public void aDealEndedByAnExhaustedDrawDeckCostsTheWorstHandAChip() {
        // 2 cards in the draw deck: the third pass cannot replace the table, so the deal ends
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> {
            SchwimmenGameState s = livesState(livesParams(), 3, seed);
            arrangePlay(s, 0, TABLE, HAND0, HAND1, HAND2);
            setChips(s, 3, 3, 3);
            leaveInDrawDeck(s, 2);
            for (int i = 0; i < 3; i++)
                takeTurn(s, fm, new Pass());
            return s;
        });
        assertEquals(3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals(1, state.getRoundCounter());
        assertNewDeal(state, 0);
    }

    @Test
    public void aDealEndedByTheSafeguardCostsTheWorstHandAChip() {
        // maxCircuitsPerDeal 1: the deal ends after one turn each (the passes replace the table first)
        SchwimmenParameters params = livesParams();
        params.setParameterValue("maxCircuitsPerDeal", 1);
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> {
            SchwimmenGameState s = livesState(params, 3, seed);
            arrangePlay(s, 0, TABLE, HAND0, HAND1, HAND2);
            setChips(s, 3, 3, 3);
            for (int i = 0; i < 3; i++)
                takeTurn(s, fm, new Pass());
            return s;
        });
        assertEquals(3, state.getChips(0));
        assertEquals(2, state.getChips(1));
        assertEquals(3, state.getChips(2));
        assertEquals(1, state.getRoundCounter());
        assertNewDeal(state, 0);
    }

    // ---- the end of the game ----

    @Test
    public void theLastPlayerStillInWins() {
        // 3 players, player 2 out (-3). Player 0 (21 clubs, 2 chips) closes; player 1 (9 diamonds) is swimming and
        // drops out with 1 player left in: -1 - 1 = -2. Play would return to player 0 past player 2.
        SchwimmenGameState state = livesState(livesParams(), 3, ordinarySeed(3, 0));
        arrangePlay(state, 0, TABLE, HAND3, HAND1, h());
        setChips(state, 2, 0, -3);
        passAndClose(state);
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Pass());

        assertFalse(state.isNotTerminal());
        assertEquals(2, state.getChips(0));
        assertEquals(-2, state.getChips(1));
        assertEquals(-3, state.getChips(2));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(1, state.getOrdinalPosition(0));
        assertEquals("went out later than player 2", 2, state.getOrdinalPosition(1));
        assertEquals(3, state.getOrdinalPosition(2));
    }

    @Test
    public void feuerThatPutsOutTheLastOtherPlayerWinsTheGame() {
        // player 2 out (-3); player 0 exchanges into three Aces; player 1 is swimming and drops out: -1 - 1 = -2
        SchwimmenGameState state = livesState(livesParams(), 3, ordinarySeed(3, 0));
        arrangePlay(state, 0, h("AC", "8D", "9D"), h("AH", "AD", "7C"), h("KS", "QS", "JS"), h());
        setChips(state, 1, 0, -3);
        fm.next(state, new ExchangeOne(card("7C"), card("AC")));

        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getChips(0));
        assertEquals(-2, state.getChips(1));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void playersStillInWhoAllDropOutTogetherShareTheWin() {
        // player 2 out (-3); players 0 and 1 are swimming with the same worst hand (10 hearts each): both drop
        // out with no player left in: -1 - 0 = -1 each, and they share first place
        SchwimmenGameState state = livesState(livesParams(), 3, ordinarySeed(3, 0));
        arrangePlay(state, 0, h("QS", "8C", "7D"), h("KH", "7C", "8D"), h("JH", "7S", "9D"), h());
        setChips(state, 0, 0, -3);
        passAndClose(state);
        fm.next(state, new Pass());

        assertFalse(state.isNotTerminal());
        assertEquals(-1, state.getChips(0));
        assertEquals(-1, state.getChips(1));
        assertEquals(-3, state.getChips(2));
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void atMaxDealsTheMostChipsWinAndEqualMostShare() {
        // maxDeals 1: the game ends after the first deal is scored. Chips 3, 2, 3: players 0 and 2 share first place
        // even though player 0's hand (16) beats player 2's (10)
        SchwimmenParameters params = livesParams();
        params.setParameterValue("maxDeals", 1);
        SchwimmenGameState state = closedStandardDeal(params, ordinarySeed(3, 0));

        assertFalse(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(2, state.getChips(1));
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[2]);
    }

    @Test
    public void beforeMaxDealsTheGameGoesOn() {
        // the pair of the test above with maxDeals 2: the first deal is scored and the second starts
        SchwimmenParameters params = livesParams();
        params.setParameterValue("maxDeals", 2);
        SchwimmenGameState state = withOrdinaryNextDeal(3, seed -> closedStandardDeal(params, seed));
        assertEquals(1, state.getRoundCounter());
        assertEquals(2, state.getChips(1));
        assertNewDeal(state, 0);
    }

    // ---- random games ----

    @Test
    public void randomChipsGamesRunManyDealsToTheLastPlayerIn() {
        int games = 0, multiDealGames = 0;
        for (int n : new int[]{2, 3, 5}) {
            for (long seed = 1; seed <= 3; seed++) {
                SchwimmenParameters params = livesParams();
                Game game = newGame(n, seed, params);
                SchwimmenGameState state = (SchwimmenGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                int steps = 0;
                int[] chips = new int[n];
                for (int p = 0; p < n; p++) chips[p] = state.getChips(p);
                while (state.isNotTerminal() && steps++ < 50000) {
                    assertTrue("player " + state.getCurrentPlayer() + " is out but to play",
                            state.isInGame(state.getCurrentPlayer()));
                    for (int p = 0; p < n; p++) {
                        double h = state.getHeuristicScore(p);
                        assertTrue("heuristic " + h, h >= 0 && h <= 1);
                    }
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse(actions.isEmpty());
                    int deals = state.getRoundCounter();
                    fm.next(state, actions.get(rnd.nextInt(actions.size())));

                    assertAllCardsPresent(state);
                    int in = 0, lost = 0;
                    for (int p = 0; p < n; p++) {
                        assertTrue("chips never rise", state.getChips(p) <= chips[p]);
                        if (state.getChips(p) < chips[p]) lost++;
                        if (state.isInGame(p)) in++;
                        if (state.isNotTerminal())   // no new deal after the last one
                            assertEquals("hand of player " + p, state.isInGame(p) ? 3 : 0,
                                    state.getPlayerHand(p).getSize());
                    }
                    int scored = state.getRoundCounter() - deals;
                    if (scored > 0 || !state.isNotTerminal())
                        assertTrue("a scored deal costs somebody a chip", lost > 0);
                    else
                        assertEquals("no chips change during a deal", 0, lost);
                    // one deal scored: a player who dropped out has -1 - (players left in)
                    if (scored == 1 || (scored == 0 && !state.isNotTerminal()))
                        for (int p = 0; p < n; p++)
                            if (chips[p] >= 0 && state.getChips(p) < 0)
                                assertEquals("drop-out encoding", -1 - in, state.getChips(p));
                    for (int p = 0; p < n; p++) chips[p] = state.getChips(p);
                }
                assertFalse("chips game did not end within 50000 actions", state.isNotTerminal());
                if (state.getRoundCounter() > 1) multiDealGames++;
                int in = 0;
                for (int p = 0; p < n; p++) if (state.isInGame(p)) in++;
                assertTrue("at most one player left in (maxDeals is 100)", in <= 1);
                for (int p = 0; p < n; p++) {
                    assertEquals(state.getChips(p), state.getGameScore(p), 1e-9);
                    assertEquals("player " + p + " of " + n + ", seed " + seed,
                            oracleChipsResult(state, p), state.getPlayerResults()[p]);
                }
                games++;
            }
        }
        assertEquals(9, games);
        assertTrue("no random chips game lasted more than one deal", multiDealGames > 0);
    }
}

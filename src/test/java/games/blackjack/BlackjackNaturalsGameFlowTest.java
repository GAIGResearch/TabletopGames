package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.handValue;
import static games.blackjack.BlackjackGameState.isTenValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games from the factory, driven only by fm.next, in both payout modes.
 */
public class BlackjackNaturalsGameFlowTest {

    @Test
    public void aScriptedGameWithTheDefaultTwentyOnePayout() {
        Game game = newGame(3, 11);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // dealer A + 9: the peek finds no Blackjack and play starts. Player 0 has a natural and still plays;
        // player 1 (10 + 5) hits the 6 to 21; player 2 has 18. The dealer's soft 20 stands.
        stackDeal(state, new String[]{"AH KC", "10S 5C", "10C 8H"}, "AS", "9D", "6H 2C");

        fm.next(state, new Bet(2));
        fm.next(state, new Bet(4));
        fm.next(state, new Bet(6));
        declineInsurance(state, fm);     // every player offered insurance declines it
        assertEquals(Play, state.getGamePhase());
        assertEquals("a natural still plays by default", 0, state.getCurrentPlayer());
        assertEquals(setOf("9D"), setOf(state.getHoleCard()));
        assertArrayEquals(new int[]{8, 6, 4}, state.chips);

        fm.next(state, new Stand());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Hit());
        assertEquals("21 is not bust: player 1 continues", 1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Stand());

        // natural: 8 + 2 + 4 = 14; 3-card 21: 6 + 4 + 8 = 18; 18 v 20 loses: 4
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("AS 9D"), setOf(state.getDealerHand()));
        assertEquals(card("2C"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{14, 18, 4}, state.chips);
        assertArrayEquals(new GameResult[]{WIN_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aScriptedGameWithNaturalsPaidAtThreeToTwo() {
        BlackjackParameters params = new BlackjackParameters();
        pagatNaturals(params);
        Game game = newGame(3, 11, params);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // dealer K + 6: the peek finds no Blackjack. Player 0's natural (bet 10) is paid 15 at once and player 0
        // does not play. Player 1 (9 + 2) hits the K to 21; player 2 has 17. The dealer's 16 draws the 5: 21.
        stackDeal(state, new String[]{"AH QC", "9S 2C", "10C 7H"}, "KS", "6D", "KH 5C 3D");

        fm.next(state, new Bet(10));
        fm.next(state, new Bet(4));
        fm.next(state, new Bet(2));
        declineInsurance(state, fm);     // every player offered insurance declines it
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(25, state.getChips(0));
        assertEquals(0, state.getBet(0, 0));
        assertEquals(1, state.getHoleCard().getSize());

        fm.next(state, new Hit());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Stand());

        // the natural beat the dealer's 21 (already paid, 25); player 1's 21 pushes (10); 17 loses (8)
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("KS 6D 5C"), setOf(state.getDealerHand()));
        assertEquals(card("3D"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{25, 10, 8}, state.chips);
        assertArrayEquals(new GameResult[]{WIN_GAME, DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aScriptedGameEndingAtADealerBlackjack() {
        Game game = newGame(2, 3);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        stackDeal(state, new String[]{"AH QC", "9S 9C"}, "JS", "AD", "");
        fm.next(state, new Bet(6));
        assertTrue(state.isNotTerminal());
        fm.next(state, new Bet(8));
        declineInsurance(state, fm);     // every player offered insurance declines it
        // natural pushes (10); 18 loses (2)
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("JS AD"), setOf(state.getDealerHand()));
        assertArrayEquals(new int[]{10, 2}, state.chips);
        assertArrayEquals(new GameResult[]{DRAW_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void randomGamesInBothModesEndWithTheRightPayoutsAndEveryCardAccountedFor() {
        for (boolean naturalOnly : new boolean[]{false, true}) {
            int dealerBlackjacks = 0, naturals = 0, naturalsPlaying = 0, twentyOneWins = 0, holeChecks = 0;
            for (int nPlayers : new int[]{1, 3, 7}) {
                for (long seed = 0; seed < 30; seed++) {
                    BlackjackParameters params = new BlackjackParameters();
                    if (naturalOnly)
                        pagatNaturals(params);
                    Game game = newGame(nPlayers, seed, params);
                    BlackjackGameState state = (BlackjackGameState) game.getGameState();
                    AbstractForwardModel fm = game.getForwardModel();
                    Random rnd = new Random(seed);
                    int[] bets = new int[nPlayers];
                    int steps = 0;
                    while (state.isNotTerminal() && steps++ < 200) {
                        List<AbstractAction> actions = fm.computeAvailableActions(state);
                        assertFalse("no actions", actions.isEmpty());
                        int player = state.getCurrentPlayer();
                        if (state.getGamePhase() == Play) {
                            List<FrenchCard> hand = state.getPlayerHand(player, 0).getComponents();
                            if (isNaturalHand(hand)) {
                                assertFalse("a natural plays with payout21NaturalOnly", naturalOnly);
                                naturalsPlaying++;
                            }
                            // the peek found no Blackjack: a copy never completes one in the hole
                            FrenchCard up = state.getDealerHand().peek();
                            if (state.getHoleCard().getSize() == 1 && (up.type == Ace || isTenValue(up))) {
                                BlackjackGameState copy = (BlackjackGameState) state.copy(player);
                                FrenchCard hole = copy.getHoleCard().peek();
                                assertFalse("redeterminised to a dealer Blackjack",
                                        up.type == Ace ? isTenValue(hole) : hole.type == Ace);
                                holeChecks++;
                            }
                        }
                        // insurance is always declined here (BlackjackInsuranceGameFlowTest buys it at random)
                        AbstractAction action = state.getGamePhase() == BlackjackGameState.BlackjackGamePhase.Insurance
                                ? new Insurance(false) : actions.get(rnd.nextInt(actions.size()));
                        if (action instanceof Bet bet)
                            bets[player] = bet.amount;
                        fm.next(state, action);
                        assertAllCardsPresent(state);
                    }
                    assertFalse("game did not end within 200 actions", state.isNotTerminal());

                    List<FrenchCard> dealer = state.getDealerHand().getComponents();
                    assertEquals(0, state.getHoleCard().getSize());
                    if (isNaturalHand(dealer)) dealerBlackjacks++;
                    for (int p = 0; p < nPlayers; p++) {
                        List<FrenchCard> hand = state.getPlayerHand(p, 0).getComponents();
                        if (isNaturalHand(hand)) naturals++;
                        if (handValue(hand) == 21 && !isNaturalHand(dealer) && handValue(dealer) != 21) twentyOneWins++;
                        String where = "mode " + naturalOnly + ", " + nPlayers + " players, seed " + seed + ", player " + p;
                        assertEquals(where, 0, state.getBet(p, 0));
                        int expected = expectedChips(bets[p], hand, dealer, naturalOnly, naturalOnly ? 1.5 : 2.0);
                        assertEquals(where, expected, state.getChips(p));
                        GameResult result = expected > 10 ? WIN_GAME : expected == 10 ? DRAW_GAME : LOSE_GAME;
                        assertEquals(where, result, state.getPlayerResults()[p]);
                    }
                }
            }
            String mode = "naturalOnly " + naturalOnly + ": ";
            assertTrue(mode + "no dealer Blackjack", dealerBlackjacks > 0);
            assertTrue(mode + "no natural", naturals > 0);
            assertTrue(mode + "no winning 21", twentyOneWins > 0);
            assertTrue(mode + "no redeterminisation after a failed peek", holeChecks > 0);
            if (!naturalOnly)
                assertTrue("no natural was offered a play by default", naturalsPlaying > 0);
        }
    }
}

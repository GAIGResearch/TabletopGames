package games.blackjack;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.blackjack.BlackjackGameState.BlackjackGamePhase;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Hit;
import games.blackjack.actions.Insurance;
import games.blackjack.actions.Stand;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.FrenchCardType.Ace;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.isTenValue;
import static games.blackjack.BlackjackTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games from the factory, driven only by fm.next, with insurance bought.
 */
public class BlackjackInsuranceGameFlowTest {

    static final BlackjackGamePhase INSURANCE = BlackjackGamePhase.Insurance;

    @Test
    public void aScriptedGameWithInsuranceBoughtAndPaid() {
        Game game = newGame(3, 11);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // dealer A + Q: Blackjack. Player 0 has 19, player 1 a natural, player 2 bets 8 and cannot afford insurance
        stackDeal(state, new String[]{"10H 9C", "AH KC", "7C 7D"}, "AS", "QD", "5H");

        fm.next(state, new Bet(4));
        fm.next(state, new Bet(6));
        fm.next(state, new Bet(8));
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new Insurance(true));        // 6 - 2 = 4
        assertEquals(INSURANCE, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertArrayEquals(new int[]{4, 4, 2}, state.chips);
        assertTrue(state.isNotTerminal());
        fm.next(state, new Insurance(true));        // 4 - 3 = 1; player 2 is skipped, so the dealer peeks

        // 19 loses, insurance 3 x 2: 4 + 6 = 10. The natural pushes, insurance 3 x 3: 1 + 6 + 9 = 16. 14 loses: 2
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("AS QD"), setOf(state.getDealerHand()));
        assertEquals(0, state.getHoleCard().getSize());
        assertEquals(card("5H"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{10, 16, 2}, state.chips);
        assertArrayEquals(new int[]{0, 0, 0}, state.insurance);
        assertArrayEquals(new GameResult[]{DRAW_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void aScriptedGameWithInsuranceBoughtAndLost() {
        BlackjackParameters params = new BlackjackParameters();
        pagatNaturals(params);
        Game game = newGame(3, 11, params);
        BlackjackGameState state = (BlackjackGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // dealer K + 7: no Blackjack. Player 0 has a natural, player 1 hits 10 + 5 with the 6 to 21, player 2 stands
        // on 16. The dealer's 17 stands.
        stackDeal(state, new String[]{"AH KC", "10S 5C", "9C 7H"}, "KS", "7D", "6H 2C");

        fm.next(state, new Bet(2));
        fm.next(state, new Bet(4));
        fm.next(state, new Bet(6));
        assertEquals(INSURANCE, state.getGamePhase());
        fm.next(state, new Insurance(true));        // 8 - 1 = 7
        assertEquals("the natural is not paid before the peek", 2, state.getBet(0, 0));
        fm.next(state, new Insurance(false));
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Insurance(true));        // 4 - 3 = 1

        // the peek: every insurance lost. The natural is paid 2 + 3 at once (7 + 5 = 12) and player 1 plays first
        assertEquals(Play, state.getGamePhase());
        assertEquals(1, state.getCurrentPlayer());
        assertArrayEquals(new int[]{12, 6, 1}, state.chips);
        assertArrayEquals(new int[]{0, 0, 0}, state.insurance);
        assertEquals(1, state.getHoleCard().getSize());

        fm.next(state, new Hit());
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new Stand());
        assertEquals(2, state.getCurrentPlayer());
        fm.next(state, new Stand());

        // a 3-card 21 v 17 pays 1:1 with naturalOnly: 6 + 8 = 14. 16 loses: 1
        assertFalse(state.isNotTerminal());
        assertEquals(setOf("KS 7D"), setOf(state.getDealerHand()));
        assertEquals(card("2C"), state.getDrawDeck().peek());
        assertArrayEquals(new int[]{12, 14, 1}, state.chips);
        assertArrayEquals(new GameResult[]{WIN_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    @Test
    public void randomGamesWithInsuranceInBothModesEndWithTheRightChipsAndEveryCardAccountedFor() {
        Set<AbstractAction> buyOrDecline = Set.of(new Insurance(true), new Insurance(false));
        for (boolean naturalOnly : new boolean[]{false, true}) {
            int paid = 0, lost = 0, declined = 0, skipped = 0, looseHoles = 0;
            for (int nPlayers : new int[]{1, 3, 7}) {
                for (long seed = 0; seed < 40; seed++) {
                    BlackjackParameters params = new BlackjackParameters();
                    if (naturalOnly)
                        pagatNaturals(params);
                    Game game = newGame(nPlayers, seed, params);
                    BlackjackGameState state = (BlackjackGameState) game.getGameState();
                    AbstractForwardModel fm = game.getForwardModel();
                    Random rnd = new Random(seed);
                    int[] bets = new int[nPlayers];
                    int[] bought = new int[nPlayers];
                    boolean[] offered = new boolean[nPlayers];
                    int steps = 0;
                    while (state.isNotTerminal() && steps++ < 200) {
                        List<AbstractAction> actions = fm.computeAvailableActions(state);
                        assertFalse("no actions", actions.isEmpty());
                        int player = state.getCurrentPlayer();
                        if (state.getGamePhase() == INSURANCE) {
                            FrenchCard up = state.getDealerHand().peek();
                            assertTrue("insurance under a 2-9", up.type == Ace || isTenValue(up));
                            assertEquals(buyOrDecline, new HashSet<>(actions));
                            assertEquals("the hole card is face down", 1, state.getHoleCard().getSize());
                            assertTrue("offered without the chips", 2 * state.getChips(player) >= bets[player]);
                            assertFalse("offered twice", offered[player]);
                            offered[player] = true;
                            for (int p = 0; p < nPlayers; p++) {
                                assertEquals(bought[p], state.getInsurance(p));
                                assertEquals("chips + bet + insurance", 10,
                                        state.getChips(p) + state.getBet(p, 0) + state.getInsurance(p));
                            }
                            // no peek yet: a copy may complete a Blackjack in the hole
                            BlackjackGameState copy = (BlackjackGameState) state.copy(player);
                            FrenchCard hole = copy.getHoleCard().peek();
                            if (up.type == Ace ? isTenValue(hole) : hole.type == Ace)
                                looseHoles++;
                        } else if (state.getGamePhase() == Play) {
                            for (int p = 0; p < nPlayers; p++)
                                assertEquals("insurance after the peek", 0, state.getInsurance(p));
                        }
                        AbstractAction action = actions.get(rnd.nextInt(actions.size()));
                        if (action instanceof Bet bet)
                            bets[player] = bet.amount;
                        if (action instanceof Insurance ins) {
                            if (ins.buy) bought[player] = bets[player] / 2;
                            else declined++;
                        }
                        fm.next(state, action);
                        assertAllCardsPresent(state);
                    }
                    assertFalse("game did not end within 200 actions", state.isNotTerminal());

                    List<FrenchCard> dealer = state.getDealerHand().getComponents();
                    FrenchCard up = dealer.get(dealer.size() - 1);    // Deck.add puts later cards on top (index 0)
                    boolean dealerBlackjack = isNaturalHand(dealer);
                    for (int p = 0; p < nPlayers; p++) {
                        String where = "mode " + naturalOnly + ", " + nPlayers + " players, seed " + seed + ", player " + p;
                        // offered exactly when the up card is an Ace or ten and the chips left cover half the bet
                        boolean canAfford = 2 * (10 - bets[p]) >= bets[p];
                        assertEquals(where + ": offered", (up.type == Ace || isTenValue(up)) && canAfford, offered[p]);
                        if ((up.type == Ace || isTenValue(up)) && !canAfford) skipped++;
                        if (bought[p] > 0) {
                            if (dealerBlackjack) paid++;
                            else lost++;
                        }
                        List<FrenchCard> hand = state.getPlayerHand(p, 0).getComponents();
                        // insurance: 3 x the stake back on a dealer Blackjack (net +2 x), otherwise the stake is lost
                        int expected = expectedChips(bets[p], hand, dealer, naturalOnly, naturalOnly ? 1.5 : 2.0)
                                + (dealerBlackjack ? 2 * bought[p] : -bought[p]);
                        assertEquals(where, expected, state.getChips(p));
                        assertEquals(where, 0, state.getBet(p, 0));
                        assertEquals(where, 0, state.getInsurance(p));
                        GameResult result = expected > 10 ? WIN_GAME : expected == 10 ? DRAW_GAME : LOSE_GAME;
                        assertEquals(where, result, state.getPlayerResults()[p]);
                    }
                }
            }
            String mode = "naturalOnly " + naturalOnly + ": ";
            assertTrue(mode + "no insurance paid", paid > 0);
            assertTrue(mode + "no insurance lost", lost > 0);
            assertTrue(mode + "no insurance declined", declined > 0);
            assertTrue(mode + "nobody skipped for want of chips", skipped > 0);
            assertTrue(mode + "no copy completed a Blackjack in the hole before the peek", looseHoles > 0);
        }
    }
}

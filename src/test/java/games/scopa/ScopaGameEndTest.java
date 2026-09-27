package games.scopa;

import core.CoreConstants;
import core.Game;
import core.components.TarotCard;
import org.junit.Test;

import static core.CoreConstants.GameResult.*;
import static core.components.TarotCard.*;
import static core.components.TarotCard.Suit.*;
import static games.scopa.ScopaTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the (single) deal in a real game: the last play is made with fm.next, and the higher game score wins
 * (equal scores draw). Every test arranges all 40 cards: the captured piles, the last card(s) in hand and the table,
 * with an empty draw deck. Primiera values: 7 = 21, 6 = 18, Ace = 16, 5 = 15, 4 = 14, 3 = 13, 2 = 12, courts = 10.
 */
public class ScopaGameEndTest {

    @Test
    public void theWorkedExampleEndsWithPlayerZeroWinningFourToThree() {
        Game game = newGame(24);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        // P0 (22): Coins 7, Ace, 2, Knave, Cavalier, King (6); Swords Ace-4, 6; Batons Ace-4, 6; Cups Ace-6
        // P1 (16 now, 18 after the last play): Coins 3-6 (4); Swords 7, Knave, Cavalier, King; Batons the same;
        //    Cups 7, Knave, Cavalier, King - plus the 5 of Swords in hand capturing the 5 of Batons on the table
        arrangeLastPlay(state, 1, of(), of(sword(5)), of(baton(5)),
                cat(suit(Coins, 7, 1, 2, KNAVE, CAVALIER, KING), suit(Swords, 1, 2, 3, 4, 6),
                        suit(Batons, 1, 2, 3, 4, 6), suit(Cups, 1, 2, 3, 4, 5, 6)),
                cat(suit(Coins, 3, 4, 5, 6), suit(Swords, 7, KNAVE, CAVALIER, KING),
                        suit(Batons, 7, KNAVE, CAVALIER, KING), suit(Cups, 7, KNAVE, CAVALIER, KING)));
        state.scopas[0] = 1;
        state.scopas[1] = 2;
        state.lastCapturer = 0;

        // the sweep with the last card of the deal is no scopa
        play(state, fm, 1, capture(sword(5), baton(5)));

        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(22, state.getCapturedCards(0).getSize());
        assertEquals(18, state.getCapturedCards(1).getSize());
        assertEquals(2, state.getScopas(1));
        // primiera: P0 Coins 7 21 + Swords 6 18 + Batons 6 18 + Cups 6 18 = 75
        //           P1 Coins 6 18 + Swords 7 21 + Batons 7 21 + Cups 7 21 = 81 -> P1
        // P0: scopa 1 + cards (22 > 18) 1 + Coins (6 > 4) 1 + 7 of Coins 1 = 4
        // P1: scopas 2 + primiera 1 = 3
        assertEquals(4.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    @Test
    public void playerOneWinsOnCategoryPointsWithTheTableLeftOvers() {
        Game game = newGame(25);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        // P0 (18): Coins 3-6; Swords 5, 7, Knave, Cavalier, King; Batons the same; Cups 7, Knave, Cavalier, King
        // P1 (20 now): Coins 7, Ace, 2, Knave, Cavalier, King; Swords Ace-4, 6; Batons Ace-4, 6; Cups Ace-3, 6
        // P1 plays the 5 of Cups: nothing on the table {4 of Cups} is a 5 -> table; last card of the deal, so the
        // table (4 and 5 of Cups) goes to P1, the last capturer -> 22 cards
        arrangeLastPlay(state, 1, of(), of(cup(5)), of(cup(4)),
                cat(suit(Coins, 3, 4, 5, 6), suit(Swords, 5, 7, KNAVE, CAVALIER, KING),
                        suit(Batons, 5, 7, KNAVE, CAVALIER, KING), suit(Cups, 7, KNAVE, CAVALIER, KING)),
                cat(suit(Coins, 7, 1, 2, KNAVE, CAVALIER, KING), suit(Swords, 1, 2, 3, 4, 6),
                        suit(Batons, 1, 2, 3, 4, 6), suit(Cups, 1, 2, 3, 6)));
        state.lastCapturer = 1;

        play(state, fm, 1, toTable(cup(5)));

        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(18, state.getCapturedCards(0).getSize());
        assertEquals(22, state.getCapturedCards(1).getSize());
        // no scopas; primiera P0 Coins 6 18 + 7s 21 * 3 = 81, P1 Coins 7 21 + 6s 18 * 3 = 75 -> P0
        // P0: primiera 1 = 1
        // P1: cards (22 > 18) 1 + Coins (6 > 4) 1 + 7 of Coins 1 = 3
        assertEquals(1.0, state.getGameScore(0), 0.0);
        assertEquals(3.0, state.getGameScore(1), 0.0);
        assertEquals(LOSE_GAME, state.getPlayerResults()[0]);
        assertEquals(WIN_GAME, state.getPlayerResults()[1]);
    }

    @Test
    public void equalScoresAtTheEndAreADrawForBoth() {
        Game game = newGame(26);
        ScopaGameState state = (ScopaGameState) game.getGameState();
        ScopaForwardModel fm = (ScopaForwardModel) game.getForwardModel();
        // P1 (19): Coins Ace-7, King (8); Swords Ace-6, Knave, Cavalier, King (9); Batons 6; Cups Ace
        // P0 (19 now, 21 after the table): Coins Knave, Cavalier (2); Swords 7; Batons Ace-5, 7, Knave (7);
        //    Cups 2-7, Knave, Cavalier, King (9); in play: P0's Cavalier of Batons in hand, the King of Batons on
        //    the table
        arrangeLastPlay(state, 0, of(baton(CAVALIER)), of(), of(baton(KING)),
                cat(suit(Coins, KNAVE, CAVALIER), suit(Swords, 7), suit(Batons, 1, 2, 3, 4, 5, 7, KNAVE),
                        suit(Cups, 2, 3, 4, 5, 6, 7, KNAVE, CAVALIER, KING)),
                cat(suit(Coins, 1, 2, 3, 4, 5, 6, 7, KING), suit(Swords, 1, 2, 3, 4, 5, 6, KNAVE, CAVALIER, KING),
                        suit(Batons, 6), suit(Cups, 1)));
        state.scopas[0] = 1;
        state.lastCapturer = 0;

        // the Cavalier (9) takes nothing from {King (10)}; the table then goes to P0, the last capturer
        play(state, fm, 0, toTable(baton(CAVALIER)));

        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        assertEquals(21, state.getCapturedCards(0).getSize());
        assertEquals(19, state.getCapturedCards(1).getSize());
        // primiera: P0 Coins court 10 + Swords 7 21 + Batons 7 21 + Cups 7 21 = 73
        //           P1 Coins 7 21 + Swords 6 18 + Batons 6 18 + Cups Ace 16 = 73 -> tie, nobody
        // P0: scopa 1 + cards (21 > 19) 1 + Coins (2 < 8) 0 = 2
        // P1: Coins (8 > 2) 1 + 7 of Coins 1 = 2
        assertEquals(2.0, state.getGameScore(0), 0.0);
        assertEquals(2.0, state.getGameScore(1), 0.0);
        assertEquals(DRAW_GAME, state.getPlayerResults()[0]);
        assertEquals(DRAW_GAME, state.getPlayerResults()[1]);
    }
}

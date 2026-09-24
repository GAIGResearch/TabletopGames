package games.leducpoker;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Helpers to build and arrange Leduc Poker states. The arrange helpers move the state's own cards,
 * so the six cards are always conserved.
 */
final class LeducPokerTestUtils {

    /** The six cards of the deck, built independently of LeducPokerParameters.deckCards(). */
    static final List<FrenchCard> ALL_CARDS = List.of(
            card("JS"), card("QS"), card("KS"), card("JH"), card("QH"), card("KH"));

    private LeducPokerTestUtils() {
    }

    /**
     * A real 2-player game from the factory, reset with random players, for integration tests driven by fm.next.
     */
    static Game newGame(long seed) {
        return newGame(seed, null);
    }

    /**
     * As newGame(seed), with the given parameters (null for the defaults). The seed overrides any seed on params.
     */
    static Game newGame(long seed, LeducPokerParameters params) {
        Game game = GameType.LeducPoker.createGameInstance(2, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 2; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A card from a two-letter code: rank (J, Q, K) then suit (S, H). e.g. "JS", "KH".
     */
    static FrenchCard card(String code) {
        FrenchCard.Suite suit = switch (code.charAt(1)) {
            case 'S' -> FrenchCard.Suite.Spades;
            case 'H' -> FrenchCard.Suite.Hearts;
            default -> throw new IllegalArgumentException("Unknown suit in " + code);
        };
        FrenchCard.FrenchCardType type = switch (code.charAt(0)) {
            case 'J' -> Jack;
            case 'Q' -> Queen;
            case 'K' -> King;
            default -> throw new IllegalArgumentException("Unknown rank in " + code);
        };
        return new FrenchCard(type, suit);
    }

    static List<Deck<FrenchCard>> allDecks(LeducPokerGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.hands);
        decks.add(state.drawDeck);
        decks.add(state.board);
        return decks;
    }

    /**
     * Gives player 0 and player 1 the given private cards and puts drawTop on top of the draw deck (drawTop[0] at
     * index 0, the next card dealt). Any board card goes back into the draw deck, so the board is left empty.
     * The cards are moved from wherever they are; the remaining cards go below drawTop in the draw deck.
     */
    static void arrange(LeducPokerGameState state, FrenchCard card0, FrenchCard card1, FrenchCard... drawTop) {
        List<FrenchCard> rest = new ArrayList<>();
        for (Deck<FrenchCard> deck : allDecks(state)) {
            rest.addAll(deck.getComponents());
            deck.clear();
        }
        state.hands.get(0).add(take(rest, card0));
        state.hands.get(1).add(take(rest, card1));
        List<FrenchCard> draw = new ArrayList<>();
        for (FrenchCard c : drawTop)
            draw.add(take(rest, c));
        draw.addAll(rest);
        for (FrenchCard c : draw)
            state.drawDeck.addToBottom(c);
        assertAllCardsPresent(state);
    }

    /**
     * As arrange, then moves boardCard from the draw deck to the board (a test-only shortcut into betting round 1,
     * leaving the betting counters untouched).
     */
    static void arrangeWithBoard(LeducPokerGameState state, FrenchCard card0, FrenchCard card1, FrenchCard boardCard) {
        arrange(state, card0, card1, boardCard);
        state.board.add(state.drawDeck.draw());
    }

    private static FrenchCard take(List<FrenchCard> cards, FrenchCard card) {
        int idx = cards.indexOf(card);
        if (idx < 0)
            throw new IllegalArgumentException(card + " is not available to arrange");
        return cards.remove(idx);
    }

    /**
     * Plays the given actions in order through fm.next.
     */
    static void play(AbstractForwardModel fm, LeducPokerGameState state, AbstractAction... actions) {
        for (AbstractAction a : actions)
            fm.next(state, a);
    }

    /**
     * Each of the six cards is in exactly one of the hands, the draw deck and the board.
     */
    static void assertAllCardsPresent(LeducPokerGameState state) {
        List<FrenchCard> found = new ArrayList<>();
        for (Deck<FrenchCard> deck : allDecks(state))
            found.addAll(deck.getComponents());
        assertEquals("number of cards", 6, found.size());
        assertTrue("cards " + found, found.containsAll(ALL_CARDS));
    }

    /**
     * Rank for a test oracle, written from the rules rather than taken from LeducPokerUtils.
     */
    static int oracleRank(FrenchCard c) {
        return switch (c.type) {
            case Jack -> 0;
            case Queen -> 1;
            case King -> 2;
            default -> throw new IllegalArgumentException(c.toString());
        };
    }

    /**
     * Showdown winner for a test oracle, with the default rules: 0 or 1, or -1 for a tie.
     */
    static int oracleWinner(FrenchCard c0, FrenchCard c1, FrenchCard board) {
        return oracleWinner(c0, c1, board, false);
    }

    /**
     * Showdown winner for a test oracle: 0 or 1, or -1 for a tie.
     */
    static int oracleWinner(FrenchCard c0, FrenchCard c1, FrenchCard board, boolean highCardUsesBoard) {
        // a pair with the board wins
        boolean pair0 = c0.type == board.type, pair1 = c1.type == board.type;
        if (pair0 != pair1) return pair0 ? 0 : 1;
        // otherwise the higher private card, or with highCardUsesBoard the higher of each card and the board card
        int r0 = oracleRank(c0), r1 = oracleRank(c1);
        if (highCardUsesBoard) {
            r0 = Math.max(r0, oracleRank(board));
            r1 = Math.max(r1, oracleRank(board));
        }
        return r0 > r1 ? 0 : r1 > r0 ? 1 : -1;
    }
}

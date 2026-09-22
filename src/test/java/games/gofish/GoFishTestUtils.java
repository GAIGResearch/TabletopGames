package games.gofish;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.*;

import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Helpers to arrange Go Fish states. Every card helper moves cards from wherever they currently are (a hand, the draw
 * deck or a player's books), so the 52 cards are always conserved.
 */
final class GoFishTestUtils {

    private GoFishTestUtils() {
    }

    /**
     * A real game from the factory with the given parameters (null for the defaults), reset with random players, for
     * tests driven by fm.next. The seed overrides any seed on params.
     */
    static Game newGame(int nPlayers, long seed, GoFishParameters params) {
        Game game = GameType.GoFish.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /**
     * A directly set-up state (no Game) with the given parameters and seed.
     */
    static GoFishGameState newState(GoFishParameters params, int nPlayers, long seed) {
        params.setRandomSeed(seed);
        GoFishGameState state = new GoFishGameState(params, nPlayers);
        new GoFishForwardModel().setup(state);
        return state;
    }

    static GoFishGameState newState(int nPlayers, long seed) {
        return newState(new GoFishParameters(), nPlayers, seed);
    }

    /**
     * A card from a short code: rank (2-10, J, Q, K, A) then suit (H, D, C, S). e.g. "8H", "10D", "KS".
     */
    static FrenchCard card(String code) {
        FrenchCard.Suite suit = switch (code.charAt(code.length() - 1)) {
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            default -> throw new IllegalArgumentException("Unknown suit in " + code);
        };
        return switch (code.substring(0, code.length() - 1)) {
            case "J" -> new FrenchCard(Jack, suit);
            case "Q" -> new FrenchCard(Queen, suit);
            case "K" -> new FrenchCard(King, suit);
            case "A" -> new FrenchCard(Ace, suit);
            default -> new FrenchCard(Number, suit, Integer.parseInt(code.substring(0, code.length() - 1)));
        };
    }

    /**
     * The four cards of a rank (FrenchCard.number: 2-10, J 11, Q 12, K 13, A 14).
     */
    static List<FrenchCard> cardsOfRank(int rank) {
        List<FrenchCard> cards = new ArrayList<>();
        for (FrenchCard.Suite s : FrenchCard.Suite.values()) {
            cards.add(switch (rank) {
                case 11 -> new FrenchCard(Jack, s);
                case 12 -> new FrenchCard(Queen, s);
                case 13 -> new FrenchCard(King, s);
                case 14 -> new FrenchCard(Ace, s);
                default -> new FrenchCard(Number, s, rank);
            });
        }
        return cards;
    }

    static List<Deck<FrenchCard>> allDecks(GoFishGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.addAll(state.playerBooks);
        return decks;
    }

    /**
     * Remove the card from whichever deck holds it and return the instance that was there.
     */
    static FrenchCard takeFromWherever(GoFishGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            int idx = deck.getComponents().indexOf(card);
            if (idx >= 0)
                return deck.pick(idx);
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Player p's hand becomes exactly these cards: the old hand goes to the bottom of the draw deck first, then each
     * card is moved from wherever it is. Do not give 4 of a rank (a book is never held in a hand).
     */
    static void giveHand(GoFishGameState state, int p, FrenchCard... cards) {
        Deck<FrenchCard> hand = state.playerHands.get(p);
        while (hand.getSize() > 0)
            state.drawDeck.addToBottom(hand.draw());
        for (FrenchCard c : cards)
            hand.add(takeFromWherever(state, c));
    }

    /**
     * Put these cards on top of the draw deck, the first argument on top (drawn first).
     */
    static void stackDrawDeck(GoFishGameState state, FrenchCard... cards) {
        for (int i = cards.length - 1; i >= 0; i--)
            state.drawDeck.add(takeFromWherever(state, cards[i]));
    }

    /**
     * Lay down a book of each rank for player p, moving the 4 cards from wherever they are.
     */
    static void giveBooks(GoFishGameState state, int p, int... ranks) {
        for (int r : ranks)
            for (FrenchCard c : cardsOfRank(r))
                state.playerBooks.get(p).add(takeFromWherever(state, c));
    }

    /**
     * Lay down for player p a book of every rank except the given ones, moving the 4 cards from wherever they are.
     * Call it before giveHand: afterwards the hands and draw deck can only hold cards of the kept ranks, so a test
     * that places every kept card in a hand has an empty draw deck.
     */
    static void bookAllRanksExcept(GoFishGameState state, int p, int... keep) {
        Set<Integer> kept = new HashSet<>();
        for (int r : keep) kept.add(r);
        for (int r = 2; r <= 14; r++)
            if (!kept.contains(r)) giveBooks(state, p, r);
    }

    /**
     * Parameters for the playUntilAllBooks variant (everything else default).
     */
    static GoFishParameters playUntilAllBooks() {
        GoFishParameters params = new GoFishParameters();
        params.setParameterValue("playUntilAllBooks", true);
        return params;
    }

    /**
     * The cards of a deck as a set (order ignored).
     */
    static Set<FrenchCard> cardSet(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    static Set<FrenchCard> cards(String... codes) {
        Set<FrenchCard> set = new HashSet<>();
        for (String c : codes) set.add(card(c));
        return set;
    }

    /**
     * All 52 distinct cards are present across hands, draw deck and books.
     */
    static void assertAllCardsPresent(GoFishGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        for (Deck<FrenchCard> d : allDecks(state)) all.addAll(d.getComponents());
        assertEquals("cards in hands, draw deck and books", 52, all.size());
        assertEquals("distinct cards", 52, new HashSet<>(all).size());
    }

    /**
     * Every books deck holds only complete ranks (4 cards each), and no hand holds all 4 cards of a rank.
     */
    static void assertBooksWellFormed(GoFishGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            Map<Integer, Integer> inBooks = new HashMap<>();
            for (FrenchCard c : state.playerBooks.get(p).getComponents()) inBooks.merge(c.number, 1, Integer::sum);
            for (Map.Entry<Integer, Integer> e : inBooks.entrySet())
                assertEquals("player " + p + " books of rank " + e.getKey(), 4, (int) e.getValue());
            Map<Integer, Integer> inHand = new HashMap<>();
            for (FrenchCard c : state.playerHands.get(p).getComponents()) inHand.merge(c.number, 1, Integer::sum);
            for (Map.Entry<Integer, Integer> e : inHand.entrySet())
                assertTrue("player " + p + " holds a book of " + e.getKey() + " in hand", e.getValue() < 4);
        }
    }

    /**
     * The index of the card (by value) in player p's hand; fails if p does not hold it.
     */
    static int indexInHand(GoFishGameState state, int p, FrenchCard card) {
        int idx = state.playerHands.get(p).getComponents().indexOf(card);
        if (idx < 0) fail("player " + p + " does not hold " + card);
        return idx;
    }

    /**
     * Arrange: make the card in player p's hand visible to every player.
     */
    static void showToAll(GoFishGameState state, int p, FrenchCard card) {
        boolean[] all = new boolean[state.getNPlayers()];
        Arrays.fill(all, true);
        state.playerHands.get(p).setVisibilityOfComponent(indexInHand(state, p, card), all);
    }

    static boolean isVisibleToAll(GoFishGameState state, int p, FrenchCard card) {
        PartialObservableDeck<FrenchCard> hand = state.playerHands.get(p);
        int idx = indexInHand(state, p, card);
        for (int q = 0; q < state.getNPlayers(); q++)
            if (!hand.isComponentVisible(idx, q)) return false;
        return true;
    }

    /**
     * The card in player p's hand is visible to all players.
     */
    static void assertVisibleToAll(GoFishGameState state, int p, FrenchCard card) {
        assertTrue(card + " in the hand of " + p + " should be visible to all", isVisibleToAll(state, p, card));
    }

    /**
     * The card in player p's hand is visible to p and to nobody else.
     */
    static void assertVisibleOnlyToOwner(GoFishGameState state, int p, FrenchCard card) {
        PartialObservableDeck<FrenchCard> hand = state.playerHands.get(p);
        int idx = indexInHand(state, p, card);
        for (int q = 0; q < state.getNPlayers(); q++)
            assertEquals(card + " in the hand of " + p + ", visible to " + q, q == p, hand.isComponentVisible(idx, q));
    }

    /**
     * How many cards of the rank in player p's hand are visible to all players.
     */
    static int visibleToAllOfRank(GoFishGameState state, int p, int rank) {
        int n = 0;
        for (FrenchCard c : state.playerHands.get(p).getComponents())
            if (c.number == rank && isVisibleToAll(state, p, c)) n++;
        return n;
    }
}

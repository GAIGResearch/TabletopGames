package games.cuckoo;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.assertEquals;

/**
 * Helpers to arrange Cuckoo states. Every card helper moves cards from wherever they currently are, so the 52 cards
 * are always conserved.
 */
final class CuckooTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private CuckooTestUtils() {
    }

    /**
     * A real game from the factory with default parameters, reset with random players, for tests driven by fm.next.
     */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /**
     * As newGame(nPlayers, seed), with the given parameters (null for the defaults). The seed overrides any seed on params.
     */
    static Game newGame(int nPlayers, long seed, CuckooParameters params) {
        Game game = GameType.Cuckoo.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A directly set-up state (no Game), with the given number of lives each and random seed.
     */
    static CuckooGameState newState(int nPlayers, int nLives, long seed) {
        CuckooParameters params = new CuckooParameters();
        params.setParameterValue("nLives", nLives);
        params.setRandomSeed(seed);
        CuckooGameState state = new CuckooGameState(params, nPlayers);
        new CuckooForwardModel().setup(state);
        return state;
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

    static List<Deck<FrenchCard>> allDecks(CuckooGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerCards);
        decks.add(state.drawDeck);
        return decks;
    }

    private static void takeFromWherever(CuckooGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Take the player out of the game: no lives, went out in the given round, and their card (if any) goes to the
     * bottom of the draw deck. Call before dealCards.
     */
    static void knockOut(CuckooGameState state, int player, int roundOut) {
        state.lives[player] = 0;
        state.roundEliminated[player] = roundOut;
        Deck<FrenchCard> deck = state.playerCards.get(player);
        while (deck.getSize() > 0)
            state.drawDeck.addToBottom(deck.draw());
    }

    /**
     * Give player p the p-th card (null for a player who is out of the game and holds nothing). All the players'
     * current cards go to the bottom of the draw deck first, so a card may be taken from any player.
     */
    static void dealCards(CuckooGameState state, FrenchCard... cardsByPlayer) {
        if (cardsByPlayer.length != state.getNPlayers())
            throw new IllegalArgumentException("one card (or null) per player");
        for (Deck<FrenchCard> deck : state.playerCards)
            while (deck.getSize() > 0)
                state.drawDeck.addToBottom(deck.draw());
        for (int p = 0; p < cardsByPlayer.length; p++) {
            if (cardsByPlayer[p] == null) continue;
            takeFromWherever(state, cardsByPlayer[p]);
            state.playerCards.get(p).add(cardsByPlayer[p]);
        }
    }

    /**
     * Move this card (which must be in the draw deck, so call after dealCards) to the top of the draw deck.
     */
    static void putOnTopOfDrawDeck(CuckooGameState state, FrenchCard card) {
        if (!state.drawDeck.contains(card))
            throw new IllegalArgumentException(card + " is not in the draw deck");
        state.drawDeck.remove(card);
        state.drawDeck.add(card);
    }

    /**
     * Make this player the dealer, and the given player the one to decide now.
     */
    static void setDealerAndTurn(CuckooGameState state, int dealer, int currentPlayer) {
        state.dealer = dealer;
        state.setFirstPlayer(currentPlayer);
    }

    /**
     * Advance the framework's round counter to the given round (from a lower one), via StandardForwardModel.endRound.
     * Leaves the turn to be set again with setDealerAndTurn.
     */
    static void advanceRoundCounter(CuckooGameState state, CuckooForwardModel fm, int round) {
        while (state.getRoundCounter() < round)
            fm.endRound(state, state.getCurrentPlayer());
    }

    /**
     * The next player after p (increasing number, wrapping round) with lives left, from the lives given.
     * Written independently of CuckooGameState.nextPlayerInGame, for use as an oracle.
     */
    static int nextWithLives(int[] lives, int p) {
        int n = lives.length;
        for (int i = 1; i <= n; i++) {
            int q = (p + i) % n;
            if (lives[q] > 0) return q;
        }
        return -1;
    }

    static void assertAllCardsPresent(CuckooGameState state) {
        List<FrenchCard> cards = new ArrayList<>();
        allDecks(state).forEach(d -> cards.addAll(d.getComponents()));
        assertEquals("number of cards", 52, cards.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(cards));
    }

    /**
     * Each player in the game holds exactly one card, each player out of the game none, and the draw deck the rest.
     */
    static void assertOneCardPerPlayerInGame(CuckooGameState state) {
        int inGame = 0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            boolean in = state.lives[p] > 0;
            if (in) inGame++;
            assertEquals("cards held by player " + p, in ? 1 : 0, state.playerCards.get(p).getSize());
        }
        assertEquals("draw deck size", 52 - inGame, state.drawDeck.getSize());
    }

    /**
     * Asserts the whole knowledge matrix: row o is a string with one character per holder, '1' if observer o knows
     * the holder's card and '0' if not. e.g. assertKnowledge(state, "1100", "1100", "0010", "0001").
     */
    static void assertKnowledge(CuckooGameState state, String... rows) {
        if (rows.length != state.getNPlayers())
            throw new IllegalArgumentException("one row per observer");
        StringBuilder expected = new StringBuilder(), actual = new StringBuilder();
        for (int o = 0; o < rows.length; o++) {
            expected.append(rows[o]).append(o < rows.length - 1 ? " " : "");
            for (int h = 0; h < rows.length; h++)
                actual.append(state.knowsCard(o, h) ? '1' : '0');
            if (o < rows.length - 1) actual.append(' ');
        }
        assertEquals("knowledge[observer][holder], one group per observer", expected.toString(), actual.toString());
    }
}

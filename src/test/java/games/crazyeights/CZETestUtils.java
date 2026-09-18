package games.crazyeights;

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
import static org.junit.Assert.fail;

/**
 * Helpers to arrange Crazy Eights states. Every helper moves cards from wherever they currently are,
 * so the 52 cards are always conserved.
 */
final class CZETestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private CZETestUtils() {
    }

    /**
     * A real game from the factory, reset with random players, for integration tests driven by fm.next.
     */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /**
     * As newGame(nPlayers, seed), with the given parameters (null for the defaults). The seed overrides any seed on params.
     */
    static Game newGame(int nPlayers, long seed, CZEParameters params) {
        Game game = GameType.CrazyEights.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
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

    static List<Deck<FrenchCard>> allDecks(CZEGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.add(state.discardPile);
        return decks;
    }

    private static void takeFromWherever(CZEGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Put the card on top of the discard pile, with the given suit to match.
     * Call this before giveHand, so the hand cards are not taken from under the new top card.
     */
    static void setTopDiscard(CZEGameState state, FrenchCard card, FrenchCard.Suite suitToMatch) {
        takeFromWherever(state, card);
        state.discardPile.add(card);
        state.currentSuit = suitToMatch;
    }

    static void setTopDiscard(CZEGameState state, FrenchCard card) {
        setTopDiscard(state, card, card.suite);
    }

    /**
     * Replace the player's hand with exactly these cards. The old hand goes to the bottom of the stock.
     */
    static void giveHand(CZEGameState state, int player, FrenchCard... cards) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.drawDeck.addToBottom(c);
        }
        for (FrenchCard c : cards) {
            takeFromWherever(state, c);
            hand.add(c);
        }
    }

    /**
     * Leave nothing that could be drawn: the stock, and every discard except the top card, go to the player's hand.
     */
    static void leaveNothingToDraw(CZEGameState state, int player) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        while (state.drawDeck.getSize() > 0)
            hand.add(state.drawDeck.draw());
        while (state.discardPile.getSize() > 1)
            hand.add(state.discardPile.pickLast());
    }

    /**
     * Empty the stock by moving all its cards under the top discard (to the bottom of the discard pile).
     */
    static void moveStockUnderTopDiscard(CZEGameState state) {
        while (state.drawDeck.getSize() > 0)
            state.discardPile.addToBottom(state.drawDeck.draw());
    }

    /**
     * Move these cards, from wherever they are, to the bottom of the discard pile (under the top card).
     */
    static void putUnderTopDiscard(CZEGameState state, FrenchCard... cards) {
        for (FrenchCard c : cards) {
            takeFromWherever(state, c);
            state.discardPile.addToBottom(c);
        }
    }

    /**
     * The first seed (from 0) whose deal, with these parameters and number of players, turns up an Eight as the
     * starter card (eightStarter true) or a card that is not an Eight (false). The deal does not depend on
     * dealerNominatesStarterSuit. Leaves params' random seed set to the seed returned; fails if none is found.
     */
    static long seedGivingStarter(CZEParameters params, int nPlayers, boolean eightStarter) {
        CZEForwardModel fm = new CZEForwardModel();
        for (long seed = 0; seed < 1000; seed++) {
            params.setRandomSeed(seed);
            CZEGameState state = new CZEGameState(params, nPlayers);
            fm.setup(state);
            if (CZEGameState.isEight(state.getTopCard()) == eightStarter)
                return seed;
        }
        fail("no seed below 1000 gives " + (eightStarter ? "an Eight" : "a non-Eight") + " starter");
        return -1;
    }

    static void assertAllCardsPresent(CZEGameState state) {
        List<FrenchCard> cards = new ArrayList<>();
        allDecks(state).forEach(d -> cards.addAll(d.getComponents()));
        assertEquals("number of cards", 52, cards.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(cards));
    }
}

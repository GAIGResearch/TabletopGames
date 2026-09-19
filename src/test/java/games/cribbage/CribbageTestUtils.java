package games.cribbage;

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
import static games.cribbage.CribbageGameState.CribbageGamePhase.Play;
import static org.junit.Assert.assertEquals;

/**
 * Helpers to arrange Cribbage states. Every helper moves cards from wherever they currently are,
 * so the 52 cards are always conserved.
 */
final class CribbageTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    /**
     * The crib cards used by arrangePlay: the first two are the non-dealer's, the last two the dealer's.
     * With the starter King of Spades they score nothing in the show: no fifteens (A, 3, 7, 9 and 10 have no
     * subset totalling 15), no pairs, no run of three, no flush and no Jack.
     */
    static final List<FrenchCard> ZERO_CRIB = List.of(card("AC"), card("3D"), card("7H"), card("9C"));
    static final FrenchCard ZERO_STARTER = card("KS");

    private CribbageTestUtils() {
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
    static Game newGame(long seed, CribbageParameters params) {
        Game game = GameType.Cribbage.createGameInstance(2, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 2; p++)
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

    static List<FrenchCard> cards(String... codes) {
        List<FrenchCard> list = new ArrayList<>();
        for (String c : codes)
            list.add(card(c));
        return list;
    }

    /** Every deck holding cards: hands, played cards, crib and draw deck (the starter is a separate field). */
    static List<Deck<FrenchCard>> allDecks(CribbageGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.addAll(state.playedCards);
        decks.add(state.crib);
        decks.add(state.drawDeck);
        return decks;
    }

    /** Remove the card from whichever deck (or the starter) holds it. */
    private static void takeFromWherever(CribbageGameState state, FrenchCard card) {
        if (card.equals(state.starter)) {
            state.starter = null;
            return;
        }
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Replace the player's hand with exactly these cards. The old hand goes to the bottom of the draw deck.
     */
    static void giveHand(CribbageGameState state, int player, List<FrenchCard> cards) {
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

    /** Move the card to the top of the draw deck, where the starter is turned up from. */
    static void putOnTopOfDrawDeck(CribbageGameState state, FrenchCard card) {
        takeFromWherever(state, card);
        state.drawDeck.add(card);
    }

    /**
     * Arrange the start of the play, as if both players had discarded: every card goes back to the draw deck;
     * then the crib is ZERO_CRIB (each pair visible only to its discarder), the starter is ZERO_STARTER, the
     * hands are exactly the cards given (any number, need not be 4), phase Play, and the non-dealer to act.
     * The hands must not contain any of the crib cards or the starter.
     */
    static void arrangePlay(CribbageGameState state, List<FrenchCard> nonDealerHand, List<FrenchCard> dealerHand) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck == state.drawDeck) continue;
            for (FrenchCard c : new ArrayList<>(deck.getComponents())) {
                deck.remove(c);
                state.drawDeck.addToBottom(c);
            }
        }
        if (state.starter != null) {
            state.drawDeck.addToBottom(state.starter);
            state.starter = null;
        }
        state.playSequence.clear();

        int dealer = state.getDealer();
        int nonDealer = state.getNonDealer();
        for (int i = 0; i < ZERO_CRIB.size(); i++) {
            FrenchCard c = ZERO_CRIB.get(i);
            takeFromWherever(state, c);
            boolean[] visibility = new boolean[state.getNPlayers()];
            visibility[i < 2 ? nonDealer : dealer] = true;
            state.crib.add(c, visibility);
        }
        takeFromWherever(state, ZERO_STARTER);
        state.starter = ZERO_STARTER;
        giveHand(state, nonDealer, nonDealerHand);
        giveHand(state, dealer, dealerHand);
        state.setGamePhase(Play);
        state.setTurnOwner(nonDealer);
    }

    /**
     * Replace the crib with exactly these four cards: the first two visible only to the non-dealer, the last two
     * only to the dealer. The old crib cards go to the bottom of the draw deck. Use after arrangePlay.
     */
    static void replaceCrib(CribbageGameState state, List<FrenchCard> cribCards) {
        for (FrenchCard c : new ArrayList<>(state.crib.getComponents())) {
            state.crib.remove(c);
            state.drawDeck.addToBottom(c);
        }
        for (int i = 0; i < cribCards.size(); i++) {
            FrenchCard c = cribCards.get(i);
            takeFromWherever(state, c);
            boolean[] visibility = new boolean[state.getNPlayers()];
            visibility[i < 2 ? state.getNonDealer() : state.getDealer()] = true;
            state.crib.add(c, visibility);
        }
    }

    /**
     * Put these cards into the current count as if the player had played them: each is moved to the player's
     * played cards and appended to playSequence. Does not change the scores or the player to act.
     */
    static void playedInCount(CribbageGameState state, int player, FrenchCard... cards) {
        for (FrenchCard c : cards) {
            takeFromWherever(state, c);
            state.playedCards.get(player).add(c);
            state.playSequence.add(c);
        }
    }

    /**
     * Move these cards to the player's played cards as if played in an earlier, finished count: they are NOT
     * added to playSequence. Does not change the scores or the player to act.
     */
    static void playedEarlier(CribbageGameState state, int player, FrenchCard... cards) {
        for (FrenchCard c : cards) {
            takeFromWherever(state, c);
            state.playedCards.get(player).add(c);
        }
    }

    /** All 52 cards are present exactly once across the decks and the starter. */
    static void assertAllCardsPresent(CribbageGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        if (state.starter != null) all.add(state.starter);
        assertEquals("number of cards", 52, all.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(all));
    }

    /** The cards in the crib visible to the player (the ones they discarded). */
    static List<FrenchCard> cribCardsVisibleTo(CribbageGameState state, int player) {
        List<FrenchCard> visible = new ArrayList<>();
        for (int i = 0; i < state.crib.getSize(); i++)
            if (state.crib.isComponentVisible(i, player))
                visible.add(state.crib.get(i));
        return visible;
    }
}

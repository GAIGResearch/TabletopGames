package games.rummy;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.rummy.actions.Discard;
import games.rummy.actions.LayOff;
import games.rummy.actions.Meld;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static org.junit.Assert.*;

/**
 * Factories and arrange helpers for the Rummy tests. Every arrange helper MOVES cards from wherever they are
 * (hands, draw deck, discard pile, melds); cards no longer wanted go to the BOTTOM of the discard pile, so the top
 * discard is not disturbed and the 52 cards are conserved.
 */
class RummyTestUtils {

    private RummyTestUtils() {
    }

    /** All 52 cards, one of each. */
    static final Set<FrenchCard> FULL_DECK = new HashSet<>(
            FrenchCard.generateDeck("all", VISIBLE_TO_ALL).getComponents());

    /** A factory game of nPlayers RandomPlayers, reset with the given seed. */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, new RummyParameters());
    }

    /** A factory game of nPlayers RandomPlayers with the given parameters, reset with the given seed. */
    static Game newGame(int nPlayers, long seed, RummyParameters params) {
        Game g = GameType.Rummy.createGameInstance(nPlayers, seed, params);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A state with the given parameters, player count and seed, set up by the forward model. */
    static RummyGameState newState(RummyParameters params, int nPlayers, long seed, RummyForwardModel fm) {
        params.setRandomSeed(seed);
        RummyGameState state = new RummyGameState(params, nPlayers);
        fm.setup(state);
        return state;
    }

    /** A state with default parameters, set up by the forward model. */
    static RummyGameState newState(int nPlayers, long seed, RummyForwardModel fm) {
        return newState(new RummyParameters(), nPlayers, seed, fm);
    }

    /** A card from a code: rank 2-10, J, Q, K, A then suit H, D, C, S - e.g. "10H", "JS", "AD". */
    static FrenchCard card(String code) {
        String rank = code.substring(0, code.length() - 1);
        FrenchCard.Suite suit = switch (code.charAt(code.length() - 1)) {
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            default -> throw new IllegalArgumentException(code);
        };
        return switch (rank) {
            case "J" -> new FrenchCard(FrenchCard.FrenchCardType.Jack, suit);
            case "Q" -> new FrenchCard(FrenchCard.FrenchCardType.Queen, suit);
            case "K" -> new FrenchCard(FrenchCard.FrenchCardType.King, suit);
            case "A" -> new FrenchCard(FrenchCard.FrenchCardType.Ace, suit);
            default -> new FrenchCard(FrenchCard.FrenchCardType.Number, suit, Integer.parseInt(rank));
        };
    }

    static List<FrenchCard> cards(String... codes) {
        return Arrays.stream(codes).map(RummyTestUtils::card).toList();
    }

    /** Card points, written from the rules independently of RummyUtils. */
    static int pointsByRule(FrenchCard c) {
        return switch (c.type) {
            case Ace -> 1;
            case Jack, Queen, King -> 10;
            default -> c.number;
        };
    }

    static int pointsByRule(Deck<FrenchCard> hand) {
        int total = 0;
        for (FrenchCard c : hand.getComponents())
            total += pointsByRule(c);
        return total;
    }

    /** Every card in the state: hands, draw deck, discard pile and melds. */
    static List<FrenchCard> allCards(RummyGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        for (Deck<FrenchCard> hand : state.playerHands)
            all.addAll(hand.getComponents());
        all.addAll(state.drawDeck.getComponents());
        all.addAll(state.discardPile.getComponents());
        for (RummyMeld meld : state.melds)
            all.addAll(meld.getComponents());
        return all;
    }

    /** The 52 distinct cards are all present, each exactly once. */
    static void assertAllCardsPresent(RummyGameState state) {
        List<FrenchCard> all = allCards(state);
        assertEquals("number of cards", 52, all.size());
        assertEquals("distinct cards", FULL_DECK, new HashSet<>(all));
    }

    /** Removes the card from wherever it is in the state; fails if it is nowhere. */
    static void take(RummyGameState state, FrenchCard c) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.add(state.discardPile);
        decks.addAll(state.melds);
        for (Deck<FrenchCard> deck : decks)
            if (deck.contains(c)) {
                deck.remove(c);
                return;
            }
        fail(c + " is not in the state");
    }

    /** Player p's hand becomes exactly these cards (none = empty); its other cards go to the bottom of the discard pile. */
    static void giveHand(RummyGameState state, int p, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        Deck<FrenchCard> hand = state.playerHands.get(p);
        for (FrenchCard c : new ArrayList<>(hand.getComponents()))
            if (!wanted.contains(c)) {
                hand.remove(c);
                state.discardPile.addToBottom(c);
            }
        for (FrenchCard c : wanted)
            if (!hand.contains(c)) {
                take(state, c);
                hand.add(c);
            }
    }

    /**
     * The draw deck becomes exactly these cards, the FIRST listed on top (none = empty); its other cards go to the
     * bottom of the discard pile.
     */
    static void setDrawDeck(RummyGameState state, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        for (FrenchCard c : new ArrayList<>(state.drawDeck.getComponents()))
            if (!wanted.contains(c)) {
                state.drawDeck.remove(c);
                state.discardPile.addToBottom(c);
            }
        for (FrenchCard c : wanted)
            take(state, c);
        for (int i = wanted.size() - 1; i >= 0; i--)
            state.drawDeck.add(wanted.get(i));   // add puts it on top (index 0)
    }

    /**
     * The discard pile becomes exactly these cards, the FIRST listed on top (none = empty); its other cards go to the
     * bottom of the draw deck.
     */
    static void setDiscardPile(RummyGameState state, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        for (FrenchCard c : new ArrayList<>(state.discardPile.getComponents()))
            if (!wanted.contains(c)) {
                state.discardPile.remove(c);
                state.drawDeck.addToBottom(c);
            }
        for (FrenchCard c : wanted)
            take(state, c);
        for (int i = wanted.size() - 1; i >= 0; i--)
            state.discardPile.add(wanted.get(i));
    }

    /**
     * Arranges player p in the PLAY phase with takenCard as the card taken from the discard pile this turn (null if
     * the player drew from the draw deck). The taken card must already be in p's hand.
     */
    static void arrangePlay(RummyGameState state, int p, FrenchCard takenCard) {
        if (takenCard != null)
            assertTrue("the taken card is in the hand", state.playerHands.get(p).contains(takenCard));
        state.setTurnOwner(p);
        state.setGamePhase(RummyGameState.Phase.PLAY);
        state.takenCard = takenCard;
    }

    /**
     * Appends a meld of exactly these cards to the table, moving them from wherever they are. The FIRST listed goes
     * at index 0, so list a run by rank with Aces low and a set by suit: Diamonds, Hearts, Clubs, Spades.
     */
    static RummyMeld addMeld(RummyGameState state, String... codes) {
        RummyMeld meld = new RummyMeld();
        for (FrenchCard c : cards(codes)) {
            take(state, c);
            meld.addToBottom(c);
        }
        state.melds.add(meld);
        return meld;
    }

    static Meld meld(String... codes) {
        return new Meld(cards(codes));
    }

    static LayOff layOff(String code, LayOff.Position position) {
        return new LayOff(card(code), position);
    }

    /** A Discard action for each card. */
    static Set<AbstractAction> discards(String... codes) {
        Set<AbstractAction> set = new HashSet<>();
        for (FrenchCard c : cards(codes))
            set.add(new Discard(c));
        return set;
    }

    static Set<AbstractAction> union(Set<AbstractAction> discards, AbstractAction... others) {
        Set<AbstractAction> set = new HashSet<>(discards);
        set.addAll(Arrays.asList(others));
        return set;
    }

    /** Rank with Aces low, written from the rules independently of RummyUtils. */
    static int rankByRule(FrenchCard c) {
        return switch (c.type) {
            case Ace -> 1;
            case Jack -> 11;
            case Queen -> 12;
            case King -> 13;
            default -> c.number;
        };
    }

    /** Oracle from the rules: whether the cards, in any order, form a set or a run. */
    static boolean isMeldByRule(List<FrenchCard> meldCards) {
        if (meldCards.size() < 3 || new HashSet<>(meldCards).size() != meldCards.size()) return false;
        // a set is 3 or 4 cards of one rank
        int rank0 = rankByRule(meldCards.get(0));
        if (meldCards.stream().allMatch(c -> rankByRule(c) == rank0)) return meldCards.size() <= 4;
        // a run is one suit in consecutive ranks, Aces low, with no wrap from King to Ace
        FrenchCard.Suite suit = meldCards.get(0).suite;
        if (!meldCards.stream().allMatch(c -> c.suite == suit)) return false;
        List<Integer> ranks = meldCards.stream().map(RummyTestUtils::rankByRule).sorted().toList();
        for (int i = 1; i < ranks.size(); i++)
            if (ranks.get(i) != ranks.get(i - 1) + 1) return false;
        return true;
    }

    /** The cards of a meld on the table, in table order. */
    static List<FrenchCard> meldCards(RummyGameState state, int index) {
        return state.melds.get(index).getComponents();
    }

    /** The current legal actions as a set. */
    static Set<AbstractAction> actionSet(RummyGameState state, RummyForwardModel fm) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /**
     * Checks the visibility of every hand card against the rules, where known holds the cards taken from the discard
     * pile and still held.
     */
    static void assertHandVisibility(RummyGameState state, Set<FrenchCard> known) {
        int n = state.getNPlayers();
        for (int p = 0; p < n; p++) {
            PartialObservableDeck<FrenchCard> hand = state.playerHands.get(p);
            for (int i = 0; i < hand.getSize(); i++) {
                FrenchCard c = hand.get(i);
                for (int q = 0; q < n; q++) {
                    boolean expected = known.contains(c) || q == p;
                    assertEquals("player " + p + "'s " + c + " (index " + i + ") visible to player " + q,
                            expected, hand.getVisibilityForPlayer(i, q));
                }
            }
        }
    }

    /** The cards of a deck as a set (compare decks by content, never with Deck.equals). */
    static Set<FrenchCard> setOf(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }
}

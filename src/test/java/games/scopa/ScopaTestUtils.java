package games.scopa;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.TarotCard;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static org.junit.Assert.*;

/**
 * Factories and arrange helpers for the Scopa tests. Every arrange helper MOVES cards between the state's decks
 * (hands, draw deck, table, captured piles), so the 40 cards are conserved. Displaced cards go to the bottom of the
 * draw deck, except in emptyDrawDeck - so call emptyDrawDeck last.
 */
class ScopaTestUtils {

    private ScopaTestUtils() {
    }

    static final int N_PLAYERS = 2;

    /** All 40 cards, one of each. */
    static final Set<TarotCard> FULL_PACK = new HashSet<>(
            TarotCard.generateItalianDeck("all", HIDDEN_TO_ALL).getComponents());

    /** A factory game (2 RandomPlayers), reset with the given seed. */
    static Game newGame(long seed) {
        return newGame(seed, new ScopaParameters());
    }

    /** A factory game (2 RandomPlayers) with the given parameters, reset with the given seed. */
    static Game newGame(long seed, ScopaParameters params) {
        Game g = GameType.Scopa.createGameInstance(N_PLAYERS, seed, params);
        g.reset(IntStream.range(0, N_PLAYERS)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A 2-player state with default parameters and the given seed, set up by the forward model. */
    static ScopaGameState newState(long seed, ScopaForwardModel fm) {
        ScopaParameters params = new ScopaParameters();
        params.setRandomSeed(seed);
        ScopaGameState state = new ScopaGameState(params, N_PLAYERS);
        fm.setup(state);
        return state;
    }

    /** A 2-player state with the given parameters (seeded here), set up by the forward model. */
    static ScopaGameState newState(long seed, ScopaForwardModel fm, ScopaParameters params) {
        params.setRandomSeed(seed);
        ScopaGameState state = new ScopaGameState(params, N_PLAYERS);
        fm.setup(state);
        return state;
    }

    /** Parameters with ScopaParameters.targetScore and redealOnKings set through setParameterValue. */
    static ScopaParameters params(int targetScore, boolean redealOnKings) {
        ScopaParameters params = new ScopaParameters();
        params.setParameterValue("targetScore", targetScore);
        params.setParameterValue("redealOnKings", redealOnKings);
        return params;
    }

    /**
     * A 40-card pack for ScopaForwardModel.deal(state, pack): these cards first (index 0 first), then the rest of the
     * Italian deck in its generated (unshuffled) order.
     */
    static Deck<TarotCard> stackedPack(TarotCard... first) {
        Deck<TarotCard> pack = new Deck<>("Stacked pack", HIDDEN_TO_ALL);
        List<TarotCard> firstList = Arrays.asList(first);
        for (TarotCard c : first)
            pack.addToBottom(c);
        for (TarotCard c : TarotCard.generateItalianDeck("all", HIDDEN_TO_ALL).getComponents())
            if (!firstList.contains(c))
                pack.addToBottom(c);
        assertEquals("stacked pack size", 40, pack.getSize());
        return pack;
    }

    // ---- cards and actions ----

    static TarotCard sword(int n) {
        return new TarotCard(TarotCard.Suit.Swords, n);
    }

    static TarotCard baton(int n) {
        return new TarotCard(TarotCard.Suit.Batons, n);
    }

    static TarotCard cup(int n) {
        return new TarotCard(TarotCard.Suit.Cups, n);
    }

    static TarotCard coin(int n) {
        return new TarotCard(TarotCard.Suit.Coins, n);
    }

    /** Play the card, capturing these table cards. */
    static PlayCard capture(TarotCard card, TarotCard... captured) {
        return new PlayCard(card, Arrays.asList(captured));
    }

    /** Play the card to the table, capturing nothing. */
    static PlayCard toTable(TarotCard card) {
        return new PlayCard(card);
    }

    /** A list of captures as a set of sets, so the comparison ignores order at both levels. */
    static Set<Set<TarotCard>> asSets(List<List<TarotCard>> captures) {
        return captures.stream().map(HashSet::new).collect(Collectors.toSet());
    }

    /** A set of captures, each given as an array of cards. */
    static Set<Set<TarotCard>> sets(TarotCard[]... captures) {
        return Arrays.stream(captures).map(c -> (Set<TarotCard>) new HashSet<>(Arrays.asList(c))).collect(Collectors.toSet());
    }

    static TarotCard[] of(TarotCard... cards) {
        return cards;
    }

    /** The legal actions as a set. */
    static Set<AbstractAction> actionSet(ScopaForwardModel fm, ScopaGameState state) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /** Asserts that p is to act, then applies the action with fm.next. */
    static void play(ScopaGameState state, ScopaForwardModel fm, int p, PlayCard action) {
        assertEquals("player to act before " + action, p, state.getCurrentPlayer());
        fm.next(state, action);
    }

    // ---- conservation ----

    /** Every card in the state: hands, draw deck, table and captured piles. */
    static List<TarotCard> allCards(ScopaGameState state) {
        List<TarotCard> all = new ArrayList<>();
        for (Deck<TarotCard> d : allDecks(state))
            all.addAll(d.getComponents());
        return all;
    }

    static List<Deck<TarotCard>> allDecks(ScopaGameState state) {
        List<Deck<TarotCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.add(state.table);
        decks.addAll(state.capturedCards);
        return decks;
    }

    /** The 40 distinct cards are all present, each exactly once. */
    static void assertAllCardsPresent(ScopaGameState state) {
        List<TarotCard> all = allCards(state);
        assertEquals("number of cards", 40, all.size());
        assertEquals("distinct cards", FULL_PACK, new HashSet<>(all));
    }

    /** The cards of a deck as a set (compare decks by content). */
    static Set<TarotCard> setOf(Deck<TarotCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    static Set<TarotCard> setOf(TarotCard... cards) {
        return new HashSet<>(Arrays.asList(cards));
    }

    // ---- arranging (moves cards; conserves the 40) ----

    /** Removes the card from whichever of the state's decks holds it; fails if none does. */
    static void take(ScopaGameState state, TarotCard c) {
        for (Deck<TarotCard> d : allDecks(state))
            if (d.contains(c)) {
                d.remove(c);
                return;
            }
        fail(c + " is not in the state");
    }

    /**
     * The deck becomes exactly these cards, in this order (index 0 first); its other cards go to the bottom of the
     * spill deck, and the wanted cards are taken from wherever they are.
     */
    static void setDeck(ScopaGameState state, Deck<TarotCard> deck, Deck<TarotCard> spill, TarotCard... cards) {
        List<TarotCard> wanted = Arrays.asList(cards);
        for (TarotCard c : new ArrayList<>(deck.getComponents()))
            if (!wanted.contains(c)) {
                deck.remove(c);
                spill.addToBottom(c);
            }
        List<TarotCard> kept = new ArrayList<>(deck.getComponents());
        deck.clear();
        for (TarotCard c : wanted) {
            if (!kept.contains(c))
                take(state, c);
            deck.addToBottom(c);
        }
    }

    /** Player p's hand becomes exactly these cards; its other cards go to the bottom of the draw deck. */
    static void giveHand(ScopaGameState state, int p, TarotCard... cards) {
        setDeck(state, state.playerHands.get(p), state.drawDeck, cards);
    }

    /** The table becomes exactly these cards; its other cards go to the bottom of the draw deck. */
    static void setTable(ScopaGameState state, TarotCard... cards) {
        setDeck(state, state.table, state.drawDeck, cards);
    }

    /**
     * Makes both hands and the table exactly as given, moving their other cards to the draw deck, and makes toAct
     * the player to act. The draw deck keeps the rest (30 cards if 3 + 3 + 4 are given).
     */
    static void arrange(ScopaGameState state, int toAct, TarotCard[] hand0, TarotCard[] hand1, TarotCard... table) {
        giveHand(state, 0, hand0);
        giveHand(state, 1, hand1);
        setTable(state, table);
        state.setTurnOwner(toAct);
    }

    /**
     * The captured piles become exactly these cards (taken from wherever they are; the piles' other cards go to the
     * bottom of the draw deck). Call it after arrange, so the hands and table are not raided afterwards.
     */
    static void setPiles(ScopaGameState state, TarotCard[] pile0, TarotCard[] pile1) {
        setDeck(state, state.capturedCards.get(0), state.drawDeck, pile0);
        setDeck(state, state.capturedCards.get(1), state.drawDeck, pile1);
    }

    /**
     * Arranges the last play of the deal: the hands, the table, both captured piles and the player to act. Together
     * they must place all 40 cards, which is asserted along with the empty draw deck.
     */
    static void arrangeLastPlay(ScopaGameState state, int toAct, TarotCard[] hand0, TarotCard[] hand1,
                                TarotCard[] table, TarotCard[] pile0, TarotCard[] pile1) {
        arrange(state, toAct, hand0, hand1, table);
        setPiles(state, pile0, pile1);
        assertEquals("draw deck", 0, state.getDrawDeck().getSize());
        assertAllCardsPresent(state);
    }

    /** Concatenates card arrays (to build a pile suit by suit). */
    static TarotCard[] cat(TarotCard[]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(TarotCard[]::new);
    }

    /** The cards of one suit with these numbers, e.g. suit(Coins, 1, 2, KING). */
    static TarotCard[] suit(TarotCard.Suit s, int... numbers) {
        return Arrays.stream(numbers).mapToObj(n -> new TarotCard(s, n)).toArray(TarotCard[]::new);
    }

    /**
     * Moves every draw deck card to the bottom of player p's captured pile, leaving the draw deck empty as in the last
     * hands of the deal. It does NOT change lastCapturer. Call it after the other arrange helpers.
     */
    static void emptyDrawDeck(ScopaGameState state, int p) {
        while (state.drawDeck.getSize() > 0)
            state.capturedCards.get(p).addToBottom(state.drawDeck.draw());
    }
}

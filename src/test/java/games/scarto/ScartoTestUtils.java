package games.scarto;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.TarotCard;
import games.GameType;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static org.junit.Assert.*;

/**
 * Factories and arrange helpers for the Scarto tests. Every arrange helper MOVES cards between the state's decks
 * (hands, current trick, cardsWon, scarto), so the 78 cards are conserved. Displaced cards go to the scarto unless
 * the helper says otherwise - so arrange the scarto last (setDeck with another spill deck) when its content matters.
 */
class ScartoTestUtils {

    private ScartoTestUtils() {
    }

    static final int N_PLAYERS = 3;

    /** All 78 cards, one of each. */
    static final Set<TarotCard> FULL_PACK = new HashSet<>(
            TarotCard.generateDeck("all", HIDDEN_TO_ALL).getComponents());

    /** A factory game (3 RandomPlayers), reset with the given seed. */
    static Game newGame(long seed) {
        return newGame(seed, new ScartoParameters());
    }

    /** A factory game (3 RandomPlayers) with the given parameters, reset with the given seed. */
    static Game newGame(long seed, ScartoParameters params) {
        Game g = GameType.Scarto.createGameInstance(N_PLAYERS, seed, params);
        g.reset(IntStream.range(0, N_PLAYERS)
                .mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A 3-player state with default parameters and the given seed, set up by the forward model. */
    static ScartoGameState newState(long seed, ScartoForwardModel fm) {
        return newState(seed, fm, new ScartoParameters());
    }

    /** A 3-player state with the given parameters (seed set to the given one), set up by the forward model. */
    static ScartoGameState newState(long seed, ScartoForwardModel fm, ScartoParameters params) {
        params.setRandomSeed(seed);
        ScartoGameState state = new ScartoGameState(params, N_PLAYERS);
        fm.setup(state);
        return state;
    }

    /** Parameters for the several-deals variant: nDeals = 3. */
    static ScartoParameters threeDeals() {
        ScartoParameters params = new ScartoParameters();
        params.setParameterValue("nDeals", 3);
        return params;
    }

    /** Parameters for the dealer's exchange: dealerExchange = true (one deal). */
    static ScartoParameters exchange() {
        ScartoParameters params = new ScartoParameters();
        params.setParameterValue("dealerExchange", true);
        return params;
    }

    /** Parameters for the dealer's exchange over three deals: dealerExchange = true, nDeals = 3. */
    static ScartoParameters exchangeThreeDeals() {
        ScartoParameters params = exchange();
        params.setParameterValue("nDeals", 3);
        return params;
    }

    // ---- cards ----

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

    static TarotCard trump(int n) {
        return new TarotCard(TarotCard.Suit.Trumps, n);
    }

    /** A new Fool (equal to TarotCard.FOOL). */
    static TarotCard fool() {
        return new TarotCard(TarotCard.Suit.None, 0);
    }

    /** A 3-player trick with Scarto's card order, led by the given player, holding these cards in play order. */
    static Trick<TarotCard, TarotCard.Suit> scartoTrick(int leader, TarotCard... cards) {
        Trick<TarotCard, TarotCard.Suit> t = new Trick<>("Trick", N_PLAYERS, leader, ScartoCardOrder.INSTANCE);
        for (TarotCard c : cards)
            t.play(c);
        return t;
    }

    // ---- conservation ----

    /** Every card in the state: hands, current trick, cardsWon and scarto. */
    static List<TarotCard> allCards(ScartoGameState state) {
        List<TarotCard> all = new ArrayList<>();
        for (Deck<TarotCard> hand : state.playerHands)
            all.addAll(hand.getComponents());
        all.addAll(state.currentTrick.getComponents());
        for (Deck<TarotCard> won : state.cardsWon)
            all.addAll(won.getComponents());
        all.addAll(state.scarto.getComponents());
        return all;
    }

    /** The 78 distinct cards are all present, each exactly once. */
    static void assertAllCardsPresent(ScartoGameState state) {
        List<TarotCard> all = allCards(state);
        assertEquals("number of cards", 78, all.size());
        assertEquals("distinct cards", FULL_PACK, new HashSet<>(all));
    }

    /** The cards of a deck as a set (compare decks by content). */
    static Set<TarotCard> setOf(Deck<TarotCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    // ---- arranging (moves cards; conserves the 78) ----

    /** Removes the card from whichever of the state's decks holds it; fails if none does. */
    static void take(ScartoGameState state, TarotCard c) {
        List<Deck<TarotCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.addAll(state.cardsWon);
        decks.add(state.scarto);
        for (Deck<TarotCard> d : decks)
            if (d.contains(c)) {
                d.remove(c);
                return;
            }
        fail(c + " is not in the state");
    }

    /**
     * Makes sure player p holds each of these cards, keeping the size of every deck: a card held elsewhere swaps
     * places with a card of p's hand that is not in the list. Fails if p's hand has no card to spare.
     */
    static void putInHand(ScartoGameState state, int p, TarotCard... cards) {
        Deck<TarotCard> hand = state.playerHands.get(p);
        List<TarotCard> wanted = Arrays.asList(cards);
        List<Deck<TarotCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.addAll(state.cardsWon);
        decks.add(state.scarto);
        for (TarotCard c : cards) {
            if (hand.contains(c))
                continue;
            Deck<TarotCard> from = decks.stream().filter(d -> d.contains(c)).findFirst().orElse(null);
            assertNotNull(c + " is not in the state", from);
            int handIdx = -1;
            for (int i = 0; i < hand.getSize() && handIdx < 0; i++)
                if (!wanted.contains(hand.get(i)))
                    handIdx = i;
            assertTrue("no card to swap out of hand " + p, handIdx >= 0);
            TarotCard out = hand.get(handIdx);
            int fromIdx = from.getComponents().indexOf(c);
            hand.setComponent(handIdx, c);
            from.setComponent(fromIdx, out);
        }
    }

    /**
     * The deck becomes exactly these cards, in this order (index 0 first); its other cards go to the bottom of the
     * spill deck, and the wanted cards are taken from wherever they are.
     */
    static void setDeck(ScartoGameState state, Deck<TarotCard> deck, Deck<TarotCard> spill, TarotCard... cards) {
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

    /** Player p's hand becomes exactly these cards in this order (hand order); its other cards go to the scarto. */
    static void giveHand(ScartoGameState state, int p, TarotCard... cards) {
        setDeck(state, state.playerHands.get(p), state.scarto, cards);
    }

    /** Player p's cardsWon becomes exactly these cards; its other cards go to the scarto. */
    static void setCardsWon(ScartoGameState state, int p, TarotCard... cards) {
        setDeck(state, state.cardsWon.get(p), state.scarto, cards);
    }

    /** The scarto becomes exactly these cards; its other cards go to the spill deck. */
    static void setScarto(ScartoGameState state, Deck<TarotCard> spill, TarotCard... cards) {
        setDeck(state, state.scarto, spill, cards);
    }

    /**
     * The current trick becomes a new trick led by the leader, holding these cards played in turn from the leader
     * (taken from wherever they are); the player after the last one listed is to act. The old trick's cards go to
     * the scarto.
     */
    static void arrangeTrick(ScartoGameState state, int leader, TarotCard... cards) {
        for (TarotCard played : state.currentTrick.getComponents())
            state.scarto.addToBottom(played);
        state.currentTrick = new Trick<>("CurrentTrick", N_PLAYERS, leader, ScartoCardOrder.INSTANCE);
        for (TarotCard c : cards) {
            take(state, c);
            state.currentTrick.play(c);
        }
        state.setTurnOwner((leader + cards.length) % N_PLAYERS);
    }

    /** Asserts that p is to act, then plays the card with fm.next. */
    static void play(ScartoGameState state, ScartoForwardModel fm, int p, TarotCard card) {
        assertEquals("player to act before " + card, p, state.getCurrentPlayer());
        fm.next(state, new PlayCard<>(card));
    }

    /** The PlayCard actions for these cards, in this order. */
    static List<PlayCard<TarotCard>> playCards(TarotCard... cards) {
        return Arrays.stream(cards).map(PlayCard::new).toList();
    }
}

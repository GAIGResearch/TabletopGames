package games.schwimmen;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.GameType;
import games.schwimmen.actions.Close;
import games.schwimmen.actions.CloseDecision;
import games.schwimmen.actions.ExchangeAll;
import games.schwimmen.actions.ExchangeOne;
import games.schwimmen.actions.Pass;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.*;

/**
 * Factories, arrange helpers and rule oracles for the Schwimmen tests. Every arrange helper MOVES cards from wherever
 * they are, so the 32 cards are always conserved; each helper's Javadoc says where displaced cards go.
 */
final class SchwimmenTestUtils {

    /** The 32-card piquet pack: Seven to Ace in each suit. */
    static final Set<FrenchCard> FULL_PACK;

    static {
        List<FrenchCard> all = new ArrayList<>(FrenchCard.generateDeck("all", VISIBLE_TO_ALL).getComponents());
        all.removeIf(c -> c.number < 7);
        FULL_PACK = Set.copyOf(all);
    }

    private SchwimmenTestUtils() {
    }

    /** A factory game of nPlayers RandomPlayers, reset with the given seed. */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, new SchwimmenParameters());
    }

    /** A factory game of nPlayers RandomPlayers with the given parameters, reset with the given seed. */
    static Game newGame(int nPlayers, long seed, SchwimmenParameters params) {
        Game g = GameType.Schwimmen.createGameInstance(nPlayers, seed, params);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A state with the given parameters, player count and seed, set up by the forward model (dealer's choice pending). */
    static SchwimmenGameState newState(SchwimmenParameters params, int nPlayers, long seed, SchwimmenForwardModel fm) {
        params.setRandomSeed(seed);
        SchwimmenGameState state = new SchwimmenGameState(params, nPlayers);
        fm.setup(state);
        return state;
    }

    static SchwimmenGameState newState(int nPlayers, long seed, SchwimmenForwardModel fm) {
        return newState(new SchwimmenParameters(), nPlayers, seed, fm);
    }

    /**
     * The first seed from 'from' whose deal (with default parameters) gives no hand, and no extra hand, a single-suit
     * total of 31 or three Aces - so a Schnauz or Feuer cannot end a scripted game before it starts.
     */
    static long ordinarySeed(int nPlayers, long from) {
        SchwimmenForwardModel fm = new SchwimmenForwardModel();
        for (long seed = from; seed < from + 1000; seed++) {
            SchwimmenGameState s = newState(nPlayers, seed, fm);
            boolean special = isSpecial(s.extraHand.getComponents());
            for (int p = 0; p < nPlayers; p++)
                special |= isSpecial(s.playerHands.get(p).getComponents());
            if (!special) return seed;
        }
        fail("no ordinary deal in 1000 seeds");
        return -1;
    }

    /** Parameters for pagat's chips game (livesGame = true) with the default 3 starting chips. */
    static SchwimmenParameters livesParams() {
        SchwimmenParameters params = new SchwimmenParameters();
        params.setParameterValue("livesGame", true);
        return params;
    }

    /** Set every player's chips (a new array; -1 - k for a player who dropped out with k players left in). */
    static void setChips(SchwimmenGameState state, int... chips) {
        assertEquals("one entry per player", state.getNPlayers(), chips.length);
        state.chips = chips.clone();
    }

    /** Test oracle: the player's result at the end of the chips game. */
    static CoreConstants.GameResult oracleChipsResult(SchwimmenGameState state, int player) {
        // the most chips wins, players sharing the most chips draw, everyone else loses
        int best = Integer.MIN_VALUE, atBest = 0;
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (state.getChips(p) > best) {
                best = state.getChips(p);
                atBest = 1;
            } else if (state.getChips(p) == best) atBest++;
        }
        if (state.getChips(player) < best) return CoreConstants.GameResult.LOSE_GAME;
        return atBest > 1 ? CoreConstants.GameResult.DRAW_GAME : CoreConstants.GameResult.WIN_GAME;
    }

    /** A card from a short code: rank (7-10, J, Q, K, A) then suit (H, D, C, S). e.g. "7H", "10D", "KS". */
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

    static String[] h(String... codes) {
        return codes;
    }

    static List<FrenchCard> cards(String... codes) {
        return Arrays.stream(codes).map(SchwimmenTestUtils::card).toList();
    }

    /** Every deck of the state: the hands, then the extra hand, table, draw deck and discard pile. */
    static List<Deck<FrenchCard>> allDecks(SchwimmenGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.extraHand);
        decks.add(state.table);
        decks.add(state.drawDeck);
        decks.add(state.discardPile);
        return decks;
    }

    private static void takeFromWherever(SchwimmenGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Arrange the position after the dealer kept their hand: the extra hand goes face up to the table, and player
     * 'current' is to play with no passes counted. Call this first, then giveHand / setTable / leaveInDrawDeck.
     * Does not use ChooseHand (so the arrangement does not depend on the rule under test).
     */
    static void startPlay(SchwimmenGameState state, int current) {
        while (state.extraHand.getSize() > 0)
            state.table.add(state.extraHand.draw());
        state.consecutivePasses = 0;
        state.setTurnOwner(current);
    }

    /**
     * startPlay(state, current), then every player's hand and the table set to exactly these cards (all distinct).
     * Displaced cards go to the draw deck (see giveHand / setTable); the discard pile stays empty.
     */
    static void arrangePlay(SchwimmenGameState state, int current, String[] table, String[]... hands) {
        assertEquals("one hand per player", state.getNPlayers(), hands.length);
        startPlay(state, current);
        for (int p = 0; p < hands.length; p++)
            giveHand(state, p, hands[p]);
        setTable(state, table);
        for (int p = 0; p < hands.length; p++)
            assertEquals(cards(hands[p]), state.playerHands.get(p).getComponents());
        assertEquals(cards(table), state.table.getComponents());
        assertAllCardsPresent(state);
    }

    /**
     * Replace the player's hand with exactly these cards, each seen only by its owner. The old hand goes to the
     * bottom of the draw deck.
     */
    static void giveHand(SchwimmenGameState state, int player, String... codes) {
        PartialObservableDeck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.drawDeck.addToBottom(c);
        }
        for (FrenchCard c : cards(codes)) {
            takeFromWherever(state, c);
            hand.addToBottom(c);
        }
    }

    /**
     * Add a card to the player's hand as one taken from the table: visible to every player. The card is moved from
     * wherever it is; nothing is displaced (the hand grows by one - remove a card first with giveHand).
     */
    static void giveTakenCard(SchwimmenGameState state, int player, String code) {
        FrenchCard c = card(code);
        takeFromWherever(state, c);
        boolean[] all = new boolean[state.getNPlayers()];
        Arrays.fill(all, true);
        state.playerHands.get(player).add(c, all);
    }

    /** Replace the table with exactly these cards. The old table cards go to the bottom of the draw deck. */
    static void setTable(SchwimmenGameState state, String... codes) {
        while (state.table.getSize() > 0)
            state.drawDeck.addToBottom(state.table.draw());
        for (FrenchCard c : cards(codes)) {
            takeFromWherever(state, c);
            state.table.addToBottom(c);
        }
    }

    /**
     * Leave exactly k cards in the draw deck (its bottom k): the cards above them go to the discard pile.
     * Call it after giveHand / setTable, which put displaced cards under the draw deck.
     */
    static void leaveInDrawDeck(SchwimmenGameState state, int k) {
        assertTrue("draw deck has fewer than " + k + " cards", state.drawDeck.getSize() >= k);
        while (state.drawDeck.getSize() > k)
            state.discardPile.add(state.drawDeck.draw());
    }

    /**
     * Play one whole turn through the forward model: the turn action, then - if the close decision follows for the
     * same player - Close(false). So the turn passes on as in a game where nobody closes.
     */
    static void takeTurn(SchwimmenGameState state, AbstractForwardModel fm, AbstractAction action) {
        int player = state.getCurrentPlayer();
        fm.next(state, action);
        if (state.isNotTerminal() && state.isActionInProgress()
                && state.currentActionInProgress() instanceof CloseDecision
                && state.getCurrentPlayer() == player)
            fm.next(state, new Close(false));
    }

    /** The 11 turn actions for this hand and table: 3 x 3 single exchanges, exchange all and pass. */
    static Set<AbstractAction> turnActions(String[] hand, String[] table) {
        Set<AbstractAction> expected = new HashSet<>();
        for (String hc : hand)
            for (String tc : table)
                expected.add(new ExchangeOne(card(hc), card(tc)));
        expected.add(new ExchangeAll());
        expected.add(new Pass());
        return expected;
    }

    static final Set<AbstractAction> CLOSE_CHOICES = Set.of(new Close(true), new Close(false));

    /**
     * An ExchangeOne among the available actions that leaves the current player's hand neither 31 in one suit nor
     * three of a kind (so a special hand cannot end a scripted game). Fails if there is none.
     */
    static ExchangeOne harmlessExchange(SchwimmenGameState state, AbstractForwardModel fm) {
        List<FrenchCard> hand = state.playerHands.get(state.getCurrentPlayer()).getComponents();
        for (AbstractAction a : fm.computeAvailableActions(state)) {
            if (a instanceof ExchangeOne ex) {
                List<FrenchCard> after = new ArrayList<>(hand);
                after.remove(ex.handCard);
                after.add(ex.tableCard);
                if (!isSpecial(after) && !isThreeOfAKind(after)) return ex;
            }
        }
        fail("no harmless ExchangeOne available: " + fm.computeAvailableActions(state));
        return null;
    }

    /**
     * Arrange the position straight after the deal (dealer's choice pending, table empty): every player's hand and
     * the extra hand set to exactly these cards (all distinct). All other cards go to the draw deck; the discard pile
     * and table are emptied into it. Conserves the 32 cards. Does not run the special-hand check.
     */
    static void arrangeDeal(SchwimmenGameState state, String[] extra, String[]... hands) {
        assertEquals("one hand per player", state.getNPlayers(), hands.length);
        List<Deck<FrenchCard>> sources = new ArrayList<>(state.playerHands);
        sources.add(state.extraHand);
        sources.add(state.table);
        sources.add(state.discardPile);
        for (Deck<FrenchCard> d : sources)
            for (FrenchCard c : new ArrayList<>(d.getComponents())) {
                d.remove(c);
                state.drawDeck.addToBottom(c);
            }
        for (int p = 0; p < hands.length; p++)
            for (FrenchCard c : cards(hands[p])) {
                state.drawDeck.remove(c);
                state.playerHands.get(p).addToBottom(c);
            }
        for (FrenchCard c : cards(extra)) {
            state.drawDeck.remove(c);
            state.extraHand.addToBottom(c);
        }
        state.consecutivePasses = 0;
        state.setTurnOwner(state.dealer);
        for (int p = 0; p < hands.length; p++)
            assertEquals(cards(hands[p]), state.playerHands.get(p).getComponents());
        assertEquals(cards(extra), state.extraHand.getComponents());
        assertTrue(state.isDealerChoicePending());
        assertAllCardsPresent(state);
    }

    /** Test oracle: the hand value by the full rules. */
    static double oracleHandValue(List<FrenchCard> hand) {
        // three Aces 32, any other three of a kind 30.5, otherwise the best single-suit total
        if (isThreeOfAKind(hand))
            return hand.get(0).type == Ace ? 32 : 30.5;
        return bestSuitTotal(hand);
    }

    /** Test oracle: the suit rank for the tie-break. */
    static int oracleSuitRank(FrenchCard.Suite s) {
        return switch (s) {
            case Clubs -> 4;
            case Spades -> 3;
            case Hearts -> 2;
            case Diamonds -> 1;
        };
    }

    /** Test oracle: the rank that orders hands of equal value. */
    static int oracleTiebreakRank(List<FrenchCard> hand) {
        // three of a kind ranks by the rank of its cards
        if (isThreeOfAKind(hand)) {
            FrenchCard c = hand.get(0);
            return switch (c.type) {
                case Ace -> 14;
                case King -> 13;
                case Queen -> 12;
                case Jack -> 11;
                default -> c.number;
            };
        }
        // otherwise, the rank of the highest suit in which the hand's best total is reached
        int best = bestSuitTotal(hand), rank = 0;
        Map<FrenchCard.Suite, Integer> totals = new EnumMap<>(FrenchCard.Suite.class);
        for (FrenchCard c : hand)
            totals.merge(c.suite, oracleCardValue(c), Integer::sum);
        for (Map.Entry<FrenchCard.Suite, Integer> e : totals.entrySet())
            if (e.getValue() == best) rank = Math.max(rank, oracleSuitRank(e.getKey()));
        return rank;
    }

    /** Test oracle: > 0 if hand a beats hand b, 0 if they share, < 0 if b beats a (default parameters). */
    static int oracleCompare(List<FrenchCard> a, List<FrenchCard> b) {
        int byValue = Double.compare(oracleHandValue(a), oracleHandValue(b));
        if (byValue != 0) return byValue;
        return Integer.compare(oracleTiebreakRank(a), oracleTiebreakRank(b));
    }

    /** Test oracle: the result the player should get when the deal ends with the current hands (default parameters). */
    static CoreConstants.GameResult oracleResult(SchwimmenGameState state, int player) {
        List<FrenchCard> mine = state.getPlayerHand(player).getComponents();
        boolean shared = false;
        for (int p = 0; p < state.getNPlayers(); p++) {
            if (p == player) continue;
            int cmp = oracleCompare(state.getPlayerHand(p).getComponents(), mine);
            if (cmp > 0) return CoreConstants.GameResult.LOSE_GAME;
            if (cmp == 0) shared = true;
        }
        return shared ? CoreConstants.GameResult.DRAW_GAME : CoreConstants.GameResult.WIN_GAME;
    }

    /** True if any player's hand (not the extra hand) is Schnauz or Feuer by the oracle. */
    static boolean anyPlayerSpecial(SchwimmenGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            if (isSpecial(state.getPlayerHand(p).getComponents())) return true;
        return false;
    }

    /** Test oracle: the card's value by the rules. */
    static int oracleCardValue(FrenchCard c) {
        return switch (c.type) {
            case Ace -> 11;
            case King, Queen, Jack -> 10;
            default -> c.number;
        };
    }

    /** Test oracle: the best single-suit total of the cards. */
    static int bestSuitTotal(List<FrenchCard> hand) {
        Map<FrenchCard.Suite, Integer> totals = new EnumMap<>(FrenchCard.Suite.class);
        for (FrenchCard c : hand)
            totals.merge(c.suite, oracleCardValue(c), Integer::sum);
        return totals.values().stream().max(Integer::compare).orElse(0);
    }

    static boolean isThreeOfAKind(List<FrenchCard> hand) {
        return hand.size() == 3 && hand.stream().map(c -> c.type + ":" + c.number).distinct().count() == 1;
    }

    /** True if the hand ends the deal at once: 31 in one suit (Schnauz) or three Aces (Feuer). */
    static boolean isSpecial(List<FrenchCard> hand) {
        return bestSuitTotal(hand) == 31 || (isThreeOfAKind(hand) && hand.get(0).type == Ace);
    }

    /** Every one of the 32 cards is in exactly one deck. */
    static void assertAllCardsPresent(SchwimmenGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 32, all.size());
        assertEquals(FULL_PACK, new HashSet<>(all));
    }

    static boolean visibleToAll(PartialObservableDeck<FrenchCard> hand, int idx, int nPlayers) {
        for (int p = 0; p < nPlayers; p++)
            if (!hand.getVisibilityForPlayer(idx, p)) return false;
        return true;
    }

    static boolean visibleOnlyToOwner(PartialObservableDeck<FrenchCard> hand, int idx, int nPlayers) {
        for (int p = 0; p < nPlayers; p++)
            if (hand.getVisibilityForPlayer(idx, p) != (p == hand.getOwnerId())) return false;
        return true;
    }

    static int indexOf(Deck<FrenchCard> deck, String code) {
        int idx = deck.getComponents().indexOf(card(code));
        assertTrue(code + " is not in " + deck.getComponents(), idx >= 0);
        return idx;
    }
}

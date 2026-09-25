package games.president;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.president.actions.GiveCard;
import games.president.actions.Pass;
import games.president.actions.PlayCards;
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
 * Factories and arrange helpers for the President tests. Every arrange helper MOVES cards from wherever they are
 * (hands, play pile, discard pile); cards no longer wanted go to the discard pile, so the 52 cards are conserved.
 */
class PresidentTestUtils {

    private PresidentTestUtils() {
    }

    /** All 52 cards, one of each. */
    static final Set<FrenchCard> FULL_DECK = new HashSet<>(
            FrenchCard.generateDeck("all", VISIBLE_TO_ALL).getComponents());

    /** A factory game of nPlayers RandomPlayers, reset with the given seed. */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, new PresidentParameters());
    }

    /** A factory game of nPlayers RandomPlayers with the given parameters, reset with the given seed. */
    static Game newGame(int nPlayers, long seed, PresidentParameters params) {
        Game g = GameType.President.createGameInstance(nPlayers, seed, params);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A state with the given parameters, player count and seed, set up by the forward model. */
    static PresidentGameState newState(PresidentParameters params, int nPlayers, long seed, PresidentForwardModel fm) {
        params.setRandomSeed(seed);
        PresidentGameState state = new PresidentGameState(params, nPlayers);
        fm.setup(state);
        return state;
    }

    /** A state with default parameters, set up by the forward model. */
    static PresidentGameState newState(int nPlayers, long seed, PresidentForwardModel fm) {
        return newState(new PresidentParameters(), nPlayers, seed, fm);
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
        return Arrays.stream(codes).map(PresidentTestUtils::card).toList();
    }

    /** Every card in the state: hands, play pile and discard pile. */
    static List<FrenchCard> allCards(PresidentGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        for (Deck<FrenchCard> hand : state.playerHands)
            all.addAll(hand.getComponents());
        all.addAll(state.playPile.getComponents());
        all.addAll(state.discardPile.getComponents());
        return all;
    }

    /** The 52 distinct cards are all present, each exactly once. */
    static void assertAllCardsPresent(PresidentGameState state) {
        List<FrenchCard> all = allCards(state);
        assertEquals("number of cards", 52, all.size());
        assertEquals("distinct cards", FULL_DECK, new HashSet<>(all));
    }

    /** Removes the card from wherever it is in the state; fails if it is nowhere. */
    static void take(PresidentGameState state, FrenchCard c) {
        for (Deck<FrenchCard> hand : state.playerHands)
            if (hand.contains(c)) {
                hand.remove(c);
                return;
            }
        if (state.playPile.contains(c)) {
            state.playPile.remove(c);
            return;
        }
        if (state.discardPile.contains(c)) {
            state.discardPile.remove(c);
            return;
        }
        fail(c + " is not in the state");
    }

    /** Player p's hand becomes exactly these cards (none = empty); its other cards go to the discard pile. */
    static void giveHand(PresidentGameState state, int p, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        Deck<FrenchCard> hand = state.playerHands.get(p);
        for (FrenchCard c : new ArrayList<>(hand.getComponents()))
            if (!wanted.contains(c)) {
                hand.remove(c);
                state.discardPile.add(c);
            }
        for (FrenchCard c : wanted)
            if (!hand.contains(c)) {
                take(state, c);
                hand.add(c);
            }
    }

    /**
     * Arranges a trick in progress: the play pile becomes these cards, the LAST listed on top (the pile's old cards
     * go to the discard pile); lastPlayer played the top set of setSize cards, passesInRow passes have followed it,
     * and toAct is to act. The rank of the top set is the rank of the last card listed.
     */
    static void arrangeTrick(PresidentGameState state, int lastPlayer, int setSize, int passesInRow, int toAct,
                             String... codes) {
        for (FrenchCard c : new ArrayList<>(state.playPile.getComponents())) {
            state.playPile.remove(c);
            state.discardPile.add(c);
        }
        for (FrenchCard c : cards(codes)) {
            take(state, c);
            state.playPile.add(c);   // add puts it on top (index 0)
        }
        state.setLastPlayer(lastPlayer);
        state.setSetSize(setSize);
        state.setPassesInRow(passesInRow);
        state.setTurnOwner(toAct);
    }

    /**
     * Arranges players already out, in this order: their hands are emptied (to the discard pile) and they are
     * the finishing order so far. Scores are NOT set - set them in the test.
     */
    static void arrangeOut(PresidentGameState state, int... players) {
        state.finishingOrder = new ArrayList<>();
        for (int p : players) {
            giveHand(state, p);
            state.finishingOrder.add(p);
        }
    }

    /**
     * Arranges the EXCHANGE phase: the President is to act and has cardsToGive cards still to give to the Scum.
     * Hands are not changed - arrange them with giveHand.
     */
    static void arrangeExchange(PresidentGameState state, int president, int scum, int cardsToGive) {
        state.setGamePhase(PresidentGameState.Phase.EXCHANGE);
        state.scum = scum;
        state.cardsToGive = cardsToGive;
        state.setTurnOwner(president);
    }

    static PresidentParameters multiDealParams(int targetScore, int exchangeCards) {
        PresidentParameters params = new PresidentParameters();
        params.setParameterValue("targetScore", targetScore);
        params.setParameterValue("exchangeCards", exchangeCards);
        return params;
    }

    /** One GiveCard per card in the deck. */
    static Set<AbstractAction> giveCardsFor(Deck<FrenchCard> hand) {
        Set<AbstractAction> actions = new HashSet<>();
        for (FrenchCard c : hand.getComponents())
            actions.add(new GiveCard(c));
        return actions;
    }

    /** A sort key that orders cards as the exchange takes them, the highest key first. */
    static int exchangeOrder(FrenchCard c) {
        // by PresidentUtils.rank; among equal ranks the earlier FrenchCard.Suite counts as higher
        return PresidentUtils.rank(c) * 4 + (3 - c.suite.ordinal());
    }

    /** The number of cards dealt to player p when the deal starts with player first. */
    static int dealtTo(int p, int first, int nPlayers) {
        // 52 / n each, plus one for the first 52 % n players from first
        int seatsAfterFirst = (p - first + nPlayers) % nPlayers;
        return 52 / nPlayers + (seatsAfterFirst < 52 % nPlayers ? 1 : 0);
    }

    /** Asserts that p is to act, then plays count cards of the number with fm.next. */
    static void play(PresidentGameState state, PresidentForwardModel fm, int p, int number, int count) {
        assertEquals("player to act before playing " + count + " x " + number, p, state.getCurrentPlayer());
        fm.next(state, new PlayCards(number, count));
    }

    /** Asserts that p is to act, then passes with fm.next. */
    static void pass(PresidentGameState state, PresidentForwardModel fm, int p) {
        assertEquals("player to act before passing", p, state.getCurrentPlayer());
        fm.next(state, new Pass());
    }

    /** The current legal actions as a set. */
    static Set<AbstractAction> actionSet(PresidentGameState state, PresidentForwardModel fm) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /** The cards of a deck as a set (compare decks by content, never with Deck.equals). */
    static Set<FrenchCard> setOf(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    /** The FrenchCard numbers of the cards in a deck, in deck order (top first). */
    static List<Integer> numbers(Deck<FrenchCard> deck) {
        return deck.getComponents().stream().map(c -> c.number).toList();
    }
}

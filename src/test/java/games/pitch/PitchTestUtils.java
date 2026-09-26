package games.pitch;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import players.simple.RandomPlayer;
import games.tricktaking.CardOrder;
import games.tricktaking.Trick;

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
 * Factories and arrange helpers for the Pitch tests. Every arrange helper MOVES cards from wherever they are
 * (hands, undealt deck, current trick, team decks), so the 52 cards are conserved.
 */
class PitchTestUtils {

    private PitchTestUtils() {
    }

    /** All 52 cards, one of each. */
    static final Set<FrenchCard> FULL_DECK = new HashSet<>(
            FrenchCard.generateDeck("all", VISIBLE_TO_ALL).getComponents());

    /** A factory game (4 RandomPlayers), reset with the given seed. */
    static Game newGame(long seed) {
        return newGame(seed, new PitchParameters());
    }

    /** A factory game (4 RandomPlayers) with the given parameters, reset with the given seed. */
    static Game newGame(long seed, PitchParameters params) {
        Game g = GameType.Pitch.createGameInstance(4, seed, params);
        g.reset(IntStream.range(0, 4).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A 4-player state with default parameters and the given seed, set up by the forward model. */
    static PitchGameState newState(long seed, PitchForwardModel fm) {
        PitchParameters params = new PitchParameters();
        params.setRandomSeed(seed);
        PitchGameState state = new PitchGameState(params, 4);
        fm.setup(state);
        return state;
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
        return Arrays.stream(codes).map(PitchTestUtils::card).toList();
    }

    /** A new face-up deck holding these cards (for PitchUtils tests - not part of any state). */
    static Deck<FrenchCard> deckOf(String... codes) {
        Deck<FrenchCard> deck = new Deck<>("test", VISIBLE_TO_ALL);
        for (FrenchCard c : cards(codes))
            deck.addToBottom(c);
        return deck;
    }

    /** Every card in the state: hands, undealt deck, current trick and both team decks. */
    static List<FrenchCard> allCards(PitchGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        for (Deck<FrenchCard> hand : state.playerHands)
            all.addAll(hand.getComponents());
        all.addAll(state.undealtDeck.getComponents());
        all.addAll(state.currentTrick.getComponents());
        for (Deck<FrenchCard> tricks : state.teamTricks)
            all.addAll(tricks.getComponents());
        return all;
    }

    /** The 52 distinct cards are all present, each exactly once. */
    static void assertAllCardsPresent(PitchGameState state) {
        List<FrenchCard> all = allCards(state);
        assertEquals("number of cards", 52, all.size());
        assertEquals("distinct cards", FULL_DECK, new HashSet<>(all));
    }

    /** Removes the card from wherever it is in the state; fails if it is nowhere. */
    static void take(PitchGameState state, FrenchCard c) {
        for (Deck<FrenchCard> hand : state.playerHands)
            if (hand.contains(c)) {
                hand.remove(c);
                return;
            }
        if (state.undealtDeck.contains(c)) {
            state.undealtDeck.remove(c);
            return;
        }
        for (Deck<FrenchCard> tricks : state.teamTricks)
            if (tricks.contains(c)) {
                tricks.remove(c);
                return;
            }
        if (state.currentTrick.contains(c)) {
            state.currentTrick.remove(c);
            return;
        }
        fail(c + " is not in the state");
    }

    /** Player p's hand becomes exactly these cards; its other cards go to the bottom of the undealt deck. */
    static void giveHand(PitchGameState state, int p, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        Deck<FrenchCard> hand = state.playerHands.get(p);
        for (FrenchCard c : new ArrayList<>(hand.getComponents()))
            if (!wanted.contains(c)) {
                hand.remove(c);
                state.undealtDeck.addToBottom(c);
            }
        for (FrenchCard c : wanted)
            if (!hand.contains(c)) {
                take(state, c);
                hand.add(c);
            }
    }

    /** The team's deck of won cards becomes exactly these cards; its other cards go to the undealt deck. */
    static void setTeamTricks(PitchGameState state, int team, String... codes) {
        List<FrenchCard> wanted = cards(codes);
        Deck<FrenchCard> tricks = state.teamTricks.get(team);
        for (FrenchCard c : new ArrayList<>(tricks.getComponents()))
            if (!wanted.contains(c)) {
                tricks.remove(c);
                state.undealtDeck.addToBottom(c);
            }
        for (FrenchCard c : wanted)
            if (!tricks.contains(c)) {
                take(state, c);
                tricks.addToBottom(c);
            }
    }

    /**
     * Arranges the bidding so far: bids[i] is player i's bid (0 = pass), and player bids.length is to act.
     * Only for positions before the dealer (player 3) has acted.
     */
    static void arrangeBids(PitchGameState state, int... bids) {
        assertTrue(bids.length < 4);
        Arrays.fill(state.playerBids, -1);
        state.pitcher = -1;
        int high = 0;
        for (int p = 0; p < bids.length; p++) {
            state.playerBids[p] = bids[p];
            // the pitcher is the first player to reach the highest bid
            if (bids[p] > high) {
                high = bids[p];
                state.pitcher = p;
            }
        }
        state.setGamePhase(PitchGameState.Phase.BIDDING);
        state.setTurnOwner(bids.length);
    }

    /**
     * Arranges a completed auction: the pitcher bid `bid`, everyone else passed (0), phase PLAYING with `toAct`
     * to lead a new trick, and the given trumps (null = the first lead is still to come).
     */
    static void startPlay(PitchGameState state, int pitcher, int bid, FrenchCard.Suite trumps, int toAct) {
        Arrays.fill(state.playerBids, 0);
        state.playerBids[pitcher] = bid;
        state.pitcher = pitcher;
        state.trumpSuit = trumps;
        state.setGamePhase(PitchGameState.Phase.PLAYING);
        // toAct leads a new trick (arrangeTrick replaces it when cards have already been played)
        state.currentTrick = new Trick<>("CurrentTrick", 4, toAct, CardOrder.STANDARD);
        state.setTurnOwner(toAct);
    }

    /**
     * The current trick becomes the given cards, played in turn from the leader; the player after the last one
     * listed is to act. Cards are taken from wherever they are.
     */
    static void arrangeTrick(PitchGameState state, int leader, String... codes) {
        for (FrenchCard played : state.currentTrick.getComponents())
            state.undealtDeck.addToBottom(played);
        state.currentTrick = new Trick<>("CurrentTrick", 4, leader, CardOrder.STANDARD);
        List<FrenchCard> trick = cards(codes);
        for (FrenchCard c : trick) {
            take(state, c);
            state.currentTrick.play(c);
        }
        state.setTurnOwner((leader + trick.size()) % 4);
    }

    /** Asserts that p is to act, then plays the card with fm.next. */
    static void play(PitchGameState state, PitchForwardModel fm, int p, String code) {
        assertEquals("player to act before " + code, p, state.getCurrentPlayer());
        fm.next(state, new games.pitch.actions.PlayCard(card(code)));
    }

    /** Asserts each player's known voids (players 0-3). */
    static void assertVoids(PitchGameState state, Set<?> v0, Set<?> v1, Set<?> v2, Set<?> v3) {
        assertEquals("player 0 voids", v0, state.getKnownVoids().get(0));
        assertEquals("player 1 voids", v1, state.getKnownVoids().get(1));
        assertEquals("player 2 voids", v2, state.getKnownVoids().get(2));
        assertEquals("player 3 voids", v3, state.getKnownVoids().get(3));
    }

    /** The cards of a deck as a set (compare decks by content, never with Deck.equals). */
    static Set<FrenchCard> setOf(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    /**
     * Arranges the last trick: team decks as given (5 tricks = 20 cards), one card in each hand, trumps Hearts,
     * the auction as given (bids[pitcher] is the pitcher's bid), `leader` to lead. All other cards go to the
     * undealt deck.
     */
    static void arrangeLastTrick(PitchGameState state, int pitcher, int[] bids, int leader, String[] team0,
                                 String[] team1, String h0, String h1, String h2, String h3) {
        assertEquals(20, team0.length + team1.length);
        setTeamTricks(state, 0, team0);
        setTeamTricks(state, 1, team1);
        giveHand(state, 0, h0);
        giveHand(state, 1, h1);
        giveHand(state, 2, h2);
        giveHand(state, 3, h3);
        startPlay(state, pitcher, bids[pitcher], FrenchCard.Suite.Hearts, leader);
        System.arraycopy(bids, 0, state.playerBids, 0, 4);
        assertAllCardsPresent(state);
        assertTrue(state.isNotTerminal());
    }

    /**
     * Oracle: the points each team scores from the cards it won, with the default (RECYCLE) counting.
     * At least one trump must have been played.
     */
    static int[] oraclePoints(List<List<FrenchCard>> won, FrenchCard.Suite trumps) {
        List<FrenchCard> trumpsPlayed = new ArrayList<>();
        for (List<FrenchCard> w : won)
            for (FrenchCard c : w)
                if (c.suite == trumps) trumpsPlayed.add(c);
        // High and Low are the extremes of the trumps played, not of the whole suit
        int high = trumpsPlayed.stream().mapToInt(c -> c.number).max().orElseThrow();
        int low = trumpsPlayed.stream().mapToInt(c -> c.number).min().orElseThrow();
        int[] points = new int[2];
        int[] game = new int[2];
        for (int t = 0; t < 2; t++)
            for (FrenchCard c : won.get(t)) {
                // a trump that is High, Low or the Jack (11) scores 1, even when it is more than one of them
                if (c.suite == trumps && (c.number == high || c.number == low || c.number == 11))
                    points[t]++;
                // card values for the Game point
                game[t] += switch (c.number) {
                    case 14 -> 4;
                    case 13 -> 3;
                    case 12 -> 2;
                    case 11 -> 1;
                    case 10 -> 10;
                    default -> 0;
                };
            }
        // equal totals give the Game point to nobody
        if (game[0] > game[1]) points[0]++;
        if (game[1] > game[0]) points[1]++;
        return points;
    }

    /** Oracle: the player who wins a trick of four cards played by `players` in order. */
    static int oracleTrickWinner(List<Integer> players, List<FrenchCard> played, FrenchCard.Suite trumps) {
        int best = 0;
        for (int i = 1; i < 4; i++) {
            FrenchCard c = played.get(i), b = played.get(best);
            boolean beats = (c.suite == trumps && (b.suite != trumps || c.number > b.number))
                    || (c.suite == b.suite && c.number > b.number);
            if (beats) best = i;
        }
        return players.get(best);
    }

    /** Oracle: the change in each team's score for a deal, given the oracle points (default parameters). */
    static int[] oracleScoreChange(int[] points, int pitcher, int bid, int pitcherTricks) {
        int[] change = points.clone();
        int pt = pitcher % 2;
        if (bid == 5)
            // smudge: +5 for all six tricks and exactly 4 points, otherwise -5
            change[pt] = pitcherTricks == 6 && points[pt] == 4 ? 5 : -5;
        else if (points[pt] < bid)
            // set: the pitching team loses its bid
            change[pt] = -bid;
        return change;
    }
}

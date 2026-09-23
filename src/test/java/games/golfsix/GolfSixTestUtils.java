package games.golfsix;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.GameType;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Helpers to arrange Six-card Golf states. Every card helper moves cards from wherever they currently are (by
 * swapping them), so the 52 cards are always conserved and every deck keeps its size.
 */
final class GolfSixTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private GolfSixTestUtils() {
    }

    /**
     * A real game from the factory with default parameters, reset with random players, for tests driven by fm.next.
     */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /**
     * As newGame(nPlayers, seed), with the given parameters (null for the defaults). The seed overrides any seed on
     * params.
     */
    static Game newGame(int nPlayers, long seed, GolfSixParameters params) {
        Game game = GameType.GolfSix.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A directly set-up state (no Game) with these parameters, which should already have their seed set.
     */
    static GolfSixGameState newState(GolfSixParameters params, int nPlayers) {
        GolfSixGameState state = new GolfSixGameState(params, nPlayers);
        new GolfSixForwardModel().setup(state);
        return state;
    }

    /**
     * A directly set-up state with default parameters and the given seed.
     */
    static GolfSixGameState newState(int nPlayers, long seed) {
        GolfSixParameters params = new GolfSixParameters();
        params.setRandomSeed(seed);
        return newState(params, nPlayers);
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
        return Arrays.stream(codes).map(GolfSixTestUtils::card).toList();
    }

    static List<Deck<FrenchCard>> allDecks(GolfSixGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.grids);
        decks.add(state.drawDeck);
        decks.add(state.discardPile);
        decks.add(state.drawnCard);
        return decks;
    }

    /**
     * Swap the given card (wherever it is) with the card at index idx of target. Visibility stays with the positions.
     */
    static void moveCardTo(GolfSixGameState state, FrenchCard card, Deck<FrenchCard> target, int idx) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            int from = deck.getComponents().indexOf(card);
            if (from < 0) continue;
            FrenchCard displaced = target.get(idx);
            deck.setComponent(from, displaced);
            target.setComponent(idx, card);
            return;
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Put these six cards (by code, in position order) in the player's grid. Face-up status stays with the positions.
     */
    static void setGrid(GolfSixGameState state, int player, String... codes) {
        if (codes.length != GolfSixParameters.GRID_SIZE)
            throw new IllegalArgumentException("one card per grid position");
        for (int pos = 0; pos < codes.length; pos++)
            moveCardTo(state, card(codes[pos]), state.grids.get(player), pos);
    }

    /**
     * Put these cards on top of the draw deck, the first given on top.
     */
    static void stackDrawDeck(GolfSixGameState state, String... codes) {
        for (int i = 0; i < codes.length; i++)
            moveCardTo(state, card(codes[i]), state.drawDeck, i);
    }

    /**
     * Put this card on top of the discard pile.
     */
    static void setTopDiscard(GolfSixGameState state, String code) {
        moveCardTo(state, card(code), state.discardPile, 0);
    }

    /**
     * Turn these positions of the player's grid face-up (visible to all).
     */
    static void faceUp(GolfSixGameState state, int player, int... positions) {
        boolean[] all = new boolean[state.getNPlayers()];
        Arrays.fill(all, true);
        for (int pos : positions)
            state.grids.get(player).setVisibilityOfComponent(pos, all);
    }

    /**
     * Turn positions 0 and 1 of every grid face-up, so that the initial reveal (default initialFaceUp 2) is over for
     * everyone and the current player makes a normal turn.
     */
    static void skipReveal(GolfSixGameState state) {
        for (int p = 0; p < state.getNPlayers(); p++)
            faceUp(state, p, 0, 1);
    }

    /**
     * Give the current player this drawn card, as if drawn from the discard pile (visible to all) or from the draw
     * deck (visible to the current player only). The card is first moved to the top of that pile, then taken off it.
     */
    static void setDrawnCard(GolfSixGameState state, String code, boolean fromDiscard) {
        if (state.drawnCard.getSize() != 0)
            throw new IllegalStateException("there is already a drawn card");
        Deck<FrenchCard> source = fromDiscard ? state.discardPile : state.drawDeck;
        moveCardTo(state, card(code), source, 0);
        boolean[] visibility = new boolean[state.getNPlayers()];
        if (fromDiscard) Arrays.fill(visibility, true);
        else visibility[state.getCurrentPlayer()] = true;
        state.drawnCard.add(source.draw(), visibility);
        state.drawnFromDiscard = fromDiscard;
    }

    /**
     * Move all but the top n cards of the draw deck to the bottom of the discard pile (the top discard stays on top).
     */
    static void leaveInDrawDeck(GolfSixGameState state, int n) {
        while (state.drawDeck.getSize() > n)
            state.discardPile.addToBottom(state.drawDeck.pick(state.drawDeck.getSize() - 1));
    }

    /**
     * Default parameters with the two variant parameters set. No seed is set.
     */
    static GolfSixParameters variant(boolean finalTurns, int nDeals) {
        GolfSixParameters params = new GolfSixParameters();
        params.setParameterValue("finalTurns", finalTurns);
        params.setParameterValue("nDeals", nDeals);
        return params;
    }

    static Set<AbstractAction> actions(GolfSixForwardModel fm, GolfSixGameState state) {
        return new HashSet<>(fm.computeAvailableActions(state));
    }

    /**
     * Integration step: asserts the action is available, applies it with fm.next, then checks all 52 cards are present.
     */
    static void step(GolfSixForwardModel fm, GolfSixGameState state, AbstractAction action) {
        assertTrue(action + " is not available", fm.computeAvailableActions(state).contains(action));
        fm.next(state, action);
        assertAllCardsPresent(state);
    }

    /**
     * The six TurnUp actions of a player with no face-up card (the start of a deal).
     */
    static Set<AbstractAction> allTurnUps() {
        Set<AbstractAction> set = new HashSet<>();
        for (int pos = 0; pos < GolfSixParameters.GRID_SIZE; pos++)
            set.add(new games.golfsix.actions.TurnUp(pos));
        return set;
    }

    /**
     * A copy of every grid's cards, by player then position.
     */
    static List<List<FrenchCard>> gridSnapshot(GolfSixGameState state) {
        List<List<FrenchCard>> snapshot = new ArrayList<>();
        for (PartialObservableDeck<FrenchCard> grid : state.grids)
            snapshot.add(new ArrayList<>(grid.getComponents()));
        return snapshot;
    }

    static void assertAllCardsPresent(GolfSixGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 52, all.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(all));
    }

    /**
     * Whether every observer can see the card at this position (the rules' "face-up").
     */
    static boolean visibleToAll(PartialObservableDeck<FrenchCard> deck, int idx, int nPlayers) {
        for (int o = 0; o < nPlayers; o++)
            if (!deck.getVisibilityForPlayer(idx, o)) return false;
        return true;
    }

    /**
     * Whether no observer can see the card at this position (the rules' "face-down").
     */
    static boolean visibleToNone(PartialObservableDeck<FrenchCard> deck, int idx, int nPlayers) {
        for (int o = 0; o < nPlayers; o++)
            if (deck.getVisibilityForPlayer(idx, o)) return false;
        return true;
    }

    /**
     * The face-up positions of the player's grid, as a string of 6 characters: 'U' face-up to all, 'D' face-down to
     * all, '?' anything else (which the rules never allow). e.g. "UUDDDD".
     */
    static String faceUpPattern(GolfSixGameState state, int player) {
        StringBuilder sb = new StringBuilder();
        PartialObservableDeck<FrenchCard> grid = state.grids.get(player);
        for (int pos = 0; pos < GolfSixParameters.GRID_SIZE; pos++)
            sb.append(visibleToAll(grid, pos, state.getNPlayers()) ? 'U'
                    : visibleToNone(grid, pos, state.getNPlayers()) ? 'D' : '?');
        return sb.toString();
    }

    /**
     * Test oracle for the default card values, written independently of GolfSixUtils: A 1, 2 -2, 3-10 face value,
     * J and Q 10, K 0; the two cards of a column of the same rank score 0.
     */
    static int oracleGridScore(List<FrenchCard> grid) {
        int total = 0;
        for (int col = 0; col < 3; col++) {
            FrenchCard top = grid.get(col), bottom = grid.get(col + 3);
            boolean sameRank = top.type == bottom.type && top.number == bottom.number;
            if (!sameRank) total += oracleValue(top) + oracleValue(bottom);
        }
        return total;
    }

    private static int oracleValue(FrenchCard c) {
        if (c.type == Ace) return 1;
        if (c.type == King) return 0;
        if (c.type == Jack || c.type == Queen) return 10;
        return c.number == 2 ? -2 : c.number;
    }
}

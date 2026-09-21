package games.agram;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.tricktaking.PlayCard;
import players.simple.RandomPlayer;
import games.GameType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.components.FrenchCard.FrenchCardType.Ace;
import static core.components.FrenchCard.FrenchCardType.Number;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Helpers to arrange Agram states. Every helper moves cards from wherever they currently are,
 * so the 35 cards are always conserved.
 */
final class AgramTestUtils {

    /**
     * The 35 Agram cards, written out from the rules: A, 10, 9, ..., 3 in each suit, without the Ace of Spades.
     */
    static final List<FrenchCard> FULL_DECK = new ArrayList<>();

    static {
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            if (suit != FrenchCard.Suite.Spades)
                FULL_DECK.add(new FrenchCard(Ace, suit));
            for (int n = 3; n <= 10; n++)
                FULL_DECK.add(new FrenchCard(Number, suit, n));
        }
    }

    private AgramTestUtils() {
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
    static Game newGame(int nPlayers, long seed, AgramParameters params) {
        Game game = GameType.Agram.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A card from a short code: rank (3-10, A) then suit (H, D, C, S). e.g. "AH", "10S", "7C".
     */
    static FrenchCard card(String code) {
        FrenchCard.Suite suit = switch (code.charAt(code.length() - 1)) {
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            default -> throw new IllegalArgumentException("Unknown suit in " + code);
        };
        String rank = code.substring(0, code.length() - 1);
        return rank.equals("A") ? new FrenchCard(Ace, suit) : new FrenchCard(Number, suit, Integer.parseInt(rank));
    }

    static PlayCard play(String code) {
        return new PlayCard(card(code));
    }

    /**
     * One PlayCard per card code.
     */
    static HashSet<AbstractAction> plays(String... codes) {
        HashSet<AbstractAction> actions = new HashSet<>();
        for (String c : codes)
            actions.add(play(c));
        return actions;
    }

    static List<Deck<FrenchCard>> allDecks(AgramGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.drawDeck);
        decks.add(state.currentTrick);
        decks.add(state.discardPile);
        return decks;
    }

    private static void takeFromWherever(AgramGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Replace the player's hand with exactly these card codes. The old hand goes to the bottom of the undealt cards.
     */
    static void giveHand(AgramGameState state, int player, String... codes) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.drawDeck.addToBottom(c);
        }
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromWherever(state, c);
            hand.add(c);
        }
    }

    /**
     * Play these cards in turn with fm.next, from whoever is the current player.
     */
    static void playCards(AgramGameState state, AbstractForwardModel fm, String... codes) {
        for (String code : codes)
            fm.next(state, play(code));
    }

    /**
     * Deal a one-trick deal and play it: each player in turn, from the current player, is given just the next card
     * (giveHand, so the rest of their hand goes to the undealt cards) and then the cards are played in that order.
     * The deal ends when the last card is played, as every hand is then empty.
     */
    static void playOneTrickDeal(AgramGameState state, AbstractForwardModel fm, String... codesInPlayOrder) {
        int n = state.getNPlayers();
        assertEquals("one card per player", n, codesInPlayOrder.length);
        int leader = state.getCurrentPlayer();
        for (int i = 0; i < n; i++)
            giveHand(state, (leader + i) % n, codesInPlayOrder[i]);
        playCards(state, fm, codesInPlayOrder);
    }

    /**
     * The winner of a trick, from the rules: whoever played the highest card of the suit of the first card.
     */
    static int expectedTrickWinner(List<FrenchCard> cards, List<Integer> players) {
        int best = 0;
        for (int i = 1; i < cards.size(); i++) {
            if (cards.get(i).suite == cards.get(0).suite && cards.get(i).number > cards.get(best).number)
                best = i;
        }
        return players.get(best);
    }

    static List<FrenchCard> cardsOf(Deck<FrenchCard> deck) {
        return new ArrayList<>(deck.getComponents());
    }

    static List<FrenchCard> cards(String... codes) {
        List<FrenchCard> list = new ArrayList<>();
        for (String c : codes)
            list.add(card(c));
        return list;
    }

    /**
     * Play one random legal action; fails (rather than throwing from Random) if none is offered.
     */
    static void playRandomAction(AgramGameState state, AbstractForwardModel fm, Random rnd) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse("no actions offered to player " + state.getCurrentPlayer() + " in a game in progress",
                actions.isEmpty());
        fm.next(state, actions.get(rnd.nextInt(actions.size())));
    }

    /**
     * No player's hand in handsFrom holds a card of a suit that voidsFrom records them as known to be void in.
     * handsFrom may be a (redeterminised) copy of voidsFrom, so that the check does not rely on the copy's knownVoids.
     */
    static void assertHandsRespectKnownVoids(String label, AgramGameState handsFrom, AgramGameState voidsFrom) {
        for (int p = 0; p < voidsFrom.getNPlayers(); p++) {
            Set<FrenchCard.Suite> voids = voidsFrom.getKnownVoids().get(p);
            for (FrenchCard c : handsFrom.getPlayerHands().get(p).getComponents())
                assertFalse(label + ": player " + p + " is known to be void in " + voids + " but holds " + c,
                        voids.contains(c.suite));
        }
    }

    static void assertAllCardsPresent(AgramGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 35, all.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(all));
    }
}

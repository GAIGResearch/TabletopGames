package games.goofspiel;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.goofspiel.actions.Bid;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static core.components.FrenchCard.FrenchCardType.*;
import static org.junit.Assert.*;

/**
 * Factories, card builders and arrange helpers for the Goofspiel tests. The arrange helpers move cards
 * between the state's decks; they never create new ones.
 */
class GoofspielTestUtils {

    /** The ranks of a full suit, lowest (Ace, value 1) first. */
    static final List<String> RANKS = List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K");

    /** The ranks of a full suit with aceHigh, lowest (2) first: the Ace comes after the King, value 14. */
    static final List<String> RANKS_ACE_HIGH = List.of("2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A");

    /** A seeded game with random players, created through the GameType factory. */
    static Game newGame(int nPlayers, long seed) {
        return newGame(nPlayers, seed, null);
    }

    /** As newGame(nPlayers, seed), with the given parameters (null for the defaults). */
    static Game newGame(int nPlayers, long seed, GoofspielParameters params) {
        Game g = params == null ? GameType.Goofspiel.createGameInstance(nPlayers, seed)
                : GameType.Goofspiel.createGameInstance(nPlayers, seed, params);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A state set up directly (default parameters, the given seed). */
    static GoofspielGameState newState(int nPlayers, long seed) {
        return newState(nPlayers, seed, new GoofspielParameters());
    }

    /** A state set up directly with the given parameters and seed. */
    static GoofspielGameState newState(int nPlayers, long seed, GoofspielParameters params) {
        params.setRandomSeed(seed);
        GoofspielGameState state = new GoofspielGameState(params, nPlayers);
        new GoofspielForwardModel().setup(state);
        return state;
    }

    /** A card from its rank and suit letter, e.g. "9C", "10H", "AD", "KS". */
    static FrenchCard card(String name) {
        String rank = name.substring(0, name.length() - 1);
        FrenchCard.Suite suit = switch (name.charAt(name.length() - 1)) {
            case 'C' -> FrenchCard.Suite.Clubs;
            case 'S' -> FrenchCard.Suite.Spades;
            case 'H' -> FrenchCard.Suite.Hearts;
            case 'D' -> FrenchCard.Suite.Diamonds;
            default -> throw new IllegalArgumentException(name);
        };
        return switch (rank) {
            case "A" -> new FrenchCard(Ace, suit);
            case "J" -> new FrenchCard(Jack, suit);
            case "Q" -> new FrenchCard(Queen, suit);
            case "K" -> new FrenchCard(King, suit);
            default -> new FrenchCard(Number, suit, Integer.parseInt(rank));
        };
    }

    /** The 13 cards of a suit, Ace to King, written out from the rules (not from GoofspielParameters). */
    static List<FrenchCard> fullSuit(FrenchCard.Suite suit) {
        char letter = suit.name().charAt(0);
        return RANKS.stream().map(r -> card(r + letter)).toList();
    }

    /** The default (ace low) value of a card, written out from the rules: Ace 1, 2-10, J 11, Q 12, K 13. */
    static int value(FrenchCard c) {
        return c.type == Ace ? 1 : c.number;
    }

    /** The value of a card, written out from the rules: as value(c), except that with aceHigh the Ace is 14. */
    static int value(FrenchCard c, boolean aceHigh) {
        return c.type == Ace ? (aceHigh ? 14 : 1) : c.number;
    }

    /**
     * The cards of a suit in play, written out from the rules: the n lowest-valued ranks, lowest first
     * (ace low: A..n; aceHigh: 2..n+1, so the Ace only with n = 13).
     */
    static List<FrenchCard> suit(FrenchCard.Suite suit, int cardsPerSuit, boolean aceHigh) {
        char letter = suit.name().charAt(0);
        return (aceHigh ? RANKS_ACE_HIGH : RANKS).subList(0, cardsPerSuit).stream().map(r -> card(r + letter)).toList();
    }

    /**
     * Parameters set through setParameterValue from name/value pairs, e.g.
     * params("tieRule", TieRule.DISCARD, "cardsPerSuit", 5).
     */
    static GoofspielParameters params(Object... nameValuePairs) {
        GoofspielParameters params = new GoofspielParameters();
        for (int i = 0; i < nameValuePairs.length; i += 2)
            params.setParameterValue((String) nameValuePairs[i], nameValuePairs[i + 1]);
        return params;
    }

    /** The cards of a deck as a multiset (count per card), ignoring order. */
    static Map<FrenchCard, Integer> multiset(Collection<FrenchCard> cards) {
        Map<FrenchCard, Integer> m = new HashMap<>();
        for (FrenchCard c : cards) m.merge(c, 1, Integer::sum);
        return m;
    }

    static Map<FrenchCard, Integer> multiset(Deck<FrenchCard> deck) {
        return multiset(deck.getComponents());
    }

    static Map<FrenchCard, Integer> cards(String... names) {
        return multiset(Arrays.stream(names).map(GoofspielTestUtils::card).toList());
    }

    /**
     * Arrange the order in which the prizes come up: the first card named goes on offer now, the next one is on
     * top of the prize deck, and so on. Cards not named follow in their current order. The prizes currently
     * on offer go back into the prize deck first. Only for the start of a round with a single prize on offer.
     */
    static void arrangePrizes(GoofspielGameState state, String... prizes) {
        Deck<FrenchCard> deck = state.getPrizeDeck();
        Deck<FrenchCard> offer = state.getPrizesOnOffer();
        while (offer.getSize() > 0)
            deck.add(offer.draw());
        for (int i = prizes.length - 1; i >= 0; i--) {
            FrenchCard c = card(prizes[i]);
            deck.remove(c);
            deck.add(c);
        }
        offer.add(deck.draw());
    }

    /** Players 0, 1, ... bid the named cards in seat order, one fm.next each. */
    static void playRound(AbstractForwardModel fm, GoofspielGameState state, String... bids) {
        for (int p = 0; p < bids.length; p++)
            fm.next(state, new Bid(p, card(bids[p])));
    }

    /** Values of the given cards, sorted. */
    static List<Integer> values(Collection<FrenchCard> cards) {
        return cards.stream().map(GoofspielTestUtils::value).sorted().toList();
    }

    /**
     * Every card is in play exactly once: each player's suit is split between their hand, bid and played bids,
     * and the Diamonds between the prize deck, the prizes on offer, the won prizes and the discarded prizes.
     * The suits are those of the state's cardsPerSuit and aceHigh (see suit(...)).
     */
    static void assertAllCardsPresent(GoofspielGameState state) {
        GoofspielParameters params = (GoofspielParameters) state.getGameParameters();
        int n = (int) params.getParameterValue("cardsPerSuit");
        boolean aceHigh = (boolean) params.getParameterValue("aceHigh");
        FrenchCard.Suite[] handSuits = {FrenchCard.Suite.Clubs, FrenchCard.Suite.Spades, FrenchCard.Suite.Hearts};
        for (int p = 0; p < state.getNPlayers(); p++) {
            List<FrenchCard> own = new ArrayList<>(state.getHand(p).getComponents());
            own.addAll(state.getBid(p).getComponents());
            own.addAll(state.getPlayedBids(p).getComponents());
            assertEquals("player " + p + "'s cards", multiset(suit(handSuits[p % 3], n, aceHigh)), multiset(own));
        }
        List<FrenchCard> prizes = new ArrayList<>(state.getPrizeDeck().getComponents());
        prizes.addAll(state.getPrizesOnOffer().getComponents());
        prizes.addAll(state.getDiscardedPrizes().getComponents());
        for (int p = 0; p < state.getNPlayers(); p++)
            prizes.addAll(state.getWonPrizes(p).getComponents());
        assertEquals("prize cards", multiset(suit(FrenchCard.Suite.Diamonds, n, aceHigh)), multiset(prizes));
    }
}

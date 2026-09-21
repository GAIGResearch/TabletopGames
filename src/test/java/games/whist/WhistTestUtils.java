package games.whist;

import core.AbstractPlayer;
import core.Game;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.tricktaking.TrickTakingTestUtils.card;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Helpers to arrange Whist states. Every helper moves cards from wherever they currently are (hands, trick, discard
 * pile), so the 52 cards are always conserved. Card codes are as TrickTakingTestUtils.card ("10H", "KS").
 */
final class WhistTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private WhistTestUtils() {
    }

    /**
     * A real 4-player game from the factory, reset with random players, for integration tests driven by fm.next.
     */
    static Game newGame(long seed) {
        return newGame(seed, null);
    }

    /**
     * As newGame(seed), with the given parameters (null for the defaults). The seed overrides any seed on params.
     */
    static Game newGame(long seed, WhistParameters params) {
        Game game = GameType.Whist.createGameInstance(4, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A state from direct setup with default parameters and the given seed, for unit tests.
     */
    static WhistGameState newState(long seed) {
        WhistParameters params = new WhistParameters();
        params.setRandomSeed(seed);
        WhistGameState state = new WhistGameState(params, 4);
        new WhistForwardModel().setup(state);
        return state;
    }

    /**
     * Parameters with the given number of deals and trump settings (set through setParameterValue).
     */
    static WhistParameters params(int nDeals, WhistParameters.TrumpMode trumpMode, boolean noTrumpsInRotation) {
        WhistParameters params = new WhistParameters();
        params.setParameterValue("nDeals", nDeals);
        params.setParameterValue("trumpMode", trumpMode);
        params.setParameterValue("noTrumpsInRotation", noTrumpsInRotation);
        return params;
    }

    /**
     * A state from direct setup with the given parameters and seed, for unit tests.
     */
    static WhistGameState newState(long seed, WhistParameters params) {
        params.setRandomSeed(seed);
        WhistGameState state = new WhistGameState(params, 4);
        new WhistForwardModel().setup(state);
        return state;
    }

    /**
     * Arrange and play the 13th trick of the current deal so that the given player wins it: the tricks already taken
     * are tricksBefore (they should sum to 12), the winner leads the Ace of Clubs and the others follow with the 2, 3
     * and 4 of Clubs in turn - so the winner is the same whatever the trumps. Ends the deal through fm.next.
     */
    static void playLastTrick(WhistGameState state, WhistForwardModel fm, int[] tricksBefore, int winner) {
        assertTrue("the game is over before this deal's last trick", state.isNotTerminal());
        String[] codes = new String[4];
        String[] followers = {"2C", "3C", "4C"};
        codes[winner] = "AC";
        for (int i = 1; i < 4; i++)
            codes[(winner + i) % 4] = followers[i - 1];
        arrangeLastTrick(state, winner, tricksBefore, codes);
        for (int i = 0; i < 4; i++)
            playCards(state, fm, codes[(winner + i) % 4]);
    }

    static List<Deck<FrenchCard>> allDecks(WhistGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.add(state.discardPile);
        return decks;
    }

    private static void takeFromWherever(WhistGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    /**
     * Replace the player's hand with exactly these cards. The old hand goes to the bottom of the discard pile.
     */
    static void giveHand(WhistGameState state, int player, String... codes) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.discardPile.addToBottom(c);
        }
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromWherever(state, c);
            hand.addToBottom(c);
        }
    }

    /**
     * Replace the current trick (whose cards go to the discard pile) by one led by the given player, holding these
     * cards in play order, and make the next player to play to it the current player. Does not use Trick.play.
     */
    static void arrangeTrick(WhistGameState state, int leader, String... codes) {
        state.discardPile.add(state.currentTrick);
        state.currentTrick.clear();
        state.currentTrick = new Trick("CurrentTrick", 4, leader);
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromWherever(state, c);
            state.currentTrick.addToBottom(c);
        }
        state.setTurnOwner((leader + codes.length) % 4);
    }

    /**
     * Make the card the turned-up trump card and its suit trumps. Does not move the card.
     */
    static void setTrumps(WhistGameState state, String code) {
        state.trumpCard = card(code);
        state.trumpSuit = state.trumpCard.suite;
    }

    /**
     * Arrange the 13th trick of the deal: every other card on the discard pile, player p holding just lastCards[p],
     * the tricks already taken as given (they should sum to 12), an empty trick led by the leader, who is to play.
     */
    static void arrangeLastTrick(WhistGameState state, int leader, int[] tricksTaken, String... lastCards) {
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.discardPile.add(hand);
            hand.clear();
        }
        for (int p = 0; p < 4; p++)
            giveHand(state, p, lastCards[p]);
        arrangeTrick(state, leader);
        state.tricksTaken = tricksTaken.clone();
    }

    /**
     * Replace every hand: hands[p] becomes player p's hand, taking the cards from wherever they are. Cards in no
     * listed hand end on the discard pile.
     */
    @SafeVarargs
    static void arrangeHands(WhistGameState state, List<FrenchCard>... hands) {
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.discardPile.add(hand);
            hand.clear();
        }
        for (int p = 0; p < hands.length; p++) {
            for (FrenchCard c : hands[p]) {
                takeFromWherever(state, c);
                state.playerHands.get(p).addToBottom(c);
            }
        }
    }

    static void playCards(WhistGameState state, WhistForwardModel fm, String... codes) {
        for (String code : codes)
            fm.next(state, new PlayCard(card(code)));
    }

    static void assertAllCardsPresent(WhistGameState state) {
        List<FrenchCard> cards = new ArrayList<>();
        allDecks(state).forEach(d -> cards.addAll(d.getComponents()));
        assertEquals("number of cards", 52, cards.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(cards));
    }
}

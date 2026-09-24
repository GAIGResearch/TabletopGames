package games.klaverjassen;

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

/**
 * Helpers to arrange Klaverjassen states. Every arrange helper moves cards from wherever they currently are (hands,
 * trick, discard pile), so the 32 cards are always conserved. Card codes are as TrickTakingTestUtils.card ("10H",
 * "JS", "AC"). Team 0 is players 0 and 2, team 1 players 1 and 3.
 */
final class KlaverjassenTestUtils {

    /**
     * The 32-card pack: Seven to Ace in each suit (FrenchCard numbers Aces as 14).
     */
    static final List<FrenchCard> FULL_PACK = FrenchCard.generateDeck("FullPack", VISIBLE_TO_ALL).getComponents()
            .stream().filter(c -> c.number >= 7).toList();

    private static final List<Integer> TRUMP_ORDER = List.of(7, 8, 12, 13, 10, 14, 9, 11);  // lowest first: 7 8 Q K 10 A 9 J
    private static final List<Integer> PLAIN_ORDER = List.of(7, 8, 9, 11, 12, 13, 10, 14);  // lowest first: 7 8 9 J Q K 10 A

    private KlaverjassenTestUtils() {
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
    static Game newGame(long seed, KlaverjassenParameters params) {
        Game game = GameType.Klaverjassen.createGameInstance(4, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A state from direct setup with default parameters and the given seed, for unit tests.
     */
    static KlaverjassenGameState newState(long seed) {
        return newState(seed, new KlaverjassenParameters());
    }

    /**
     * A state from direct setup with the given parameters and seed, for unit tests.
     */
    static KlaverjassenGameState newState(long seed, KlaverjassenParameters params) {
        params.setRandomSeed(seed);
        KlaverjassenGameState state = new KlaverjassenGameState(params, 4);
        new KlaverjassenForwardModel().setup(state);
        return state;
    }

    /**
     * Arrange trumps without going through ChooseTrump: set the trump suit and replace the current trick (its cards go
     * to the discard pile) by an empty one led by the trump chooser with the matching KlaverjassenCardOrder. Does not
     * change the current player.
     */
    static void setTrumps(KlaverjassenGameState state, FrenchCard.Suite trumps) {
        state.trumpSuit = trumps;
        state.discardPile.add(state.currentTrick);
        state.currentTrick.clear();
        state.currentTrick = new Trick("CurrentTrick", 4, state.getTrumpChooser(), new KlaverjassenCardOrder(trumps));
    }

    /**
     * A free-standing 4-player trick with the Klaverjassen card order for the given trumps, led by the leader and
     * holding these cards in play order (built with addToBottom, not Trick.play). The cards are new objects, so use
     * this only for tests that do not touch a state.
     */
    static Trick trick(FrenchCard.Suite trumps, int leader, String... codes) {
        Trick t = new Trick("Trick", 4, leader, new KlaverjassenCardOrder(trumps));
        for (String code : codes)
            t.addToBottom(card(code));
        return t;
    }

    static List<Deck<FrenchCard>> allDecks(KlaverjassenGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.add(state.discardPile);
        return decks;
    }

    private static void takeFromWherever(KlaverjassenGameState state, FrenchCard card) {
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
    static void giveHand(KlaverjassenGameState state, int player, String... codes) {
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
     * Replace every hand: hands[p] becomes player p's hand, taking the cards from wherever they are. Cards in no
     * listed hand end on the discard pile.
     */
    @SafeVarargs
    static void arrangeHands(KlaverjassenGameState state, List<FrenchCard>... hands) {
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

    /**
     * Replace the current trick (whose cards go to the discard pile) by one led by the given player, with the card
     * order of the current trumps (state.trumpSuit), holding these cards in play order; make the next player to play
     * to it the current player. Does not use Trick.play, so records no voids.
     */
    static void arrangeTrick(KlaverjassenGameState state, int leader, String... codes) {
        state.discardPile.add(state.currentTrick);
        state.currentTrick.clear();
        state.currentTrick = new Trick("CurrentTrick", 4, leader, new KlaverjassenCardOrder(state.trumpSuit));
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromWherever(state, c);
            state.currentTrick.addToBottom(c);
        }
        state.setTurnOwner((leader + codes.length) % 4);
    }

    /**
     * Arrange the 8th (last) trick of the hand, with trumps already set: every other card on the discard pile,
     * player p holding just lastCards[p], an empty trick led by the leader (who is to play), and the hand's card
     * points and tricks so far as given (tricksBefore should sum to 7).
     */
    static void arrangeLastTrick(KlaverjassenGameState state, int leader, int[] pointsBefore, int[] tricksBefore,
                                 String... lastCards) {
        for (Deck<FrenchCard> hand : state.playerHands) {
            state.discardPile.add(hand);
            hand.clear();
        }
        for (int p = 0; p < 4; p++)
            giveHand(state, p, lastCards[p]);
        arrangeTrick(state, leader);
        state.handPoints = pointsBefore.clone();
        state.tricksWon = tricksBefore.clone();
    }

    static void playCards(KlaverjassenGameState state, KlaverjassenForwardModel fm, String... codes) {
        for (String code : codes)
            fm.next(state, new PlayCard(card(code)));
    }

    /**
     * All 32 cards of the pack, each exactly once, across the hands, the current trick and the discard pile.
     */
    static void assertAllCardsPresent(KlaverjassenGameState state) {
        List<FrenchCard> cards = new ArrayList<>();
        allDecks(state).forEach(d -> cards.addAll(d.getComponents()));
        assertEquals("number of cards", 32, cards.size());
        assertEquals(new HashSet<>(FULL_PACK), new HashSet<>(cards));
    }

    /**
     * Oracle from the rules, for random-play tests: the card points of the card with the given trumps.
     */
    static int expectedCardPoints(FrenchCard c, FrenchCard.Suite trumps) {
        boolean trump = c.suite == trumps;
        return switch (c.number) {
            case 11 -> trump ? 20 : 2;   // Jack
            case 9 -> trump ? 14 : 0;
            case 14 -> 11;               // Ace
            case 10 -> 10;
            case 13 -> 4;                // King
            case 12 -> 3;                // Queen
            default -> 0;                // 8, 7
        };
    }

    /**
     * Oracle from the rules, for random-play tests: the index in the (complete or partial) trick of the card winning
     * it.
     */
    static int expectedWinningIndex(List<FrenchCard> trick, FrenchCard.Suite trumps) {
        // the highest trump, else the highest card of the suit led
        int best = 0;
        for (int i = 1; i < trick.size(); i++) {
            FrenchCard c = trick.get(i), b = trick.get(best);
            if (c.suite == b.suite) {
                List<Integer> order = c.suite == trumps ? TRUMP_ORDER : PLAIN_ORDER;
                if (order.indexOf(c.number) > order.indexOf(b.number)) best = i;
            } else if (c.suite == trumps) {
                best = i;
            }
        }
        return best;
    }

    /**
     * Oracle from the Amsterdam rules, for random-play tests: the cards of the hand (in hand order) that the player to
     * play to the (partial) trick may play.
     */
    static List<FrenchCard> expectedLegalPlays(List<FrenchCard> hand, List<FrenchCard> trick, int leader,
                                               FrenchCard.Suite trumps,
                                               KlaverjassenParameters.PartnerTrumpRule rule) {
        // leading: any card
        if (trick.isEmpty())
            return new ArrayList<>(hand);
        // partners sit two apart
        int player = (leader + trick.size()) % 4;
        FrenchCard.Suite led = trick.get(0).suite;
        int winningIndex = expectedWinningIndex(trick, trumps);
        FrenchCard winning = trick.get(winningIndex);
        boolean partnerWinning = (leader + winningIndex) % 4 == (player + 2) % 4;
        boolean winningIsTrump = winning.suite == trumps;
        List<FrenchCard> trumpsHeld = hand.stream().filter(c -> c.suite == trumps).toList();
        List<FrenchCard> higherTrumps = trumpsHeld.stream()
                .filter(c -> !winningIsTrump || TRUMP_ORDER.indexOf(c.number) > TRUMP_ORDER.indexOf(winning.number))
                .toList();
        List<FrenchCard> nonTrumps = hand.stream().filter(c -> c.suite != trumps).toList();

        // trump led and holding trumps: those beating the highest trump in the trick (the winning card), else any trump
        if (led == trumps && !trumpsHeld.isEmpty())
            return new ArrayList<>(higherTrumps.isEmpty() ? trumpsHeld : higherTrumps);
        // plain suit led and holding it: any card of that suit
        if (led != trumps) {
            List<FrenchCard> follow = hand.stream().filter(c -> c.suite == led).toList();
            if (!follow.isEmpty())
                return new ArrayList<>(follow);
        }
        // an opponent winning: trumps that beat the winning card (any trump if it is plain), else non-trumps, else
        // anything
        if (!partnerWinning) {
            if (!higherTrumps.isEmpty()) return new ArrayList<>(higherTrumps);
            if (!nonTrumps.isEmpty()) return new ArrayList<>(nonTrumps);
            return new ArrayList<>(hand);
        }
        // partner winning: with a plain card anything; with a trump, DISCARD allows non-trumps and NO_UNDERTRUMP also
        // trumps above partner's, else anything
        if (!winningIsTrump)
            return new ArrayList<>(hand);
        List<FrenchCard> allowed = hand.stream().filter(c -> c.suite != trumps
                || (rule == KlaverjassenParameters.PartnerTrumpRule.NO_UNDERTRUMP && higherTrumps.contains(c))).toList();
        return new ArrayList<>(allowed.isEmpty() ? hand : allowed);
    }

    /**
     * Oracle from the rules, for random-play tests: the roem of the four cards of a completed trick with the given
     * trumps.
     */
    static int expectedRoem(List<FrenchCard> trick, FrenchCard.Suite trumps) {
        int roem = 0;
        // the longest run of one suit in the sequence A K Q J 10 9 8 7 (numbers 14..7 consecutive): 3 cards 20,
        // 4 cards 50
        int longest = 1;
        for (FrenchCard.Suite suit : FrenchCard.Suite.values()) {
            boolean[] has = new boolean[15];
            trick.stream().filter(c -> c.suite == suit).forEach(c -> has[c.number] = true);
            int run = 0;
            for (int n = 7; n <= 14; n++) {
                run = has[n] ? run + 1 : 0;
                longest = Math.max(longest, run);
            }
        }
        if (longest >= 4) roem += 50;
        else if (longest == 3) roem += 20;
        boolean trumpKing = trick.stream().anyMatch(c -> c.suite == trumps && c.number == 13);
        boolean trumpQueen = trick.stream().anyMatch(c -> c.suite == trumps && c.number == 12);
        if (trumpKing && trumpQueen) roem += 20;   // stuk
        // four Kings, Queens, Aces or Tens 100; four Jacks 200
        if (trick.size() == 4 && trick.stream().allMatch(c -> c.number == trick.get(0).number)) {
            int n = trick.get(0).number;
            if (n == 11) roem += 200;
            else if (n >= 10) roem += 100;       // 10, Q, K, A
        }
        return roem;
    }
}

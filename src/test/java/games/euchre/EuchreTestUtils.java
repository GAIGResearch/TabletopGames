package games.euchre;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.euchre.actions.CallTrump;
import games.euchre.actions.Discard;
import games.euchre.actions.Pass;
import games.tricktaking.PlayCard;
import games.tricktaking.Trick;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static games.tricktaking.TrickTakingTestUtils.card;
import static games.tricktaking.TrickTakingTestUtils.cards;
import static org.junit.Assert.*;

/**
 * Helpers to arrange Euchre states. Every helper moves cards from wherever they are, so the 24 cards are always
 * conserved. Card codes are as TrickTakingTestUtils.card ("10H", "KS").
 */
final class EuchreTestUtils {

    /**
     * The 24 cards: 9, 10, J, Q, K, A of each suit.
     */
    static final List<FrenchCard> ALL_CARDS = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents()
            .stream().filter(c -> c.number >= 9).toList();

    private EuchreTestUtils() {
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
    static Game newGame(long seed, EuchreParameters params) {
        Game game = GameType.Euchre.createGameInstance(4, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < 4; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
    }

    /**
     * A state from direct setup with default parameters and the given seed, for unit tests.
     */
    static EuchreGameState newState(long seed) {
        return newState(seed, new EuchreParameters());
    }

    /**
     * As newState(seed), with the given parameters. The seed overrides any seed on params.
     */
    static EuchreGameState newState(long seed, EuchreParameters params) {
        params.setRandomSeed(seed);
        EuchreGameState state = new EuchreGameState(params, 4);
        new EuchreForwardModel().setup(state);
        return state;
    }

    /**
     * Parameters with the given target score (set through setParameterValue).
     */
    static EuchreParameters params(int targetScore) {
        EuchreParameters params = new EuchreParameters();
        params.setParameterValue("targetScore", targetScore);
        return params;
    }

    /**
     * Parameters with the given target score and EuchreParameters.sittingOutDealerPicksUp (set through
     * setParameterValue).
     */
    static EuchreParameters params(int targetScore, boolean sittingOutDealerPicksUp) {
        EuchreParameters params = params(targetScore);
        params.setParameterValue("sittingOutDealerPicksUp", sittingOutDealerPicksUp);
        return params;
    }

    static List<Deck<FrenchCard>> allDecks(EuchreGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.kitty);
        decks.add(state.currentTrick);
        decks.add(state.discardPile);
        return decks;
    }

    /**
     * Replace the whole deal (at the start of bidding): hands[p] becomes player p's hand; the up-card and every card in
     * no hand go to the kitty, with the up-card on top as state.upCard.
     */
    @SafeVarargs
    static void arrangeDeal(EuchreGameState state, String upCode, List<FrenchCard>... hands) {
        assertNull("arrangeDeal is for the start of bidding", state.trumpSuit);
        List<FrenchCard> pool = new ArrayList<>();
        for (Deck<FrenchCard> deck : allDecks(state)) {
            pool.addAll(deck.getComponents());
            deck.clear();
        }
        for (int p = 0; p < hands.length; p++) {
            for (FrenchCard c : hands[p]) {
                assertTrue(c + " dealt twice", pool.remove(c));
                state.playerHands.get(p).addToBottom(c);
            }
        }
        FrenchCard up = card(upCode);
        assertTrue(up + " dealt twice", pool.remove(up));
        for (FrenchCard c : pool)
            state.kitty.addToBottom(c);
        state.kitty.add(up);   // the top
        state.upCard = up;
    }

    /**
     * The deal most tests use. Up-card 9H; kitty 9H (top), JH, JD, 9C - so the red Jacks, the bowers when Hearts or
     * Diamonds are trumps, never reach a hand.
     * <pre>
     * P0: 9S 10S JS AH 9D      (the only Diamond: 9D)
     * P1: QS KS AS 10H 10D
     * P2: QD AD QH QC 10C      (void in Spades)
     * P3: KD KH AC KC JC       (dealer; void in Spades)
     * </pre>
     */
    static void standardDeal(EuchreGameState state) {
        arrangeDeal(state, "9H",
                cards("9S", "10S", "JS", "AH", "9D"),
                cards("QS", "KS", "AS", "10H", "10D"),
                cards("QD", "AD", "QH", "QC", "10C"),
                cards("KD", "KH", "AC", "KC", "JC"));
    }

    /**
     * The deal of the bowers tests, for Hearts as trumps called in round 2: the red Jacks are in hands.
     * Up-card 9C; kitty 9C (top), 10C, QC, KC.
     * <pre>
     * P0: JD AS KS 9D 10D      (the left bower is the only trump)
     * P1: JH QH AD 9S 10S      (the right bower)
     * P2: AH KH KD QD JS       (JS is an ordinary spade, the only one)
     * P3: 10H 9H QS AC JC      (dealer; no diamonds)
     * </pre>
     */
    static void bowerDeal(EuchreGameState state) {
        arrangeDeal(state, "9C",
                cards("JD", "AS", "KS", "9D", "10D"),
                cards("JH", "QH", "AD", "9S", "10S"),
                cards("AH", "KH", "KD", "QD", "JS"),
                cards("10H", "9H", "QS", "AC", "JC"));
    }

    /**
     * Arrange the position at the start of play without driving the bidding: trumps and the maker set as if the
     * maker had called after the others in turn from the dealer's left passed. With discardCode set, the call was in
     * round 1 (trumps must be the up-card's suit): the dealer takes the up-card from the kitty and discards that
     * card to it. With discardCode null, the call was in round 2: no pickup. The player on the dealer's left is to
     * lead, to a trick with the state's card order.
     */
    static void startPlay(EuchreGameState state, FrenchCard.Suite trumps, int maker, String discardCode) {
        int dealer = state.getDealer();
        int offset = (maker - dealer - 1 + 8) % 4;   // how many players passed before the maker's call
        state.trumpSuit = trumps;
        state.maker = maker;
        state.alone = false;
        if (discardCode != null) {
            assertEquals("a round-1 call is for the up-card's suit", state.upCard.suite, trumps);
            state.kitty.remove(state.upCard);
            state.playerHands.get(dealer).add(state.upCard);
            FrenchCard discard = card(discardCode);
            state.playerHands.get(dealer).remove(discard);
            state.kitty.add(discard);
            state.dealerDiscard = discard;
            state.passes = offset;
        } else {
            assertNotEquals("a round-2 call is not for the up-card's suit", state.upCard.suite, trumps);
            state.dealerDiscard = null;
            state.passes = 4 + offset;
        }
        int leader = (dealer + 1) % 4;
        state.currentTrick = new Trick("CurrentTrick", 4, leader, state.getCardOrder());
        state.setTurnOwner(leader);
    }

    /**
     * As startPlay, but the maker goes alone. Their partner sits out, and the player on the maker's left is to
     * lead, to a trick the partner takes no part in. With discardCode set and the dealer the one sitting out,
     * this is the default EuchreParameters.sittingOutDealerPicksUp (the dealer took the up-card and discarded).
     */
    static void startPlayAlone(EuchreGameState state, FrenchCard.Suite trumps, int maker, String discardCode) {
        startPlay(state, trumps, maker, discardCode);
        state.alone = true;
        int leader = (maker + 1) % 4;
        state.currentTrick = new Trick("CurrentTrick", 4, leader, state.getCardOrder(), state.getSittingOut());
        state.setTurnOwner(leader);
    }

    /**
     * Take the card from a hand, the current trick or the discard pile (never the kitty, whose size would change).
     */
    private static void takeFromPlay(EuchreGameState state, FrenchCard card) {
        List<Deck<FrenchCard>> decks = new ArrayList<>(state.playerHands);
        decks.add(state.currentTrick);
        decks.add(state.discardPile);
        for (Deck<FrenchCard> deck : decks) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in a hand, the trick or the discard pile");
    }

    /**
     * Replace the player's hand with exactly these cards (taken from hands, trick or discard pile). The old hand goes
     * to the bottom of the discard pile.
     */
    static void giveHand(EuchreGameState state, int player, String... codes) {
        Deck<FrenchCard> hand = state.playerHands.get(player);
        for (FrenchCard c : new ArrayList<>(hand.getComponents())) {
            hand.remove(c);
            state.discardPile.addToBottom(c);
        }
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromPlay(state, c);
            hand.addToBottom(c);
        }
    }

    /**
     * Replace the current trick (whose cards go to the discard pile) by one led by the given player, holding these
     * cards in play order and the state's card order, and make the next player to play to it the current player.
     * Does not use Trick.play.
     */
    static void arrangeTrick(EuchreGameState state, int leader, String... codes) {
        assertTrue("arrangeTrick places cards only when nobody sits out",
                codes.length == 0 || state.getSittingOut() < 0);
        state.discardPile.add(state.currentTrick);
        state.currentTrick.clear();
        state.currentTrick = new Trick("CurrentTrick", 4, leader, state.getCardOrder(), state.getSittingOut());
        for (String code : codes) {
            FrenchCard c = card(code);
            takeFromPlay(state, c);
            state.currentTrick.addToBottom(c);
        }
        state.setTurnOwner((leader + codes.length) % 4);
    }

    /**
     * Arrange the 5th trick of the deal (after startPlay): every other card out of the hands onto the discard pile,
     * player p holding just lastCards[p], the tricks already taken as given (they should sum to 4), an empty trick led
     * by the leader, who is to play. The kitty is untouched.
     */
    static void arrangeLastTrick(EuchreGameState state, int leader, int[] tricksTaken, String... lastCards) {
        assertEquals("tricks before the last", 4, tricksTaken[0] + tricksTaken[1] + tricksTaken[2] + tricksTaken[3]);
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
     * Arrange and play the 5th trick so that the given player wins it whatever the trumps (unless Clubs are trumps):
     * the winner leads the Ace of Clubs and the others follow with the K, Q and 10 of Clubs in turn. None of these is
     * in the standard deal's kitty, and none is a Jack, so the bowers do not change the result.
     */
    static void playLastTrick(EuchreGameState state, EuchreForwardModel fm, int[] tricksBefore, int winner) {
        assertTrue("the game is over before this deal's last trick", state.isNotTerminal());
        assertNotEquals(FrenchCard.Suite.Clubs, state.trumpSuit);
        String[] codes = new String[4];
        String[] followers = {"KC", "QC", "10C"};
        codes[winner] = "AC";
        for (int i = 1; i < 4; i++)
            codes[(winner + i) % 4] = followers[i - 1];
        arrangeLastTrick(state, winner, tricksBefore, codes);
        for (int i = 0; i < 4; i++)
            playCards(state, fm, codes[(winner + i) % 4]);
    }

    /**
     * As playLastTrick, when the maker is alone (after startPlayAlone): the winner leads the Ace of Clubs and the
     * other two players in the play follow with the K and Q of Clubs in turn, skipping the partner sitting out, who
     * keeps their 5 cards (they must hold none of AC, KC, QC - with the standard deal, players 0 and 1 hold none).
     * tricksBefore sums to 4 with 0 for the partner sitting out.
     */
    static void playLastTrickAlone(EuchreGameState state, EuchreForwardModel fm, int[] tricksBefore, int winner) {
        int out = state.getSittingOut();
        assertTrue("nobody sits out", out >= 0);
        assertNotEquals("the partner sitting out wins no trick", out, winner);
        assertEquals("the partner sitting out wins no trick", 0, tricksBefore[out]);
        assertEquals("tricks before the last", 4, Arrays.stream(tricksBefore).sum());
        assertNotEquals(FrenchCard.Suite.Clubs, state.trumpSuit);
        List<Integer> order = new ArrayList<>();   // the order of play, from the winner, skipping the partner
        for (int i = 0; i < 4; i++)
            if ((winner + i) % 4 != out) order.add((winner + i) % 4);
        String[] codes = {"AC", "KC", "QC"};
        for (int p = 0; p < 4; p++) {
            if (p == out) continue;
            Deck<FrenchCard> hand = state.playerHands.get(p);
            state.discardPile.add(hand);
            hand.clear();
        }
        for (int i = 0; i < 3; i++)
            giveHand(state, order.get(i), codes[i]);
        assertEquals("the partner sitting out keeps 5 cards", 5, state.getPlayerHand(out).getSize());
        arrangeTrick(state, winner);
        state.tricksTaken = tricksBefore.clone();
        playCards(state, fm, codes);
    }

    static void playCards(EuchreGameState state, EuchreForwardModel fm, String... codes) {
        for (String code : codes) {
            int player = state.getCurrentPlayer();
            assertTrue("player " + player + " is to play but does not hold " + code,
                    state.getPlayerHand(player).contains(card(code)));
            fm.next(state, new PlayCard(card(code)));
        }
    }

    static void pass(EuchreGameState state, EuchreForwardModel fm, int times) {
        for (int i = 0; i < times; i++)
            fm.next(state, new Pass());
    }

    static Set<AbstractAction> available(EuchreGameState state, EuchreForwardModel fm) {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        Set<AbstractAction> set = new HashSet<>(actions);
        assertEquals("duplicate actions in " + actions, actions.size(), set.size());
        return set;
    }

    static Set<AbstractAction> plays(String... codes) {
        Set<AbstractAction> s = new HashSet<>();
        for (String c : codes)
            s.add(new PlayCard(card(c)));
        return s;
    }

    static Set<AbstractAction> discards(String... codes) {
        Set<AbstractAction> s = new HashSet<>();
        for (String c : codes)
            s.add(new Discard(card(c)));
        return s;
    }

    /**
     * The calls of these suits as trumps, each both with and without going alone.
     */
    static Set<AbstractAction> calls(FrenchCard.Suite... suits) {
        Set<AbstractAction> s = new HashSet<>();
        for (FrenchCard.Suite suit : suits) {
            s.add(new CallTrump(suit, false));
            s.add(new CallTrump(suit, true));
        }
        return s;
    }

    static Set<AbstractAction> passAndCalls(FrenchCard.Suite... suits) {
        Set<AbstractAction> s = calls(suits);
        s.add(new Pass());
        return s;
    }

    static void assertAllCardsPresent(EuchreGameState state) {
        List<FrenchCard> cards = new ArrayList<>();
        allDecks(state).forEach(d -> cards.addAll(d.getComponents()));
        assertEquals("number of cards", 24, cards.size());
        assertEquals(new HashSet<>(ALL_CARDS), new HashSet<>(cards));
    }
}

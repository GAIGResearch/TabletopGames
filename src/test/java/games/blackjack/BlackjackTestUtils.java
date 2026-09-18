package games.blackjack;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.FrenchCard;
import games.GameType;
import games.blackjack.BlackjackGameState.BlackjackGamePhase;
import games.blackjack.actions.Bet;
import games.blackjack.actions.Insurance;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
import static core.components.FrenchCard.FrenchCardType.*;
import static games.blackjack.BlackjackGameState.BlackjackGamePhase.Play;
import static games.blackjack.BlackjackGameState.handValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Helpers to arrange Blackjack states. Every helper moves cards from wherever they currently are (draw deck, dealer's
 * cards, any player hand), so the 52 cards are always conserved.
 * <p>
 * Arrangements use dealer up cards 2-9 and avoid winning 21s unless a test is about them: an Ace or ten-value up
 * card brings in the peek and insurance, and a winning 21 is paid differently.
 */
final class BlackjackTestUtils {

    static final List<FrenchCard> FULL_DECK = FrenchCard.generateDeck("FullDeck", VISIBLE_TO_ALL).getComponents();

    private BlackjackTestUtils() {
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
    static Game newGame(int nPlayers, long seed, BlackjackParameters params) {
        Game game = GameType.Blackjack.createGameInstance(nPlayers, seed, params);
        List<AbstractPlayer> players = new ArrayList<>();
        for (int p = 0; p < nPlayers; p++)
            players.add(new RandomPlayer(new Random(seed + p)));
        game.reset(players);
        return game;
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

    /**
     * Cards from space-separated codes, e.g. cards("AS 6H").
     */
    static List<FrenchCard> cards(String codes) {
        List<FrenchCard> result = new ArrayList<>();
        for (String code : codes.trim().split("\\s+"))
            result.add(card(code));
        return result;
    }

    static List<Deck<FrenchCard>> allDecks(BlackjackGameState state) {
        List<Deck<FrenchCard>> decks = new ArrayList<>();
        decks.add(state.drawDeck);
        decks.add(state.dealerHand);
        decks.add(state.holeCard);
        for (List<Deck<FrenchCard>> hands : state.playerHands)
            decks.addAll(hands);
        return decks;
    }

    private static void takeFromWherever(BlackjackGameState state, FrenchCard card) {
        for (Deck<FrenchCard> deck : allDecks(state)) {
            if (deck.contains(card)) {
                deck.remove(card);
                return;
            }
        }
        throw new IllegalArgumentException(card + " is not in any deck");
    }

    private static void emptyToDrawDeckBottom(BlackjackGameState state, Deck<FrenchCard> deck) {
        for (FrenchCard c : new ArrayList<>(deck.getComponents())) {
            deck.remove(c);
            state.drawDeck.addToBottom(c);
        }
    }

    /**
     * Move these cards to the top of the draw deck, the first card on top (drawn first).
     */
    static void stackDrawDeck(BlackjackGameState state, String codes) {
        List<FrenchCard> cs = cards(codes);
        for (int i = cs.size() - 1; i >= 0; i--) {
            takeFromWherever(state, cs.get(i));
            state.drawDeck.add(cs.get(i));
        }
    }

    /**
     * Stack the draw deck for the deal, before the last bet: playerCards[i] is the two cards ("10H 6C") for the i-th
     * player dealt in, in seat order - every player unless some sit out for want of chips, when only the
     * players who bet are listed. The deal order is one card to each of them in turn, the dealer's up card, a second
     * card to each, then the hole card; extraCards (may be empty) follow in the draw deck, for hits and the dealer's draws.
     */
    static void stackDeal(BlackjackGameState state, String[] playerCards, String up, String hole, String extraCards) {
        StringBuilder order = new StringBuilder();
        for (String pc : playerCards)
            order.append(pc.trim().split("\\s+")[0]).append(' ');
        order.append(up).append(' ');
        for (String pc : playerCards)
            order.append(pc.trim().split("\\s+")[1]).append(' ');
        order.append(hole).append(' ');
        order.append(extraCards);
        stackDrawDeck(state, order.toString());
    }

    /**
     * From the Betting phase with no bets yet: stack the deal (see stackDeal) and play Bet(bets[i]) for the i-th
     * player to bet (every player, or only those with the chips to bet) through fm.next, so the deal and everything that follows it straight away (the peek, naturals paid at once)
     * happens as in a real game. Under an Ace or ten-value up card this stops in the Insurance phase if
     * anyone can afford insurance: follow it with declineInsurance or insure.
     */
    static void betAndDeal(BlackjackGameState state, AbstractForwardModel fm, int[] bets, String[] playerCards,
                           String up, String hole, String extraCards) {
        stackDeal(state, playerCards, up, hole, extraCards);
        for (int bet : bets)
            fm.next(state, new Bet(bet));
    }

    /**
     * Every player offered insurance declines it, through fm.next, so the peek and what follows it happen. Does
     * nothing if the game is not in the Insurance phase. For tests of other rules, where declining changes nothing.
     */
    static void declineInsurance(BlackjackGameState state, AbstractForwardModel fm) {
        for (int i = 0; i < state.getNPlayers() && state.getGamePhase() == BlackjackGamePhase.Insurance; i++)
            fm.next(state, new Insurance(false));
    }

    /**
     * The insurance decisions in the order they are offered, through fm.next: Insurance(buys[i]) for the i-th player
     * offered it. Asserts that each decision is made in the Insurance phase.
     */
    static void insure(BlackjackGameState state, AbstractForwardModel fm, boolean... buys) {
        for (boolean buy : buys) {
            assertEquals("an insurance decision outside the Insurance phase", BlackjackGamePhase.Insurance, state.getGamePhase());
            fm.next(state, new Insurance(buy));
        }
    }

    /**
     * Pagat's natural payout: only a natural is paid payout21 = 1.5, at once, and that player does not play.
     */
    static void pagatNaturals(BlackjackParameters params) {
        params.setParameterValue("payout21NaturalOnly", true);
        params.setParameterValue("payout21", 1.5);
    }

    /**
     * Replace the cards of the player's first hand with exactly these cards. The old cards go to the bottom of the draw deck.
     */
    static void giveHand(BlackjackGameState state, int player, String codes) {
        Deck<FrenchCard> hand = state.playerHands.get(player).get(0);
        emptyToDrawDeckBottom(state, hand);
        for (FrenchCard c : cards(codes)) {
            takeFromWherever(state, c);
            hand.add(c);
        }
    }

    /**
     * Give the dealer exactly this up card and hole card. Any old dealer cards go to the bottom of the draw deck.
     */
    static void setDealer(BlackjackGameState state, String up, String hole) {
        emptyToDrawDeckBottom(state, state.dealerHand);
        emptyToDrawDeckBottom(state, state.holeCard);
        FrenchCard upCard = card(up), holeCard = card(hole);
        takeFromWherever(state, upCard);
        state.dealerHand.add(upCard);
        takeFromWherever(state, holeCard);
        state.holeCard.add(holeCard);
    }

    /**
     * From a freshly set-up state: arrange the position straight after the deal, without playing the Betting phase.
     * bets[p] chips are moved from player p's chips to their bet; hands[p] ("10H 6C") are their cards; the dealer
     * has the up card and hole card; drawDeckTop (may be "") is stacked on top of the draw deck. Play phase, player 0 to
     * act on their first hand.
     */
    static void arrangePlay(BlackjackGameState state, int[] bets, String[] hands, String up, String hole, String drawDeckTop) {
        for (int p = 0; p < state.getNPlayers(); p++) {
            state.chips[p] -= bets[p];
            state.bets.get(p).set(0, bets[p]);
            giveHand(state, p, hands[p]);
        }
        setDealer(state, up, hole);
        if (!drawDeckTop.isBlank())
            stackDrawDeck(state, drawDeckTop);
        state.setGamePhase(Play);
        state.setTurnOwner(0);
        state.activeHand = 0;
    }

    /**
     * The cards in a deck, as a set (the order within a hand does not matter to the rules).
     */
    static HashSet<FrenchCard> setOf(Deck<FrenchCard> deck) {
        return new HashSet<>(deck.getComponents());
    }

    static HashSet<FrenchCard> setOf(String codes) {
        return new HashSet<>(cards(codes));
    }

    /**
     * Two cards making 21, written out from the rules (not the production isNatural).
     */
    static boolean isNaturalHand(List<FrenchCard> cards) {
        return cards.size() == 2 && handValue(cards) == 21;
    }

    /**
     * The chips a player ends with, from the rules, ignoring insurance: bet b from startingChips 10, the final hand,
     * the dealer's final cards, and the payout mode.
     */
    static int expectedChips(int b, List<FrenchCard> hand, List<FrenchCard> dealer, boolean naturalOnly, double payout21) {
        int t = handValue(hand), d = handValue(dealer);
        boolean natural = isNaturalHand(hand);
        if (isNaturalHand(dealer))                  // only a dealer Blackjack (found by the peek) has 2 cards making 21
            return natural ? 10 : 10 - b;
        if (naturalOnly && natural)
            return 10 + (int) Math.floor(b * payout21);
        if (t > 21)
            return 10 - b;
        if (d > 21 || t > d)
            return 10 + (t == 21 && !naturalOnly ? (int) Math.floor(b * payout21) : b);
        return t == d ? 10 : 10 - b;
    }

    static void assertAllCardsPresent(BlackjackGameState state) {
        List<FrenchCard> all = new ArrayList<>();
        allDecks(state).forEach(d -> all.addAll(d.getComponents()));
        assertEquals("number of cards", 52, all.size());
        assertEquals(new HashSet<>(FULL_DECK), new HashSet<>(all));
    }

    /**
     * Conservation once extra packs may have been added: every one of the 52 cards appears the same
     * number of times, at least once, across all the decks. Returns that number: the packs in play.
     */
    static int assertCardsConserved(BlackjackGameState state) {
        Map<FrenchCard, Integer> counts = new HashMap<>();
        for (FrenchCard c : FULL_DECK)
            counts.put(c, 0);
        int total = 0;
        for (Deck<FrenchCard> d : allDecks(state)) {
            for (FrenchCard c : d.getComponents()) {
                assertTrue("not a card of the pack: " + c, counts.containsKey(c));
                counts.merge(c, 1, Integer::sum);
                total++;
            }
        }
        int packs = total / 52;
        assertTrue("fewer than 52 cards: " + total, packs >= 1);
        assertEquals("the number of cards is not a multiple of 52", 52 * packs, total);
        for (Map.Entry<FrenchCard, Integer> e : counts.entrySet())
            assertEquals("copies of " + e.getKey() + " with " + packs + " packs", packs, (int) e.getValue());
        return packs;
    }

    /**
     * Leave only the top n cards in the draw deck, moving the rest (from the bottom) to the given deck, e.g. the hand of
     * a player who has finished or sits out. The cards stay in play, so conservation holds.
     */
    static void leaveInDrawDeck(BlackjackGameState state, int n, Deck<FrenchCard> dump) {
        while (state.drawDeck.getSize() > n) {
            FrenchCard c = state.drawDeck.get(state.drawDeck.getSize() - 1);
            state.drawDeck.remove(state.drawDeck.getSize() - 1);
            dump.add(c);
        }
    }

    /**
     * Move these cards to the top of the draw deck, the first card on top (drawn first).
     */
    static void stackDrawDeck(BlackjackGameState state, List<FrenchCard> cs) {
        for (int i = cs.size() - 1; i >= 0; i--) {
            takeFromWherever(state, cs.get(i));
            state.drawDeck.add(cs.get(i));
        }
    }

    /**
     * The legal bets for a player with these chips, from the rules: every even amount from 2 to min(10, chips).
     */
    static Set<AbstractAction> betsUpTo(int chips) {
        Set<AbstractAction> bets = new HashSet<>();
        for (int amount = 2; amount <= Math.min(10, chips); amount += 2)
            bets.add(new Bet(amount));
        return bets;
    }

    /**
     * A new hand has just started: the game goes on in the Betting phase with firstBettor to bet, the round
     * counter equals the hands played, all 52 cards are back in the draw deck, and every hand, bet and insurance is empty.
     */
    static void assertNewHandStarted(BlackjackGameState state, int handsPlayed, int firstBettor) {
        assertNewHandStarted(state, handsPlayed, firstBettor, 1);
    }

    /**
     * As assertNewHandStarted(state, handsPlayed, firstBettor), with this many 52-card packs in play (a pack
     * is added when the draw deck runs out, and all of them are gathered into the draw deck). With 1 pack the checks are
     * exactly those of the three-argument version.
     */
    static void assertNewHandStarted(BlackjackGameState state, int handsPlayed, int firstBettor, int packs) {
        assertTrue("the game ended instead of starting hand " + (handsPlayed + 1), state.isNotTerminal());
        assertEquals(BlackjackGamePhase.Betting, state.getGamePhase());
        assertEquals("first to bet", firstBettor, state.getCurrentPlayer());
        assertEquals("round counter = hands played", handsPlayed, state.getRoundCounter());
        assertEquals("every card back in the draw deck", 52 * packs, state.getDrawDeck().getSize());
        if (packs == 1)
            assertAllCardsPresent(state);
        else
            assertEquals("packs in play", packs, assertCardsConserved(state));
        assertEquals(0, state.getDealerHand().getSize());
        assertEquals(0, state.getHoleCard().getSize());
        for (int p = 0; p < state.getNPlayers(); p++) {
            assertEquals("hands of player " + p, 1, state.getPlayerHands(p).size());
            assertEquals("cards of player " + p, 0, state.getPlayerHand(p, 0).getSize());
            assertEquals("bet of player " + p, 0, state.getBet(p, 0));
            assertEquals("insurance of player " + p, 0, state.getInsurance(p));
        }
        assertEquals(0, state.getActiveHand());
    }

    /**
     * True if every card of before appears in after in the same relative order (after may have other cards between
     * them). A shuffled 52-card draw deck keeps the order of the 40-odd cards left from the last hand with negligible
     * probability, whereas putting the used cards back anywhere without shuffling always keeps it.
     */
    static boolean keepsRelativeOrder(List<FrenchCard> before, List<FrenchCard> after) {
        int i = 0;
        for (FrenchCard c : after)
            if (i < before.size() && c.equals(before.get(i)))
                i++;
        return i == before.size();
    }
}

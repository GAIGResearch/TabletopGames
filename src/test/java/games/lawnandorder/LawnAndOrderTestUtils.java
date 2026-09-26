package games.lawnandorder;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.components.Deck;
import games.GameType;
import games.lawnandorder.LawnAndOrderGameState.Decision;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.Pass;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.LawnCard.Attribute;
import games.lawnandorder.components.LawnCard.Category;
import games.lawnandorder.components.RuleCard;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;

/**
 * The arrange helpers move cards between the state's decks and never create or delete one, so the 64 Lawn cards and
 * 16 Rule cards are always all present exactly once (assertAllCardsPresent). Arrange lawns before hands: setting a
 * lawn can take a card from a hand, but a hand helper does not touch lawns.
 */
class LawnAndOrderTestUtils {

    // ---------------------------------------------------------------- factories

    /** A seeded game with random players, created through the GameType factory. */
    static Game newGame(int nPlayers, long seed) {
        Game g = GameType.LawnAndOrder.createGameInstance(nPlayers, seed);
        g.reset(IntStream.range(0, nPlayers).mapToObj(p -> (AbstractPlayer) new RandomPlayer(new Random(seed + p))).toList());
        return g;
    }

    /** A state set up directly with default parameters and the given seed (deals exactly as newGame(n, seed)). */
    static LawnAndOrderGameState newState(int nPlayers, long seed) {
        LawnAndOrderParameters params = new LawnAndOrderParameters();
        params.setRandomSeed(seed);
        LawnAndOrderGameState state = new LawnAndOrderGameState(params, nPlayers);
        new LawnAndOrderForwardModel().setup(state);
        return state;
    }

    /** As newState(nPlayers, seed), with the named parameters set (setParameterValue) before setup. */
    static LawnAndOrderGameState newState(int nPlayers, long seed, Map<String, Object> parameterValues) {
        LawnAndOrderParameters params = new LawnAndOrderParameters();
        parameterValues.forEach(params::setParameterValue);
        params.setRandomSeed(seed);
        LawnAndOrderGameState state = new LawnAndOrderGameState(params, nPlayers);
        new LawnAndOrderForwardModel().setup(state);
        return state;
    }

    // ---------------------------------------------------------------- components

    /** A Lawn card, e.g. card(ORNAMENT, PINK, OVERSIZED). Equal by value to the one in the game. */
    static LawnCard card(Attribute type, Attribute colour, Attribute feature) {
        return new LawnCard(type, colour, feature);
    }

    /** The Standard Rule that condemns the attribute. */
    static RuleCard rule(Attribute condemned) {
        return RuleCard.standard(condemned);
    }

    static RuleCard special(RuleCard.Special special) {
        return RuleCard.special(special);
    }

    /** The 64 Lawn cards, built independently of the game's setup. */
    static List<LawnCard> allLawnCards() {
        List<LawnCard> cards = new ArrayList<>();
        for (Attribute t : Attribute.values())
            for (Attribute c : Attribute.values())
                for (Attribute f : Attribute.values())
                    if (t.category == Category.TYPE && c.category == Category.COLOUR && f.category == Category.FEATURE)
                        cards.add(card(t, c, f));
        return cards;
    }

    /** The 16 Rule cards, built independently of the game's setup. */
    static List<RuleCard> allRuleCards() {
        List<RuleCard> rules = new ArrayList<>();
        for (Attribute a : Attribute.values())
            rules.add(rule(a));
        rules.add(special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        rules.add(special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        rules.add(special(RuleCard.Special.EMERGENCY_SESSION));
        rules.add(special(RuleCard.Special.ZERO_TOLERANCE));
        return rules;
    }

    // ---------------------------------------------------------------- arranging Lawn cards

    /**
     * Take the card out of whichever deck holds it (draw deck, a hand, a chosen card, a lawn or the discard deck).
     * If it was in a hand, that hand is refilled with the top card of the draw deck, so hand sizes do not change.
     */
    private static void takeLawnCard(LawnAndOrderGameState state, LawnCard c) {
        if (state.drawDeck.contains(c)) {
            state.drawDeck.remove(c);
            return;
        }
        for (Deck<LawnCard> hand : state.hands)
            if (hand.contains(c)) {
                hand.remove(c);
                hand.add(state.drawDeck.draw());
                return;
            }
        List<Deck<LawnCard>> others = new ArrayList<>(state.chosenCards);
        others.addAll(state.lawns);
        others.add(state.discardDeck);
        for (Deck<LawnCard> d : others)
            if (d.contains(c)) {
                d.remove(c);
                return;
            }
        fail(c + " is not in any Lawn card deck");
    }

    /**
     * Make the player's lawn exactly these cards, the first named on top (index 0). Cards previously on the lawn go
     * to the bottom of the draw deck; the named cards are taken from wherever they are (see takeLawnCard).
     */
    static void setLawn(LawnAndOrderGameState state, int player, LawnCard... cards) {
        Deck<LawnCard> lawn = state.lawns.get(player);
        List<LawnCard> named = List.of(cards);
        for (LawnCard c : new ArrayList<>(lawn.getComponents())) {
            lawn.remove(c);
            if (!named.contains(c))
                state.drawDeck.addToBottom(c);
        }
        for (int i = cards.length - 1; i >= 0; i--) {
            // a named card that was already on this lawn is now in no deck; any other is taken from its deck
            if (!isLoose(state, cards[i]))
                takeLawnCard(state, cards[i]);
            lawn.add(cards[i]);
        }
    }

    /** True if the card is in no deck at all (it was just lifted off the deck being rebuilt). */
    private static boolean isLoose(LawnAndOrderGameState state, LawnCard c) {
        if (state.drawDeck.contains(c) || state.discardDeck.contains(c)) return false;
        for (int p = 0; p < state.getNPlayers(); p++)
            if (state.hands.get(p).contains(c) || state.chosenCards.get(p).contains(c) || state.lawns.get(p).contains(c))
                return false;
        return true;
    }

    /**
     * Put the named cards into the player's hand, keeping its size: for each named card not already there, the
     * lowest hand card that is not named goes to the bottom of the draw deck and the named card is taken from
     * wherever it is (see takeLawnCard). The rest of the hand is unchanged.
     */
    static void putInHand(LawnAndOrderGameState state, int player, LawnCard... cards) {
        Deck<LawnCard> hand = state.hands.get(player);
        List<LawnCard> named = List.of(cards);
        for (LawnCard c : cards) {
            if (hand.contains(c)) continue;
            LawnCard displaced = null;
            for (int i = hand.getSize() - 1; i >= 0; i--)
                if (!named.contains(hand.get(i))) {
                    displaced = hand.get(i);
                    break;
                }
            assertNotNull("no room in hand " + player + " for " + c, displaced);
            hand.remove(displaced);
            takeLawnCard(state, c);
            hand.add(c);
            state.drawDeck.addToBottom(displaced);
        }
    }

    /**
     * Set aside all but the top `keep` cards of the draw deck: the others go, from the bottom up, to the discard deck,
     * seen by nobody (the discard deck is emptied at the end of the round, so the cards come back then).
     */
    static void setAsideDrawDeck(LawnAndOrderGameState state, int keep) {
        while (state.drawDeck.getSize() > keep)
            state.discardDeck.add(state.drawDeck.pick(state.drawDeck.getSize() - 1), new boolean[state.getNPlayers()]);
    }

    /**
     * Set aside every card in the player's hand except the named ones (which must be in it): they go to the discard
     * deck, seen by nobody. Shrinks the hand.
     */
    static void keepOnlyInHand(LawnAndOrderGameState state, int player, LawnCard... keep) {
        Deck<LawnCard> hand = state.hands.get(player);
        List<LawnCard> named = List.of(keep);
        for (LawnCard c : named)
            assertTrue(c + " is not in hand " + player, hand.contains(c));
        for (LawnCard c : new ArrayList<>(hand.getComponents()))
            if (!named.contains(c)) {
                hand.remove(c);
                state.discardDeck.add(c, new boolean[state.getNPlayers()]);
            }
    }

    // ---------------------------------------------------------------- arranging Rule cards

    /**
     * Take the Rule card out of the agenda, the Insider Tips or the revealed rules. A card taken from the Insider
     * Tips is replaced there, at the same position and with the same visibility, by the bottom card of the agenda.
     */
    private static void takeRule(LawnAndOrderGameState state, RuleCard r) {
        if (state.agenda.contains(r)) {
            state.agenda.remove(r);
            return;
        }
        for (int i = 0; i < state.insiderTips.getSize(); i++)
            if (state.insiderTips.get(i).equals(r)) {
                state.insiderTips.setComponent(i, state.agenda.pickLast());
                return;
            }
        if (state.revealedRules.contains(r)) {
            state.revealedRules.remove(r);
            return;
        }
        fail(r + " is not in the agenda, the Insider Tips or the revealed rules");
    }

    /** Put the named Rule cards on top of the agenda, the first named on top (the next to be revealed). */
    static void setAgendaTop(LawnAndOrderGameState state, RuleCard... rules) {
        for (int i = rules.length - 1; i >= 0; i--) {
            takeRule(state, rules[i]);
            state.agenda.add(rules[i]);
        }
    }

    /** Reveal the named Rule cards as if in earlier turns of the round (they are in force; the last named on top). */
    static void revealEarlier(LawnAndOrderGameState state, RuleCard... rules) {
        for (RuleCard r : rules) {
            takeRule(state, r);
            state.revealedRules.add(r);
        }
    }

    /**
     * Rebuild the agenda (first named on top), the Insider Tips (in order, each keeping the visibility of the tip at
     * its position) and the revealed rules (last named on top) from literal lists, which between them must hold
     * all 16 Rule cards. The number of tips must not change.
     */
    static void setRules(LawnAndOrderGameState state, List<RuleCard> agenda, List<RuleCard> tips, List<RuleCard> revealed) {
        assertEquals("number of Insider Tips", state.insiderTips.getSize(), tips.size());
        List<RuleCard> all = new ArrayList<>(agenda);
        all.addAll(tips);
        all.addAll(revealed);
        assertEquals("the lists must hold the 16 Rule cards", multiset(allRuleCards()), multiset(all));
        List<boolean[]> visibility = new ArrayList<>();
        for (int i = 0; i < tips.size(); i++)
            visibility.add(state.insiderTips.getVisibilityOfComponent(i).clone());
        state.agenda.clear();
        state.insiderTips.clear();
        state.revealedRules.clear();
        for (int i = agenda.size() - 1; i >= 0; i--)
            state.agenda.add(agenda.get(i));
        for (int i = tips.size() - 1; i >= 0; i--)
            state.insiderTips.add(tips.get(i), visibility.get(i));
        for (RuleCard r : revealed)
            state.revealedRules.add(r);
    }

    // ---------------------------------------------------------------- driving

    /**
     * For a two-player state: reveals "No Pink" and the earlier rules, puts agendaTop on top of the agenda, and gives
     * lawn 0 three cards with no Pink or Repurposed and citationsBefore citations. Returns {a Pink card in player 0's
     * hand, a card with no Pink or Repurposed in player 1's hand}.
     */
    static LawnCard[] arrangeFourthCardWithOneCitation(LawnAndOrderGameState state, int citationsBefore, RuleCard agendaTop, RuleCard... earlier) {
        revealEarlier(state, rule(PINK));
        revealEarlier(state, earlier);
        setAgendaTop(state, agendaTop);
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(STRUCTURE, RED, ILLUMINATED), card(WATER_FEATURE, YELLOW, OVERSIZED));
        state.citations[0] = citationsBefore;
        LawnCard played = card(ORNAMENT, PINK, OVERSIZED);
        LawnCard other = card(ORNAMENT, BLUE, ILLUMINATED);
        putInHand(state, 0, played);
        putInHand(state, 1, other);
        return new LawnCard[]{played, other};
    }

    /**
     * Play one Play Object step: each player with a card given (index = player; null for a player who is not
     * choosing) plays it, in whatever order the forward model hands out the turn - always acting for the current
     * player. One fm.next per card given.
     */
    static void playTurn(AbstractForwardModel fm, LawnAndOrderGameState state, LawnCard... cardsByPlayer) {
        int n = (int) Arrays.stream(cardsByPlayer).filter(Objects::nonNull).count();
        for (int i = 0; i < n; i++) {
            int p = state.getCurrentPlayer();
            assertNotNull("player " + p + " has the turn but the test gave them no card", cardsByPlayer[p]);
            fm.next(state, new PlayObject(p, cardsByPlayer[p]));
        }
    }

    /**
     * Play one Continue-or-Pass step: each player with a decision given (index = player; null for a player who is
     * not choosing) makes it, always acting for the current player. One fm.next per decision given.
     */
    static void decide(AbstractForwardModel fm, LawnAndOrderGameState state, Decision... byPlayer) {
        int n = (int) Arrays.stream(byPlayer).filter(Objects::nonNull).count();
        for (int i = 0; i < n; i++) {
            int p = state.getCurrentPlayer();
            assertNotNull("player " + p + " has the turn but the test gave them no decision", byPlayer[p]);
            assertNotEquals(Decision.NONE, byPlayer[p]);
            fm.next(state, byPlayer[p] == Decision.CONTINUE ? new Continue(p) : new Pass(p));
        }
    }

    /** Every active player passes, one fm.next each, always acting for the current player. */
    static void everyonePasses(AbstractForwardModel fm, LawnAndOrderGameState state) {
        int n = state.getPlayersStillToChoose().size();
        for (int i = 0; i < n; i++)
            fm.next(state, new Pass(state.getCurrentPlayer()));
    }

    /** The actions available to the player (the two-argument _computeAvailableActions), as a set. */
    static Set<AbstractAction> actionsFor(AbstractForwardModel fm, LawnAndOrderGameState state, int player) {
        return new HashSet<>(fm.computeAvailableActions(state, null, player));
    }

    /** The three track scores of a player, in Category order (Type, Colour, Feature). */
    static int[] tracks(LawnAndOrderGameState state, int player) {
        return new int[]{state.getTrackScore(player, Category.TYPE), state.getTrackScore(player, Category.COLOUR),
                state.getTrackScore(player, Category.FEATURE)};
    }

    // ---------------------------------------------------------------- checks

    static <T> Map<T, Integer> multiset(Collection<T> items) {
        Map<T, Integer> m = new HashMap<>();
        for (T t : items) m.merge(t, 1, Integer::sum);
        return m;
    }

    /**
     * Every Lawn card is in exactly one of the draw deck, hands, chosen cards, lawns and the discard deck, and every
     * Rule card in exactly one of the agenda, Insider Tips and revealed rules.
     */
    static void assertAllCardsPresent(LawnAndOrderGameState state) {
        List<LawnCard> lawnCards = new ArrayList<>(state.drawDeck.getComponents());
        for (int p = 0; p < state.getNPlayers(); p++) {
            lawnCards.addAll(state.hands.get(p).getComponents());
            lawnCards.addAll(state.chosenCards.get(p).getComponents());
            lawnCards.addAll(state.lawns.get(p).getComponents());
        }
        lawnCards.addAll(state.discardDeck.getComponents());
        assertEquals("Lawn cards", multiset(allLawnCards()), multiset(lawnCards));
        List<RuleCard> ruleCards = new ArrayList<>(state.agenda.getComponents());
        ruleCards.addAll(state.insiderTips.getComponents());
        ruleCards.addAll(state.revealedRules.getComponents());
        assertEquals("Rule cards", multiset(allRuleCards()), multiset(ruleCards));
    }
}

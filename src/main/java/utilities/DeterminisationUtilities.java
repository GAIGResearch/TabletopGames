package utilities;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class DeterminisationUtilities {

    /**
     * The constrained reshuffle is exponential in the number of decks being filled, so we refuse to
     * attempt it for an unreasonable number of them.
     */
    private static final int MAX_CONSTRAINED_DECKS = 10;

    /**
     * A single position within a deck that holds a card the perspective player cannot see, and which
     * is therefore a candidate for reshuffling.
     */
    private record Slot<C extends Component>(Deck<C> deck, int index) {
    }

    /**
     *  Reshuffles all cards across the list of decks that meet the lambda predicate, and are not visible to player.
     *
     *  This is done in situ - it takes account of hidden information in PartialObservableDecks, and the visibility
     *  mode of Decks
     *
     * @param player
     * @param decks
     * @param lambda
     * @param <C>
     */
    public static <C extends Component> void reshuffle(int player, List<Deck<C>> decks, Predicate<C> lambda, Random rnd) {
        if (player < 0) return;
        unconstrainedReshuffle(hiddenSlots(player, decks, lambda), rnd);
    }

    private static <C extends Component> void unconstrainedReshuffle(List<Slot<C>> slots, Random rnd) {
        Deck<C> allCards = new Deck<>("temp", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (Slot<C> slot : slots)
            allCards.add(slot.deck().get(slot.index()));
        allCards.shuffle(rnd);
        for (Slot<C> slot : slots)
            slot.deck().setComponent(slot.index(), allCards.draw());
    }

    /**
     * As {@link #reshuffle(int, List, Predicate, Random)}, but constrained so that a card is only ever placed
     * in a deck for which {@code permitted} holds. This supports the common situation in which we know
     * something about what another player <i>cannot</i> have; for example that they are void in a suit
     * because they failed to follow it in a previous trick.
     * <p>
     * The reshuffle honours all of the constraints simultaneously, or none of them. A card is picked at
     * random for each position from those that are still compatible with a valid completion, so no
     * back-tracking or retrying is needed.
     *
     * @param permitted given a deck and a card, is that card a valid holding for that deck? Decks with no
     *                  constraints (a draw pile, say) should simply return true for everything.
     * @return true if all the constraints were met. If they are mutually inconsistent (which means the
     *         information they are derived from is itself inconsistent) then the decks are reshuffled
     *         without any constraints, and false is returned.
     */
    public static <C extends Component> boolean reshuffle(int player, List<Deck<C>> decks, Predicate<C> lambda,
                                                          Random rnd, BiPredicate<Deck<C>, C> permitted) {
        if (player < 0) return true;
        if (permitted == null) {
            reshuffle(player, decks, lambda, rnd);
            return true;
        }
        // slots is the decks/indices that are to be shuffled (not perfectly known to perspective player)
        List<Slot<C>> slots = hiddenSlots(player, decks, lambda);
        if (slots.isEmpty()) return true;
        int nCards = slots.size();

        // The distinct decks that we have to fill. targetIndex provides the index to the list for any Deck (using reference equality)
        // slotTarget stores which underlying Deck each of the target slots is in; we can then assign cards to slots, and later put them into Decks
        List<Deck<C>> targets = new ArrayList<>();
        IdentityHashMap<Deck<C>, Integer> targetIndex = new IdentityHashMap<>();
        int[] slotTarget = new int[nCards]; //
        for (int i = 0; i < nCards; i++) {
            Deck<C> deck = slots.get(i).deck();
            slotTarget[i] = targetIndex.computeIfAbsent(deck, d -> {
                targets.add(d);
                return targets.size() - 1;
            });
        }
        int nTargets = targets.size();
        if (nTargets > MAX_CONSTRAINED_DECKS)
            throw new IllegalArgumentException("Constrained reshuffle is not supported for " + nTargets + " decks");
        int nSubsets = 1 << nTargets;

        int[] capacity = new int[nTargets];
        for (int t : slotTarget) capacity[t]++;

        // Each card is compatible with some subset of the target decks, held as a bitmask. Cards with the
        // same mask are interchangeable, so we count how many there are of each mask.
        List<C> cards = cardsIn(slots);  // cards to shuffle
        int[] cardMask = new int[nCards];
        int[] cardsWithMask = new int[nSubsets];
        int distinctMasks = 0;
        for (int i = 0; i < nCards; i++) {
            int mask = 0;
            C card = cards.get(i);
            // the mask for a card is the decks it is permitted to be in
            // the potential number of masks is 2^decks (nSubsets)
            for (int t = 0; t < nTargets; t++)
                if (permitted.test(targets.get(t), card)) mask |= 1 << t;
            cardMask[i] = mask;
            if (cardsWithMask[mask] == 0) distinctMasks++;
            cardsWithMask[mask]++;
        }

        if (distinctMasks == 1 && cardsWithMask[nSubsets - 1] == nCards) {
            // nothing is actually restricted, so there is no point in doing any of the work below
            unconstrainedReshuffle(slots, rnd);
            return true;
        }

        // A valid assignment exists when, for every subset D of the target decks, the number of
        // positions to fill in D is no greater than the number of cards that are allowed in any deck of D
        // (aka Hall's condition). We track this for every subset and only ever place a card where
        // doing so leaves it non-negative; this guarantees that the assignment can always be completed.
        // cardsWithMask[] hold the number of cards permitted for the mask (and each mask is one subset D)
        int[] headroom = new int[nSubsets];
        for (int subset = 1; subset < nSubsets; subset++) {
            int demand = 0, supply = 0;
            for (int t = 0; t < nTargets; t++)
                if ((subset >> t & 1) == 1) demand += capacity[t];
            for (int mask = 1; mask < nSubsets; mask++)
                if ((mask & subset) != 0) supply += cardsWithMask[mask];
            headroom[subset] = supply - demand;
            if (headroom[subset] < 0) {
                // the constraints contradict each other, so we fall back on an unconstrained reshuffle
                unconstrainedReshuffle(slots, rnd);
                return false;
            }
        }

        // Deal the cards out in a random order, each to a random deck that still leaves a valid completion
        int[] order = new int[nCards];
        for (int i = 0; i < nCards; i++) order[i] = i;
        shuffle(order, nCards, rnd);

        int[] cardTarget = new int[nCards];
        int[] candidates = new int[nTargets];
        for (int i = 0; i < nCards; i++) {
            int card = order[i];
            int mask = cardMask[card];
            int nCandidates = 0;
            for (int t = 0; t < nTargets; t++)
                if ((mask >> t & 1) == 1 && capacity[t] > 0) candidates[nCandidates++] = t;
            shuffle(candidates, nCandidates, rnd);

            int chosen = -1;
            for (int c = 0; c < nCandidates && chosen == -1; c++) {
                // placing this card in t reduces the headroom of every subset that excludes t, but could
                // otherwise have used the card
                int t = candidates[c];
                boolean valid = true;
                for (int subset = 1; subset < nSubsets && valid; subset++)
                    if ((subset >> t & 1) == 0 && (subset & mask) != 0 && headroom[subset] < 1) valid = false;
                if (valid) chosen = t;
            }
            if (chosen == -1)
                throw new AssertionError("No valid deck for card despite Hall's condition holding");

            for (int subset = 1; subset < nSubsets; subset++)
                if ((subset >> chosen & 1) == 0 && (subset & mask) != 0) headroom[subset]--;
            capacity[chosen]--;
            cardTarget[card] = chosen;
        }

        // Finally group the cards by the deck they have been assigned to, and fill that deck's positions
        int[] cursor = new int[nTargets];
        for (int t = 1; t < nTargets; t++) cursor[t] = cursor[t - 1] + countOf(cardTarget, t - 1);
        int[] cardsByTarget = new int[nCards];
        int[] next = cursor.clone();
        for (int card = 0; card < nCards; card++)
            cardsByTarget[next[cardTarget[card]]++] = card;

        for (int i = 0; i < nCards; i++) {
            int t = slotTarget[i];
            slots.get(i).deck().setComponent(slots.get(i).index(), cards.get(cardsByTarget[cursor[t]++]));
        }
        return true;
    }

    private static int countOf(int[] values, int target) {
        int retValue = 0;
        for (int value : values) if (value == target) retValue++;
        return retValue;
    }

    /**
     * The positions across all the decks that hold a card the specified player cannot see, and which also
     * meet the lambda predicate. This takes account of hidden information in PartialObservableDecks, and
     * the visibility mode of Decks.
     */
    private static <C extends Component> List<Slot<C>> hiddenSlots(int player, List<Deck<C>> decks, Predicate<C> lambda) {
        List<Slot<C>> retValue = new ArrayList<>();
        for (Deck<C> d : decks) {
            int length = d.getSize();
            if (d instanceof PartialObservableDeck<C> pod) {
                for (int i = 0; i < length; i++) {
                    if (!pod.getVisibilityForPlayer(i, player) && lambda.test(pod.get(i)))
                        retValue.add(new Slot<>(d, i));
                }
            } else {
                switch (d.getVisibilityMode()) {
                    case VISIBLE_TO_ALL:
                        // don't shuffle
                        break;
                    case VISIBLE_TO_OWNER:
                        if (d.getOwnerId() == player)
                            break;
                    case HIDDEN_TO_ALL:
                        for (int i = 0; i < length; i++)
                            if (lambda.test(d.get(i)))
                                retValue.add(new Slot<>(d, i));
                        break;
                    case TOP_VISIBLE_TO_ALL:
                        for (int i = 1; i < length; i++)
                            if (lambda.test(d.get(i)))
                                retValue.add(new Slot<>(d, i));
                        break;
                    case BOTTOM_VISIBLE_TO_ALL:
                        for (int i = 0; i < length - 1; i++)
                            if (lambda.test(d.get(i)))
                                retValue.add(new Slot<>(d, i));
                        break;
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
        return retValue;
    }

    private static <C extends Component> List<C> cardsIn(List<Slot<C>> slots) {
        List<C> retValue = new ArrayList<>(slots.size());
        for (Slot<C> slot : slots)
            retValue.add(slot.deck().get(slot.index()));
        return retValue;
    }

    /**
     * A Fisher-Yates shuffle of the first count entries of the array.
     */
    private static void shuffle(int[] values, int count, Random rnd) {
        for (int i = count - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            Utils.swap(values, i, j);
        }
    }
}

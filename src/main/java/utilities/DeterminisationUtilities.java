package utilities;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public class DeterminisationUtilities {

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
        // Gather up all unknown cards for reshuffling
        if (player < 0) return;

        Deck<C> allCards = new Deck<>("temp", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // The fully observable decks care now filtered to remove any that are visible to us
        for (Deck<C> d : decks) {
            int length = d.getSize();
            if (d instanceof PartialObservableDeck<C> pod) {
                for (int i = 0; i < length; i++) {
                    if (!pod.getVisibilityForPlayer(i, player) && lambda.test(pod.get(i)))
                        allCards.add(pod.get(i));
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
                                allCards.add(d.get(i));
                        break;
                    case TOP_VISIBLE_TO_ALL:
                        for (int i = 1; i < length; i++)
                            if (lambda.test(d.get(i)))
                                allCards.add(d.get(i));
                        break;
                    case BOTTOM_VISIBLE_TO_ALL:
                        for (int i = 0; i < length - 1;  i++)
                            if (lambda.test(d.get(i)))
                                allCards.add(d.get(i));
                        break;
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
        allCards.shuffle(rnd);

        // and put the shuffled cards in place
        for (Deck<C> d : decks) {
            int length = d.getSize();
            if (d instanceof PartialObservableDeck<C> pod) {
                for (int i = 0; i < length; i++) {
                    if (!pod.getVisibilityForPlayer(i, player) && lambda.test(pod.get(i)))
                        pod.setComponent(i, allCards.draw());
                }
            } else {
                switch (d.getVisibilityMode()) {
                    case VISIBLE_TO_ALL:
                        break;
                    case VISIBLE_TO_OWNER:
                        if (d.getOwnerId() == player)
                            break;
                    case HIDDEN_TO_ALL:
                        for (int i = 0; i < length; i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case TOP_VISIBLE_TO_ALL:
                        for (int i = 1; i < length; i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case BOTTOM_VISIBLE_TO_ALL:
                        for (int i = 0; i < length - 1;  i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
    }
}

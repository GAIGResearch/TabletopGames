package utilities;

import core.CoreConstants;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;

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

        Deck<C> allCards = new Deck<>("temp", -1, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);

        // The fully observable decks care now filtered to remove any that are visible to us
        for (Deck<C> d : decks) {
            if (d instanceof PartialObservableDeck) {
                PartialObservableDeck<C> pod = (PartialObservableDeck<C>) d;
                for (int i = 0; i < pod.getSize(); i++) {
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
                        allCards.add(d.stream().filter(lambda).collect(toList()));
                        break;
                    case FIRST_VISIBLE_TO_ALL:
                        Deck<C> temp = d.copy();
                        temp.draw();
                        allCards.add(temp.stream().filter(lambda).collect(toList()));
                        break;
                    case LAST_VISIBLE_TO_ALL:
                        throw new AssertionError("Not supported : LAST_VISIBLE_TO_ALL");
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
        allCards.shuffle(rnd);

        // and put the shuffled cards in place
        for (Deck<C> d : decks) {
            if (d instanceof PartialObservableDeck) {
                PartialObservableDeck<C> pod = (PartialObservableDeck<C>) d;
                for (int i = 0; i < pod.getSize(); i++) {
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
                        for (int i = 0; i < d.getSize(); i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case FIRST_VISIBLE_TO_ALL:
                        for (int i = 1; i < d.getSize(); i++)
                            if (lambda.test(d.get(i)))
                                d.setComponent(i, allCards.draw());
                        break;
                    case LAST_VISIBLE_TO_ALL:
                        throw new AssertionError("Not supported : LAST_VISIBLE_TO_ALL");
                    case MIXED_VISIBILITY:
                        throw new AssertionError("Not supported : MIXED_VISIBILITTY");
                }
            }
        }
    }
}

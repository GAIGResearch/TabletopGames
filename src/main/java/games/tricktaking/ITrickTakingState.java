package games.tricktaking;

import core.components.Component;
import core.components.Deck;

/**
 * What the shared trick-taking code (e.g. {@link PlayCard}) needs from a game state.
 *
 * @param <C> the type of card
 * @param <S> the type of suit
 */
public interface ITrickTakingState<C extends Component, S extends Enum<S>> {

    Deck<C> getPlayerHand(int player);

    Trick<C, S> getCurrentTrick();

    KnownVoids<S> getKnownVoids();

    /**
     * Records in the known voids what the player's card, about to be played to the current trick, reveals. By
     * default, a card that does not follow the suit led shows the player has none of that suit; a game where the
     * card reveals more (e.g. one where a player unable to follow must trump) overrides this.
     */
    default void recordVoids(int player, C card) {
        getKnownVoids().record(player, getCurrentTrick(), card);
    }
}

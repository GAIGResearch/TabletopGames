package games.tricktaking;

import core.components.Deck;
import core.components.FrenchCard;

/**
 * What the shared trick-taking code (e.g. {@link PlayCard}) needs from a game state.
 */
public interface ITrickTakingState {

    Deck<FrenchCard> getPlayerHand(int player);

    Trick getCurrentTrick();

    KnownVoids getKnownVoids();
}

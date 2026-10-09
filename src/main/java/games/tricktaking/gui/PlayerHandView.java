package games.tricktaking.gui;

import core.components.Deck;
import core.components.FrenchCard;

/**
 * A {@link CardHandView} of French cards, drawn from their images.
 */
public class PlayerHandView extends CardHandView<FrenchCard, FrenchCard.Suite> {

    public PlayerHandView(Deck<FrenchCard> hand, int playerId, int playerAreaWidth) {
        super(CardArt.FRENCH, hand, playerId, playerAreaWidth);
    }
}

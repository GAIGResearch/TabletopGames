package games.tricktaking.gui;

import core.components.FrenchCard;

/**
 * A {@link CardTrickView} of French cards, drawn from their images.
 */
public class TrickView extends CardTrickView<FrenchCard, FrenchCard.Suite> {

    public TrickView(int nPlayers) {
        this(nPlayers, 360);
    }

    /**
     * @param minWidth the least the panel may be - wide enough for the longest line of text the game shows
     */
    public TrickView(int nPlayers, int minWidth) {
        super(CardArt.FRENCH, nPlayers, minWidth);
    }
}

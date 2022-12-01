package games.sirius;

import core.CoreConstants;
import core.components.Deck;

import java.util.Objects;

public class PlayerArea {

    Deck<SiriusCard> deck;
    int player;

    public PlayerArea(int player) {
        this.player = player;
        deck = new Deck<>("Cards of Player " + player, player, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
    }

    @Override
    public int hashCode() {
        return player + Objects.hash(deck);
    }

    public PlayerArea copy() {
        PlayerArea retValue = new PlayerArea(player);
        retValue.deck = deck.copy();
        return retValue;
    }

}

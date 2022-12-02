package games.sirius;

import core.CoreConstants;
import core.components.Deck;

import java.util.Objects;

public class PlayerArea {

    Deck<SiriusCard> deck;
    Deck<SiriusCard> soldCards;
    int player;
    int medalTotal;

    public PlayerArea(int player) {
        this.player = player;
        deck = new Deck<>("Cards of Player " + player, player, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        soldCards = new Deck<>("Sold Cards of Player " + player, player, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
    }

    @Override
    public int hashCode() {
        return player + Objects.hash(deck, soldCards);
    }

    public PlayerArea copy() {
        PlayerArea retValue = new PlayerArea(player);
        retValue.deck = deck.copy();
        retValue.soldCards = soldCards.copy();
        return retValue;
    }

}

package games.sirius;

import core.CoreConstants;
import core.components.Deck;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerArea {

    Deck<SiriusCard> deck;
    Deck<SiriusCard> soldCards;
    int player;
    List<Medal> medals;

    public PlayerArea(int player) {
        this.player = player;
        deck = new Deck<>("Cards of Player " + player, player, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        soldCards = new Deck<>("Sold Cards of Player " + player, player, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        medals = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        return player + Objects.hash(deck, soldCards, medals);
    }

    public PlayerArea copy() {
        PlayerArea retValue = new PlayerArea(player);
        retValue.deck = deck.copy();
        retValue.soldCards = soldCards.copy();
        retValue.medals = new ArrayList<>(medals); // medals are immutable, so this is safe
        return retValue;
    }

    public List<Medal> getMedals() {
        return new ArrayList<>(medals);
    }

}

package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanCard;

import java.util.*;

public class PlayMonopoly extends AbstractAction {
    public final CatanParameters.Resource resource;
    public final int player;

    public PlayMonopoly(CatanParameters.Resource resource, int player){
        this.resource = resource;
        this.player = player;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;

        Deck<CatanCard> playerDevDeck = cgs.getPlayerDevCards(player);
        Optional<CatanCard> monopoly = playerDevDeck.stream()
                .filter(card -> card.cardType == CatanCard.CardType.MONOPOLY)
                .findFirst();
        if (monopoly.isPresent()) {
            CatanCard monopolyCard = monopoly.get();
            playerDevDeck.remove(monopolyCard);

            int nCollected = 0;
            for (int targetPlayerID = 0; targetPlayerID < gs.getNPlayers(); targetPlayerID++){
                if (targetPlayerID != gs.getCurrentPlayer()) {
                    Counter c = cgs.getPlayerResources(targetPlayerID).get(resource);
                    nCollected += c.getValue();
                    c.setValue(0);
                }
            }
            cgs.getPlayerResources(player).get(resource).increment(nCollected);
        } else {
            throw new AssertionError("Cannot use a Monopoly Card that is not in hand.");
        }

        return true;
    }

    @Override
    public PlayMonopoly copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayMonopoly)) return false;
        PlayMonopoly monopoly = (PlayMonopoly) o;
        return player == monopoly.player && resource == monopoly.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, player);
    }

    @Override
    public String toString() {
        return "p" + player + " plays Dev:Monopoly (" + resource.toString() + ")";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

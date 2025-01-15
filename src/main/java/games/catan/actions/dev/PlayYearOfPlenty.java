package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanCard;

import java.util.*;

public class PlayYearOfPlenty extends AbstractAction {
    public final CatanParameters.Resource[] resources;
    public final int player;
    public final boolean removeCard;

    public PlayYearOfPlenty(CatanParameters.Resource[] resources, int player, boolean removeCard) {
        this.resources = resources;
        this.player = player;
        this.removeCard = removeCard;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        Deck<CatanCard> playerDevDeck = cgs.getPlayerDevCards(player);
        Map<CatanParameters.Resource, Counter> playerResources = cgs.getPlayerResources(player);

        Optional<CatanCard> yearOfPlenty = playerDevDeck.stream()
                .filter(card -> card.cardType == CatanCard.CardType.YEAR_OF_PLENTY)
                .findFirst();
        if (!removeCard || yearOfPlenty.isPresent()) {
            if (removeCard) {
                CatanCard yearOfPlentyCard = yearOfPlenty.get();
                playerDevDeck.remove(yearOfPlentyCard);
            }
            for (CatanParameters.Resource r: resources) {
                if (cgs.getResourcePool().get(r).getValue() <= 0) {
                    throw new AssertionError("Cannot use a Year of Plenty Card for resources that are not in deck: " + Arrays.toString(resources));
                }
            }
            for (CatanParameters.Resource r: resources) {
                // swaps resources from common to player deck
                playerResources.get(r).increment();
                cgs.getResourcePool().get(r).decrement();
            }
        } else {
            throw new AssertionError("Cannot use a Year of Plenty Card that is not in hand.");
        }
        return true;
    }

    @Override
    public PlayYearOfPlenty copy() {
        return new PlayYearOfPlenty(resources.clone(), player, removeCard);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayYearOfPlenty)) return false;
        PlayYearOfPlenty that = (PlayYearOfPlenty) o;
        return player == that.player && removeCard == that.removeCard && Arrays.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(player, removeCard);
        result = 31 * result + Arrays.hashCode(resources);
        return result;
    }

    @Override
    public String toString() {
        return "p" + player + " plays Dev:YearOfPlenty (" + Arrays.toString(resources) + ")";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

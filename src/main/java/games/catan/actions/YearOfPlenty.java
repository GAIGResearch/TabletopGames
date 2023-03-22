package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanCard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

public class YearOfPlenty extends AbstractAction {
    public final CatanParameters.Resource[] resources;
    public final int player;

    public YearOfPlenty(CatanParameters.Resource[] resources, int player) {
        this.resources = resources;
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        Deck<CatanCard> playerDevDeck = cgs.getPlayerDevCards(player);
        HashMap<CatanParameters.Resource, Counter> playerResources = cgs.getPlayerResources(player);

        Optional<CatanCard> yearOfPlenty = playerDevDeck.stream()
                .filter(card -> card.cardType == CatanCard.CardType.YEAR_OF_PLENTY)
                .findFirst();
        if (yearOfPlenty.isPresent()) {
            CatanCard yearOfPlentyCard = yearOfPlenty.get();
            playerDevDeck.remove(yearOfPlentyCard);
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
    public YearOfPlenty copy() {return this;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof YearOfPlenty)) return false;
        YearOfPlenty that = (YearOfPlenty) o;
        return player == that.player && Arrays.equals(resources, that.resources);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(player);
        result = 31 * result + Arrays.hashCode(resources);
        return result;
    }

    @Override
    public String toString() {
        return player + " Year of Plenty for " + Arrays.toString(resources);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

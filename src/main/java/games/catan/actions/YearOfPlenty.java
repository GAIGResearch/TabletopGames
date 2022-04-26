package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.dominion.DominionConstants;
import games.dominion.cards.DominionCard;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.cardType;
import static games.catan.CatanConstants.resourceDeckHash;

public class YearOfPlenty extends AbstractAction {
    public final CatanParameters.Resources resource1;
    public final CatanParameters.Resources resource2;

    public YearOfPlenty(CatanParameters.Resources resource1, CatanParameters.Resources resource2) {
        this.resource1 = resource1;
        this.resource2 = resource2;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState) gs;
        List<Card> playerResourceDeck = ((Deck<Card>) cgs.getComponentActingPlayer(playerHandHash)).getComponents();
        List<Card> commonResourceDeck = ((Deck<Card>) cgs.getComponent(resourceDeckHash)).getComponents();
        Deck<Card> playerDevDeck = (Deck<Card>) cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>) cgs.getComponent(CatanConstants.developmentDiscardDeck);

        Optional<Card> yearOfPlenty = playerDevDeck.stream()
                .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(CatanParameters.CardTypes.YEAR_OF_PLENTY.toString()))
                .findFirst();
        if(yearOfPlenty.isPresent()){
            Card yearOfPlentyCard = yearOfPlenty.get();
            Optional<Card> firstResource = commonResourceDeck.stream()
                    .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(resource1.toString()))
                    .findFirst();
            Optional<Card> secondResource = commonResourceDeck.stream()
                    .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(resource2.toString()))
                    .findFirst();
            if(firstResource.isPresent() && secondResource.isPresent()){
                Card firstResourceCard = firstResource.get();
                Card secondResourceCard = secondResource.get();
                // removes dev card and adds it to discard dev deck
                playerDevDeck.remove(yearOfPlentyCard);
                developmentDiscardDeck.add(yearOfPlentyCard);
                // swaps resources from common to player deck
                playerResourceDeck.add(firstResourceCard);
                commonResourceDeck.remove(firstResourceCard);
                playerResourceDeck.add(secondResourceCard);
                commonResourceDeck.remove(secondResourceCard);
            } else {
                commonResourceDeck.stream().forEach(card -> System.out.println(card.getProperty(cardType)));
                throw new AssertionError("Cannot use a Year of Plenty Card for resources that are not in deck: " + resource1.toString() + " " + resource2.toString());
            }
        } else {
            throw new AssertionError("Cannot use a Year of Plenty Card that is not in hand.");
        }

        return true;
    }

    @Override
    public AbstractAction copy() {return this;}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof YearOfPlenty) {
            YearOfPlenty otherAction = (YearOfPlenty) other;
            return resource1.equals(otherAction.resource1) && resource2.equals(otherAction.resource2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource1, resource2);
    }

    @Override
    public String toString() {
        return "Year of Plenty with resource1 = " + resource1.toString() + " and resource 2 = " + resource2.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

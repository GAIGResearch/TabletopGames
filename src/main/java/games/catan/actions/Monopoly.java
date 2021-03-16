package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTurnOrder;

import java.util.List;

import static core.CoreConstants.playerHandHash;

public class Monopoly extends AbstractAction {
    CatanParameters.Resources resource;
    Card card;

    public Monopoly(CatanParameters.Resources resource, Card card){
        this.resource = resource;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        List<Card> playerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();

        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);

        for (int i = 0; i < gs.getNPlayers(); i++){
            if (i != gs.getCurrentPlayer()) {
                List<Card> otherPlayerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
                for (int j = 0; j < otherPlayerResourceDeck.size(); j++){
                    if (otherPlayerResourceDeck.get(j).getProperty(CatanConstants.cardType).toString().equals(resource)){
                        Card card = otherPlayerResourceDeck.remove(j);
                        playerResourceDeck.add(card);

                        playerDevDeck.remove(this.card);
                        developmentDiscardDeck.add(card);
                    }
                }
            }
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Monopoly(resource, card);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof Monopoly){
            Monopoly otherAction = (Monopoly)other;
            return resource.equals(otherAction.resource) && card.equals(otherAction.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Monopoly with resource = " + resource.toString() + " and card " + card;
    }
}

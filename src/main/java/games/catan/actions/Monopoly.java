package games.catan.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTurnOrder;

import java.util.*;

import static core.CoreConstants.playerHandHash;

public class Monopoly extends AbstractAction {
    public final CatanParameters.Resources resource;

    public Monopoly(CatanParameters.Resources resource){
        this.resource = resource;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        List<Card> playerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
        Deck<Card> playerDevDeck = (Deck<Card>)cgs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        Deck<Card> developmentDiscardDeck = (Deck<Card>)cgs.getComponent(CatanConstants.developmentDiscardDeck);

        Optional<Card> monopoly = playerDevDeck.stream()
                .filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(CatanParameters.CardTypes.MONOPOLY.toString()))
                .findFirst();
        if(monopoly.isPresent()){
            Card monopolyCard = monopoly.get();
            playerDevDeck.remove(monopolyCard);
            List<Card> targetResourceDeck = new ArrayList<>();

            for (int targetPlayerID = 0; targetPlayerID < gs.getNPlayers(); targetPlayerID++){
                if (targetPlayerID != gs.getCurrentPlayer()) {
                    targetResourceDeck = ((Deck<Card>)cgs.getComponent(CoreConstants.playerHandHash, targetPlayerID)).getComponents();
                    for (int j = 0; j < targetResourceDeck.size(); j++){
                        if (targetResourceDeck.get(j).getProperty(CatanConstants.cardType).toString().equals(resource.toString())){
                            Card card = targetResourceDeck.remove(j);
                            playerResourceDeck.add(card);
                            developmentDiscardDeck.add(card);
                        }
                    }
                }
            }
        } else {
            throw new AssertionError("Cannot use a Monopoly Card that is not in hand.");
        }

        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Monopoly){
            Monopoly otherAction = (Monopoly)other;
            return resource.equals(otherAction.resource);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource);
    }

    @Override
    public String toString() {
        return "Monopoly with resource = " + resource.toString();
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

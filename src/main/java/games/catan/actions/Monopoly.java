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

    public Monopoly(CatanParameters.Resources resource){
        this.resource = resource;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        List<Card> playerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
        for (int i = 0; i < gs.getNPlayers(); i++){
            if (i != gs.getCurrentPlayer()) {
                List<Card> playerHand = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
                for (int j = 0; j < playerHand.size(); j++){
                    if (playerHand.get(j).getProperty(CatanConstants.cardType).toString().equals(resource)){
                        Card card = playerHand.remove(j);
                        playerResourceDeck.add(card);
                    }
                }
            }
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Monopoly(resource);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof Monopoly){
            Monopoly otherAction = (Monopoly)other;
            return resource.equals(otherAction.resource);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Monopoly with resource = " + resource.toString();
    }
}

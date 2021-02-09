package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.List;

import static core.CoreConstants.playerHandHash;

public class YearOfPlenty extends AbstractAction {
    CatanParameters.Resources resource1;
    CatanParameters.Resources resource2;

    public YearOfPlenty(CatanParameters.Resources resource1, CatanParameters.Resources resource2){
        this.resource1 = resource1;
        this.resource2 = resource2;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        List<Card> playerResourceDeck = ((Deck<Card>)cgs.getComponentActingPlayer(playerHandHash)).getComponents();
        List<Card> commonResourceDeck = ((Deck<Card>)cgs.getComponent(playerHandHash)).getComponents();
        boolean resource1_done = false;
        boolean resource2_done = false;
        for (int i = 0; i < commonResourceDeck.size(); i++){
            Card card = commonResourceDeck.get(i);
            if( !resource1_done && card.getProperty(CatanConstants.cardType).toString().equals(resource1)){
                playerResourceDeck.add(card);
                commonResourceDeck.remove(i);
                resource1_done = true;
            }
            else if( !resource2_done && card.getProperty(CatanConstants.cardType).toString().equals(resource2)){
                playerResourceDeck.add(card);
                commonResourceDeck.remove(i);
                resource2_done = true;
            }
            if (resource1_done && resource2_done){
                return true;
            }
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        return new YearOfPlenty(resource1, resource2);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof YearOfPlenty){
            YearOfPlenty otherAction = (YearOfPlenty)other;
            return resource1.equals(otherAction.resource1) && resource2.equals(otherAction.resource2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Year of Plenty with resource1 = " + resource1.toString() + " and resource 2 = " + resource2.toString();
    }
}

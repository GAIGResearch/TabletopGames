package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class AddCardToSupporters extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public AddCardToSupporters(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID)!= RootParameters.Factions.WoodlandAlliance){
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            for (int i = 0; i < hand.getSize(); i++){
                if (hand.get(i).equals(card)){
                    boolean[] visibility = new boolean[currentState.getNPlayers()];
                    visibility[playerID] = true;
                    visibility[currentState.getFactionPlayerID(RootParameters.Factions.WoodlandAlliance)] = true;
                    currentState.getSupporters().add(hand.get(i), visibility);
                    hand.remove(i);
                    return true;
                }
            }

        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new AddCardToSupporters(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof AddCardToSupporters){
            AddCardToSupporters other = (AddCardToSupporters) obj;
            return playerID == other.playerID && card.cardtype == other.card.cardtype && card.suit == other.card.suit;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("AddCardToSupporters", playerID, card.cardtype, card.suit);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.suit.toString() + " " + card.cardtype.toString() + " to the Supporters deck";
    }
}

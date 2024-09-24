package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class Train extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public Train(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getCurrentPlayer() == playerID && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            Deck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> discardPile = state.getDiscardPile();
            state.increaseActionsPlayed();
            for (int i = 0; i < hand.getSize(); i++){
                if (hand.get(i).equals(card)){
                    discardPile.add(hand.get(i));
                    hand.remove(i);
                    try {
                        state.addOfficer();
                    }catch (Exception e){
                        System.out.println(e.toString());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Train(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Train){
            Train other = (Train) obj;
            return playerID == other.playerID && card.equals(other.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Train", playerID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString()  + " discards " + card.cardtype.toString() + " to train an officer";
    }
}

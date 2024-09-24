package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class Mobilize extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public Mobilize(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            Deck<RootCard> hand = state.getPlayerHand(playerID);
            Deck<RootCard> supporters = state.getSupporters();
            for(int i = 0; i < hand.getSize(); i++){
                if(hand.get(i).equals(card)){
                    supporters.add(hand.get(i));
                    hand.remove(i);
                }
            }
            state.increaseActionsPlayed();
            //todo discarding supporters
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Mobilize(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof Mobilize){
            Mobilize other = (Mobilize) obj;
            return playerID == other.playerID && card.equals(other.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Mobilize", playerID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.cardtype.toString() + "to supporters";
    }
}

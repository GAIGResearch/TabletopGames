package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class AddToDecree extends AbstractAction {
    public final int playerID;
    public final RootCard card;
    public final int index;
    public final boolean passSubGamePhase;

    public AddToDecree(int playerID, int index, RootCard card, boolean passSubGamePhase){
        this.card = card;
        this.playerID = playerID;
        this.index = index;
        this.passSubGamePhase = passSubGamePhase;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if(state.getPlayerFaction(playerID) == RootParameters.Factions.EyrieDynasties && state.getCurrentPlayer() == playerID){
            if(passSubGamePhase){
                state.increaseSubGamePhase();
            }
            Deck<RootCard> hand = state.getPlayerHand(playerID);
            for(int i = 0; i < hand.getSize(); i++ ){
                if (hand.get(i).equals(card)){
                    state.getDecree().get(index).add(hand.get(i));
                    hand.remove(i);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new AddToDecree(playerID, index, card, passSubGamePhase);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof AddToDecree){
            AddToDecree other= (AddToDecree) obj;
            return playerID == other.playerID && index == other.index && card.cardtype == other.card.cardtype && card.suit == other.card.suit && passSubGamePhase == other.passSubGamePhase;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("AddToDecree", playerID, index, card.cardtype, card.suit,passSubGamePhase);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootParameters rp = (RootParameters) gameState.getGameParameters();
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.suit + " card " + card.cardtype + " to the decree at " + rp.decreeInitializer.get(index).toString();
    }
}

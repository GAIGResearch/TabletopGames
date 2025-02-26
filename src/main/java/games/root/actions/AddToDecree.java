package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;

import java.util.Objects;

public class AddToDecree extends AbstractAction {
    public final int playerID;
    public final int index;
    public final boolean passSubGamePhase;
    public final int cardIdx, cardId;

    public AddToDecree(int playerID, int index, int cardIdx, int cardId, boolean passSubGamePhase){
        this.cardIdx = cardIdx;
        this.cardId = cardId;
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
            RootCard card = hand.pick(cardIdx);
            state.getDecree().get(index).add(card);
            return true;
        }
        return false;
    }

    @Override
    public AddToDecree copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddToDecree that = (AddToDecree) o;
        return playerID == that.playerID && index == that.index && passSubGamePhase == that.passSubGamePhase && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, index, passSubGamePhase, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID + " adds card " + cardIdx + " to the decree at " + index;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootParameters rp = (RootParameters) gameState.getGameParameters();
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " adds " + card.suit + " card " + card.cardType + " to the decree at " + rp.decreeInitializer.get(index).toString();
    }
}

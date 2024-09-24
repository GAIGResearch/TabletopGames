package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class Overwork extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public RootCard card;

    public Overwork(int playerID, int locationID, RootCard card){
        this.card = card;
        this.playerID = playerID;
        this.locationID = locationID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat && currentState.getWood()>0){
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
            for (int i = 0; i < hand.getSize(); i++){
                if (hand.get(i).equals(card)){
                    location.addToken(RootParameters.TokenType.Wood);
                    currentState.removeWood();
                    currentState.getDiscardPile().add(hand.get(i));
                    hand.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new Overwork(playerID, locationID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof Overwork o){
            return playerID == o.playerID && locationID == o.locationID && card.equals(o.card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("Overwork", playerID, locationID, card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " overworks and places a wood token on " + gs.getGameMap().getNodeByID(locationID).identifier + " by spending " + card.suit.toString() + " card " + card.cardtype.toString() ;
    }
}

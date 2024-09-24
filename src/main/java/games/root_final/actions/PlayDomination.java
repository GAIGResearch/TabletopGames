package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.PartialObservableDeck;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;

import java.util.Objects;

public class PlayDomination extends AbstractAction {
    public final int playerID;
    public RootCard card;

    public PlayDomination(int playerID, RootCard card){
        this.playerID = playerID;
        this.card = card;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            if (currentState.getGameScore(playerID) >= 10){
                PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
                for (int i = 0; i< hand.getSize(); i++){
                    if (hand.get(i).equals(card)){
                        currentState.setGameScorePlayer(playerID, 0);
                        switch (hand.get(i).suit){
                            case Rabbit -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DR);
                            case Mouse -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DM);
                            case Fox -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DF);
                            case Bird -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DB);
                        }
                        hand.remove(i);
                        return true;
                    }
                }
            }else{
                System.out.println("Trying to play domination card with less than 10 score");
                return false;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlayDomination(playerID, card);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof PlayDomination pd){
            return pd.playerID == playerID && pd.card.equals(card);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PlayDomination", card.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " plays " + card.suit.toString() + " " + card.cardtype.toString();
    }
}

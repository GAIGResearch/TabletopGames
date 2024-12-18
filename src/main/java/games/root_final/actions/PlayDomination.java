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
    public final int cardIdx, cardId;

    public PlayDomination(int playerID, int cardIdx, int cardId){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID){
            if (currentState.getGameScore(playerID) >= 10){  // todo param
                PartialObservableDeck<RootCard> hand = currentState.getPlayerHand(playerID);
                RootCard card = hand.pick(cardIdx);
                currentState.setGameScorePlayer(playerID, 0);
                switch (card.suit){
                    case Rabbit -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DR);
                    case Mouse -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DM);
                    case Fox -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DF);
                    case Bird -> currentState.setPlayerVictoryCondition(playerID, RootParameters.VictoryCondition.DB);
                }
                return true;
            }else{
                System.out.println("Trying to play domination card with less than 10 score");
                return false;
            }
        }
        return false;
    }

    @Override
    public PlayDomination copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayDomination that = (PlayDomination) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId);
    }

    @Override
    public String toString() {
        return "p" + playerID  + " plays domination";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootCard card = (RootCard) gs.getComponentById(cardId);
        return gs.getPlayerFaction(playerID).toString() + " plays " + card.suit.toString() + " " + card.cardtype.toString();
    }
}

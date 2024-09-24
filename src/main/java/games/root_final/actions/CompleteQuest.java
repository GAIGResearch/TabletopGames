package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import evaluation.metrics.Event;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootCard;
import games.root_final.cards.RootQuestCard;

import java.util.Objects;

public class CompleteQuest extends AbstractAction {
    public final int playerID;
    public RootQuestCard card;
    public final boolean draw;

    public CompleteQuest(int playerID, RootQuestCard card, boolean draw){
        this.playerID = playerID;
        this.card = card;
        this.draw = draw;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (state.getCurrentPlayer() == playerID && state.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            Deck<RootQuestCard> activeQuests = state.getActiveQuests();
            Deck<RootQuestCard> quests = state.getQuestDrawPile();
            PartialObservableDeck<RootCard> hand = state.getPlayerHand(playerID);
            for (int i = 0; i < activeQuests.getSize(); i++){
                if (activeQuests.get(i).equals(card)){
                    activeQuests.remove(i);
                    if (quests.getSize() > 0){
                        activeQuests.add(quests.draw());
                    }
                    state.CompleteQuest(card.suit);

                    if (draw){
                        for (int e = 0; e < 2; e++) {
                            if (state.getDrawPile().getSize() == 0){
                                for (int j = 0; j< state.getDiscardPile().getSize(); e++){
                                    state.getDrawPile().add(state.getDiscardPile().draw());
                                }
                                state.getDrawPile().shuffle(0, state.getDrawPile().getSize(), state.getRnd());
                            }
                            if (state.getDrawPile().getSize() > 0) {
                                hand.add(state.getDrawPile().draw());
                            }else {
                                state.logEvent(Event.GameEvent.GAME_EVENT, "Draw pile and discard piles are empty");
                            }
                        }
                    } else {
                        state.addGameScorePLayer(playerID, state.getCompletedQuests(card.suit));
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new CompleteQuest(playerID, card, draw);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof CompleteQuest c){
            return playerID == c.playerID && card.equals(c.card) && draw == c.draw;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("CompleteQuest", playerID, card.hashCode(), draw);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        if (draw){
            return gs.getPlayerFaction(playerID).toString() + " completes " + card.suit.toString() + " quest " + card.cardType.toString() + " and draws 2 cards";
        }
        return gs.getPlayerFaction(playerID).toString() + " completes " + card.suit.toString() + " quest " + card.cardType.toString() + " and scores points";

    }
}

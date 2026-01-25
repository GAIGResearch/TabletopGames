package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import evaluation.metrics.Event;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.cards.RootCard;
import games.root.components.cards.RootQuestCard;

import java.util.Objects;

public class CompleteQuest extends AbstractAction {
    public final int playerID;
    public final int cardIdx, cardId;
    public final boolean draw;

    public CompleteQuest(int playerID, int cardIdx, int cardId, boolean draw){
        this.playerID = playerID;
        this.cardIdx = cardIdx;
        this.cardId = cardId;
        this.draw = draw;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        if (state.getCurrentPlayer() == playerID && state.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            Deck<RootQuestCard> activeQuests = state.getActiveQuests();
            Deck<RootQuestCard> quests = state.getQuestDrawPile();
            PartialObservableDeck<RootCard> hand = state.getPlayerHand(playerID);

            RootQuestCard card = activeQuests.pick(cardIdx);
            if (quests.getSize() > 0){
                activeQuests.add(quests.draw());
            }
            state.completeQuest(card.suit);

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
                state.addGameScorePlayer(playerID, state.getCompletedQuests(card.suit));
            }
            return true;
        }
        return false;
    }

    @Override
    public CompleteQuest copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompleteQuest that = (CompleteQuest) o;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId && draw == that.draw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, cardIdx, cardId, draw);
    }

    @Override
    public String toString() {
        return "p" + playerID + " completes quest " + cardIdx;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        RootQuestCard card = (RootQuestCard) gs.getComponentById(cardId);
        if (draw){
            return gs.getPlayerFaction(playerID).toString() + " completes " + card.suit.toString() + " quest " + card.cardType.toString() + " and draws 2 cards";
        }
        return gs.getPlayerFaction(playerID).toString() + " completes " + card.suit.toString() + " quest " + card.cardType.toString() + " and scores points";

    }
}

package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.CompleteQuest;
import games.root.actions.ExhaustItem;
import games.root.actions.choosers.ChooseCard;
import games.root.components.cards.RootQuestCard;
import games.root.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class VagabondQuestSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage{
        chooseQuest,
        Exhaust1,
        Exhaust2,
        CompleteQuest,
    }

    Stage stage = Stage.chooseQuest;
    int cardIdx = -1, cardId = -1;
    boolean done = false;

    public VagabondQuestSequence(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            currentState.increaseActionsPlayed();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        RootQuestCard card = null;
        if (cardId != -1) card = (RootQuestCard) gs.getComponentById(cardId);
        switch (stage){
            case chooseQuest:
                Deck<RootQuestCard> activeQuests = gs.getActiveQuests();
                for (int i = 0; i < activeQuests.getSize(); i++){
                    if (gs.canCompleteSpecificQuest(activeQuests.get(i))){
                        actions.add(new ChooseCard(playerID, i, activeQuests.get(i).getComponentID()));
                    }
                }
                return actions;
            case Exhaust1:
                if (card == null) return actions;
                for(Item item: gs.getSatchel()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getTeas()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getBags()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getCoins()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                return actions;
            case Exhaust2:
                if (card == null) return actions;
                for(Item item: gs.getSatchel()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getTeas()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getBags()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getCoins()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item.itemType);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                return actions;
            case CompleteQuest:
                actions.add(new CompleteQuest(playerID, cardIdx, cardId, true));
                actions.add(new CompleteQuest(playerID, cardIdx, cardId, false));
                return actions;
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        switch (stage){
            case chooseQuest:
                if (action instanceof ChooseCard cqc) {
                    cardIdx = cqc.cardIdx;
                    cardId = cqc.cardId;
                }
                stage = Stage.Exhaust1;
                break;
            case Exhaust1:
                stage = Stage.Exhaust2;
                break;
            case Exhaust2:
                stage = Stage.CompleteQuest;
                break;
            case CompleteQuest:
                done = true;
                break;
        }

    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public VagabondQuestSequence copy() {
        VagabondQuestSequence copy = new VagabondQuestSequence(playerID);
        copy.done = done;
        copy.cardId = cardId;
        copy.cardIdx = cardIdx;
        copy.stage = stage;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VagabondQuestSequence that)) return false;
        return playerID == that.playerID && cardIdx == that.cardIdx && cardId == that.cardId && done == that.done && stage == that.stage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, stage, cardIdx, cardId, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to complete a quest";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to complete a quest";
    }
}

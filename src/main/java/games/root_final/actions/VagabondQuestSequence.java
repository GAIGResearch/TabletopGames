package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.cards.RootQuestCard;
import games.root_final.components.Item;

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

    public Stage stage = Stage.chooseQuest;
    public RootQuestCard card;
    public boolean done = false;

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
        switch (stage){
            case chooseQuest:
                Deck<RootQuestCard> activeQuests = gs.getActiveQuests();
                for (int i = 0; i < activeQuests.getSize(); i++){
                    if (gs.canCompleteSpecificQuest(activeQuests.get(i))){
                        actions.add(new ChooseQuestCard(playerID, activeQuests.get(i)));
                    }
                }
                return actions;
            case Exhaust1:
                for(Item item: gs.getSachel()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getTeas()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getBags()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getCoins()){
                    if (item.itemType == card.requirement1 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                return actions;
            case Exhaust2:
                for(Item item: gs.getSachel()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getTeas()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getBags()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                for(Item item: gs.getCoins()){
                    if (item.itemType == card.requirement2 && item.refreshed && !item.damaged){
                        ExhaustItem action = new ExhaustItem(playerID, item);
                        if (!actions.contains(action)){
                            actions.add(action);
                        }
                    }
                }
                return actions;
            case CompleteQuest:
                actions.add(new CompleteQuest(playerID, card, true));
                actions.add(new CompleteQuest(playerID, card, false));
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
                if (action instanceof ChooseQuestCard cqc) {
                    card = cqc.card;
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
        copy.card = card;
        copy.stage = stage;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof VagabondQuestSequence v){
            if (card == null || v.card == null){
                return playerID == v.playerID && stage == v.stage && card == v.card && done == v.done;
            }
            return playerID == v.playerID && stage == v.stage && card.equals(v.card) && done == v.done;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VQuestSequence", playerID, stage, done);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to complete a quest";
    }
}

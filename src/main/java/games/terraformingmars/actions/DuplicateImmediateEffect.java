package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.components.TMCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DuplicateImmediateEffect extends TMAction implements IExtendedSequence {
    final public TMTypes.Tag tagRequirement;  // tag card chosen must have
    final public Class<? extends TMAction> actionClass;  // what type of effect can be duplicated
    final public boolean production;  // If modify player resource, must it be production?

    public DuplicateImmediateEffect(TMTypes.Tag tagRequirement, Class<? extends TMAction> actionClass, boolean production) {
        super(-1, true);
        this.actionClass = actionClass;
        this.tagRequirement = tagRequirement;
        this.production = production;
        this.setCardID(-1);
    }

    public DuplicateImmediateEffect(int player, int cardID, Class<? extends TMAction> actionClass, TMTypes.Tag tagRequirement, boolean production) {
        super(player, true);
        this.setCardID(cardID);
        this.actionClass = actionClass;
        this.tagRequirement = tagRequirement;
        this.production = production;
    }

    @Override
    public boolean _execute(TMGameState gameState) {
        if (getCardID() == -1) {
            // Put viable cards in card choice deck
            boolean found = false;
            for (TMCard card : gameState.getPlayedCards()[player].getComponents()) {
                for (TMTypes.Tag t : card.tags) {
                    if (t == tagRequirement) {
                        for (TMAction action : card.immediateEffects) {
                            if (action.getClass().equals(actionClass) && (!actionClass.equals(ModifyPlayerResource.class) || ((ModifyPlayerResource) action).production == production)) {
                                gameState.getPlayerCardChoice()[player].add(card);
                                found = true;
                                break;
                            }
                        }
                        if (found) {
                            gameState.setActionInProgress(this);
                            break;
                        }
                    }
                }
                if (found) break;
            }
            if (!found) {
                int b = 0;
            }
        } else {
            // Execute all effects that match this on the card
            TMCard card = (TMCard) gameState.getComponentById(getCardID());
            for (TMAction action : card.immediateEffects) {
                if (action.getClass().equals(actionClass) && (!actionClass.equals(ModifyPlayerResource.class) || ((ModifyPlayerResource) action).production == production)) {
                    action.player = player;
                    action.execute(gameState);
                }
            }
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Choose card from card choice, and execute all effects that match this
        TMGameState gs = (TMGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        for (TMCard card: gs.getPlayerCardChoice()[player].getComponents()) {
            actions.add(new DuplicateImmediateEffect(player, card.getComponentID(), actionClass, tagRequirement, production));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        setCardID(((TMAction)action).getCardID());
        TMGameState gs = (TMGameState) state;
        gs.getPlayerCardChoice()[player].clear();
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return getCardID() != -1;
    }

    @Override
    public DuplicateImmediateEffect copy() {
        return (DuplicateImmediateEffect) super.copy();
    }

    @Override
    public DuplicateImmediateEffect _copy() {
        return new DuplicateImmediateEffect(player, getCardID(), actionClass, tagRequirement, production);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DuplicateImmediateEffect)) return false;
        if (!super.equals(o)) return false;
        DuplicateImmediateEffect that = (DuplicateImmediateEffect) o;
        return production == that.production && tagRequirement == that.tagRequirement && Objects.equals(actionClass, that.actionClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagRequirement, actionClass, production);
    }
}

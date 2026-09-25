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
    public TMTypes.Tag tagRequirement;  // tag card chosen must have
    public String actionClassName;  // what type of effect can be duplicated
    public boolean production;  // If modify player resource, must it be production?

    public DuplicateImmediateEffect() // This is needed for JSON Deserializer
    {
        super();
    }

    public DuplicateImmediateEffect(TMTypes.Tag tagRequirement, String actionClassName, boolean production) {
        super(-1, true);
        this.actionClassName = actionClassName;
        this.tagRequirement = tagRequirement;
        this.production = production;
        this.setCardID(-1);
    }

    public DuplicateImmediateEffect(int player, int cardID, String actionClassName, TMTypes.Tag tagRequirement, boolean production) {
        super(player, true);
        this.setCardID(cardID);
        this.actionClassName = actionClassName;
        this.tagRequirement = tagRequirement;
        this.production = production;
    }

    @Override
    public boolean _execute(TMGameState gameState) {
        if (getCardID() == -1) {
            // Put all viable cards in card choice deck
            for (TMCard card : gameState.getPlayedCards()[player].getComponents()) {
                if (hasTag(card) && canDuplicate(card, gameState)) {
                    gameState.getPlayerCardChoice()[player].add(card);
                }
            }
            if (gameState.getPlayerCardChoice()[player].getSize() > 0) {
                gameState.setActionInProgress(this);
            }
        } else {
            // Execute all effects that match this on the card
            TMCard card = (TMCard) gameState.getComponentById(getCardID());
            for (TMAction action : card.immediateEffects) {
                if (matches(action)) {
                    action.player = player;
                    action.execute(gameState);
                }
            }
        }
        return true;
    }

    private boolean hasTag(TMCard card) {
        for (TMTypes.Tag t : card.tags) {
            if (t == tagRequirement) return true;
        }
        return false;
    }

    private boolean canDuplicate(TMCard card, TMGameState gs) {
        // Card must have at least one matching effect, and the player must be able to execute all of them
        boolean found = false;
        for (TMAction action : card.immediateEffects) {
            if (matches(action)) {
                action.player = player;
                if (!action.canBePlayed(gs)) return false;
                found = true;
            }
        }
        return found;
    }

    private boolean matches(TMAction action) {
        return action.getClass().getSimpleName().equalsIgnoreCase(actionClassName) && (!actionClassName.equalsIgnoreCase("ModifyPlayerResource") || ((ModifyPlayerResource) action).production == production);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        // Choose card from card choice, and execute all effects that match this
        TMGameState gs = (TMGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        for (TMCard card: gs.getPlayerCardChoice()[player].getComponents()) {
            actions.add(new DuplicateImmediateEffect(player, card.getComponentID(), actionClassName, tagRequirement, production));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
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
        return new DuplicateImmediateEffect(player, getCardID(), actionClassName, tagRequirement, production);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DuplicateImmediateEffect)) return false;
        if (!super.equals(o)) return false;
        DuplicateImmediateEffect that = (DuplicateImmediateEffect) o;
        return production == that.production && tagRequirement == that.tagRequirement && Objects.equals(actionClassName, that.actionClassName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tagRequirement, actionClassName, production);
    }

    @Override
    public String toString() {
        return "Duplicate card";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

package games.dominion.actions;

import core.actions.AbstractAction;
import games.dominion.DominionGameState;
import games.dominion.cards.CardType;

import java.util.List;

/**
 * This marks an Action which triggers a whole sequence of decisions. To avoid bloating the forward model
 * or GamesState with keeping track of specific logic for each different card, this is tracked locally. When one of these
 * actions is first executed, it stores a reference to itself on the GameState. This is the trigger for FM/GS to
 * delegate action specification to the ExtendedAction.
 * After each such action is executed, the ExtendedAction is informed of the decision to keep track of relevant state,
 * and once complete it marks itself as executed, which will prompt the GameState/ForwardModel to move on.
 */
public abstract class ExtendedDominionAction extends DominionAction {

    protected ExtendedDominionAction(CardType type, int playerId) {
        super(type, playerId);
    }

    public abstract List<AbstractAction> followOnActions(DominionGameState state);

    public abstract void registerActionTaken(DominionGameState state, AbstractAction action);

    public abstract boolean executionComplete();
}

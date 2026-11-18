package games.gofish;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * <p>Sets which kinds of public knowledge ({@link GoFishKnowledge}) an agent uses about the other hands in Go Fish.
 * Before the agent decides, the decorator makes the agent's observation forget the other kinds and redeterminises it,
 * so that the agent's search, and every perspective copy it makes from the observation, works only from what the
 * agent knows. It changes no actions, and does nothing in any other game.</p>
 *
 * <p>In a player's JSON (the args are cards fished for, voids, the asker's card):</p>
 * <pre>
 * "actionRestriction" : {
 *     "class" : "games.gofish.GoFishKnowledgeDecorator",
 *     "args" : [true, false, false]
 * }
 * </pre>
 */
public class GoFishKnowledgeDecorator implements IPlayerDecorator {

    public final Set<GoFishKnowledge> knowledge;

    public GoFishKnowledgeDecorator(boolean cardsFishedFor, boolean voids, boolean askersCard) {
        knowledge = Collections.unmodifiableSet(GoFishKnowledge.of(cardsFishedFor, voids, askersCard));
    }

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        // within a search the states are already restricted, so this does nothing
        if (state instanceof GoFishGameState goFishState)
            goFishState.restrictKnowledge(knowledge, state.getCurrentPlayer());
        return possibleActions;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GoFishKnowledgeDecorator that && knowledge.equals(that.knowledge);
    }

    @Override
    public int hashCode() {
        return knowledge.hashCode() + 7193;
    }

    @Override
    public String toString() {
        return "GoFishKnowledge" + knowledge;
    }
}

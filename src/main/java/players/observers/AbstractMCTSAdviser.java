package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.mcts.MCTSEnums;
import players.mcts.MCTSPlayer;

public abstract class AbstractMCTSAdviser implements IAdviceFilter {

    protected double adviceThreshold;

    public AbstractMCTSAdviser(double adviceThreshold) {
        this.adviceThreshold = adviceThreshold;
    }

    @Override
    public boolean provideAdvice(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee,
                                 AbstractAction advice, GameAdviser adviser) {
        if (adviser.player instanceof MCTSPlayer mctsPlayer) {
            double valueOfProposedAction = mctsPlayer.getValue(proposedAction);
            double valueOfOurAction = mctsPlayer.getValue(advice);
            if (mctsPlayer.getParameters().selectionPolicy == MCTSEnums.SelectionPolicy.ROBUST) {
                // use visit count proportion instead
                valueOfProposedAction = mctsPlayer.getVisitProportion(proposedAction);
                valueOfOurAction = mctsPlayer.getVisitProportion(advice);
            }
            return valueOfOurAction - valueOfProposedAction > adviceThreshold;
        } else {
            throw new AssertionError("AbstractMCTSAdviser only supports MCTS players");
        }
    }
}

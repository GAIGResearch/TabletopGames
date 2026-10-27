package players.muditbot;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import players.basicMCTS.BasicMCTSParams;
import players.simple.RandomPlayer;

import java.util.List;

/**
 * MuditBot variant with draft-aware rollouts and known-card estimates. It keeps the
 * same MCTS search, but changes the state estimate and rollout action policy.
 */
public class MuditBotCardPicking extends MuditBot {

    public MuditBotCardPicking() {
        super();
        useCardPickingStrategy();
    }

    public MuditBotCardPicking(long seed) {
        super(seed);
        useCardPickingStrategy();
    }

    public MuditBotCardPicking(BasicMCTSParams params) {
        super(params);
        useCardPickingStrategy();
    }

    private void useCardPickingStrategy() {
        // Replace full-deck estimates with visibility-aware card availability while
        // preserving the standard MuditBot settings and search implementation.
        getParameters().setParameterValue("heuristic",
                (IStateHeuristic) MuditBot::evaluateCardPickingState);
        getParameters().heuristic = MuditBot::evaluateCardPickingState;
        setName("muditbot_cardpicking");
    }

    @Override
    public AbstractAction getRolloutAction(AbstractGameState state, List<AbstractAction> actions,
                                           RandomPlayer randomPlayer) {
        // Use the Sushi Go card-value policy during simulated drafts; non-card or
        // extended-sequence actions fall back to the base random rollout policy.
        return selectCardPickingRolloutAction(state, actions, randomPlayer);
    }

    @Override
    public MuditBotCardPicking copy() {
        return new MuditBotCardPicking((BasicMCTSParams) getParameters().copy());
    }
}

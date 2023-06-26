package players.rl;

import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;

public abstract class QWeightsDataStructure {

    protected RLParams params;
    protected RLTrainingParams trainingParams;

    protected QWeightsDataStructure() {
        initQWeights();
    }

    protected abstract void initQWeights();

    protected abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    protected abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    protected abstract void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1);

    protected abstract void parseQWeights(String[] qWeightStrings);

    protected abstract String qWeightsToString();

    protected void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);
            qLearning(player, t0, t1);
        }
    }

    protected void setParams(RLParams params) {
        this.params = params;
    }

    protected void setTrainingParams(RLTrainingParams trainingParams) {
        this.trainingParams = trainingParams;
    }

}
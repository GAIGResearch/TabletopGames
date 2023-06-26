package players.rl.dataStructures;
import java.util.List;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.rl.RLParams;
import players.rl.RLPlayer;
import players.rl.RLTrainingParams;

public abstract class QWeightsDataStructure {

    public RLParams params;
    public RLTrainingParams trainingParams;

    QWeightsDataStructure() {
        initQWeights();
    }

    public abstract void initQWeights();

    public abstract void add(RLPlayer player, AbstractGameState state, AbstractAction action, double q);

    public abstract double evaluateQ(RLPlayer player, AbstractGameState state, AbstractAction action);

    public abstract void qLearning(RLPlayer player, TurnSAR t0, TurnSAR t1);

    public abstract void parseQWeights(String[] qWeightStrings);

    public abstract String qWeightsToString();

    public void qLearning(RLPlayer player, List<TurnSAR> turns) {
        // Learn
        for (int i = turns.size() - 2; i >= 0; i--) {
            TurnSAR t0 = turns.get(i);
            TurnSAR t1 = turns.get(i + 1);
            qLearning(player, t0, t1);
        }
    }

    public void setParams(RLParams params) {
        this.params = params;
    }

    public void setTrainingParams(RLTrainingParams trainingParams) {
        this.trainingParams = trainingParams;
    }

}
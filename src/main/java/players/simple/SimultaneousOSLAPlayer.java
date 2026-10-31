package players.simple;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimultaneousOSLAPlayer extends OSLAPlayer{

    RandomPlayer randomPlayer;

    public SimultaneousOSLAPlayer(Random random) {
        super(random);
        setName("SimultaneousOSLAPlayer");
    }
    public SimultaneousOSLAPlayer() {
        super();
        setName("SimultaneousOSLAPlayer");
    }

    public SimultaneousOSLAPlayer(IStateHeuristic heuristic) {
        super(heuristic, new Random());
        setName("SimultaneousOSLAPlayer");
    }

    public SimultaneousOSLAPlayer(IStateHeuristic heuristic, Random random) {
        super(heuristic, random);
        setName("SimultaneousOSLAPlayer");
    }

    // TODO parameterized when to stop rolling out
    // TODO make this RolloutOSLA?
    @Override
    protected void rollNextGameState(AbstractGameState gs, AbstractAction a) {
        int currentRound = gs.getRoundCounter();
        getForwardModel().next(gs, a);
        if (randomPlayer == null) {
            randomPlayer = new RandomPlayer(new Random(rnd.nextInt()));
        }
        List<AbstractAction> actions;
        while (gs.getRoundCounter() == currentRound && gs.isNotTerminal()) {
            actions = getForwardModel().computeAvailableActions(gs);
            if (actions.isEmpty()) {
                break;
            }
            getForwardModel().next(gs, randomPlayer.getAction(gs, actions));
        }
    }

    @Override
    public SimultaneousOSLAPlayer copy() {
        SimultaneousOSLAPlayer retValue = new SimultaneousOSLAPlayer(heuristic, new Random(rnd.nextInt()));
        retValue.setForwardModel(getForwardModel());
        return retValue;
    }
}

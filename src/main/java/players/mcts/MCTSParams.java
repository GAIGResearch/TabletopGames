package players.mcts;

import core.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import players.PlayerConstants;
import players.PlayerParameters;
import players.simple.RandomPlayer;

import java.io.FileReader;
import java.util.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = false;
    public double epsilon = 1e-6;
    public String rolloutType = "Random";

    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(6, 8, 10, 12, 20));
        addTunableParameter("rolloutsEnabled", false, Arrays.asList(false, true));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("rolloutType", "Random");
    }

    @Override
    public void _reset() {
        K = (double) getParameterValue("K");
        rolloutLength = (int) getParameterValue("rolloutLength");
        rolloutsEnabled = (boolean) getParameterValue("rolloutsEnabled");
        epsilon = (double) getParameterValue("epsilon");
        rolloutType = (String) getParameterValue("rolloutType");
        super._reset();
    }

    @Override
    protected AbstractParameters _copy() {
        return new MCTSParams(System.currentTimeMillis());
    }

    /**
     * @return Returns the AbstractPlayer policy that will take actions during an MCTS rollout.
     *         This defaults to a Random player.
     */
    public AbstractPlayer getRolloutStrategy() {
        return new RandomPlayer(new Random(getRandomSeed()));
    }


    @Override
    public MCTSPlayer instantiate() {
        return new MCTSPlayer(this);
    }
}

package players.mcts;

import core.*;
import players.PlayerParameters;
import players.simple.RandomPlayer;
import java.util.*;

public class MCTSParams extends PlayerParameters {

    public double K = Math.sqrt(2);
    public int rolloutLength = 10;
    public boolean rolloutsEnabled = false;
    public double epsilon = 1e-6;
    public String rolloutType = "Random";
    public final Map<String, Class<?>> types = new HashMap<>();
    {
       types.put("K", double.class);
       types.put("rolloutLength", int.class);
       types.put("rolloutsEnabled", boolean.class);
       types.put("epsilon", double.class);
       types.put("rolloutType", String.class);
    }

    public MCTSParams() {
        this(System.currentTimeMillis());
    }
    public MCTSParams(long seed) {
        super(seed);
        addTunableParameter("K", Math.sqrt(2), Arrays.asList(0.0, 0.1, 1.0, Math.sqrt(2), 3.0, 10.0));
        addTunableParameter("rolloutLength", 10, Arrays.asList(6, 8, 10, 12, 20));
        addTunableParameter("rolloutsEnabled", false, Arrays.asList(false, true));
        addTunableParameter("epsilon", 1e-6);
        addTunableParameter("rolloutType", "Random");
    }

    /**
     * Names all parameters for printing purposes.
     *
     * @return mapping from int ID of parameter to parameter name.
     */
    @Override
    public Map<String, Class<?>> getParameterTypes() {
        return types;
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

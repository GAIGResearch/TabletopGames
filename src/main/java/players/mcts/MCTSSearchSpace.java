package players.mcts;

import core.AbstractPlayer;
import evodef.AgentSearchSpace;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MCTSSearchSpace extends AgentSearchSpace<AbstractPlayer> {

    private MCTSParams baseParams;

    public MCTSSearchSpace(MCTSParams defaultParams, String searchSpaceFile) {
        super(searchSpaceFile);
        baseParams = defaultParams;
    }

    @NotNull
    @Override
    public Map<String, Class<?>> getTypes() {
        Map<String, Class<?>> retValue  = new HashMap<>();
        retValue.put("K", Double.class);
        retValue.put("rolloutLength", Integer.class);
        retValue.put("rolloutsEnabled", Boolean.class);
        retValue.put("rolloutType", String.class);
        retValue.put("epsilon", Double.class);
        return retValue;
    }

    @Override
    public MCTSPlayer getAgent(@NotNull double[] settings) {
        Map<String, Object> settingsMap = settingsToMap(settings);
        MCTSParams params = (MCTSParams) baseParams.copy();
        params.K = (double) settingsMap.getOrDefault("K", params.K);
        params.rolloutLength = (int) settingsMap.getOrDefault("rolloutLength", params.rolloutLength);
        params.rolloutsEnabled = (boolean) settingsMap.getOrDefault("rolloutsEnabled", params.rolloutsEnabled);
        params.rolloutType = (String) settingsMap.getOrDefault("rolloutType", params.rolloutType);
        params.epsilon = (double) settingsMap.getOrDefault("epsilon", params.epsilon);
        return new MCTSPlayer(params);
    }
}

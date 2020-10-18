package players.mcts;

import com.sun.org.apache.xpath.internal.operations.Bool;
import evodef.AgentSearchSpace;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MCTSSearchSpace extends AgentSearchSpace<MCTSPlayer> {

    private MCTSParams baseParams;

    public MCTSSearchSpace(MCTSParams defaultParams, @NotNull String searchSpaceFile) {
        super(searchSpaceFile);
        baseParams = defaultParams;
    }

    public static <T> KClass<T> getKClass(Class<T> cls){
        return JvmClassMappingKt.getKotlinClass(cls);
    }

    @NotNull
    @Override
    public Map<String, KClass<?>> getTypes() {
        Map<String, KClass<?>> retValue  = new HashMap<>();
        retValue.put("K", getKClass(Integer.class));
        retValue.put("rolloutLength", getKClass(Integer.class));
        retValue.put("rolloutsEnabled", getKClass(Boolean.class));
        retValue.put("rolloutType", getKClass(String.class));
        retValue.put("epsilon", getKClass(Double.class));
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

package core;

import evaluation.optimisation.TunableParameters;
import core.actions.ActionSpace;

import java.util.Arrays;
import java.util.Objects;

public class CoreParameters extends TunableParameters<Object> {
    public boolean verbose = false;
    public boolean recordEventHistory = true;  // include in history text game events
    public boolean partialObservable = true;
    public boolean competitionMode = false;
    public boolean disqualifyPlayerOnIllegalActionPlayed = false;
    public boolean disqualifyPlayerOnTimeout = false;
    public boolean alwaysDisplayFullObservable = false;
    public boolean alwaysDisplayCurrentPlayer = false;
    public long frameSleepMS = 100L;

    // Action space type for this game
    public ActionSpace actionSpace = new ActionSpace(ActionSpace.Structure.Flat, ActionSpace.Flexibility.Default, ActionSpace.Context.Dependent);

    public CoreParameters() {
        addTunableParameter("verbose", verbose, Arrays.asList(false, true));
        addTunableParameter("recordEventHistory", recordEventHistory, Arrays.asList(false, true));
        addTunableParameter("partial observable", partialObservable, Arrays.asList(false, true));
        addTunableParameter("competition mode", competitionMode, Arrays.asList(false, true));
        addTunableParameter("disqualify player on illegal action played", disqualifyPlayerOnIllegalActionPlayed, Arrays.asList(false, true));
        addTunableParameter("disqualify player on timeout", disqualifyPlayerOnTimeout, Arrays.asList(false, true));
        addTunableParameter("always display full observable", alwaysDisplayFullObservable, Arrays.asList(false, true));
        addTunableParameter("always display current player", alwaysDisplayCurrentPlayer, Arrays.asList(false, true));
        addTunableParameter("frame sleep MS", frameSleepMS, Arrays.asList(0L, 100L, 500L, 1000L, 5000L));
        addTunableParameter("actionSpaceStructure", ActionSpace.Structure.Default, Arrays.asList(ActionSpace.Structure.values()));
        addTunableParameter("actionSpaceFlexibility", ActionSpace.Flexibility.Default, Arrays.asList(ActionSpace.Flexibility.values()));
        addTunableParameter("actionSpaceContext", ActionSpace.Context.Default, Arrays.asList(ActionSpace.Context.values()));
    }

    @Override
    protected AbstractParameters _copy() {
        return new CoreParameters();
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoreParameters)) return false;
        if (!super.equals(o)) return false;
        CoreParameters that = (CoreParameters) o;
        return verbose == that.verbose && recordEventHistory == that.recordEventHistory && partialObservable == that.partialObservable && competitionMode == that.competitionMode && disqualifyPlayerOnIllegalActionPlayed == that.disqualifyPlayerOnIllegalActionPlayed && disqualifyPlayerOnTimeout == that.disqualifyPlayerOnTimeout && alwaysDisplayFullObservable == that.alwaysDisplayFullObservable && alwaysDisplayCurrentPlayer == that.alwaysDisplayCurrentPlayer && frameSleepMS == that.frameSleepMS && Objects.equals(actionSpace, that.actionSpace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), verbose, recordEventHistory, partialObservable, competitionMode, disqualifyPlayerOnIllegalActionPlayed, disqualifyPlayerOnTimeout, alwaysDisplayFullObservable, alwaysDisplayCurrentPlayer, frameSleepMS, actionSpace);
    }

    @Override
    public Object instantiate() {
        return null;
    }

    @Override
    public void _reset() {
        verbose = (boolean) getParameterValue("verbose");
        recordEventHistory = (boolean) getParameterValue("recordEventHistory");
        partialObservable = (boolean) getParameterValue("partial observable");
        competitionMode = (boolean) getParameterValue("competition mode");
        disqualifyPlayerOnIllegalActionPlayed = (boolean) getParameterValue("disqualify player on illegal action played");
        disqualifyPlayerOnTimeout = (boolean) getParameterValue("disqualify player on timeout");
        alwaysDisplayFullObservable = (boolean) getParameterValue("always display full observable");
        alwaysDisplayCurrentPlayer = (boolean) getParameterValue("always display current player");
        frameSleepMS = Long.parseLong(String.valueOf(getParameterValue("frame sleep MS")));
        actionSpace = new ActionSpace ((ActionSpace.Structure) getParameterValue("actionSpaceStructure"),
                (ActionSpace.Flexibility) getParameterValue("actionSpaceFlexibility"),
                (ActionSpace.Context) getParameterValue("actionSpaceContext"));
    }
}

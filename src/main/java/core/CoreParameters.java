package core;

import evaluation.TunableParameters;

import java.util.Arrays;
import java.util.Objects;

public class CoreParameters extends TunableParameters {
    public boolean verbose = false;
    public boolean partialObservable = true;
    public boolean competitionMode = false;
    public boolean disqualifyPlayerOnIllegalActionPlayed = false;
    public boolean disqualifyPlayerOnTimeout = false;
    public boolean alwaysDisplayFullObservable = false;
    public boolean alwaysDisplayCurrentPlayer = false;
    public long frameSleepMS = 100L;

    public CoreParameters() {
        super(0);
        addTunableParameter("verbose", verbose, Arrays.asList(false, true));
        addTunableParameter("partial observable", partialObservable, Arrays.asList(false, true));
        addTunableParameter("competition mode", competitionMode, Arrays.asList(false, true));
        addTunableParameter("disqualify player on illegal action played", disqualifyPlayerOnIllegalActionPlayed, Arrays.asList(false, true));
        addTunableParameter("disqualify player on timeout", disqualifyPlayerOnTimeout, Arrays.asList(false, true));
        addTunableParameter("always display full observable", alwaysDisplayFullObservable, Arrays.asList(false, true));
        addTunableParameter("always display current player", alwaysDisplayCurrentPlayer, Arrays.asList(false, true));
        addTunableParameter("frame sleep MS", frameSleepMS, Arrays.asList(0L, 100L, 500L, 1000L, 5000L));
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
        return verbose == that.verbose && partialObservable == that.partialObservable && competitionMode == that.competitionMode && disqualifyPlayerOnIllegalActionPlayed == that.disqualifyPlayerOnIllegalActionPlayed && disqualifyPlayerOnTimeout == that.disqualifyPlayerOnTimeout && alwaysDisplayFullObservable == that.alwaysDisplayFullObservable && alwaysDisplayCurrentPlayer == that.alwaysDisplayCurrentPlayer && frameSleepMS == that.frameSleepMS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), verbose, partialObservable, competitionMode, disqualifyPlayerOnIllegalActionPlayed, disqualifyPlayerOnTimeout, alwaysDisplayFullObservable, alwaysDisplayCurrentPlayer, frameSleepMS);
    }

    @Override
    public Object instantiate() {
        return null;
    }

    @Override
    public void _reset() {
        verbose = (boolean) getParameterValue("verbose");
        partialObservable = (boolean) getParameterValue("partial observable");
        competitionMode = (boolean) getParameterValue("competition mode");
        disqualifyPlayerOnIllegalActionPlayed = (boolean) getParameterValue("disqualify player on illegal action played");
        disqualifyPlayerOnTimeout = (boolean) getParameterValue("disqualify player on timeout");
        alwaysDisplayFullObservable = (boolean) getParameterValue("always display full observable");
        alwaysDisplayCurrentPlayer = (boolean) getParameterValue("always display current player");
        frameSleepMS = Long.parseLong(String.valueOf(getParameterValue("frame sleep MS")));
    }
}

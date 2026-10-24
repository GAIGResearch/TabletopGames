package games.diplomacy;

import evaluation.optimisation.TunableParameters;

import java.util.Arrays;

/**
 * Parameters for Diplomacy. The defaults are the rules of the 2000 (4th edition) Avalon Hill rulebook.
 * The board - provinces, moves, supply centres, the powers and their starting units, the start year and the
 * centres needed to win - comes from mapFile (see DiplomacyMap).
 */
public class DiplomacyParameters extends TunableParameters<DiplomacyParameters> {

    // the board, loaded from mapFile; derived from that parameter, so not part of equals
    private DiplomacyMap map;

    public String mapFile = "data/diplomacy/standardMap.json";
    // the game ends after the Fall turn of this year if nobody has won (not in the rulebook)
    public int lastYear = 1920;
    public DiplomacyParadoxRule paradoxRule = DiplomacyParadoxRule.RULEBOOK_2000;
    // the orders offered support and convoy only the power's own units (not in the rulebook: without negotiation,
    // help for another power's unit is wasted)
    public boolean ownUnitsOnly = true;

    public DiplomacyParameters() {
        addTunableParameter("mapFile", "data/diplomacy/standardMap.json");
        addTunableParameter("lastYear", 1920, Arrays.asList(1905, 1910, 1915, 1920, 1930));
        addTunableParameter("paradoxRule", DiplomacyParadoxRule.RULEBOOK_2000, Arrays.asList(DiplomacyParadoxRule.values()));
        addTunableParameter("ownUnitsOnly", true, Arrays.asList(true, false));
        _reset();
    }

    @Override
    public void _reset() {
        mapFile = (String) getParameterValue("mapFile");
        if (map == null || !map.fileName.equals(mapFile))
            map = new DiplomacyMap(mapFile);
        lastYear = (int) getParameterValue("lastYear");
        paradoxRule = (DiplomacyParadoxRule) getParameterValue("paradoxRule");
        ownUnitsOnly = (boolean) getParameterValue("ownUnitsOnly");
    }

    public DiplomacyMap getMap() {
        return map;
    }

    @Override
    protected DiplomacyParameters _copy() {
        return new DiplomacyParameters();
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof DiplomacyParameters;
    }

    @Override
    public DiplomacyParameters instantiate() {
        return this;
    }
}

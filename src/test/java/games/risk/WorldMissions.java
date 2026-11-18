package games.risk;

import games.risk.components.RiskMission;

import java.util.ArrayList;
import java.util.List;

import static games.risk.WorldContinents.*;

/**
 * The Secret Mission cards of the default map (data/risk/worldMap.json "missions", in file order), plus the six destroy
 * cards (one per colour), for the tests.
 */
class WorldMissions {

    static final RiskMission NA_AFRICA = RiskMission.conquer(List.of(NORTH_AMERICA, AFRICA), false);
    static final RiskMission NA_AUSTRALIA = RiskMission.conquer(List.of(NORTH_AMERICA, AUSTRALIA), false);
    static final RiskMission ASIA_AUSTRALIA = RiskMission.conquer(List.of(ASIA, AUSTRALIA), false);
    static final RiskMission ASIA_AFRICA = RiskMission.conquer(List.of(ASIA, AFRICA), false);
    static final RiskMission EUROPE_AUSTRALIA_3RD = RiskMission.conquer(List.of(EUROPE, AUSTRALIA), true);
    static final RiskMission EUROPE_SOUTH_AMERICA_3RD = RiskMission.conquer(List.of(EUROPE, SOUTH_AMERICA), true);
    static final RiskMission OCCUPY_24 = RiskMission.occupy(24, 1);
    static final RiskMission OCCUPY_18_TWO_ARMIES = RiskMission.occupy(18, 2);

    /** The 8 missions of the map file, in file order. */
    static final List<RiskMission> MAP_MISSIONS = List.of(NA_AFRICA, NA_AUSTRALIA, ASIA_AUSTRALIA, ASIA_AFRICA,
            EUROPE_AUSTRALIA_3RD, EUROPE_SOUTH_AMERICA_3RD, OCCUPY_24, OCCUPY_18_TWO_ARMIES);

    static final List<RiskMission> DESTROY_MISSIONS = List.of(RiskMission.destroy(0), RiskMission.destroy(1),
            RiskMission.destroy(2), RiskMission.destroy(3), RiskMission.destroy(4), RiskMission.destroy(5));

    /** All 14 mission cards. */
    static final List<RiskMission> ALL_MISSIONS;

    static {
        List<RiskMission> all = new ArrayList<>(MAP_MISSIONS);
        all.addAll(DESTROY_MISSIONS);
        ALL_MISSIONS = List.copyOf(all);
    }

    static RiskMission destroy(int k) {
        return RiskMission.destroy(k);
    }

    private WorldMissions() {
    }
}

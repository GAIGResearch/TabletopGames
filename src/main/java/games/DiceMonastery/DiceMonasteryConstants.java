package games.DiceMonastery;

import core.interfaces.IGamePhase;

public class DiceMonasteryConstants {

    public enum ActionArea {
        MEADOW(1), KITCHEN(2), WORKSHOP(3),
        GATEHOUSE(1), LIBRARY(4), CHAPEL(1),
        DORMITORY(0);

        public final int dieMinimum;

        ActionArea(int dieMinimum) {
            this.dieMinimum = dieMinimum;
        }
    }

    public enum Resource {
        GRAIN, HONEY, WAX, SKEP, BREAD, SHILLINGS, PRAYERS
    }

    public enum Phase implements IGamePhase {
        PLACE_MONKS, USE_MONKS
    }

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }
}

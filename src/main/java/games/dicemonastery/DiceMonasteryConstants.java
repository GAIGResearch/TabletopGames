package games.dicemonastery;

import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.dicemonastery.actions.*;

public class DiceMonasteryConstants {

    public enum ActionArea {
        MEADOW(1), KITCHEN(2), WORKSHOP(3),
        GATEHOUSE(1), LIBRARY(4), CHAPEL(1),
        DORMITORY(0), STOREROOM(0), SUPPLY(0);

        public final int dieMinimum;

        ActionArea(int dieMinimum) {
            this.dieMinimum = dieMinimum;
        }

        public ActionArea next() {
            switch (this) {
                case MEADOW:
                    return KITCHEN;
                case KITCHEN:
                    return WORKSHOP;
                case WORKSHOP:
                    return GATEHOUSE;
                case GATEHOUSE:
                    return LIBRARY;
                case LIBRARY:
                    return CHAPEL;
                case CHAPEL:
                    return MEADOW;
                default:
                    throw new AssertionError("Should not be processing " + this);
            }
        }
    }

    public enum Resource {
        GRAIN, HONEY, WAX, SKEP, BREAD, SHILLINGS, PRAYERS, PIGMENT, INK, CALF_SKIN, VELLUM, BEER, MEAD, CANDLE
    }

    public enum Phase implements IGamePhase {
        PLACE_MONKS, USE_MONKS
    }

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER;

        public Season next() {
            switch (this) {
                case SPRING:
                    return AUTUMN;
                case SUMMER:
                    return AUTUMN;
                case AUTUMN:
                    return SPRING;
                case WINTER:
                    return SPRING;
                default:
                    throw new AssertionError("Should not be processing " + this);
            }
        }

    }


}

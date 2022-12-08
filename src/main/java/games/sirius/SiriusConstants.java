package games.sirius;

import core.interfaces.IGamePhase;

public class SiriusConstants {

    public enum SiriusPhase implements IGamePhase {
        Move, Draw, Favour
    }

    public enum MoonType {
        MINING, TRADING, PROCESSING, METROPOLIS, OUTPOST
    }

    public enum SiriusCardType {
        AMMONIA, CONTRABAND, SMUGGLER, FAVOUR
    }


}

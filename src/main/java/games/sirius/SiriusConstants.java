package games.sirius;

import core.interfaces.IGamePhase;

public class SiriusConstants {

    public enum SiriusPhase implements IGamePhase {
        Move, Draw
    }

    public enum MoonType {
        MINING, TRADING
    }

    public enum SiriusCardType {
        AMMONIA, CONTRABAND, SMUGGLER, FAVOUR
    }


}

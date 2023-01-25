package games.sirius;

import core.interfaces.IGamePhase;

import static games.sirius.SiriusConstants.SiriusCardType.*;

public class SiriusConstants {

    public enum SiriusPhase implements IGamePhase {
        Move, Draw, Favour
    }

    public enum MoonType {
        MINING(AMMONIA), TRADING(null), PROCESSING(CONTRABAND), METROPOLIS(FAVOUR), OUTPOST(SMUGGLER);

        final SiriusCardType linkedCardType;

        MoonType(SiriusCardType cardType) {
            linkedCardType = cardType;
        }
    }

    public enum SiriusCardType {
        AMMONIA, CONTRABAND, SMUGGLER, FAVOUR
    }


}

package games.sirius;

import core.components.*;

import static utilities.Utils.ComponentType.TOKEN;

public class Medal extends Component {

    public final SiriusConstants.SiriusCardType medalType;
    public final int value;
    public Medal(SiriusConstants.SiriusCardType medalType, int value) {
        super(TOKEN, String.format("%s Medal (%d)", medalType, value));
        this.medalType = medalType;
        this.value = value;
    }

    @Override
    public Component copy() {
        return this; // immutable
    }
}

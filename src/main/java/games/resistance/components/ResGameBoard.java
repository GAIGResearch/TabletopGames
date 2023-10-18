package games.resistance.components;

import core.components.Component;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.ModifyGlobalParameter;
import utilities.Utils;

import java.util.Arrays;
import java.util.Objects;

import static core.CoreConstants.ComponentType.BOARD;
import static core.CoreConstants.ComponentType.BOARD_NODE;

public class ResGameBoard extends Component {

    int[] missionSuccessValues;

    public ResGameBoard(int[] missionSuccessValues) {
        super(BOARD, "Board");
        this.missionSuccessValues = missionSuccessValues;
    }

    protected ResGameBoard(int[] missionSuccessValues, int componentID) {
        super(BOARD, "Board", componentID);
        this.missionSuccessValues = missionSuccessValues;
    }

    public int[] getMissionSuccessValues() {
        return missionSuccessValues;
    }

    @Override
    public ResGameBoard copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResGameBoard)) return false;
        if (!super.equals(o)) return false;
        ResGameBoard other = (ResGameBoard) o;
        return Arrays.equals(missionSuccessValues, other.missionSuccessValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(missionSuccessValues));
    }
}

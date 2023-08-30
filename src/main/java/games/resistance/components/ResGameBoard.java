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

    int[] missionSuccessValues = new int[5];
    ResGameBoard type;

    public ResGameBoard(int[] missionSuccessValues) {
        super(BOARD, "Board");
        this.missionSuccessValues = missionSuccessValues;
    }

    protected ResGameBoard(int[] missionSuccessValues, int componentID) {
        super(BOARD, "Board", componentID);
        this.missionSuccessValues = missionSuccessValues;
    }

    public void setType(ResGameBoard type) {
        this.type = type;
    }
    public int[] getMissionSuccessValues() {
        return missionSuccessValues;
    }

    @Override
    public ResGameBoard copy() {
        ResGameBoard copy = new ResGameBoard(missionSuccessValues, componentID);
        copyComponentTo(copy);
        copy.type = type;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResGameBoard)) return false;
        if (!super.equals(o)) return false;

        return Arrays.equals(missionSuccessValues,this.missionSuccessValues) && type == this.type;
    }

    @Override
    public int hashCode() {
        // Potentially get rid of ownerID. Sets the owner of the gameboard as the game.
        int result = Objects.hash(super.hashCode(), ownerId, type, missionSuccessValues);
        return result;
    }
}

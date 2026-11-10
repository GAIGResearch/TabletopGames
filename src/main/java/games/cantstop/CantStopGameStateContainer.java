package games.cantstop;

import core.AbstractGameState;
import core.AbstractGameStateContainer;
import core.components.Dice;

import java.util.List;
import java.util.Map;

public class CantStopGameStateContainer extends AbstractGameStateContainer {

    protected boolean[] completedColumns;
    protected int[][] playerMarkerPositions;
    protected List<Dice> dice;

    public CantStopGameStateContainer(CantStopGameState gs) {
        super(gs);
        completedColumns = gs.completedColumns;
        playerMarkerPositions = gs.playerMarkerPositions;
        dice = gs.dice;
    }
}

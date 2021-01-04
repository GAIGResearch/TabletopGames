package games.santorini;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Component;
import core.components.GridBoard;
import core.interfaces.IPrintable;
import core.turnorders.AlternatingTurnOrder;
import games.santorini.components.PlayerPosition;
import games.santorini.components.SantoriniCell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SantoriniGameState extends AbstractGameState implements IPrintable {

    public GridBoard<SantoriniCell> grid;
    List<PlayerPosition> playerPositions;

    public SantoriniGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, new AlternatingTurnOrder(nPlayers));
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(grid);
            addAll(playerPositions);
         }};
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        SantoriniGameState sgs = new SantoriniGameState(gameParameters, getNPlayers());
        sgs.grid = grid.copy();
        for (int i=0; i<getNPlayers(); i++)
            sgs.playerPositions.set(i, (PlayerPosition) playerPositions.get(i).copy());
        return sgs;
    }

    @Override
    protected double _getScore(int playerId) {
        return new SantoriniHeuristic().evaluateState(this, playerId);
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId) {
        return new ArrayList<>();
    }

    @Override
    protected void _reset() {
        grid = null;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SantoriniGameState that = (SantoriniGameState) o;
        return Objects.equals(grid, that.grid) && Objects.equals(playerPositions, that.playerPositions);
    }

    @Override
    public void printToConsole() {
        System.out.println(grid);
    }
}

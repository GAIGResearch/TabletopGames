package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Settlement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SetupBuild extends AbstractAction implements IExtendedSequence {

    public final int player;
    public boolean builtSettlement;
    public boolean builtRoad;
    public int X;
    public int Y;

    public SetupBuild(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState state) {
        state.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState gs) {
        CatanGameState state = (CatanGameState) gs;
        List<AbstractAction> actions = new ArrayList<>();
        if (!builtSettlement) {
            CatanTile[][] board = state.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                        Settlement settlement = tile.getSettlements()[i];

                        // where it is legal to place tile then it can be placed from there
                        if (settlement.getOwner() == -1 &&
                                !(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT)))
                        {
                            if (state.checkSettlementPlacement(settlement, player)) {
                                actions.add(new BuildSettlement(x, y, i, getCurrentPlayer(state), true));
                            }
                        }
                    }
                }
            }
            return actions;
        } else {
            CatanTile tile = state.getBoard()[X][Y];
            for(int i = 0; i < CatanConstants.HEX_SIDES; i++){
                if(state.checkRoadPlacement(i,tile, player)){
                    actions.add(new BuildRoad(X, Y, i, getCurrentPlayer(state), true));
                }
            }
            return actions;
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof BuildSettlement){
            builtSettlement = true;
            BuildSettlement buildSettlement = (BuildSettlement) action;
            X = buildSettlement.x;
            Y = buildSettlement.y;
        }
        if (action instanceof BuildRoad){
            builtRoad = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return builtSettlement && builtRoad;
    }

    @Override
    public SetupBuild copy() {
        SetupBuild retValue = new SetupBuild(player);
        retValue.builtSettlement = this.builtSettlement;
        retValue.builtRoad = this.builtRoad;
        retValue.X = this.X;
        retValue.Y = this.Y;
        return retValue;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof SetupBuild){
            SetupBuild other = (SetupBuild) obj;
            return other.player == player && other.builtSettlement == builtSettlement && other.builtRoad == builtRoad && other.X == X && other.Y == Y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(builtSettlement, builtRoad, X, Y);
    }

    @Override
    public String toString(){
        return "Setup Build IExtendedSequence: BuiltSettlement =" + builtSettlement + "BuiltRoad =" + builtRoad + " X =" + X + " Y =" + Y;
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

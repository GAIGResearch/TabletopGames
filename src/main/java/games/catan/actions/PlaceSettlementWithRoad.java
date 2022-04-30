package games.catan.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.catan.CatanConstants.*;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class PlaceSettlementWithRoad extends AbstractAction {
    public final int x;
    public final int y;
    public final int i;
    public final int player;

    public PlaceSettlementWithRoad(int x, int y, int i, int player) {
        this.x = x;
        this.y = y;
        this.i = i;
        this.player = player;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        BuildSettlement buildSettlement  = new BuildSettlement(x,y,i,player,true);
        BuildRoad buildRoad = new BuildRoad(x,y,i,player,true);

        if (buildSettlement.execute(gs) && buildRoad.execute(gs)){
            // players get the resources in the second round after the settlements they placed
            if (gs.getTurnOrder().getRoundCounter() == 1){
                CatanGameState cgs = ((CatanGameState)gs);
                CatanTile[][] board = cgs.getBoard();
                // in the second round players get the resources from the the tiles around the settlement
                ArrayList<CatanTile> tiles = new ArrayList<CatanTile>();
                CatanTile tile = cgs.getBoard()[buildSettlement.x][buildSettlement.y];
                // next step is to find the tiles around the settlement
                int[][] neighbourCoords =  CatanTile.getNeighboursOnVertex(tile, buildSettlement.vertex);
                tiles.add(tile);
                tiles.add(board[neighbourCoords[0][0]][neighbourCoords[0][1]]);
                tiles.add(board[neighbourCoords[1][0]][neighbourCoords[1][1]]);

                // todo might not need the resources array
                int[] resources = new int[CatanParameters.Resources.values().length];
                Deck<Card> resourceDeck = (Deck<Card>) cgs.getComponent(resourceDeckHash);
                Deck<Card> playerHand = (Deck<Card>) cgs.getComponentActingPlayer(CoreConstants.playerHandHash);
                for (CatanTile t: tiles){
                    CatanParameters.Resources res = CatanParameters.productMapping.get(t.getType());
                    if (res!=null){
                        resources[CatanParameters.Resources.valueOf(res.toString()).ordinal()] += 1;
                        List<Card> cards = resourceDeck.getComponents();

                        for (int i = 0; i < cards.size(); i++){
                            Card c = cards.get(i);
                            if (c.getProperty(cardType).toString().equals(res.toString())){
                                resourceDeck.remove(c);
                                playerHand.add(c);
                                if(gs.getCoreGameParameters().verbose){
                                    System.out.println("At setup Player " + cgs.getCurrentPlayer() + " got " + c.getProperty(cardType));
                                }
                                break;
                            }
                        }
                    }
                }

            }
            return true;
        } else {
            throw new AssertionError("Could not execute chosen settlement and road build");
        }
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlaceSettlementWithRoad){
            PlaceSettlementWithRoad other = (PlaceSettlementWithRoad)obj;
            return other.x == x && other.y == y && other.i==i && other.player == player;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x,y,i,player);
    }

    @Override
    public String toString() {
        return String.format("PlaceSettlementWithRoad: x=%d y=%d i=%d player=%d",x,y,i,player);
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

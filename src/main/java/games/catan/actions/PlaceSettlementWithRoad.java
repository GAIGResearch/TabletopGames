package games.catan.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Deck;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.catan.CatanConstants.*;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class PlaceSettlementWithRoad extends AbstractAction {
    BuildSettlement bs;
    BuildRoad br;

    public PlaceSettlementWithRoad(BuildSettlement bs, BuildRoad br){
        this.bs = bs;
        this.br = br;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (this.bs.execute(gs) && this.br.execute(gs)){
            if (gs.getTurnOrder().getRoundCounter() == 1){
                CatanGameState cgs = ((CatanGameState)gs);
                CatanTile[][] board = cgs.getBoard();
                // in the second round players get the resources from the the tiles around the settlement
                ArrayList<CatanTile> tiles = new ArrayList<CatanTile>();
                CatanTile tile = cgs.getBoard()[bs.x][bs.y];
                // next step is to find the tiles around the settlement
                int[][] neighbourCoords =  CatanTile.get_neighbours_on_vertex(tile, bs.vertex);
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

                        // todo does not give all the resources
                        for (int i = 0; i < cards.size(); i++){
                            Card c = cards.get(i);
                            if (c.getProperty(cardType).toString().equals(res.toString())){
                                resourceDeck.remove(c);
                                playerHand.add(c);
                                System.out.println("At setup Player " + cgs.getCurrentPlayer() + " got " + c.getProperty(cardType));
                                break;
                            }
                        }
                    }
                }

            }
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof PlaceSettlementWithRoad){
            PlaceSettlementWithRoad otherAction = (PlaceSettlementWithRoad)other;
            return bs.equals(otherAction.bs) && br.equals(otherAction.br);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "PlaceSettlementWithRoad settlement = " + bs.toString() + " and road = " + br.toString();
    }
}

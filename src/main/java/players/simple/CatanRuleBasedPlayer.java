package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.catan.*;
import games.catan.actions.YearOfPlenty;
import games.catan.components.Settlement;


import java.util.*;

public class CatanRuleBasedPlayer extends AbstractPlayer {

    private List<int[]> desiredResources;
    private boolean roadBlocked = false;

    private final Random rnd; // random generator for selection of equally ranked actions

    // All current actions in the game, used for dynamically creating sub sets of the actions
    private enum ActionType {
            DoNothing,
            AcceptTrade,
            BuildCity,
            BuildRoad,
            BuildRoadByRef,
            BuildSettlement,
            BuildSettlementByRef,
            BuyDevelopmentCard,
            DefaultTrade,
            DiscardCards,
            Monopoly,
            MoveRobber,
            OfferPlayerTrade,
            PlaceRoad,
            PlaceSettlementWithRoad,
            PlayDevelopmentCard,
            PlayKnightCard,
            StealResource,
            YearOfPlenty,
    }


    public CatanRuleBasedPlayer(Random rnd) {this.rnd = rnd;}

    public CatanRuleBasedPlayer() {this(new Random());}

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        CatanGameState cgs = (CatanGameState) gameState;

        roadBlocked = checkIfRoadBlocked(cgs);

        List<List<AbstractAction>> actionLists = getActionSubsets(cgs , possibleActions);
        for(List<AbstractAction> actionList : actionLists){
            if (actionList.size()>0){
                int randomAction = rnd.nextInt(actionList.size());
                return actionList.get(randomAction);
            }
        }

        return null; // should never be reached

    }

    private boolean checkIfRoadBlocked(CatanGameState cgs){

        CatanTile[][] board = cgs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                    Settlement settlement = tile.getSettlements()[i];
                    // where it is legal to place tile then it can be placed from there
                    if (!(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))
                            && CatanActionFactory.checkSettlementPlacement(settlement, cgs)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private List<List<AbstractAction>> getActionSubsets(CatanGameState cgs, List<AbstractAction> possibleActions){
        List<List<AbstractAction>> actionLists = new ArrayList<>();
        actionLists.add(new ArrayList<>()); // highest priority actions
        actionLists.add(new ArrayList<>()); // 2nd priority actions
        actionLists.add(new ArrayList<>()); // 3rd priority actions
        actionLists.add(new ArrayList<>()); // 4th priority actions
        actionLists.add(new ArrayList<>()); // 5th priority actions
        actionLists.add(new ArrayList<>()); // 6th priority actions
        actionLists.add(new ArrayList<>()); // default case actions
        ActionType actionType = null;

        for(AbstractAction action : possibleActions){
            actionType = ActionType.valueOf(action.getClass().getSimpleName());
            switch (actionType) {
                case PlayKnightCard:
                    if(KnightCardCheck(cgs, action)){
                        actionLists.get(0).add(action);
                    }
                    break;

                case Monopoly:
                    if(MonopolyCardCheck(cgs, action)){
                        actionLists.get(2).add(action);
                    }
                    break;

                case YearOfPlenty:
                    if(YearOfPlentyCardCheck(cgs, action)==0){
                        actionLists.get(2).add(action);
                    } else if (YearOfPlentyCardCheck(cgs, action)==1)
                    {
                        actionLists.get(3).add(action);
                    } else if (YearOfPlentyCardCheck(cgs, action)==2)
                    {
                        actionLists.get(4).add(action);
                    }
                    break;

                case PlaceRoad:
                    if(placeRoadCheck(cgs, action)==0){
                        actionLists.get(1).add(action);
                    } else if(placeRoadCheck(cgs, action)==1){
                        actionLists.get(2).add(action);
                    } else if(placeRoadCheck(cgs, action)==2){
                        actionLists.get(3).add(action);
                    }
                    break;

                case BuildCity:
                    actionLists.get(0).add(action);
                    break;

                case BuildSettlement:
                    actionLists.get(1).add(action);
                    break;

                case BuyDevelopmentCard:
                    actionLists.get(4).add(action);
                    break;

                case BuildRoad:
                    if(buildRoadCheck(cgs, action)){
                        actionLists.get(5).add(action);
                    }
                    break;

                case DefaultTrade:
                    if(defaultTradeCheck(cgs, action)){
                        actionLists.get(4).add(action);
                    }
                    break;

                case OfferPlayerTrade:
                    if(offerPlayerTradeCheck(cgs, action)){
                        actionLists.get(5).add(action);
                    }
                    break;

                case MoveRobber:
                    if(moveRobberCheck(cgs,action)){
                        actionLists.get(0).add(action);
                    }

                case AcceptTrade:
                    if(acceptTradeCheck(cgs,action)){
                        actionLists.get(0).add(action);
                    }

                default:
                    actionLists.get(actionLists.size()-1).add(action);
            }
        }

        return actionLists;
    }

    private boolean KnightCardCheck(CatanGameState cgs, AbstractAction action){
        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
        for(Settlement settlement : robberTile.getSettlements()){
            if (settlement.getOwner()==getPlayerID()){
                return true;
            }
        }

        int[] knights = cgs.getKnights();
        int firstKnights = -1;
        int secondKnights = -1;
        for (int knightCount : knights){
            if (knightCount > firstKnights){
                secondKnights = firstKnights;
                firstKnights = knightCount;
            }
            else if (knightCount > secondKnights){
                secondKnights = knightCount;
            }
        }

        if (knights[getPlayerID()]==secondKnights&&firstKnights-secondKnights<2){
            return true;
        }

        return false;
    }

    private boolean MonopolyCardCheck(CatanGameState cgs, AbstractAction action){
        //TODO at a later date as complicated
        // Track rolls made against settlements owned to track players resources
        return rnd.nextInt(2)==0;
    }

    private int YearOfPlentyCardCheck(CatanGameState cgs, AbstractAction action){
        int[] resources = cgs.getPlayerResources(getPlayerID());
        YearOfPlenty yearOfPlenty = (YearOfPlenty) action;
        resources[yearOfPlenty.resource1.ordinal()] = resources[yearOfPlenty.resource1.ordinal()] +1;
        resources[yearOfPlenty.resource2.ordinal()] = resources[yearOfPlenty.resource2.ordinal()] +1;

        int[] costDiffs = costDifferenceCheck(resources);

        if (costDiffs[0]==0){
            return 0;
        } else if (costDiffs[1]==0){
            return 1;
        } else if (roadBlocked && costDiffs[2] == 0){
            return 2;
        }

        return -1;
    }

    private int placeRoadCheck(CatanGameState cgs, AbstractAction action){
        //TODO implement
        return rnd.nextInt(3);
    }

    private boolean buildRoadCheck(CatanGameState cgs, AbstractAction action){
        //TODO workout a way of deciding whether or not building a road is a good idea?
        return rnd.nextInt(2)==0;
    }

    private boolean defaultTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if default trade should be made
        return rnd.nextInt(2)==0;
    }

    private boolean offerPlayerTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if trade should be offered
        //TODO some state tracking for negotiations might be needed
        return rnd.nextInt(2)==0;
    }

    private boolean moveRobberCheck(CatanGameState cgs, AbstractAction action){
        //TODO identify good spots to move the robber
        return rnd.nextInt(2)==0;
    }

    private boolean acceptTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO identify whether or not to accept trade

        return rnd.nextInt(2)==0;
    }

    public String toString() { return "CatanRuleBased";}

    private int[] arraySubtraction(int[] arr1, int[] arr2){
        int[] arr3 = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++){
            arr3[i] = Math.max(0,arr1[i]-arr2[i]);
        }
        return arr3;
    }

    private int sumArray(int[] arr){
        int sum = 0;
        for (int i = 0; i < arr.length; i++){
            sum += arr[i];
        }
        return sum;
    }

    private int[] costDifferenceCheck(int[] resources){
        int[] cityCostDiff = new int[5], settlementCostDiff = new int[5], roadCostDiff = new int[5];
        for (int i = 0; i < resources.length; i++){
            cityCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("city")[i] - resources[i]);
            settlementCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("settlement")[i] - resources[i]);
            roadCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("road")[i] - resources[i]);
        }
        int[] costs = new int[3];
        for (int i = 0; i < resources.length; i++){
            costs[0] += cityCostDiff[i];
            costs[1] += settlementCostDiff[i];
            costs[2] += roadCostDiff[i];
        }
        return costs;
    }

}

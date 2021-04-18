package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.catan.*;
import games.catan.actions.*;
import games.catan.components.Settlement;


import java.util.*;

public class CatanRuleBasedPlayer extends AbstractPlayer {

    private int[][] resourcesRequiredToAffordCosts;
    private int[] currentResources;
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


    public CatanRuleBasedPlayer(Random rnd) {
        this.rnd = rnd;
        resourcesRequiredToAffordCosts = new int[4][5]; // 0: road, 1: settlement, 2: city, 3: development card
        currentResources = new int[5];
    }

    public CatanRuleBasedPlayer() {this(new Random());}

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        CatanGameState cgs = (CatanGameState) gameState;
        roadBlocked = checkIfRoadBlocked(cgs);
        this.currentResources = cgs.getPlayerResources(getPlayerID());
        calculateResourcesRequired();
        int[] tempResources = new int[5];
        ActionType actionType = null;
        CatanGameState.CatanGamePhase gamePhase = (CatanGameState.CatanGamePhase) cgs.getGamePhase();

        List<List<AbstractAction>> actionPriorityLists = new ArrayList<>();
        // Levels of priorities for actions
        for (int i = 0; i < 10; i++){
            actionPriorityLists.add(new ArrayList<>());
        }

        switch (gamePhase){
            case Trade:
                // If the player has the resources to get a city or settlement already don't do anything in the trade phase
                if(sumResources(resourcesRequiredToAffordCosts[1])==0 || sumResources(resourcesRequiredToAffordCosts[2])==0){
                    for (AbstractAction action : possibleActions) {
                        if (action.getClass().getSimpleName().equals("DoNothing")){
                            return action;
                        }
                    }
                }
                // Loop through and prioritise trade actions
                for (AbstractAction action : possibleActions) {
                    actionType = ActionType.valueOf(action.getClass().getSimpleName());
                    tempResources = currentResources;
                    switch (actionType) {
                        case DefaultTrade:
                            DefaultTrade defaultTrade = (DefaultTrade) action;
                            tempResources[defaultTrade.resourceToGet.ordinal()]+=1;
                            if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],tempResources)==0){
                                actionPriorityLists.get(0).add(action);
                            } else if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)==0){
                                actionPriorityLists.get(1).add(action);
                            } else if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[0],tempResources)==0 && roadBlocked){
                                actionPriorityLists.get(2).add(action);
                            }
                            break;
                        case OfferPlayerTrade:
                            OfferPlayerTrade offerPlayerTrade = (OfferPlayerTrade) action;
                            checkOfferPlayerTrade(tempResources, actionPriorityLists, action, offerPlayerTrade);
                            break;
                        default:
                            // add to lowest priority list as default
                            actionPriorityLists.get(actionPriorityLists.size()-1).add(action);
                    }
                }
                break;
            case Build:
                for (AbstractAction action : possibleActions) {
                    actionType = ActionType.valueOf(action.getClass().getSimpleName());
                    tempResources = currentResources;
                    switch (actionType){
                        case PlayKnightCard:
                            if(KnightCardCheck(cgs, action)){
                                actionPriorityLists.get(0).add(action);
                            }
                            break;
                        case BuildCity:
                            actionPriorityLists.get(1).add(action);
                            break;
                        case BuildSettlement:
                            actionPriorityLists.get(2).add(action);
                            break;
                        case PlaceRoad:
                            if(placeRoadCheck(cgs, action)){
                                actionPriorityLists.get(3).add(action);
                            }
                            break;
                        case Monopoly:
                            if(MonopolyCardCheck(cgs, action)){
                                actionPriorityLists.get(4).add(action);
                            }
                            break;
                        case BuildRoad:
                            if(roadBlocked){
                                if(buildRoadCheck(cgs, action)){
                                    actionPriorityLists.get(5).add(action);
                                }
                            } else {
                                if(buildRoadCheck(cgs, action)){
                                    actionPriorityLists.get(9).add(action);
                                }
                            }
                            break;
                        case YearOfPlenty:
                            int check = YearOfPlentyCardCheck(cgs, action);
                            if(check==0){
                                actionPriorityLists.get(6).add(action);
                            } else if (check==1)
                            {
                                actionPriorityLists.get(7).add(action);
                            } else if (check==2)
                            {
                                actionPriorityLists.get(8).add(action);
                            }
                            break;

                        default:
                            // add to lowest priority list as default
                            actionPriorityLists.get(actionPriorityLists.size()-1).add(action);
                    }
                }
                break;
            case Robber:
                CatanTile[][] board = cgs.getBoard();
                for(AbstractAction action : possibleActions){
                    MoveRobber moveRobber = (MoveRobber) action;
                    CatanTile tile = board[moveRobber.getXY()[0]][moveRobber.getXY()[1]];
                    if(tile.getType()==CatanParameters.TileType.DESERT){
                        actionPriorityLists.get(actionPriorityLists.size()-2).add(action);
                    } else {
                        int tileRank = 5;
                        Settlement[] settlements = tile.getSettlements();
                        for (Settlement settlement : settlements) {
                            if (settlement.getOwner() == getPlayerID()) {
                                actionPriorityLists.get(actionPriorityLists.size() - 1).add(action);
                                break;
                            }
                            if (settlement.getOwner() != -1) {
                                tileRank += 1;
                            }
                        }
                        actionPriorityLists.get(tileRank).add(action);
                    }
                }
                break;
            case TradeReaction:
                // If the player has the resources to get a city or settlement already don't do anything
                if(sumResources(resourcesRequiredToAffordCosts[1])==0
                        || sumResources(resourcesRequiredToAffordCosts[2])==0
                        || (roadBlocked && calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)==0))
                {
                    for (AbstractAction action : possibleActions) {
                        if (action.getClass().getSimpleName().equals("DoNothing")){
                            return action;
                        }
                    }
                }
                // Loop through and prioritise trade reaction actions
                for (AbstractAction action : possibleActions) {
                    actionType = ActionType.valueOf(action.getClass().getSimpleName());
                    tempResources = currentResources;
                    switch (actionType) {
                        case AcceptTrade:
                            AcceptTrade acceptTrade = (AcceptTrade) action;
                            for(int i = 0; i < tempResources.length; i++){
                                tempResources[i]= Math.max(0,(tempResources[i]+acceptTrade.getOfferedTrade().getResourcesOffered()[i]-acceptTrade.getOfferedTrade().getResourcesRequested()[i]));
                            }
                            if(!roadBlocked){
                                if( calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],tempResources)==0
                                        || calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)==0)
                                {
                                    actionPriorityLists.get(0).add(action);
                                } else if ( calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],tempResources)<calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],currentResources)
                                        || calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)<calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],currentResources))
                                {
                                    // if the trade helps player get closer to city or settlement then chance agent will accept
                                    if(rnd.nextBoolean()){
                                        actionPriorityLists.get(0).add(action);
                                    }
                                }
                            } else if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)==0 && sumResources(acceptTrade.getOfferedTrade().getResourcesRequested())==sumResources(acceptTrade.getOfferedTrade().getResourcesOffered())){
                                actionPriorityLists.get(0).add(action);
                            }
                            break;
                        case OfferPlayerTrade:
                            OfferPlayerTrade offerPlayerTrade = (OfferPlayerTrade) action;
                            checkOfferPlayerTrade(tempResources, actionPriorityLists, action, offerPlayerTrade);
                            break;
                        default:
                            // add to lowest priority list as default
                            actionPriorityLists.get(actionPriorityLists.size()-1).add(action);
                    }
                }
            default:
                // safety implementation to ensure that actions are taken when no rules are met
                int randomAction = rnd.nextInt(possibleActions.size());
                return possibleActions.get(randomAction);
        }

        for(List<AbstractAction> actionList : actionPriorityLists){
            if (actionList.size()>0){
                int randomAction = rnd.nextInt(actionList.size());
                return actionList.get(randomAction);
            }
        }

        return new DoNothing(); // should never be reached

    }

    private void checkOfferPlayerTrade(int[] tempResources, List<List<AbstractAction>> actionPriorityLists, AbstractAction action, OfferPlayerTrade offerPlayerTrade) {
        for(int i = 0; i < tempResources.length; i++){
            tempResources[i]= Math.max(0,(tempResources[i]+offerPlayerTrade.getResourcesRequested()[i]-offerPlayerTrade.getResourcesOffered()[i]));
        }
        if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],tempResources)==0){
            if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())*2){
                actionPriorityLists.get(3).add(action);
            }
            else if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())){
                actionPriorityLists.get(5).add(action);
            }
        }
        if(calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],tempResources)==0){
            if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())*2){
                actionPriorityLists.get(4).add(action);
            }
            else if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())){
                actionPriorityLists.get(6).add(action);
            }
        }
    }

    private int sumResources(int[] resources){
        int sum = 0;
        for(int i = 0; i < resources.length; i++){
            sum += resources[i];
        }
        return sum;
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
        int[] resources = currentResources;
        YearOfPlenty yearOfPlenty = (YearOfPlenty) action;
        resources[yearOfPlenty.resource1.ordinal()] = resources[yearOfPlenty.resource1.ordinal()] +1;
        resources[yearOfPlenty.resource2.ordinal()] = resources[yearOfPlenty.resource2.ordinal()] +1;

        int[] costDiffs = new int[3];
        costDiffs[0] = calculateTotalResourceDifference(resourcesRequiredToAffordCosts[0],resources);
        costDiffs[1] = calculateTotalResourceDifference(resourcesRequiredToAffordCosts[1],resources);
        costDiffs[2] = calculateTotalResourceDifference(resourcesRequiredToAffordCosts[2],resources);

        if (roadBlocked && costDiffs[0] == 0){
            return 2;
        } else if (costDiffs[1]==0){
            return 1;
        } else if (costDiffs[2]==0){
            return 0;
        }

        return -1;
    }

    private boolean placeRoadCheck(CatanGameState cgs, AbstractAction action){
        //TODO implement
        return true;
    }

    private boolean buildRoadCheck(CatanGameState cgs, AbstractAction action){
        //TODO workout a way of deciding whether or not building a road is a good idea?
        return rnd.nextInt(4)>0;
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

    private void calculateResourcesRequired(){
        int[] cityCostDiff = new int[5], settlementCostDiff = new int[5], roadCostDiff = new int[5], developmentCardDiff = new int[5];
        for (int i = 0; i < currentResources.length; i++){
            cityCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("city")[i] - currentResources[i]);
            settlementCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("settlement")[i] - currentResources[i]);
            roadCostDiff[i] = Math.max(0,CatanParameters.costMapping.get("road")[i] - currentResources[i]);
            developmentCardDiff[i] = Math.max(0,CatanParameters.costMapping.get("developmentCard")[i] - currentResources[i]);
        }
        for (int i = 0; i < currentResources.length; i++){
            resourcesRequiredToAffordCosts[0] = roadCostDiff;
            resourcesRequiredToAffordCosts[1] = settlementCostDiff;
            resourcesRequiredToAffordCosts[2] = cityCostDiff;
            resourcesRequiredToAffordCosts[3] = developmentCardDiff;
        }
    }

    private int calculateTotalResourceDifference(int[] resources1, int[] resources2){
        return sumArray(arraySubtraction(resources1,resources2));
    }

}

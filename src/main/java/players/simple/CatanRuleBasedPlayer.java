//package players.simple;
//
//import core.AbstractGameState;
//import core.AbstractPlayer;
//import core.actions.AbstractAction;
//import core.actions.DoNothing;
//import core.components.Counter;
//import games.catan.*;
//import games.catan.actions.*;
//import games.catan.components.CatanTile;
//import games.catan.components.Building;
//
//
//import java.util.*;
//
//public class CatanRuleBasedPlayer extends AbstractPlayer {
//
//    private int[][] resourcesRequiredToAffordCosts;
//    private HashMap<CatanParameters.Resource, Counter> currentResources;
//    private boolean roadBlocked = false;
//
//    private final Random rnd; // random generator for selection of equally ranked actions
//    private final CatanParameters params;
//
//    // All current actions in the game, used for dynamically creating sub sets of the actions
//    private enum ActionType {
//            DoNothing,
//            AcceptTrade,
//            BuildCity,
//            BuildRoad,
//            BuildRoadByRef,
//            BuildSettlement,
//            BuildSettlementByRef,
//            BuyDevelopmentCard,
//            DefaultTrade,
//            DiscardCards,
//            Monopoly,
//            MoveRobber,
//            OfferPlayerTrade,
//            PlaceRoad,
//            PlaceSettlementWithRoad,
//            PlayDevelopmentCard,
//            PlayKnightCard,
//            StealResource,
//            YearOfPlenty,
//    }
//
//
//    public CatanRuleBasedPlayer(CatanParameters params, Random rnd) {
//        this.rnd = rnd;
//        this.params = params;
//        resourcesRequiredToAffordCosts = new int[4][5]; // 0: road, 1: settlement, 2: city, 3: development card
//    }
//
//    public CatanRuleBasedPlayer() {this(new CatanParameters(0), new Random());}
//
//    @Override
//    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
//        CatanGameState cgs = (CatanGameState) gameState;
//        roadBlocked = checkIfRoadBlocked(cgs);
//        this.currentResources = cgs.getPlayerResources(getPlayerID());
//        calculateResourcesRequired();
//        HashMap<CatanParameters.Resource, Counter> tempResources;
//        ActionType actionType;
//        CatanGameState.CatanGamePhase gamePhase = (CatanGameState.CatanGamePhase) cgs.getGamePhase();
//
//        List<List<AbstractAction>> actionPriorityLists = new ArrayList<>();
//        // Levels of priorities for actions
//        for (int i = 0; i < 11; i++){
//            actionPriorityLists.add(new ArrayList<>());
//        }
//
//        switch (gamePhase){
//            case Setup:
//                return possibleActions.get(rnd.nextInt(possibleActions.size()));
//            case Trade:
//                // If the player has the resources to get a city/settlement/road already don't do anything in the trade phase
//                if((!roadBlocked
//                        && (sumArray(resourcesRequiredToAffordCosts[1])==0
//                            || sumArray(resourcesRequiredToAffordCosts[2])==0))
//                    || roadBlocked
//                        && sumArray(resourcesRequiredToAffordCosts[0])==0)
//                {
//                    for (AbstractAction action : possibleActions) {
//                        if (action instanceof DoNothing || action instanceof EndNegotiation) {
//                            return action;
//                        }
//                    }
//                }
//                // Loop through and prioritise trade actions
//                for (AbstractAction action : possibleActions) {
//                    actionType = ActionType.valueOf(action.getClass().getSimpleName());
//                    tempResources = currentResources.clone();
//                    switch (actionType) {
//                        case DefaultTrade:
//                            DefaultTrade defaultTrade = (DefaultTrade) action;
//                            tempResources[defaultTrade.resourceToGet.ordinal()]+=1;
//                            tempResources[defaultTrade.resourceOffer.ordinal()]-=4;
//                            if(roadBlocked){
//                                if(calculateTotalResourceDifference(resourceCosts[0],tempResources)==0)
//                                {
//                                    actionPriorityLists.get(0).add(action);
//                                } else if(calculateTotalResourceDifference(resourceCosts[3],tempResources)==0
//                                        && (calculateTotalResourceDifference(resourceCosts[3],tempResources)<calculateTotalResourceDifference(resourceCosts[3],currentResources)))
//                                {
//                                    actionPriorityLists.get(1).add(action);
//                                } else if (calculateTotalResourceDifference(resourceCosts[0],tempResources)<calculateTotalResourceDifference(resourceCosts[0],currentResources)
//                                            || calculateTotalResourceDifference(resourceCosts[3],tempResources)<calculateTotalResourceDifference(resourceCosts[3],currentResources)
//                                        && (calculateTotalResourceDifference(resourceCosts[3],tempResources)!=0
//                                            && calculateTotalResourceDifference(resourceCosts[3],currentResources) != 0))
//                                {
//                                    actionPriorityLists.get(2).add(action);
//                                }
//                            }
//                            else if(calculateTotalResourceDifference(resourceCosts[2],tempResources)==0
//                                    || (calculateTotalResourceDifference(resourceCosts[2],tempResources)<calculateTotalResourceDifference(resourceCosts[2],currentResources)
//                                        && sumArray(currentResources)>6) )
//                            {
//                                actionPriorityLists.get(3).add(action);
//                            } else if(calculateTotalResourceDifference(resourceCosts[1],tempResources)==0
//                                    || (calculateTotalResourceDifference(resourceCosts[1],tempResources)<calculateTotalResourceDifference(resourceCosts[1],currentResources)
//                                        && sumArray(currentResources)>6) )
//                            {
//                                actionPriorityLists.get(4).add(action);
//                            } else if(calculateTotalResourceDifference(resourceCosts[0],tempResources)==0
//                                    || (calculateTotalResourceDifference(resourceCosts[0],tempResources)<calculateTotalResourceDifference(resourceCosts[0],currentResources)
//                                        && sumArray(currentResources)>6) )
//                            {
//                                actionPriorityLists.get(5).add(action);
//                            }
//                            break;
//                        case OfferPlayerTrade:
//                            OfferPlayerTrade offerPlayerTrade = (OfferPlayerTrade) action;
//                            checkOfferPlayerTrade(tempResources, actionPriorityLists, action, offerPlayerTrade);
//                            break;
//                        default:
//                            // add to lowest priority list as default
//                            actionPriorityLists.get(actionPriorityLists.size()-1).add(action);
//                    }
//                }
//                break;
//            case Build:
//                for (AbstractAction action : possibleActions) {
//                    actionType = ActionType.valueOf(action.getClass().getSimpleName());
//                    switch (actionType){
//                        case PlayKnightCard:
//                            if(KnightCardCheck(cgs, action)){
//                                actionPriorityLists.get(0).add(action);
//                            }
//                            break;
//                        case BuildCity:
//                            actionPriorityLists.get(1).add(action);
//                            break;
//                        case BuildSettlement:
//                            actionPriorityLists.get(2).add(action);
//                            break;
//                        case PlaceRoad:
//                            if(placeRoadCheck(cgs, action)){
//                                actionPriorityLists.get(3).add(action);
//                            }
//                            break;
//                        case Monopoly:
//                            if(MonopolyCardCheck(cgs, action)){
//                                actionPriorityLists.get(4).add(action);
//                            }
//                            break;
//                        case BuildRoad:
//                            if(roadBlocked){
//                                actionPriorityLists.get(5).add(action);
//                            } else if (buildRoadCheck(cgs, action) && cgs.getLongestRoadOwner()!= getPlayerID()) {
//                                actionPriorityLists.get(10).add(action);
//                            } else if(sumArray(currentResources)>7){
//                                actionPriorityLists.get(10).add(action);
//                            }
//                            break;
//                        case YearOfPlenty:
//                            int check = YearOfPlentyCardCheck(cgs, action);
//                            if(check==0){
//                                actionPriorityLists.get(6).add(action);
//                            } else if (check==1)
//                            {
//                                actionPriorityLists.get(7).add(action);
//                            } else if (check==2)
//                            {
//                                actionPriorityLists.get(8).add(action);
//                            }
//                            break;
//                        case BuyDevelopmentCard:
//                            if (cgs.getPlayerDevCards(getPlayerID()).length<1
//                                    || sumArray(currentResources)>7
//                                    || (roadBlocked && sumArray(resourcesRequiredToAffordCosts[0])>1)){
//
//                                actionPriorityLists.get(9).add(action);
//                            }
//
//                        default:
//                            // add to lowest priority list as default
//                            actionPriorityLists.get(actionPriorityLists.size()-1).add(action);
//                    }
//                }
//                break;
//            case Robber:
//                CatanTile[][] board = cgs.getBoard();
//                for(AbstractAction action : possibleActions){
//                    MoveRobber moveRobber = (MoveRobber) action;
//                    CatanTile tile = board[moveRobber.getXY()[0]][moveRobber.getXY()[1]];
//                    if(tile.getTileType()== CatanTile.TileType.DESERT){
//                        actionPriorityLists.get(actionPriorityLists.size()-2).add(action);
//                    } else {
//                        int tileRank = 5;
//                        Building[] settlements = tile.getSettlements();
//                        for (Building settlement : settlements) {
//                            if (settlement.getOwner() == getPlayerID()) {
//                                actionPriorityLists.get(actionPriorityLists.size() - 1).add(action);
//                                break;
//                            }
//                            if (settlement.getOwner() != -1) {
//                                tileRank += 1;
//                            }
//                        }
//                        actionPriorityLists.get(tileRank).add(action);
//                    }
//                }
//                break;
//            case Discard:
//                for (AbstractAction action : possibleActions) {
//                    if (action.getClass().getSimpleName().equals("DoNothing")){
//                        return action;
//                    }
//                    tempResources = currentResources.clone();
//                    DiscardCards discardCards = (DiscardCards) action;
//                    CatanParameters.Resource[] cardsToDiscard = discardCards.getToBeDiscarded();
//                    int[] discardValues = new int[5];
//                    for(CatanParameters.Resource card : cardsToDiscard){
//                        if (card.equals(CatanParameters.Resource.BRICK)){
//                            discardValues[0]+=1;
//                        } else if (card.equals(CatanParameters.Resource.LUMBER)){
//                            discardValues[1]+=1;
//                        } else if (card.toString().equals(CatanParameters.Resource.ORE)){
//                            discardValues[2]+=1;
//                        } else if (card.equals(CatanParameters.Resource.GRAIN)){
//                            discardValues[3]+=1;
//                        } else if (card.equals(CatanParameters.Resource.WOOL)){
//                            discardValues[4]+=1;
//                        }
//                    }
//                    tempResources = arraySubtraction(tempResources,discardValues);
//                    if(calculateTotalResourceDifference(resourceCosts[2],tempResources)==calculateTotalResourceDifference(resourceCosts[2],currentResources)){
//                        actionPriorityLists.get(0).add(action);
//                    } else if (calculateTotalResourceDifference(resourceCosts[2],tempResources)==calculateTotalResourceDifference(resourceCosts[2],currentResources)+1){
//                        actionPriorityLists.get(1).add(action);
//                    } else if(calculateTotalResourceDifference(resourceCosts[1],tempResources)==calculateTotalResourceDifference(resourceCosts[1],currentResources)){
//                        actionPriorityLists.get(2).add(action);
//                    } else if (calculateTotalResourceDifference(resourceCosts[1],tempResources)==calculateTotalResourceDifference(resourceCosts[1],currentResources)+1){
//                        actionPriorityLists.get(3).add(action);
//                    } else {
//                        actionPriorityLists.get(4).add(action);
//                    }
//                }
//                break;
//            case Steal:
//                return possibleActions.get(rnd.nextInt(possibleActions.size()));
//            default:
//                // safety implementation to ensure that actions are taken when no rules are met
//                return possibleActions.get(rnd.nextInt(possibleActions.size()));
//        }
//
//        for(List<AbstractAction> actionList : actionPriorityLists){
//            if (actionList.size()>0){
//                return actionList.get(rnd.nextInt(actionList.size()));
//            }
//        }
//
//        System.out.println("Error: Catan Rule Based Agent did not select an action!");
//        return possibleActions.get(rnd.nextInt(possibleActions.size())); // should never be reached
//
//    }
//
//    private void checkOfferPlayerTrade(int[] tempResources, List<List<AbstractAction>> actionPriorityLists, AbstractAction action, OfferPlayerTrade offerPlayerTrade) {
//        for(int i = 0; i < tempResources.length; i++){
//            tempResources[i]= Math.max(0,(tempResources[i]+offerPlayerTrade.getResourcesRequested()[i]-offerPlayerTrade.getResourcesOffered()[i]));
//        }
//        if(roadBlocked && calculateTotalResourceDifference(resourceCosts[0],tempResources)<calculateTotalResourceDifference(resourceCosts[0],currentResources)){
//            actionPriorityLists.get(6).add(action);
//        }
//        if(calculateTotalResourceDifference(resourceCosts[2],tempResources)==0){
//            if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())*2){
//                actionPriorityLists.get(7).add(action);
//            }
//            else if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())){
//                actionPriorityLists.get(9).add(action);
//            }
//        }
//        if(calculateTotalResourceDifference(resourceCosts[1],tempResources)==0){
//            if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())*2){
//                actionPriorityLists.get(8).add(action);
//            }
//            else if(sumArray(offerPlayerTrade.getResourcesOffered())>=sumArray(offerPlayerTrade.getResourcesRequested())){
//                actionPriorityLists.get(10).add(action);
//            }
//        }
//    }
//
//    private boolean checkIfRoadBlocked(CatanGameState cgs){
//
//        CatanTile[][] board = cgs.getBoard();
//        for (int x = 0; x < board.length; x++) {
//            for (int y = 0; y < board[x].length; y++) {
//                CatanTile tile = board[x][y];
//                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
//                    Building settlement = tile.getSettlements()[i];
//                    // where it is legal to place tile then it can be placed from there
//                    if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
//                            && cgs.checkSettlementPlacement(settlement, getPlayerID())) {
//                        return false;
//                    }
//                }
//            }
//        }
//
//        return true;
//    }
//
//
//    private boolean KnightCardCheck(CatanGameState cgs, AbstractAction action){
//        CatanTile robberTile = cgs.getRobber(cgs.getBoard());
//        for(Building settlement : robberTile.getSettlements()){
//            if (settlement.getOwner()==getPlayerID()){
//                return true;
//            }
//        }
//
//        int[] knights = cgs.getKnights();
//        int firstKnights = -1;
//        int secondKnights = -1;
//        for (int knightCount : knights){
//            if (knightCount > firstKnights){
//                secondKnights = firstKnights;
//                firstKnights = knightCount;
//            }
//            else if (knightCount > secondKnights){
//                secondKnights = knightCount;
//            }
//        }
//
//        if (knights[getPlayerID()]==secondKnights&&firstKnights-secondKnights<2){
//            return true;
//        }
//
//        return false;
//    }
//
//    private boolean MonopolyCardCheck(CatanGameState cgs, AbstractAction action){
//        //TODO at a later date as complicated
//        // Track rolls made against settlements owned to track players resources
//        return rnd.nextInt(2)==0;
//    }
//
//    private int YearOfPlentyCardCheck(CatanGameState cgs, AbstractAction action){
//        int[] resources = currentResources;
//        YearOfPlenty yearOfPlenty = (YearOfPlenty) action;
//        resources[yearOfPlenty.resource1.ordinal()] = resources[yearOfPlenty.resource1.ordinal()] +1;
//        resources[yearOfPlenty.resource2.ordinal()] = resources[yearOfPlenty.resource2.ordinal()] +1;
//
//        int[] costDiffs = new int[3];
//        costDiffs[0] = calculateTotalResourceDifference(resourceCosts[0],resources);
//        costDiffs[1] = calculateTotalResourceDifference(resourceCosts[1],resources);
//        costDiffs[2] = calculateTotalResourceDifference(resourceCosts[2],resources);
//
//        if (roadBlocked && costDiffs[0] == 0){
//            return 2;
//        } else if (costDiffs[1]==0){
//            return 1;
//        } else if (costDiffs[2]==0){
//            return 0;
//        }
//
//        return -1;
//    }
//
//    private boolean placeRoadCheck(CatanGameState cgs, AbstractAction action){
//        return true;
//    }
//
//    private boolean buildRoadCheck(CatanGameState cgs, AbstractAction action){
//            return rnd.nextInt(4)>0;
//    }
//
//    public String toString() { return "CatanRuleBased";}
//
//    private int[] arraySubtraction(int[] arr1, int[] arr2){
//        int[] arr3 = new int[arr1.length];
//        for (int i = 0; i < arr1.length; i++){
//            arr3[i] = Math.max(0,arr1[i]-arr2[i]);
//        }
//        return arr3;
//    }
//
//    private int sumArray(int[] arr){
//        int sum = 0;
//        for (int i = 0; i < arr.length; i++){
//            sum += arr[i];
//        }
//        return sum;
//    }
//
//    private void calculateResourcesRequired(){
//        int[][] costDiff = new int[4][5];
//        for (int i = 0; i < currentResources.length; i++){
//            costDiff[0][i] = Math.max(0,resourceCosts[0][i] - currentResources[i]);
//            costDiff[1][i] = Math.max(0,resourceCosts[1][i] - currentResources[i]);
//            costDiff[2][i] = Math.max(0,resourceCosts[2][i] - currentResources[i]);
//            costDiff[3][i] = Math.max(0,resourceCosts[3][i] - currentResources[i]);
//        }
//        for (int i = 0; i < currentResources.length; i++){
//            resourcesRequiredToAffordCosts[0] = costDiff[0];
//            resourcesRequiredToAffordCosts[1] = costDiff[1];
//            resourcesRequiredToAffordCosts[2] = costDiff[2];
//            resourcesRequiredToAffordCosts[3] = costDiff[3];
//        }
//    }
//
//    private int calculateTotalResourceDifference(int[] resources1, int[] resources2){
//        return sumArray(arraySubtraction(resources1,resources2));
//    }
//
//    @Override
//    public AbstractPlayer copy() {
//        // todo
//        return this;
//    }
//}

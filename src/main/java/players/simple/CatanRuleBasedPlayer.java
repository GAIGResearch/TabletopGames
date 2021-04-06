package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.catan.CatanGameState;
import games.catan.actions.AcceptTrade;
import games.catan.actions.OfferPlayerTrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanRuleBasedPlayer extends AbstractPlayer {


    private final Random rnd; // random generator for selection of equally ranked actions

    // All current actions in the game, used for dynamically creating sub sets of the actions
    private enum ActionType {
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

        List<List<AbstractAction>> actionLists = getActionSubsets((CatanGameState) gameState, possibleActions);
        for(List<AbstractAction> actionList : actionLists){
            if (actionList.size()>0){
                return selectRandomAction(actionList);
            }
        }

        return null; // should never be reached

    }

    public String toString() { return "CatanRuleBased";}

    private AbstractAction selectRandomAction(List<AbstractAction> actions){
        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    private List<List<AbstractAction>> getActionSubsets(CatanGameState cgs, List<AbstractAction> possibleActions){
        List<List<AbstractAction>> actionLists = new ArrayList<>();
        actionLists.add(new ArrayList<>()); // highest priority actions - Play development cards
        actionLists.add(new ArrayList<>()); // 2nd priority actions
        actionLists.add(new ArrayList<>()); // 3rd priority actions
        actionLists.add(new ArrayList<>()); // 4th priority actions
        actionLists.add(new ArrayList<>()); // 5th priority actions
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
                        actionLists.get(0).add(action);
                    }
                    break;

                case YearOfPlenty:
                    if(YearOfPlentyCardCheck(cgs, action)){
                        actionLists.get(0).add(action);
                    }
                    break;

                case PlaceRoad:
                    //TODO currently bugged in game
                    break;

                case BuildCity:
                    actionLists.get(1).add(action);
                    break;

                case BuildSettlement:
                    actionLists.get(2).add(action);
                    break;

                case BuyDevelopmentCard:
                    actionLists.get(3).add(action);
                    break;

                case BuildRoad:
                    if(BuildRoadCheck(cgs, action)){
                        actionLists.get(4).add(action);
                    }
                    break;

                case DefaultTrade:
                    if(DefaultTradeCheck(cgs, action)){
                        actionLists.get(2).add(action);
                    }
                    break;

                case OfferPlayerTrade:
                    if(OfferPlayerTradeCheck(cgs, action)){
                        actionLists.get(3).add(action);
                    }
                    break;

                case MoveRobber:
                    if(MoveRobberCheck(cgs,action)){
                        actionLists.get(0).add(action);
                    }

                case AcceptTrade:
                    if(AcceptTradeCheck(cgs,action)){
                        actionLists.get(0).add(action);
                    }


                default:
                    actionLists.get(actionLists.size()-1).add(action);
            }
        }

        return actionLists;
    }

    private boolean KnightCardCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if knight card should be played
        return rnd.nextInt(2)==0;
    }

    private boolean MonopolyCardCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if monopoly card should be played
        return rnd.nextInt(2)==0;
    }

    private boolean YearOfPlentyCardCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if Year of Plenty card should be played
        return rnd.nextInt(2)==0;
    }

    private boolean BuildRoadCheck(CatanGameState cgs, AbstractAction action){
        //TODO workout a way of deciding whether or not building a road is a good idea?
        return rnd.nextInt(2)==0;
    }


    private boolean DefaultTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if default trade should be made
        return rnd.nextInt(2)==0;
    }

    private boolean OfferPlayerTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO check if trade should be offered
        //TODO some state tracking for negotiations might be needed
        return rnd.nextInt(2)==0;
    }

    private boolean MoveRobberCheck(CatanGameState cgs, AbstractAction action){
        //TODO identify good spots to move the robber
        return rnd.nextInt(2)==0;
    }

    private boolean AcceptTradeCheck(CatanGameState cgs, AbstractAction action){
        //TODO identify good spots to move the robber
        return rnd.nextInt(2)==0;
    }
}

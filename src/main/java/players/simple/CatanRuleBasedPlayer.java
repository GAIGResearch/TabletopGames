package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.catan.CatanGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanRuleBasedPlayer extends AbstractPlayer {


    private final Random rnd; // random generator for selection of equally ranked actions

    // All current actions in the game, used for dynamically creating sub sets of the actions
    //TODO : This can be done using isInstance for a more resilient system but that introduces the
    // overheads of creating a new object to check against each time, not sure if it is worth it
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

    /**
     *
     * @param gameState observation of the current game state
     * @param possibleActions
     * @return AbstractAction
     */
    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {

        List<List<AbstractAction>> actionLists = getActionSubsets((CatanGameState) gameState, possibleActions);

        // DEVELOPMENT CARDS
        // Design a good scenario for when each development card should be played
        // If the scenario is met, play the card

        // TRADE
        // first check if there are any trades available (need resources)
        // identify which resource to request would be the best based on how close to building something
        // randomly select a trade from this list

        // BUY
        // build city
        // build settlement
        // buy development card
        // build road

        // TRADE RESPONSE
        // Identify if the offered resources is wanted
        // If not, reject
        // If yes but offer is bad, renegotiate
        // If yes and offer is good, accept

        // ROBBER
        // Only place robber on tiles with settlements

        // random select from all actions if all rules fail

        return selectRandomAction(possibleActions);
    }

    public String toString() { return "CatanRuleBased";}

    private AbstractAction selectRandomAction(List<AbstractAction> actions){
        int randomAction = rnd.nextInt(actions.size());
        return actions.get(randomAction);
    }

    private List<List<AbstractAction>> getActionSubsets(CatanGameState cgs, List<AbstractAction> possibleActions){
        List<List<AbstractAction>> actionLists = new ArrayList<>();
        actionLists.add(new ArrayList<>()); // build city actions
        actionLists.add(new ArrayList<>()); // build settlement actions
        actionLists.add(new ArrayList<>()); // buy development card actions
        actionLists.add(new ArrayList<>()); // build road actions
        actionLists.add(new ArrayList<>()); // default case actions
        ActionType actionType = null;

        for(AbstractAction action : possibleActions){
            actionType = ActionType.valueOf(action.getClass().getSimpleName());
            switch (actionType) {
                case BuildCity:
                    actionLists.get(0).add(action);
                    break;

                case BuildSettlement:
                    actionLists.get(1).add(action);
                    break;

                case BuyDevelopmentCard:
                    actionLists.get(2).add(action);
                    break;

                case BuildRoad:
                    if(BuildRoadCheck(action)){
                        actionLists.get(3).add(action);
                    }
                    break;

                default:
                    actionLists.get(actionLists.size()-1).add(action);
            }
        }

        return actionLists;
    }

    private boolean BuildRoadCheck(AbstractAction action){
        //TODO workout a way of deciding whether or not building a road is a good idea?
        return rnd.nextInt(2)==0;
    }
}

package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanRuleBasedPlayer extends AbstractPlayer {


    private final Random rnd; // random generator for selection of equally ranked actions

    public CatanRuleBasedPlayer(Random rnd) {this.rnd = rnd;}

    public CatanRuleBasedPlayer() {this(new Random());}

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        // list of actions, need to select one from the list
        // check for each available actions, add possible ones to list to select from
        // actions are either going to be trade/buy or play a dev card
        List<AbstractAction> actionsToSelectFrom = new ArrayList<>();

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
        if (actionsToSelectFrom.size() == 0){
            actionsToSelectFrom = possibleActions;
        }

        int randomAction = rnd.nextInt(possibleActions.size());
        return actionsToSelectFrom.get(randomAction);
    }

    public String toString() { return "CatanRuleBased";}
}

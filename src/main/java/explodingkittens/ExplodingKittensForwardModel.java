package explodingkittens;

import actions.*;
import core.ForwardModel;
import core.Game;
import core.GameState;
import explodingkittens.actions.IsNope;
import explodingkittens.actions.IsNopeable;

import java.util.Iterator;
import java.util.Stack;

public class ExplodingKittensForwardModel implements ForwardModel {

    private Game game;
    private Stack<Action> actionStack = new Stack<>();
    private int requiredPasses;
    private int initialPlayer;

    @Override
    public void setup(GameState firstState, Game game) {
    }

    @Override
    public void next(GameState currentState, Action action) {
        System.out.println(action.toString());

        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) currentState;
        if (actionStack.size() == 0){
            if (action instanceof IsNopeable){
                actionStack.add(action);
                ekgs.gamePhase = ExplodingKittensGamePhase.NopePhase;
                requiredPasses = ekgs.nPlayersActive-1;
                initialPlayer = ekgs.getActivePlayer();
                ekgs.setActivePlayer(ekgs.nextPlayer(ekgs.getActivePlayer()));
            } else {
                action.execute(currentState);
            }
        } else {
            // action is either nope or pass
            if (((IsNope) action).isNope()){
                actionStack.add(action);
                requiredPasses = ekgs.nPlayersActive-1;
                ekgs.setActivePlayer(ekgs.nextPlayer(ekgs.getActivePlayer()));
            } else {
                requiredPasses -= 1;
                if (requiredPasses == 0){
                    ekgs.setActivePlayer(initialPlayer);
                    // apply stack
                    if (actionStack.size()%2 == 0){
                        while (actionStack.size() > 1)
                            actionStack.pop().execute(currentState);
                            //Action was successfully noped
                        ((IsNopeable) actionStack.pop()).nopedExecute(currentState);
                        System.out.println("Action was successfully noped");
                        actionStack.clear();
                    } else {
                        while (actionStack.size() > 1)
                            actionStack.pop().execute(currentState);

                        //Action can be played
                        Action stackedAction = actionStack.get(0);
                        stackedAction.execute(currentState);
                        actionStack.clear();
                    }
                    if (ekgs.gamePhase == ExplodingKittensGamePhase.NopePhase)
                        ekgs.gamePhase = ExplodingKittensGamePhase.PlayerMove;
                } else {
                    ekgs.setActivePlayer(ekgs.nextPlayer(ekgs.getActivePlayer()));
                }
            }
        }
    }

    private void printActionStack(){
        System.out.print("Action Stack:");
        for (Iterator<Action> it = actionStack.iterator(); it.hasNext(); ) {
            Action a = it.next();
            System.out.print(a.toString() + ",");
        }
    }
}

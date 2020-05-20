package games.explodingkittens;

import core.actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;
import games.explodingkittens.actions.IsNope;
import games.explodingkittens.actions.IsNopeable;

import java.util.Stack;

import static games.explodingkittens.ExplodingKittensGameState.GamePhase.NopePhase;
import static games.explodingkittens.ExplodingKittensGameState.GamePhase.PlayerMove;


public class ExplodingKittensForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();

    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        System.out.println(action.toString());
        ExplodingKittenTurnOrder ekTurnOrder = (ExplodingKittenTurnOrder) gameState.getTurnOrder();
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;

        if (actionStack.size() == 0){
            if (action instanceof IsNopeable){
                actionStack.add(action);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
            } else {
                action.execute(gameState);
            }
        } else {
            // action is either nope or pass
            if (((IsNope) action).isNope()){
                actionStack.add(action);
                action.execute(gameState);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
            } else {
                ekTurnOrder.endPlayerTurnStep(gameState);

                if (ekTurnOrder.reactionsFinished()){
                    // apply stack
                    if (actionStack.size()%2 == 0){
                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);
                            //Action was successfully noped
                        ((IsNopeable) actionStack.pop()).nopedExecute(gameState, ekTurnOrder);
                        System.out.println("Action was successfully noped");
                    } else {
                        if (actionStack.size() > 2)
                            System.out.println("All nopes were noped");

                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);

                        //Action can be played
                        IAction stackedAction = actionStack.get(0);
                        stackedAction.execute(gameState);
                    }
                    actionStack.clear();
                    if (ekgs.getGamePhase() == NopePhase)
                        ekgs.setGamePhase(PlayerMove);
                }
            }
        }
    }

    private void printActionStack(){
        System.out.print("Action Stack:");
        for (IAction a : actionStack) {
            System.out.print(a.toString() + ",");
        }
    }
}

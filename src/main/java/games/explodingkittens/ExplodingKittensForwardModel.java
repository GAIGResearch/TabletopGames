package games.explodingkittens;

import actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;
import games.explodingkittens.actions.IsNope;
import games.explodingkittens.actions.IsNopeable;
import turnorder.TurnOrder;

import java.util.Stack;


public class ExplodingKittensForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();
    private int initialPlayer = -1;

    @Override
    public void next(AbstractGameState gameState, TurnOrder turnOrder, IAction action) {
        System.out.println(action.toString());
        ExplodingKittenTurnOrder ekTurnOrder = (ExplodingKittenTurnOrder) turnOrder;
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;

        if (actionStack.size() == 0){
            if (action instanceof IsNopeable){
                actionStack.add(action);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
                initialPlayer = ((ExplodingKittenTurnOrder) turnOrder).currentPlayer;
            } else {
                action.Execute(gameState, turnOrder);
            }
        } else {
            // action is either nope or pass
            if (((IsNope) action).isNope()){
                actionStack.add(action);
                action.Execute(gameState, turnOrder);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
            } else {
                turnOrder.endPlayerTurn(gameState);

                if (!ekTurnOrder.reactionsRemaining()){
                    // apply stack
                    if (actionStack.size()%2 == 0){
                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);
                            //Action was successfully noped
                        ((IsNopeable) actionStack.pop()).nopedExecute(gameState, turnOrder);
                        System.out.println("Action was successfully noped");
                        actionStack.clear();
                    } else {
                        if (actionStack.size() > 2)
                            System.out.println("All nopes were noped");
                        ((ExplodingKittenTurnOrder) turnOrder).currentPlayer = initialPlayer;

                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);

                        //Action can be played
                        IAction stackedAction = actionStack.get(0);
                        stackedAction.Execute(gameState, turnOrder);
                        actionStack.clear();
                    }
                    if (ekgs.gamePhase == ExplodingKittensGamePhase.NopePhase)
                        ekgs.gamePhase = ExplodingKittensGamePhase.PlayerMove;
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

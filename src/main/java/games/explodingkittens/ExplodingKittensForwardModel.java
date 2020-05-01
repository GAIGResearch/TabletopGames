package games.explodingkittens;

import core.actions.IAction;
import core.AbstractGameState;
import core.ForwardModel;
import games.explodingkittens.actions.IsNope;
import games.explodingkittens.actions.IsNopeable;

import java.util.Stack;


public class ExplodingKittensForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();
    private int initialPlayer = -1;

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        System.out.println(action.toString());
        ExplodingKittenTurnOrder ekTurnOrder = (ExplodingKittenTurnOrder) gameState.getTurnOrder();
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;

        if (actionStack.size() == 0){
            if (action instanceof IsNopeable){
                actionStack.add(action);
                ekTurnOrder.registerNopeableActionByPlayer(ekgs);
                initialPlayer = ekTurnOrder.currentPlayer;
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
                ekTurnOrder.endPlayerTurn(gameState);

                if (!ekTurnOrder.reactionsRemaining()){
                    // apply stack
                    if (actionStack.size()%2 == 0){
                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);
                            //Action was successfully noped
                        ((IsNopeable) actionStack.pop()).nopedExecute(gameState, ekTurnOrder);
                        System.out.println("Action was successfully noped");
                        actionStack.clear();
                    } else {
                        if (actionStack.size() > 2)
                            System.out.println("All nopes were noped");
                        ((ExplodingKittenTurnOrder) ekTurnOrder).currentPlayer = initialPlayer;

                        while (actionStack.size() > 1)
                            actionStack.pop();//.Execute(gameState, turnOrder);

                        //Action can be played
                        IAction stackedAction = actionStack.get(0);
                        stackedAction.execute(gameState);
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

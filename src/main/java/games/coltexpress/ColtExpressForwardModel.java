package games.coltexpress;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;

import java.util.Stack;


public class ColtExpressForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        ColtExpressTurnOrder llTurnOrder = (ColtExpressTurnOrder) gameState.getTurnOrder();
        ColtExpressGameState llgs = (ColtExpressGameState) gameState;
        if (action != null) {
            System.out.println(action.toString());
            action.execute(gameState);
        }
    }

    private void checkWinningCondition(ColtExpressGameState gs) {

    }

    private void printActionStack(){
        System.out.print("Action Stack:");
        for (IAction a : actionStack) {
            System.out.print(a.toString() + ",");
        }
    }
}

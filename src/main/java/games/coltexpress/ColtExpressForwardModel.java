package games.coltexpress;

import core.AbstractGameState;
import core.ForwardModel;
import core.actions.IAction;

import java.util.Stack;


public class ColtExpressForwardModel extends ForwardModel {

    private Stack<IAction> actionStack = new Stack<>();

    @Override
    public void setup(AbstractGameState firstState) {

    }

    @Override
    public void next(AbstractGameState gameState, IAction action) {
        ColtExpressTurnOrder ceto = (ColtExpressTurnOrder) gameState.getTurnOrder();
        ColtExpressGameState cegs = (ColtExpressGameState) gameState;

        if (action != null) {
            System.out.println(action.toString());
            action.execute(gameState);
        } else {
            System.out.println("Player cannot do anything since he has drawn cards or " +
                    " doesn't have any targets available");
        }

        switch (cegs.getGamePhase()){
            case PlanActions:
                ceto.endPlayerTurn(gameState);
                break;
            case ExecuteActions:
                ceto.endPlayerTurn(gameState);
                if (cegs.plannedActions.getSize() == 0)
                    ceto.endRoundCard(gameState);
                break;
            case DraftCharacter:
                break;
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

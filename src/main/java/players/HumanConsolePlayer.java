package players;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;

import java.util.List;
import java.util.Scanner;


public class HumanConsolePlayer extends AbstractPlayer {

    @Override
    public int getAction(AbstractGameState observation) {
        List<AbstractAction> actions = observation.getActions();

        if (observation instanceof IPrintable)
            ((IPrintable) observation).printToConsole();

        for (int i = 0; i < actions.size(); i++)
            if (actions.get(i) != null) {
                System.out.print("Action " + i + ": ");
                actions.get(i).printToConsole();
            }
            else
                System.out.println("action i: Action does not implement IPrintableAction");

        System.out.println("Type the index of your desired action:");
        Scanner in = new Scanner(System.in);
        boolean invalid = true;
        int playerAction = 0;
        while (invalid) {
            playerAction = in.nextInt();
            if (playerAction < 0 || playerAction >= actions.size())
                System.out.println("Chosen index" + playerAction + " is invalid. " +
                        "Choose any number in the range of [0, "+ actions.size()+ "]:");
            else
                invalid = false;
        }

        return playerAction;
    }

    @Override
    public void registerUpdatedObservation(AbstractGameState observation) {
        //if (observation instanceof IPrintable)
        //   ((IPrintable) observation).printToConsole();
        System.out.println("No actions available. End turn by pressing any key...");
        Scanner in = new Scanner(System.in);
        in.next();
    }
}


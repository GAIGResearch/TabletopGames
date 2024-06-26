package players.human;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IPrintable;

import java.util.List;
import java.util.Scanner;


public class HumanConsolePlayer extends AbstractPlayer {

    public HumanConsolePlayer() {
        super(null, "HumanConsolePlayer");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {

        if (observation instanceof IPrintable)
            ((IPrintable) observation).printToConsole();

        for (int i = 0; i < actions.size(); i++)
            if (actions.get(i) != null) {
                System.out.println("Action " + i + ": " + actions.get(i).getString(observation));
            }
            else
                System.out.println("Null action");

        System.out.println("Type the index of your desired action:");
        Scanner in = new Scanner(System.in);
        boolean invalid = true;
        int playerAction = 0;
        while (invalid) {
            playerAction = in.nextInt();
            if (playerAction < 0 || playerAction >= actions.size())
                System.out.println("Chosen index" + playerAction + " is invalid. " +
                        "Choose any number in the range of [0, "+ (actions.size()-1)+ "]:");
            else
                invalid = false;
        }

        return actions.get(playerAction);
    }

    @Override
    public void registerUpdatedObservation(AbstractGameState observation) {
        //if (observation instanceof IPrintable)
        //   ((IPrintable) observation).printToConsole();
        System.out.println("No actions available. End turn by pressing any key...");
        Scanner in = new Scanner(System.in);
        in.next();
    }

    @Override
    public AbstractPlayer copy() {
        return this;
    }
}


package players;

import core.AbstractPlayer;
import core.actions.IAction;
import core.observations.IPrintable;
import core.observations.IObservation;

import java.util.List;
import java.util.Scanner;


public class HumanConsolePlayer extends AbstractPlayer {

    @Override
    public void initializePlayer(IObservation observation) {

    }

    @Override
    public void finalizePlayer(IObservation observation) {

    }

    @Override
    public int getAction(IObservation observation, List<IAction> actions) {
        if (observation instanceof IPrintable)
            ((IPrintable) observation).printToConsole();

        for (int i = 0; i < actions.size(); i++)
            if (actions.get(i) instanceof IPrintable) {
                System.out.print("Action " + i + ": ");
                ((IPrintable) actions.get(i)).printToConsole();
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
    public void registerUpdatedObservation(IObservation observation) {
        if (observation instanceof IPrintable)
            ((IPrintable) observation).printToConsole();
        System.out.println("No actions available. End turn by pressing any key...");
        Scanner in = new Scanner(System.in);
        in.next();
    }


}


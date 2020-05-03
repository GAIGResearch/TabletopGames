package players;

import actions.IAction;
import observations.IPrintable;
import observations.Observation;
import players.AbstractPlayer;

import java.util.List;
import java.util.Scanner;


public class HumanConsolePlayer extends AbstractPlayer {

    public HumanConsolePlayer(int playerID){
        super(playerID);
    }

    @Override
    public void initializePlayer(Observation observation) {

    }

    @Override
    public void finalizePlayer(Observation observation) {

    }

    @Override
    public int getAction(Observation observation, List<IAction> actions) {
        if (observation instanceof IPrintable)
            ((IPrintable) observation).PrintToConsole();

        for (int i = 0; i < actions.size(); i++)
            if (actions.get(i) instanceof IPrintable) {
                System.out.print("Action " + i + ": ");
                ((IPrintable) actions.get(i)).PrintToConsole();
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
}


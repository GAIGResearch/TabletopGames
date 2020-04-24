package updated_core.players;

import updated_core.actions.IAction;
import updated_core.observations.Observation;
import updated_core.observations.PrintableObservation;

import java.util.ArrayList;
import java.util.Scanner;


public class HumanConsolePlayer extends AbstractPlayer {

    public HumanConsolePlayer(int playerID){
        super(playerID);
    }

    @Override
    public void initializePlayer(Observation observation) {

    }

    @Override
    public void finalizePlayer() {

    }

    public int getAction(Observation observation, ArrayList<IAction> actions) {
        if (observation instanceof PrintableObservation)
            ((PrintableObservation) observation).PrintToConsole();

        for (int i = 0; i < actions.size(); i++)
            if (observation instanceof PrintableObservation)
                ((PrintableObservation) observation).PrintToConsole();
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


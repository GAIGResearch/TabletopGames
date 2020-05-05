package players;

import core.actions.IAction;
import core.observations.IObservation;
import core.observations.IPrintable;

import javax.swing.*;
import java.util.List;
import java.util.Scanner;


public class HumanGUIPlayer extends AbstractPlayer {
    ActionController ac;

    public HumanGUIPlayer(int playerID, ActionController ac){
        super(playerID);
        this.ac = ac;
    }

    @Override
    public void initializePlayer(IObservation observation) {

    }

    @Override
    public void finalizePlayer(IObservation observation) {

    }

    @Override
    public int getAction(IObservation observation, List<IAction> actions) {
        return actions.indexOf(ac.getAction());
    }
}


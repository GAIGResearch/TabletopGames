package core.evaluation;

import core.AbstractPlayer;

import java.util.LinkedList;

public abstract class AbstractTournament {
    LinkedList<AbstractPlayer> agents;

    public AbstractTournament(LinkedList<AbstractPlayer> agents){
        this.agents = agents;
    }

    public abstract void runTournament();


}

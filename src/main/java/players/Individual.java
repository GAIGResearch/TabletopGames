package players;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class Individual implements Comparable {

    protected AbstractAction[] actions;         // actions in individual. length of individual = actions.length
    protected AbstractGameState[] gameStates;   // actions in individual. length of individual = actions.length
    protected double value;
    private Random gen;

    Individual(int L, AbstractForwardModel fm, AbstractGameState gs, int playerID, Random gen) {
        // initialize
        actions = new AbstractAction[L];
        gameStates = new AbstractGameState[L+1];
        gameStates[0] = gs;

        // set actions
        for (int i = 0; i < L; i++) {
            AbstractGameState gsCopy = gs.copy();
            List<AbstractAction> currentActions = gs.getActions();

            actions[i] = currentActions.get(gen.nextInt(currentActions.size()));
            fm.next(gsCopy, actions[i]);
            gameStates[i+1] = gsCopy;
            gs = gsCopy;
        }
        this.gen = gen;
        this.value = gs.getScore(playerID);
    }

    Individual(Individual I){
        actions = new AbstractAction[I.actions.length];
        gameStates = new AbstractGameState[I.gameStates.length];

        for (int i = 0; i < I.actions.length; i++){
            actions[i] = I.actions[i];
            gameStates[i] = I.gameStates[i];
        }

        value = I.value;
        gen = I.gen;
    }

    public void mutate(AbstractForwardModel fm, int playerID){
        int startIndex = gen.nextInt(actions.length-1);
        AbstractGameState gs = gameStates[startIndex];
        for (int i = startIndex; i < actions.length; i++){
            AbstractGameState gsCopy = gs.copy();
            List<AbstractAction> currentActions = gs.getActions();

            actions[i] = currentActions.get(gen.nextInt(currentActions.size()));
            fm.next(gsCopy, actions[i]);
            gameStates[i+1] = gsCopy;
            gs = gsCopy;
        }
        this.value = gs.getScore(playerID);
    }

    @Override
    public int compareTo(Object o) {
        Individual a = this;
        Individual b = (Individual)o;
        return Double.compare(b.value, a.value);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Individual)) return false;

        Individual a = this;
        Individual b = (Individual)o;

        for (int i = 0; i < actions.length; i++) {
            if (a.actions[i] != b.actions[i]) return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("" + value + ": ");
        for (AbstractAction action : actions) s.append(action).append(" ");
        return s.toString();
    }
}

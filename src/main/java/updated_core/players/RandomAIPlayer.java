package updated_core.players;

import updated_core.actions.IAction;
import updated_core.observations.Observation;

import java.util.ArrayList;
import java.util.Random;


public class RandomAIPlayer extends AbstractPlayer {

    private Random rnd;
    public RandomAIPlayer(int playerID){
        super(playerID);
        rnd = new Random();
    }

    @Override
    public void initializePlayer(Observation observation) {

    }

    @Override
    public void finalizePlayer() {

    }

    public int getAction(Observation observation, ArrayList<IAction> actions) {
        return rnd.nextInt(actions.size());
    }
}

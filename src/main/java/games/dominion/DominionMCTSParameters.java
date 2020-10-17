package games.dominion;

import core.AbstractPlayer;
import players.mcts.MCTSParams;

import java.util.*;

public class DominionMCTSParameters extends MCTSParams {
    public DominionMCTSParameters(long seed) {
        super(seed);
    }


    @Override
    public HashMap<Integer, ArrayList<?>> getSearchSpace() {
        HashMap<Integer, ArrayList<?>> retValue = super.getSearchSpace();
        retValue.put(3, new ArrayList<String>() {{
            add("Random");
            add("BigMoney");
        }});
        return retValue;
    }

    @Override
    public AbstractPlayer getRolloutStrategy() {
        switch (rolloutType) {
            case "BigMoney":
                return new BigMoney();
            default:
                return super.getRolloutStrategy();
        }
    }
}

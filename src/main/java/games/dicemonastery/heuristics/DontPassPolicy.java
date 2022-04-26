package games.dicemonastery.heuristics;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.actions.*;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class DontPassPolicy extends AbstractPlayer {

    Random rnd = new Random(System.currentTimeMillis());

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {

        DiceMonasteryGameState state = (DiceMonasteryGameState) gameState;
        int player = state.getCurrentPlayer();


        AbstractAction vellum = new PrepareVellum(state.getParams().prepareVellumCost);
        AbstractAction bakeBread = new BakeBread(state.getParams().bakeBreadCost);

        // We first check a few priorities
        if (possibleActions.contains(vellum))
            return vellum;

        if (possibleActions.stream().anyMatch(a -> a instanceof HarvestWheat))
            return possibleActions.stream().filter(a -> a instanceof HarvestWheat).max(Comparator.comparingInt(hw -> ((HarvestWheat) hw).getActionPoints())).get();

        if (possibleActions.contains(bakeBread)) {
            int bread = state.getResource(player, BREAD, STOREROOM);
            int honey = state.getResource(player, HONEY, STOREROOM);
            if (bread + honey < state.monksIn(null, player).size()) {
                // we do not have enough food for Winter
                return bakeBread;
            }
        }

        if (possibleActions.stream().anyMatch(a -> a instanceof WriteText || a instanceof GoOnPilgrimage)) {
            possibleActions.removeIf(a -> !(a instanceof WriteText) && !(a instanceof GoOnPilgrimage));
        }

        AbstractAction firstAction = possibleActions.get(0);

        possibleActions.remove(new Pass());
        possibleActions.remove(new DoNothing());

        if (possibleActions.isEmpty())
            return firstAction;

        return possibleActions.get(rnd.nextInt(possibleActions.size()));
    }

    @Override
    public DontPassPolicy copy() {
        return this;
    }
}

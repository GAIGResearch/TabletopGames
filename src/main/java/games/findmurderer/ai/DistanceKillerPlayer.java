package games.findmurderer.ai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.findmurderer.MurderGameState;
import games.findmurderer.MurderParameters;
import games.findmurderer.actions.Kill;
import games.findmurderer.actions.Move;
import games.findmurderer.components.Person;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class DistanceKillerPlayer extends AbstractPlayer {
    // Parameters
    double passProbability = 0;

    // Constant
    static final DoNothing passAction = new DoNothing();

    // Other variables
    long randomSeed;
    Random r;
    HashMap<Integer, Double> distanceToKiller;  // maps component ID of person -> distance to killer
    double maxDistance;

    public DistanceKillerPlayer() {
        randomSeed = System.currentTimeMillis();
        r = new Random(randomSeed);
    }

    @Override
    public AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        MurderGameState mgs = (MurderGameState) gameState;
        MurderParameters mp = (MurderParameters) gameState.getGameParameters();
        List<Person> people = mgs.getGrid().getNonNullComponents();

        distanceToKiller = new HashMap<>();
        Vector2D killerPosition = mgs.getPersonToPositionMap().get(mgs.getKiller().getComponentID());
        for (Person p: people) {
            Vector2D pos = mgs.getPersonToPositionMap().get(p.getComponentID());
            distanceToKiller.put(p.getComponentID(), mp.distanceFunction.apply(killerPosition, pos));
        }

        maxDistance = Math.max(mp.distanceFunction.apply(killerPosition, new Vector2D(0,0)),
                mp.distanceFunction.apply(killerPosition, new Vector2D(0, mgs.getGrid().getHeight())));
        maxDistance = Math.max(maxDistance, mp.distanceFunction.apply(killerPosition, new Vector2D(mgs.getGrid().getWidth(), 0)));
        maxDistance = Math.max(maxDistance, mp.distanceFunction.apply(killerPosition, new Vector2D(mgs.getGrid().getWidth(), mgs.getGrid().getHeight())));

        if (possibleActions.contains(passAction) && r.nextDouble() < passProbability || possibleActions.size() == 1 && possibleActions.get(0) instanceof DoNothing) {
            // If a pass probability is set, then pass action will be returned with that probability
            return passAction;
        }
        else {
            // If not passing, have to choose which action to do (e.g. which person to kill).

            ArrayList<Integer> possibleTargets = new ArrayList<>();
            ArrayList<AbstractAction> moves = new ArrayList<>();
            HashMap<Integer, Kill> targetToActionMap = new HashMap<>();
            double probSum = 0;

            boolean chooseKillAction = true;
            // TODO: also don't kill if getting too suspicious
            if (mp.distanceFunction.apply(killerPosition, mgs.getDetectiveFocus()) < mp.detectiveVisionRange) {
                // Detective is observing, don't kill
                chooseKillAction = false;
            } else {
                // Get a list of all possible targets, keeping a mapping from target ID to action object used to kill them
                for (AbstractAction aa: possibleActions) {
                    if (aa instanceof Kill) {
                        Kill a = (Kill)aa;
                        double distance = distanceToKiller.get(a.target);
                        probSum += maxDistance / distance;
                        targetToActionMap.put(a.target, a);
                        possibleTargets.add(a.target);
                    } else if (aa instanceof Move) moves.add(aa);
                }
                if (possibleTargets.size() == 0) {
                    chooseKillAction = false;
                }
            }

            if (!chooseKillAction) {
                // If we shouldn't kill, move randomly if possible, if not justpass

                // TODO: smarter move
                if (moves.size() > 1) return moves.get(r.nextInt(moves.size()-1));
                else if (moves.size() == 1) return moves.get(0);
                return passAction;

            } else {

                // Calculate probabilities to kill each target based on their distance to the killer
                double[] probabilities = new double[possibleTargets.size()];
                for (int i = 0; i < probabilities.length; i++) {
                    probabilities[i] = (maxDistance / distanceToKiller.get(possibleTargets.get(i))) / probSum;
                }

                // Choose random target based on probabilities
                double p = r.nextDouble();
                double sum = 0.0;
                int i = 0;
                while (sum < p) {
                    sum += probabilities[i];
                    i++;
                }

                // Return action that kills selected target
                return targetToActionMap.get(possibleTargets.get(i - 1));
            }
        }
    }

    @Override
    public DistanceKillerPlayer copy() {
        DistanceKillerPlayer player = new DistanceKillerPlayer();
        player.passProbability = passProbability;
        player.randomSeed = randomSeed;
        player.r = r;
        player.distanceToKiller = new HashMap<>(distanceToKiller);
        return player;
    }
}

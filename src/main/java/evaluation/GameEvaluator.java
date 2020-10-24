package evaluation;

import core.*;
import evodef.*;

import java.util.*;

public class GameEvaluator implements SolutionEvaluator {

    Game game;
    AgentSearchSpace<AbstractPlayer> searchSpace;
    int nPlayers;
    List<AbstractPlayer> opponents;
    int nEvals = 0;
    Random rnd;
    boolean avoidOppDupes;

    public GameEvaluator(Game game, AgentSearchSpace<AbstractPlayer> searchSpace,
                         List<AbstractPlayer> opponents, Random rnd,
                         boolean avoidOpponentDuplicates) {
        this.game = game;
        this.searchSpace = searchSpace;
        this.nPlayers = game.getGameState().getNPlayers();
        this.opponents = opponents;
        this.rnd = rnd;
        this.avoidOppDupes = avoidOpponentDuplicates;
        if (avoidOppDupes && opponents.size() < nPlayers - 1)
            throw new AssertionError("Insufficient Opponents to avoid duplicates");
    }

    @Override
    public void reset() {
        nEvals = 0;
    }

    @Override
    public double evaluate(int[] settings) {
        return evaluate(searchSpace.convertSettings(settings));
    }

    @Override
    public double evaluate(double[] settings) {

        double finalScore = 0.0;

        AbstractPlayer player = searchSpace.getAgent(settings);
        List<AbstractPlayer> allPlayers = new ArrayList<>(nPlayers);

        // We can reduce variance here by cycling the playerIndex on each iteration
        int playerIndex = nEvals % nPlayers;
        for (int i = 0; i < nPlayers; i++) {
            if (i != playerIndex) {
                int oppIndex;
                do {
                    oppIndex = rnd.nextInt(opponents.size());
                } while (avoidOppDupes && allPlayers.contains(opponents.get(oppIndex)));
                allPlayers.add(opponents.get(oppIndex));
            } else {
                allPlayers.add(player);
            }
        }

        game.reset(allPlayers, rnd.nextLong());

        game.run();

        AbstractGameState finalState = game.getGameState();
        finalScore += finalState.getScore(playerIndex);

        nEvals++;

        return finalScore;
    }

    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

    @Override
    public int nEvals() {
        return nEvals;
    }

    @Override
    public EvolutionLogger logger() {
        return null;
    }


}

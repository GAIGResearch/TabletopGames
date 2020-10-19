package evaluation;

import core.*;
import evodef.*;
import games.GameType;

import java.util.*;

public class GameEvaluator implements SolutionEvaluator {

    AgentSearchSpace<AbstractPlayer> searchSpace;
    AbstractForwardModel forwardModel;
    AbstractGameState initialState;
    GameType gameType;
    int nPlayers;
    List<AbstractPlayer> opponents;
    int nEvals = 0;
    Random rnd;
    boolean avoidOppDupes;

    public GameEvaluator(GameType gameType, AgentSearchSpace<AbstractPlayer> searchSpace,
                         AbstractForwardModel forwardModel, AbstractGameState initialState,
                         int nPlayers, List<AbstractPlayer> opponents, Random rnd,
                         boolean avoidOpponentDuplicates) {
        this.searchSpace = searchSpace;
        this.forwardModel = forwardModel;
        this.initialState = initialState.copy();
        this.gameType = gameType;
        this.nPlayers = nPlayers;
        this.opponents = opponents;
        this.rnd = rnd;
        this.avoidOppDupes = avoidOpponentDuplicates;
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
        allPlayers.add(playerIndex, player);
        for (int i = 0; i < nPlayers; i++) {
            if (i != playerIndex) {
                int oppIndex = 0;
                if (avoidOppDupes && opponents.size() < nPlayers - 1)
                    throw new AssertionError("Insufficient Opponents to avoid duplicates");
                do {
                    oppIndex = rnd.nextInt(opponents.size());
                } while (avoidOppDupes && allPlayers.contains(opponents.get(oppIndex)));
                allPlayers.add(i, opponents.get(oppIndex));
            }
        }

        Game game = new Game(gameType, allPlayers, forwardModel, initialState.copy());

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

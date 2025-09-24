package evaluation.optimisation;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameHeuristic;
import core.interfaces.IStateHeuristic;
import evaluation.listeners.IGameListener;
import evaluation.optimisation.ntbea.AgentSearchSpace;
import evaluation.optimisation.ntbea.SearchSpace;
import evaluation.optimisation.ntbea.SolutionEvaluator;
import games.GameType;
import players.IAnyTimePlayer;

import java.util.*;
import java.util.stream.IntStream;

import static evaluation.optimisation.NTBEAParameters.Mode.CoopNTBEA;
import static evaluation.optimisation.NTBEAParameters.Mode.StableNTBEA;
import static java.util.stream.Collectors.toList;

/**
 * Game Evaluator is used for NTBEA optimisation of parameters. It implements the SolutionEvaluator interface.
 * On each NTBEA trial the evaluate(int[] settings) function is called with the set of parameters to try next.
 * The meaning of these settings is encapsulated in the AgentSearchSpace, as this will vary with whatever is being
 * optimised.
 */
public class GameEvaluator implements SolutionEvaluator {

    NTBEAParameters params;
    public boolean debug = false;
    GameType game;
    AbstractParameters gameParams;
    AgentSearchSpace<?> searchSpace;
    int nPlayers;
    List<AbstractPlayer> opponents;
    int nEvals = 0;
    Random rnd;
    boolean avoidOppDupes;
    IStateHeuristic stateHeuristic;
    IGameHeuristic gameHeuristic;
    List<IGameListener> listeners = new ArrayList<>();

    /**
     * GameEvaluator
     *
     * @param game                    The game that will be run for each trial. After each trial it is reset().
     * @param params                  The NTBEAParameters object that defines any parameter settings
     * @param opponents               A List of opponents to be played against. In each trial a random set of these opponents will be
     *                                used in addition to the main agent being tested.
     *                                To use the same set of opponents in each game, this should contain N-1 AbstractPlayers, where
     *                                N is the number of players in the game.
     * @param avoidOpponentDuplicates If this is true, then each individual in opponents will only be used once per game.
     *                                If this is false, then it is important not to use AbstractPlayers that maintain
     *                                any state, or that make any use of their playerId. (So RandomPlayer is fine.)
     */
    public GameEvaluator(GameType game,
                         NTBEAParameters params,
                         int nPlayers,
                         List<AbstractPlayer> opponents,
                         IStateHeuristic stateHeuristic, IGameHeuristic gameHeuristic,
                         boolean avoidOpponentDuplicates) {
        this.game = game;
        this.params = params;
        this.gameParams = params.gameParams;
        this.searchSpace = (ITPSearchSpace<?>) params.searchSpace;
        this.nPlayers = nPlayers;
        this.stateHeuristic = stateHeuristic;
        this.gameHeuristic = gameHeuristic;
        this.opponents = opponents;
        this.rnd = new Random(params.seed);
        this.avoidOppDupes = avoidOpponentDuplicates && opponents.size() > 1;
        if (avoidOppDupes && opponents.size() < nPlayers - 1)
            throw new AssertionError("Insufficient Opponents to avoid duplicates");
    }

    @Override
    public void reset() {
        nEvals = 0;
    }

    /**
     * There should never be a need to call this method directly. It is called by the NTBEA framework as needed.
     *
     * @param settings is an integer array corresponding to the searchSpace.
     *                 The length of settings corresponds to searchSpace.nDims()
     *                 the value of settings[i] is a number in [0, searchSpace.nValues(i)]
     *                 the actual underlying parameter value can be found with searchSpace.value(i, settings[i])
     * @return Returns the game score for the agent being optimised
     */
    @Override
    public double evaluate(int[] settings) {
        if (debug)
            System.out.printf("Starting evaluation %d of %s at %tT%n", nEvals,
                    Arrays.toString(settings), System.currentTimeMillis());
        Object configuredThing = searchSpace.instantiate(settings);
        boolean tuningPlayer = configuredThing instanceof AbstractPlayer;
        boolean tuningGame = configuredThing instanceof Game;

        Game newGame = tuningGame ? (Game) configuredThing : game.createGameInstance(nPlayers, gameParams);
        // we assign one player to each team (the default for a game is each player being their own team of 1)
        int nTeams = newGame.getGameState().getNTeams();

        // We can reduce variance here by cycling the teamIndex on each iteration
        // If we're not tuning the player, then setting index to -99 means we just use the provided opponents list
        // in setupPlayers()
        int teamIndex = tuningPlayer ? nEvals % nTeams : -99;

        // We generally one game per evaluation, unless we are in 'Stable' mode,
        // in which case we reduce variance by running one game for each position the tuned agent can be in
        if (params.mode == StableNTBEA && !tuningPlayer)
            throw new AssertionError("StableNTBEA mode requires tuning of player");
        int gamesToRun = params.mode == StableNTBEA ? nTeams : 1;
        long seed = rnd.nextLong();
        double retValue = 0.0;
        for (int loop = 0; loop < gamesToRun; loop++) {
            int thisTeamIndex = teamIndex == -99 ? -99 : (teamIndex + loop) % nTeams;
            List<AbstractPlayer> allPlayers = setupPlayers(thisTeamIndex, nTeams, settings);

            // always reset the random seed for each new game
            newGame.reset(allPlayers, seed);
            newGame.run();

            int playerOnTeam = -1;
            for (int p = 0; p < newGame.getGameState().getNPlayers(); p++) {
                if (newGame.getGameState().getTeam(p) == thisTeamIndex) {
                    playerOnTeam = p;
                }
            }
            if (tuningPlayer && playerOnTeam == -1)
                throw new AssertionError("No Player found on team " + thisTeamIndex);
            retValue += (tuningGame ? gameHeuristic.evaluateGame(newGame) : stateHeuristic.evaluateState(newGame.getGameState(), playerOnTeam)) / gamesToRun;
        }

        nEvals++;
        return retValue;
    }

    private List<AbstractPlayer> setupPlayers(int teamIndex, int nTeams, int[] settings) {
        List<AbstractPlayer> allPlayers = new ArrayList<>(nPlayers);
        // create a random permutation of opponents - this is used if we want to avoid opponent duplicates
        // if we allow duplicates, then we randomise them all independently
        List<Integer> opponentOrdering = IntStream.range(0, opponents.size()).boxed().collect(toList());
        Collections.shuffle(opponentOrdering);
        int count = 0;
        for (int i = 0; i < nTeams; i++) {
            if (params.mode != CoopNTBEA && i != teamIndex) {
                int oppIndex = (avoidOppDupes) ? count : rnd.nextInt(opponents.size());
                count = (count + 1) % nTeams;
                allPlayers.add(opponents.get(oppIndex).copy());
            } else {
                AbstractPlayer tunedPlayer = (AbstractPlayer) searchSpace.instantiate(settings); // we create for each, in case this is coop
                allPlayers.add(tunedPlayer);
            }
        }
        if (params.budget > 0) {
            for (AbstractPlayer player : allPlayers) {
                if (player instanceof IAnyTimePlayer anyTime)
                    anyTime.setBudget(params.budget);
            }
        }
        return allPlayers;
    }

    public void addListener(IGameListener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    /**
     * @return The searchSpace
     */
    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

    /**
     * @return The number of NTBEA iterations/trials that have been run so far
     */
    @Override
    public int nEvals() {
        return nEvals;
    }


}

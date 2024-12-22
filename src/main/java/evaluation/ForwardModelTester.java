package evaluation;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.GameType;
import players.PlayerFactory;
import utilities.Utils;

import java.util.*;

public class ForwardModelTester {

    /**
     * The idea here is to run a game from start to finish - optionally using some agent to make
     * decisions.
     * At each point in the game we:
     * i) copy the game state once for each action, and check it is equal to the original (plus same hashcode)
     * ii) Apply each action to the copied state
     * iii) Then after applying each action confirm that a) the hashcode of the parent state has changed
     * (we cannot necessarily assert that the new state *has* changed)
     * iv) As we proceed through the game, we retain a List of the (copies of) the game state at each previous decision point.
     * These should remain unchanged.
     * <p>
     * This is a useful way of checking automatically if some copy() is not doing a full deep copy, as in this case
     * an action may (incorrectly) change the state of the historic game state from which it was copied.
     */

    List<Integer> hashCodes = new ArrayList<>();
    List<int[]> superHashCodes = new ArrayList<>();
    List<String> hashNames = new ArrayList<>();
    List<AbstractGameState> stateHistory = new ArrayList<>();
    List<AbstractAction> actionHistory = new ArrayList<>();
    int decision = 0;

    public static void main(String... args) {
        new ForwardModelTester(args);
    }

    public ForwardModelTester(String... args) {
        this(null, args);
    }

    public ForwardModelTester(AbstractParameters params, String... args) {
        String agentToPlay = Utils.getArg(args, "agent", "random");
        int numberOfGames = Utils.getArg(args, "nGames", 1);
        String gameToRun = Utils.getArg(args, "game", "MonopolyDeal");
        int nPlayers = Utils.getArg(args, "nPlayers", 2);
        boolean verbose = Arrays.asList(args).contains("verbose");
        GameType gt = GameType.valueOf(gameToRun);
        long seed = Utils.getArg(args, "seed", System.currentTimeMillis());
        Game game = params == null ? gt.createGameInstance(nPlayers, seed) : gt.createGameInstance(nPlayers, params);
        List<AbstractPlayer> allPlayers = new ArrayList<>();
        AbstractPlayer agent = PlayerFactory.createPlayer(agentToPlay);
        for (int i = 0; i < nPlayers; i++)
            allPlayers.add(agent.copy());
        Random rnd = new Random(seed);
        for (int loop = 1; loop <= numberOfGames; loop++) {
            hashCodes = new ArrayList<>();
            superHashCodes = new ArrayList<>();
            stateHistory = new ArrayList<>();
            actionHistory = new ArrayList<>();
            hashNames = new ArrayList<>();
            seed = rnd.nextInt();
            System.out.printf("Running Game %d of %s with seed %d at %tc%n", loop, gameToRun, seed, System.currentTimeMillis());
            game.reset(allPlayers, seed);

            decision = 0;
            boolean allFine;
            do {
                AbstractGameState stateCopy = game.getGameState().copy();
                stateHistory.add(stateCopy);
                hashCodes.add(game.getGameState().hashCode());
                superHashCodes.add(game.getGameState().hashCodeArray());
                hashNames.add(game.getGameState().toString());
                if (stateCopy.hashCode() != game.getGameState().hashCode()) {
                    String error = String.format("Problem on state copy - orig/copy hashcodes are %d/%d",
                             game.getGameState().hashCode(), stateCopy.hashCode());
                    System.out.println(error);


                    int[] origArr = stateCopy.hashCodeArray();
                    int[] origSupArr = stateCopy.superHashCodeArray();
                    int[] copyArr = game.getGameState().hashCodeArray();
                    int[] copySupArr = game.getGameState().superHashCodeArray();

                    Set<Integer> diffs = new HashSet<>();
                    Set<Integer> diffsup = new HashSet<>();
                    for (int i = 0; i < origArr.length; i++) {
                        if (origArr[i] != copyArr[i]) {
                            System.out.println(i + ": " + origArr[i] + " != " + copyArr[i]);
                            diffs.add(i);
                            }
                        }

                    for (int j = 0; j < origSupArr.length; j++) {
                        if (origSupArr[j] != copySupArr[j]) {
                            System.out.println("SuperHash: " + j + ": " + origSupArr[j] + " != " + copySupArr[j]);
                            diffsup.add(j);
                            if (j == 3)
                            {
                                System.out.println("Orig: " + stateCopy.getActionsInProgress().toString());
                                for (IExtendedSequence action : stateCopy.getActionsInProgress())
                                    System.out.println("Hash: " + action.hashCode());
                                System.out.println("Copy: " + game.getGameState().getActionsInProgress().toString());
                                for (IExtendedSequence action : game.getGameState().getActionsInProgress())
                                    System.out.println("Hash: " + action.hashCode());
                            }
                        }
                    }

                    System.out.println("Diffs: " + diffs);
                    System.out.println("Super Diffs: " + diffsup);

                    System.out.printf("\tOrig: %s%n\tCopy: %s%n", game.getGameState().toString(), stateCopy);
                    throw new AssertionError("Copy of game state should have same hashcode as original");
                }
                allFine = checkHistory();
                int player = game.getGameState().getCurrentPlayer();
                int currentRound = game.getGameState().getRoundCounter();
                AbstractAction action = game.oneAction();
                actionHistory.add(action);
                decision++;
                if (verbose)
                    System.out.printf("Decision %d made by player %d in Round %d (%s)%n", decision, player, currentRound, action);

            } while (allFine && game.getGameState().isNotTerminal());
        }
    }

    private boolean checkHistory() {
        // Here we run through the history of game state to make sure that their hashcodes are unchanged
        for (int i = 0; i < stateHistory.size(); i++) {
            if (stateHistory.get(i).hashCode() != hashCodes.get(i)) {
                String error = String.format("Mismatch on action %d after decision %d (%s) - old/new hashcodes are %d/%d",
                        i, decision, actionHistory.get(decision-1), hashCodes.get(i), stateHistory.get(i).hashCode());
                System.out.println(error);
                for (int j = Math.max(0, i - 20); j <= i; j++)
                    System.out.printf("Decision %d: %s%n", j, actionHistory.get(j).toString());
                System.out.printf("\tOld: %s%n\tNew: %s%n", hashNames.get(i), stateHistory.get(i).toString());
                throw new AssertionError(error + "\n");
            }
        }
        return true;
    }
}

package core;

import games.GameType;
import players.human.ActionController;
import players.simple.LLMPlayer;
import players.simple.OSLAPlayer;
import utilities.Utils;

import java.util.ArrayList;


public class LLMTest {

    /**
     * The recommended way to run a game is via evaluations.Frontend, however that may not work on
     * some games for some screen sizes due to the vagaries of Java Swing...
     * <p>
     * Test class used to run a specific game. The user must specify:
     * 1. Action controller for GUI interactions / null for no visuals
     * 2. Random seed for the game
     * 3. Players for the game
     * 4. Game parameter configuration
     * 5. Mode of running
     * and then run this class.
     */
    public static void main(String[] args) {
        GameType gameType = GameType.valueOf(Utils.getArg(args, "game", "TicTacToe"));
        boolean useGUI = Utils.getArg(args, "gui", true);
        int turnPause = Utils.getArg(args, "turnPause", 0);
        long seed = Utils.getArg(args, "seed", System.currentTimeMillis());
        ActionController ac = new ActionController();

        /* Set up players for the game */
        ArrayList<AbstractPlayer> players = new ArrayList<>();
//        players.add(new MCTSPlayer());
        players.add(new OSLAPlayer());
        players.add(new LLMPlayer());

//        MCTSParams params = new MCTSParams();
//        params.heuristic = new StringHeuristic();
//        players.add(new MCTSPlayer(params));

        int nGames = 20;
        int wins = 0;
        int ties = 0;
        int playerToMonitor = 1;
        for (int i = 0; i < nGames; i++) {
            Game game = gameType.createGameInstance(players.size());
            game.reset(players);
            game.run();
            CoreConstants.GameResult[] results = game.getGameState().getPlayerResults();
            if (results[playerToMonitor] == CoreConstants.GameResult.WIN_GAME) {
                wins++;
            } else if (results[playerToMonitor] == CoreConstants.GameResult.DRAW_GAME) {
                ties++;
            }
        }
        System.out.println(wins*1.0/nGames + "," + ties*1.0/nGames);

        // TODO human in the loop with GUI
    }

}

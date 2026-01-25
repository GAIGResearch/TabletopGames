package players;

import core.*;
import players.human.ActionController;
import players.human.HumanConsolePlayer;
import players.human.HumanGUIPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.*;
import static players.PlayerType.Property.*;

/**
 * Encapsulates all players available in the framework.
 * All player types further include a list of features, which can be used to filter the player collection.
 */
public enum PlayerType {

    /**
     * Each player in the framework corresponds to a enum value here, giving a list of categories the player belongs to,
     * and a list of features the game uses.
     * Add here all players.
     */
    HumanGUIPlayer(new ArrayList<Property>() {{ add(Human); }}),
    HumanConsolePlayer (new ArrayList<Property>() {{ add(Human); }}),
    Random (new ArrayList<Property>() {{ add(Simple); add(Stochastic); }}),
    OSLA (new ArrayList<Property>() {{ add(Simple); add(Stochastic); add(ForwardPlanning); add(Greedy); }}),
    MCTS (new ArrayList<Property>() {{ add(Stochastic); add(ForwardPlanning); add(TreeSearch); }}),
    RMHC (new ArrayList<Property>() {{ add(Stochastic); add(ForwardPlanning); add(EvolutionaryAlgorithm); }});

    /**
     * Converts a given string to the enum type corresponding to the player.
     * Add here all players.
     * @param player - string of a player type
     * @return - PlayerType corresponding to String
     */
    public PlayerType stringToGameType(String player) {
        switch (player.toLowerCase()) {
            case "random":
                return Random;
            case "osla":
                return OSLA;
            case "mcts":
                return MCTS;
            case "rmhc":
                return RMHC;
            case "console":
                return HumanConsolePlayer;
            case "gui":
                return HumanGUIPlayer;
        }
        System.out.println("Player type not found, returning null. ");
        return null;
    }

    /**
     * Creates an instance of the given player type, with a specific random seed.
     * Add here all players implemented.
     * @param seed - random seed for this player.
     * @param ac - action controller object for GUI player, can be null.
     * @param params - parameters for the agent, can be null (goes to default values).
     * @return - instance of AbstractPlayer object; null if player not implemented.
     */
    public AbstractPlayer createPlayerInstance(long seed, ActionController ac, PlayerParameters params) {
        AbstractPlayer player = null;
        Random r = new Random(seed);
        if (params != null) {
            params.setRandomSeed(seed);
        }

        switch(this) {
            case HumanGUIPlayer:
                player = new HumanGUIPlayer(ac);
                break;
            case HumanConsolePlayer:
                player = new HumanConsolePlayer();
                break;
            case Random:
                player = new RandomPlayer(r);
                break;
            case OSLA:
                player = new OSLAPlayer(r);
                break;
            case MCTS:
                if (params == null) {
                    params = new MCTSParams();
                }
                player = new MCTSPlayer((MCTSParams) params);
                break;
            case RMHC:
                if (params == null) {
                    params = new RMHCParams();
                }
                player = new RMHCPlayer((RMHCParams) params);
                break;
        }

        return player;
    }

    public PlayerParameters createParameterSet() {
        switch(this) {
            case MCTS:
                return new MCTSParams();
            case RMHC:
                return new RMHCParams();
            default:
                return null;
        }
    }

    /**
     *Creates an instance of the given player type, with a new random seed.
     * @param ac - action controller object for GUI player, can be null.
     * @param params - parameters for the agent, can be null (goes to default values).
     * @return - instance of AbstractPlayer object; null if player not implemented.
     */
    public AbstractPlayer createPlayerInstance(ActionController ac, PlayerParameters params) {
        return createPlayerInstance(System.currentTimeMillis(), ac, params);
    }

    // Shortcut for creating AI player, with a new random seed and new parameters (Human GUI player would not be functional)
    public AbstractPlayer createPlayerInstance() {
        return createPlayerInstance(System.currentTimeMillis(), null, null);
    }
    // Shortcut for creating AI player, with a given random seed and new parameters (Human GUI player would not be functional)
    public AbstractPlayer createPlayerInstance(long seed) {
        return createPlayerInstance(seed, null, null);
    }
    // Shortcut for creating AI player, with a new random seed and given parameters (Human GUI player would not be functional)
    public AbstractPlayer createPlayerInstance(PlayerParameters params) {
        return createPlayerInstance(System.currentTimeMillis(), null, params);
    }
    // Shortcut for creating AI player, with a given random seed and given parameters (Human GUI player would not be functional)
    public AbstractPlayer createPlayerInstance(long seed, PlayerParameters params) {
        return createPlayerInstance(seed, null, params);
    }
    // Shortcut for creating AI player with a new random seed and new parameters (functional Human GUI player)
    public AbstractPlayer createPlayerInstance(ActionController ac) {
        return createPlayerInstance(System.currentTimeMillis(), ac, null);
    }

    // Properties of this AI player type
    private ArrayList<Property> properties;
    PlayerType(ArrayList<Property> properties) {
        this.properties = properties;
    }
    // Getters
    public ArrayList<Property> getProperties() {
        return properties;
    }

    public enum Property {
        Simple,
        Human,
        Stochastic,
        ForwardPlanning,
        TreeSearch,
        EvolutionaryAlgorithm,
        Greedy;

        /**
         * Retrieves a list of all players with this property.
         * @return - list of player types.
         */
        public List<PlayerType> getAllPlayers() {
            ArrayList<PlayerType> players = new ArrayList<>();
            for (PlayerType gt: PlayerType.values()) {
                if (gt.getProperties().contains(this)) {
                    players.add(gt);
                }
            }
            return players;
        }

        /**
         * Retrieves a list of all players that do NOT have this property.
         * @return - list of player types.
         */
        public List<PlayerType> getAllPlayersExcluding() {
            ArrayList<PlayerType> players = new ArrayList<>();
            for (PlayerType gt: PlayerType.values()) {
                if (!gt.getProperties().contains(this)) {
                    players.add(gt);
                }
            }
            return players;
        }
    }

    @Override
    public String toString() {
        boolean implemented = createPlayerInstance(null, null) != null;
        return this.name() + ANSI_RESET + " {" +
                "\n\tproperties = " + properties +
                (implemented? ANSI_GREEN: ANSI_RED) +
                "\n\timplemented = " + implemented + ANSI_RESET +
                "\n}\n";
    }

    public static void main(String[] args) {
        System.out.println("Players available in the framework: \n");
        for (PlayerType pt: PlayerType.values()) {
            System.out.println(pt.toString());
        }
    }
}

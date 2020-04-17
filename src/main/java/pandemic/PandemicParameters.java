package pandemic;

import core.GameParameters;

import java.util.HashMap;

public class PandemicParameters extends GameParameters {
    public long game_seed = System.currentTimeMillis(); //0;
    public int lose_max_outbreak = 8;

    public int max_cubes_per_city = 3;  // More cause outbreak

    public int n_epidemic_cards = 4;
    public int n_cubes_epidemic = 3;

    public int[] infection_rate = new int[]{2, 2, 2, 3, 3, 4, 4};  // How many cards are drawn for each counter
    public int n_infection_cards_setup = 3;
    public int n_infections_setup = 3;
    public int n_cubes_infection = 1;
    public int n_initial_disease_cubes = 24;
    public int n_cards_for_cure = 5;
    public int n_cards_for_cure_reduced = 4;

    public int n_players = 4;
    public int max_cards_per_player = 7;  // Max cards in hand per player
    public int n_cards_draw = 2;  // Number of cards players draw each turn
    public HashMap<Integer, Integer> n_cards_per_player = new HashMap<Integer, Integer>() {  // Mapping n_players : n_cards_per_player
        {
            put(2, 4);
            put(3, 3);
            put(4, 2);
        }
    };
    public int n_actions_per_turn = 4;
}

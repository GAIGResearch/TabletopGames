package pandemic;

import core.GameParameters;

import java.util.HashMap;

public class PandemicParameters extends GameParameters {
    long game_seed = System.currentTimeMillis(); //0;
    int lose_max_outbreak = 8;

    int max_cubes_per_city = 3;  // More cause outbreak

    int n_epidemic_cards = 4;
    int n_cubes_epidemic = 3;

    int[] infection_rate = new int[]{2, 2, 2, 3, 3, 4, 4};  // How many cards are drawn for each counter
    int n_infection_cards_setup = 3;
    int n_infections_setup = 3;
    int n_cubes_infection = 1;
    int n_initial_disease_cubes = 24;
    int n_cards_for_cure = 5;
    int n_cards_for_cure_reduced = 4;
    int n_forecast_cards = 6;  // How many cards should be viewed and rearranged for the Forecast event card

    int n_players = 4;
    int max_cards_per_player = 7;  // Max cards in hand per player
    int n_cards_draw = 2;  // Number of cards players draw each turn

    // Number of cards each player receives.
    HashMap<Integer, Integer> n_cards_per_player = new HashMap<Integer, Integer>() {  // Mapping n_players : n_cards_per_player
        {
            put(2, 4);
            put(3, 3);
            put(4, 2);
        }
    };
    int n_actions_per_turn = 4;
    int n_research_stations = 6;


    @Override
    public GameParameters copy() {
        PandemicParameters ppCopy = new PandemicParameters();

        ppCopy.game_seed = game_seed;
        ppCopy.lose_max_outbreak = lose_max_outbreak;
        ppCopy.max_cubes_per_city = max_cubes_per_city;  // More cause outbreak
        ppCopy.n_epidemic_cards = n_epidemic_cards;
        ppCopy.n_cubes_epidemic = n_cubes_epidemic;
        ppCopy.n_infection_cards_setup = n_infection_cards_setup;
        ppCopy.n_infections_setup = n_infections_setup;
        ppCopy.n_cubes_infection = n_cubes_infection;
        ppCopy.n_initial_disease_cubes = n_initial_disease_cubes;
        ppCopy.n_cards_for_cure = n_cards_for_cure;
        ppCopy.n_cards_for_cure_reduced = n_cards_for_cure_reduced;
        ppCopy.n_players = n_players;
        ppCopy.max_cards_per_player = max_cards_per_player;  // Max cards in hand per player
        ppCopy.n_cards_draw = n_cards_draw;  // Number of cards players draw each turn
        ppCopy.n_actions_per_turn = n_actions_per_turn;
        ppCopy.n_research_stations = n_research_stations;

        // How many cards are drawn for each counter
        ppCopy.infection_rate = new int[infection_rate.length];
        System.arraycopy(infection_rate, 0, ppCopy.infection_rate, 0, infection_rate.length);

        // Number of cards each player receives.
        ppCopy.n_cards_per_player = new HashMap<>();
        for(int key : n_cards_per_player.keySet())
            ppCopy.n_cards_per_player.put(key, n_cards_per_player.get(key));

        return ppCopy;
    }
}

package games.pandemic;

import core.AbstractParameters;
import core.Game;
import evaluation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class PandemicParameters extends TunableParameters {

    String dataPath;

    int lose_max_outbreak = 8;
    int max_cubes_per_city = 3;  // More cause outbreak
    int n_city_cards = 48;
    int n_event_cards = 5;
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
    int max_cards_per_player = 7;  // Max cards in hand per player
    int n_cards_draw = 2;  // Number of cards players draw each turn
    int n_actions_per_turn = 4;
    int n_research_stations = 6;

    // Number of cards each player receives.
    HashMap<Integer, Integer> n_cards_per_player = new HashMap<Integer, Integer>() {  // Mapping n_players : n_cards_per_player
        {
            put(2, 4);
            put(3, 3);
            put(4, 2);
        }
    };

    public PandemicParameters(String dataPath, long seed) {
        super(seed);
        this.dataPath = dataPath;

        addTunableParameter("lose_max_outbreak", 8, Arrays.asList(5,8,10,15));
        addTunableParameter("max_cubes_per_city", 3, Arrays.asList(3,5,8,10));
        addTunableParameter("n_epidemic_cards", 4, Arrays.asList(1,2,3,4,5,6));
        addTunableParameter("n_cubes_epidemic", 3, Arrays.asList(3,5,8,10));
        addTunableParameter("n_infection_cards_setup", 3, Arrays.asList(1,3,5,8));
        addTunableParameter("n_infections_setup", 3, Arrays.asList(1,3,5,8));
        addTunableParameter("n_cubes_infection", 1, Arrays.asList(1,2,3));
        addTunableParameter("n_initial_disease_cubes", 24, Arrays.asList(15,20,24,30,50));
        addTunableParameter("n_cards_for_cure", 5, Arrays.asList(3,4,5,6,7));
        addTunableParameter("n_cards_for_cure_reduced", 4, Arrays.asList(2,3,4,5,6));
        addTunableParameter("n_forecast_cards", 6, Arrays.asList(3,4,5,6,7,8));
        addTunableParameter("max_cards_per_player", 7, Arrays.asList(5,7,10,15));
        addTunableParameter("n_cards_draw", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("n_actions_per_turn", 4, Arrays.asList(1,2,3,4,5,6,7,8));
        addTunableParameter("n_research_stations", 6, Arrays.asList(4,5,6,7,8));
    }

    public PandemicParameters(PandemicParameters pandemicParameters) {
        this(pandemicParameters.dataPath, System.currentTimeMillis());

        this.lose_max_outbreak = pandemicParameters.lose_max_outbreak;
        this.max_cubes_per_city = pandemicParameters.max_cubes_per_city;  // More cause outbreak
        this.n_city_cards = pandemicParameters.n_city_cards;
        this.n_event_cards = pandemicParameters.n_event_cards;
        this.n_epidemic_cards = pandemicParameters.n_epidemic_cards;
        this.n_cubes_epidemic = pandemicParameters.n_cubes_epidemic;
        this.n_infection_cards_setup = pandemicParameters.n_infection_cards_setup;
        this.n_infections_setup = pandemicParameters.n_infections_setup;
        this.n_cubes_infection = pandemicParameters.n_cubes_infection;
        this.n_initial_disease_cubes = pandemicParameters.n_initial_disease_cubes;
        this.n_cards_for_cure = pandemicParameters.n_cards_for_cure;
        this.n_cards_for_cure_reduced = pandemicParameters.n_cards_for_cure_reduced;
        this.max_cards_per_player = pandemicParameters.max_cards_per_player;  // Max cards in hand per player
        this.n_cards_draw = pandemicParameters.n_cards_draw;  // Number of cards players draw each turn
        this.n_actions_per_turn = pandemicParameters.n_actions_per_turn;
        this.n_research_stations = pandemicParameters.n_research_stations;
        this.n_forecast_cards = pandemicParameters.n_forecast_cards;

        // How many cards are drawn for each counter
        this.infection_rate = new int[infection_rate.length];
        System.arraycopy(pandemicParameters.infection_rate, 0, infection_rate, 0, infection_rate.length);

        // Number of cards each player receives.
        this.n_cards_per_player = new HashMap<>();
        for(int key : pandemicParameters.n_cards_per_player.keySet())
            this.n_cards_per_player.put(key, pandemicParameters.n_cards_per_player.get(key));
    }

    public int getLose_max_outbreak() {
        return lose_max_outbreak;
    }

    public int getMax_cubes_per_city() {
        return max_cubes_per_city;
    }

    public int getN_epidemic_cards() {
        return n_epidemic_cards;
    }

    public int getN_cubes_epidemic() {
        return n_cubes_epidemic;
    }

    public int[] getInfection_rate() {
        return infection_rate;
    }

    public int getN_infection_cards_setup() {
        return n_infection_cards_setup;
    }

    public int getN_infections_setup() {
        return n_infections_setup;
    }

    public int getN_cubes_infection() {
        return n_cubes_infection;
    }

    public int getN_initial_disease_cubes() {
        return n_initial_disease_cubes;
    }

    public int getN_cards_for_cure() {
        return n_cards_for_cure;
    }

    public int getN_cards_for_cure_reduced() {
        return n_cards_for_cure_reduced;
    }

    public int getN_forecast_cards() {
        return n_forecast_cards;
    }

    public int getMax_cards_per_player() {
        return max_cards_per_player;
    }

    public int getN_cards_draw() {
        return n_cards_draw;
    }

    public HashMap<Integer, Integer> getN_cards_per_player() {
        return n_cards_per_player;
    }

    public int getN_actions_per_turn() {
        return n_actions_per_turn;
    }

    public int getN_research_stations() {
        return n_research_stations;
    }

    public String getDataPath(){return dataPath;}

    @Override
    protected AbstractParameters _copy() {
        return new PandemicParameters(this);
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PandemicParameters)) return false;
        if (!super.equals(o)) return false;
        PandemicParameters that = (PandemicParameters) o;
        return lose_max_outbreak == that.lose_max_outbreak &&
                max_cubes_per_city == that.max_cubes_per_city &&
                n_city_cards == that.n_city_cards &&
                n_event_cards == that.n_event_cards &&
                n_epidemic_cards == that.n_epidemic_cards &&
                n_cubes_epidemic == that.n_cubes_epidemic &&
                n_infection_cards_setup == that.n_infection_cards_setup &&
                n_infections_setup == that.n_infections_setup &&
                n_cubes_infection == that.n_cubes_infection &&
                n_initial_disease_cubes == that.n_initial_disease_cubes &&
                n_cards_for_cure == that.n_cards_for_cure &&
                n_cards_for_cure_reduced == that.n_cards_for_cure_reduced &&
                n_forecast_cards == that.n_forecast_cards &&
                max_cards_per_player == that.max_cards_per_player &&
                n_cards_draw == that.n_cards_draw &&
                n_actions_per_turn == that.n_actions_per_turn &&
                n_research_stations == that.n_research_stations &&
                Objects.equals(dataPath, that.dataPath) &&
                Arrays.equals(infection_rate, that.infection_rate) &&
                Objects.equals(n_cards_per_player, that.n_cards_per_player);
    }

    @Override
    public void _reset() {
        lose_max_outbreak = (int) getParameterValue("lose_max_outbreak");
        max_cubes_per_city = (int) getParameterValue("max_cubes_per_city");
        n_epidemic_cards = (int) getParameterValue("n_epidemic_cards");
        n_cubes_epidemic = (int) getParameterValue("n_cubes_epidemic");
        n_infection_cards_setup = (int) getParameterValue("n_infection_cards_setup");
        n_infections_setup = (int) getParameterValue("n_infections_setup");
        n_cubes_infection = (int) getParameterValue("n_cubes_infection");
        n_initial_disease_cubes = (int) getParameterValue("n_initial_disease_cubes");
        n_cards_for_cure = (int) getParameterValue("n_cards_for_cure");
        n_cards_for_cure_reduced = (int) getParameterValue("n_cards_for_cure_reduced");
        n_forecast_cards = (int) getParameterValue("n_forecast_cards");
        max_cards_per_player = (int) getParameterValue("max_cards_per_player");
        n_cards_draw = (int) getParameterValue("n_cards_draw");
        n_actions_per_turn = (int) getParameterValue("n_actions_per_turn");
        n_research_stations = (int) getParameterValue("n_research_stations");
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, lose_max_outbreak, max_cubes_per_city, n_city_cards, n_event_cards, n_epidemic_cards, n_cubes_epidemic, n_infection_cards_setup, n_infections_setup, n_cubes_infection, n_initial_disease_cubes, n_cards_for_cure, n_cards_for_cure_reduced, n_forecast_cards, max_cards_per_player, n_cards_draw, n_cards_per_player, n_actions_per_turn, n_research_stations);
        result = 31 * result + Arrays.hashCode(infection_rate);
        return result;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Pandemic, new PandemicForwardModel(this, GameType.Pandemic.getMinPlayers()), new PandemicGameState(this, GameType.Pandemic.getMinPlayers()));
    }
}

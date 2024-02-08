package games.pandemic;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class PandemicParameters extends TunableParameters {

    int loseMaxOutbreak = 8;
    int maxCubesPerCity = 3;  // More cause outbreak
    int nEpidemicCards = 4;
    int nCubesEpidemic = 3;
    int[] infectionRate = new int[]{2, 2, 2, 3, 3, 4, 4};  // How many cards are drawn for each counter
    int nInfectionCardsSetup = 3;
    int nInfectionsSetup = 3;
    int nCubesInfection = 1;
    int nInitialDiseaseCubes = 24;
    int nCardsForCure = 5;
    int nCardsForCureReducedBy = 1;  // How many cards less are needed if rules say less are needed (e.g. Scientist role)
    int nForecastCards = 6;  // How many cards should be viewed and rearranged for the Forecast event card
    int maxCardsPerPlayer = 7;  // Max cards in hand per player
    int nCardsDraw = 2;  // Number of cards players draw each turn
    int nActionsPerTurn = 4;
    int nResearchStations = 6;

    // Number of cards each player receives at the start of the game.
    HashMap<Integer, Integer> nCardsPerPlayer = new HashMap<Integer, Integer>() {  // Mapping n_players : n_cards_per_player
        {
            put(2, 4);
            put(3, 3);
            put(4, 2);
        }
    };

    boolean survivalRules = false;  // Only keeps 2 event cards: Airlift and Government Grant

    // Player roles from PandemicConstants.PlayerRole (.name()). Possible to have multiple roles separated by ","
    String player0Role = "Any";
    String player1Role = "Any";
    String player2Role = "Any";
    String player3Role = "Any";

    // Not parameters, just init state definitions. Should be adjusted based on game setup in FM.setup()
    int nCityCards = 48;
    int nEventCards = 5;

    // Not parameter
    String dataPath;

    public PandemicParameters(String dataPath) {
        this.dataPath = dataPath;

        addTunableParameter("loseMaxOutbreak", 8, Arrays.asList(3,5,8,10,15));
        addTunableParameter("maxCubesPerCity", 3, Arrays.asList(2,3,5,8,10));
        addTunableParameter("nEpidemicCards", 4, Arrays.asList(0,1,2,3,4,5,6));
        addTunableParameter("nCubesEpidemic", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nInfectionCardsSetup", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nInfectionsSetup", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nCubesInfection", 1, Arrays.asList(1,2,3));
        addTunableParameter("nInitialDiseaseCubes", 24, Arrays.asList(15,20,24,30,50));
        addTunableParameter("nCardsForCure", 5, Arrays.asList(2,3,4,5,6,7));
        addTunableParameter("nCardsForCureReducedBy", 1, Arrays.asList(1,2,3));
        addTunableParameter("nForecastCards", 6, Arrays.asList(3,4,5,6,7,8));
        addTunableParameter("maxCardsPerPlayer", 7, Arrays.asList(5,7,10,15));
        addTunableParameter("nCardsDraw", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nActionsPerTurn", 4, Arrays.asList(1,2,3,4,5,6,7,8));
        addTunableParameter("nResearchStations", 6, Arrays.asList(4,5,6,7,8));
        addTunableParameter("survivalRules", false, Arrays.asList(false,true));
        addTunableParameter("player0Role", "Any", PandemicConstants.PlayerRole.getRoleListIncludeAny());
        addTunableParameter("player1Role", "Any", PandemicConstants.PlayerRole.getRoleListIncludeAny());
        addTunableParameter("player2Role", "Any", PandemicConstants.PlayerRole.getRoleListIncludeAny());
        addTunableParameter("player3Role", "Any", PandemicConstants.PlayerRole.getRoleListIncludeAny());
        _reset();
    }

    public PandemicParameters(PandemicParameters pandemicParameters) {
        this(pandemicParameters.dataPath);

        this.loseMaxOutbreak = pandemicParameters.loseMaxOutbreak;
        this.maxCubesPerCity = pandemicParameters.maxCubesPerCity;  // More cause outbreak
        this.nCityCards = pandemicParameters.nCityCards;
        this.nEventCards = pandemicParameters.nEventCards;
        this.nEpidemicCards = pandemicParameters.nEpidemicCards;
        this.nCubesEpidemic = pandemicParameters.nCubesEpidemic;
        this.nInfectionCardsSetup = pandemicParameters.nInfectionCardsSetup;
        this.nInfectionsSetup = pandemicParameters.nInfectionsSetup;
        this.nCubesInfection = pandemicParameters.nCubesInfection;
        this.nInitialDiseaseCubes = pandemicParameters.nInitialDiseaseCubes;
        this.nCardsForCure = pandemicParameters.nCardsForCure;
        this.nCardsForCureReducedBy = pandemicParameters.nCardsForCureReducedBy;
        this.maxCardsPerPlayer = pandemicParameters.maxCardsPerPlayer;  // Max cards in hand per player
        this.nCardsDraw = pandemicParameters.nCardsDraw;  // Number of cards players draw each turn
        this.nActionsPerTurn = pandemicParameters.nActionsPerTurn;
        this.nResearchStations = pandemicParameters.nResearchStations;
        this.nForecastCards = pandemicParameters.nForecastCards;
        this.survivalRules = pandemicParameters.survivalRules;
        this.player0Role = pandemicParameters.player0Role;
        this.player1Role = pandemicParameters.player1Role;
        this.player2Role = pandemicParameters.player2Role;
        this.player3Role = pandemicParameters.player3Role;

        // How many cards are drawn for each counter
        this.infectionRate = new int[infectionRate.length];
        System.arraycopy(pandemicParameters.infectionRate, 0, infectionRate, 0, infectionRate.length);

        // Number of cards each player receives.
        this.nCardsPerPlayer = new HashMap<>();
        for(int key : pandemicParameters.nCardsPerPlayer.keySet())
            this.nCardsPerPlayer.put(key, pandemicParameters.nCardsPerPlayer.get(key));
    }

    public int getLoseMaxOutbreak() {
        return loseMaxOutbreak;
    }

    public int getMaxCubesPerCity() {
        return maxCubesPerCity;
    }

    public int getnEpidemicCards() {
        return nEpidemicCards;
    }

    public int getnCubesEpidemic() {
        return nCubesEpidemic;
    }

    public int[] getInfectionRate() {
        return infectionRate;
    }

    public int getnInfectionCardsSetup() {
        return nInfectionCardsSetup;
    }

    public int getnInfectionsSetup() {
        return nInfectionsSetup;
    }

    public int getnCubesInfection() {
        return nCubesInfection;
    }

    public int getnInitialDiseaseCubes() {
        return nInitialDiseaseCubes;
    }

    public int getnCardsForCure() {
        return nCardsForCure;
    }

    public int getnCardsForCureReducedBy() {
        return nCardsForCureReducedBy;
    }

    public int getnForecastCards() {
        return nForecastCards;
    }

    public int getMaxCardsPerPlayer() {
        return maxCardsPerPlayer;
    }

    public int getnCardsDraw() {
        return nCardsDraw;
    }

    public HashMap<Integer, Integer> getnCardsPerPlayer() {
        return nCardsPerPlayer;
    }

    public int getnActionsPerTurn() {
        return nActionsPerTurn;
    }

    public int getnResearchStations() {
        return nResearchStations;
    }

    public String getDataPath(){return dataPath;}

    public String getPlayer0Role() {
        return player0Role;
    }

    public String getPlayer1Role() {
        return player1Role;
    }

    public String getPlayer2Role() {
        return player2Role;
    }

    public String getPlayer3Role() {
        return player3Role;
    }

    public boolean isSurvivalRules() {
        return survivalRules;
    }

    public int getnCityCards() {
        return nCityCards;
    }

    public int getnEventCards() {
        return nEventCards;
    }

    @Override
    protected AbstractParameters _copy() {
        return new PandemicParameters(this);
    }

    @Override
    public void _reset() {
        loseMaxOutbreak = (int) getParameterValue("loseMaxOutbreak");
        maxCubesPerCity = (int) getParameterValue("maxCubesPerCity");
        nEpidemicCards = (int) getParameterValue("nEpidemicCards");
        nCubesEpidemic = (int) getParameterValue("nCubesEpidemic");
        nInfectionCardsSetup = (int) getParameterValue("nInfectionCardsSetup");
        nInfectionsSetup = (int) getParameterValue("nInfectionsSetup");
        nCubesInfection = (int) getParameterValue("nCubesInfection");
        nInitialDiseaseCubes = (int) getParameterValue("nInitialDiseaseCubes");
        nCardsForCure = (int) getParameterValue("nCardsForCure");
        nCardsForCureReducedBy = (int) getParameterValue("nCardsForCureReducedBy");
        nForecastCards = (int) getParameterValue("nForecastCards");
        maxCardsPerPlayer = (int) getParameterValue("maxCardsPerPlayer");
        nCardsDraw = (int) getParameterValue("nCardsDraw");
        nActionsPerTurn = (int) getParameterValue("nActionsPerTurn");
        nResearchStations = (int) getParameterValue("nResearchStations");
        survivalRules = (boolean) getParameterValue("survivalRules");
        player0Role = (String) getParameterValue("player0Role");
        player1Role = (String) getParameterValue("player1Role");
        player2Role = (String) getParameterValue("player2Role");
        player3Role = (String) getParameterValue("player3Role");
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PandemicParameters that = (PandemicParameters) o;
        return loseMaxOutbreak == that.loseMaxOutbreak && maxCubesPerCity == that.maxCubesPerCity && nEpidemicCards == that.nEpidemicCards && nCubesEpidemic == that.nCubesEpidemic && nInfectionCardsSetup == that.nInfectionCardsSetup && nInfectionsSetup == that.nInfectionsSetup && nCubesInfection == that.nCubesInfection && nInitialDiseaseCubes == that.nInitialDiseaseCubes && nCardsForCure == that.nCardsForCure && nCardsForCureReducedBy == that.nCardsForCureReducedBy && nForecastCards == that.nForecastCards && maxCardsPerPlayer == that.maxCardsPerPlayer && nCardsDraw == that.nCardsDraw && nActionsPerTurn == that.nActionsPerTurn && nResearchStations == that.nResearchStations && survivalRules == that.survivalRules && nCityCards == that.nCityCards && nEventCards == that.nEventCards && Arrays.equals(infectionRate, that.infectionRate) && Objects.equals(nCardsPerPlayer, that.nCardsPerPlayer) && Objects.equals(player0Role, that.player0Role) && Objects.equals(player1Role, that.player1Role) && Objects.equals(player2Role, that.player2Role) && Objects.equals(player3Role, that.player3Role) && Objects.equals(dataPath, that.dataPath);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), loseMaxOutbreak, maxCubesPerCity, nEpidemicCards, nCubesEpidemic, nInfectionCardsSetup, nInfectionsSetup, nCubesInfection, nInitialDiseaseCubes, nCardsForCure, nCardsForCureReducedBy, nForecastCards, maxCardsPerPlayer, nCardsDraw, nActionsPerTurn, nResearchStations, nCardsPerPlayer, survivalRules, player0Role, player1Role, player2Role, player3Role, nCityCards, nEventCards, dataPath);
        result = 31 * result + Arrays.hashCode(infectionRate);
        return result;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Pandemic, new PandemicForwardModel(this, GameType.Pandemic.getMinPlayers()), new PandemicGameState(this, GameType.Pandemic.getMinPlayers()));
    }
}

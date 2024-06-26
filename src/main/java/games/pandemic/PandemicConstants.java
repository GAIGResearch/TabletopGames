package games.pandemic;

import utilities.Hash;

import java.util.ArrayList;
import java.util.List;

public class PandemicConstants {
    public final static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public final static int playerLocationHash = Hash.GetInstance().hash("playerLocation");
    public final static int neighboursHash = Hash.GetInstance().hash("neighbours");
    public final static int researchStationHash = Hash.GetInstance().hash("Research Stations");
    public final static int playerCardHash = Hash.GetInstance().hash("playerCard");
    public final static int pandemicBoardHash = Hash.GetInstance().hash("pandemicBoard");
    public final static int infectionRateHash = Hash.GetInstance().hash("Infection Rate");
    public final static int outbreaksHash = Hash.GetInstance().hash("Outbreaks");
    public final static int epidemicCard = Hash.GetInstance().hash("epidemic");
    public final static int infectionHash = Hash.GetInstance().hash("infection");
    public final static int countryHash = Hash.GetInstance().hash("country");
    public final static int edgeHash = Hash.GetInstance().hash("edge");
    public final static int effectHash = Hash.GetInstance().hash("effect");

    // mostly for setup
    public final static int playerDeckHash = Hash.GetInstance().hash("Player Deck");
    public final static int playerDeckDiscardHash = Hash.GetInstance().hash("Player Deck Discard");
    public final static int infectionDiscardHash = Hash.GetInstance().hash("Infection Discard");
    public final static int playerRolesHash = Hash.GetInstance().hash("Player Roles");

    // for contingency planner
    public final static int plannerDeckHash = Hash.GetInstance().hash("plannerDeck");

    public enum PlayerRole {
        Any,
        Scientist,
        QuarantineSpecialist,
        Researcher,
        Dispatcher,
        ContingencyPlanner,
        Medic,
        OperationsExpert;

        public static List<String> getRoleList() {
            ArrayList<String> list = new ArrayList<>();
            for (PlayerRole pr: values()) {
                if (pr != Any) {
                    list.add(pr.name());
                }
            }
            return list;
        }
        public static List<String> getRoleListIncludeAny() {
            ArrayList<String> list = new ArrayList<>();
            for (PlayerRole pr: values()) {
                list.add(pr.name());
            }
            return list;
        }
    }

    public enum EventCards {
        All,
        ResilientPopulation,
        Airlift,
        GovernmentGrant,
        OneQuietNight,
        Forecast
    }
}

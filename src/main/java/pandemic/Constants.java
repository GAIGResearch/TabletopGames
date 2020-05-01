package pandemic;

import utilities.Hash;

public class Constants {

    public enum GameResult {
        GAME_WIN(3),
        GAME_DRAW(1),
        GAME_ONGOING(0),
        GAME_LOSE(-1);

        public final int value;

        GameResult(int value) {
            this.value = value;
        }
    }

    public final static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public final static int playerLocationHash = Hash.GetInstance().hash("playerLocation");
    public final static int neighboursHash = Hash.GetInstance().hash("neighbours");
    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int researchStationHash = Hash.GetInstance().hash("researchStation");
    public final static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public final static int playerCardHash = Hash.GetInstance().hash("playerCard");
    public final static int pandemicBoardHash = Hash.GetInstance().hash("pandemicBoard");
    public final static int infectionRateHash = Hash.GetInstance().hash("Infection Rate");
    public final static int outbreaksHash = Hash.GetInstance().hash("Outbreaks");
    public final static int epidemicCard = Hash.GetInstance().hash("epidemic");
    public final static int playersBNHash = Hash.GetInstance().hash("players");
    public final static int colorHash = Hash.GetInstance().hash("color");
    public final static int infectionHash = Hash.GetInstance().hash("infection");
    public final static int countryHash = Hash.GetInstance().hash("country");

    // mostly for setup
    public final static int playerDeckHash = Hash.GetInstance().hash("Player Deck");
    public final static int playerDeckDiscardHash = Hash.GetInstance().hash("Player Deck Discard");
    public final static int infectionDiscardHash = Hash.GetInstance().hash("Infection Discard");
    public final static int playerRolesHash = Hash.GetInstance().hash("Player Roles");

    // for contingency planner
    public final static int plannerDeckHash = Hash.GetInstance().hash("plannerDeck");
}

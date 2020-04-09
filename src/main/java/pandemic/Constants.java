package pandemic;

import utilities.Hash;

public class Constants {
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
}

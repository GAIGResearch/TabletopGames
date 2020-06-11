package games.catan;

import utilities.Hash;


public class CatanConstants {
    public final static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int typeHash = Hash.GetInstance().hash("type");
    public final static int numberHash = Hash.GetInstance().hash("number");
    public final static int robberHash = Hash.GetInstance().hash("robber");

    public final static int settlementCounterHash = Hash.GetInstance().hash("settlementCounter");
    public final static int cityCounterHash = Hash.GetInstance().hash("cityCounter");
    public final static int roadCounterHash = Hash.GetInstance().hash("roadCounter");

    public final static int cardType = Hash.GetInstance().hash("cardType");

    public final static int countHash = Hash.GetInstance().hash("count");
}

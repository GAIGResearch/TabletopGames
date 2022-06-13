package games.catan;

import utilities.Hash;

import java.awt.*;


public class CatanConstants {
    public final static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int typeHash = Hash.GetInstance().hash("type");
    public final static int numberHash = Hash.GetInstance().hash("number");
    public final static int robberHash = Hash.GetInstance().hash("robber");

    public final static int settlementCounterHash = Hash.GetInstance().hash("settlementCounter");
    public final static int cityCounterHash = Hash.GetInstance().hash("cityCounter");
    public final static int roadCounterHash = Hash.GetInstance().hash("roadCounter");

    public final static int resourceDeckHash = Hash.GetInstance().hash("resourceDeck");
    public final static int developmentDeckHash = Hash.GetInstance().hash("developmentDeck");
    public final static int developmentDiscardDeck = Hash.GetInstance().hash("developmentDiscardDeck");

    public final static int cardType = Hash.GetInstance().hash("cardType");

    public final static int countHash = Hash.GetInstance().hash("count");

    public final static Color[] PlayerColors = new Color[]{Color.RED, Color.YELLOW, Color.BLUE, Color.GREEN};

    public final static int HEX_SIDES = 6;
    public final static int BOARD_SIZE = 7; // 7x7 representation using the r-even representation

}

package games.catan;

import utilities.Hash;


public class CatanConstants {
    public final static String[] colors = new String[]{"yellow", "red", "blue", "black"};

    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int typeHash = Hash.GetInstance().hash("type");
    public final static int numberHash = Hash.GetInstance().hash("numberHash");
}

package utilities;

public class CoreConstants {
    public final static boolean VERBOSE = true;
    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int colorHash = Hash.GetInstance().hash("color");
    public final static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public final static int playersHash = Hash.GetInstance().hash("players");
    public final static int imgHash = Hash.GetInstance().hash("img");
    public final static int backgroundImgHash = Hash.GetInstance().hash("backgroundImg");
    public static boolean PARTIAL_OBSERVABLE = false;
}

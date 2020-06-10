package core;

import utilities.Hash;

public class CoreConstants {
    public final static boolean VERBOSE = true;
    public final static boolean PARTIAL_OBSERVABLE = false;
    public final static boolean DISQUALIFY_PLAYER_ON_ILLEGAL_ACTION_PLAYED = false;

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int colorHash = Hash.GetInstance().hash("color");
    public final static int sizeHash = Hash.GetInstance().hash("size");
    public final static int orientationHash = Hash.GetInstance().hash("orientation");
    public final static int coordinateHash = Hash.GetInstance().hash("coordinates");
    public final static int neighbourHash = Hash.GetInstance().hash("neighbours");
    public final static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public final static int playersHash = Hash.GetInstance().hash("players");
    public final static int imgHash = Hash.GetInstance().hash("img");
    public final static int backgroundImgHash = Hash.GetInstance().hash("backgroundImg");
}

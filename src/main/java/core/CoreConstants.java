package core;

import utilities.Hash;

public class CoreConstants {
    public final static boolean VERBOSE = true;
    public final static boolean PARTIAL_OBSERVABLE = false;
    public final static boolean DISQUALIFY_PLAYER_ON_ILLEGAL_ACTION_PLAYED = false;
    public final static int nameHash = Hash.GetInstance().hash("name");
    public final static int colorHash = Hash.GetInstance().hash("color");
    public final static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public final static int playersHash = Hash.GetInstance().hash("players");
    public final static int imgHash = Hash.GetInstance().hash("img");
    public final static int backgroundImgHash = Hash.GetInstance().hash("backgroundImg");
}

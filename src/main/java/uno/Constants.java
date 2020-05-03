package uno;

import utilities.Hash;

public class Constants {
    public final static int GAME_WIN     = 1;
    public final static int GAME_LOSE    = 0;
    public final static int GAME_ONGOING = -1;

    public final static int playerHandHash = Hash.GetInstance().hash("playerHand");
    public final static int drawDeckHash   = Hash.GetInstance().hash("drawDeck");
    public final static int mainDeckHash   = Hash.GetInstance().hash("mainDeck");

    public final static int numberHash = Hash.GetInstance().hash("number");
    public final static int colorHash  = Hash.GetInstance().hash("color");
    public final static int typeHash   = Hash.GetInstance().hash("type");
}

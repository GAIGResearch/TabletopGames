package core;

import utilities.Hash;

public class CoreConstants {
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

    public enum GameEvents {
        ABOUT_TO_START, GAME_OVER, ROUND_OVER, TURN_OVER, ACTION_CHOSEN, ACTION_TAKEN, GAME_EVENT
        // Mostly self-explanatory, except:
        // GAME_EVENT is some game-specific event that is worthy of notice and is not linked directly to a player action
        //     An example might be the draw of a Epidemic card in Pandemic
        // ACTION_CHOSEN is triggered immediately after a decision is made, but before it is implemented.
        //     Hence it contains the Game State used to make the decision - this is important in Expert Iteration for example.
        // ACTION_TAKEN is triggered after a decision is implemented. The state hence contains the results of the action.
        //     This is useful if we want to update a GUI or similar.
    }

    /**
     * Used in Components that contain other Components (see IComponentContainer) to mark which players can see the
     * contents.
     * MIXED_VISIBILITY is an indicator that none of the previous three apply, and that the IComponentContainer
     * will need to implement more sophisticated logic. This is done for example in PartialObservableDeck - and
     * this should cover almost all future eventualities.
     */
    public enum VisibilityMode {
        VISIBLE_TO_ALL, HIDDEN_TO_ALL, VISIBLE_TO_OWNER, FIRST_VISIBLE_TO_ALL, LAST_VISIBLE_TO_ALL, MIXED_VISIBILITY
    }
}

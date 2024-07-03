package games.seasaltpaper.cards;

import core.actions.AbstractAction;
import games.seasaltpaper.SeaSaltPaperGameState;

import java.util.ArrayList;
import java.util.List;

// TODO change this class to be a collection of static function rather than a wrapper
public class HandManager {

    private HandManager(){}

    public static  List<AbstractAction> generateDuoActions(SeaSaltPaperGameState gs, int playerID) {
        return new ArrayList<>();
    }

    public static int calculatePoint(SeaSaltPaperGameState gs, int playerID)
    {
        return 0;
    }

    public static int calculateColorBonus(SeaSaltPaperGameState gs, int playerID)
    {
        return 0;
    }

}

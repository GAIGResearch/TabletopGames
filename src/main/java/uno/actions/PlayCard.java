package uno.actions;

import actions.Action;
import components.Card;
import content.PropertyString;
import core.GameState;
import uno.UnoGameState;
import utilities.Hash;

public class PlayCard  implements Action {

    Card card;

    PlayCard(Card c) {
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        PropertyString type   = (PropertyString) card.getProperty(Hash.GetInstance().hash("type"));
        PropertyString number = (PropertyString) card.getProperty(Hash.GetInstance().hash("number"));
        PropertyString color  = (PropertyString) card.getProperty(Hash.GetInstance().hash("color"));

        // TODO: play the card

        String mainColor   = ((UnoGameState) gs).GetMainColor();
        int    mainNumber  = ((UnoGameState) gs).GetMainNumber();



        return true;
    }
}

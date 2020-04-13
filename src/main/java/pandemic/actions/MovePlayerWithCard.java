package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import components.Deck;
import content.PropertyBoolean;
import content.PropertyIntArrayList;
import content.PropertyString;
import content.PropertyStringArray;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;

import static pandemic.Constants.nameHash;
import static pandemic.Constants.playerHandHash;


public class MovePlayerWithCard extends MovePlayer implements Action {

    Card card;

    public MovePlayerWithCard(int playerIdx, String city, Card c) {
        super(playerIdx, city);
        this.card = c;
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);

        // Discard the card played
        Deck playerHand = (Deck)gs.getAreas().get(gs.getActivePlayer()).getComponent(playerHandHash);
        playerHand.discard(card);

        return false;
    }
}

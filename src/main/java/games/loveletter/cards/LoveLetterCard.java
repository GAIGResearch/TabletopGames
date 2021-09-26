package games.loveletter.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.loveletter.LoveLetterParameters;
import games.loveletter.actions.*;

public class LoveLetterCard extends Card {

    // each card consists of a type and a value. the card type defines the actions available to the player
    public enum CardType {
        Princess(8, "In case the princess is discarded or played the player is immediately removed from the game.", PrincessAction.class),
        Countess(7, "The Countess needs to be discarded in case the player also hold a King or a Prince card.", CountessAction.class),
        King(6, "The King lets two players swap their hand cards.", KingAction.class),
        Prince(5, "The targeted player discards its current and draws a new one.", PrinceAction.class),
        Handmaid(4, "The handmaid protects the player from any targeted effects until the next turn.", HandmaidAction.class),
        Baron(3, "The Baron lets two players compare their hand card. The player with the lesser valued card is removed from the game.", BaronAction.class),
        Priest(2, "The Priest allows a player to see another player's hand cards.", PriestAction.class),
        Guard(1, "The guard allows to attempt guessing another player's card. If the guess is correct, the targeted opponent is removed from the game.", GuardAction.class);

        private final String cardText;
        private final int value;
        private final Class<? extends AbstractAction> actionClass;
        CardType(int value, String text, Class<? extends AbstractAction> action){
            this.value = value;
            this.cardText = text;
            this.actionClass = action;
        }

        public Class<? extends AbstractAction> getActionClass() {
            return actionClass;
        }

        public int getValue(){ return value;}

        public static int getMaxCardValue() { return 8; }

        public String getCardText(LoveLetterParameters params) {
            return this.name() + " (" + value + "; x" + params.cardCounts.get(this) + "): " + cardText;
        }
    }

    public final CardType cardType;

    public LoveLetterCard(CardType cardType) {
        super(cardType.toString());
        this.cardType = cardType;
    }

    public LoveLetterCard(CardType cardType, int componentID) {
        super(cardType.toString(), componentID);
        this.cardType = cardType;
    }

    public String toString(){
        return cardType.toString();
    }

    @Override
    public Card copy() {
        return new LoveLetterCard(cardType, componentID);
    }
}

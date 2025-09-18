package games.descent2e.actions.archetypeskills;

import core.AbstractGameState;
import games.descent2e.DescentGameState;
import games.descent2e.DescentTypes;
import games.descent2e.actions.DescentAction;
import games.descent2e.actions.Triggers;
import games.descent2e.components.DescentCard;
import games.descent2e.components.Figure;

import java.util.Objects;

public class Unseen extends DescentAction {

    int userID = -1;
    int cardID = -1;
    public Unseen(int userID, int cardID) {
        super(Triggers.ACTION_POINT_SPEND);
        this.userID = userID;
        this.cardID = cardID;
    }

    @Override
    public boolean execute(DescentGameState gs) {
        Figure f = (Figure) gs.getComponentById(userID);
        f.getAttribute(Figure.Attribute.Fatigue).increment(2);
        DescentCard card = (DescentCard) gs.getComponentById(cardID);
        if (card != null)
            f.exhaustCard(card);
        f.addBonus(DescentTypes.SkillBonus.Unseen);
        f.addActionTaken(toString());

        return true;
    }

    @Override
    public Unseen copy() {
        return new Unseen(userID, cardID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Unseen t) {
            return userID == t.userID && cardID == t.cardID;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userID, cardID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Figure f = (Figure) gameState.getComponentById(userID);
        return "Unseen: Attacks against " + f.getName().replace("Hero: ", "") + " must spend 1 Surge to hit";
    }

    @Override
    public String toString() {
    return "Unseen: Attacks against " + userID + " must spend 1 Surge to hit";
    }

    @Override
    public boolean canExecute(DescentGameState dgs) {
        Figure f = (Figure) dgs.getComponentById(userID);
        if (f == null) return false;
        if (f.getAttributeValue(Figure.Attribute.Fatigue) + 2 > f.getAttributeMax(Figure.Attribute.Fatigue)) return false;
        DescentCard card = (DescentCard) dgs.getComponentById(cardID);
        if (card == null) return false;
        if (f.isExhausted(card)) return false;

        return !f.hasBonus(DescentTypes.SkillBonus.Unseen);
    }
}

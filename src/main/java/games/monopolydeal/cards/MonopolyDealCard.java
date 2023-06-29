package games.monopolydeal.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.*;

public class MonopolyDealCard extends Card{
    CardType type;

    protected MonopolyDealCard(games.monopolydeal.cards.CardType type) {
        super(type.name());
        this.type = type;
    }

    public static MonopolyDealCard create(games.monopolydeal.cards.CardType type) {
        switch (type) {
            case Money10:
            case Money1:
            case Money2:
            case Money3:
            case Money4:
            case Money5:
            case PassGo:
            case DoubleTheRent:
            case ItsMyBirthday:
            case DebtCollector:
            case SlyDeal:
            case ForcedDeal:
            case DealBreaker:
            case JustSayNo:
            case MulticolorRent:
            case GreenBlueRent:
            case BrownLightBlueRent:
            case PinkOrangeRent:
            case RailRoadUtilityRent:
            case RedYellowRent:
            case House:
            case Hotel:
            case BrownProperty:
            case BlueProperty:
            case GreenProperty:
            case LightBlueProperty:
            case OrangeProperty:
            case PinkProperty:
            case RailRoadProperty:
            case UtilityProperty:
            case RedProperty:
            case YellowProperty:
            case MulticolorWild:
            case GreenBlueWild:
            case BrownLightBlueWild:
            case PinkOrangeWild:
            case RailRoadGreenWild:
            case RailRoadLightBlueWild:
            case RailRoadUtilityWild:
            case RedYellowWild:
                return new MonopolyDealCard(type);
            default:
                throw new AssertionError("Not yet implemented : " + type);
        }
    }
    public boolean isActionCard() {
        return type.isAction;
    }
    public boolean isPropertyCard(){ return type.isProperty; }
    public boolean isPropertyWildCard(){ return type.isPropertyWild; }
    public int cardMoneyValue(){ return type.moneyValue; }
    public CardType cardType() {
        return type;
    }

    @Override
    public MonopolyDealCard copy() {
        // Currently all cardTypes are immutable - so we can save resources when copying
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MonopolyDealCard) {
            MonopolyDealCard other = (MonopolyDealCard) obj;
            return other.type == type;
        }
        return false;
    }
}

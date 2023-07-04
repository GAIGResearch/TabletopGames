package games.monopolydeal.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.monopolydeal.MonopolyDealGameState;
import games.monopolydeal.actions.*;
import org.apache.poi.ss.formula.atp.Switch;

public class MonopolyDealCard extends Card{
    CardType type;

    SetType useAs; // Used by property wild;

    protected MonopolyDealCard(games.monopolydeal.cards.CardType type) {
        super(type.name());
        this.type = type;
        this.useAs = getSetType(type);
        if(type.isPropertyWild){
            //Modify use as;
        }
    }
    public SetType getSetType(CardType type){
        SetType sType;
        switch (type){
            case BrownProperty:
            case BrownLightBlueWild:
                sType = SetType.Brown;
                break;
            case BlueProperty:
                sType = SetType.Blue;
                break;
            case GreenProperty:
            case GreenBlueWild:
                sType = SetType.Green;
                break;
            case LightBlueProperty:
                sType = SetType.LightBlue;
                break;
            case OrangeProperty:
                sType = SetType.Orange;
                break;
            case PinkProperty:
            case PinkOrangeWild:
                sType = SetType.Pink;
                break;
            case RailRoadProperty:
            case RailRoadGreenWild:
            case RailRoadLightBlueWild:
            case RailRoadUtilityWild:
                sType = SetType.RailRoad;
                break;
            case RedProperty:
                sType = SetType.Red;
                break;
            case UtilityProperty:
                sType = SetType.Utility;
                break;
            case YellowProperty:
            case RedYellowWild:
                sType = SetType.Yellow;
                break;
            default:
                sType = SetType.UNDEFINED;
                break;
        }
        return sType;
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
    public SetType getUseAs() { return useAs; }
    public void setUseAs(SetType sType) {  useAs = sType;}

    public CardType cardType() { return type; }

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

    public boolean isNotMulticolor() {
        if(type == CardType.MulticolorWild)
            return false;
        return true;
    }

    public SetType getAlternateSetType(MonopolyDealCard card) {
        SetType sType= card.getUseAs();
        CardType cType = card.cardType();
        switch(cType){
            case MulticolorWild:
                sType = SetType.UNDEFINED;
                break;
            case GreenBlueWild:
                if(sType == SetType.Green)sType = SetType.Blue;
                else sType = SetType.Green;
                break;
            case BrownLightBlueWild:
                if(sType == SetType.Brown)sType = SetType.LightBlue;
                else sType = SetType.Brown;
                break;
            case PinkOrangeWild:
                if(sType == SetType.Pink)sType = SetType.Orange;
                else sType = SetType.Pink;
                break;
            case RailRoadGreenWild:
                if(sType == SetType.RailRoad)sType = SetType.Green;
                else sType = SetType.RailRoad;
                break;
            case RailRoadLightBlueWild:
                if(sType == SetType.RailRoad)sType = SetType.LightBlue;
                else sType = SetType.RailRoad;
                break;
            case RailRoadUtilityWild:
                if(sType == SetType.RailRoad)sType = SetType.Utility;
                else sType = SetType.RailRoad;
                break;
            case RedYellowWild:
                if(sType == SetType.Red)sType = SetType.Yellow;
                else sType = SetType.Red;
                break;
            default:
                throw new AssertionError("Not yet implemented : " + type);
        }
        return sType;
    }
}

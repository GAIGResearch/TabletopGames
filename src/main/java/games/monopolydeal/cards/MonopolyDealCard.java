package games.monopolydeal.cards;

import core.components.Card;
import games.monopolydeal.MonopolyDealGameState;

import java.util.Objects;

public class MonopolyDealCard extends Card{
    CardType type;

    SetType useAs; // Used by property wild;

    public MonopolyDealCard(CardType type) {
        super(type.name());
        this.type = type;
        this.useAs = type.getSetType();
    }
    private MonopolyDealCard(CardType type, int id) {
        super(type.name(), id);
        this.type = type;
        this.useAs = type.getSetType();
    }
    public static MonopolyDealCard create(CardType type) {
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
    public boolean isDoubleTheRent(){ return type == CardType.DoubleTheRent; }
    @Override
    public MonopolyDealCard copy() {
        // Only property cards need to keep reference of their useAs other cards are immutable
        if(!(isPropertyCard()))
            return this;
        else {
            MonopolyDealCard cardCopy = new MonopolyDealCard(this.cardType(), componentID);
            cardCopy.useAs = this.useAs;
            return cardCopy;
        }
    }

    @Override
    public int hashCode(){
        return Objects.hash(super.hashCode(), type, useAs);
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

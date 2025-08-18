package games.monopolydeal.cards;

import core.components.Card;

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
        return switch (type) {
            case Money10, Money1, Money2, Money3, Money4, Money5, PassGo, DoubleTheRent, ItsMyBirthday, DebtCollector,
                 SlyDeal, ForcedDeal, DealBreaker, JustSayNo, MulticolorRent, GreenBlueRent, BrownLightBlueRent,
                 PinkOrangeRent, RailRoadUtilityRent, RedYellowRent, House, Hotel, BrownProperty, BlueProperty,
                 GreenProperty, LightBlueProperty, OrangeProperty, PinkProperty, RailRoadProperty, UtilityProperty,
                 RedProperty, YellowProperty, MulticolorWild, GreenBlueWild, BrownLightBlueWild, PinkOrangeWild,
                 RailRoadGreenWild, RailRoadLightBlueWild, RailRoadUtilityWild, RedYellowWild ->
                    new MonopolyDealCard(type);
        };
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
            MonopolyDealCard cardCopy = new MonopolyDealCard(type, componentID);
            cardCopy.useAs = this.useAs;
            return cardCopy;
        }
    }

    // We do not use super.hashCode/equals as part of this.hashCode/equals because we want cards to be the same, even if they have different component ids
    @Override
    public int hashCode(){
        return Objects.hash(type.ordinal(), useAs.ordinal());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MonopolyDealCard other) {
            return other.type == type && other.useAs == useAs;
        }
        return false;
    }

    public boolean isNotMulticolor() {
        return type != CardType.MulticolorWild;
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

package games.root_final.cards;

import core.components.Card;
import games.dominion.cards.DominionCard;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RootQuestCard extends Card {

    public enum CardType{
        Errand,
        Escort,
        ExpelBandits,
        FendOffABear,
        Fundraising,
        GiveASpeech,
        GuardDuty,
        LogisticsHelp,
        RepairAShed,
    }

    public final CardType cardType;
    public final RootParameters.ClearingTypes suit;
    public Item.ItemType requirement1;
    public Item.ItemType requirement2;

    public RootQuestCard(CardType cardType, RootParameters.ClearingTypes clearingType) {
        super(cardType.toString());
        this.cardType = cardType;
        suit = clearingType;
        requirement1 = getFirstItem(cardType);
        requirement2 = getSecondItem(cardType);
    }

    public RootQuestCard(CardType cardType, RootParameters.ClearingTypes clearingType, int componentID){
        super(cardType.toString(), componentID);
        this.cardType = cardType;
        suit = clearingType;
        requirement1 = getFirstItem(cardType);
        requirement2 = getSecondItem(cardType);
    }

    @Override
    public RootQuestCard copy(){
        return new RootQuestCard(cardType, suit, componentID);
    }

    private Item.ItemType getFirstItem(CardType cardType){
        return switch (cardType){
            case Errand, Fundraising -> Item.ItemType.tea;
            case Escort, LogisticsHelp -> Item.ItemType.boot;
            case GuardDuty, GiveASpeech, RepairAShed, FendOffABear -> Item.ItemType.torch;
            case ExpelBandits -> Item.ItemType.sword;
        };
    }
    private Item.ItemType getSecondItem(CardType cardType){
        return switch (cardType){
            case Errand, Escort -> Item.ItemType.boot;
            case Fundraising -> Item.ItemType.coin;
            case GuardDuty, ExpelBandits -> Item.ItemType.sword;
            case GiveASpeech -> Item.ItemType.tea;
            case RepairAShed -> Item.ItemType.hammer;
            case FendOffABear -> Item.ItemType.crossbow;
            case LogisticsHelp -> Item.ItemType.bag;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RootQuestCard that = (RootQuestCard) o;
        return cardType == that.cardType && suit == that.suit && requirement1 == that.requirement1 && requirement2 == that.requirement2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardType, suit, requirement1, requirement2);
    }

    @Override
    public String toString(){
        return cardType.name();
    }
}

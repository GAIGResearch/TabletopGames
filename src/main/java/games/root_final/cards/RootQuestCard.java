package games.root_final.cards;

import core.components.Card;
import games.dominion.cards.DominionCard;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.ArrayList;
import java.util.List;

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
            case Errand -> Item.ItemType.tea;
            case Escort -> Item.ItemType.boot;
            case Fundraising -> Item.ItemType.tea;
            case GuardDuty -> Item.ItemType.torch;
            case GiveASpeech -> Item.ItemType.torch;
            case RepairAShed -> Item.ItemType.torch;
            case ExpelBandits -> Item.ItemType.sword;
            case FendOffABear -> Item.ItemType.torch;
            case LogisticsHelp -> Item.ItemType.boot;
        };
    }
    private Item.ItemType getSecondItem(CardType cardType){
        return switch (cardType){
            case Errand -> Item.ItemType.boot;
            case Escort -> Item.ItemType.boot;
            case Fundraising -> Item.ItemType.coin;
            case GuardDuty -> Item.ItemType.sword;
            case GiveASpeech -> Item.ItemType.tea;
            case RepairAShed -> Item.ItemType.hammer;
            case ExpelBandits -> Item.ItemType.sword;
            case FendOffABear -> Item.ItemType.crossbow;
            case LogisticsHelp -> Item.ItemType.bag;
        };
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RootQuestCard other) {
            return other.cardType == cardType && other.suit == suit;
        }
        return false;
    }

    @Override
    public String toString(){
        return cardType.name();
    }
}

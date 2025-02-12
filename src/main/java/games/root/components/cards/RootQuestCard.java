package games.root.components.cards;

import games.root.RootParameters;
import games.root.components.Item;

import java.util.Objects;

public class RootQuestCard extends RootCard {

    public Item.ItemType requirement1;
    public Item.ItemType requirement2;

    public RootQuestCard(CardType cardType, RootParameters.ClearingTypes clearingType) {
        super(cardType, clearingType);
        requirement1 = getFirstItem(cardType);
        requirement2 = getSecondItem(cardType);
    }

    public RootQuestCard(CardType cardType, RootParameters.ClearingTypes clearingType, int componentID){
        super(cardType, clearingType, componentID);
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
            default -> throw new IllegalStateException("Unexpected value: " + cardType);
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
            default -> throw new IllegalStateException("Unexpected value: " + cardType);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RootQuestCard that)) return false;
        if (!super.equals(o)) return false;
        return requirement1 == that.requirement1 && requirement2 == that.requirement2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), requirement1, requirement2);
    }
}

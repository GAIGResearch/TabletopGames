package games.root_final.cards;

import core.actions.AbstractAction;
import core.components.Card;
import games.dominion.cards.DominionCard;
import games.root_final.RootParameters;
import games.root_final.components.Item;

import java.util.ArrayList;
import java.util.List;

public class RootCard extends Card {
    public enum CardType{
        //Bird
        Armorers,
        Sappers,
        BrutalTactics,
        RoyalClaim,
        BirdyBindle,
        WoodlandRunners,
        ArmsTrader,
        CrossBow,
        Ambush,
        Dominance,
        //Rabbit
        BetterBurrowBank,
        Cobbler,
        CommandWarren,
        BakeSale,
        SmugglersTrail,
        RootTea,
        AVisitToFriends,
        FavorOfTheRabbits,
        //Mouse
        Codebreakers,
        ScoutingParty,
        Sword,
        TravelGear,
        Investments,
        FavorOfTheMice,
        MouseInASack,
        //Fox
        StandAndDeliver,
        TaxCollector,
        ProtectionRacket,
        GentlyUsedKnapsack,
        FavorOfTheFoxes,
        FoxfolkSteel,
        Anvil,
        Vizier
    }

    public enum CraftingType{
        immediateDiscard,
        craftedCard,
        itemCard,
        unCraftable
    }
    public final CardType cardtype;

    public CraftingType craftingType;
    public final List<RootParameters.ClearingTypes> craftingCost;
    public final RootParameters.ClearingTypes suit;
    public RootCard(CardType cardType, RootParameters.ClearingTypes clearingType){
        super(cardType.toString());
        this.cardtype = cardType;

        this.suit = clearingType;
        this.craftingCost = getCraftingCost(suit, cardtype);
    }

    public RootCard(CardType cardType, RootParameters.ClearingTypes clearingType, int componentID){
        super(cardType.toString(), componentID);
        this.cardtype = cardType;
        this.suit = clearingType;
        this.craftingCost = getCraftingCost(suit, cardtype);
    }

    @Override
    public Card copy(){
        return new RootCard(cardtype, suit, componentID);
    }

    @Override
    public String toString(){
        return cardtype.name();
    }

    public Item.ItemType getCraftableItem(){
        return switch (cardtype) {
            case ArmsTrader, FoxfolkSteel, Sword -> Item.ItemType.sword;
            case CrossBow -> Item.ItemType.crossbow;
            case Anvil -> Item.ItemType.hammer;
            case AVisitToFriends, TravelGear, WoodlandRunners -> Item.ItemType.boot;
            case BakeSale, ProtectionRacket, Investments -> Item.ItemType.coin;
            case BirdyBindle, SmugglersTrail, GentlyUsedKnapsack, MouseInASack -> Item.ItemType.bag;
            case RootTea -> Item.ItemType.tea;
            default -> null;
        };
    }

    public AbstractAction getAction(){
        return null;
    }

    private List<RootParameters.ClearingTypes> getCraftingCost(RootParameters.ClearingTypes suit, CardType cardType){
        List<RootParameters.ClearingTypes> craftingCost = new ArrayList<>();
        if (suit == RootParameters.ClearingTypes.Rabbit){
            switch (cardType){
                case BetterBurrowBank, Cobbler, CommandWarren:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.craftedCard;
                    break;
                case BakeSale:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case SmugglersTrail, RootTea:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.itemCard;
                    break;
                case AVisitToFriends:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case FavorOfTheRabbits:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.immediateDiscard;
                    break;
                case Ambush, Dominance:
                    craftingType = CraftingType.unCraftable;
                    break;

            }
        } else if (suit == RootParameters.ClearingTypes.Fox) {
            switch (cardType){
                case StandAndDeliver:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.craftedCard;
                    break;
                case TaxCollector:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.craftedCard;
                    break;
                case RootTea, GentlyUsedKnapsack:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.itemCard;
                    break;
                case ProtectionRacket:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case TravelGear:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case FavorOfTheFoxes:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.immediateDiscard;
                    break;
                case FoxfolkSteel:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case Anvil:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case Ambush, Dominance:
                    craftingType = CraftingType.unCraftable;
                    break;

            }
        } else if (suit == RootParameters.ClearingTypes.Mouse) {
            switch (cardType){
                case Codebreakers:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.craftedCard;
                    break;
                case ScoutingParty:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.craftedCard;
                    break;
                case CrossBow:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case Sword:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case TravelGear:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case Investments:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case FavorOfTheMice:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.immediateDiscard;
                    break;
                case RootTea, MouseInASack:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.itemCard;
                    break;
                case Ambush, Dominance:
                    craftingType = CraftingType.unCraftable;
                    break;

            }
        } else if (suit == RootParameters.ClearingTypes.Bird) {
            switch (cardType){
                case WoodlandRunners:
                    craftingCost.add(RootParameters.ClearingTypes.Rabbit);
                    craftingType = CraftingType.itemCard;
                    break;
                case Armorers:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.craftedCard;
                case Sappers:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.craftedCard;
                    break;
                case BrutalTactics:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.craftedCard;
                    break;
                case RoyalClaim:
                    craftingCost.add(RootParameters.ClearingTypes.Bird);
                    craftingCost.add(RootParameters.ClearingTypes.Bird);
                    craftingCost.add(RootParameters.ClearingTypes.Bird);
                    craftingCost.add(RootParameters.ClearingTypes.Bird);
                    craftingType = CraftingType.craftedCard;
                    break;
                case BirdyBindle:
                    craftingCost.add(RootParameters.ClearingTypes.Mouse);
                    craftingType = CraftingType.itemCard;
                    break;
                case ArmsTrader:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case CrossBow:
                    craftingCost.add(RootParameters.ClearingTypes.Fox);
                    craftingType = CraftingType.itemCard;
                    break;
                case Ambush, Dominance:
                    craftingType = CraftingType.unCraftable;
                    break;
            }
        }
        return craftingCost;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RootCard other) {
            return other.cardtype == cardtype && other.suit == suit && componentID == other.componentID;
        }
        return false;
    }
}

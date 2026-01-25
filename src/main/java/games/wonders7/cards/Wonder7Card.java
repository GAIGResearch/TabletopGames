package games.wonders7.cards;

import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7Constants.*;
import games.wonders7.Wonders7GameParameters;
import games.wonders7.Wonders7GameState;
import org.jetbrains.annotations.NotNull;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.cards.Wonder7Board.Wonder.TheStatueOfZeusInOlympia;
import static games.wonders7.cards.Wonder7Card.Type.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class Wonder7Card extends Card {

    public enum Type {
        RawMaterials,
        ManufacturedGoods,
        CivilianStructures,
        ScientificStructures,
        CommercialStructures,
        MilitaryStructures,
        Guilds
    }

    public enum CardType {
        LumberYard(1), StonePit(1), ClayPool(1), OreVein(1), TreeFarm(1), Excavation(1),
        ClayPit(1), TimberYard(1), ForestCave(1), Mine(1),
        Sawmill(2), Quarry(2), Brickyard(2), Foundry(2),
        Loom(1), Glassworks(1), Press(1),
        LoomAge2(2), GlassworksAge2(2), PressAge2(2),
        Workshop(1), Scriptorium(1), Apothecary(1),
        Dispensary(2), Laboratory(2), Library(2), School(2),
        Observatory(3), University(3), Academy(3), Study(3), Lodge(3),
        Baths(1), Altar(1), Theatre(1), Well(1),
        Statue(2), Aqueduct(2), Courthouse(2), Temple(2),
        TownHall(3), Senate(3), Pantheon(3), Palace(3), Gardens(3),
        Tavern(1), EastTradingPost(1), WestTradingPost(1), Marketplace(1),
        Caravansery(2), Forum(2), Vineyard(2), Bazaar(2),
        ChamberOfCommerce(3), Arena(3),  Haven(3), Lighthouse(3), Ludus(3),
        Stockade(1), Barracks(1), GuardTower(1),
        Walls(2), TrainingGround(2), Stables(2), ArcheryRange(2),
        SiegeWorkshop(3), Fortifications(3), Arsenal(3), Circus(3), Castrum(3),
        WorkersGuild(3), CraftsmenGuild(3), MagistratesGuild(3), TradersGuild(3), SpiesGuild(3),
        PhilosophersGuild(3), ShipownersGuild(3), BuildersGuild(3), DecoratorsGuild(3), ScientistsGuild(3);

        public final int age;

        CardType(int age) {
            this.age = age;
        }

    }

    public final Type type;  // Different type of cards, brown cards, grey cards...)
    public final CardType cardType; // Name of card
    public final Map<Resource, Integer> constructionCost; // The resources required to construct structure
    public final Map<Resource, Integer> resourcesProduced; // Resources the card creates
    public final List<CardType> prerequisiteCard;
    protected List<CardEffect> instantEffects = emptyList();
    protected List<CardEffect> endGameEffects = emptyList();

    public static Wonder7Card factory(CardType cardType, Wonders7GameParameters params) {
        switch (cardType) {
            case LumberYard:
                return new Wonder7Card(cardType, RawMaterials, emptyMap(), Map.of(Wood, params.rawMaterialLow));
            case StonePit:
                return new Wonder7Card(cardType, RawMaterials, emptyMap(), Map.of(Stone, params.rawMaterialLow));
            case ClayPool:
                return new Wonder7Card(cardType, RawMaterials, emptyMap(), Map.of(Clay, params.rawMaterialLow));
            case OreVein:
                return new Wonder7Card(cardType, RawMaterials, emptyMap(), Map.of(Ore, params.rawMaterialLow));
            case TreeFarm:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Wood_Clay, params.rawMaterialLow));
            case Excavation:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Stone_Clay, params.rawMaterialLow));
            case ClayPit:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Ore_Clay, params.rawMaterialLow));
            case TimberYard:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Wood_Stone, params.rawMaterialLow));
            case ForestCave:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Wood_Ore, params.rawMaterialLow));
            case Mine:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Stone_Ore, params.rawMaterialLow));
            case Sawmill:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Wood, params.rawMaterialHigh));
            case Quarry:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Stone, params.rawMaterialHigh));
            case Brickyard:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Clay, params.rawMaterialHigh));
            case Foundry:
                return new Wonder7Card(cardType, RawMaterials, Map.of(Coin, 1), Map.of(Ore, params.rawMaterialHigh));
            case Loom, LoomAge2:
                return new Wonder7Card(cardType, ManufacturedGoods, emptyMap(), Map.of(Textile, params.manufacturedMaterial));
            case Glassworks, GlassworksAge2:
                return new Wonder7Card(cardType, ManufacturedGoods, emptyMap(), Map.of(Glass, params.manufacturedMaterial));
            case Press, PressAge2:
                return new Wonder7Card(cardType, ManufacturedGoods, emptyMap(), Map.of(Papyrus, params.manufacturedMaterial));
            case Well, Baths, Altar, Theatre:
                return new Wonder7Card(cardType, CivilianStructures, emptyMap(), Map.of(Victory, params.victoryLow));
            case Statue:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Ore, 2, Wood, 1), Map.of(Victory, params.victoryMed),
                        List.of(CardType.Well));
            case Aqueduct:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Stone, 3), Map.of(Victory, params.victoryHigh),
                        List.of(CardType.Baths));
            case Courthouse:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Clay, 2, Textile, 1), Map.of(Victory, params.victoryMed),
                        List.of(CardType.Scriptorium));
            case Temple:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Wood, 1, Clay, 1, Glass, 1), Map.of(Victory, params.victoryMed));
            case TownHall:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Stone, 2, Ore, 1, Glass, 1), Map.of(Victory, params.victoryVeryHigh));
            case Senate:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Wood, 2, Stone, 1, Ore, 1), Map.of(Victory, params.victoryVeryHigh),
                        List.of(CardType.Library));
            case Gardens:
                return new Wonder7Card(cardType, CivilianStructures, Map.of(Clay, 2, Wood, 1), Map.of(Victory, params.victoryHigh),
                        List.of(CardType.Theatre));
            case Pantheon:
                return new Wonder7Card(cardType, CivilianStructures,
                        Map.of(Clay, 2, Ore, 1, Glass, 1, Papyrus, 1, Textile, 1),
                        Map.of(Victory, params.victoryPantheon), List.of(CardType.Altar));
            case Palace:
                return new Wonder7Card(cardType, CivilianStructures,
                        Map.of(Wood, 1, Stone, 1, Clay, 1, Ore, 1, Glass, 1, Papyrus, 1, Textile, 1),
                        Map.of(Victory, params.victoryPalace));
            case Tavern:
                return new Wonder7Card(cardType, CommercialStructures, emptyMap(), Map.of(Coin, params.tavernMoney));
            case EastTradingPost, WestTradingPost, Marketplace:
                return new Wonder7Card(cardType, CommercialStructures, emptyMap(), emptyMap());
            case Caravansery:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Wood, 2), Map.of(BasicWild, params.wildcardProduction),
                        List.of(CardType.Marketplace));
            case Forum:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Clay, 2), Map.of(RareWild, params.wildcardProduction),
                        List.of(CardType.EastTradingPost, CardType.WestTradingPost));
            case Bazaar:
                return new Wonder7Card(cardType, CommercialStructures, emptyMap(), emptyMap(),
                        List.of(new GainResourceEffect(Coin, ManufacturedGoods, params.commercialMultiplierMed, true, true)),
                        emptyList(), emptyList());
            case Vineyard:
                return new Wonder7Card(cardType, CommercialStructures, emptyMap(), emptyMap(),
                        List.of(new GainResourceEffect(Coin, RawMaterials, params.commercialMultiplierLow, true, true)),
                        emptyList(), emptyList());
            case Lighthouse:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Glass, 1, Stone, 1), emptyMap(),
                        List.of(new GainResourceEffect(Coin, CommercialStructures, params.commercialMultiplierLow, true, false)),
                        List.of(new GainResourceEffect(Victory, CommercialStructures, params.commercialMultiplierLow, true, false)),
                        List.of(CardType.Caravansery));
            case Haven:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Clay, 2), emptyMap(),
                        List.of(new GainResourceEffect(Coin, RawMaterials, params.commercialMultiplierLow, true, false)),
                        List.of(new GainResourceEffect(Victory, RawMaterials, params.commercialMultiplierLow, true, false)),
                        List.of(CardType.Forum));
            case Ludus:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Stone, 1, Ore, 1), emptyMap(),
                        List.of(new GainResourceEffect(Coin, MilitaryStructures, params.commercialMultiplierHigh, true, false)),
                        List.of(new GainResourceEffect(Victory, MilitaryStructures, params.commercialMultiplierLow, true, false)),
                        emptyList());
            case ChamberOfCommerce:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Clay, 2, Papyrus, 1), emptyMap(),
                        List.of(new GainResourceEffect(Coin, ManufacturedGoods, params.commercialMultiplierMed, true, false)),
                        List.of(new GainResourceEffect(Victory, ManufacturedGoods, params.commercialMultiplierMed, true, false)),
                        emptyList());
            case Arena:
                return new Wonder7Card(cardType, CommercialStructures, Map.of(Stone, 2, Ore, 1), emptyMap(),
                        List.of(new GainResourceEffect(Coin,
                                (state, player) -> (state.getPlayerWonderBoard(player).wonderStage - 1) * params.commercialMultiplierHigh)),
                        List.of(new GainResourceEffect(Victory,
                                (state, player) -> (state.getPlayerWonderBoard(player).wonderStage - 1) * params.commercialMultiplierLow)),
                        List.of(CardType.Dispensary));
            case Stockade, Barracks, GuardTower:
                return new Wonder7Card(cardType, MilitaryStructures, emptyMap(), Map.of(Shield, params.militaryLow));
            case Walls:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Stone, 3), Map.of(Shield, params.militaryMed));
            case Stables:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Clay, 1, Wood, 1, Ore, 1),
                        Map.of(Shield, params.militaryMed), List.of(CardType.Apothecary));
            case ArcheryRange:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Wood, 2, Ore, 1),
                        Map.of(Shield, params.militaryMed), List.of(CardType.Workshop));
            case TrainingGround:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Ore, 2, Wood, 1),
                        Map.of(Shield, params.militaryMed));
            case Castrum:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Clay, 2, Wood, 1, Papyrus, 1),
                        Map.of(Shield, params.militaryHigh));
            case Fortifications:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Ore, 3, Clay, 1),
                        Map.of(Shield, params.militaryHigh), List.of(CardType.Walls));
            case Arsenal:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Wood, 2, Ore, 1, Textile, 1),
                        Map.of(Shield, params.militaryHigh));
            case Circus:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Stone, 3, Ore, 1),
                        Map.of(Shield, params.militaryHigh), List.of(CardType.TrainingGround));
            case SiegeWorkshop:
                return new Wonder7Card(cardType, MilitaryStructures, Map.of(Clay, 3, Wood, 1),
                        Map.of(Shield, params.militaryHigh), List.of(CardType.Laboratory));
            case Apothecary:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Textile, 1), Map.of(Compass, params.scienceCompass));
            case Workshop:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Glass, 1), Map.of(Cog, params.scienceCog));
            case Scriptorium:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Papyrus, 1), Map.of(Tablet, params.scienceTablet));
            case Dispensary:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Ore, 2, Glass, 1), Map.of(Compass, params.scienceCompass),
                        List.of(CardType.Apothecary));
            case Laboratory:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Clay, 2, Papyrus, 1), Map.of(Cog, params.scienceCog),
                        List.of(CardType.Workshop));
            case Library:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Stone, 2, Textile, 1), Map.of(Tablet, params.scienceTablet),
                        List.of(CardType.Scriptorium));
            case School:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Wood, 1, Papyrus, 1), Map.of(Tablet, params.scienceTablet));
            case Observatory:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Ore, 2, Glass, 1, Textile, 1), Map.of(Cog, params.scienceCog),
                        List.of(CardType.Laboratory));
            case University:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Wood, 2, Glass, 1, Papyrus, 1), Map.of(Tablet, params.scienceTablet),
                        List.of(CardType.Library));
            case Academy:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Stone, 3, Glass, 1), Map.of(Compass, params.scienceCompass),
                        List.of(CardType.School));
            case Study:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Wood, 1, Papyrus, 1, Textile, 1), Map.of(Cog, params.scienceCog),
                        List.of(CardType.School));
            case Lodge:
                return new Wonder7Card(cardType, ScientificStructures, Map.of(Clay, 2, Papyrus, 1, Textile, 1), Map.of(Compass, params.scienceCompass),
                        List.of(CardType.Dispensary));
            case WorkersGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Ore, 2, Clay, 1, Stone, 1, Wood, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, RawMaterials, params.guildMultiplierLow, false, true)), emptyList());
            case CraftsmenGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Ore, 2, Stone, 2),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, ManufacturedGoods, params.guildMultiplierMed, false, true)), emptyList());
            case MagistratesGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Wood, 3, Stone, 1, Textile, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, CivilianStructures, params.guildMultiplierLow, false, true)), emptyList());
            case TradersGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Ore, 2, Clay, 1, Glass, 1, Papyrus, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, CommercialStructures, params.guildMultiplierLow, false, true)), emptyList());
            case SpiesGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Clay, 2, Glass, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, MilitaryStructures, params.guildMultiplierLow, false, true)), emptyList());
            case PhilosophersGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Clay, 3, Papyrus, 1, Textile, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, ScientificStructures, params.guildMultiplierLow, false, true)), emptyList());
            case ShipownersGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Wood, 3, Glass, 1, Papyrus, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory, ManufacturedGoods, params.guildMultiplierLow, true, false),
                                new GainResourceEffect(Victory, RawMaterials, params.guildMultiplierLow, true, false),
                                new GainResourceEffect(Victory, Guilds, params.guildMultiplierLow, true, false)), emptyList());
            case BuildersGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Stone, 3, Clay, 2, Glass, 1),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory,
                                (state, player) -> {
                                    int retValue = 0;
                                    for (int i = -1; i <= 1; i++) {
                                        retValue += (state.getPlayerWonderBoard((player + i + state.getNPlayers()) % state.getNPlayers()).wonderStage - 1) * params.builderMultiplier;
                                    }
                                    return retValue;
                                })
                        ), emptyList());
            case DecoratorsGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Ore, 2, Stone, 2, Glass, 2),
                        emptyMap(), emptyList(),
                        List.of(new GainResourceEffect(Victory,
                                (state, player) -> {
                                    int totalStagesOfWonder = state.getPlayerWonderBoard(player).totalWonderStages;
                                    int stagesBuilt = state.getPlayerWonderBoard(player).wonderStage - 1;
                                    return (stagesBuilt == totalStagesOfWonder) ? params.decoratorVictoryPoints : 0;
                                })), emptyList());
            case ScientistsGuild:
                return new Wonder7Card(cardType, Guilds, Map.of(Wood, 2, Ore, 2, Papyrus, 1),
                        Map.of(ScienceWild, 1));
            default:
                throw new AssertionError("Unknown card type: " + cardType);
        }
    }

    // A card with a card effect (either instantaneous, or end of game VP)
    public Wonder7Card(CardType cardType, Type type,
                       Map<Wonders7Constants.Resource, Integer> constructionCost,
                       Map<Wonders7Constants.Resource, Integer> resourcesProduced,
                       List<CardEffect> instantEffects,
                       List<CardEffect> endGameEffects,
                       List<CardType> prerequisiteCard) {
        super(cardType.name());
        this.cardType = cardType;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.instantEffects = instantEffects;
        this.endGameEffects = endGameEffects;
        this.prerequisiteCard = prerequisiteCard;
    }

    public Wonder7Card(CardType cardType, Type type,
                       Map<Wonders7Constants.Resource, Integer> constructionCost,
                       Map<Wonders7Constants.Resource, Integer> resourcesProduced) {
        this(cardType, type, constructionCost, resourcesProduced, emptyList(), emptyList(), emptyList());
    }

    public Wonder7Card(CardType cardType, Type type,
                       Map<Wonders7Constants.Resource, Integer> constructionCost,
                       Map<Wonders7Constants.Resource, Integer> resourcesProduced,
                       List<CardType> prerequisiteCard) {
        this(cardType, type, constructionCost, resourcesProduced, emptyList(), emptyList(), prerequisiteCard);
    }


    protected Wonder7Card(CardType cardType, Type type,
                          Map<Resource, Integer> constructionCost,
                          Map<Resource, Integer> resourcesProduced,
                          List<CardType> prerequisiteCard,
                          List<CardEffect> instantEffects,
                          List<CardEffect> endGameEffects,
                          int componentID) {
        super(cardType.name(), componentID);
        this.cardType = cardType;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
        this.instantEffects = instantEffects;
        this.endGameEffects = endGameEffects;
    }

    public int getNProduced(Resource resource) {
        return resourcesProduced.get(resource);
    }

    public void applyInstantCardEffects(Wonders7GameState state, int playerId) {
        for (CardEffect e : instantEffects) {
            e.apply(state, playerId);
        }
    }

    public int endGameVP(Wonders7GameState state, int playerId) {
        int vp = 0;
        for (CardEffect e : endGameEffects) {
            if (e instanceof GainResourceEffect gre) {
                if (gre.resource == Resource.Victory)
                    vp += gre.gain(state, playerId);
            }
        }
        return vp;
    }

    @Override
    public String toString() {
        String cost = mapToStr(constructionCost);
        String makes = mapToStr(resourcesProduced);
        return "{" + cardType +
                "(" + type + ")" +
                (!cost.isEmpty() ? ":cost=" + cost : ",free") +
                (!makes.isEmpty() ? ",makes=" + makes : "") + "}  ";
    }

    private String mapToStr(Map<Resource, Integer> m) {
        return getString(m);
    }

    @NotNull
    public static String getString(Map<Resource, Integer> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Resource, Integer> e : m.entrySet()) {
            if (e.getValue() > 0) s.append(e.getValue()).append(" ").append(e.getKey()).append(",");
        }
        s.append("]");
        if (s.toString().equals("]")) return "";
        return s.toString().replace(",]", "");
    }

    public boolean isFree(int player, Wonders7GameState wgs) {
        // Checks if the player has prerequisite cards and can play for free
        for (Wonder7Card card : wgs.getPlayedCards(player).getComponents()) {
            if (prerequisiteCard.contains(card.cardType)) {
                return true;
            }
        }
        Wonder7Board wonder = wgs.getPlayerWonderBoard(player);
        if (wonder.wonderType() == TheStatueOfZeusInOlympia  && wonder.getSide() == 0 && wonder.wonderStage > 2) {
            // if this is the first card of the type, then it is buildable for free
            if (wgs.getPlayedCards(player).getComponents().stream().noneMatch(c -> c.type == type))
                return true;
        }
        // then check for Olympia night side (first slot means first card is free)
        if (wonder.wonderType() == TheStatueOfZeusInOlympia && wonder.getSide() == 1 && wonder.wonderStage > 1) {
            if (wgs.getPlayerHand(player).getSize() == 7) {
                return true;
            }
        }
        // and second slot means last card is free
        if (wonder.wonderType() == TheStatueOfZeusInOlympia && wonder.getSide() == 1 && wonder.wonderStage > 2) {
            if (wgs.getPlayerHand(player).getSize() == 2) {
                return true;
            }
        }
        return constructionCost.isEmpty(); // Card is free (no construction cost)
    }

    public boolean isAlreadyPlayed(int player, Wonders7GameState wgs) {
        for (Wonder7Card card : wgs.getPlayedCards(player).getComponents()) {
            if (cardType == card.cardType) {
                // Player already has an identical structure, can't play another
                return true;
            }
            // then the special cases for Manufactured resources, as the only ones wit identical cards in different Ages
            if (cardType == CardType.Loom || cardType == CardType.LoomAge2) {
                if (card.cardType == CardType.Loom || card.cardType == CardType.LoomAge2) {
                    return true;
                }
            }
            if (cardType == CardType.Glassworks || cardType == CardType.GlassworksAge2) {
                if (card.cardType == CardType.Glassworks || card.cardType == CardType.GlassworksAge2) {
                    return true;
                }
            }
            if (cardType == CardType.Press || cardType == CardType.PressAge2) {
                if (card.cardType == CardType.Press || card.cardType == CardType.PressAge2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list of all possible build options (trade sources) for the player
     * An empty List<List<TradeSource>> means the player cannot play the card
     * A list that contains an empty List<TradeSource> means the player can play the card for free
     * <p>
     * This includes the player's own composite resources (at a cost of zero).
     */
    public List<List<TradeSource>> buildOptions(int player, Wonders7GameState wgs) {

        // Collects the resources player does not have
        List<Resource> remainingRequirements = new ArrayList<>();
        for (Resource resource : constructionCost.keySet()) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(player).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                if (resource == Coin)
                    return emptyList(); // If player can't afford the card (not enough coins)
                for (int i = 0; i < constructionCost.get(resource) - wgs.getPlayerResources(player).get(resource); i++)
                    remainingRequirements.add(resource);
            }
        }
        if (remainingRequirements.isEmpty())
            return List.of(emptyList()); // If player can afford the card (no resources needed)
        // at this point we have paid anything for which we have the direct resources
        // Now we consider composite resources and purchase options from neighbours

        // we include this in a single algorithm to allow for recursive analysis of all trading options
        // our composite resources have a cost of zero, so will always be allocated first.

        List<TradeSource> compositeSources = new ArrayList<>();
        for (Resource resource : wgs.getPlayerResources(player).keySet()) {
            if (resource.isComposite()) {
                for (int i = 0; i < wgs.getPlayerResources(player).get(resource); i++)
                    compositeSources.add(new TradeSource(resource, 0, -1));
            }
        }

        // get neighbour resources (that would help satisfy our requirements)
        int leftNeighbour = (wgs.getNPlayers() + player - 1) % wgs.getNPlayers();
        int rightNeighbour = (player + 1) % wgs.getNPlayers();
        List<TradeSource> leftNeighbourHas = extractNeighbourTradeOptions(player, wgs, remainingRequirements, leftNeighbour);
        List<TradeSource> rightNeighbourHas = extractNeighbourTradeOptions(player, wgs, remainingRequirements, rightNeighbour);

        // Now amalgamate these three sets of possibilities
        List<TradeSource> tradeSources = new ArrayList<>(compositeSources);
        tradeSources.addAll(leftNeighbourHas);
        tradeSources.addAll(rightNeighbourHas);

        // sort in increasing order of cost, with composite resources after non-composite
        tradeSources.sort(Comparator.comparingInt(TradeSource::cost).thenComparingInt(ts -> ts.resource().isComposite() ? 1 : 0));

        // now we go through the trade sources in increasing order of cost, and buy the resources we need
        // a composite resource is applied to all possible requirements...which branches the search so that all possibilities are considered

        List<TradingOption> allPossibleOptions = getTradingOptions(
                new TradingOption(emptyList(),
                        remainingRequirements,
                        tradeSources,
                        wgs.getPlayerResources(player).get(Coin) - constructionCost.getOrDefault(Coin, 0)));

        if (allPossibleOptions.isEmpty()) {
            return emptyList();  // no way to satisfy the requirements
        }
        return allPossibleOptions.stream().map(to -> to.plannedPurchases).collect(Collectors.toList());
    }

    /**
     * This method checks if the player can play the card.
     * Further, if they can play the card it returns the additional costs they have to pay
     * to other players to acquire their resources
     * This is a List<TradeSource> (TradeSource is a record with fields Resource, cost, fromPlayer) defined in Wonders7Constants
     */
    public Pair<Boolean, List<TradeSource>> isPlayable(int player, Wonders7GameState wgs) {
        if (isAlreadyPlayed(player, wgs))
            return new Pair<>(false, emptyList()); // If player already has an identical structure (can't play another
        if (isFree(player, wgs))
            return new Pair<>(true, emptyList()); // If player can play for free (has prerequisite card

        List<List<TradeSource>> allPossibleOptions = buildOptions(player, wgs);

        if (allPossibleOptions.isEmpty())
            return new Pair<>(false, emptyList()); // If player can't afford the card (not enough coins)

        // Now find the cheapest option
        List<TradeSource> cheapestOption = allPossibleOptions.stream()
                .min(Comparator.comparingInt(ts -> ts.stream().mapToInt(TradeSource::cost).sum()))
                .orElse(emptyList());

        // then remove the resources with a cost of zero
        return new Pair<>(true, cheapestOption.stream().filter(ts -> ts.cost() > 0).collect(Collectors.toList()));
    }

    private List<TradeSource> extractNeighbourTradeOptions(int player, Wonders7GameState wgs, List<Resource> neededResources,
                                                           int neighbour) {
        List<TradeSource> tradeSources = new ArrayList<>();
        for (Resource resource : wgs.getPlayerResources(neighbour).keySet()) {
            if (!resource.isTradeable()) continue; // ignore Coins, Victory etc. symbols
            // is this relevant to us?
            if (neededResources.stream().anyMatch(resource::includes)) {
                for (int i = 0; i < wgs.getPlayerResources(neighbour).get(resource); i++)
                    tradeSources.add(new TradeSource(resource, wgs.costOfResource(resource, player, neighbour), neighbour));
            }
        }
        return tradeSources;
    }

    // helper record for recursive analysis of all trading options
    record TradingOption(List<TradeSource> plannedPurchases, List<Resource> remainingRequirements,
                         List<TradeSource> tradeSources, int coinsLeft) {
    }

    private List<TradingOption> getTradingOptions(TradingOption current) {
        // We loop over the remaining tradeSources until we find one that can satisfy a requirement.
        // If it can satisfy a unique requirement, we add it to the plannedPurchases,
        // remove the requirement from the remainingRequirements and keep looping.
        List<TradeSource> usedTradeSources = new ArrayList<>(current.plannedPurchases);
        List<TradeSource> remainingTradeSources = new ArrayList<>(current.tradeSources);
        List<Resource> remainingRequirements = new ArrayList<>(current.remainingRequirements);
        int coinsLeft = current.coinsLeft;
        for (TradeSource tradeSource : current.tradeSources) {
            if (tradeSource.cost() > coinsLeft) {
                // we have run out of money, so this branch is invalid
                return emptyList();
            }
            Set<Resource> matchingRequirements = remainingRequirements.stream()
                    .filter(tradeSource.resource()::includes)
                    .collect(Collectors.toSet());
            if (matchingRequirements.isEmpty()) {
                // this trade source is not relevant, we remove it and do not branch
                remainingTradeSources.remove(tradeSource);
            } else if (matchingRequirements.size() > 1) {
                // we can use this resource to satisfy multiple requirements
                // so we branch
                List<TradingOption> allOptions = new ArrayList<>();
                for (Resource requirement : matchingRequirements) {
                    List<TradeSource> newPlannedPurchases = new ArrayList<>(usedTradeSources);
                    newPlannedPurchases.add(tradeSource);
                    List<Resource> newRemainingRequirements = new ArrayList<>(remainingRequirements);
                    newRemainingRequirements.remove(requirement);
                    List<TradeSource> newTradeSources = new ArrayList<>(remainingTradeSources);
                    newTradeSources.remove(tradeSource);
                    List<TradingOption> newOptions = getTradingOptions(
                            new TradingOption(newPlannedPurchases, newRemainingRequirements, newTradeSources, coinsLeft - tradeSource.cost()));
                    allOptions.addAll(newOptions);
                }
                return allOptions;
            } else {
                // we can use this resource to satisfy a single requirement, so no branching required
                usedTradeSources.add(tradeSource);
                remainingTradeSources.remove(tradeSource);
                remainingRequirements.remove(matchingRequirements.iterator().next());
                coinsLeft -= tradeSource.cost();
                if (remainingRequirements.isEmpty()) {
                    // we have satisfied all requirements, so this is a valid option
                    return List.of(new TradingOption(usedTradeSources, remainingRequirements, remainingTradeSources, coinsLeft));
                }
            }
        }
        // if we get here, we have exhausted all trade sources without meeting requirements, so this branch is invalid
        return emptyList();
    }

    @Override
    public Card copy() {
        return new Wonder7Card(cardType, type, constructionCost, resourcesProduced, prerequisiteCard, instantEffects, endGameEffects, componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wonder7Card card) {
            return cardType == card.cardType && super.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cardType.ordinal());
    }
}


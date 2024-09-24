package games.root_final;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Dice;
import evaluation.optimisation.TunableParameters;
import games.battlelore.components.Unit;
import games.explodingkittens.cards.ExplodingKittensCard;
import games.root_final.cards.EyrieRulers;
import games.root_final.cards.RootCard;
import games.root_final.cards.RootQuestCard;
import games.root_final.cards.VagabondCharacters;
import games.root_final.components.Item;
import scala.Int;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class RootParameters extends AbstractParameters {

    public String dataPath = "data/root/";
    public Dice.Type dieType = Dice.Type.d4;
    public int cardsDrawnPerTurn = 2;

    public int maxCardsEndTurn = 5;
    public int handSize = 5;
    public int maxWood = 8;
    public int maxViziers = 2;
    public int sympathyTokens = 10;

    public enum MapType {
        Summer,
        Winter,
    }
    public enum Relationship {
        Hostile,
        Neutral,
        One,
        Two,
        Allied,
    }

    public enum VictoryCondition {
        Score,
        DB,
        DM,
        DF,
        DR,
    }

    public enum TokenType {
        Wood,
        Sympathy,
        Keep,
    }

    public enum BuildingType {
        Roost,
        FoxBase,
        MouseBase,
        RabbitBase,
        Recruiter,
        Sawmill,
        Workshop,
        Ruins,
    }

    HashMap<Integer, Integer> catBuildingCost = new HashMap<>(){
        {
            put(6,0);
            put(5,1);
            put(4,2);
            put(3,3);
            put(2,3);
            put(1,4);
        }
    };

    public HashMap<Integer, Integer> catDrawingBonus = new HashMap<>(){
        {
            put(6,0);
            put(5,0);
            put(4,0);
            put(3,1);
            put(2,1);
            put(1,2);
            put(0,2);
        }
    };

    public HashMap<Integer, Integer> eyrieDrawingBonus = new HashMap<>(){
        {
            put(7,0);
            put(6,0);
            put(5,0);
            put(4,1);
            put(3,1);
            put(2,1);
            put(1,2);
            put(0,2);


        }
    };

    HashMap<BuildingType, Integer> buildingCount = new HashMap<BuildingType, Integer>() {
        {
            put(BuildingType.Roost, 7);
            put(BuildingType.FoxBase, 1);
            put(BuildingType.MouseBase, 1);
            put(BuildingType.RabbitBase, 1);
            put(BuildingType.Recruiter, 6);
            put(BuildingType.Sawmill, 6);
            put(BuildingType.Workshop, 6);
            put(BuildingType.Ruins, 4);

        }
    };

    public HashMap<Integer, Integer> sawmillPoints = new HashMap<>(){
        {
            put(6,0);
            put(5,1);
            put(4,2);
            put(3,3);
            put(2,4);
            put(1,5);
        }
    };

    public HashMap<Integer, Integer> workshopPoints = new HashMap<>(){
        {
            put(6,0);
            put(5,2);
            put(4,2);
            put(3,3);
            put(2,4);
            put(1,5);
        }
    };

    public HashMap<Integer, Integer> recruiterPoints = new HashMap<>(){
        {
            put(6,0);
            put(5,1);
            put(4,2);
            put(3,3);
            put(2,3);
            put(1,4);
        }
    };

    public HashMap<Integer, Integer> roostPoints = new HashMap<>(){
        {
            put(7,0);
            put(6,0);
            put(5,1);
            put(4,2);
            put(3,3);
            put(2,4);
            put(1,4);
            put(0,5);
        }
    };

    public HashMap<Integer, Integer> sympathyPoints = new HashMap<>(){
        {
            put(10,0);
            put(9,1);
            put(8,1);
            put(7,1);
            put(6,2);
            put(5,2);
            put(4,3);
            put(3,4);
            put(2,4);
            put(1,4);
        }
    };

    public HashMap<Item.ItemType, Integer> startingItems = new HashMap<>(){
        {
            put(Item.ItemType.boot, 1);
            put(Item.ItemType.torch,1);
            put(Item.ItemType.bag,1);
            put(Item.ItemType.crossbow,1);
            put(Item.ItemType.sword,1);
            put(Item.ItemType.hammer,1);
            put(Item.ItemType.tea,1);
        }
    };

    public HashMap<Item.ItemType, Integer> craftableItems = new HashMap<>(){
        {
            put(Item.ItemType.sword,2);
            put(Item.ItemType.boot,2);
            put(Item.ItemType.bag,2);
            put(Item.ItemType.tea,2);
            put(Item.ItemType.coin,2);
            put(Item.ItemType.crossbow,1);
            put(Item.ItemType.hammer,1);

        }
    };

    public HashMap<Item.ItemType, Integer> ruinItems = new HashMap<>(){
        {
            put(Item.ItemType.bag,1);
            put(Item.ItemType.hammer,1);
            put(Item.ItemType.sword,1);
            put(Item.ItemType.boot,1);
        }
    };

    public HashMap<Item.ItemType, Integer> itemCraftPoints = new HashMap<>(){
        {
            put(Item.ItemType.sword,2);
            put(Item.ItemType.bag,1);
            put(Item.ItemType.coin,3);
            put(Item.ItemType.hammer,2);
            put(Item.ItemType.boot,1);
            put(Item.ItemType.crossbow,1);
            put(Item.ItemType.tea,2);
        }
    };


    public HashMap<EyrieRulers.CardType, Boolean[]> eyrieRulers = new HashMap<EyrieRulers.CardType, Boolean[]>() {
        {
            put(EyrieRulers.CardType.Despot, new Boolean[]{false, true, false, true});
            put(EyrieRulers.CardType.Builder, new Boolean[]{true, true, false, false});
            put(EyrieRulers.CardType.Charismatic, new Boolean[]{true, false, true, false});
            put(EyrieRulers.CardType.Commander, new Boolean[]{false, true, true, false});
        }
    };

    public enum DecreeAction {
        Recruit,
        Move,
        Battle,
        Build
    }

    public HashMap<Integer, DecreeAction> decreeInitializer = new HashMap<>() {
        {
            put(0, DecreeAction.Recruit);
            put(1, DecreeAction.Move);
            put(2, DecreeAction.Battle);
            put(3, DecreeAction.Build);
        }
    };

    HashMap<VagabondCharacters.CardType, List<Item.ItemType>> vagabondCharacterInitializer = new HashMap<>() {
        {
            ArrayList<Item.ItemType> thiefItems = new ArrayList<>();
            thiefItems.add(Item.ItemType.boot);
            thiefItems.add(Item.ItemType.torch);
            thiefItems.add(Item.ItemType.tea);
            thiefItems.add(Item.ItemType.sword);
            put(VagabondCharacters.CardType.Thief, thiefItems);

            ArrayList<Item.ItemType> tinkerItems = new ArrayList<>();
            tinkerItems.add(Item.ItemType.boot);
            tinkerItems.add(Item.ItemType.torch);
            tinkerItems.add(Item.ItemType.bag);
            tinkerItems.add(Item.ItemType.hammer);
            put(VagabondCharacters.CardType.Tinker, tinkerItems);

            ArrayList<Item.ItemType> rangerItems = new ArrayList<>();
            rangerItems.add(Item.ItemType.boot);
            rangerItems.add(Item.ItemType.torch);
            rangerItems.add(Item.ItemType.crossbow);
            rangerItems.add(Item.ItemType.sword);
            put(VagabondCharacters.CardType.Ranger, rangerItems);
        }
    };

    HashMap<RootQuestCard.CardType, ClearingTypes[]> questCardInitializer = new HashMap<>() {
        {
            put(RootQuestCard.CardType.Errand, new ClearingTypes[]{ClearingTypes.Fox, ClearingTypes.Rabbit});
            put(RootQuestCard.CardType.Escort, new ClearingTypes[]{ClearingTypes.Mouse});
            put(RootQuestCard.CardType.ExpelBandits, new ClearingTypes[]{ClearingTypes.Mouse, ClearingTypes.Rabbit});
            put(RootQuestCard.CardType.FendOffABear, new ClearingTypes[]{ClearingTypes.Mouse, ClearingTypes.Rabbit});
            put(RootQuestCard.CardType.Fundraising, new ClearingTypes[]{ClearingTypes.Fox});
            put(RootQuestCard.CardType.GiveASpeech, new ClearingTypes[]{ClearingTypes.Fox, ClearingTypes.Rabbit});
            put(RootQuestCard.CardType.GuardDuty, new ClearingTypes[]{ClearingTypes.Mouse, ClearingTypes.Rabbit});
            put(RootQuestCard.CardType.LogisticsHelp, new ClearingTypes[]{ClearingTypes.Mouse, ClearingTypes.Fox});
            put(RootQuestCard.CardType.RepairAShed, new ClearingTypes[]{ClearingTypes.Fox});
        }
    };

    public HashMap<String, String> cornerPairs = new HashMap<>() {
        {
            put("Top Left", "Bottom Right");
            put("Bottom Left", "Top Right");
            put("Top Right", "Bottom Left");
            put("Bottom Right", "Top Left");
        }
    };

    public HashMap<RootCard.CardType, Integer> birdCards = new HashMap<>() {
        {
            put(RootCard.CardType.Armorers, 2);
            put(RootCard.CardType.Sappers, 2);
            put(RootCard.CardType.BrutalTactics, 2);
            put(RootCard.CardType.RoyalClaim, 1);
            put(RootCard.CardType.BirdyBindle, 1);
            put(RootCard.CardType.WoodlandRunners, 1);
            put(RootCard.CardType.ArmsTrader, 1);
            put(RootCard.CardType.CrossBow, 1);
            put(RootCard.CardType.Ambush, 2);
            put(RootCard.CardType.Dominance, 1);
        }

    };

    public HashMap<RootCard.CardType, Integer> rabbitCards = new HashMap<>() {
        {
            put(RootCard.CardType.BetterBurrowBank, 2);
            put(RootCard.CardType.Cobbler, 2);
            put(RootCard.CardType.CommandWarren, 2);
            put(RootCard.CardType.BakeSale, 1);
            put(RootCard.CardType.SmugglersTrail, 1);
            put(RootCard.CardType.RootTea, 1);
            put(RootCard.CardType.AVisitToFriends, 1);
            put(RootCard.CardType.FavorOfTheRabbits, 1);
            put(RootCard.CardType.Ambush, 1);
            put(RootCard.CardType.Dominance, 1);
        }
    };

    public HashMap<RootCard.CardType, Integer> mouseCards = new HashMap<>() {
        {
            put(RootCard.CardType.Codebreakers, 2);
            put(RootCard.CardType.ScoutingParty, 2);
            put(RootCard.CardType.CrossBow, 1);
            put(RootCard.CardType.Sword, 1);
            put(RootCard.CardType.TravelGear, 1);
            put(RootCard.CardType.Investments, 1);
            put(RootCard.CardType.FavorOfTheMice, 1);
            put(RootCard.CardType.RootTea, 1);
            put(RootCard.CardType.MouseInASack, 1);
            put(RootCard.CardType.Ambush, 1);
            put(RootCard.CardType.Dominance, 1);
        }
    };

    public HashMap<RootCard.CardType, Integer> foxCards = new HashMap<>() {
        {
            put(RootCard.CardType.StandAndDeliver, 2);
            put(RootCard.CardType.TaxCollector,3);
            put(RootCard.CardType.RootTea,1);
            put(RootCard.CardType.ProtectionRacket,1);
            put(RootCard.CardType.TravelGear,1);
            put(RootCard.CardType.GentlyUsedKnapsack,1);
            put(RootCard.CardType.FavorOfTheFoxes,1);
            put(RootCard.CardType.FoxfolkSteel,1);
            put(RootCard.CardType.Anvil,1);
            put(RootCard.CardType.Ambush,1);
            put(RootCard.CardType.Dominance,1);
        }
    };

    public enum ClearingTypes {
        Rabbit,
        Mouse,
        Fox,
        Bird,
        Forrest,
    }

    public enum Factions {
        MarquiseDeCat,
        EyrieDynasties,
        WoodlandAlliance,
        Vagabond,
    }

    public HashMap<Integer, Integer> SympathyDiscardCost = new HashMap<Integer, Integer>() {
        {
            put(10, 1);
            put(9, 1);
            put(8, 1);
            put(7, 2);
            put(6, 2);
            put(5, 2);
            put(4, 3);
            put(3, 3);
            put(2, 3);
            put(1, 3);
        }
    };
    public HashMap<Factions, Integer> FactionIndexes = new HashMap<Factions, Integer>() {{
        put(Factions.MarquiseDeCat, 1);
        put(Factions.EyrieDynasties, 2);
        put(Factions.WoodlandAlliance, 3);
        put(Factions.Vagabond, 4);
    }};

    public HashMap<Factions, Integer> MaxWarriors = new HashMap<Factions, Integer>() {{
        put(Factions.MarquiseDeCat, 25);
        put(Factions.EyrieDynasties, 20);
        put(Factions.WoodlandAlliance, 10);
        put(Factions.Vagabond, 1);
    }};

    public Factions player0Role = Factions.MarquiseDeCat;
    public Factions player1Role = Factions.EyrieDynasties;
    public Factions player2Role = Factions.WoodlandAlliance;
    public Factions player3Role = Factions.Vagabond;

    public Factions[] indexFactions = new Factions[]{player0Role, player1Role, player2Role, player3Role};

    protected Factions[] getIndexFactions() {
        return indexFactions;
    }

    public Factions getPlayerFaction(int index) {
        return indexFactions[index];
    }

    public int getCatBuildingCost(int current){
        if (current>0 && current<=6){
            return catBuildingCost.get(current);
        }

        return 99999;
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        return this; //The current version does not support changing any of the parameters
    }

    @Override
    protected boolean _equals(Object o) {
        // TODO: compare all variables.
        return o instanceof RootParameters;
    }

    @Override
    public int hashCode() {
        // TODO: include the hashcode of all variables.
        return super.hashCode() + Objects.hash();
    }
}

package games.coltexpress;

import core.AbstractParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.coltexpress.cards.ColtExpressCard;
import utilities.Group;

import java.util.*;

import static games.coltexpress.ColtExpressTypes.LootType.*;
import static games.coltexpress.ColtExpressTypes.CharacterType.*;
import static games.coltexpress.ColtExpressTypes.EndRoundCard.*;
import static games.coltexpress.ColtExpressTypes.RegularRoundCard.*;

import utilities.Pair;

public class ColtExpressParameters extends TunableParameters {

    String dataPath = "data/coltexpress/";

    // Other parameters
    public int nCardsInHand = 6;
    public int nCardsInHandExtraDoc = 1;
    public int nBulletsPerPlayer = 6;
    public int nMaxRounds = 5;
    public int shooterReward = 1000;
    public int nCardsDraw = 3;
    public int nRoofMove = 4;
    public int nCardHostageReward = 250;
    public int nCardTakeItAllReward = 1000;
    public int initialCharacterShuffleSeed = -1;
    public int roundDeckShuffleSeed = -1;
    public int trainShuffleSeed = -1;
    public int playerHandShuffleSeed = -1;

    // How many cards of each type are in a player's deck, total minimum nCardsInHand + nCardsInHandExtraDoc
    public HashMap<ColtExpressCard.CardType, Integer> cardCounts = new HashMap<ColtExpressCard.CardType, Integer>() {{
        put(ColtExpressCard.CardType.MoveSideways, 2);
        put(ColtExpressCard.CardType.MoveVertical, 2);
        put(ColtExpressCard.CardType.Punch, 1);
        put(ColtExpressCard.CardType.MoveMarshal, 1);
        put(ColtExpressCard.CardType.Shoot, 2);
        put(ColtExpressCard.CardType.CollectMoney, 2);
    }};

    // Character types available for this game, minimum nPlayers
    public ColtExpressTypes.CharacterType[] characterTypes = new ColtExpressTypes.CharacterType[]{
            Ghost,
            Cheyenne,
            Django,
            Tuco,
            Doc,
            Belle
    };

    // End round cards available for the game, minimum 1
    public ColtExpressTypes.EndRoundCard[] endRoundCards = new ColtExpressTypes.EndRoundCard[]{
            MarshallsRevenge,
            Hostage,
            PickPocket
    };

    // Regular round cards available for the game, minimum nMaxRounds - 1
    public ColtExpressTypes.RegularRoundCard[] roundCards = new ColtExpressTypes.RegularRoundCard[]{
            AngryMarshall,
            Braking,
            Bridge,
            PassengerRebellion,
            SwivelArm,
            TakeItAll,
            Tunnel
    };

    // Configurations of train compartments available for the game. Values for loot are randomly chosen from those available
    public ArrayList<HashMap<ColtExpressTypes.LootType, Integer>> trainCompartmentConfigurations = new ArrayList<HashMap<ColtExpressTypes.LootType, Integer>>() {{
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(Purse, 1);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(Purse, 2);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(Purse, 3);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(Purse, 1);
            put(ColtExpressTypes.LootType.Jewel, 1);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(Purse, 4);
            put(ColtExpressTypes.LootType.Jewel, 1);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{
            put(ColtExpressTypes.LootType.Jewel, 3);
        }});
        add(new HashMap<ColtExpressTypes.LootType, Integer>() {{  // Locomotive
            put(ColtExpressTypes.LootType.Strongbox, 1);
        }});
    }};

    // List of loot the player starts with, group:
    // - a: type
    // - b: what value
    // - c: how many of this type/value combination
    public ArrayList<Group<ColtExpressTypes.LootType, Integer, Integer>> playerStartLoot = new ArrayList<Group<ColtExpressTypes.LootType, Integer, Integer>>() {{
        add(new Group<>(Purse, 250, 1));
    }};

    // Loot types available for the game. Each type has a list of pairs:
    // - a: what value
    // - b: how many of this type/value combination
    public HashMap<ColtExpressTypes.LootType, ArrayList<Pair<Integer, Integer>>> loot = new HashMap<ColtExpressTypes.LootType, ArrayList<Pair<Integer, Integer>>>() {{
        put(Purse, new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(250, 8));
            add(new Pair<>(300, 2));
            add(new Pair<>(350, 2));
            add(new Pair<>(400, 2));
            add(new Pair<>(450, 2));
            add(new Pair<>(500, 2));
        }});
        put(Jewel, new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(500, 6));
        }});
        put(Strongbox, new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(1000, 2));
        }});
    }};

    public ColtExpressParameters() {
        addTunableParameter("nCardsInHand", 6, Arrays.asList(3,4,5,6,7,8,9,10));
        addTunableParameter("nCardsInHandExtraDoc", 1, Arrays.asList(1,2,3));
        addTunableParameter("nBulletsPerPlayer", 6, Arrays.asList(4,6,8,10,12));
        addTunableParameter("nMaxRounds", 5, Arrays.asList(3, 5, 7, 10));
        addTunableParameter("shooterReward", 1000, Arrays.asList(100, 500, 1000, 1500));
        addTunableParameter("nCardsDraw", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("nRoofMove", 4, Arrays.asList(1,2,3,4,5,6));
        addTunableParameter("nCardHostageReward", 250, Arrays.asList(50, 100, 200, 250, 300, 350, 500));
        addTunableParameter("nCardTakeItAllReward", 1000, Arrays.asList(100, 500, 1000, 1500, 2000));
        for (ColtExpressCard.CardType c: cardCounts.keySet()) {
            addTunableParameter(c.name() + " count", cardCounts.get(c), Arrays.asList(1,2,3,4,5));
        }
        addTunableParameter("initialCharacterShuffleSeed", -1);
        addTunableParameter("roundDeckShuffleSeed", -1);
        addTunableParameter("trainShuffleSeed", -1);
        addTunableParameter("playerHandShuffleSeed", -1);
    }

    @Override
    public void _reset() {
        nCardsInHand = (int) getParameterValue("nCardsInHand");
        nCardsInHandExtraDoc = (int) getParameterValue("nCardsInHandExtraDoc");
        nBulletsPerPlayer = (int) getParameterValue("nBulletsPerPlayer");
        nMaxRounds = (int) getParameterValue("nMaxRounds");
        shooterReward = (int) getParameterValue("shooterReward");
        nCardsDraw = (int) getParameterValue("nCardsDraw");
        nRoofMove = (int) getParameterValue("nRoofMove");
        nCardHostageReward = (int) getParameterValue("nCardHostageReward");
        nCardTakeItAllReward = (int) getParameterValue("nCardTakeItAllReward");
        cardCounts.replaceAll((c, v) -> (Integer) getParameterValue(c.name() + " count"));
        initialCharacterShuffleSeed = (int) getParameterValue("initialCharacterShuffleSeed");
        roundDeckShuffleSeed = (int) getParameterValue("roundDeckShuffleSeed");
        trainShuffleSeed = (int) getParameterValue("trainShuffleSeed");
        playerHandShuffleSeed = (int) getParameterValue("playerHandShuffleSeed");
    }

    @Override
    protected AbstractParameters _copy() {
        ColtExpressParameters cep = new ColtExpressParameters();
        cep.dataPath = dataPath;
        cep.nCardsInHand = nCardsInHand;
        cep.nCardsInHandExtraDoc = nCardsInHandExtraDoc;
        cep.nBulletsPerPlayer = nBulletsPerPlayer;
        cep.nMaxRounds = nMaxRounds;
        cep.shooterReward = shooterReward;
        cep.nCardsDraw = nCardsDraw;
        cep.nRoofMove = nRoofMove;
        cep.cardCounts = new HashMap<>(cardCounts);
        cep.characterTypes = characterTypes.clone();
        cep.endRoundCards = endRoundCards.clone();
        cep.roundCards = roundCards.clone();
        cep.trainCompartmentConfigurations = new ArrayList<>();
        for (HashMap<ColtExpressTypes.LootType, Integer> a: trainCompartmentConfigurations) {
            cep.trainCompartmentConfigurations.add(new HashMap<>(a));
        }
        cep.playerStartLoot = new ArrayList<>();
        for (Group<ColtExpressTypes.LootType, Integer, Integer> g: playerStartLoot) {
            cep.playerStartLoot.add(new Group<>(g.a, g.b, g.c));
        }
        cep.loot = new HashMap<>();
        for (Map.Entry<ColtExpressTypes.LootType, ArrayList<Pair<Integer, Integer>>> e: loot.entrySet()) {
            ArrayList<Pair<Integer, Integer>> values = new ArrayList<>();
            for (Pair<Integer, Integer> p: e.getValue()) {
                values.add(new Pair<>(p.a, p.b));
            }
            cep.loot.put(e.getKey(), values);
        }

        return cep;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColtExpressParameters)) return false;
        ColtExpressParameters that = (ColtExpressParameters) o;
        return nCardsInHand == that.nCardsInHand &&
                nCardsInHandExtraDoc == that.nCardsInHandExtraDoc &&
                nBulletsPerPlayer == that.nBulletsPerPlayer &&
                nMaxRounds == that.nMaxRounds &&
                shooterReward == that.shooterReward &&
                nCardsDraw == that.nCardsDraw &&
                nRoofMove == that.nRoofMove &&
                nCardHostageReward == that.nCardHostageReward &&
                nCardTakeItAllReward == that.nCardTakeItAllReward &&
                Objects.equals(dataPath, that.dataPath) &&
                Objects.equals(cardCounts, that.cardCounts) &&
                Arrays.equals(characterTypes, that.characterTypes) &&
                Arrays.equals(endRoundCards, that.endRoundCards) &&
                Arrays.equals(roundCards, that.roundCards) &&
                Objects.equals(trainCompartmentConfigurations, that.trainCompartmentConfigurations) &&
                Objects.equals(playerStartLoot, that.playerStartLoot) &&
                Objects.equals(loot, that.loot);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), dataPath, trainCompartmentConfigurations, playerStartLoot, loot);
        result = 31 * result + Arrays.hashCode(characterTypes);
        result = 31 * result + Arrays.hashCode(endRoundCards);
        result = 31 * result + Arrays.hashCode(roundCards);
        return result;
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.ColtExpress, new ColtExpressForwardModel(), new ColtExpressGameState(this, GameType.ColtExpress.getMinPlayers()));
    }
}

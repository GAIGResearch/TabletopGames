package games.coltexpress;

import core.AbstractParameters;
import games.coltexpress.cards.ColtExpressCard;
import utilities.Group;

import java.util.*;

import static games.coltexpress.ColtExpressTypes.LootType.*;
import static games.coltexpress.ColtExpressTypes.CharacterType.*;
import static games.coltexpress.ColtExpressTypes.EndRoundCard.*;
import static games.coltexpress.ColtExpressTypes.RegularRoundCard.*;

import utilities.Pair;

public class ColtExpressParameters extends AbstractParameters {

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

    // How many cards of each type are in a player's deck, total minimum nCardsInHand + nCardsInHandExtraDoc
    public HashMap<ColtExpressCard.CardType, Integer> cardCounts = new HashMap<ColtExpressCard.CardType, Integer>() {{
        put(ColtExpressCard.CardType.MoveSideways, 2);
        put(ColtExpressCard.CardType.MoveUp, 2);
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

    public ColtExpressParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        ColtExpressParameters cep = new ColtExpressParameters(System.currentTimeMillis());
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

}

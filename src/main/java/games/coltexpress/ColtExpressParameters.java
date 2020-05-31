package games.coltexpress;

import core.AbstractGameParameters;
import games.coltexpress.cards.ColtExpressCard;
import utilities.Group;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static games.coltexpress.ColtExpressParameters.LootType.Purse;
import static games.coltexpress.ColtExpressTypes.CharacterType.*;
import static games.coltexpress.ColtExpressTypes.EndRoundCard.*;
import static games.coltexpress.ColtExpressTypes.RegularRoundCard.*;

import utilities.Pair;

public class ColtExpressParameters extends AbstractGameParameters {

    // Other parameters
    public int nCardsInHand = 6;
    public int nCardsInHandExtraDoc = 1;
    public int nBulletsPerPlayer = 6;
    public int nMaxRounds = 5;
    public int shooterReward = 1000;
    public int nCardsDraw = 3;
    public int nRoofMove = 4;

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
    public ArrayList<HashMap<LootType, Integer>> trainCompartmentConfigurations = new ArrayList<HashMap<LootType, Integer>>() {{
        add(new HashMap<LootType, Integer>() {{
            put(Purse, 1);
        }});
        add(new HashMap<LootType, Integer>() {{
            put(Purse, 2);
        }});
        add(new HashMap<LootType, Integer>() {{
            put(Purse, 3);
        }});
        add(new HashMap<LootType, Integer>() {{
            put(Purse, 1);
            put(LootType.Jewel, 1);
        }});
        add(new HashMap<LootType, Integer>() {{
            put(Purse, 4);
            put(LootType.Jewel, 1);
        }});
        add(new HashMap<LootType, Integer>() {{
            put(LootType.Jewel, 3);
        }});
        add(new HashMap<LootType, Integer>() {{  // Locomotive
            put(LootType.Strongbox, 1);
        }});
    }};

    // List of loot the player starts with, group:
    // - a: type
    // - b: what value
    // - c: how many of this type/value combination
    public ArrayList<Group<LootType, Integer, Integer>> playerStartLoot = new ArrayList<Group<LootType, Integer, Integer>>() {{
        add(new Group<>(Purse, 250, 1));
    }};

    // Loot types available for this game. The types should not change, but the values can.
    // Each type has a list of pairs:
    // - a: what value
    // - b: how many of this type/value combination
    public enum LootType {
        Purse(new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(250, 8));
            add(new Pair<>(300, 2));
            add(new Pair<>(350, 2));
            add(new Pair<>(400, 2));
            add(new Pair<>(450, 2));
            add(new Pair<>(500, 2));
        }}),
        Jewel(new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(500, 6));
        }}),
        Strongbox(new ArrayList<Pair<Integer, Integer>>() {{
            add(new Pair<>(1000, 2));
        }});

        private ArrayList<Pair<Integer, Integer>> valueList;
        private ArrayList<Integer> pickedCount;
        private ArrayList<Integer> stillAvailableIdx;

        LootType(ArrayList<Pair<Integer, Integer>> valueList){
            this.valueList = valueList;
            pickedCount = new ArrayList<>(valueList.size());
            stillAvailableIdx = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                stillAvailableIdx.add(i);
            }
        }

        public ArrayList<Pair<Integer, Integer>> getValueList() {
            return valueList;
        }

        public int getRandomValue(long seed) {
            Random r = new Random(seed);
            if (stillAvailableIdx.size() > 0) {
                int idx = stillAvailableIdx.get(r.nextInt(stillAvailableIdx.size()));
                return getValue(idx);
            }
            return -1;
        }

        public int getRandomValue() {
            return this.getRandomValue(System.currentTimeMillis());
        }

        public int getDefaultValue() {
            return getValue(0);
        }

        private int getValue(int idx) {
            if (stillAvailableIdx.contains(idx)) {
                pickedCount.set(idx, pickedCount.get(idx) + 1);
                if (pickedCount.get(idx) >= valueList.get(idx).b) {
                    stillAvailableIdx.remove(Integer.valueOf(idx));
                }
                return valueList.get(idx).a;
            }
            return -1;
        }

    }
}

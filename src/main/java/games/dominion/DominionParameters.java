package games.dominion;

import core.*;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.dominion.cards.*;
import org.json.simple.JSONArray;

import java.util.*;

public class DominionParameters extends TunableParameters {

    public List<CardType> cardsUsed = new ArrayList<>();
    String dataPath = "data/dominion/";
    public int initialShuffleSeed = -1;

    public int HAND_SIZE = 5;
    public int PILES_EXHAUSTED_FOR_GAME_END = 3;
    public int KINGDOM_CARDS_OF_EACH_TYPE = 10;
    public int CURSE_CARDS_PER_PLAYER = 10;
    public int STARTING_COPPER = 7;
    public int STARTING_ESTATES = 3;
    public int COPPER_SUPPLY = 32;
    public int SILVER_SUPPLY = 40;
    public int GOLD_SUPPLY = 30;
    public int[] VICTORY_CARDS_PER_PLAYER = {-1, -1, 8, 12, 12}; // 2-4 players only

    // Default cards, based on first game cardset
    public String[] DEFAULT_CARDS = new String[]{
            CardType.CELLAR.toString(),
            CardType.MARKET.toString(),
            CardType.MERCHANT.toString(),
            CardType.MILITIA.toString(),
            CardType.MINE.toString(),
            CardType.MOAT.toString(),
            CardType.REMODEL.toString(),
            CardType.SMITHY.toString(),
            CardType.VILLAGE.toString(),
            CardType.WORKSHOP.toString(),
    };


    public DominionParameters() {
        addTunableParameter("HAND_SIZE", 5, Arrays.asList(3,5,7,10));
        addTunableParameter("PILES_EXHAUSTED_FOR_GAME_END", 3, Arrays.asList(1, 3,5,7,10));
        addTunableParameter("KINGDOM_CARDS_OF_EACH_TYPE", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("CURSE_CARDS_PER_PLAYER", 10, Arrays.asList(5, 10, 15, 20));
        addTunableParameter("STARTING_COPPER", 7, Arrays.asList(3,5,7,10,15));
        addTunableParameter("STARTING_ESTATES", 3, Arrays.asList(1,3,5,7,10));
        addTunableParameter("COPPER_SUPPLY", 32, Arrays.asList(10,20,32,40,50));
        addTunableParameter("SILVER_SUPPLY", 40, Arrays.asList(10,20,30,40,50));
        addTunableParameter("GOLD_SUPPLY", 30, Arrays.asList(10,20,30,40,50));
        addTunableParameter("initialShuffleSeed", -1);
        addStaticParameter("CARDS", Arrays.asList(DEFAULT_CARDS));
        _reset();
    }

    @Override
    public void _reset() {
        HAND_SIZE = (int) getParameterValue("HAND_SIZE");
        PILES_EXHAUSTED_FOR_GAME_END = (int) getParameterValue("PILES_EXHAUSTED_FOR_GAME_END");
        KINGDOM_CARDS_OF_EACH_TYPE = (int) getParameterValue("KINGDOM_CARDS_OF_EACH_TYPE");
        CURSE_CARDS_PER_PLAYER = (int) getParameterValue("CURSE_CARDS_PER_PLAYER");
        STARTING_COPPER = (int) getParameterValue("STARTING_COPPER");
        STARTING_ESTATES = (int) getParameterValue("STARTING_ESTATES");
        COPPER_SUPPLY = (int) getParameterValue("COPPER_SUPPLY");
        SILVER_SUPPLY = (int) getParameterValue("SILVER_SUPPLY");
        GOLD_SUPPLY = (int) getParameterValue("GOLD_SUPPLY");
        initialShuffleSeed = (int) getParameterValue("initialShuffleSeed");

        cardsUsed.clear();
        List<Object> cardsJSON = (List<Object>) getParameterValue("CARDS");
        //if (!cardsJSON.isEmpty() && cardsJSON.size() != 10) throw new IllegalArgumentException("CardsUsed has a size of " + cardsJSON.size() + " instead of 10");
        for (Object o : cardsJSON) {
            CardType card = CardType.valueOf((String) o);
            cardsUsed.add(card);
        }
    }

    public DominionParameters(String[] cards) {
        for (String cardName : cards) {
            try {
                cardsUsed.add(CardType.valueOf(cardName));
            } catch (Exception e) {
                System.out.println("Error initialising Parameters with " + cardName);
                System.out.println(e.getMessage());
            }
        }
    }

    // Used by unit tests only
    public static DominionParameters sizeDistortion() {
        DominionParameters retValue = new DominionParameters();
        retValue.cardsUsed.add(CardType.ARTISAN);
        retValue.cardsUsed.add(CardType.BANDIT);
        retValue.cardsUsed.add(CardType.BUREAUCRAT);
        retValue.cardsUsed.add(CardType.CHAPEL);
        retValue.cardsUsed.add(CardType.FESTIVAL);
        retValue.cardsUsed.add(CardType.GARDENS);
        retValue.cardsUsed.add(CardType.SENTRY);
        retValue.cardsUsed.add(CardType.THRONE_ROOM);
        retValue.cardsUsed.add(CardType.WITCH);
        retValue.cardsUsed.add(CardType.CURSE);
        retValue.cardsUsed.add(CardType.WORKSHOP);
        return retValue;
    }

    // Used by unit tests only
    public static DominionParameters improvements() {
        DominionParameters retValue = new DominionParameters();
        retValue.cardsUsed.add(CardType.ARTISAN);
        retValue.cardsUsed.add(CardType.CELLAR);
        retValue.cardsUsed.add(CardType.MARKET);
        retValue.cardsUsed.add(CardType.MERCHANT);
        retValue.cardsUsed.add(CardType.MINE);
        retValue.cardsUsed.add(CardType.MOAT);
        retValue.cardsUsed.add(CardType.MONEYLENDER);
        retValue.cardsUsed.add(CardType.POACHER);
        retValue.cardsUsed.add(CardType.REMODEL);
        retValue.cardsUsed.add(CardType.WITCH);
        retValue.cardsUsed.add(CardType.CURSE);
        // Note that the three Victory cards and three Treasure cards are always included
        return retValue;
    }


    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     *
     * @return - new game parameters object.
     */
    @Override
    protected AbstractParameters _copy() {
        DominionParameters retValue = new DominionParameters();
        retValue.cardsUsed = new ArrayList<>(cardsUsed);
        retValue.VICTORY_CARDS_PER_PLAYER = VICTORY_CARDS_PER_PLAYER.clone();
        return retValue;
    }

    /**
     * Checks if the given object is the same as the current.
     *
     * @param o - other object to test equals for.
     * @return true if the two objects are equal, false otherwise
     */
    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DominionParameters) {
            DominionParameters dp = (DominionParameters) o;
            return dp.cardsUsed.equals(cardsUsed) &&
                    Arrays.equals(dp.VICTORY_CARDS_PER_PLAYER, VICTORY_CARDS_PER_PLAYER);
        }
        return false;
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Dominion, new DominionForwardModel(), new DominionGameState(this, GameType.Dominion.getMinPlayers()));
    }
}

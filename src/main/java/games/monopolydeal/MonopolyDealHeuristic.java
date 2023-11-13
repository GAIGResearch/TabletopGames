package games.monopolydeal;

import core.AbstractGameState;
import core.AbstractParameters;
import core.components.Deck;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.monopolydeal.cards.CardType;
import games.monopolydeal.cards.MonopolyDealCard;
import games.monopolydeal.cards.PropertySet;
import games.monopolydeal.cards.SetType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MonopolyDealHeuristic extends TunableParameters implements IStateHeuristic {

    int BANK_VALUE_1 = 1;
    int BANK_VALUE_2 = 2;
    int BANK_VALUE_3 = 2;
    int BANK_VALUE_4 = 3;
    int BANK_VALUE_5 = 3;
    int BANK_VALUE_10 = 5;

    int BROWN_VALUE = 1;
    int LIGHTBLUE_VALUE = 1;
    int PINK_VALUE = 2;
    int ORANGE_VALUE = 2;
    int RED_VALUE = 3;
    int YELLOW_VALUE = 3;
    int GREEN_VALUE = 4;
    int BLUE_VALUE = 4;
    int RAILROAD_VALUE = 1;
    int UTILITY_VALUE = 1;


    public MonopolyDealHeuristic(){
        addTunableParameter("BANK_VALUE_1", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BANK_VALUE_2", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BANK_VALUE_3", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BANK_VALUE_4", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BANK_VALUE_5", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BANK_VALUE_10", 5, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BROWN_VALUE", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("LIGHTBLUE_VALUE", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("PINK_VALUE", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("ORANGE_VALUE", 2, Arrays.asList(1,2,3,4,5));
        addTunableParameter("RED_VALUE", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("YELLOW_VALUE", 3, Arrays.asList(1,2,3,4,5));
        addTunableParameter("GREEN_VALUE", 4, Arrays.asList(1,2,3,4,5));
        addTunableParameter("BLUE_VALUE", 4, Arrays.asList(1,2,3,4,5));
        addTunableParameter("RAILROAD_VALUE", 1, Arrays.asList(1,2,3,4,5));
        addTunableParameter("UTILITY_VALUE", 1, Arrays.asList(1,2,3,4,5));
        insertValues();
    }
    @Override
    public void _reset() {
        BANK_VALUE_1 = (int) getParameterValue("BANK_VALUE_1");
        BANK_VALUE_2 = (int) getParameterValue("BANK_VALUE_2");
        BANK_VALUE_3 = (int) getParameterValue("BANK_VALUE_3");
        BANK_VALUE_4 = (int) getParameterValue("BANK_VALUE_4");
        BANK_VALUE_5 = (int) getParameterValue("BANK_VALUE_5");
        BANK_VALUE_10 = (int) getParameterValue("BANK_VALUE_10");
        BROWN_VALUE = (int) getParameterValue("BROWN_VALUE");
        LIGHTBLUE_VALUE = (int) getParameterValue("LIGHTBLUE_VALUE");
        PINK_VALUE = (int) getParameterValue("PINK_VALUE");
        ORANGE_VALUE = (int) getParameterValue("ORANGE_VALUE");
        RED_VALUE = (int) getParameterValue("RED_VALUE");
        YELLOW_VALUE = (int) getParameterValue("YELLOW_VALUE");
        GREEN_VALUE = (int) getParameterValue("GREEN_VALUE");
        BLUE_VALUE = (int) getParameterValue("BLUE_VALUE");
        RAILROAD_VALUE = (int) getParameterValue("RAILROAD_VALUE");
        UTILITY_VALUE = (int) getParameterValue("UTILITY_VALUE");
    }
    HashMap<CardType,Integer> cardValue = new HashMap<>();
    HashMap<SetType, Integer> setValue = new HashMap<>();

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double[] scores = new double[gs.getNPlayers()];
        MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
        for(int i=0; i<gs.getNPlayers(); i++) scores[i] = playerHeuristicScore(MDGS,playerId);
        double minScore = -99.0d, maxScore = 99.0d;
        for(int i=0; i<gs.getNPlayers();i++){
            if(minScore>scores[i]) minScore = scores[i];
            if(maxScore<scores[i]) maxScore = scores[i];
        }
        return ((scores[playerId]-minScore)/(maxScore-minScore)*2)-1;
    }
    double playerHeuristicScore(MonopolyDealGameState MDGS, int playerID){
        Deck<MonopolyDealCard> playerBank = MDGS.getPlayerBank(playerID);
        PropertySet[] propertySets = MDGS.getPropertySets(playerID);

        // split value for Sets and bank
        int propertyValue = 0, bankValue = 0;
        for(int i=0;i<playerBank.getSize();i++) bankValue = bankValue + cardValue.get(playerBank.get(i).cardType());
        for (PropertySet pSet: propertySets) propertyValue = propertyValue + (setValue.get(pSet.getSetType()) * pSet.getSize());

        return bankValue/(1.0 *(MDGS.getRoundCounter()+1)) + propertyValue;
    }
    private void insertValues(){
        // Money values
        cardValue.put(CardType.Money10,BANK_VALUE_10);
        cardValue.put(CardType.Money1,BANK_VALUE_1);
        cardValue.put(CardType.Money2,BANK_VALUE_2);
        cardValue.put(CardType.Money3,BANK_VALUE_3);
        cardValue.put(CardType.Money4,BANK_VALUE_4);
        cardValue.put(CardType.Money5,BANK_VALUE_5);

        //Action Cards
        cardValue.put(CardType.PassGo,BANK_VALUE_1);
        cardValue.put(CardType.DoubleTheRent,BANK_VALUE_10);

        cardValue.put(CardType.ForcedDeal,BANK_VALUE_3);
        cardValue.put(CardType.SlyDeal,BANK_VALUE_3);

        cardValue.put(CardType.DebtCollector,BANK_VALUE_3);
        cardValue.put(CardType.ItsMyBirthday,BANK_VALUE_2);

        cardValue.put(CardType.House,BANK_VALUE_3);
        cardValue.put(CardType.Hotel,BANK_VALUE_4);

        cardValue.put(CardType.DealBreaker,BANK_VALUE_5);
        cardValue.put(CardType.JustSayNo,BANK_VALUE_4);

        //Rent Cards
        cardValue.put(CardType.MulticolorRent,BANK_VALUE_1);
        cardValue.put(CardType.GreenBlueRent,BANK_VALUE_1);
        cardValue.put(CardType.BrownLightBlueRent,BANK_VALUE_1);
        cardValue.put(CardType.PinkOrangeRent,BANK_VALUE_1);
        cardValue.put(CardType.RedYellowRent,BANK_VALUE_1);
        cardValue.put(CardType.RailRoadUtilityRent,BANK_VALUE_1);

        setValue.put(SetType.Brown,BROWN_VALUE);
        setValue.put(SetType.Blue,BLUE_VALUE);
        setValue.put(SetType.Green,GREEN_VALUE);
        setValue.put(SetType.LightBlue,LIGHTBLUE_VALUE);
        setValue.put(SetType.Orange,ORANGE_VALUE);
        setValue.put(SetType.Pink,PINK_VALUE);
        setValue.put(SetType.RailRoad,RAILROAD_VALUE);
        setValue.put(SetType.Utility,UTILITY_VALUE);
        setValue.put(SetType.Red,RED_VALUE);
        setValue.put(SetType.Yellow,YELLOW_VALUE);
        setValue.put(SetType.UNDEFINED,0);

    }

    @Override
    protected AbstractParameters _copy() {
        MonopolyDealHeuristic heuristic = new MonopolyDealHeuristic();
        heuristic.BANK_VALUE_1 = BANK_VALUE_1;
        heuristic.BANK_VALUE_2 = BANK_VALUE_2;
        heuristic.BANK_VALUE_3 = BANK_VALUE_3;
        heuristic.BANK_VALUE_4 = BANK_VALUE_4;
        heuristic.BANK_VALUE_5 = BANK_VALUE_5;
        heuristic.BANK_VALUE_10 = BANK_VALUE_10;

        heuristic.BROWN_VALUE = BROWN_VALUE;
        heuristic.LIGHTBLUE_VALUE = LIGHTBLUE_VALUE;
        heuristic.PINK_VALUE = PINK_VALUE;
        heuristic.ORANGE_VALUE = ORANGE_VALUE;
        heuristic.RED_VALUE = RED_VALUE;
        heuristic.YELLOW_VALUE = YELLOW_VALUE;
        heuristic.GREEN_VALUE = GREEN_VALUE;
        heuristic.BLUE_VALUE = BLUE_VALUE;
        heuristic.RAILROAD_VALUE = RAILROAD_VALUE;
        heuristic.UTILITY_VALUE = UTILITY_VALUE;

        return heuristic;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MonopolyDealHeuristic that = (MonopolyDealHeuristic) o;
        return BANK_VALUE_1 == that.BANK_VALUE_1 && BANK_VALUE_2 == that.BANK_VALUE_2 && BANK_VALUE_3 == that.BANK_VALUE_3 && BANK_VALUE_4 == that.BANK_VALUE_4 && BANK_VALUE_5 == that.BANK_VALUE_5 && BANK_VALUE_10 == that.BANK_VALUE_10 && BROWN_VALUE == that.BROWN_VALUE && LIGHTBLUE_VALUE == that.LIGHTBLUE_VALUE && PINK_VALUE == that.PINK_VALUE && ORANGE_VALUE == that.ORANGE_VALUE && RED_VALUE == that.RED_VALUE && YELLOW_VALUE == that.YELLOW_VALUE && GREEN_VALUE == that.GREEN_VALUE && BLUE_VALUE == that.BLUE_VALUE && RAILROAD_VALUE == that.RAILROAD_VALUE && UTILITY_VALUE == that.UTILITY_VALUE && Objects.equals(cardValue, that.cardValue) && Objects.equals(setValue, that.setValue);
    }

    @Override
    public Object instantiate() {
        return _copy();
    }
}

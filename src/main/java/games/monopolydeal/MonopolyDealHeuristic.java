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
import java.util.Objects;

public class MonopolyDealHeuristic extends TunableParameters implements IStateHeuristic {

    //Bank values
    int BANK_VALUE_1 = 1;
    int BANK_VALUE_2 = 2;
    int BANK_VALUE_3 = 2;
    int BANK_VALUE_4 = 3;
    int BANK_VALUE_5 = 3;
    int BANK_VALUE_10 = 5;

    //PropertySet values
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
    int COMPLETESET_VALUE = 5;

    //Hand values
    int HAND_SLYDEAL = 3;
    int HAND_FORCEDDEAL = 2;
    int HAND_DEBTCOLLECTOR = 3;
    int HAND_ITSMYBIRTHDAY = 3;
    int HAND_DEALBREAKER = 4;
    int HAND_JUSTSAYNO = 4;
    int HAND_MULTICOLORRENT = 2;
    int HAND_PROPERTYRENT = 2;
    int HAND_MULTICOLORWILD = 4;



    public MonopolyDealHeuristic(){
        addTunableParameter("BANK_VALUE_1", 1, Arrays.asList(1,2));
        addTunableParameter("BANK_VALUE_2", 2, Arrays.asList(2,3));
        addTunableParameter("BANK_VALUE_3", 2, Arrays.asList(2,3));
        addTunableParameter("BANK_VALUE_4", 3, Arrays.asList(2,3,4));
        addTunableParameter("BANK_VALUE_5", 3, Arrays.asList(2,3,4));
        addTunableParameter("BANK_VALUE_10", 5, Arrays.asList(3,4,5));
        addTunableParameter("BROWN_VALUE", 1, Arrays.asList(1,2,3));
        addTunableParameter("LIGHTBLUE_VALUE", 1, Arrays.asList(1,2,3));
        addTunableParameter("PINK_VALUE", 2, Arrays.asList(1,2,3));
        addTunableParameter("ORANGE_VALUE", 2, Arrays.asList(1,2,3));
        addTunableParameter("RED_VALUE", 3, Arrays.asList(2,3,4));
        addTunableParameter("YELLOW_VALUE", 3, Arrays.asList(2,3,4));
        addTunableParameter("GREEN_VALUE", 4, Arrays.asList(3,4,5));
        addTunableParameter("BLUE_VALUE", 4, Arrays.asList(3,4,5));
        addTunableParameter("RAILROAD_VALUE", 1, Arrays.asList(1,2,3));
        addTunableParameter("UTILITY_VALUE", 1, Arrays.asList(1,2,3));
        addTunableParameter("HAND_SLYDEAL", 3, Arrays.asList(2,3,4));
        addTunableParameter("HAND_FORCEDDEAL", 2, Arrays.asList(1,2,3));
        addTunableParameter("HAND_DEBTCOLLECTOR", 3, Arrays.asList(2,3,4));
        addTunableParameter("HAND_ITSMYBIRTHDAY", 3, Arrays.asList(2,3,4));
        addTunableParameter("HAND_DEALBREAKER", 4, Arrays.asList(3,4,5));
        addTunableParameter("HAND_JUSTSAYNO", 4, Arrays.asList(3,4,5));
        addTunableParameter("HAND_MULTICOLORRENT", 2, Arrays.asList(1,2,3));
        addTunableParameter("HAND_PROPERTYRENT", 2, Arrays.asList(1,2,3));
        addTunableParameter("COMPLETESET_VALUE", 5, Arrays.asList(3,5,7));
        addTunableParameter("HAND_MULTICOLORWILD", 4, Arrays.asList(3,4,5));
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
        HAND_SLYDEAL = (int) getParameterValue("HAND_SLYDEAL");
        HAND_FORCEDDEAL = (int) getParameterValue("HAND_FORCEDDEAL");
        HAND_DEBTCOLLECTOR = (int) getParameterValue("HAND_DEBTCOLLECTOR");
        HAND_ITSMYBIRTHDAY = (int) getParameterValue("HAND_ITSMYBIRTHDAY");
        HAND_DEALBREAKER = (int) getParameterValue("HAND_DEALBREAKER");
        HAND_JUSTSAYNO = (int) getParameterValue("HAND_JUSTSAYNO");
        HAND_MULTICOLORRENT = (int) getParameterValue("HAND_MULTICOLORRENT");
        HAND_PROPERTYRENT = (int) getParameterValue("HAND_PROPERTYRENT");
        COMPLETESET_VALUE = (int) getParameterValue("COMPLETESET_VALUE");
        HAND_MULTICOLORWILD = (int) getParameterValue("HAND_MULTICOLORWILD");
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
        return ((scores[playerId]-minScore)/(maxScore-minScore)*2.0)-1;
    }
    double playerHeuristicScore(MonopolyDealGameState MDGS, int playerID){
        Deck<MonopolyDealCard> playerBank = MDGS.getPlayerBank(playerID);
        PropertySet[] propertySets = MDGS.getPropertySets(playerID);
        Deck<MonopolyDealCard> playerHand = MDGS.getPlayerHand(playerID);

        // split value for Sets and bank
        int propertyValue = 0, bankValue = 0, pHandValue = 0;
        for(int i=0;i<playerBank.getSize();i++)
            bankValue = bankValue + cardValue.get(playerBank.get(i).cardType());
        for (PropertySet pSet: propertySets) {
            if(pSet.isComplete)
                propertyValue = propertyValue + COMPLETESET_VALUE;
            propertyValue = propertyValue + (setValue.get(pSet.getSetType()) * pSet.getSize());
        }
        for(int i=0; i<playerHand.getSize(); i++){
            switch (playerHand.get(i).cardType()){
                case MulticolorWild:
                    pHandValue = pHandValue + HAND_MULTICOLORWILD;
                    break;
                case SlyDeal:
                    pHandValue = pHandValue + HAND_SLYDEAL;
                    break;
                case ForcedDeal:
                    pHandValue = pHandValue + HAND_FORCEDDEAL;
                    break;
                case DebtCollector:
                    pHandValue = pHandValue + HAND_DEBTCOLLECTOR;
                    break;
                case ItsMyBirthday:
                    pHandValue = pHandValue + HAND_ITSMYBIRTHDAY;
                    break;
                case DealBreaker:
                    pHandValue = pHandValue + HAND_DEALBREAKER;
                    break;
                case JustSayNo:
                    pHandValue = pHandValue + HAND_JUSTSAYNO;
                    break;
                case MulticolorRent:
                    pHandValue = pHandValue + HAND_MULTICOLORRENT;
                    break;
                case BrownLightBlueRent:
                case PinkOrangeRent:
                case RedYellowRent:
                case GreenBlueRent:
                case RailRoadUtilityRent:
                    pHandValue = pHandValue + HAND_PROPERTYRENT;
                    break;
                default:
                    break;

            }
        }
        return bankValue/(Math.sqrt((MDGS.getRoundCounter()+1)*1.0))
                + propertyValue + pHandValue;
    }
    private void insertValues(){
        // Money values
        cardValue.put(CardType.Money10,BANK_VALUE_10);
        cardValue.put(CardType.Money1,BANK_VALUE_1);
        cardValue.put(CardType.Money2,BANK_VALUE_2);
        cardValue.put(CardType.Money3,BANK_VALUE_3);
        cardValue.put(CardType.Money4,BANK_VALUE_4);
        cardValue.put(CardType.Money5,BANK_VALUE_5);

        // Action Cards
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

        // Rent Cards
        cardValue.put(CardType.MulticolorRent,BANK_VALUE_1);
        cardValue.put(CardType.GreenBlueRent,BANK_VALUE_1);
        cardValue.put(CardType.BrownLightBlueRent,BANK_VALUE_1);
        cardValue.put(CardType.PinkOrangeRent,BANK_VALUE_1);
        cardValue.put(CardType.RedYellowRent,BANK_VALUE_1);
        cardValue.put(CardType.RailRoadUtilityRent,BANK_VALUE_1);

        // PropertySet values
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

        heuristic.HAND_SLYDEAL = HAND_SLYDEAL;
        heuristic.HAND_FORCEDDEAL = HAND_FORCEDDEAL;
        heuristic.HAND_DEBTCOLLECTOR = HAND_DEBTCOLLECTOR;
        heuristic.HAND_ITSMYBIRTHDAY = HAND_ITSMYBIRTHDAY;
        heuristic.HAND_DEALBREAKER = HAND_DEALBREAKER;
        heuristic.HAND_JUSTSAYNO = HAND_JUSTSAYNO;
        heuristic.HAND_MULTICOLORRENT = HAND_MULTICOLORRENT;
        heuristic.HAND_PROPERTYRENT = HAND_PROPERTYRENT;
        heuristic.COMPLETESET_VALUE = COMPLETESET_VALUE;
        heuristic.HAND_MULTICOLORWILD = HAND_MULTICOLORWILD;

        return heuristic;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MonopolyDealHeuristic heuristic = (MonopolyDealHeuristic) o;
        return BANK_VALUE_1 == heuristic.BANK_VALUE_1 && BANK_VALUE_2 == heuristic.BANK_VALUE_2 && BANK_VALUE_3 == heuristic.BANK_VALUE_3 && BANK_VALUE_4 == heuristic.BANK_VALUE_4 && BANK_VALUE_5 == heuristic.BANK_VALUE_5 && BANK_VALUE_10 == heuristic.BANK_VALUE_10 && BROWN_VALUE == heuristic.BROWN_VALUE && LIGHTBLUE_VALUE == heuristic.LIGHTBLUE_VALUE && PINK_VALUE == heuristic.PINK_VALUE && ORANGE_VALUE == heuristic.ORANGE_VALUE && RED_VALUE == heuristic.RED_VALUE && YELLOW_VALUE == heuristic.YELLOW_VALUE && GREEN_VALUE == heuristic.GREEN_VALUE && BLUE_VALUE == heuristic.BLUE_VALUE && RAILROAD_VALUE == heuristic.RAILROAD_VALUE && UTILITY_VALUE == heuristic.UTILITY_VALUE && COMPLETESET_VALUE == heuristic.COMPLETESET_VALUE && HAND_SLYDEAL == heuristic.HAND_SLYDEAL && HAND_FORCEDDEAL == heuristic.HAND_FORCEDDEAL && HAND_DEBTCOLLECTOR == heuristic.HAND_DEBTCOLLECTOR && HAND_ITSMYBIRTHDAY == heuristic.HAND_ITSMYBIRTHDAY && HAND_DEALBREAKER == heuristic.HAND_DEALBREAKER && HAND_JUSTSAYNO == heuristic.HAND_JUSTSAYNO && HAND_MULTICOLORRENT == heuristic.HAND_MULTICOLORRENT && HAND_PROPERTYRENT == heuristic.HAND_PROPERTYRENT && HAND_MULTICOLORWILD == heuristic.HAND_MULTICOLORWILD && Objects.equals(cardValue, heuristic.cardValue) && Objects.equals(setValue, heuristic.setValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), BANK_VALUE_1, BANK_VALUE_2, BANK_VALUE_3, BANK_VALUE_4, BANK_VALUE_5, BANK_VALUE_10, BROWN_VALUE, LIGHTBLUE_VALUE, PINK_VALUE, ORANGE_VALUE, RED_VALUE, YELLOW_VALUE, GREEN_VALUE, BLUE_VALUE, RAILROAD_VALUE, UTILITY_VALUE, COMPLETESET_VALUE, HAND_SLYDEAL, HAND_FORCEDDEAL, HAND_DEBTCOLLECTOR, HAND_ITSMYBIRTHDAY, HAND_DEALBREAKER, HAND_JUSTSAYNO, HAND_MULTICOLORRENT, HAND_PROPERTYRENT, HAND_MULTICOLORWILD, cardValue, setValue);
    }

    @Override
    public Object instantiate() {
        return _copy();
    }
}

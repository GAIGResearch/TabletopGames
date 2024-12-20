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

import static games.monopolydeal.MonopolyDealHeuristicType.*;

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

    MonopolyDealHeuristicType HEURISTIC_TYPE = ALL;

    public MonopolyDealHeuristic(int type){
        this();
        HEURISTIC_TYPE = MonopolyDealHeuristicType.values()[type];
    }
    public MonopolyDealHeuristic(){
        addTunableParameter("HEURISTIC_TYPE",ALL,Arrays.asList(MonopolyDealHeuristicType.values()));
        insertValues();
    }
    @Override
    public void _reset() {
        HEURISTIC_TYPE = (MonopolyDealHeuristicType) getParameterValue("HEURISTIC_TYPE");
    }
    HashMap<CardType,Integer> cardValue = new HashMap<>();
    HashMap<SetType, Integer> setValue = new HashMap<>();

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        if (gs.isNotTerminal()) {
            double[] scores = new double[gs.getNPlayers()];
            MonopolyDealGameState MDGS = (MonopolyDealGameState) gs;
            for (int i = 0; i < gs.getNPlayers(); i++) scores[i] = playerHeuristicScore(MDGS, playerId);
            double maxOther = 0;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (i != playerId) {
                    double neg = (gs.getGameScore(i) + scores[i])/2.;
                    maxOther += neg;
//                    if (neg > maxOther) maxOther = neg;
                }
            }
            return (gs.getGameScore(playerId) + scores[playerId])/2.;// - maxOther/(gs.getNPlayers()-1);
        } else {
            // The game finished, we can instead return the actual result of the game for the given player.
            return gs.getPlayerResults()[playerId].value;
        }
    }
    double playerHeuristicScore(MonopolyDealGameState MDGS, int playerID){
        switch (HEURISTIC_TYPE){
            case PROPERTYONLY:
                return getPropertyValue(MDGS, playerID);
            case PROPERTYBANK:
                return (getBankValue(MDGS, playerID) + getPropertyValue(MDGS, playerID))/2.;
            case PROPERTYHAND:
                return (getPropertyValue(MDGS, playerID) + getPlayerHandValue(MDGS,playerID))/2.;
            case BASICALL:
                return (getPropertyValue(MDGS, playerID) + getPlayerHandValue(MDGS,playerID) + getBankValue(MDGS, playerID))/2.;
            case ALL:
                return (getBankValue(MDGS, playerID)/(Math.sqrt((MDGS.getRoundCounter()+1)*1.0))
                        + getPropertyValue(MDGS, playerID) + getPlayerHandValue(MDGS,playerID))/3;
            default:
                throw new AssertionError("Not yet implemented");
        }
    }

    private double getPropertyValue(MonopolyDealGameState MDGS, int playerID){
        double idealPropertyValue = 50.;
        PropertySet[] propertySets = MDGS.getPropertySets(playerID);
        int propertyValue = 0;
        for (PropertySet pSet: propertySets) {
            int val = setValue.get(pSet.getSetType()) * pSet.getSize();
            if(pSet.isComplete)
                val += COMPLETESET_VALUE;
            propertyValue += val;
        }
        return Math.min(1.0,propertyValue / idealPropertyValue);
    }

    private double getBankValue(MonopolyDealGameState MDGS, int playerID){
        double maxBankValue = 50.;
//        Deck<MonopolyDealCard> playerBank = MDGS.getPlayerBank(playerID);
//        int bankValue = 0;
//        for(int i=0;i<playerBank.getSize();i++)
//            bankValue += cardValue.get(playerBank.get(i).cardType());
        return Math.min(1, MDGS.getBankValue(playerID)/maxBankValue);
    }

    private double getPlayerHandValue(MonopolyDealGameState MDGS, int playerID){
        double maxValue = 28;  // 7 max, 4 biggest value
        Deck<MonopolyDealCard> playerHand = MDGS.getPlayerHand(playerID);
        int pHandValue = 0;
        for(int i=0; i<playerHand.getSize(); i++){
            int value = 0;
            switch (playerHand.get(i).cardType()){
                case MulticolorWild:
                    value = HAND_MULTICOLORWILD;
                    break;
                case SlyDeal:
                    value = HAND_SLYDEAL;
                    break;
                case ForcedDeal:
                    value = HAND_FORCEDDEAL;
                    break;
                case DebtCollector:
                    value =  HAND_DEBTCOLLECTOR;
                    break;
                case ItsMyBirthday:
                    value = HAND_ITSMYBIRTHDAY;
                    break;
                case DealBreaker:
                    value = HAND_DEALBREAKER;
                    break;
                case JustSayNo:
                    value = HAND_JUSTSAYNO;
                    break;
                case MulticolorRent:
                    value = HAND_MULTICOLORRENT;
                    break;
                case BrownLightBlueRent:
                case PinkOrangeRent:
                case RedYellowRent:
                case GreenBlueRent:
                case RailRoadUtilityRent:
                    value = HAND_PROPERTYRENT;
                    break;
                default:
                    break;

            }
            pHandValue += value;
        }
        return Math.min(1.0, pHandValue/maxValue);
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

        heuristic.HEURISTIC_TYPE = HEURISTIC_TYPE;

        return heuristic;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonopolyDealHeuristic)) return false;
        if (!super.equals(o)) return false;
        MonopolyDealHeuristic that = (MonopolyDealHeuristic) o;
        return BANK_VALUE_1 == that.BANK_VALUE_1 && BANK_VALUE_2 == that.BANK_VALUE_2 && BANK_VALUE_3 == that.BANK_VALUE_3 && BANK_VALUE_4 == that.BANK_VALUE_4 && BANK_VALUE_5 == that.BANK_VALUE_5 && BANK_VALUE_10 == that.BANK_VALUE_10 && BROWN_VALUE == that.BROWN_VALUE && LIGHTBLUE_VALUE == that.LIGHTBLUE_VALUE && PINK_VALUE == that.PINK_VALUE && ORANGE_VALUE == that.ORANGE_VALUE && RED_VALUE == that.RED_VALUE && YELLOW_VALUE == that.YELLOW_VALUE && GREEN_VALUE == that.GREEN_VALUE && BLUE_VALUE == that.BLUE_VALUE && RAILROAD_VALUE == that.RAILROAD_VALUE && UTILITY_VALUE == that.UTILITY_VALUE && COMPLETESET_VALUE == that.COMPLETESET_VALUE && HAND_SLYDEAL == that.HAND_SLYDEAL && HAND_FORCEDDEAL == that.HAND_FORCEDDEAL && HAND_DEBTCOLLECTOR == that.HAND_DEBTCOLLECTOR && HAND_ITSMYBIRTHDAY == that.HAND_ITSMYBIRTHDAY && HAND_DEALBREAKER == that.HAND_DEALBREAKER && HAND_JUSTSAYNO == that.HAND_JUSTSAYNO && HAND_MULTICOLORRENT == that.HAND_MULTICOLORRENT && HAND_PROPERTYRENT == that.HAND_PROPERTYRENT && HAND_MULTICOLORWILD == that.HAND_MULTICOLORWILD && HEURISTIC_TYPE == that.HEURISTIC_TYPE && Objects.equals(cardValue, that.cardValue) && Objects.equals(setValue, that.setValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), BANK_VALUE_1, BANK_VALUE_2, BANK_VALUE_3, BANK_VALUE_4, BANK_VALUE_5, BANK_VALUE_10, BROWN_VALUE, LIGHTBLUE_VALUE, PINK_VALUE, ORANGE_VALUE, RED_VALUE, YELLOW_VALUE, GREEN_VALUE, BLUE_VALUE, RAILROAD_VALUE, UTILITY_VALUE, COMPLETESET_VALUE, HAND_SLYDEAL, HAND_FORCEDDEAL, HAND_DEBTCOLLECTOR, HAND_ITSMYBIRTHDAY, HAND_DEALBREAKER, HAND_JUSTSAYNO, HAND_MULTICOLORRENT, HAND_PROPERTYRENT, HAND_MULTICOLORWILD, HEURISTIC_TYPE, cardValue, setValue);
    }

    @Override
    public Object instantiate() {
        return _copy();
    }

}

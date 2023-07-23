package games.monopolydeal;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import games.GameType;
import games.monopolydeal.cards.CardType;
import evaluation.TunableParameters;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>This class should hold a series of variables representing game parameters (e.g. number of cards dealt to players,
 * maximum number of rounds in the game etc.). These parameters should be used everywhere in the code instead of
 * local variables or hard-coded numbers, by accessing these parameters from the game state via {@link AbstractGameState#getGameParameters()}.</p>
 *
 * <p>It should then implement appropriate {@link #_copy()}, {@link #_equals(Object)} and {@link #hashCode()} functions.</p>
 *
 * <p>The class can optionally extend from {@link evaluation.TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
public class MonopolyDealParameters extends TunableParameters {

    public int HAND_SIZE;
    public int DRAWS_WHEN_EMPTY;
    public int INITIAL_DEAL;
    public int ACTIONS_PER_TURN;
    public int DRAWS_PER_TURN;
    public int SETS_TO_WIN;
    Map<CardType, Integer> cardsIncludedInGame = new HashMap<>();
    public MonopolyDealParameters(long seed) {
        super(seed);
        addTunableParameter("HAND_SIZE", 7, Arrays.asList(3,5,7,10));
        addTunableParameter("INITIAL_DEAL", 5, Arrays.asList(3,5));
        addTunableParameter("DRAWS_PER_TURN", 2, Arrays.asList(3,5,7,10));
        addTunableParameter("DRAWS_WHEN_EMPTY", 5, Arrays.asList(3,5,7,10));
        addTunableParameter("SETS_TO_WIN", 3, Arrays.asList(3,5,7,10));
        addTunableParameter("ACTIONS_PER_TURN", 3, Arrays.asList(3,5,7,10));
        _reset();
    }

    @Override
    public void _reset() {
        HAND_SIZE = (int) getParameterValue("HAND_SIZE");
        INITIAL_DEAL = (int) getParameterValue("INITIAL_DEAL");
        ACTIONS_PER_TURN = (int) getParameterValue("ACTIONS_PER_TURN");
        DRAWS_WHEN_EMPTY = (int) getParameterValue("DRAWS_WHEN_EMPTY");
        DRAWS_PER_TURN = (int) getParameterValue("DRAWS_PER_TURN");
        SETS_TO_WIN = (int) getParameterValue("SETS_TO_WIN");
        addCards();
    }

    private void addCards() {
        //Money Cards
        cardsIncludedInGame.put(CardType.Money10,1);
        cardsIncludedInGame.put(CardType.Money1,6);
        cardsIncludedInGame.put(CardType.Money2,5);
        cardsIncludedInGame.put(CardType.Money3,3);
        cardsIncludedInGame.put(CardType.Money4,3);
        cardsIncludedInGame.put(CardType.Money5,2);

        //Action Cards
        cardsIncludedInGame.put(CardType.PassGo,10);
        cardsIncludedInGame.put(CardType.DoubleTheRent,2);
//
//        cardsIncludedInGame.put(CardType.ForcedDeal,4);
//        cardsIncludedInGame.put(CardType.SlyDeal,3);
//
        cardsIncludedInGame.put(CardType.DebtCollector,3);
        cardsIncludedInGame.put(CardType.ItsMyBirthday,13);
//
//        cardsIncludedInGame.put(CardType.House,3);
//        cardsIncludedInGame.put(CardType.Hotel,3);
//
        cardsIncludedInGame.put(CardType.DealBreaker,2);
        cardsIncludedInGame.put(CardType.JustSayNo,13);

        //Property Cards
        cardsIncludedInGame.put(CardType.BrownProperty,2);
        cardsIncludedInGame.put(CardType.BlueProperty,2);
        cardsIncludedInGame.put(CardType.GreenProperty,3);
        cardsIncludedInGame.put(CardType.LightBlueProperty,3);
        cardsIncludedInGame.put(CardType.OrangeProperty,3);
        cardsIncludedInGame.put(CardType.PinkProperty,3);
        cardsIncludedInGame.put(CardType.RedProperty,3);
        cardsIncludedInGame.put(CardType.YellowProperty,3);
        cardsIncludedInGame.put(CardType.RailRoadProperty,4);
        cardsIncludedInGame.put(CardType.UtilityProperty,2);

        //PropertyWild Cards
        cardsIncludedInGame.put(CardType.MulticolorWild,2);
        cardsIncludedInGame.put(CardType.PinkOrangeWild,2);
        cardsIncludedInGame.put(CardType.RedYellowWild,2);
        cardsIncludedInGame.put(CardType.GreenBlueWild,1);
        cardsIncludedInGame.put(CardType.BrownLightBlueWild,1);
        cardsIncludedInGame.put(CardType.RailRoadGreenWild,1);
        cardsIncludedInGame.put(CardType.RailRoadUtilityWild,1);
        cardsIncludedInGame.put(CardType.RailRoadLightBlueWild,1);

        //Rent Cards
        cardsIncludedInGame.put(CardType.MulticolorRent,3);
        cardsIncludedInGame.put(CardType.GreenBlueRent,2);
//        cardsIncludedInGame.put(CardType.BrownLightBlueRent,2);
//        cardsIncludedInGame.put(CardType.PinkOrangeRent,2);
//        cardsIncludedInGame.put(CardType.RedYellowRent,2);
//        cardsIncludedInGame.put(CardType.RailRoadUtilityRent,2);
    }

    @Override
    protected AbstractParameters _copy() {
        // TODO: deep copy of all variables.
        return this;
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MonopolyDealParameters that = (MonopolyDealParameters) o;
        return HAND_SIZE == that.HAND_SIZE && DRAWS_WHEN_EMPTY == that.DRAWS_WHEN_EMPTY && INITIAL_DEAL == that.INITIAL_DEAL && ACTIONS_PER_TURN == that.ACTIONS_PER_TURN && DRAWS_PER_TURN == that.DRAWS_PER_TURN && SETS_TO_WIN == that.SETS_TO_WIN && Objects.equals(cardsIncludedInGame, that.cardsIncludedInGame);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), HAND_SIZE, DRAWS_WHEN_EMPTY, INITIAL_DEAL, ACTIONS_PER_TURN, DRAWS_PER_TURN, SETS_TO_WIN, cardsIncludedInGame);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.MonopolyDeal,new MonopolyDealForwardModel(),new MonopolyDealGameState(this,GameType.MonopolyDeal.getMinPlayers()));
    }
}

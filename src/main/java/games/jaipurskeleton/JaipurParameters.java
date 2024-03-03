package games.jaipurskeleton;

import core.AbstractGameState;
import core.AbstractParameters;
//import evaluation.TunableParameters;
import core.Game;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.jaipurskeleton.components.JaipurCard;

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
 * <p>The class can optionally extend from {@link evaluation.optimisation.TunableParameters} instead, which allows to use
 * automatic game parameter optimisation tools in the framework.</p>
 */
//public class JaipurParameters extends AbstractParameters {
public class JaipurParameters extends TunableParameters {

    Map<JaipurCard.GoodType, Integer> goodNCardsMinimumSell = new HashMap<JaipurCard.GoodType, Integer>() {{
        put(JaipurCard.GoodType.Diamonds, 2);
        put(JaipurCard.GoodType.Gold, 2);
        put(JaipurCard.GoodType.Silver, 2);
        put(JaipurCard.GoodType.Cloth, 1);
        put(JaipurCard.GoodType.Spice, 1);
        put(JaipurCard.GoodType.Leather, 1);
    }};
    Map<Integer, Integer[]> bonusTokensAvailable = new HashMap<Integer, Integer[]>() {{
        put(3, new Integer[]{1,1,2,2,2,3,3});
        put(4, new Integer[]{4,4,5,5,6,6});
        put(5, new Integer[]{8,8,9,10,10});
    }};

    Map<JaipurCard.GoodType, Integer[]> goodTokensProgression = new HashMap< JaipurCard.GoodType, Integer[]>() {{
        put(JaipurCard.GoodType.Diamonds, new Integer[]{5,5,5,7,7});
        put(JaipurCard.GoodType.Gold, new Integer[]{5,5,5,6,6});
        put(JaipurCard.GoodType.Silver, new Integer[]{5,5,5,5,5});
        put(JaipurCard.GoodType.Cloth, new Integer[]{1,1,2,2,3,3,5});
        put(JaipurCard.GoodType.Spice, new Integer[]{1,1,2,2,3,3,5});
        put(JaipurCard.GoodType.Leather, new Integer[]{1,1,1,1,1,1,2,3,4});
    }};



    int nPointsMostCamels = 5;
    int nGoodTokensEmptyRoundEnd = 3;
    int nRoundsWinForGameWin = 10;

    int initialCamelAmount = 3;

    int initialHandAmount = 5;

    int MarketMax = 5;

    int HandLimit = 7;

    boolean toggle = false;

    Map<JaipurCard.GoodType, Integer> goodNCardsInDeck = new HashMap<JaipurCard.GoodType, Integer>() {{
        put(JaipurCard.GoodType.Diamonds, 6);
        put(JaipurCard.GoodType.Gold, 6);
        put(JaipurCard.GoodType.Silver, 6);
        put(JaipurCard.GoodType.Cloth, 8);
        put(JaipurCard.GoodType.Spice, 8);
        put(JaipurCard.GoodType.Leather, 10);
        put(JaipurCard.GoodType.Camel, 11);
    }};

    public JaipurParameters() {
        super();
        for (JaipurCard.GoodType gt: goodNCardsMinimumSell.keySet()) {
            addTunableParameter(gt.name() + " minSell", goodNCardsMinimumSell.get( gt), Arrays.asList(1,2,3,4,5));
        }
        addTunableParameter("nPointsMostCamels", 5, Arrays.asList(0, 2, 5, 7, 10));
        addTunableParameter("nGoodTokensEmptyRoundEnd", 3, Arrays.asList(1, 2, 3, 4, 6));
        addTunableParameter("nRoundsWinForGameWin", 10, Arrays.asList(1, 2, 5, 7, 10));
        addTunableParameter("initialCamelAmount", 3, Arrays.asList(1, 2, 3, 4, 5));
        addTunableParameter("initialHandAmount", 5, Arrays.asList(1, 2, 5, 7, 8));
        addTunableParameter("MarketMax", 5, Arrays.asList(5, 6, 7, 8, 10));
        addTunableParameter("HandLimit", 7, Arrays.asList(1, 2, 5, 7, 10));
        for (JaipurCard.GoodType gt: goodNCardsInDeck.keySet()) {
            addTunableParameter(gt.name() + " InDeck", goodNCardsInDeck.get( gt), Arrays.asList(6, 7, 8, 10, 11));
        }
    }

    // Copy constructor
    private JaipurParameters(JaipurParameters jaipurParameters) {
        super();
        this.goodNCardsMinimumSell = new HashMap<>(jaipurParameters.getGoodNCardsMinimumSell());
        this.bonusTokensAvailable = new HashMap<>();
        for (int n: jaipurParameters.getBonusTokensAvailable().keySet()) {
            this.bonusTokensAvailable.put(n, jaipurParameters.getBonusTokensAvailable().get(n).clone());
        }
        this.goodTokensProgression = new HashMap<>();
        for (JaipurCard.GoodType gt: jaipurParameters.getGoodTokensProgression(). keySet()) {
            this.goodTokensProgression.put(gt, jaipurParameters. getGoodTokensProgression().get(gt).clone());
        }
        this.goodNCardsInDeck = new HashMap<>(jaipurParameters.getGoodNCardsInDeck());

        this.nPointsMostCamels = jaipurParameters.getNPointsMostCamels();
        this.nGoodTokensEmptyRoundEnd = jaipurParameters.getNGoodTokensEmptyGameEnd();
        this.nRoundsWinForGameWin = jaipurParameters.getNRoundsWinForGameWin();
        this.initialCamelAmount = jaipurParameters.getInitialCamelAmount();
        this.initialHandAmount = jaipurParameters.getInitialHandAmount();
        this.MarketMax = jaipurParameters.getMarketMax();
        this.HandLimit = jaipurParameters.getHandLimit();
        this.toggle = jaipurParameters.getToggle();
    }

    public Map<JaipurCard.GoodType, Integer> getGoodNCardsMinimumSell() {
        return goodNCardsMinimumSell;
    }

    public Map<Integer, Integer[]> getBonusTokensAvailable() {
        return bonusTokensAvailable;
    }
    public Map<JaipurCard.GoodType, Integer[]> getGoodTokensProgression() { return goodTokensProgression;}

    public Map<JaipurCard.GoodType, Integer> getGoodNCardsInDeck() {
        return goodNCardsInDeck;
    }

    public int getNPointsMostCamels() {
        return nPointsMostCamels;
    }

    public int getNGoodTokensEmptyGameEnd() {
        return nGoodTokensEmptyRoundEnd;
    }

    public int getNRoundsWinForGameWin() {
        return nRoundsWinForGameWin;
    }

    public int getInitialCamelAmount() {
        return initialCamelAmount;
    }

    public int getInitialHandAmount() {
        return initialHandAmount;
    }

    public int getMarketMax() {
        return MarketMax;
    }

    public int getHandLimit() {
        return HandLimit;
    }

    public boolean getToggle() {
        return toggle;
    }

    @Override
    protected AbstractParameters _copy() {
        return new JaipurParameters(this);
    }

    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JaipurParameters that)) return false;
        if (!super.equals(o)) return false;
        //JaipurParameters that = (JaipurParameters) o;
        return nPointsMostCamels == that.nPointsMostCamels && nGoodTokensEmptyRoundEnd == that.nGoodTokensEmptyRoundEnd && nRoundsWinForGameWin == that.nRoundsWinForGameWin && initialCamelAmount == that.initialCamelAmount && initialHandAmount == that.initialHandAmount && MarketMax == that.MarketMax && HandLimit == that.HandLimit && toggle == that.toggle && Objects.equals(goodNCardsMinimumSell, that.goodNCardsMinimumSell) && Objects.equals(goodNCardsInDeck, that.goodNCardsInDeck) && Objects.equals(bonusTokensAvailable, that.bonusTokensAvailable) && Objects.equals(goodTokensProgression, that.goodTokensProgression);
    }

    @Override
    public void _reset() {
        goodNCardsMinimumSell.replaceAll((gt, v)-> (Integer) getParameterValue(gt. name() + " minSell"));
        nPointsMostCamels = (int) getParameterValue("nPointsMostCamels");
        nGoodTokensEmptyRoundEnd = (int) getParameterValue("nGoodTokensEmptyRoundEnd");
        nRoundsWinForGameWin = (int) getParameterValue("nRoundsWinForGameWin");
        initialCamelAmount = (int) getParameterValue("initialCamelAmount");
        initialHandAmount = (int) getParameterValue("initialHandAmount");
        MarketMax = (int) getParameterValue("MarketMax");
        HandLimit = (int) getParameterValue("HandLimit");
        goodNCardsInDeck.replaceAll((gt, v)-> (Integer) getParameterValue(gt. name() + " InDeck"));
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), goodNCardsMinimumSell, bonusTokensAvailable, goodTokensProgression, goodNCardsInDeck, nPointsMostCamels, nGoodTokensEmptyRoundEnd, nRoundsWinForGameWin, initialCamelAmount, MarketMax, HandLimit, toggle, initialHandAmount);
    }

    @Override
    public Object instantiate() {
        return new Game(GameType.Jaipur, new JaipurForwardModel(), new JaipurGameState(this, GameType.Jaipur.getMinPlayers()));
    }

    @Override
    public String toString() {
        return "JaipurParameters{" +
                "goodNCardsMinimumSell=" + goodNCardsMinimumSell +
                ", bonusTokensAvailable=" + bonusTokensAvailable +
                ", goodTokensProgression=" + goodTokensProgression +
                ", nPointsMostCamels=" + nPointsMostCamels +
                ", nGoodTokensEmptyRoundEnd=" + nGoodTokensEmptyRoundEnd +
                ", nRoundsWinForGameWin=" + nRoundsWinForGameWin +
                ", initialCamelAmount=" + initialCamelAmount +
                ", initialHandAmount=" + initialHandAmount +
                ", MarketMax=" + MarketMax +
                ", HandLimit=" + HandLimit +
                ", toggle=" + toggle +
                ", goodNCardsInDeck=" + goodNCardsInDeck +
                '}';
    }
}

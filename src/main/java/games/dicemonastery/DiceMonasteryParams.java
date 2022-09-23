package games.dicemonastery;

import evaluation.TunableParameters;

import java.util.Arrays;
import java.util.Objects;

public class DiceMonasteryParams extends TunableParameters {
    public DiceMonasteryParams() {
        this(System.currentTimeMillis());
    }

    public DiceMonasteryParams(long seed) {
        super(seed);
        addTunableParameter("YEARS", 4);
        addTunableParameter("dataPath", "data/dicemonastery/");
        addTunableParameter("mandateTreasureLoss", false);
        addTunableParameter("calfSkinsRotInWinter", true);
        addTunableParameter("libraryWritingSets", false);
        addTunableParameter("takePigmentCost", 1);
        addTunableParameter("prepareInkCost", 2);
        addTunableParameter("brewBeerCost", 2);
        addTunableParameter("brewMeadCost", 2);
        addTunableParameter("bakeBreadCost", 1);
        addTunableParameter("bakeBreadYield", 2);
        addTunableParameter("makeCandleCost", 2);
        addTunableParameter("prepareVellumCost", 2);
        addTunableParameter("hireNoviceCost", 3);
        addTunableParameter("waxPerCandle", 1);
        _reset();
    }

    public String dataPath = "data/dicemonastery/";
    public int YEARS = 4;
    public int[] BONUS_TOKENS_PER_PLAYER = {0, 0, 1, 2, 2};
    public boolean mandateTreasureLoss = false;
    public boolean calfSkinsRotInWinter = true;
    public boolean libraryWritingSets = false;
    public int brewBeerCost = 2, brewMeadCost =2, bakeBreadCost = 1, bakeBreadYield = 2;
    public int takePigmentCost = 1, prepareInkCost = 2, makeCandleCost=2, prepareVellumCost=2;
    public int hireNoviceCost = 3, waxPerCandle = 1;

    @Override
    public void _reset() {
        YEARS = (int) getParameterValue("YEARS");
        dataPath = (String) getParameterValue("dataPath");
        mandateTreasureLoss = (boolean) getParameterValue("mandateTreasureLoss");
        calfSkinsRotInWinter = (boolean) getParameterValue("calfSkinsRotInWinter");
        libraryWritingSets = (boolean)  getParameterValue("libraryWritingSets");
        brewBeerCost = (int) getParameterValue("brewBeerCost");
        brewMeadCost = (int) getParameterValue("brewMeadCost");
        bakeBreadCost = (int) getParameterValue("bakeBreadCost");
        bakeBreadYield = (int) getParameterValue("bakeBreadYield");
        takePigmentCost = (int) getParameterValue("takePigmentCost");
        prepareInkCost = (int) getParameterValue("prepareInkCost");
        makeCandleCost = (int) getParameterValue("makeCandleCost");
        waxPerCandle = (int) getParameterValue("waxPerCandle");
        prepareVellumCost = (int) getParameterValue("prepareVellumCost");
        hireNoviceCost = (int) getParameterValue("hireNoviceCost");
    }

    @Override
    public DiceMonasteryParams instantiate() {
        return this;
    }

    @Override
    protected DiceMonasteryParams _copy() {
        DiceMonasteryParams retValue = new DiceMonasteryParams(System.currentTimeMillis());
        retValue.YEARS = YEARS;
        retValue.dataPath = dataPath;
        retValue.BONUS_TOKENS_PER_PLAYER = BONUS_TOKENS_PER_PLAYER.clone();
        retValue.mandateTreasureLoss = mandateTreasureLoss;
        retValue.calfSkinsRotInWinter = calfSkinsRotInWinter;
        retValue.libraryWritingSets = libraryWritingSets;
        retValue.brewBeerCost = brewBeerCost;
        retValue.brewMeadCost = brewMeadCost;
        retValue.bakeBreadCost = bakeBreadCost;
        retValue.bakeBreadYield = bakeBreadYield;
        retValue.prepareInkCost = prepareInkCost;
        retValue.prepareVellumCost = prepareVellumCost;
        retValue.takePigmentCost = takePigmentCost;
        retValue.makeCandleCost = makeCandleCost;
        retValue.waxPerCandle = waxPerCandle;
        retValue.hireNoviceCost = hireNoviceCost;
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof DiceMonasteryParams) {
            DiceMonasteryParams other = (DiceMonasteryParams) o;
            return other.YEARS == YEARS && other.dataPath.equals(dataPath) && other.mandateTreasureLoss == mandateTreasureLoss &&
                    other.libraryWritingSets == libraryWritingSets && other.hireNoviceCost == hireNoviceCost &&
                    other.brewMeadCost == brewMeadCost && other.brewBeerCost == brewBeerCost && other.waxPerCandle == waxPerCandle &&
                    other.bakeBreadCost == bakeBreadCost && other.bakeBreadYield == bakeBreadYield &&
                    other.takePigmentCost == takePigmentCost && other.prepareInkCost == prepareInkCost &&
                    other.makeCandleCost == makeCandleCost && other.prepareVellumCost == prepareVellumCost &&
                    other.calfSkinsRotInWinter == calfSkinsRotInWinter && Arrays.equals(other.BONUS_TOKENS_PER_PLAYER, BONUS_TOKENS_PER_PLAYER);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(YEARS, dataPath, mandateTreasureLoss, calfSkinsRotInWinter, libraryWritingSets, hireNoviceCost, waxPerCandle,
                brewBeerCost, brewMeadCost, bakeBreadCost, bakeBreadYield, takePigmentCost, prepareInkCost, prepareVellumCost, makeCandleCost)
                + 71 * Arrays.hashCode(BONUS_TOKENS_PER_PLAYER);
    }


    public String getDataPath() {
        return dataPath;
    }
}

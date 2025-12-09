package games.pickomino;

import core.AbstractParameters;

import java.util.Arrays;
import java.util.Objects;


public class PickominoParameters extends AbstractParameters {

    public boolean canSteal = true;
    public int minTileValue = 21; // minimum value of the tiles
    public int maxTileValue = 36; // maximum value of the tiles
    public int[] tilesPointsSteps = new int[] {21, 25, 29, 33}; // points steps for the tiles (i.e. tiles from 21 to 24 will give 1 points, tiles from 25 to 28 will give 2 points, etc.)
    public int numberOfDices = 8; // number of dices used to roll the tiles
    private int maxScore;

    public int getMaxScore() {
        return maxScore;
    }

    public PickominoParameters(){
        computeMaxScore();
    }

    private PickominoParameters(PickominoParameters parameters){
        // save computation time from computeMaxScore()
    }

    @Override
    protected AbstractParameters _copy() {
        PickominoParameters copy = new PickominoParameters(this);
        copy.canSteal = this.canSteal;
        copy.minTileValue = this.minTileValue;
        copy.maxTileValue = this.maxTileValue;
        copy.tilesPointsSteps = this.tilesPointsSteps;
        copy.numberOfDices = this.numberOfDices;
        copy.maxScore = this.maxScore;
        return copy;
    }

    @Override
    protected boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PickominoParameters that = (PickominoParameters) o;
        return canSteal == that.canSteal &&
                minTileValue == that.minTileValue &&
                maxTileValue == that.maxTileValue &&
                Arrays.equals(tilesPointsSteps, that.tilesPointsSteps) &&
                numberOfDices == that.numberOfDices;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), canSteal, minTileValue, maxTileValue, Arrays.hashCode(tilesPointsSteps), numberOfDices);
        return result;
    }

    private void computeMaxScore(){
        maxScore = 0;
        int multiplier = 1;
        for(int i = 1; i < tilesPointsSteps.length; ++i){
            maxScore += (tilesPointsSteps[i] - tilesPointsSteps[i-1]) * multiplier;
            ++multiplier;
        }
        maxScore += (maxTileValue - tilesPointsSteps[tilesPointsSteps.length - 1]) * multiplier;

    }
}


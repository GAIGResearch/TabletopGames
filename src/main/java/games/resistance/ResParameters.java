package games.resistance;

import core.AbstractGameState;
import core.AbstractParameters;
import core.Game;
import games.resistance.components.ResGameBoard;
import gametemplate.GTParameters;

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
public class ResParameters extends AbstractParameters {
    public String dataPath = "data/resistance/";
    public ResParameters(long seed) {
        super(seed);
    }

    private ResGameState gameState;

    public int getMaxRounds(){return 5;}

    public ResGameBoard getPlayerBoard(int numberPlayers){
        if (numberPlayers == 5)
        {return new ResGameBoard(new int[]{2, 3, 2, 3, 3});}
        if (numberPlayers == 6)
        {return new ResGameBoard(new int[] {2, 3, 4, 3, 4});}
        if (numberPlayers == 7)
        {return new ResGameBoard(new int[]{2, 3, 3, 4, 4});}
        if (numberPlayers == 8 || numberPlayers == 9 || numberPlayers == 10)
        {return new ResGameBoard(new int[]{3, 4, 4, 5, 5});}
        throw new AssertionError("shouldn't be null, incorrect players:" + numberPlayers);
    }


    public int[] getFactions(int numberPlayers){

        if (numberPlayers == 5)
        {return new int[]{3,2};}
        if (numberPlayers == 6)
        {return new int[]{4,2};}
        if (numberPlayers == 7)
        {return new int[]{4,3};}
        if (numberPlayers == 8)
        {return new int[]{5,3};}
        if (numberPlayers == 9)
        {return new int[]{6,3};}
        if (numberPlayers == 10)
        {return new int[]{6,4};}
        return null;

    }

    public String getDataPath() { return dataPath; }
    @Override
    protected AbstractParameters _copy() {

        ResParameters resp = new ResParameters(System.currentTimeMillis());



        //resp.factions = factions;
        return resp;

    }

    @Override
    protected boolean _equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof ResParameters)) return false;
        if (!super.equals(o)) return false;
        ResParameters that = (ResParameters) o;
        return
                o instanceof ResParameters;
        
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode());
    }
}

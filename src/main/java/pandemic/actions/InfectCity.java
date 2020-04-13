package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import content.*;
import core.GameParameters;
import core.GameState;
import pandemic.PandemicGameState;
import pandemic.PandemicParameters;
import utilities.Utils;

import static pandemic.Constants.*;

public class InfectCity implements Action {

    GameParameters gp;
    Card infectingCard;
    int count;

    public InfectCity(GameParameters gp, Card infectingCard, int count) {
        this.infectingCard = infectingCard;
        this.count = count;
    }

    public InfectCity(Card infectingCard) {
        this.infectingCard = infectingCard;
        this.count = 1;
    }

    @Override
    public boolean execute(GameState gs) {
        PropertyColor color = (PropertyColor) infectingCard.getProperty(colorHash);
        int colorIdx = Utils.indexOf(colors, color.valueStr);
        PropertyString city = (PropertyString) infectingCard.getProperty(nameHash);

        BoardNode bn = ((PandemicGameState)gs).world.getNode(nameHash, city.value);
        if (bn != null) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
            int[] array = infectionArray.getValues();

            // Add count cubes to this city
            if (array[colorIdx] > ((PandemicParameters)gp).max_cubes_per_city)
            array[colorIdx] += count;  // TODO: check outbreak & max cubes on city

            // TODO: diseases eradicated?
            // Decrease the number of remaining cubes
            gs.findCounter("Disease Cube " + color.valueStr).decrement(count);
            return true;
        }
        return false;
    }
}

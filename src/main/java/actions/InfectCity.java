package actions;

import components.BoardNode;
import components.Card;
import content.*;
import core.GameState;
import pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import static pandemic.PandemicGameState.colors;

public class InfectCity implements Action{

    Card infectingCard;
    int count;

    public InfectCity(Card infectingCard, int count) {
        this.infectingCard = infectingCard;
        this.count = count;
    }

    public InfectCity(Card infectingCard) {
        this.infectingCard = infectingCard;
        this.count = 1;
    }

    @Override
    public boolean execute(GameState gs) {
        PropertyColor color = (PropertyColor) infectingCard.getProperty("color");
        PropertyString city = (PropertyColor) infectingCard.getProperty("name");

        BoardNode bn = ((PandemicGameState)gs).findBoardNode(city.value);
        if (bn != null) {
            PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(Hash.GetInstance().hash("infection"));
            int[] array = infectionArray.getValues();
            // Add count cubes to this city
            array[Utils.indexOf(colors, color.valueStr)] += count;  // TODO: check outbreak & max cubes on city
            // TODO: diseases eradicated?
            // Decrease the number of remaining cubes
            gs.findCounter(Hash.GetInstance().hash("diseaseCubeCounter" + color.valueStr)).decrement(count);
            return true;
        }
        return false;
    }
}

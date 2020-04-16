package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import components.Counter;
import content.*;
import core.GameParameters;
import core.GameState;
import pandemic.PandemicGameState;
import pandemic.PandemicParameters;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashSet;

import static pandemic.Constants.*;

public class InfectCity implements Action {

    PandemicParameters gp;
    Card infectingCard;
    int count;

    public InfectCity(GameParameters gp, Card infectingCard, int count) {
        this.gp = (PandemicParameters)gp;
        this.infectingCard = infectingCard;
        this.count = count;
    }

    @Override
    public boolean execute(GameState gs) {
        // todo
        PandemicGameState pgs = (PandemicGameState)gs;
        PropertyColor color = (PropertyColor) infectingCard.getProperty(colorHash);

        boolean disease_eradicated = pgs.findCounter("Disease " + color.valueStr).getValue() == 2;
        if (!disease_eradicated) {  // Only infect if disease is not eradicated

            Counter diseaseCubeCounter = gs.findCounter("Disease Cube " + color.valueStr);
            int colorIdx = Utils.indexOf(colors, color.valueStr);
            PropertyString city = (PropertyString) infectingCard.getProperty(nameHash);

            BoardNode bn = pgs.world.getNode(nameHash, city.value);
            if (bn != null) {
                PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
                int[] array = infectionArray.getValues();

                // Add count cubes to this city
                array[colorIdx] += count;
                int max_cubes = gp.max_cubes_per_city;

                if (array[colorIdx] > max_cubes) {  // Outbreak!
                    Counter outbreakCounter = pgs.findCounter("Outbreaks");

                    diseaseCubeCounter.decrement(max_cubes - array[colorIdx]);
                    array[colorIdx] = max_cubes;
                    HashSet<BoardNode> allCityOutbreaks = new HashSet<>();  // Make sure we don't get stuck in loop
                    ArrayList<BoardNode> outbreaks = outbreak(bn, colorIdx, diseaseCubeCounter, outbreakCounter);
                    while (outbreaks.size() > 0) {  // Chain reaction
                        ArrayList<BoardNode> outbreaks2 = new ArrayList<>();
                        for (BoardNode b2 : outbreaks) {
                            if (!allCityOutbreaks.contains(b2)) {
                                outbreaks2.addAll(outbreak(b2, colorIdx, diseaseCubeCounter, outbreakCounter));
                            }
                        }
                        outbreaks.clear();
                        outbreaks.addAll(outbreaks2);
                        allCityOutbreaks.addAll(outbreaks2);
                    }
                } else {
                    // Decrease the number of remaining cubes
                    diseaseCubeCounter.decrement(count);
                }
                return true;
            }
        }
        return false;
    }

    private ArrayList<BoardNode> outbreak(BoardNode n, int colorIdx, Counter diseaseCubeCounter,
                                          Counter outbreakCounter) {
        // Returns list of neighbouring board nodes which have outbreaks happening as well for chain reactions
        ArrayList<BoardNode> outbreaks = new ArrayList<>();
        outbreakCounter.increment(1);

        // Find neighbouring board nodes
        for (BoardNode b2 : n.getNeighbours()){
            // Try to add a disease cube here
            PropertyIntArray infectionArray = (PropertyIntArray) b2.getProperty(infectionHash);
            int[] array = infectionArray.getValues();
            if (array[colorIdx] == gp.max_cubes_per_city) {
                // Chain outbreak
                outbreaks.add(b2);
            } else {
                // Only add a cube here
                array[colorIdx] += 1;
                diseaseCubeCounter.decrement(1);
            }
        }
        return outbreaks;
    }
}

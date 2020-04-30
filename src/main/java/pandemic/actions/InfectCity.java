package pandemic.actions;

import actions.Action;
import components.BoardNode;
import components.Card;
import components.Counter;
import content.*;
import core.GameParameters;
import core.GameState;
import pandemic.Constants;
import pandemic.PandemicGameState;
import pandemic.PandemicParameters;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashSet;

import static pandemic.Constants.*;

public class InfectCity implements Action {

    private Card infectingCard;
    private int count;
    private int maxCubesPerCity;

    public InfectCity(int maxCubesPerCity, Card infectingCard, int count) {
        this.maxCubesPerCity = maxCubesPerCity;
        this.infectingCard = infectingCard;
        this.count = count;
    }

    @Override
    public boolean execute(GameState gs) {
        // todo Quarantine Specialist
        PandemicGameState pgs = (PandemicGameState)gs;
        PropertyColor color = (PropertyColor) infectingCard.getProperty(colorHash);

        Counter diseaseCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color.valueStr));

        boolean disease_eradicated = diseaseCounter.getValue() == 2;
        if (!disease_eradicated) {  // Only infect if disease is not eradicated
            Counter diseaseCubeCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color.valueStr));
            int colorIdx = Utils.indexOf(colors, color.valueStr);
            PropertyString city = (PropertyString) infectingCard.getProperty(nameHash);

            BoardNode bn = pgs.world.getNode(nameHash, city.value);
            if (bn != null) {
                // check if quarantine specialist is on that node
                PropertyIntArrayList players = (PropertyIntArrayList)bn.getProperty(Constants.playersBNHash);
                for (int playerIdx: players.getValues()){
                    Card playerCard = (Card) pgs.getComponent(Constants.playerCardHash, playerIdx);
                    String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
                    if (roleString.equals("Quarantine Specialist")){
                        // no infection or outbreak
                        return true;
                    }
                }
                PropertyIntArray infectionArray = (PropertyIntArray) bn.getProperty(infectionHash);
                int[] array = infectionArray.getValues();

                // Add count cubes to this city
                array[colorIdx] += count;

                if (array[colorIdx] > maxCubesPerCity) {  // Outbreak!
                    Counter outbreakCounter = (Counter) pgs.getComponent(Constants.outbreaksHash);

                    diseaseCubeCounter.decrement(maxCubesPerCity - array[colorIdx]);
                    array[colorIdx] = maxCubesPerCity;
                    HashSet<BoardNode> allCityOutbreaks = new HashSet<>();  // Make sure we don't get stuck in loop
                    ArrayList<BoardNode> outbreaks = outbreak(bn, gs, colorIdx, diseaseCubeCounter, outbreakCounter);
                    while (outbreaks.size() > 0) {  // Chain reaction
                        ArrayList<BoardNode> outbreaks2 = new ArrayList<>();
                        for (BoardNode b2 : outbreaks) {
                            if (!allCityOutbreaks.contains(b2)) {
                                outbreaks2.addAll(outbreak(b2, gs, colorIdx, diseaseCubeCounter, outbreakCounter));
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

    private ArrayList<BoardNode> outbreak(BoardNode n, GameState gs, int colorIdx, Counter diseaseCubeCounter,
                                          Counter outbreakCounter) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // Returns list of neighbouring board nodes which have outbreaks happening as well for chain reactions
        ArrayList<BoardNode> outbreaks = new ArrayList<>();
        outbreakCounter.increment(1);

        // Find neighbouring board nodes
        for (BoardNode b2 : n.getNeighbours()){

            PropertyIntArrayList players = (PropertyIntArrayList)b2.getProperty(Constants.playersBNHash);
            for (int playerIdx: players.getValues()){
                Card playerCard = (Card)pgs.getComponent(Constants.playerCardHash, playerIdx);
                String roleString = ((PropertyString)playerCard.getProperty(nameHash)).value;
                if (!roleString.equals("Quarantine Specialist")) {
                    // no infection or outbreak in the city where the QS is placed
                    // Try to add a disease cube here
                    PropertyIntArray infectionArray = (PropertyIntArray) b2.getProperty(infectionHash);
                    int[] array = infectionArray.getValues();
                    if (array[colorIdx] == maxCubesPerCity) {
                        // Chain outbreak
                        outbreaks.add(b2);
                    } else {
                        // Only add a cube here
                        array[colorIdx] += 1;
                        diseaseCubeCounter.decrement(1);
                    }
                }
            }
        }
        return outbreaks;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof InfectCity)
        {
            InfectCity otherAction = (InfectCity) other;
            return infectingCard == otherAction.infectingCard && count == otherAction.count && maxCubesPerCity == otherAction.maxCubesPerCity;

        }else return false;
    }
}
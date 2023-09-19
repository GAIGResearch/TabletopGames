package games.pandemic.actions;

import core.actions.AbstractAction;
import core.actions.DrawCard;
import core.components.BoardNode;
import core.components.Card;
import core.components.Counter;
import core.properties.*;
import core.AbstractGameState;
import games.pandemic.PandemicConstants;
import games.pandemic.PandemicGameState;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import static games.pandemic.PandemicConstants.*;
import static core.CoreConstants.*;

public class InfectCity extends DrawCard {

    private final int count;
    private final int maxCubesPerCity;

    public InfectCity(int deckFrom, int deckTo, int fromIndex, int maxCubesPerCity, int count) {
        super(deckFrom, deckTo, fromIndex);
        this.maxCubesPerCity = maxCubesPerCity;
        this.count = count;
    }

    @Override
    public AbstractAction copy() {
        return new InfectCity(this.deckFrom, this.deckTo, this.fromIndex, this.maxCubesPerCity, this.count);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        PandemicGameState pgs = (PandemicGameState)gs;
        Card infectingCard = getCard(gs);
        PropertyColor color = (PropertyColor) infectingCard.getProperty(colorHash);
        Counter diseaseCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease " + color.valueStr));

        boolean disease_eradicated = diseaseCounter.getValue() == 2;
        if (!disease_eradicated) {  // Only infect if disease is not eradicated
            Counter diseaseCubeCounter = (Counter) pgs.getComponent(Hash.GetInstance().hash("Disease Cube " + color.valueStr));
            int colorIdx = Utils.indexOf(colors, color.valueStr);
            PropertyString city = (PropertyString) infectingCard.getProperty(nameHash);

            BoardNode bn = pgs.getWorld().getNodeByStringProperty(nameHash, city.value);
            if (bn != null) {
                // check if quarantine specialist is on that node
                PropertyIntArrayList players = (PropertyIntArrayList)bn.getProperty(playersHash);
                for (int playerIdx: players.getValues()){
                    Card playerCard = (Card) pgs.getComponent(PandemicConstants.playerCardHash, playerIdx);
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
                    Counter outbreakCounter = (Counter) pgs.getComponent(PandemicConstants.outbreaksHash);

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



    private ArrayList<BoardNode> outbreak(BoardNode n, AbstractGameState gs, int colorIdx, Counter diseaseCubeCounter,
                                          Counter outbreakCounter) {
        PandemicGameState pgs = (PandemicGameState)gs;
        // Returns list of neighbouring board nodes which have outbreaks happening as well for chain reactions
        ArrayList<BoardNode> outbreaks = new ArrayList<>();
        outbreakCounter.increment(1);

        // Find neighbouring board nodes
        for (BoardNode b2 : n.getNeighbours().keySet()){

            PropertyIntArrayList players = (PropertyIntArrayList)b2.getProperty(playersHash);
            for (int playerIdx: players.getValues()){
                Card playerCard = (Card)pgs.getComponent(PandemicConstants.playerCardHash, playerIdx);
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

    public int getCount() {
        return count;
    }

    public int getMaxCubesPerCity() {
        return maxCubesPerCity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InfectCity)) return false;
        if (!super.equals(o)) return false;
        InfectCity that = (InfectCity) o;
        return count == that.count &&
                maxCubesPerCity == that.maxCubesPerCity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), count, maxCubesPerCity);
    }

    @Override
    public String toString() {
        return "Infect city with " + count + " cubes";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

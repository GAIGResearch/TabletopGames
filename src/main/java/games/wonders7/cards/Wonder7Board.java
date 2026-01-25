package games.wonders7.cards;

import core.AbstractGameState;
import core.components.Card;
import games.wonders7.Wonders7Constants.Resource;
import games.wonders7.Wonders7GameState;

import java.util.*;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.cards.Wonder7Card.getString;
import static java.util.Collections.emptyMap;

public class Wonder7Board extends Card {

    public enum Wonder {
        TheColossusOfRhodes(Ore,
                List.of(Map.of(Wood, 2), Map.of(Clay, 3), Map.of(Ore, 4)),
                List.of(Map.of(Victory, 3), Map.of(Shield, 5), Map.of(Victory, 7)),
                List.of(Map.of(Stone, 3), Map.of(Ore, 4)),
                List.of(Map.of(Shield, 1, Coin, 3, Victory, 3), Map.of(Shield, 1, Coin, 4, Victory, 4))
        ),
        TheLighthouseOfAlexandria(Glass,
                List.of(Map.of(Stone, 2), Map.of(Ore, 3), Map.of(Papyrus, 1, Textile, 1)),
                List.of(Map.of(Victory, 3), Map.of(BasicWild, 1), Map.of(Victory, 7)),
                List.of(Map.of(Clay, 2), Map.of(Ore, 3), Map.of(Wood, 4)),
                List.of(Map.of(BasicWild, 1), Map.of(RareWild, 1), Map.of(Victory, 7))
        ),
        TheTempleOfArtemisInEphesus(Papyrus,
                List.of(Map.of(Clay, 2), Map.of(Wood, 2), Map.of(Ore, 2, Glass, 1)),
                List.of(Map.of(Victory, 3), Map.of(Coin, 9), Map.of(Victory, 7)),
                List.of(Map.of(Stone, 2), Map.of(Wood, 2), Map.of(Ore, 2, Textile, 1)),
                List.of(Map.of(Coin, 4, Victory, 2), Map.of(Coin, 4, Victory, 3), Map.of(Coin, 4, Victory, 5))
        ),
        TheHangingGardensOfBabylon(Wood,
                List.of(Map.of(Clay, 2), Map.of(Ore, 2, Textile, 1), Map.of(Wood, 4)),
                List.of(Map.of(Victory, 3), Map.of(ScienceWild, 1), Map.of(Victory, 7)),
                List.of(Map.of(Stone, 2), Map.of(Clay, 3, Glass, 1)),
                List.of(emptyMap(), Map.of(ScienceWild, 1))
        ),
        TheStatueOfZeusInOlympia(Clay,
                List.of(Map.of(Stone, 2), Map.of(Wood, 2), Map.of(Clay, 3)),
                List.of(Map.of(Victory, 3), emptyMap(), Map.of(Victory, 7)),
                List.of(Map.of(Ore, 2), Map.of(Clay, 3), Map.of(Glass, 1, Papyrus, 1, Textile, 1)),
                List.of(Map.of(Victory, 2), Map.of(Victory, 3), Map.of(Victory, 5))
        ),
        TheMausoleumOfHalicarnassus(Textile,
                List.of(Map.of(Ore, 2), Map.of(Glass, 1, Papyrus, 1), Map.of(Stone, 3)),
                List.of(Map.of(Victory, 3), emptyMap(), Map.of(Victory, 7)),
                List.of(Map.of(Clay, 2), Map.of(Glass, 1, Papyrus, 1), Map.of(Wood, 3)),
                List.of(Map.of(Victory, 2), Map.of(Victory, 1), emptyMap())
        ),
        ThePyramidsOfGiza(Clay,
                List.of(Map.of(Wood, 2), Map.of(Clay, 2, Textile, 1), Map.of(Stone, 4)),
                List.of(Map.of(Victory, 3), Map.of(Victory, 5), Map.of(Victory, 7)),
                List.of(Map.of(Wood, 2), Map.of(Stone, 3), Map.of(Clay, 3), Map.of(Stone, 4, Papyrus, 1)),
                List.of(Map.of(Victory, 3), Map.of(Victory, 5), Map.of(Victory, 5), Map.of(Victory, 7))
        );

        public final Resource resourcesProduced; // Default wonder production
        public final List<List<Map<Resource, Integer>>> constructionCosts; // Cost of each stage
        public final List<List<Map<Resource, Integer>>> stageProduce; // Production of each stage
        public final int[] wonderStages;

        Wonder(Resource resourceProduced,
               List<Map<Resource, Integer>> constructionCostsDay,
               List<Map<Resource, Integer>> stageProduceDay,
               List<Map<Resource, Integer>> constructionCostsNight,
               List<Map<Resource, Integer>> stageProduceNight
        ) {
            this.constructionCosts = new ArrayList<>();
            this.constructionCosts.add(constructionCostsDay);
            this.constructionCosts.add(constructionCostsNight);
            this.stageProduce = new ArrayList<>();
            this.stageProduce.add(stageProduceDay);
            this.stageProduce.add(stageProduceNight);
            this.resourcesProduced = resourceProduced;
            this.wonderStages = new int[2];
            this.wonderStages[0] = constructionCostsDay.size();
            this.wonderStages[1] = constructionCostsNight.size();
        }
    }

    private final Wonder type;
    public boolean effectUsed;
    protected int wonderStage;
    private final int wonderSide;
    public final List<Map<Resource, Integer>> constructionCosts; // Cost of each stage
    public final List<Map<Resource, Integer>> stageProduce; // Production of each stage
    public final int totalWonderStages;

    public Wonder7Board(Wonder type, int side) {
        super(type.toString());
        this.type = type;
        this.wonderSide = side;
        this.wonderStage = 1;
        this.effectUsed = false;
        this.constructionCosts = type.constructionCosts.get(wonderSide);
        this.stageProduce = type.stageProduce.get(wonderSide);
        this.totalWonderStages = type.wonderStages[wonderSide];
    }

    // Copy constructor
    protected Wonder7Board(Wonder type, int side, int componentID) {
        super(type.toString(), componentID);
        this.type = type;
        this.wonderSide = side;
        this.wonderStage = 1;
        this.effectUsed = false;
        this.constructionCosts = type.constructionCosts.get(wonderSide);
        this.stageProduce = type.stageProduce.get(wonderSide);
        this.totalWonderStages = type.wonderStages[wonderSide];
    }

    @Override
    public String toString() {
        StringBuilder stages = new StringBuilder();
        for (int i = 0; i < type.stageProduce.size(); i++) {
            String cost = getString(type.constructionCosts.get(wonderSide).get(i));
            String makes = getString(type.stageProduce.get(wonderSide).get(i));
            stages.append("{").append(i + 1).append(":").append(!cost.isEmpty() ? "cost=" + cost : "free").append(!cost.isEmpty() && !makes.isEmpty() ? "," : "").append(!makes.equals("") ? "makes=" + makes : "").append("}  ");
            if (i != type.stageProduce.size() - 1) stages.append(", ");
        }
        return type.name() + (effectUsed ? "(used)" : "") + "[" + (wonderStage - 1) + "]" +
                ",makes=" + type.resourcesProduced + " " + stages;
    }

    public Wonder wonderType() {
        return type;
    }
    public int getSide() {
        return wonderSide;
    }
    public int nextStageToBuild() {
        return wonderStage;
    }
    public boolean isPlayable(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        if (wonderStage > type.wonderStages[wonderSide]) {
            return false;
        }
        // Checks if player can afford the cost of the card
        Set<Resource> key = type.constructionCosts.get(wonderSide).get(wonderStage - 1).keySet(); //Gets the resources of the player
        for (Resource resource : key) {// Goes through every resource the player has
            if (!((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) >= type.constructionCosts.get(wonderSide).get(wonderStage - 1).get(resource))) { // Checks if players resource count is more or equal to card resource count (i.e. the player can afford the card)
                return false; // Player cant afford card
            }
        }
        return true;
    }

    public void changeStage() {
        wonderStage++;
        effectUsed = false;
    }

    @Override
    public Wonder7Board copy() {
        Wonder7Board board = new Wonder7Board(type, wonderSide, componentID);
        board.wonderStage = wonderStage;
        board.effectUsed = effectUsed;
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonder7Board that)) return false;
        if (!super.equals(o)) return false;
        return effectUsed == that.effectUsed && wonderStage == that.wonderStage && type == that.type && wonderSide == that.wonderSide;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, effectUsed, wonderStage, wonderSide);
    }
}

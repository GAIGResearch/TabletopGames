package games.wonders7.cards;

import core.AbstractGameState;
import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;
import utilities.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.Wonders7Constants.createHashList;

public class Wonder7Board extends Card {
    public enum Wonder {
        TheColossusOfRhodes(
                createHashList(createCardHash(Wood, Wood),
                        createCardHash(Clay, Clay, Clay),
                        createCardHash(Ore, Ore, Ore)),
                createHashList(createCardHash(Victory, Victory, Victory),
                        createCardHash(Shield, Shield),
                        createCardHash(new Pair<>(Victory, 7))),
                Ore),
        TheLighthouseOfAlexandria(null, null, Glass),
        TheTempleOfArtemisInEphesus(
                createHashList(createCardHash(Stone, Stone), createCardHash(Wood, Wood), createCardHash(Papyrus, Papyrus)),
                createHashList(createCardHash(new Pair<>(Victory, 3)), createCardHash(new Pair<>(Coin, 9)), createCardHash(new Pair<>(Victory,7))),
                Papyrus),
        TheHangingGardensOfBabylon(null, null, Clay),
        TheStatueOfZeusInOlympia(createHashList(createCardHash(Wood, Wood), createCardHash(Stone, Stone), createCardHash(Ore, Ore)),
                createHashList(createCardHash(new Pair<>(Victory,3)), createCardHash(new Pair<>(Victory,3)), createCardHash(new Pair<>(Victory, 7))),
                Wood),
        TheMausoleumOfHalicarnassus(createHashList(createCardHash(Clay, Clay), createCardHash(new Pair<>(Ore, 4)), createCardHash(Textile, Textile)),
                createHashList(createCardHash(new Pair<>(Victory, 3)), createCardHash(new Pair<>(Victory, 3)), createCardHash(new Pair<>(Victory, 7))),
                Textile),
        ThePyramidsOfGiza(createHashList(createCardHash(Stone, Stone), createCardHash(new Pair<>(Wood,3)), createCardHash(new Pair<>(Stone, 4))),
                createHashList(createCardHash(new Pair<>(Victory, 3)), createCardHash(new Pair<>(Victory, 5)), createCardHash(new Pair<>(Victory, 7))),
                Stone);

        public final Map<Wonders7Constants.Resource, Long> resourcesProduced; // Default wonder production
        public final List<Map<Wonders7Constants.Resource, Long>> constructionCosts; // Cost of each stage
        public final List<Map<Wonders7Constants.Resource, Long>> stageProduce; // Production of each stage
        public final int wonderStages;

        Wonder(List<Map<Wonders7Constants.Resource, Long>> constructionCosts,
               List<Map<Wonders7Constants.Resource, Long>> stageProduce,
               Wonders7Constants.Resource... resourcesProduced) {
            this.constructionCosts = constructionCosts;
            this.stageProduce = stageProduce;
            this.resourcesProduced = Arrays.stream(resourcesProduced).collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            if (constructionCosts != null) {
                this.wonderStages = constructionCosts.size();
            } else this.wonderStages = 0;
        }

        public int getStageProduce(int stage, Wonders7Constants.Resource resource) {
            return stageProduce.get(stage).get(resource).intValue();
        }
    }

    public final Wonder type;
    public boolean effectUsed;
    public int wonderStage;

    public Wonder7Board(Wonder type) {
        super(type.toString());
        this.type = type;
        this.wonderStage = 1;
        this.effectUsed = true;
    }

    // Copy constructor
    protected Wonder7Board(Wonder type, int componentID) {
        super(type.toString(),componentID);
        this.type = type;
        this.wonderStage = 1;
        this.effectUsed = true;
    }

    @Override
    public String toString() {
        StringBuilder stages = new StringBuilder();
        for (int i = 0; i < type.stageProduce.size(); i++) {
            String cost = mapToStr(type.constructionCosts.get(i));
            String makes = mapToStr(type.stageProduce.get(i));
            stages.append("{").append(i + 1).append(":").append(!cost.equals("") ? "cost=" + cost : "free").append(!cost.equals("") && !makes.equals("") ? "," : "").append(!makes.equals("") ? "makes=" + makes : "").append("}  ");
            if (i != type.stageProduce.size()-1) stages.append(", ");
        }
        return type.name() + (effectUsed ? "(used)" : "") + "[" + (wonderStage-1) + "]" +
                ",makes=" + mapToStr(type.resourcesProduced) + " " + stages;
    }

    private String mapToStr(Map<Wonders7Constants.Resource, Long> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Wonders7Constants.Resource, Long> e: m.entrySet()) {
            if (e.getValue() > 0) s.append(e.getValue()).append(" ").append(e.getKey()).append(",");
        }
        s.append("]");
        if (s.toString().equals("]")) return "";
        return s.toString().replace(",]", "");
    }

    public boolean isPlayable(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        if (wonderStage > type.wonderStages){
            return false;
        }
        // Checks if player can afford the cost of the card
        Set<Wonders7Constants.Resource> key = type.constructionCosts.get(wonderStage-1).keySet(); //Gets the resources of the player
        for (Wonders7Constants.Resource resource : key) {// Goes through every resource the player has
            if (!((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) >= type.constructionCosts.get(wonderStage-1).get(resource))) { // Checks if players resource count is more or equal to card resource count (i.e. the player can afford the card)
                return false; // Player cant afford card
            }
        }
        return true;
    }

    public void changeStage(){
        wonderStage ++;
    }

    @Override
    public Wonder7Board copy(){
        Wonder7Board board =  new Wonder7Board(type, componentID);
        board.wonderStage = wonderStage;
        board.effectUsed = effectUsed;
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wonder7Board)) return false;
        if (!super.equals(o)) return false;
        Wonder7Board that = (Wonder7Board) o;
        return effectUsed == that.effectUsed && wonderStage == that.wonderStage && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, effectUsed, wonderStage);
    }
}

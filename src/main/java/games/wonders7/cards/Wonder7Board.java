package games.wonders7.cards;

import core.AbstractGameState;
import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;

import java.util.*;

public class Wonder7Board extends Card {
    public enum Wonder {
        Colossus,
        Lighthouse,
        Temple,
        Gardens,
        Statue,
        Mausoleum,
        Pyramids
    }

    public final String wonderName;
    public final Wonder type;
    public boolean effectUsed;
    public int wonderStage;
    public final Map<Wonders7Constants.Resource, Integer> resourcesProduced; // Default wonder production
    public List<Map<Wonders7Constants.Resource, Integer>> constructionCosts; // Cost of each stage
    public List<Map<Wonders7Constants.Resource, Integer>> stageProduce; // Production of each stage

    public Wonder7Board(Wonder type, List<Map<Wonders7Constants.Resource, Integer>> constructionCosts,
                        List<Map<Wonders7Constants.Resource, Integer>> stageProduce) {
        super(type.toString());
        this.type = type;
        this.constructionCosts = new ArrayList<>();
        this.stageProduce = new ArrayList<>();
        this.constructionCosts.addAll(constructionCosts);
        this.stageProduce.addAll(stageProduce);
        this.wonderStage = 1;
        this.effectUsed = true;

        switch (type){
            case Colossus:
                this.wonderName = "The Colossus of Rhodes          ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Ore, 1);
                break;
            case Lighthouse:
                this.wonderName = "The Lighthouse of Alexandria    ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Glass, 1);
                break;
            case Temple:
                this.wonderName = "The Temple of Artemis in Ephesus";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Papyrus, 1);
                break;
            case Gardens:
                this.wonderName = "The Hanging Gardens of Babylon  ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Clay, 1);
                break;
            case Statue:
                this.wonderName = "The Statue of Zeus in Olympia   ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Wood, 1);
                break;
            case Mausoleum:
                this.wonderName = "The Mausoleum of Halicarnassus  ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Textile, 1);
                break;
            case Pyramids:
                this.wonderName = "The Pyramids of Giza            ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Stone, 1);
                break;
            default: this.wonderName = ""; this.resourcesProduced = new HashMap<>();
            break;
        }
    }

    public Wonder7Board(Wonder type, List<Map<Wonders7Constants.Resource, Integer>> constructionCosts,
                        List<Map<Wonders7Constants.Resource, Integer>> stageProduce, int componentID) {
        super(type.toString(),componentID);
        this.type = type;
        this.constructionCosts = new ArrayList<>();
        this.stageProduce = new ArrayList<>();
        this.constructionCosts.addAll(constructionCosts);
        this.stageProduce.addAll(stageProduce);
        this.wonderStage = 1;
        this.effectUsed = true;

        switch (type){
            case Colossus:
                this.wonderName = "The Colossus of Rhodes          ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Ore, 1);
                break;
            case Lighthouse:
                this.wonderName = "The Lighthouse of Alexandria    ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Glass, 1);
                break;
            case Temple:
                this.wonderName = "The Temple of Artemis in Ephesus";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Papyrus, 1);
                break;
            case Gardens:
                this.wonderName = "The Hanging Gardens of Babylon  ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Clay, 1);
                break;
            case Statue:
                this.wonderName = "The Statue of Zeus in Olympia   ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Wood, 1);
                break;
            case Mausoleum:
                this.wonderName = "The Mausoleum of Halicarnassus  ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Textile, 1);
                break;
            case Pyramids:
                this.wonderName = "The Pyramids of Giza            ";
                this.resourcesProduced = new HashMap<>();
                this.resourcesProduced.put(Wonders7Constants.Resource.Stone, 1);
                break;
            default: this.wonderName = ""; this.resourcesProduced = new HashMap<>();
                break;
        }
    }

    @Override
    public String toString() {
        StringBuilder stages = new StringBuilder();
        for (int i = 0; i < stageProduce.size(); i++) {
            String cost = mapToStr(constructionCosts.get(i));
            String makes = mapToStr(stageProduce.get(i));
            stages.append("{").append(i + 1).append(":").append(!cost.equals("") ? "cost=" + cost : "free").append(!cost.equals("") && !makes.equals("") ? "," : "").append(!makes.equals("") ? "makes=" + makes : "").append("}  ");
            if (i != stageProduce.size()-1) stages.append(", ");
        }
        return wonderName + (effectUsed ? "(used)" : "") + "[" + (wonderStage-1) + "]" +
                ",makes=" + mapToStr(resourcesProduced) + " " + stages;
    }

    private String mapToStr(Map<Wonders7Constants.Resource, Integer> m) {
        StringBuilder s = new StringBuilder();
        for (Map.Entry<Wonders7Constants.Resource, Integer> e: m.entrySet()) {
            if (e.getValue() > 0) s.append(e.getValue()).append(" ").append(e.getKey()).append(",");
        }
        s.append("]");
        if (s.toString().equals("]")) return "";
        return s.toString().replace(",]", "");
    }

    public boolean isPlayable(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        if (wonderStage == 4){return false;}
        // Checks if player can afford the cost of the card
        Set<Wonders7Constants.Resource> key = constructionCosts.get(wonderStage-1).keySet(); //Gets the resources of the player
        for (Wonders7Constants.Resource resource : key) {// Goes through every resource the player has
            if (!((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) >= constructionCosts.get(wonderStage-1).get(resource))) { // Checks if players resource count is more or equal to card resource count (i.e. the player can afford the card)
                return false; // Player cant afford card
            }
        }
        return true;
    }

    public void changeStage(){
        wonderStage +=1;
    }

    @Override
    public Wonder7Board copy(){
        Wonder7Board board =  new Wonder7Board(type, constructionCosts, stageProduce, componentID);
        board.wonderStage = wonderStage;
        board.effectUsed = effectUsed;
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wonder7Board) {
            Wonder7Board card = (Wonder7Board) o;
            return card.wonderName.equals(wonderName) &&
                    card.wonderStage == wonderStage;
        }
        return false;
    }

    @Override
    public int hashCode(){return Objects.hash(super.hashCode(), wonderName); }

}

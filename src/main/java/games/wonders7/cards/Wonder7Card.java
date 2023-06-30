package games.wonders7.cards;

import core.AbstractGameState;
import core.components.Card;
import games.wonders7.Wonders7Constants;
import games.wonders7.Wonders7GameState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Wonder7Card extends Card {

    public enum Type {
        RawMaterials,
        ManufacturedGoods,
        CivilianStructures,
        ScientificStructures,
        CommercialStructures,
        MilitaryStructures,
        Guilds
    }

    public final Type type;  // Different type of cards, brown cards, grey cards...)
    public final String cardName; // Name of card
    public final Map<Wonders7Constants.Resource, Long> constructionCost; // The resources required to construct structure
    public final Map<Wonders7Constants.Resource, Long> resourcesProduced; // Resources the card creates
    //public final HashMap<Wonder7Card, Integer> prerequisite; // THE STRUCTURES REQUIRED TO BUILD CARD FOR FREE
    public final String prerequisiteCard;

    // A normal card with construction cost, produces resources
    public Wonder7Card(String name, Type type,
                       Map<Wonders7Constants.Resource, Long> constructionCost,
                       Map<Wonders7Constants.Resource, Long> resourcesProduced) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    // Card has prerequisite cards
    public Wonder7Card(String name, Type type,
                       Map<Wonders7Constants.Resource,Long> constructionCost,
                       Map<Wonders7Constants.Resource,Long> resourcesProduced, String prerequisiteCard) {
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    // A free card (no construction cost)
    public Wonder7Card(String name, Type type, Map<Wonders7Constants.Resource,Long> resourcesProduced){
        super(name);
        this.cardName = name;
        this.type = type;
        this.constructionCost = new HashMap<>(); // Card costs nothing
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = "";
    }

    protected Wonder7Card(String name, Type type,
                          Map<Wonders7Constants.Resource,Long> constructionCost,
                          Map<Wonders7Constants.Resource,Long> resourcesProduced, String prerequisiteCard, int componentID){
        super(name, componentID);
        this.cardName = name;
        this.type = type;
        this.constructionCost = constructionCost;
        this.resourcesProduced = resourcesProduced;
        this.prerequisiteCard = prerequisiteCard;
    }

    public int getNProduced(Wonders7Constants.Resource resource) {
        return resourcesProduced.get(resource).intValue();
    }
    public int getNCost(Wonders7Constants.Resource resource) {
        return constructionCost.get(resource).intValue();
    }

    @Override
    public String toString() {
        String cost = mapToStr(constructionCost);
        String makes = mapToStr(resourcesProduced);
        return "{" + cardName +
                "(" + type + ")" +
                (!cost.equals("") ? ":cost=" + cost : ",free") +
                (!makes.equals("") ? ",makes=" + makes : "") + "}  ";
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


    // Checks if player can pay the cost of the card or if the player is allowed to build the structure
    public boolean isPlayable(AbstractGameState gameState) {
        Wonders7GameState wgs = (Wonders7GameState) gameState;
        // Checks if the player has an identical structure
        for (int i=0;i<wgs.getPlayedCards(wgs.getCurrentPlayer()).getSize();i++){
            if(Objects.equals(wgs.getPlayedCards(wgs.getCurrentPlayer()).get(i).cardName, cardName)){
                return false;
            }
        }

        // Checks if player can afford the cost of the card
        Set<Wonders7Constants.Resource> key = constructionCost.keySet(); //Gets the resources of the player
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource the player has
            if ((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) < constructionCost.get(resource)) { // Checks if players resource count is more or equal to card resource count (i.e. the player can afford the card)
                return false; // Player cant afford card
            }
        }
        return true;
    }

    // Checks if the card is free using pre-requisite card
    public boolean isFree(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Checks if the player has an identical structure
        for (int i=0;i<wgs.getPlayedCards(wgs.getCurrentPlayer()).getSize();i++){
            if(Objects.equals(wgs.getPlayedCards(wgs.getCurrentPlayer()).get(i).cardName, cardName)){
                return false;
            }
        }

        // Checks if the player has prerequisite cards
            for (int i=0;i<wgs.getPlayedCards(wgs.getCurrentPlayer()).getSize();i++){
                if (prerequisiteCard.equals(wgs.getPlayedCards(wgs.getCurrentPlayer()).get(i).cardName)){
                    return true;
                }
            }
        return false;
    }

    // Checks if neighbour on the right can provide resources to build the structure
    public boolean isPayableR(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Checks if the player has an identical structure
        for (int i=0;i<wgs.getPlayedCards(wgs.getCurrentPlayer()).getSize();i++){
            if(Objects.equals(wgs.getPlayedCards(wgs.getCurrentPlayer()).get(i).cardName, cardName)){
                return false;
            }
        }

        // Collects the resources player does not have
        Set<Wonders7Constants.Resource> key = constructionCost.keySet();
        HashMap<Wonders7Constants.Resource, Long> neededResources = new HashMap<>();
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                neededResources.put(resource, constructionCost.get(resource)-wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource));
            }
        }
        // Calculates the cost of resources
        int coinCost=0;
        key = neededResources.keySet();
        for (Wonders7Constants.Resource resource : key)
            coinCost += 2*neededResources.get(resource); // For each unit of the resource needed
        if (coinCost>wgs.getPlayerResources(wgs.getCurrentPlayer()).get(Wonders7Constants.Resource.Coin)){return false;} // If player can pay the neighbour for the resources


        HashMap<Wonders7Constants.Resource, Long> neighbourResources = new HashMap<>(); // Resources offered by the neighbour
        // Resources provided by neighbour's wonder
        key = wgs.getPlayerWonderBoard(((wgs.getCurrentPlayer()+1)%wgs.getNPlayers())).type.resourcesProduced.keySet();
        for (Wonders7Constants.Resource resource : key) {
            neighbourResources.put(resource, wgs.getPlayerWonderBoard(((wgs.getCurrentPlayer()+1)%wgs.getNPlayers())).type.resourcesProduced.get(resource));
        }
        // Resources provided by neighbour's raw materials
        for (int i=0;i<wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).getSize();i++){
            if ((wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).type== Type.RawMaterials)){
                key = wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).resourcesProduced.keySet();
                for (Wonders7Constants.Resource resource : key) {
                    neighbourResources.put(resource, wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).resourcesProduced.get(resource));
                }
            }
        }
        // Resources provided by neighbour's manufactured goods
        for (int i=0;i<wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).getSize();i++){
            if ((wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).type== Type.ManufacturedGoods)){
                key = wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).resourcesProduced.keySet();
                for (Wonders7Constants.Resource resource : key) {
                    neighbourResources.put(resource, wgs.getPlayedCards((wgs.getCurrentPlayer()+1)%wgs.getNPlayers()).get(i).resourcesProduced.get(resource));
                }
            }
        }

        // Calculates combined resources of neighbour and player
        HashMap<Wonders7Constants.Resource, Long> combinedResources = new HashMap<>();
        key = neighbourResources.keySet();
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource provided by the neighbour
            combinedResources.put(resource, wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)+neighbourResources.get(resource)); // Adds player and neighbour values into combined resources hashmap
        }

        // Checks if the combinedResources can pay the cost of the card
        key = constructionCost.keySet();
        for (Wonders7Constants.Resource resource : key) {
            if (combinedResources.get(resource)== null){return false;}
            if ((combinedResources.get(resource)) < constructionCost.get(resource)) { // Checks whether player's resource (after 'buying' resources) count can now afford the card
                return false; // Player can't afford card with bought resources
            }
        }
        return true;
    }

    // Checks if neighbour on the left can provide resources to build the structure
    public boolean isPayableL(AbstractGameState gameState){
        Wonders7GameState wgs = (Wonders7GameState) gameState;

        // Checks if the player has an identical structure
        for (int i=0;i<wgs.getPlayedCards(wgs.getCurrentPlayer()).getSize();i++){
            if(Objects.equals(wgs.getPlayedCards(wgs.getCurrentPlayer()).get(i).cardName, cardName)){
                return false;
            }
        }

        // Collects the resources player does not have
        Set<Wonders7Constants.Resource> key = constructionCost.keySet();
        HashMap<Wonders7Constants.Resource, Long> neededResources = new HashMap<>();
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource the player needs
            if ((wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)) < constructionCost.get(resource)) { // If the player does not have resource count, added to needed resources
                neededResources.put(resource, constructionCost.get(resource)-wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource));
            }
        }
        // Calculates the cost of resources
        int coinCost=0;
        key = neededResources.keySet();
        for (Wonders7Constants.Resource resource : key)
            coinCost += 2*neededResources.get(resource); // For each unit of the resource needed
        if (coinCost>wgs.getPlayerResources(wgs.getCurrentPlayer()).get(Wonders7Constants.Resource.Coin)){return false;} // If player can pay the neighbour for the resources


        HashMap<Wonders7Constants.Resource, Long> neighbourResources = new HashMap<>(); // Resources offered by the neighbour
        // Resources provided by neighbour's wonder
        key = wgs.getPlayerWonderBoard(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).type.resourcesProduced.keySet();
        for (Wonders7Constants.Resource resource : key) {
            neighbourResources.put(resource, wgs.getPlayerWonderBoard(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).type.resourcesProduced.get(resource));
        }
        // Resources provided by neighbour's raw materials
        for (int i=0;i<wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).getSize();i++){
            if ((wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).type== Type.RawMaterials)){
                key = wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).resourcesProduced.keySet();
                for (Wonders7Constants.Resource resource : key) {
                    neighbourResources.put(resource, wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).resourcesProduced.get(resource));
                }
            }
        }
        // Resources provided by neighbour's manufactured goods
        for (int i=0;i<wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).getSize();i++){
            if ((wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).type== Type.ManufacturedGoods)){
                key = wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).resourcesProduced.keySet();
                for (Wonders7Constants.Resource resource : key) {
                    neighbourResources.put(resource, wgs.getPlayedCards(Math.floorMod(wgs.getCurrentPlayer()-1, wgs.getNPlayers())).get(i).resourcesProduced.get(resource));
                }
            }
        }

        // Calculates combined resources of neighbour and player
        HashMap<Wonders7Constants.Resource, Long> combinedResources = new HashMap<>();
        key = neighbourResources.keySet();
        for (Wonders7Constants.Resource resource : key) { // Goes through every resource provided by the neighbour
            combinedResources.put(resource, wgs.getPlayerResources(wgs.getCurrentPlayer()).get(resource)+neighbourResources.get(resource)); // Adds player and neighbour values into combined resources hashmap
        }

        // Checks if the combinedResources can pay the cost of the card
        key = constructionCost.keySet();
        for (Wonders7Constants.Resource resource : key) {
            if (combinedResources.get(resource)== null){return false;}
            if ((combinedResources.get(resource)) < constructionCost.get(resource)) { // Checks whether player's resource (after 'buying' resources) count can now afford the card
                return false; // Player can't afford card with bought resources
            }
        }
        return true;
    }

    @Override
    public Card copy(){
        return new Wonder7Card(cardName, type, constructionCost, resourcesProduced, prerequisiteCard, componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Wonder7Card) {
            Wonder7Card card = (Wonder7Card) o;
            return card.cardName.equals(cardName) &&
                    card.type == type;}
        return false;
    }

    public Type getCardType(){
        return type;
    }


    @Override
    public int hashCode(){return Objects.hash(super.hashCode(), cardName); }
}


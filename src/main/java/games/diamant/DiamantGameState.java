package games.diamant;

import core.AbstractGameState;
import core.AbstractParameters;
import core.actions.AbstractAction;
import core.components.Component;
import core.components.Counter;
import core.components.Deck;
import core.interfaces.IPrintable;
import games.GameType;
import games.diamant.cards.DiamantCard;
import games.diamant.components.ActionsPlayed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;


public class DiamantGameState extends AbstractGameState implements IPrintable {
    Deck<DiamantCard>          mainDeck;
    Deck<DiamantCard>          discardDeck;
    Deck<DiamantCard>          path;
    Deck<DiamantCard>          relicDeck;

    List<Counter> treasureChests;
    List<Counter> hands;
    List<Boolean> playerInCave;

    public List<Integer> getPlayersInCave() {
        return IntStream.range(0, getNPlayers())
                .filter(i -> playerInCave.get(i))
                .boxed()
                .collect(Collectors.toList());
    }

    // helper data class to store interesting information
    static class PlayerTurnRecord {
        public final int player;
        public final int round;
        public final int turnLeft;

        PlayerTurnRecord(int player, int round, int turn) {
            this.player = player;
            this.round = round;
            this.turnLeft = turn;
        }
    }

    List<PlayerTurnRecord> recordOfPlayerActions = new ArrayList<>();

    // List of gems on each card in the path (same length as path)
    List<Integer> gemsOnPath = new ArrayList<>();

    int nCave = 0;

    // This component store the actions played for the rest of players.
    // It is needed since in this game, actions are simultaneously played
    ActionsPlayed actionsPlayed;

    /**
     * Constructor. Initialises some generic game state variables.
     *
     * @param gameParameters - game parameters.
     * @param nPlayers      - number of players for this game.
     */
    public DiamantGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
    }

    @Override
    protected GameType _getGameType() {
        return GameType.Diamant;
    }

    @Override
    protected List<Component> _getAllComponents() {
        return new ArrayList<Component>() {{
            add(mainDeck);
            add(discardDeck);
            add(path);
            if (relicDeck != null) add(relicDeck);
            addAll(treasureChests);
            addAll(hands);
            add(actionsPlayed);
        }};
    }

    @Override
    protected AbstractGameState _copy(int playerId)
    {
        DiamantGameState dgs = new DiamantGameState(gameParameters.copy(), getNPlayers());

        dgs.mainDeck    = mainDeck.copy();
        dgs.discardDeck = discardDeck.copy();
        dgs.path        = path.copy();
        if (relicDeck != null) dgs.relicDeck = relicDeck.copy();
        dgs.actionsPlayed  = (ActionsPlayed) actionsPlayed.copy();

        dgs.gemsOnPath = new ArrayList<>(gemsOnPath);

        dgs.nCave          = nCave;
        dgs.hands          = new ArrayList<>();
        dgs.treasureChests = new ArrayList<>();
        dgs.playerInCave   = new ArrayList<>();
        dgs.recordOfPlayerActions.addAll(recordOfPlayerActions);

        for (Counter c : hands)
            dgs.hands.add(c.copy());

        for (Counter c : treasureChests)
            dgs.treasureChests.add(c.copy());

        // If there is an action played for a player, then copy it
        for (int i=0; i<getNPlayers(); i++)
        {
            if (actionsPlayed.containsKey(i))
                dgs.actionsPlayed.put(i, actionsPlayed.get(i).copy());
        }

        dgs.playerInCave.addAll(playerInCave);

        // mainDeck and is actionsPlayed are hidden.
        if (getCoreGameParameters().partialObservable && playerId != -1)
        {
            dgs.mainDeck.shuffle(redeterminisationRnd);

            dgs.actionsPlayed.clear();

            // TODO: We also should remove the history entries for the removed actions
            // This is not formally necessary, as nothing currently uses this information, but in
            // a competition setting for example, it would be critical. There is no simple way to do this at the moment
            // because history (as part of the super-class) is only copied after we return from this _copy() method.

           // Randomize actions for other players (or any other modelling approach)
            // is now the responsibility of the deciding agent (see for example OSLA)

        }
        return dgs;
    }

    @Override
    protected double _getHeuristicScore(int playerId)
    {
        return new DiamantHeuristic().evaluateState(this, playerId);
    }
    /**
     * Current score
     */
    @Override
    public double getGameScore(int playerId) {
         return treasureChests.get(playerId).getValue();
    }

    @Override
    protected ArrayList<Integer> _getUnknownComponentsIds(int playerId)
    {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(actionsPlayed.getComponentID());
        return ids;
    }

    protected void _reset() {
        mainDeck       = null;
        discardDeck    = null;
        path           = null;
        actionsPlayed  = null;
        treasureChests = new ArrayList<>();
        hands          = new ArrayList<>();
        playerInCave   = new ArrayList<>();

        gemsOnPath = new ArrayList<>();
        nCave = 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                mainDeck,
                discardDeck,
                path,
                treasureChests,
                hands,
                playerInCave,
                nCave,
                actionsPlayed,
                gemsOnPath
        );
    }

    @Override
    protected boolean _equals(Object o)
    {
        if (this == o)                        return true;
        if (!(o instanceof DiamantGameState)) return false;
        if (!super.equals(o))                 return false;

        DiamantGameState that = (DiamantGameState) o;

        return
               nCave                   == that.nCave                   &&
               Objects.equals(mainDeck,       that.mainDeck)           &&
               Objects.equals(discardDeck,    that.discardDeck)        &&
               Objects.equals(hands,          that.hands)              &&
               Objects.equals(treasureChests, that.treasureChests)     &&
               Objects.equals(path,           that.path)               &&
               Objects.equals(playerInCave,   that.playerInCave)       &&
               Objects.equals(actionsPlayed,  that.actionsPlayed) &&
               Objects.equals(gemsOnPath, that.gemsOnPath);
    }

    /**
     * Returns the number of player already in the cave
    */
    public int getNPlayersInCave()
    {
        int n = 0;
        for (Boolean b: playerInCave)
            if (b) n++;
        return n;
    }

    public List<PlayerTurnRecord> getRecordOfPlayerActions() {
        return recordOfPlayerActions;
    }

    @Override
    public void printToConsole() {
        String[] strings = new String[13];

        StringBuilder str_gemsOnHand          = new StringBuilder();
        StringBuilder str_gemsOnTreasureChest = new StringBuilder();
        StringBuilder str_playersOnCave       = new StringBuilder();

        for (Counter c:hands)          { str_gemsOnHand.         append(c.getValue()).append(" "); }
        for (Counter c:treasureChests) { str_gemsOnTreasureChest.append(c.getValue()).append(" "); }
        for (Boolean b : playerInCave)
        {
            if (b) str_playersOnCave.append("T");
            else   str_playersOnCave.append("F");
        }

        Map<DiamantCard.HazardType, Long> hazardsOnPath = getHazardsOnPath();
        Map<DiamantCard.HazardType, Long> hazardsInDeck = getNHazardCardsInMainDeck();
        strings[0]  = "----------------------------------------------------";
        strings[1]  = "Cave:                       " + nCave;
        strings[2]  = "Players on Cave:            " + str_playersOnCave;
        strings[3]  = "Path:                       " + path.toString();
        strings[4]  = "Gems on Path:               " + gemsOnPath.stream().map(String::valueOf).collect(Collectors.joining());
        strings[5]  = "Gems on hand:               " + str_gemsOnHand;
        strings[6]  = "Gems on treasure chest:     " + str_gemsOnTreasureChest;
        // then iterate over the possivble hazard values, and show the number in path and deck
        int count = 0;
        for (DiamantCard.HazardType hazardType : DiamantCard.HazardType.values()) {
            long pathCount = hazardsOnPath.getOrDefault(hazardType, 0L);
            long deckCount = hazardsInDeck.getOrDefault(hazardType, 0L);
            strings[7 + count] = "Hazard " + hazardType + " on path: " + pathCount + ", in deck: " + deckCount;
            count++;
        }

        strings[7 + count] = "----------------------------------------------------";

        for (String s : strings){
            System.out.println(s);
        }
    }

    public Map<DiamantCard.HazardType, Long> getHazardsOnPath() {
        return path.stream()
                .filter(c -> c.getCardType() == DiamantCard.DiamantCardType.Hazard)
                .collect(groupingBy(DiamantCard::getHazardType, counting()));
    }

    public Map<DiamantCard.HazardType, Long> getNHazardCardsInMainDeck() {
        return mainDeck.stream()
                .filter(c -> c.getCardType() == DiamantCard.DiamantCardType.Hazard)
                .collect(groupingBy(DiamantCard::getHazardType, counting()));
    }

    public Deck<DiamantCard> getMainDeck()       { return mainDeck;       }
    public Deck<DiamantCard> getDiscardDeck()    { return discardDeck;    }
    public List<Counter>     getHands()          { return hands;          }
    public List<Counter>     getTreasureChests() { return treasureChests; }
    public Deck<DiamantCard> getPath()           { return path;           }
    public ActionsPlayed     getActionsPlayed()  { return actionsPlayed;  }
    public Deck<DiamantCard> getRelicDeck()      { return relicDeck;      }
    public void setActionPlayed(int player, AbstractAction action) {
        actionsPlayed.put(player, action);
    }

    // Helper: get total gems on path
    public int getTotalGemsOnPath() {
        return gemsOnPath.stream().mapToInt(Integer::intValue).sum();
    }

    // Helper: get gems on a specific path index
    public int getGemsOnPathIndex(int idx) {
        return gemsOnPath.get(idx);
    }

    // Helper: set gems on a specific path index
    public void setGemsOnPathIndex(int idx, int value) {
        gemsOnPath.set(idx, value);
    }

    // Helper: clear gems on path
    public void clearGemsOnPath() {
        gemsOnPath.clear();
    }

    public List<Integer> getGemsOnPathList() {
        return gemsOnPath;
    }
}

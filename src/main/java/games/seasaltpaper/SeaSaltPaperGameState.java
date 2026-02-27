package games.seasaltpaper;

import core.AbstractParameters;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.GameType;
import games.seasaltpaper.cards.HandManager;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.io.Serializable;
import java.util.*;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class SeaSaltPaperGameState extends AbstractGameState implements IPrintable{

    public transient boolean saveState = false;  // todo: this crashes if true unless some other files exist
    public transient int saveCycle = 4;

    public enum TurnPhase {
        START,
        DRAW,
        DUO,
        FINISH;

        public TurnPhase next()
        {
            return next.get();
        }
        private Supplier<TurnPhase> next;
        static {
            START.next = () -> TurnPhase.DRAW;
            DRAW.next = () -> TurnPhase.DUO;
            DUO.next = () -> TurnPhase.FINISH;
            FINISH.next = () -> TurnPhase.DRAW;
        }
    }

    TurnPhase currentPhase = TurnPhase.START;

    List<PartialObservableDeck<SeaSaltPaperCard>> playerHands;
    List<Deck<SeaSaltPaperCard>> playerDiscards;

    Deck<SeaSaltPaperCard> discardPile1, discardPile2;  // TODO make this a list to generalize different number of discard piles
    Deck<SeaSaltPaperCard> drawPile;

    int lastChance = -1; // index of the player that play "lastChance", -1 for no one


    boolean[] protectedHands;

    int[] playerTotalScores; // player points of previous rounds

    public int[] playerPlayedDuoPoints;

    //final int winScore;

    public SeaSaltPaperGameState(AbstractParameters gameParameters, int nPlayers) {
        super(gameParameters, nPlayers);
//        gameID = SeaSaltPaperGameState.GameCount;
    }

//    private SeaSaltPaperGameState(AbstractParameters gameParameters, int nPlayers, int gameID) {
//        super(gameParameters, nPlayers);
//        this.gameID = gameID;
//    }

        @Override
    protected GameType _getGameType() { return GameType.SeaSaltPaper; }

    @Override
    protected List<Component> _getAllComponents() {
        List<Component> components = new ArrayList<>();
        components.addAll(playerHands);
        components.addAll(playerDiscards);
        components.add(drawPile);
        components.add(discardPile1);
        components.add(discardPile2);
        return components;
    }

    @Override
    protected AbstractGameState _copy(int playerId) {
        SeaSaltPaperGameState gsCopy = new SeaSaltPaperGameState(gameParameters.copy(), getNPlayers());
        SeaSaltPaperParameters params = (SeaSaltPaperParameters) gameParameters;

        gsCopy.saveState = playerId < 0 && saveState; // Copy saveState if playerId < 0, else false
//        gsCopy.saveState = false;

        gsCopy.drawPile = drawPile.copy();
        gsCopy.discardPile1 = discardPile1.copy();
        gsCopy.discardPile2 = discardPile2.copy();
        gsCopy.playerHands = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            gsCopy.playerHands.add(playerHands.get(i).copy());
        }
        gsCopy.playerDiscards = new ArrayList<>();
        for (int i = 0; i < getNPlayers(); i++) {
            gsCopy.playerDiscards.add(playerDiscards.get(i).copy());
        }

        //Redeterminize hidden info (unless playerID == -1)
        if (playerId != -1 && getCoreGameParameters().partialObservable) {
            // TODO handle discardPiles too
            if (params.individualVisibility) { // Using individual visibility
                // Only add invisible cards back to drawPile
                for (int i=0; i < getNPlayers(); i++) {
                    if (i == playerId) { continue; }
                    ArrayList<SeaSaltPaperCard> shuffledCards = new ArrayList<>(); // Cards to be removed from playerHands and redeterminized
                    for (int j=0; j < playerHands.get(i).getSize(); j++) {
                        SeaSaltPaperCard cardCopy = gsCopy.playerHands.get(i).get(j);
                        if (!playerHands.get(i).isComponentVisible(j, playerId)) { // if card not visible to playerId
                            shuffledCards.add(cardCopy);
                        }
                        else {
                            gsCopy.playerHands.get(i).setVisibilityOfComponent(j, i, true);
                        }
                    }
                    for (SeaSaltPaperCard c : shuffledCards) {
                        gsCopy.playerHands.get(i).remove(c);
                        gsCopy.drawPile.add(c);
                    }
                }
                gsCopy.drawPile.shuffle(redeterminisationRnd);
                for (int i=0; i < getNPlayers(); i++) {
                    if (i == playerId) { continue; }
                    while (gsCopy.playerHands.get(i).getSize() < playerHands.get(i).getSize()) {
                        SeaSaltPaperCard c = gsCopy.drawPile.draw();
                        gsCopy.playerHands.get(i).add(c);
                        gsCopy.playerHands.get(i).setVisibilityOfComponent(0,i,true); // Set the card visible to the owner
                    }
                }
            }
            else { // Without using individual visibility
                // TODO redeterminize discardPiles?
                for (int i=0; i < getNPlayers(); i++) {
                    if (i == playerId) { continue; }
                    gsCopy.drawPile.add(gsCopy.playerHands.get(i));
                    gsCopy.playerHands.get(i).clear();
                }
                gsCopy.drawPile.shuffle(redeterminisationRnd);
                for (int i=0; i < getNPlayers(); i++) {
                    if (i == playerId) { continue; }
                    for (int j=0; j < playerHands.get(i).getSize(); j++) {
                        gsCopy.playerHands.get(i).add(gsCopy.drawPile.draw());
                    }
                }
            }
        }

        gsCopy.playerTotalScores = playerTotalScores.clone();
        gsCopy.playerPlayedDuoPoints = playerPlayedDuoPoints.clone();
        gsCopy.protectedHands = protectedHands.clone();

        gsCopy.lastChance = lastChance;
        gsCopy.currentPhase = currentPhase;
        return gsCopy;
    }

    @Override
    protected double _getHeuristicScore(int playerId) {
        return HandManager.calculatePoint(this, playerId) + playerTotalScores[playerId];
    }

    @Override
    public double getGameScore(int playerId) {
        return playerTotalScores[playerId];
    }


    @Override
    public boolean _equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SeaSaltPaperGameState that = (SeaSaltPaperGameState) o;
        return lastChance == that.lastChance && currentPhase == that.currentPhase && Objects.equals(playerHands, that.playerHands) && Objects.equals(playerDiscards, that.playerDiscards) && Objects.equals(discardPile1, that.discardPile1) && Objects.equals(discardPile2, that.discardPile2) && Objects.equals(drawPile, that.drawPile) && Arrays.equals(protectedHands, that.protectedHands) && Arrays.equals(playerTotalScores, that.playerTotalScores) && Arrays.equals(playerPlayedDuoPoints, that.playerPlayedDuoPoints);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), currentPhase, lastChance, playerHands, playerDiscards, discardPile1, discardPile2, drawPile);
        result = 31 * result + Arrays.hashCode(protectedHands);
        result = 31 * result + Arrays.hashCode(playerTotalScores);
        result = 31 * result + Arrays.hashCode(playerPlayedDuoPoints);
        return result;
    }

    /**
     * Updates components after round setup.
     */
    void updateComponents() {
        this.addAllComponents();
    }

    public List<PartialObservableDeck<SeaSaltPaperCard>> getPlayerHands() { return playerHands; }

    public List<Deck<SeaSaltPaperCard>> getPlayerDiscards() { return playerDiscards; }

    public boolean[] getProtectedHands() { return protectedHands; }

    // Check if all hands are protected;
    public boolean allProtected() {
        boolean t = true;
        for (int i=0; i<getNPlayers(); i++) {
            t = t && protectedHands[i];
        }
        return t;
    }

    public boolean allEnemyProtected(int playerId) {
        boolean t = true;
        for (int i=0; i<getNPlayers(); i++) {
            if (i == playerId) { continue; }
            t = t && protectedHands[i];
        }
        return t;
    }

    // check if there's a valid target for SwimmerSharkDuo
    public boolean allEnemiesProtectedOrEmpty(int playerId) {
        boolean t = true;
        for (int i=0; i<getNPlayers(); i++) {
            if (i == playerId) { continue; }
            t = t && (protectedHands[i] || playerHands.get(i).getSize() == 0) ;
        }
        return t;
    }

    public boolean allDiscardPilesEmpty() {
        return discardPile1.getSize() == 0 && discardPile1.getSize() == 0;
    }

    public Deck<SeaSaltPaperCard> getDrawPile() { return drawPile; }

    public Deck<SeaSaltPaperCard> getDiscardPile1() { return discardPile1; }
    public Deck<SeaSaltPaperCard> getDiscardPile2() { return discardPile2; }
    public Deck<SeaSaltPaperCard>[] getDiscardPiles() { return new Deck[]{discardPile1, discardPile2};}

    public int[] getPlayerTotalScores() { return playerTotalScores; }
    public TurnPhase getCurrentPhase() { return currentPhase;}
    public void resetTurn() { currentPhase = TurnPhase.START; }


    public void setLastChance(int playerID) {
        if (playerID >= getNPlayers()) {
            throw new RuntimeException("Last Chance playerID " + playerID + " is not supposed to exist");
        }
        lastChance = playerID;
    }
}

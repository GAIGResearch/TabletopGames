package groupM.players.mcts;

public class RAVE {
    public static double getValue(double V, int nVisits, double value, double amafValue){
        double amafAlpha = Math.max(0, (V - nVisits) / V);
        return (amafAlpha * amafValue) + (1 - amafAlpha) * value;
    }
}
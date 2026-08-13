package games.descent2e.pcg_clean.ui;

import java.awt.Color;

/** Compact interpolation of Matplotlib's perceptually uniform viridis colour map. */
public final class ViridisColourMap {
    private static final Color[] STOPS = {
            new Color(68, 1, 84), new Color(59, 82, 139), new Color(33, 145, 140),
            new Color(94, 201, 98), new Color(253, 231, 37)
    };

    private ViridisColourMap() {}

    public static Color colour(double value) {
        double clamped = Math.max(0, Math.min(1, value));
        double scaled = clamped * (STOPS.length - 1);
        int low = Math.min((int) scaled, STOPS.length - 2);
        double fraction = scaled - low;
        Color a = STOPS[low];
        Color b = STOPS[low + 1];
        return new Color(interpolate(a.getRed(), b.getRed(), fraction),
                interpolate(a.getGreen(), b.getGreen(), fraction),
                interpolate(a.getBlue(), b.getBlue(), fraction));
    }

    private static int interpolate(int a, int b, double fraction) {
        return (int) Math.round(a + (b - a) * fraction);
    }
}

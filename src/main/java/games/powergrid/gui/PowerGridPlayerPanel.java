package games.powergrid.gui;



import games.powergrid.PowerGridGameState;
import games.powergrid.components.*;
import games.powergrid.PowerGridParameters;
import games.powergrid.PowerGridParameters.Resource;

import javax.swing.*;

import core.AbstractGameState;
import core.components.Deck;
import games.powergrid.components.PowerGridCard;
import static core.CoreConstants.VisibilityMode.*;

import java.awt.*;
import java.util.Map;

@SuppressWarnings("serial")
public class PowerGridPlayerPanel extends JPanel {
    private final JLabel nameLbl   = new JLabel();
    private final JLabel moneyLbl  = new JLabel();
    private final int playerId;

    private final JLabel coalLbl    = new JLabel("0");
    private final JLabel oilLbl     = new JLabel("0");
    private final JLabel gasLbl     = new JLabel("0");
    private final JLabel uraniumLbl = new JLabel("0");

    private PowerGridDeckView plantView;
    private final JPanel rightCardHolder = new JPanel(new BorderLayout());
    public Deck<PowerGridCard> test;


    public PowerGridPlayerPanel(int playerId,Color bgColor) {
        this.playerId = playerId;


        setOpaque(true);
        setBackground(bgColor);   // <-- use the color passed in
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 6, 4, 6),
                BorderFactory.createLineBorder(new Color(0, 0, 0, 40))
        ));

        // Left/right columns
        setLayout(new BorderLayout(8, 0));

        // ---- LEFT column
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        nameLbl.setFont(nameLbl.getFont().deriveFont(Font.BOLD, 14f));
        moneyLbl.setFont(moneyLbl.getFont().deriveFont(Font.PLAIN, 12f));
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        moneyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(nameLbl);
        header.add(moneyLbl);

        left.add(header);
        left.add(Box.createVerticalStrut(2));
        left.add(resourceRow("COAL",    coalLbl));
        left.add(resourceRow("OIL",     oilLbl));
        left.add(resourceRow("GAS",     gasLbl));
        left.add(resourceRow("URANIUM", uraniumLbl));
        left.add(Box.createVerticalGlue());

        // ---- RIGHT column (cards) inside a horizontal scroll
        rightCardHolder.setOpaque(false);
        add(left, BorderLayout.WEST);
        add(rightCardHolder, BorderLayout.CENTER);
    }

    private JComponent resourceRow(String title, JLabel valueLabel) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setOpaque(false);
        JLabel t = new JLabel(title + ": ");
        valueLabel.setFont(valueLabel.  getFont().deriveFont(Font.BOLD));
        row.add(t);
        row.add(valueLabel);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    public void setMoney(int money) {
        moneyLbl.setText("$" + money);
        revalidate();
        repaint();
    }

    public void setResources(int coal, int oil, int gas, int uranium) {
        coalLbl.setText(Integer.toString(coal));
        oilLbl.setText(Integer.toString(oil));
        gasLbl.setText(Integer.toString(gas));
        uraniumLbl.setText(Integer.toString(uranium));
        revalidate();
        repaint();
    }

    /** Show/update the player’s owned plant cards on the right */
    public void setPlantDeck(Deck<PowerGridCard> plantDeck) {
        Rectangle area = new Rectangle(0, 0, 3 * 100, 110); // ~3 cards visible
        rightCardHolder.removeAll();
        plantView = new PowerGridDeckView(
                playerId, plantDeck, true, PowerGridParameters.CARD_ASSET_PATH, area
        );
        rightCardHolder.add(plantView, BorderLayout.CENTER);
        rightCardHolder.revalidate();
        rightCardHolder.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1000, 200); // wider to give the cards space
    }
}

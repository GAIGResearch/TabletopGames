package games.descent2e.pcg;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

public class GenerateBoardsGUI {

    int FIRSTLOOP = 500;
    int GENERATIONLOOP = 200;
    int OFFSPRING = 10;
    int INFEASIBLE = 3;

    final JFrame mainWindow = new JFrame("Descent (Second Edition) Procedurally Generated Board Creator");
    JPanel mainPanel;

    public GenerateBoardsGUI() {
    }

    public void load() {
        mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainWindow.setSize(1200, 500);
        mainWindow.setResizable(false);
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setLayout(new GridLayout(0, 1,10, 10));

        mainPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        mainPanel.setBackground(Color.CYAN);

        mainWindow.add(mainPanel);

        JPanel firstloopcontainer = new JPanel(new FlowLayout());
        JLabel floop = new JLabel("Initialisation Loop:");
        NumberField floopcount = new NumberField(FIRSTLOOP);
        floopcount.addPropertyChangeListener(floopcount);
        floopcount.setColumns(4);
        floopcount.setMargin(new Insets(5, 10, 5, 10));
        floopcount.setMaximumSize(new Dimension(80, 50));

        firstloopcontainer.add(floop);
        firstloopcontainer.add(floopcount);

        JSlider firstloop = new JSlider(JSlider.HORIZONTAL, 0, 1000, 500);
        firstloop.addChangeListener(new SliderListener(floopcount));

        floopcount.setSlider(firstloop);

        firstloop.setMajorTickSpacing(200);
        firstloop.setMinorTickSpacing(50);
        firstloop.setPaintTicks(true);
        firstloop.setPaintLabels(true);

        firstloopcontainer.add(firstloop);
        mainPanel.add(firstloopcontainer);

        Button create = makeButton("Generate!");
        mainPanel.add(create);
        create.setEnabled(true);

        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(create.isEnabled()) {
                    CreateOffspring co = new CreateOffspring(FIRSTLOOP, GENERATIONLOOP, OFFSPRING, INFEASIBLE);
                    try {
                        co.begin();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    create.setEnabled(false);
                }
            }
        });

        mainWindow.setVisible(true);
    }

    public Button makeButton(String label) {
        Button button = new Button(label);
        button.setFocusable(false);
        button.setFont(new Font("Arial", Font.PLAIN, 10));
        return button;
    }

    class NumberField extends JFormattedTextField implements PropertyChangeListener {
        private JSlider slider;

        public NumberField(int value) {
            super(value);
        }

        public void setSlider(JSlider slider){
            this.slider = slider;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            int value = ((Number) this.getValue()).intValue();
            FIRSTLOOP = Math.min(Math.max(value, 4), 1000);
            this.setValue(FIRSTLOOP);
            if (slider != null)
                slider.setValue(FIRSTLOOP);
        }
    }

    class SliderListener implements ChangeListener {
        private JFormattedTextField text;
        public SliderListener(JFormattedTextField text) {
            this.text = text;
        }

        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                int result = source.getValue();
                FIRSTLOOP = Math.max(result, 4);
                text.setValue(FIRSTLOOP);
            }
        }
    }
}

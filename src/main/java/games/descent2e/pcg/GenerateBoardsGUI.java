package games.descent2e.pcg;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

public class GenerateBoardsGUI {

    int firstMin = 0;
    int firstMax = 1000;

    int secondMin = 0;
    int secondMax = 500;

    int offspringMin = 0;
    int offspringMax = 50;

    int infeasibleMin = 0;
    int infeasibleMax = 100;

    int FIRSTLOOP = firstMax / 2;
    int GENERATIONLOOP = secondMax / 2;
    int OFFSPRING = 10;
    int INFEASIBLE = 30;

    int defaultFirst = FIRSTLOOP;
    int defaultSecond = GENERATIONLOOP;
    int defaultOffspring = OFFSPRING;
    int defaultInfeasible = INFEASIBLE;

    int IDEAL_SIZE = 166;
    int IDEAL_GROUP = 5;
    float IDEAL_HEALTH = 5.872f;

    final JFrame mainWindow = new JFrame("Descent (Second Edition) Procedurally Generated Board Creator");
    JPanel mainPanel;

    TotalLabel totalGenerated = new TotalLabel();
    JSlider firstLoopSlide;
    JSlider secondLoopSlide;
    JSlider offspringLoopSlide;
    JSlider infeasibleSlide;

    public GenerateBoardsGUI() {
    }

    public void load() {
        mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainWindow.setSize(1200, 500);
        mainWindow.setResizable(false);
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setLayout(new GridLayout(0, 1,10, 10));

        mainPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        mainPanel.setBackground(Color.CYAN);

        mainWindow.add(mainPanel);

        JPanel generatorControls = new JPanel(new GridLayout(0, 1, 5, 5));

        JPanel firstloopcontainer = new JPanel(new FlowLayout());
        JLabel floop = new JLabel("Initialisation Loop:");

        NumberField floopcount = new NumberField(FIRSTLOOP, Category.firstLoop, firstMin, firstMax);
        floopcount.addPropertyChangeListener(floopcount);
        floopcount.setColumns(4);
        floopcount.setMargin(new Insets(5, 10, 5, 10));
        floopcount.setMaximumSize(new Dimension(80, 50));

        firstLoopSlide = new JSlider(JSlider.HORIZONTAL, firstMin, firstMax, FIRSTLOOP);
        firstLoopSlide.addChangeListener(new SliderListener(floopcount, Category.firstLoop, firstMin, firstMax));
        firstLoopSlide.setMajorTickSpacing(200);
        firstLoopSlide.setMinorTickSpacing(50);
        firstLoopSlide.setPaintTicks(true);
        firstLoopSlide.setPaintLabels(true);
        floopcount.setSlider(firstLoopSlide);

        firstloopcontainer.add(floop);
        firstloopcontainer.add(floopcount);
        firstloopcontainer.add(firstLoopSlide);
        generatorControls.add(firstloopcontainer);

        JPanel secondloopcontainer = new JPanel(new FlowLayout());
        JLabel sloop = new JLabel("Generation Loop:");

        NumberField sloopcount = new NumberField(GENERATIONLOOP, Category.secondLoop, secondMin, secondMax);
        sloopcount.addPropertyChangeListener(sloopcount);
        sloopcount.setColumns(3);
        sloopcount.setMargin(new Insets(5, 10, 5, 10));
        sloopcount.setMaximumSize(new Dimension(80, 50));

        secondLoopSlide = new JSlider(JSlider.HORIZONTAL, secondMin, secondMax, GENERATIONLOOP);
        secondLoopSlide.addChangeListener(new SliderListener(sloopcount, Category.secondLoop, secondMin, secondMax));
        secondLoopSlide.setMajorTickSpacing(100);
        secondLoopSlide.setMinorTickSpacing(20);
        secondLoopSlide.setPaintTicks(true);
        secondLoopSlide.setPaintLabels(true);
        sloopcount.setSlider(secondLoopSlide);

        secondloopcontainer.add(sloop);
        secondloopcontainer.add(sloopcount);
        secondloopcontainer.add(secondLoopSlide);
        generatorControls.add(secondloopcontainer);

        JPanel offspringloopcontainer = new JPanel(new FlowLayout());
        JLabel oloop = new JLabel("Offspring Per Generation:");

        NumberField oloopcount = new NumberField(OFFSPRING, Category.offspring, offspringMin, offspringMax);
        oloopcount.addPropertyChangeListener(oloopcount);
        oloopcount.setColumns(2);
        oloopcount.setMargin(new Insets(5, 10, 5, 10));
        oloopcount.setMaximumSize(new Dimension(80, 50));

        offspringLoopSlide = new JSlider(JSlider.HORIZONTAL, offspringMin, offspringMax, OFFSPRING);
        offspringLoopSlide.addChangeListener(new SliderListener(oloopcount, Category.offspring, offspringMin, offspringMax));
        offspringLoopSlide.setMajorTickSpacing(5);
        offspringLoopSlide.setMinorTickSpacing(1);
        offspringLoopSlide.setPaintTicks(true);
        offspringLoopSlide.setPaintLabels(true);
        oloopcount.setSlider(offspringLoopSlide);

        offspringloopcontainer.add(oloop);
        offspringloopcontainer.add(oloopcount);
        offspringloopcontainer.add(offspringLoopSlide);
        generatorControls.add(offspringloopcontainer);

        JPanel infeasiblecontainer = new JPanel(new FlowLayout());
        JLabel infeasibleLabel = new JLabel("Probability of Infeasible Parents:");

        NumberField infeasiblecount = new NumberField(INFEASIBLE, Category.infeasible, infeasibleMin, infeasibleMax);
        infeasiblecount.addPropertyChangeListener(infeasiblecount);
        infeasiblecount.setColumns(3);
        infeasiblecount.setMargin(new Insets(5, 10, 5, 10));
        infeasiblecount.setMaximumSize(new Dimension(80, 50));

        infeasibleSlide = new JSlider(JSlider.HORIZONTAL, infeasibleMin, infeasibleMax, INFEASIBLE);
        infeasibleSlide.addChangeListener(new SliderListener(infeasiblecount, Category.infeasible, infeasibleMin, infeasibleMax));
        infeasibleSlide.setMajorTickSpacing(10);
        infeasibleSlide.setMinorTickSpacing(5);
        infeasibleSlide.setPaintTicks(true);
        infeasibleSlide.setPaintLabels(true);
        infeasiblecount.setSlider(infeasibleSlide);

        infeasiblecontainer.add(infeasibleLabel);
        infeasiblecontainer.add(infeasiblecount);
        infeasiblecontainer.add(infeasibleSlide);
        generatorControls.add(infeasiblecontainer);

        JPanel totalHolder = new JPanel(new FlowLayout());
        totalGenerated.updateText();
        totalHolder.add(totalGenerated);
        generatorControls.add(totalHolder);

        Border blackline = BorderFactory.createLineBorder(Color.black);
        TitledBorder generatorSettings = new TitledBorder(blackline, "Generator Settings");
        generatorSettings.setTitleJustification(TitledBorder.CENTER);
        generatorControls.setBorder(generatorSettings);

        JPanel resetHolder = new JPanel(new FlowLayout());
        JButton resetGeneration = new JButton("Reset to Defaults");

        resetGeneration.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                firstLoopSlide.setValue(defaultFirst);
                secondLoopSlide.setValue(defaultSecond);
                offspringLoopSlide.setValue(defaultOffspring);
                infeasibleSlide.setValue(defaultInfeasible);
                totalGenerated.updateText();
            }
        });

        resetHolder.add(resetGeneration);
        generatorControls.add(resetHolder);

        mainPanel.add(generatorControls);

        Button create = makeButton("Generate!");
        mainPanel.add(create);
        create.setEnabled(true);

        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(create.isEnabled()) {
                    System.out.println("Generating " + (FIRSTLOOP + (GENERATIONLOOP * OFFSPRING)) + " Boards, with " + INFEASIBLE +"% chance of Infeasible Pool Parents!");
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

    class TotalLabel extends JLabel {
        public void updateText() {
            int total = FIRSTLOOP + (GENERATIONLOOP * OFFSPRING);
            setText("Generating " + total + " new Descent Quests with " + INFEASIBLE + "% likelihood of drawing parents from the Infeasible pool.");
        }
    }

    class NumberField extends JFormattedTextField implements PropertyChangeListener {
        private JSlider slider;
        private final Category category;
        private final double min;
        private final double max;

        public NumberField(int value, Category category, double min, double max) {
            super(value);
            this.category = category;
            this.min = min;
            this.max = max;
        }

        public void setSlider(JSlider slider){
            this.slider = slider;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            int value = ((Number) this.getValue()).intValue();
            final double result = Math.min(Math.max(value, min), max);
            switch (category) {
                case firstLoop -> {
                    FIRSTLOOP = (int) Math.max(result, 4);
                    this.setValue(FIRSTLOOP);
                    if (slider != null)
                        slider.setValue(FIRSTLOOP);
                }
                case secondLoop -> {
                    GENERATIONLOOP = (int) result;
                    this.setValue(GENERATIONLOOP);
                    if (slider != null)
                        slider.setValue(GENERATIONLOOP);
                }
                case offspring -> {
                    OFFSPRING = (int) result;
                    this.setValue(OFFSPRING);
                    if (slider != null)
                        slider.setValue(OFFSPRING);
                }
                case infeasible -> {
                    INFEASIBLE = (int) result;
                    this.setValue(INFEASIBLE);
                    if (slider != null)
                        slider.setValue(INFEASIBLE);
                }
            }
            totalGenerated.updateText();
        }
    }

    class SliderListener implements ChangeListener {
        private final Category category;
        private final JFormattedTextField text;
        private final double min;
        private final double max;
        public SliderListener(JFormattedTextField text, Category category, double min, double max) {
            this.text = text;
            this.category = category;
            this.min = min;
            this.max = max;
        }

        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider)e.getSource();
            if (!source.getValueIsAdjusting()) {
                double value = source.getValue();
                double result = Math.max(min, Math.min(value, max));
                switch (category) {
                    case firstLoop -> {
                        FIRSTLOOP = (int) Math.max(result, 4);
                        text.setValue(FIRSTLOOP);
                        source.setValue(FIRSTLOOP);
                    }
                    case secondLoop -> {
                        GENERATIONLOOP = (int) result;
                        text.setValue(GENERATIONLOOP);
                        source.setValue(GENERATIONLOOP);
                    }
                    case offspring -> {
                        OFFSPRING = (int) result;
                        text.setValue(OFFSPRING);
                        source.setValue(OFFSPRING);
                    }
                    case infeasible -> {
                        INFEASIBLE = (int) result;
                        text.setValue(INFEASIBLE);
                        source.setValue(INFEASIBLE);
                    }
                }
                totalGenerated.updateText();
            }
        }
    }

    enum Category {
        firstLoop,
        secondLoop,
        offspring,
        infeasible,
        idealHealth,
        idealSize,
        idealGroups
    }
}

package players.reinforcement;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import javax.swing.JFrame;
import java.util.LinkedList;
import java.util.Queue;

public class RewardChart {
    private XYSeries series;
    private JFreeChart chart;
    private Queue<Double> rewardQueue;
    private int rollingLength;
    private double rollingSum;

    public RewardChart(String title, int rollingLength) {
        this.rollingLength = rollingLength;
        this.rewardQueue = new LinkedList<>();
        this.rollingSum = 0;

        // 初始化数据系列
        series = new XYSeries("Reward");

        // 使用数据系列创建一个集合
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        // 创建图表
        chart = ChartFactory.createXYLineChart(
                title, // 图表标题
                "Episode", // x轴标签
                "Reward", // y轴标签
                dataset, // 数据集
                PlotOrientation.VERTICAL,
                true, // 显示图例
                true, // 使用 tooltips
                false // 不生成 URLs
        );

        // 获取图表的 plot 对象以修改细节
        XYPlot plot = chart.getXYPlot();
        // 设置 Y 轴范围
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setRange(-2.0, 2.0); // 假设奖励的范围从 -2 到 2
    }

    // 更新图表的方法
    public void updateChart(int episode, double reward) {
        rewardQueue.add(reward);
        rollingSum += reward;

        if (rewardQueue.size() > rollingLength) {
            rollingSum -= rewardQueue.poll();
        }

        double rollingAverage = rollingSum / rewardQueue.size();
        series.add(episode, rollingAverage);
    }

    // 显示图表的方法
    public void displayChart() {
        JFrame frame = new JFrame("Training Progress");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

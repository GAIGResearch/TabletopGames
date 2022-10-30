package players.learners;

import org.apache.spark.ml.feature.RFormula;
import org.apache.spark.ml.regression.*;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;

import java.io.FileWriter;
import java.util.*;

import static java.util.stream.Collectors.*;

public class ApacheLogisticLearner extends AbstractLearner {

    static boolean debug = false;
    static SparkSession spark = SparkSession
            .builder()
            .appName("Java Spark SQL basic example")
            //     .config("spark.driver.memory", "1g")
            .master("local").getOrCreate();
    double[] coefficients;

    public static void main(String[] args) {

        Dataset<Row> df = spark.read()
                .option("delimiter", "\t")
                .option("header", "true")
                .option("inferSchema", "true")
                .csv("LL_Log_4P_2.data");

        df.show(10);

        String[] regressors = new String[]{"SCORE", "SCORE_ADV", "TREASURE", "ACTION", "SILVER_IN_DECK", "GOLD_IN_DECK",
                "PROVINCE_IN_DECK", "ESTATE_IN_DECK", "DUCHY_IN_DECK", "TR_H", "AC_LEFT", "BUY_LEFT", "TOT_CRDS", "ROUND11", "OUR_TURN"};
        // headers are not case-sensitive, so ORDINAL7 is the current position, and Ordinal119 is the final achieved position
        try {
            df.createTempView("data");
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        // for 4 players
        df = spark.sql("select " + String.join(", ", regressors) + ", (1 - (Ordinal119 - 1) / 3) as Ordinal From data");

        df.show(10);

        RFormula formula = new RFormula()
                .setFormula("Ordinal ~ " + String.join(" + ", regressors))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(df).transform(df).select("features", "target");

        training.show(10);

        GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                .setMaxIter(10)
                .setFamily("Binomial")
                .setLink("Logit")
                .setRegParam(0.01)
                //       .setElasticNetParam(0.8)
                .setLabelCol("target")
                .setFeaturesCol("features");

        GeneralizedLinearRegressionModel lrModel = lr.fit(training);

        System.out.println(lrModel.coefficients());


    }

    @Override
    public void learnFrom(String... files) {
        loadData(files);
        // first add the target to the data array so that we can convert to an apache dataset (we just add on the target)
        double[][] apacheDataArray = new double[dataArray.length][dataArray[0].length + 1];
        for (int i = 0; i < dataArray.length; i++) {
            System.arraycopy(dataArray[i], 0, apacheDataArray[i], 0, descriptions.length + 1);
            apacheDataArray[i][descriptions.length + 1] = target[i][0]; // add target to end
        }
        // convert the raw data into Rows
        List<Row> rowList = Arrays.stream(apacheDataArray).map(doubles -> RowFactory.create(Arrays.stream(doubles).boxed())).collect(toList());
        // use the header to get the names, and all of them are double by design
        String[] apacheHeader = new String[descriptions.length + 2];
        apacheHeader[0] = "BIAS";
        System.arraycopy(descriptions, 0, apacheHeader, 1, descriptions.length);
        apacheHeader[descriptions.length + 1] = "target";
        // set up the column names
        StructType schema = new StructType(Arrays.stream(apacheHeader)
                .map(name -> new StructField(name, DataTypes.DoubleType, true, Metadata.empty())).toArray(StructField[]::new));

        // and convert to an apache Dataset
        Dataset<Row> apacheData = spark.createDataFrame(rowList, schema);

        if (debug)
            apacheData.show(10);

        RFormula formula = new RFormula()
                .setFormula("target ~ " + String.join("BIAS + ", descriptions))
                .setFeaturesCol("features")
                .setLabelCol("target");

        Dataset<Row> training = formula.fit(apacheData).transform(apacheData).select("features", "target");

        if (debug)
            training.show(10);

        GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                .setMaxIter(10)
                .setFamily("Binomial")
                .setLink("Logit")
                .setRegParam(0.1)
                //       .setElasticNetParam(0.8)
                .setLabelCol("target")
                .setFeaturesCol("features");

        GeneralizedLinearRegressionModel lrModel = lr.fit(training);

        if (debug)
            System.out.println(lrModel.coefficients());

        coefficients = lrModel.coefficients().toArray();

    }

    @Override
    public void writeToFile(String file) {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("BIAS\t" + String.join("\t", descriptions) + "\n");
            writer.write(Arrays.stream(coefficients).mapToObj(d -> String.format("%.3g", d)).collect(joining("\t")));
            writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String name() {
        return "ApacheLogistic";
    }
}

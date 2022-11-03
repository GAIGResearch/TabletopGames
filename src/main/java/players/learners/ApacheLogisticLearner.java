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
                .csv("Apache_0.data");

        df.show(10);

        String[] regressors = new String[]{"POINT_ADVANTAGE", "POINTS", "THREE_BOXES", "TWO_BOXES"};


        //"GOLD_IN_DECK", "PROVINCE_IN_DECK", "ESTATE_IN_DECK", "DUCHY_IN_DECK", "TR_H", "AC_LEFT", "BUY_LEFT", "TOT_CRDS", "ROUND11", "OUR_TURN"};

        // headers are not case-sensitive, so ORDINAL7 is the current position, and Ordinal119 is the final achieved position
        try {
            df.createTempView("data");
        } catch (AnalysisException e) {
            e.printStackTrace();
        }
        // for 4 players
        df = spark.sql(String.format("select %s, (1 - (Ordinal13 - 1) / 3) as Ordinal From data", String.join(", ", regressors)));

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
        public void learnFrom (String...files){
            loadData(files);
            // first add the target to the data array so that we can convert to an apache dataset (we just add on the target)
            double[][] apacheDataArray = new double[dataArray.length][dataArray[0].length];
            for (int i = 0; i < dataArray.length; i++) {
                // we skip the BIAS at the front here, as we add that in separately
                System.arraycopy(dataArray[i], 1, apacheDataArray[i], 0, descriptions.length);
                apacheDataArray[i][descriptions.length] = target[i][0]; // add target to end
            }
            // convert the raw data into Rows
            List<Row> rowList = Arrays.stream(apacheDataArray)
                    .map(doubleArray -> Arrays.stream(doubleArray).boxed().toArray())
                    .map(RowFactory::create)
                    .collect(toList());
            // use the header to get the names, and all of them are double by design
            String[] apacheHeader = new String[descriptions.length + 1];
            System.arraycopy(descriptions, 0, apacheHeader, 0, descriptions.length);
            apacheHeader[descriptions.length] = "target";
            // set up the column names
            StructType schema = new StructType(Arrays.stream(apacheHeader)
                    .map(name -> new StructField(name, DataTypes.DoubleType, true, Metadata.empty()))
                    .toArray(StructField[]::new)
            );

            // and convert to an apache Dataset
            Dataset<Row> apacheData = spark.createDataFrame(rowList, schema);

            if (debug)
                apacheData.show(10);

            RFormula formula = new RFormula()
                    .setFormula("target ~ " + String.join(" + ", descriptions))
                    .setFeaturesCol("features")
                    .setLabelCol("target");

            Dataset<Row> training = formula.fit(apacheData).transform(apacheData).select("features", "target");

            if (debug)
                training.show(10);

            GeneralizedLinearRegression lr = new GeneralizedLinearRegression()
                    .setFitIntercept(true)
                    .setMaxIter(10)
                    .setFamily("Binomial")
                    .setLink("Logit")
                    .setRegParam(0.1)
                    .setLabelCol("target")
                    .setFeaturesCol("features");

            GeneralizedLinearRegressionModel lrModel = lr.fit(training);

            if (debug)
                System.out.println(lrModel.coefficients());

            coefficients = new double[descriptions.length + 1];
            coefficients[0] = lrModel.intercept();
            double[] coeffs = lrModel.coefficients().toArray();
            System.arraycopy(coeffs, 0, coefficients, 1, coeffs.length);

        }

        @Override
        public void writeToFile (String file) {
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

package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.lang.Math;
import java.text.DecimalFormat;

import core.Data;
import core.Main;

public class Utils {

	public enum LogTag {
		SEMANTICS,
		FITNESSTEST,
		FITNESSTRAIN,
		FITNESSVALIDATION,
		REPULSERDISTANCES,
		LOG
	}

	private static PrintWriter fsSemantics = null;
	private static PrintWriter fsFitnesstest = null;
	private static PrintWriter fsFitnesstrain = null;
	private static PrintWriter fsFitnessval = null;
	private static PrintWriter fslog = null;
	private static PrintWriter fsRepulserDistances = null;

	private static DecimalFormat decFormatter;

	private static int dataHeaderLines = 0;

	public static Data loadData(String dataTrainFilename, String dataTestFilename, double validationSetSize, boolean shuffleValidationSplit) {
		double[][] trainingData = Utils.readData(dataTrainFilename);
		double[][] unseenData = Utils.readData(dataTestFilename);
		if (validationSetSize > 0){
			// shuffle data set
			if (shuffleValidationSplit){
				int currentIndex = trainingData.length;
				double[] temporaryValue;
				int randomIndex;
				while (currentIndex != 0) {
					randomIndex = (int)Math.floor(Math.random() * currentIndex);
					currentIndex -= 1;
					temporaryValue = trainingData[currentIndex];
					trainingData[currentIndex] = trainingData[randomIndex];
					trainingData[randomIndex] = temporaryValue;
				}
			}
			// split data set
			int numTrainInstances = (int)Math.ceil(trainingData.length*(1-validationSetSize));
			double[][] trainingNewData = Arrays.copyOfRange(trainingData, 0, numTrainInstances);
			double[][] validationData = Arrays.copyOfRange(trainingData, numTrainInstances, trainingData.length);

			log(LogTag.LOG, "\n\tRead training data from "+dataTrainFilename+" with "+trainingData.length+" instances");
			log(LogTag.LOG, "\tRead test data from " + dataTestFilename+ " with "+unseenData.length+" instances");
			log(LogTag.LOG, "\tSplit original trainingset (" + trainingData.length + " instances) by "+validationSetSize+" into " + trainingNewData.length + " training instances and " + validationData.length + " validation instances.");
			return new Data(trainingNewData, validationData, unseenData);
		}
		return new Data(trainingData, unseenData);
	}

	public static double[][] readData(String filename) {
		double[][] data = null;
		List<String> allLines = new ArrayList<String>();
		int ignored = 0;
		try {
			BufferedReader inputBuffer = new BufferedReader(new FileReader(filename));
			String line = inputBuffer.readLine();
			while (line != null) {
				if (ignored < dataHeaderLines)
					ignored++;
				else
					allLines.add(line);
				line = inputBuffer.readLine();
			}
			inputBuffer.close();
		} catch (Exception e) {
			System.out.println(e);
		}

		StringTokenizer tokens = new StringTokenizer(allLines.get(0).trim());
		int numberOfColumns = tokens.countTokens();
		data = new double[allLines.size()][numberOfColumns];
		for (int i = 0; i < data.length; i++) {
			tokens = new StringTokenizer(allLines.get(i).trim());
			for (int k = 0; k < numberOfColumns; k++) {
				data[i][k] = Double.parseDouble(tokens.nextToken().trim());
			}
		}
		return data;
	}

	public static void readConfigFile(String configFile){
		try {
			log(LogTag.LOG, "Reading Configuration: " + configFile);
			BufferedReader inputBuffer = new BufferedReader(new FileReader(configFile));
			String line = inputBuffer.readLine();
			while (line != null) {
				try{
					String[] parts = line.replaceAll("\\s","").split("=");
					switch (parts[0]) {
						case "number_of_generations":
							Main.NUMBER_OF_GENERATIONS = Integer.parseInt(parts[1]);
							break;
						case "number_of_runs":
							Main.NUMBER_OF_RUNS = Integer.parseInt(parts[1]);
							break;
						case "data_train_filename":
							Main.DATA_TRAIN_FILENAME = parts[1];
							break;
						case "data_test_filename":
							Main.DATA_TEST_FILENAME = parts[1];
							break;
						case "population_size":
							Main.POPULATION_SIZE = Integer.parseInt(parts[1]);
							break;
						case "apply_depth_limit":
							Main.APPLY_DEPTH_LIMIT = (Integer.parseInt(parts[1]) == 1);
							break;
						case "maximum_depth":
							Main.MAXIMUM_DEPTH = Integer.parseInt(parts[1]);
							break;
						case "maximum_initial_depth":
							Main.MAXIMUM_INITIAL_DEPTH = Integer.parseInt(parts[1]);
							break;
						case "crossover_probability":
							Main.CROSSOVER_PROBABILITY = Double.parseDouble(parts[1]);
							break;
						case "print_at_each_generation":
							Main.PRINT_AT_EACH_GENERATION = (Integer.parseInt(parts[1]) == 1);
							break;
						case "validation_set_size":
							Main.VALIDATION_SET_SIZE = Double.parseDouble(parts[1]);
							break;
						case "repulsor_min_age":
							Main.REPULSOR_MIN_AGE = Integer.parseInt(parts[1]);
							break;
						case "semantic_repulsor_max_number":
							Main.SEMANTIC_REPULSOR_MAX_NUMBER = Integer.parseInt(parts[1]);
							break;
						case "validation_elite_size":
							Main.VALIDATION_ELITE_SIZE = Integer.parseInt(parts[1]);
							break;
						case "use_best_as_rep_candidate":
							Main.USE_BEST_AS_REP_CANDIDATE = Integer.parseInt(parts[1]);
							break;
						case "overfit_by_median":
							Main.OVERFIT_BY_MEDIAN = (Integer.parseInt(parts[1]) == 1);
							break;
						case "shuffle_validation_split":
							Main.SHUFFLE_VALIDATION_SPLIT = (Integer.parseInt(parts[1]) == 1);
							break;
						case "tournament_size":
							Main.TOURNAMENT_SIZE = Integer.parseInt(parts[1]);
							break;
						case "log_semantics":
							Main.LOG_SEMANTICS = (Integer.parseInt(parts[1]) == 1);
							break;
						case "data_header_lines":
							dataHeaderLines = Integer.parseInt(parts[1]);
							break;
						case "aggregate_repulsors":
							Main.AGGREGATE_REPULSORS = (Integer.parseInt(parts[1])==1);
							break;
						case "force_avoid_repulsors":
							Main.FORCE_AVOID_REPULSORS = (Integer.parseInt(parts[1])==1);
							break;
						case "equality_delta":
							Main.EQUALITY_DELTA = Double.parseDouble(parts[1]);
							break;
						case "true_pareto_selection":
							Main.TRUE_PARETO_SELECTION = (Integer.parseInt(parts[1]) == 1);
							break;
						case "domination_exclude_fitness":
							Main.DOMINATION_EXCLUDE_FITNESS = (Integer.parseInt(parts[1]) == 1);
							break;
						case "merge_repulsors":
							Main.MERGE_REPULSORS = (Integer.parseInt(parts[1]) == 1);
							break;
					}
				} catch (Exception e){
					log(LogTag.LOG, "Failed reading configuration: " + line);
				}
				log(LogTag.LOG, "\t"+line);
				line = inputBuffer.readLine();
			}
			inputBuffer.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static double getAverage(double[] values) {
		double sum = 0.0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
		}
		return sum / values.length;
	}

	public static double logisticFunction(double x) {
		return 1.0 / (1.0 + Math.exp(-x));
	}

	public static void attachLogger(String stamp){
		File f = new File(Main.OUTPUT_DIR);
		f.mkdir();
		try{
			fsFitnesstest = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnesstest.txt", "UTF-8");
		} catch (Exception e){}
		try{
			fsFitnesstrain = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnesstrain.txt", "UTF-8");
		} catch (Exception e){}
		try{
			fsFitnessval = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnessvalidation.txt", "UTF-8");
		} catch (Exception e){}
		try{
			fslog = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-gp.log", "UTF-8");
		} catch (Exception e){}
		try{
			fsRepulserDistances = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-repulserdistances.txt", "UTF-8");
		} catch (Exception e){}
	}

	public static void attachLogger(String stamp, LogTag logger){
		switch (logger) {
			case SEMANTICS:
				try{
					fsSemantics = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-Semantics.txt", "UTF-8");
				} catch (Exception e){}
				break;
			case FITNESSTEST:
				try{
					fsFitnesstest = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnesstest.txt", "UTF-8");
				} catch (Exception e){}
				break;
			case FITNESSTRAIN:
				try{
					fsFitnesstrain = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnesstrain.txt", "UTF-8");
				} catch (Exception e){}
				break;
			case FITNESSVALIDATION:
				try{
					fsFitnessval = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-fitnessvalidation.txt", "UTF-8");
				} catch (Exception e){}
				break;
			case REPULSERDISTANCES:
				try{
					fsRepulserDistances = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-repulserdistances.txt", "UTF-8");
				} catch (Exception e){}
				break;
			case LOG:
				try{
					fslog = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-gp.log", "UTF-8");
				} catch (Exception e){}
				break;
		}
	}

	public static void log(LogTag tag, String line){
		PrintWriter out = null;
		switch (tag){
			case SEMANTICS:
				out = fsSemantics;
				break;
			case FITNESSTEST:
				out = fsFitnesstest;
				break;
			case FITNESSTRAIN:
				out = fsFitnesstrain;
				break;
			case FITNESSVALIDATION:
				out = fsFitnessval;
				break;
			case REPULSERDISTANCES:
				out = fsRepulserDistances;
				break;
			case LOG:
				out = fslog;
				break;
		}
		if (out != null){
			out.println(line);
		}
	}

	public static void detachLogger(){
		try{
			fsSemantics.close();
			fsSemantics = null;
		} catch(Exception e){}
		try{
			fsFitnesstest.close();
			fsFitnesstest = null;
		} catch(Exception e){}
		try{
			fsFitnesstrain.close();
			fsFitnesstrain = null;
		} catch(Exception e){}
		try{
			fsFitnessval.close();
			fsFitnessval = null;
		} catch(Exception e){}
		try{
			fslog.close();
			fslog = null;
		} catch(Exception e){}
		try{
			fsRepulserDistances.close();
			fsRepulserDistances = null;
		} catch(Exception e){}
	}

	public static void initDecFormatter(){
		Utils.decFormatter = new DecimalFormat("0.######E0");
		Utils.decFormatter.setMaximumFractionDigits(5);
	}

	public static String format(double num){
		String s = ""+num;
		if (s.length() > 14)
			return Utils.decFormatter.format(num).toLowerCase().replaceAll("e0", "");
		else
			return s;
	}
}

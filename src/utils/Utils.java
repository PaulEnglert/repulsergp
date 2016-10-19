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

import core.Data;
import core.Main;

public class Utils {
	
	public enum LogTag {
	    SEMANTICS,
	    FITNESSTEST,
	    FITNESSTRAIN,
	    FITNESSVALIDATION,
	    LOG
	}

	private static PrintWriter fsSemantics = null;
	private static PrintWriter fsFitnesstest = null;
	private static PrintWriter fsFitnesstrain = null;
	private static PrintWriter fsFitnessval = null;
	private static PrintWriter fslog = null;

	public static Data loadData(String dataFilename, double validationSetSize, boolean shuffleValidationSplit) {
		double[][] trainingData = Utils.readData(dataFilename + "_training.txt");
		double[][] unseenData = Utils.readData(dataFilename + "_unseen.txt");
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
			int numTrainInstances = (int)Math.floor(trainingData.length*(1-validationSetSize));
			double[][] trainingNewData = Arrays.copyOfRange(trainingData, 0, numTrainInstances);
			double[][] validationData = Arrays.copyOfRange(trainingData, numTrainInstances, trainingData.length);
			
			System.out.println("\tSplit original trainingset (" + trainingData.length + " instances) into " + trainingNewData.length + " training instances and " + validationData.length + " validation instances.");
			return new Data(trainingNewData, validationData, unseenData);
		}
		return new Data(trainingData, unseenData);
	}

	public static double[][] readData(String filename) {
		double[][] data = null;
		List<String> allLines = new ArrayList<String>();
		try {
			BufferedReader inputBuffer = new BufferedReader(new FileReader(filename));
			String line = inputBuffer.readLine();
			while (line != null) {
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
						case "data_filename":
							Main.DATA_FILENAME = parts[1];
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
						case "use_only_best_as_rep_candidate":
							Main.USE_ONLY_BEST_AS_REP_CANDIDATE = (Integer.parseInt(parts[1]) == 1);
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
			fslog = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-gp.txt", "UTF-8");
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
			case LOG:
				try{
					fslog = new PrintWriter(Main.OUTPUT_DIR+"/"+stamp+"-gp.txt", "UTF-8");
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
	}
}

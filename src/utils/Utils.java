package utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.lang.Math;

import core.Data;
import core.Main;

public class Utils {

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
			System.out.println("\tReading Configuration: " + configFile);
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
					} 
				} catch (Exception e){
					System.out.println("\t\tERROR: Failed reading configuration: " + line);
				}
				System.out.println("\t\t"+line);
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
}

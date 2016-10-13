package core;

import utils.Utils;

public class Main {

	// general parameters
	public static String APPID = "default";
	public static String DATA_FILENAME = "dataset";
	public static int NUMBER_OF_RUNS = 1;
	public static int NUMBER_OF_GENERATIONS = 5;

	// GP Parameters
	public static int POPULATION_SIZE = 100;
	public static boolean APPLY_DEPTH_LIMIT = true;
	public static int MAXIMUM_DEPTH = 17;
	public static int MAXIMUM_INITIAL_DEPTH = 6;
	public static double CROSSOVER_PROBABILITY = 0.9;
	public static boolean PRINT_AT_EACH_GENERATION = true;

	public static void main(String[] args) {
		Long startTime = System.currentTimeMillis();
		// load configuration file
		parseArguments(args);

		// load training and unseen data
		Data data = Utils.loadData(DATA_FILENAME);


		// run GP for a given number of runs
		double[][] resultsPerRun = new double[4][NUMBER_OF_RUNS];
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			System.out.printf("\n\t\t##### Run %d #####\n", i + 1);
			GpRun gp = new GpRun(data);
			// GsgpRun gp = new GsgpRun(data);

			// set parameters
			gp.setPopulationSize(POPULATION_SIZE);
			gp.setApplyDepthLimit(APPLY_DEPTH_LIMIT);
			gp.setMaximumDepth(MAXIMUM_DEPTH);
			gp.setMaximumInitialDepth(MAXIMUM_INITIAL_DEPTH);
			gp.setCrossoverProbability(CROSSOVER_PROBABILITY);
			gp.setPrintAtEachGeneration(PRINT_AT_EACH_GENERATION);

			gp.evolve(NUMBER_OF_GENERATIONS);
			Individual bestFound = gp.getCurrentBest();
			resultsPerRun[0][i] = bestFound.getTrainingError();
			resultsPerRun[1][i] = bestFound.getUnseenError();
			resultsPerRun[2][i] = bestFound.getSize();
			resultsPerRun[3][i] = bestFound.getDepth();
			System.out.print("\nBest =>");
			bestFound.print();
			System.out.println();
		}

		// present average results
		System.out.printf("\n\t\t##### AVERAGE results after "+NUMBER_OF_RUNS+" runs ("+NUMBER_OF_GENERATIONS+" Generations) #####\n\n");
		System.out.println("Training Error   \tTest Error      \tSize\tDepth");
		System.out.println(""+Utils.getAverage(resultsPerRun[0])+"\t"+Utils.getAverage(resultsPerRun[1])+"\t"+Utils.getAverage(resultsPerRun[2])+"\t"+Utils.getAverage(resultsPerRun[3]));

		Long endTime = System.currentTimeMillis();
		System.out.println("\nFinished after " + ((double)(endTime-startTime))/1000 + "s");
	}

	public static void parseArguments(String[] args){
		int i = 0;
		try {
			while (i < args.length){
				switch (args[i]) {
					case "-config":
						i++;
						Utils.readConfigFile(args[i]);
						break;
					case "-aid":
						i++;
						APPID = args[i];
						break;
				}
				i++;
			}
		} catch(Exception e){
			System.out.println("ERROR: Failed parsing arguments.");
		}
	}
}

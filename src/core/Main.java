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
	public static int TOURNAMENT_SIZE = 100;
	public static boolean APPLY_DEPTH_LIMIT = true;
	public static int MAXIMUM_DEPTH = 17;
	public static int MAXIMUM_INITIAL_DEPTH = 6;
	public static double CROSSOVER_PROBABILITY = 0.9;
	public static boolean PRINT_AT_EACH_GENERATION = true;
	public static boolean SHUFFLE_VALIDATION_SPLIT = false;

	// repulsor parameters
	public static double VALIDATION_SET_SIZE = 0.2;
	public static int REPULSOR_MIN_AGE = 10;
	public static int SEMANTIC_REPULSOR_MAX_NUMBER = 50;
	public static int VALIDATION_ELITE_SIZE = 50;
	public static boolean USE_ONLY_BEST_AS_REP_CANDIDATE = true;
	public static boolean OVERFIT_BY_MEDIAN = true;

	public static void main(String[] args) {
		Long startTime = System.currentTimeMillis();

		System.out.println("\nStarting Setup Phase");

		// load configuration file
		parseArguments(args);

		// load training and unseen data
		Data data = Utils.loadData(DATA_FILENAME, VALIDATION_SET_SIZE, SHUFFLE_VALIDATION_SPLIT);

		System.out.println("Finished Setup Phase\n");
		System.out.println("Starting Evolution");

		// run GP for a given number of runs
		double[][] resultsPerRun = new double[6][NUMBER_OF_RUNS];
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			System.out.printf("\tRun %d\n", i + 1);
			GpRun gp = new GpRun(data);
			// GsgpRun gp = new GsgpRun(data);

			// set parameters
			gp.setPopulationSize(POPULATION_SIZE);
			gp.setTournamentSize(TOURNAMENT_SIZE);
			gp.setApplyDepthLimit(APPLY_DEPTH_LIMIT);
			gp.setMaximumDepth(MAXIMUM_DEPTH);
			gp.setMaximumInitialDepth(MAXIMUM_INITIAL_DEPTH);
			gp.setCrossoverProbability(CROSSOVER_PROBABILITY);
			gp.setPrintAtEachGeneration(PRINT_AT_EACH_GENERATION);
			gp.setRepulsorMinAge(REPULSOR_MIN_AGE);
			gp.setRepulsorMaxNumber(SEMANTIC_REPULSOR_MAX_NUMBER);
			gp.setValidationEliteSize(VALIDATION_ELITE_SIZE);
			gp.setUseOnlyBestAsRepCandidate(USE_ONLY_BEST_AS_REP_CANDIDATE);
			gp.setOverfitByMedian(OVERFIT_BY_MEDIAN);

			gp.initialize();

			gp.evolve(NUMBER_OF_GENERATIONS);
			Individual bestFound = gp.getCurrentBest();
			resultsPerRun[0][i] = bestFound.getTrainingError();
			resultsPerRun[1][i] = bestFound.getValidationError();
			resultsPerRun[2][i] = bestFound.getUnseenError();
			resultsPerRun[3][i] = bestFound.getSize();
			resultsPerRun[4][i] = bestFound.getDepth();
			resultsPerRun[5][i] = gp.getPopulation().getRepulsorsSize();
			System.out.print("\n\t\tBest =>");
			bestFound.print();
			System.out.println();
		}

		// present average results
		System.out.printf("\n\tAVERAGE results after "+NUMBER_OF_RUNS+" runs ("+NUMBER_OF_GENERATIONS+" Generations)\n");
		System.out.println("\t\tRuns\tTraining Error   \tValidation Error \tTest Error      \tSize\tDepth\t#Repulsors");
		System.out.println("\t\t"+NUMBER_OF_RUNS+"\t"+Utils.getAverage(resultsPerRun[0])+"\t"+Utils.getAverage(resultsPerRun[1])+
			"\t"+Utils.getAverage(resultsPerRun[2])+"\t"+Utils.getAverage(resultsPerRun[3])+
			"\t"+Utils.getAverage(resultsPerRun[4])+"\t"+Utils.getAverage(resultsPerRun[5]));

		System.out.println("Finished Evolution\n");

		Long endTime = System.currentTimeMillis();
		System.out.println("Finished after " + ((double)(endTime-startTime))/1000 + "s");
	}


	public static void parseArguments(String[] args){
		boolean isConfigured = false;
		int i = 0;
		try {
			while (i < args.length){
				switch (args[i]) {
					case "-config":
						i++;
						Utils.readConfigFile(args[i]);
						isConfigured = true;
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
		if (!isConfigured){
			System.out.println("WARNING: Running without the use of argument '-config <path to confguration file>', consider using 'java -jar GP.jar -config <path>' to gain controll over the parameters.\n");
			System.out.println("The following can be used (and is default) as a configuration(.ini) file:");
			System.out.println("number_of_generations=6\nnumber_of_runs=2\ndata_filename=dataset\npopulation_size=100\napply_depth_limit=1\nmaximum_depth=17\nmaximum_initial_depth=6\ncrossover_probability=0.9\nprint_at_each_generation=1\nshuffle_validation_split=1");
		}
	}
}

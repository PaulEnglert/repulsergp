package core;

import utils.Utils;

public class Main {

	// general parameters
	public static String APPID = "default";
	public static String OUTPUT_DIR = "./results";
	public static String CONFIG_FILE = "";
	public static boolean LOG_SEMANTICS = false;
	public static String DATA_TRAIN_FILENAME = "dataset";
	public static String DATA_TEST_FILENAME = "dataset";
	public static int NUMBER_OF_RUNS = 1;
	public static int NUMBER_OF_GENERATIONS = 5;

	// GP Parameters
	public static int POPULATION_SIZE = 100;
	public static int TOURNAMENT_SIZE = 4;
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
	public static boolean AGGREGATE_REPULSORS = true;
	public static boolean FORCE_AVOID_REPULSORS = false;
	public static double EQUALITY_DELTA = 0;

	public static void main(String[] args) {
		Long startTime = System.currentTimeMillis();

		Utils.initDecFormatter();

		// load configuration file
		parseArguments(args);

		// start logging to files
		Utils.attachLogger(""+startTime);

		Utils.log(Utils.LogTag.LOG, "Algorithm: Repulsor GP");
		Utils.log(Utils.LogTag.LOG, "Timestamp: " + startTime+"\n");
		Utils.log(Utils.LogTag.LOG, "Starting Setup Phase\n");

		if (!CONFIG_FILE.equals("")){
			Utils.readConfigFile(CONFIG_FILE);
			// TODO print out configuration here
			Utils.log(Utils.LogTag.LOG, "\nThe following Configuration is being used:");
			Utils.log(Utils.LogTag.LOG, "\tnumber_of_generations=" + NUMBER_OF_GENERATIONS);
			Utils.log(Utils.LogTag.LOG, "\tnumber_of_runs=" + NUMBER_OF_RUNS);
			Utils.log(Utils.LogTag.LOG, "\tpopulation_size=" + POPULATION_SIZE);
			Utils.log(Utils.LogTag.LOG, "\ttournament_size=" + TOURNAMENT_SIZE);
			Utils.log(Utils.LogTag.LOG, "\tapply_depth_limit=" + APPLY_DEPTH_LIMIT);
			Utils.log(Utils.LogTag.LOG, "\tmaximum_depth=" + MAXIMUM_DEPTH);
			Utils.log(Utils.LogTag.LOG, "\tmaximum_initial_depth=" + MAXIMUM_INITIAL_DEPTH);
			Utils.log(Utils.LogTag.LOG, "\tcrossover_probability=" + CROSSOVER_PROBABILITY);
			Utils.log(Utils.LogTag.LOG, "\tprint_at_each_generation=" + PRINT_AT_EACH_GENERATION);
			Utils.log(Utils.LogTag.LOG, "\tvalidation_set_size=" + VALIDATION_SET_SIZE);
			Utils.log(Utils.LogTag.LOG, "\trepulsor_min_age=" + REPULSOR_MIN_AGE);
			Utils.log(Utils.LogTag.LOG, "\tsemantic_repulsor_max_number=" + SEMANTIC_REPULSOR_MAX_NUMBER);
			Utils.log(Utils.LogTag.LOG, "\tvalidation_elite_size=" + VALIDATION_ELITE_SIZE);
			Utils.log(Utils.LogTag.LOG, "\tuse_only_best_as_rep_candidate=" + USE_ONLY_BEST_AS_REP_CANDIDATE);
			Utils.log(Utils.LogTag.LOG, "\toverfit_by_median=" + OVERFIT_BY_MEDIAN);
			Utils.log(Utils.LogTag.LOG, "\tshuffle_validation_split=" + SHUFFLE_VALIDATION_SPLIT);
			Utils.log(Utils.LogTag.LOG, "\tlog_semantics=" + LOG_SEMANTICS);
			Utils.log(Utils.LogTag.LOG, "\taggregate_repulsors=" + AGGREGATE_REPULSORS);
			Utils.log(Utils.LogTag.LOG, "\tforce_avoid_repulsors=" + FORCE_AVOID_REPULSORS);
			Utils.log(Utils.LogTag.LOG, "\tequality_delta=" + EQUALITY_DELTA);
		}
		else{
			System.out.println("WARNING: Running without the use of argument '-config <path to confguration file>', consider using 'java -jar GP.jar -config <path>' to gain controll over the parameters.\n");
			System.out.println("The following can be used (and is default) as a configuration(.ini) file:");
			System.out.println("number_of_generations=6\nnumber_of_runs=2\ndata_filename=dataset\npopulation_size=100\ntournament_size=4\napply_depth_limit=1\nmaximum_depth=17\nmaximum_initial_depth=6\ncrossover_probability=0.9\nprint_at_each_generation=1"+
				"validation_set_size=0.2\nrepulsor_min_age=10\nsemantic_repulsor_max_number=50\nvalidation_elite_size=10\nuse_only_best_as_rep_candidate\noverfit_by_median=1\nlog_semantics=0");
			Utils.log(Utils.LogTag.LOG, "Configuration:");
			Utils.log(Utils.LogTag.LOG, "number_of_generations=6\nnumber_of_runs=2\ndata_filename=dataset\npopulation_size=100\ntournament_size=4\napply_depth_limit=1\nmaximum_depth=17\nmaximum_initial_depth=6\ncrossover_probability=0.9\nprint_at_each_generation=1"+
				"validation_set_size=0.2\nrepulsor_min_age=10\nsemantic_repulsor_max_number=50\nvalidation_elite_size=10\nuse_only_best_as_rep_candidate\noverfit_by_median=1\nlog_semantics=0");
		}

		if (LOG_SEMANTICS)
			Utils.attachLogger(""+startTime, Utils.LogTag.SEMANTICS);

		// load training and unseen data
		Data data = Utils.loadData(DATA_TRAIN_FILENAME, DATA_TEST_FILENAME, VALIDATION_SET_SIZE, SHUFFLE_VALIDATION_SPLIT);

		Utils.log(Utils.LogTag.LOG, "Finished Setup Phase\n");
		Utils.log(Utils.LogTag.LOG, "Starting Evolution\n");

		// run GP for a given number of runs
		double[][] resultsPerRun = new double[6][NUMBER_OF_RUNS];
		for (int i = 0; i < NUMBER_OF_RUNS; i++) {
			Utils.log(Utils.LogTag.LOG, "\n\t\t##### Run "+(i+1)+" #####\n");
			System.out.println("Run "+(i+1));

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
			gp.setLogSemantics(LOG_SEMANTICS);
			gp.setRepulsorMinAge(REPULSOR_MIN_AGE);
			gp.setRepulsorMaxNumber(SEMANTIC_REPULSOR_MAX_NUMBER);
			gp.setValidationEliteSize(VALIDATION_ELITE_SIZE);
			gp.setUseOnlyBestAsRepCandidate(USE_ONLY_BEST_AS_REP_CANDIDATE);
			gp.setOverfitByMedian(OVERFIT_BY_MEDIAN);
			gp.setAggregateRepulsors(AGGREGATE_REPULSORS);
			gp.setForceAvoidRepulsors(FORCE_AVOID_REPULSORS);
			gp.setEqualityDelta(EQUALITY_DELTA);

			gp.initialize();

			gp.evolve(NUMBER_OF_GENERATIONS);
			Individual bestFound = gp.getCurrentBest();
			resultsPerRun[0][i] = bestFound.getTrainingError();
			resultsPerRun[1][i] = bestFound.getValidationError();
			resultsPerRun[2][i] = bestFound.getUnseenError();
			resultsPerRun[3][i] = bestFound.getSize();
			resultsPerRun[4][i] = bestFound.getDepth();
			resultsPerRun[5][i] = gp.getPopulation().getRepulsorsSize();
			Utils.log(Utils.LogTag.LOG, "\nBest =>"+bestFound.print());
		}

		// present average results
		Utils.log(Utils.LogTag.LOG, "\n\t\t##### AVERAGE results after "+NUMBER_OF_RUNS+" runs ("+NUMBER_OF_GENERATIONS+" Generations) #####\n\n");
		Utils.log(Utils.LogTag.LOG, "\t\tRuns\tTraining Error   \tValidation Error \tTest Error      \tSize\tDepth\t#Repulsors");
		String dataline = "\t\t"+NUMBER_OF_RUNS;
		for (int i = 0; i < 6; i++){
			dataline += "\t"+Utils.format(Utils.getAverage(resultsPerRun[i]));
		}
		Utils.log(Utils.LogTag.LOG, dataline);

		Utils.log(Utils.LogTag.LOG, "Finished Evolution\n");

		Long endTime = System.currentTimeMillis();
		Utils.log(Utils.LogTag.LOG, "Finished after " + ((double)(endTime-startTime))/1000 + "s");

		Utils.detachLogger();
	}


	public static void parseArguments(String[] args){
		int i = 0;
		try {
			while (i < args.length){
				switch (args[i]) {
					case "-config":
						i++;
						CONFIG_FILE = args[i];
						break;
					case "-d":
					case "--directory":
						i++;
						OUTPUT_DIR = args[i];
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

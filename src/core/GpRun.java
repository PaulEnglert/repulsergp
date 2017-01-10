package core;

import utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

import programElements.Addition;
import programElements.Constant;
import programElements.InputVariable;
import programElements.Multiplication;
import programElements.Operator;
import programElements.ProgramElement;
import programElements.ProtectedDivision;
import programElements.Subtraction;

public class GpRun implements Serializable {

	private static final long serialVersionUID = 7L;

	// ##### parameters #####
	protected Data data;
	protected int id;
	protected ArrayList<ProgramElement> functionSet, terminalSet, fullSet;
	protected int populationSize = 100;
	protected int tournamentSize = 4;
	protected boolean applyDepthLimit = true;
	protected int maximumDepth = 17;
	protected int maximumInitialDepth = 6;
	protected double crossoverProbability = 0.9;
	protected boolean printAtEachGeneration = true;
	protected boolean logSemantics = false;
	protected int validationEliteSize = 10;
	protected int repulsorMinAge = 10;
	protected int repulsorMaxNumber = 25;
	protected int useBestAsRepCandidate = 1;
	protected boolean overfitByMedian = true;
	protected boolean aggregateRepulsors = true;
	protected boolean forceAvoidRepulsors = false;
	protected double equalityDelta = 0;
	protected boolean trueParetoSelection = false;
	protected boolean dominationExcludeFitness = false;
	protected boolean mergeRepulsors = false;
	protected int validationWorstSize = 10;
	protected boolean useValidationWorst = false;
	protected boolean useSelectiveValidationElite = false;
	protected int useOverfitByNSightedSteepness = 0;
	protected boolean repulseWithValidationOnly = false;

	// ##### state #####
	protected Random randomGenerator;
	protected int currentGeneration;
	protected Population population;
	protected Population validationPool;
	protected Individual currentBest;
	protected double currentSteepness;

	public GpRun(Data data) {
		this.data = data;
		this.id = 0;
	}

	public GpRun(Data data, int id) {
		this.data = data;
		this.id = id;
	}

	public void initialize() {
		Utils.log(Utils.LogTag.LOG, "Starting Initialization Phase");

		// adds all the functions to the function set
		functionSet = new ArrayList<ProgramElement>();
		functionSet.add(new Addition());
		functionSet.add(new Subtraction());
		functionSet.add(new Multiplication());
		functionSet.add(new ProtectedDivision());

		// adds all the constants to the terminal set
		terminalSet = new ArrayList<ProgramElement>();
		double[] constants = { -1.0, -0.75, -0.5, -0.25, 0.0, 0.25, 0.5, 0.75, 1.0 };
		for (int i = 0; i < constants.length; i++) {
			terminalSet.add(new Constant(constants[i]));
		}

		// adds all the input variables to the terminal set
		for (int i = 0; i < data.getDimensionality(); i++) {
			terminalSet.add(new InputVariable(i));
		}

		// creates the set which contains all the program elements
		fullSet = new ArrayList<ProgramElement>();
		for (ProgramElement programElement : functionSet) {
			fullSet.add(programElement);
		}
		for (ProgramElement programElement : terminalSet) {
			fullSet.add(programElement);
		}

		randomGenerator = new Random();
		currentGeneration = 0;
		currentSteepness = 0;

		validationPool = new Population(this.trueParetoSelection, this.dominationExcludeFitness, this.mergeRepulsors);

		// initialize and evaluate population
		rampedHalfAndHalfInitialization();
		for (int i = 0; i < populationSize; i++) {
			population.getIndividual(i).evaluate(data);
			if (!this.useSelectiveValidationElite)
				tryAddToValidationPool(population.getIndividual(i));
		}
		// if selective validation elite is used, add the best n individuals to validation elite
		if (this.useSelectiveValidationElite){
			int[] bestOf = population.getBestIndex(this.validationEliteSize);
			for (int i = 0; i < bestOf.length; i++){
				tryAddToValidationPool(population.getIndividual(bestOf[i]));
			}
		}

		updateCurrentBest();
		if ( this.forceAvoidRepulsors )
			population.calculateMaxDistance();

		if (id == 0){
			Utils.log(Utils.LogTag.FITNESSTEST, "RID;Gen;Id;Test Error;Size;Depth;TraditionalBest Id;TraditionalBest Error;TraditionalBest Size;TraditionalBest Depth");
			Utils.log(Utils.LogTag.FITNESSTRAIN, "RID;Gen;Id;Training Error;Size;Depth;pr;#rep;TraditionalBest Id;TraditionalBest Error;TraditionalBest Size;TraditionalBest Depth");
			Utils.log(Utils.LogTag.FITNESSVALIDATION, "RID;Gen;Id;Validation Error;Size;Depth;TraditionalBest Id;TraditionalBest Error;TraditionalBest Size;TraditionalBest Depth");
			Utils.log(Utils.LogTag.REPULSERDISTANCES, "RID;Gen;Id;Distance to best individual");
			if (logSemantics){
				Utils.log(Utils.LogTag.SEMANTICS, "RID;Gen;id;isRep;Semantics on training data");
			}
			Utils.log(Utils.LogTag.SPECIFICSEMANTICS, "RID;Gen;id;isOverfitting;NumTrainInstances;NumValInstances;Semantics (training & validation)");
		}

		printState();

		currentGeneration++;

		Utils.log(Utils.LogTag.LOG, "Finished Initialization Phase");
	}

	protected void rampedHalfAndHalfInitialization() {
		/*
		 * depth at the root node is 0. this implies that the number of
		 * different depths is equal to the maximumInitialDepth
		 */
		int individualsPerDepth = populationSize / maximumInitialDepth;
		int remainingIndividuals = populationSize % maximumInitialDepth;
		population = new Population(this.trueParetoSelection, this.dominationExcludeFitness, this.mergeRepulsors);
		int fullIndividuals, growIndividuals;

		for (int depth = 1; depth <= maximumInitialDepth; depth++) {
			if (depth == maximumInitialDepth) {
				fullIndividuals = (int) Math.floor((individualsPerDepth + remainingIndividuals) / 2.0);
				growIndividuals = (int) Math.ceil((individualsPerDepth + remainingIndividuals) / 2.0);
			} else {
				fullIndividuals = (int) Math.floor(individualsPerDepth / 2.0);
				growIndividuals = (int) Math.ceil(individualsPerDepth / 2.0);
			}

			for (int i = 0; i < fullIndividuals; i++) {
				population.addIndividual(full(depth));
			}
			for (int i = 0; i < growIndividuals; i++) {
				population.addIndividual(grow(depth));
			}
		}
	}

	protected Individual full(int maximumTreeDepth) {
		Individual individual = new Individual();
		fullInner(individual, 0, maximumTreeDepth);
		individual.setDepth(maximumTreeDepth);
		return individual;
	}

	protected void fullInner(Individual individual, int currentDepth, int maximumTreeDepth) {
		if (currentDepth == maximumTreeDepth) {
			ProgramElement randomTerminal = terminalSet.get(randomGenerator.nextInt(terminalSet.size()));
			individual.addProgramElement(randomTerminal);
		} else {
			Operator randomOperator = (Operator) functionSet.get(randomGenerator.nextInt(functionSet.size()));
			individual.addProgramElement(randomOperator);
			for (int i = 0; i < randomOperator.getArity(); i++) {
				fullInner(individual, currentDepth + 1, maximumTreeDepth);
			}
		}
	}

	protected Individual grow(int maximumTreeDepth) {
		Individual individual = new Individual();
		growInner(individual, 0, maximumTreeDepth);
		individual.calculateDepth();
		return individual;
	}

	protected void growInner(Individual individual, int currentDepth, int maximumTreeDepth) {
		if (currentDepth == maximumTreeDepth) {
			ProgramElement randomTerminal = terminalSet.get(randomGenerator.nextInt(terminalSet.size()));
			individual.addProgramElement(randomTerminal);
		} else {
			// equal probability of adding a terminal or an operator
			if (randomGenerator.nextBoolean()) {
				Operator randomOperator = (Operator) functionSet.get(randomGenerator.nextInt(functionSet.size()));
				individual.addProgramElement(randomOperator);
				for (int i = 0; i < randomOperator.getArity(); i++) {
					growInner(individual, currentDepth + 1, maximumTreeDepth);
				}
			} else {
				ProgramElement randomTerminal = terminalSet.get(randomGenerator.nextInt(terminalSet.size()));
				individual.addProgramElement(randomTerminal);
			}
		}
	}

	public void evolve(int numberOfGenerations) {
		Utils.log(Utils.LogTag.LOG, "Starting Evolution (" + numberOfGenerations +" Generations)");
		// evolve for a given number of generations
		while (currentGeneration <= numberOfGenerations) {
			System.out.println("Generation " + currentGeneration);
			Utils.log(Utils.LogTag.LOG, "Generation " + currentGeneration);
			Population offspring = new Population(this.trueParetoSelection, this.dominationExcludeFitness, this.mergeRepulsors);
			offspring.addIndividual(population.getBest());
			if (currentGeneration >= repulsorMinAge)
				offspring.repulsors = population.repulsors;
			offspring.setFitnessMemory(population.getFitnessMemory());

			int newReps = 0;
			// generate a new offspring population
			int recreatedCount = 0;
			while (offspring.getSize() < population.getSize()) {
				Individual p1, newIndividual;
				boolean first_try=true;
				do{
					p1 = selectParent(currentGeneration);
					// apply crossover
					if (randomGenerator.nextDouble() < crossoverProbability) {
						Individual p2 = selectParent(currentGeneration);
						newIndividual = applyStandardCrossover(p1, p2);
					}
					// apply mutation
					else {
						newIndividual = applyStandardMutation(p1);
					}

					/*
					 * add the new individual to the offspring population if its
					 * depth is not higher than the maximum (applicable only if the
					 * depth limit is enabled)
					 */
					if (applyDepthLimit && newIndividual.getDepth() > maximumDepth) {
						newIndividual = p1;
					} else {
						newIndividual.evaluate(data);
					}
					if (!first_try){
						recreatedCount++;
					}
					first_try=false;
				} while (recreatedCount < 50000 && this.forceAvoidRepulsors && this.isEqualToAnyRepulsor(newIndividual));

				offspring.addIndividual(newIndividual);
				if (!this.useSelectiveValidationElite)
					tryAddToValidationPool(newIndividual);

				newIndividual.setIsOverfitting(isOverfitting(newIndividual));
				newIndividual.setOverfitSeverity(getOverfittingSeverity(newIndividual));
				if ((currentGeneration >= repulsorMinAge) && useBestAsRepCandidate < 1 && newIndividual.getIsOverfitting()){
					offspring.addRepulsor(newIndividual, repulsorMaxNumber, false, equalityDelta);//, useValidationWorst);
				}
			}


			population = offspring;
			if (this.useSelectiveValidationElite)
				tryAddToValidationPool(population.getBest());
			population.nsgaIISort(this.aggregateRepulsors); // calculate ranks of new population
			// Utils.log(Utils.LogTag.LOG, "Current sorting:");
			// for (Individual ind : population.individuals){
			// 	boolean isRep = false;
			// 	for (Individual rep : population.repulsors){
			// 		if (0 ==ind.calculateTrainingSemanticDistance(rep.getTrainingDataOutputs()))
			// 			isRep = true;
			// 	}
			// 	Utils.log(Utils.LogTag.LOG, ""+ind.getId()+";"+ind.getTrainingError()+";"+ind.getRank()+";"+isRep);
			// }

			// calculate overfitting of each individual
			for (Individual ind : population.individuals){
				ind.setIsOverfitting(isOverfitting(ind));
				ind.setOverfitSeverity(getOverfittingSeverity(ind));
			}

			updateCurrentBest(); //use least overfitting/best not overfitting

			if ( this.forceAvoidRepulsors )
				population.calculateMaxDistance();

			// handle new repulser candidates
			Individual candidate = population.getBest();
			if ((currentGeneration >= repulsorMinAge) && useBestAsRepCandidate == 1 && candidate.getIsOverfitting()){
				if (population.addRepulsor(candidate, repulsorMaxNumber, false, equalityDelta)){//, useValidationWorst)){
					Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": Added 1 new repulsor (best was found to overfit) (Total: "+population.repulsors.size()+")");
				} else {
					Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": 1 new candidate repulser was discarded"+"(Total: "+population.repulsors.size()+")");
				}
			} else if ((currentGeneration >= repulsorMinAge) && useBestAsRepCandidate > 1){
				int[] totest = population.getBestIndex(useBestAsRepCandidate);
				Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": Testing " + totest.length + " individuals for overfitting");
				int used = 0;
				for (int i = 0; i < totest.length; i++){
					Individual ind = population.getIndividual(totest[i]);
					ind.setIsOverfitting(isOverfitting(ind));
					if (ind.getIsOverfitting()){
						ind.setOverfitSeverity(getOverfittingSeverity(ind));
						if (population.addRepulsor(ind, repulsorMaxNumber, false, equalityDelta))//, useValidationWorst))
							used++;
					}
				}
				Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": Added " + used + " new repulsor (Total: "+population.repulsors.size()+")");
			}
			if (this.forceAvoidRepulsors && population.getRepulsorsSize() > 0)
				Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+":Individuals recreated due to equality to any repulser during variation phase: " + recreatedCount);

			Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": Validation Pool Average Fitness (validation) = "+validationPool.getAverageFitness("validation"));
			// finish generation
			updateSteepness(currentBest);
			printState();
			currentGeneration++;
		}

		Utils.log(Utils.LogTag.LOG, "Finished Evolution");
	}

	protected void updateSteepness(Individual toadd){
		population.addToMemory(toadd);
		if (currentGeneration > useOverfitByNSightedSteepness){
			double[] trainfitnesses = new double[this.useOverfitByNSightedSteepness];
			for (int i = 0; i < this.useOverfitByNSightedSteepness; i++){
				trainfitnesses[i] = population.getFitnessMemory().get(i)[0];
			}
			currentSteepness = Utils.calculateSteepness(trainfitnesses);
		}
	}
	protected void printState() {
		Individual blindBest = population.getBest();
		if (printAtEachGeneration) {
			// test/unseen data
			Utils.log(Utils.LogTag.FITNESSTEST, ""+id+";"+currentGeneration+";"+currentBest.getId()+";"+Utils.format(currentBest.getUnseenError())
				+";"+Utils.format(currentBest.getSize())+";"+Utils.format(currentBest.getDepth())
				+";"+blindBest.getId()+";"+Utils.format(blindBest.getUnseenError())+";"+Utils.format(blindBest.getSize())+";"+Utils.format(blindBest.getDepth()));
			// validation data
			Utils.log(Utils.LogTag.FITNESSVALIDATION, ""+id+";"+currentGeneration+";"+currentBest.getId()+";"+Utils.format(currentBest.getValidationError())
				+";"+Utils.format(currentBest.getSize())+";"+Utils.format(currentBest.getDepth())
				+";"+blindBest.getId()+";"+Utils.format(blindBest.getValidationError())+";"+Utils.format(blindBest.getSize())+";"+Utils.format(blindBest.getDepth()));
			// training data
			Utils.log(Utils.LogTag.FITNESSTRAIN, ""+id+";"+currentGeneration+";"+currentBest.getId()+";"+Utils.format(currentBest.getTrainingError())+";"+Utils.format(currentBest.getSize())+";"+Utils.format(currentBest.getDepth())
					+";"+Utils.format(currentBest.getRank()) + ";"+Utils.format(population.getRepulsorsSize())
					+";"+blindBest.getId()+";"+Utils.format(blindBest.getTrainingError())+";"+Utils.format(blindBest.getSize())+";"+Utils.format(blindBest.getDepth())
					+";"+Utils.format(blindBest.getRank()));
			// log repulser distances
			for (int r = 0; r < population.getRepulsorsSize(); r++){
				Utils.log(Utils.LogTag.REPULSERDISTANCES, ""+id+";"+currentGeneration+";"+population.getRepulsor(r).getId()+";"+Utils.format(currentBest.calculateTrainingSemanticDistance(population.getRepulsorSemantics(r))));
			}
		}
		if (logSemantics && (currentGeneration)%50==0){
			for (int i = 0; i < population.getSize(); i++){
				Individual ind = population.getIndividual(i);
				String out = ""+Utils.format(ind.getTrainingDataOutputs()[0]);
				for (int s = 1; s < ind.getTrainingDataOutputs().length; s++){
					out += ";" + Utils.format(ind.getTrainingDataOutputs()[s]);
				}
				Utils.log(Utils.LogTag.SEMANTICS, ""+id+";"+currentGeneration+";"+ind.getId()+";0;"+out);
			}
			for (int i = 0; i < population.getRepulsorsSize(); i++){
				double[] sems = population.getRepulsorSemantics(i);
				String out = ""+Utils.format(sems[0]);
				for (int s = 1; s < sems.length; s++){
					out += ";" + Utils.format(sems[s]);
				}
				Utils.log(Utils.LogTag.SEMANTICS, ""+id+";"+currentGeneration+";"+population.getRepulsor(i).getId()+";1;"+out);
			}
		}
		// if traditional best is overfitting: log traditional best and least overfitting semantics 
		if (blindBest.getIsOverfitting()){
			int lt = data.getTrainingData().length;
			int lv = data.getValidationData().length;
			// blind/Traditional Best
			String out =  ""+id+";"+currentGeneration+";"+blindBest.getIsOverfitting()+";"+lt+";"+lv+";";
			for (int s = 0; s < blindBest.getTrainingDataOutputs().length; s++){
				out += "" + Utils.format(blindBest.getTrainingDataOutputs()[s])+";";
			}
			for (int s = 0; s < blindBest.getValidationDataOutputs().length; s++){
				if (s != blindBest.getValidationDataOutputs().length-1)
					out += "" + Utils.format(blindBest.getValidationDataOutputs()[s])+";";
				else
					out += "" + Utils.format(blindBest.getValidationDataOutputs()[s]);
			}
			//Current Best => least or not overfitting
			Utils.log(Utils.LogTag.SPECIFICSEMANTICS, out);
			out =  ""+id+";"+currentGeneration+";"+currentBest.getIsOverfitting()+";"+lt+";"+lv+";";
			for (int s = 0; s < currentBest.getTrainingDataOutputs().length; s++){
				out += "" + Utils.format(currentBest.getTrainingDataOutputs()[s])+";";
			}
			for (int s = 0; s < currentBest.getValidationDataOutputs().length; s++){
				if (s != currentBest.getValidationDataOutputs().length-1)
					out += "" + Utils.format(currentBest.getValidationDataOutputs()[s])+";";
				else
					out += "" + Utils.format(currentBest.getValidationDataOutputs()[s]);
			}
			Utils.log(Utils.LogTag.SPECIFICSEMANTICS, out);
		}
	}

	// tournament selection
	protected Individual selectParent(int generation) {
		Population tournamentPopulation = new Population(this.trueParetoSelection, this.dominationExcludeFitness, this.mergeRepulsors);
		// int tournamentSize = (int) (0.05 * population.getSize());
		if (tournamentSize == 0) tournamentSize = 1;
		for (int i = 0; i < tournamentSize; i++) {
			int index = randomGenerator.nextInt(population.getSize());
			tournamentPopulation.addIndividual(population.getIndividual(index));
		}
		if (repulsorMinAge < generation-1 && population.getRepulsorsSize() > 0) {
			return tournamentPopulation.getNonDominatedBest();
		} else {
			return tournamentPopulation.getBest();
		}
	}

	protected Individual applyStandardCrossover(Individual p1, Individual p2) {

		int p1CrossoverStart = randomGenerator.nextInt(p1.getSize());
		int p1ElementsToEnd = p1.countElementsToEnd(p1CrossoverStart);
		int p2CrossoverStart = randomGenerator.nextInt(p2.getSize());
		int p2ElementsToEnd = p2.countElementsToEnd(p2CrossoverStart);

		Individual offspring = p1.selectiveDeepCopy(p1CrossoverStart, p1CrossoverStart + p1ElementsToEnd - 1);

		// add the selected tree from the second parent to the offspring
		for (int i = 0; i < p2ElementsToEnd; i++) {
			offspring.addProgramElementAtIndex(p2.getProgramElementAtIndex(p2CrossoverStart + i), p1CrossoverStart + i);
		}

		offspring.calculateDepth();
		return offspring;
	}

	protected Individual applyStandardMutation(Individual p) {

		int mutationPoint = randomGenerator.nextInt(p.getSize());
		int parentElementsToEnd = p.countElementsToEnd(mutationPoint);
		Individual offspring = p.selectiveDeepCopy(mutationPoint, mutationPoint + parentElementsToEnd - 1);
		int maximumDepth = 6;
		Individual randomTree = grow(maximumDepth);

		// add the random tree to the offspring
		for (int i = 0; i < randomTree.getSize(); i++) {
			offspring.addProgramElementAtIndex(randomTree.getProgramElementAtIndex(i), mutationPoint + i);
		}

		offspring.calculateDepth();
		return offspring;
	}

	protected void updateCurrentBest() {
		//currentBest = population.getBest();
		currentBest = population.getBestNotOverfitting();
	}

	protected void tryAddToValidationPool(Individual individual){
		if (!useValidationWorst){
			if (validationPool.getSize() < validationEliteSize){
				validationPool.addIndividual(individual);
			} else {
				// replace worst individual in the pool
				int worstIdx = validationPool.getWorstIndex("validation");
				if (individual.getValidationError() < validationPool.getIndividual(worstIdx).getValidationError()){
					validationPool.removeIndividual(worstIdx);
					validationPool.addIndividual(individual);
				}
			}
		} else {
			if (validationPool.getSize() < validationWorstSize){
				validationPool.addIndividual(individual);
			} else {
				// replace best individual in the pool
				int bestIdx = validationPool.getBestIndex("validation");
				if (individual.getValidationError() > validationPool.getIndividual(bestIdx).getValidationError()){
					validationPool.removeIndividual(bestIdx);
					validationPool.addIndividual(individual);
				}
			}
		}
	}

	protected boolean isOverfitting(Individual individual){
		if (!useValidationWorst){
			// check against median/average of best on validation set 
			if (validationPool.getSize() < validationEliteSize)
				return false;
			else if (overfitByMedian)
				return individual.getValidationError() > validationPool.getMedianFitness("validation");
			else
				return individual.getValidationError() > validationPool.getAverageFitness("validation");
		} else if (useOverfitByNSightedSteepness == 0){
			// check if equal to median/average of validation worst (equal is within 1.5 standard deviations)
			double threshold = 0;
			double std = validationPool.getStandardDeviation("validation");
			if (validationPool.getSize() < validationWorstSize)
				return false;
			else if (overfitByMedian)
				threshold = validationPool.getMedianFitness("validation");
			else
				threshold = validationPool.getAverageFitness("validation");
				//return individual.getValidationError() > validationPool.getAverageFitness("validation");
			
			return (Math.abs(threshold - individual.getValidationError()) < 1.5*std);
		} else if (useOverfitByNSightedSteepness > 0){
			if (currentGeneration <= useOverfitByNSightedSteepness) // escape if not enough individuals have been collected yet
				return false;
			// overfit by steepness:
			// if steepness is changed & individuals validation fitness is worse than median/average
			// of previous n generations, it is overfitting, otherwhise no
			// get fitnesses except for the oldest
			double[] trainfitnesses = new double[this.useOverfitByNSightedSteepness];
			double[] valfitnesses = new double[this.useOverfitByNSightedSteepness];
			for (int i = 1; i < this.useOverfitByNSightedSteepness; i++){
				trainfitnesses[i] = population.getFitnessMemory().get(i)[0];
				valfitnesses[i-1] = population.getFitnessMemory().get(i-1)[1];
			}
			valfitnesses[this.useOverfitByNSightedSteepness-1] = population.getFitnessMemory().get(this.useOverfitByNSightedSteepness-1)[1];
			// add to test individual at the newest position
			trainfitnesses[this.useOverfitByNSightedSteepness-1] = individual.getTrainingError();
			double steepness = Utils.calculateSteepness(trainfitnesses);
			return (steepness-currentSteepness > 0.1 && individual.getValidationError() > Utils.getAverage(valfitnesses));
		}
		return false;
	}

	protected boolean isEqualToAnyRepulsor(Individual ind){
		// compute distance to all repulsers, if one of them is smaller then equalit delta, return false
		for (int i = 0; i < population.getRepulsorsSize(); i++){
			Individual repulsor = population.getRepulsor(i);
			double d = ind.calculateCombinedSemanticDistance(repulsor.getTrainingDataOutputs(), repulsor.getValidationDataOutputs());
			if (d < population.getCombinedMaximumDistance()*this.equalityDelta){
				return true;
			}
		}
		return false;
	}

	protected double getOverfittingSeverity(Individual individual){
		if (!useValidationWorst){
			double valFit=0;
			if (overfitByMedian)
				valFit = validationPool.getMedianFitness("validation");
			else
				valFit = validationPool.getAverageFitness("validation");
			return valFit-individual.getValidationError();
		} else if (useOverfitByNSightedSteepness == 0){
			double threshold = 0;
			double std = validationPool.getStandardDeviation("validation");
			if (overfitByMedian)
				threshold = validationPool.getMedianFitness("validation");
			else
				threshold = validationPool.getAverageFitness("validation");
				//return individual.getValidationError() > validationPool.getAverageFitness("validation");
			
			return (threshold - individual.getValidationError())/(1.5*std);
			// return -1 * individual.getValidationError();
		} else if (useOverfitByNSightedSteepness > 0){
			// difference in steepness
			double[] trainfitnesses = new double[this.useOverfitByNSightedSteepness];
			//double[] valfitnesses = double[this.useOverfitByNSightedSteepness];
			for (int i = 1; i < this.useOverfitByNSightedSteepness; i++){
				trainfitnesses[i] = population.getFitnessMemory().get(i)[0];
				//valfitnesses[i] = population.getFitnessMemory().get(i)[1];
			}
			// add to test individual at the newest position
			trainfitnesses[this.useOverfitByNSightedSteepness-1] = individual.getTrainingError();
			//valfitnesses[this.useOverfitByNSightedSteepness-1] = individual.getValidationError();
			double steepness = Utils.calculateSteepness(trainfitnesses);
			return (steepness-currentSteepness);
		}
		return 0;
	}

	// ##### get's and set's from here on #####

	public Individual getCurrentBest() {
		return currentBest;
	}

	public ArrayList<ProgramElement> getFunctionSet() {
		return functionSet;
	}

	public ArrayList<ProgramElement> getTerminalSet() {
		return terminalSet;
	}

	public ArrayList<ProgramElement> getFullSet() {
		return fullSet;
	}

	public boolean getApplyDepthLimit() {
		return applyDepthLimit;
	}

	public int getMaximumDepth() {
		return maximumDepth;
	}

	public int getMaximumInitialDepth() {
		return maximumInitialDepth;
	}

	public double getCrossoverProbability() {
		return crossoverProbability;
	}

	public int getCurrentGeneration() {
		return currentGeneration;
	}

	public Data getData() {
		return data;
	}

	public Population getPopulation() {
		return population;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	public Random getRandomGenerator() {
		return randomGenerator;
	}

	public boolean getPrintAtEachGeneration() {
		return printAtEachGeneration;
	}

	public void setFunctionSet(ArrayList<ProgramElement> functionSet) {
		this.functionSet = functionSet;
	}

	public void setTerminalSet(ArrayList<ProgramElement> terminalSet) {
		this.terminalSet = terminalSet;
	}

	public void setFullSet(ArrayList<ProgramElement> fullSet) {
		this.fullSet = fullSet;
	}

	public void setApplyDepthLimit(boolean applyDepthLimit) {
		this.applyDepthLimit = applyDepthLimit;
	}

	public void setMaximumDepth(int maximumDepth) {
		this.maximumDepth = maximumDepth;
	}

	public void setMaximumInitialDepth(int maximumInitialDepth) {
		this.maximumInitialDepth = maximumInitialDepth;
	}

	public void setCrossoverProbability(double crossoverProbability) {
		this.crossoverProbability = crossoverProbability;
	}

	public void setPrintAtEachGeneration(boolean printAtEachGeneration) {
		this.printAtEachGeneration = printAtEachGeneration;
	}

	public void setLogSemantics(boolean logSemantics) {
		this.logSemantics = logSemantics;
	}

	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}

	public void setTrueParetoSelection(boolean flag) {
		this.trueParetoSelection = flag;
	}

	public void setTournamentSize(int tournamentSize) {
		this.tournamentSize = tournamentSize;
	}

	public void setRepulsorMinAge(int age) {
		this.repulsorMinAge = age;
	}

	public void setRepulsorMaxNumber(int num) {
		this.repulsorMaxNumber = num;
	}

	public void setValidationEliteSize(int size) {
		this.validationEliteSize = size;
	}

	public void setUseBestAsRepCandidate(int count) {
		this.useBestAsRepCandidate = count;
	}

	public void setOverfitByMedian(boolean flag) {
		this.overfitByMedian = flag;
	}
	public void setAggregateRepulsors(boolean flag) {
		this.aggregateRepulsors = flag;
	}

	public void setForceAvoidRepulsors(boolean flag) {
		this.forceAvoidRepulsors = flag;
	}
	public void setEqualityDelta(double delta) {
		this.equalityDelta = delta;
	}

	public void setDominationExcludeFitness(boolean flag) {
		this.dominationExcludeFitness = flag;
	}
	public void setMergeRepulsors(boolean flag) {
		this.mergeRepulsors = flag;
	}

	public void setValidationWorstSize(int size){
		this.validationWorstSize = size;
	};
	public void setUseValidationWorst(boolean flag){
		this.useValidationWorst = flag;
	};
	public void setUseSelectiveValidationElite(boolean flag){
		this.useSelectiveValidationElite = flag;
	};
	public void setUseOverfitByNSightedSteepness(int n){
		this.useOverfitByNSightedSteepness = n;
		Population.setFitnessMemory(n);
	};
	public void setRepulseWithValidationOnly(boolean flag){
		this.repulseWithValidationOnly = flag;
		Population.setRepulseWithValidationOnly(flag);
	};
}

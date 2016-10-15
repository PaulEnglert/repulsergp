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
	protected boolean useOnlyBestAsRepCandidate = true;
	protected boolean overfitByMedian = true;

	// ##### state #####
	protected Random randomGenerator;
	protected int currentGeneration;
	protected Population population;
	protected Population validationElite;
	protected Individual currentBest;

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

		validationElite = new Population();

		// initialize and evaluate population
		rampedHalfAndHalfInitialization();
		for (int i = 0; i < populationSize; i++) {
			population.getIndividual(i).evaluate(data);
			tryAddToValidationElite(population.getIndividual(i));
		}

		updateCurrentBest();

		
		if (id == 0){
			Utils.log(Utils.LogTag.FITNESSTEST, "RID\tGen\tTest Error\tSize\tDepth");
			Utils.log(Utils.LogTag.FITNESSTRAIN, "RID\tGen\tTraining Error\tSize\tDepth\tpr\t#rep\tdistances to all repulsors");
			Utils.log(Utils.LogTag.FITNESSVALIDATION, "RID\tGen\nValidation Error\tSize\tDepth");
			if (logSemantics){
				Utils.log(Utils.LogTag.SEMANTICS, "RID\tGen\tidx\tisRep\tSemantics on training data");
			}
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
		population = new Population();
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
			Population offspring = new Population();
			offspring.addIndividual(population.getBest());
			if (currentGeneration >= repulsorMinAge)
				offspring.repulsors = population.repulsors;
				offspring.lostRepulsors = population.lostRepulsors;

			int newReps = 0;
			// generate a new offspring population
			while (offspring.getSize() < population.getSize()) {
				Individual p1, newIndividual;
				p1 = selectParent();
				// apply crossover
				if (randomGenerator.nextDouble() < crossoverProbability) {
					Individual p2 = selectParent();
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
				offspring.addIndividual(newIndividual);
				tryAddToValidationElite(newIndividual);
				if ((currentGeneration >= repulsorMinAge) && !useOnlyBestAsRepCandidate && isOverfitting(newIndividual)){
					if (offspring.addRepulsor(newIndividual.getTrainingDataOutputs())){
						newReps++;
						if (offspring.repulsors.size() > repulsorMaxNumber){
							offspring.lostRepulsors += offspring.repulsors.size()-repulsorMaxNumber;
							offspring.repulsors = new ArrayList<double[]>(offspring.repulsors.subList(offspring.repulsors.size()-repulsorMaxNumber, offspring.repulsors.size()));
						}
					}
				}
			}

			int prevRepCount = population.getRepulsorsSize();
			population = offspring;
			population.nsgaIISort(); // calculate ranks of new population
			updateCurrentBest();
			if ((currentGeneration >= repulsorMinAge) && useOnlyBestAsRepCandidate && isOverfitting(currentBest)){
				if (population.addRepulsor(currentBest.getTrainingDataOutputs())){
					newReps++;
					if (population.repulsors.size() > repulsorMaxNumber){
						population.lostRepulsors += population.repulsors.size()-repulsorMaxNumber;
						population.repulsors = new ArrayList<double[]>(population.repulsors.subList(population.repulsors.size()-repulsorMaxNumber, population.repulsors.size()));
					}
				}
			}
			Utils.log(Utils.LogTag.LOG, "Gen "+currentGeneration+": Added "+newReps+" new repulsor");
			printState(prevRepCount);
			currentGeneration++;
		}

		Utils.log(Utils.LogTag.LOG, "Finished Evolution");
	}

	protected void printState() {
		printState(0);
	}
	protected void printState(int numReps) {
		if (printAtEachGeneration) {
			// test/unseen data 
			Utils.log(Utils.LogTag.FITNESSTEST, ""+id+"\t"+currentGeneration+"\t"+currentBest.getUnseenError()+"\t"+currentBest.getSize()+"\t"+currentBest.getDepth());
			// validation data
			Utils.log(Utils.LogTag.FITNESSVALIDATION, ""+id+"\t"+currentGeneration+"\t"+currentBest.getValidationError()+"\t"+currentBest.getSize()+"\t"+currentBest.getDepth());
			// training data
			String outTrain = ""+id+"\t"+currentGeneration+"\t"+currentBest.getTrainingError()+"\t"+currentBest.getSize()+"\t"+currentBest.getDepth()
								+"\t"+currentBest.getRank() + "\t"+numReps;
			for (int l = 0; l < population.lostRepulsors; l++){
				outTrain += "\tNA";
			}
			for (int r = 0; r < numReps; r++){
				outTrain += "\t"+currentBest.calculateTrainingSemanticDistance(population.getRepulsorSemantics(r));
			}
			Utils.log(Utils.LogTag.FITNESSTRAIN, outTrain);
		}
		if (logSemantics){
			for (int i = 0; i < population.getSize(); i++){
				Individual ind = population.getIndividual(i);
				String out = ""+ind.getTrainingDataOutputs()[0];
				for (int s = 1; s < ind.getTrainingDataOutputs().length; s++){
					out += "\t" + ind.getTrainingDataOutputs()[s];
				}
				Utils.log(Utils.LogTag.SEMANTICS, ""+id+"\t"+currentGeneration+"\t"+i+"\t0\t"+out);
			}
			for (int i = 0; i < numReps; i++){
				double[] sems = population.repulsors.get(i);
				String out = ""+sems[0];
				for (int s = 1; s < sems.length; s++){
					out += "\t" + sems[s];
				}
				Utils.log(Utils.LogTag.SEMANTICS, ""+id+"\t"+currentGeneration+"\t"+i+"\t1\t"+out);
			}
		}
	}

	// tournament selection
	protected Individual selectParent() {
		Population tournamentPopulation = new Population();
		// int tournamentSize = (int) (0.05 * population.getSize());
		if (tournamentSize == 0) tournamentSize = 1;
		for (int i = 0; i < tournamentSize; i++) {
			int index = randomGenerator.nextInt(population.getSize());
			tournamentPopulation.addIndividual(population.getIndividual(index));
		}
		return tournamentPopulation.getNonDominatedBest();
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
		currentBest = population.getBest();
	}

	protected void tryAddToValidationElite(Individual individual){
		if (validationElite.getSize() < validationEliteSize){
			validationElite.addIndividual(individual);
		} else {
			int worstIdx = validationElite.getWorstIndex("validation");
			if (individual.getValidationError() < validationElite.getIndividual(worstIdx).getValidationError()){
				validationElite.removeIndividual(worstIdx);
				validationElite.addIndividual(individual);
			}
		}
	}

	protected boolean isOverfitting(Individual individual){
		if (validationElite.getSize() < validationEliteSize)
			return false;
		else if (overfitByMedian)
			return individual.getValidationError() > validationElite.getMedianFitness("validation");
		else
			return individual.getValidationError() > validationElite.getAverageFitness("validation");

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
	
	public void setUseOnlyBestAsRepCandidate(boolean flag) {
		this.useOnlyBestAsRepCandidate = flag;
	}

	public void setOverfitByMedian(boolean flag) {
		this.overfitByMedian = flag;
	}
}

package core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class Population implements Serializable {

	private static final long serialVersionUID = 7L;

	protected ArrayList<Individual> individuals;

	protected ArrayList<double[]> repulsors;

	public Population() {
		individuals = new ArrayList<Individual>();
		repulsors = new ArrayList<double[]>();
	}

	public Individual getBest() {
		return individuals.get(getBestIndex());
	}

	public Individual getNonDominatedBest(){
		if (repulsors.size() == 0)
			return getBest();

		int bestIndex = 0;
		Individual best = individuals.get(bestIndex);
		for (int i = 1; i < individuals.size(); i++) {
			if (individuals.get(i).getRank() < best.getRank()) {
				best = individuals.get(i);
				bestIndex = i;
			} else if (individuals.get(i).getRank() == best.getRank() && individuals.get(i).getTrainingError() < best.getTrainingError()) {
				best = individuals.get(i);
				bestIndex = i;
			}
		}
		return best;
	}

	public int getBestIndex() {
		int bestIndex = 0;
		double bestTrainingError = individuals.get(bestIndex).getTrainingError();
		for (int i = 1; i < individuals.size(); i++) {
			if (individuals.get(i).getTrainingError() < bestTrainingError) {
				bestTrainingError = individuals.get(i).getTrainingError();
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	public Individual getWorst(String dataname) {
		return individuals.get(getWorstIndex(dataname));
	}

	public int getWorstIndex(String dataname) {
		int worstIndex = 0;
		double worstError = 0;
		int i = 0;
		do {
			double f = 0;
			if (dataname.equals("training")){
				f = individuals.get(i).getTrainingError();
			} else if (dataname.equals("validation")){
				f = individuals.get(i).getValidationError();
			} else if (dataname.equals("test")){
				f = individuals.get(i).getUnseenError();
			}
			if (i == 0 || f > worstError) {
				worstError = f;
				worstIndex = i;
			}
			i++;
		} while (i < individuals.size());
		return worstIndex;
	}

	public double getMedianFitness(String dataname){
		double[] fitness = new double[individuals.size()];
		for (int i = 0; i < individuals.size(); i++){
			if (dataname.equals("training")){
				fitness[i] = individuals.get(i).getTrainingError();
			} else if (dataname.equals("validation")){
				fitness[i] = individuals.get(i).getValidationError();
			} else if (dataname.equals("test")){
				fitness[i] = individuals.get(i).getUnseenError();
			}
		}
		Arrays.sort(fitness);
		if (fitness.length%2 > 0){
			return fitness[(fitness.length/2)];
		} else {
			return (fitness[((fitness.length/2))-1]+fitness[(fitness.length/2)])/2;
		}
	}

	public double getAverageFitness(String dataname){
		double avg = 0;
		for (int i = 0; i < individuals.size(); i++){
			double f = 0;
			if (dataname.equals("training")){
				avg += individuals.get(i).getTrainingError();
			} else if (dataname.equals("validation")){
				avg += individuals.get(i).getValidationError();
			} else if (dataname.equals("test")){
				avg += individuals.get(i).getUnseenError();
			}
		}
		return avg/individuals.size();
	}

	public void addIndividual(Individual individual) {
		individuals.add(individual);
	}

	public void removeIndividual(int index) {
		individuals.remove(index);
	}

	public int getSize() {
		return individuals.size();
	}

	public int getRepulsorsSize() {
		return repulsors.size();
	}

	public Individual getIndividual(int index) {
		return individuals.get(index);
	}

	public void nsgaIISort(){
		// check if there are repulsors to work with
		if (repulsors.size() == 0){
			// System.out.println("Skipping NSGA II Sort due to empty list of repulsors.");
			return;
		}

		// prepare structures
		ArrayList<Integer> dominationFront = new ArrayList<Integer>(); // containing the indexes of the current front
		int[] dominationCounts = new int[individuals.size()];	// for each individual the number of individuals that dominate it are saved here
		Object[] dominatedIndividuals = new Object[individuals.size()]; // for each individual all indexes of individuals that it dominates are saved here

		performFastNonDominationSort(dominationFront, dominationCounts, dominatedIndividuals);

		int front = 1;
		while (dominationFront.size() != 0){
			// System.out.println("Number of Individuals in front " + front + ": " + dominationFront.size());
			dominationFront = extractNextFront(front, dominationFront, dominationCounts, dominatedIndividuals);
			front++;
		}

	}

	public void performFastNonDominationSort(ArrayList<Integer> dominationFront, int[] dominationCounts, Object[] dominatedIndividuals){
		// for each individual find the dominated individuals and count the times itself has been dominated
		for(int i = 0; i < individuals.size(); i++){

			ArrayList<Integer> dInds = new ArrayList<Integer>();

			for(int j = 0; j < individuals.size(); j++){
				if (i == j) continue; // no need to compare to oneself
				// determine domination of i over j, or vice versa based on fitness and all repulsor distances
				boolean iDominatesJ = (individuals.get(i).getTrainingError() < individuals.get(j).getTrainingError());
				boolean jDominatesI = !iDominatesJ;
				for (int r = 0; r < repulsors.size(); r++){
					double d_i = individuals.get(i).calculateTrainingSemanticDistance(repulsors.get(r));
					double d_j = individuals.get(j).calculateTrainingSemanticDistance(repulsors.get(r));
					iDominatesJ = (iDominatesJ && d_i > d_j);
					jDominatesI = (jDominatesI && d_i < d_j);
				}
				if (iDominatesJ) // add j to set of dominated solutions of i
					dInds.add(j);
				else if (jDominatesI)    // increment count of times that i has been dominated
					dominationCounts[i]++;
			}
			dominatedIndividuals[i] = dInds;
			if (dominationCounts[i] == 0){
				dominationFront.add(i);
				// add rank to individual
				individuals.get(i).setRank(1);
			}
		}
	}

	public ArrayList<Integer> extractNextFront(int front, ArrayList<Integer> dominationFront, int[] dominationCounts, Object[] dominatedIndividuals){
		ArrayList<Integer> nextFront = new ArrayList<Integer>();
		for (int i = 0; i < dominationFront.size(); i++){
			// extract next front and update ranks
			@SuppressWarnings("unchecked")
			ArrayList<Integer> currentInds = (ArrayList<Integer>)dominatedIndividuals[dominationFront.get(i)];

			for (int d = 0; d < currentInds.size(); d++){
				int q = currentInds.get(d);
				if (!nextFront.contains(q)) { // check if the index has been added to the next front
					individuals.get(q).setRank(front+1);
					dominationCounts[q]--;
					if (dominationCounts[q] == 0)
						nextFront.add(q);
				}
			}
		}
		return nextFront;
	}
}

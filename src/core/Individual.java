package core;

import utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;

import programElements.Constant;
import programElements.InputVariable;
import programElements.Operator;
import programElements.ProgramElement;
import programElements.Terminal;

public class Individual implements Serializable {

	private static final long serialVersionUID = 7L;

	protected static long nextId;

	protected long id;
	protected ArrayList<ProgramElement> program;
	protected int depth;
	protected int nsga_II_rank;
	protected double trainingError, validationError, unseenError;
	protected double[] trainingDataOutputs, validationDataOutputs, unseenDataOutputs;

	// repulser data
	protected double overfitSeverity;

	protected int evaluateIndex;
	protected int maximumDepthAchieved;
	protected int depthCalculationIndex;
	protected int printIndex;

	protected boolean sizeOverride;
	protected int computedSize;

	public Individual() {
		program = new ArrayList<ProgramElement>();
		nsga_II_rank = 0;
		id = getNextId();
	}

	protected static long getNextId() {
		return nextId++;
	}

	public void evaluate(Data data) {
		evaluateOnTrainingData(data);
		evaluateOnValidationData(data);
		evaluateOnUnseenData(data);
	}

	public double[] evaluateOnTrainingData(Data data) {
		double[][] trainingData = data.getTrainingData();
		if (sizeOverride == false) {
			trainingDataOutputs = evaluate(trainingData);
		}
		trainingError = calculateRMSE(trainingData, trainingDataOutputs);
		return trainingDataOutputs;
	}

	public double[] evaluateOnValidationData(Data data) {
		double[][] validationData = data.getValidationData();
		if (validationData == null){
			validationError = Double.NaN;
			return null;
		}
		if (sizeOverride == false) {
			validationDataOutputs = evaluate(validationData);
		}
		validationError = calculateRMSE(validationData, validationDataOutputs);
		return validationDataOutputs;
	}

	public double[] evaluateOnUnseenData(Data data) {
		double[][] unseenData = data.getUnseenData();
		if (sizeOverride == false) {
			unseenDataOutputs = evaluate(unseenData);
		}
		unseenError = calculateRMSE(unseenData, unseenDataOutputs);
		return unseenDataOutputs;
	}

	public double[] evaluate(double[][] data) {
		double[] outputs = new double[data.length];
		for (int i = 0; i < outputs.length; i++) {
			evaluateIndex = 0;
			outputs[i] = evaluateInner(data[i]);
		}
		return outputs;
	}

	protected double evaluateInner(double[] dataInstance) {
		if (program.get(evaluateIndex) instanceof InputVariable) {
			InputVariable inputVariable = (InputVariable) program.get(evaluateIndex);
			return inputVariable.getValue(dataInstance);
		} else if (program.get(evaluateIndex) instanceof Constant) {
			Constant constant = (Constant) program.get(evaluateIndex);
			return constant.getValue();
		} else {
			Operator operator = (Operator) program.get(evaluateIndex);
			double[] arguments = new double[operator.getArity()];
			for (int i = 0; i < arguments.length; i++) {
				evaluateIndex++;
				arguments[i] = evaluateInner(dataInstance);
			}
			return operator.performOperation(arguments);
		}
	}

	protected double calculateRMSE(double[][] data, double[] outputs) {
		double errorSum = 0.0;
		for (int i = 0; i < data.length; i++) {
			double target = data[i][data[0].length - 1];
			errorSum += Math.pow(outputs[i] - target, 2.0);
		}
		return Math.sqrt(errorSum / data.length);
	}

	public Individual deepCopy() {
		Individual newIndividual = new Individual();
		for (int i = 0; i < program.size(); i++) {
			newIndividual.program.add(program.get(i));
		}
		newIndividual.setDepth(depth);
		return newIndividual;
	}

	// The resulting copy is: [0, exclusionZoneStart[ + ]exclusionZoneEnd, N-1]
	public Individual selectiveDeepCopy(int exclusionZoneStartIndex, int exclusionZoneEndIndex) {
		Individual newIndividual = new Individual();
		for (int i = 0; i < exclusionZoneStartIndex; i++) {
			newIndividual.program.add(program.get(i));
		}
		for (int i = exclusionZoneEndIndex + 1; i < program.size(); i++) {
			newIndividual.program.add(program.get(i));
		}
		return newIndividual;
	}

	public void calculateDepth() {
		maximumDepthAchieved = 0;
		depthCalculationIndex = 0;
		calculateDepth(0);
		depth = maximumDepthAchieved;
	}

	protected void calculateDepth(int currentDepth) {
		if (program.get(depthCalculationIndex) instanceof Operator) {
			Operator currentOperator = (Operator) program.get(depthCalculationIndex);
			for (int i = 0; i < currentOperator.getArity(); i++) {
				depthCalculationIndex++;
				calculateDepth(currentDepth + 1);
			}
		} else {
			if (currentDepth > maximumDepthAchieved) {
				maximumDepthAchieved = currentDepth;
			}
		}
	}

	public double calculateTrainingSemanticDistance(double[] dataPoint){
		double d = 0;
		for (int i = 0; i < this.trainingDataOutputs.length; i++){
			d += (dataPoint[i]-this.trainingDataOutputs[i])*(dataPoint[i]-this.trainingDataOutputs[i]);
		}
		return Math.sqrt(d / this.trainingDataOutputs.length);
	}

	public int countElementsToEnd(int startingIndex) {
		if (program.get(startingIndex) instanceof Terminal) {
			return 1;
		} else {
			Operator operator = (Operator) program.get(startingIndex);
			int numberOfElements = 1;
			for (int i = 0; i < operator.getArity(); i++) {
				numberOfElements += countElementsToEnd(startingIndex + numberOfElements);
			}
			return numberOfElements;
		}
	}

	public void addProgramElement(ProgramElement programElement) {
		program.add(programElement);
	}

	public void addProgramElementAtIndex(ProgramElement programElement, int index) {
		program.add(index, programElement);
	}

	public void removeProgramElementAtIndex(int index) {
		program.remove(index);
	}

	public ProgramElement getProgramElementAtIndex(int index) {
		return program.get(index);
	}

	public void setProgramElementAtIndex(ProgramElement programElement, int index) {
		program.set(index, programElement);
	}

	public String print() {
		String out = " ";
		if (sizeOverride == true) {
			out += "[Individual not constructed]";
		} else {
			printIndex = 0;
			out += printInner();
		}
		return out;
	}

	protected String printInner() {
		String out = "";
		if (program.get(printIndex) instanceof Terminal) {
			out += " " + program.get(printIndex);
		} else {
			out += " (";
			out += program.get(printIndex);
			Operator currentOperator = (Operator) program.get(printIndex);
			for (int i = 0; i < currentOperator.getArity(); i++) {
				printIndex++;
				out += printInner();
			}
			out += ")";
		}
		return out;
	}

	// ##### get's and set's from here on #####

	public double getTrainingError() {
		return trainingError;
	}

	public double getValidationError() {
		return validationError;
	}

	public double getUnseenError() {
		return unseenError;
	}

	public double[] getTrainingDataOutputs() {
		return trainingDataOutputs;
	}

	public double[] getValidationDataOutputs() {
		return validationDataOutputs;
	}

	public double[] getUnseenDataOutputs() {
		return unseenDataOutputs;
	}

	public long getId() {
		return id;
	}

	public int getSize() {
		if (sizeOverride) {
			return computedSize;
		} else {
			return program.size();
		}
	}

	public int getDepth() {
		return depth;
	}

	public int getRank() {
		return nsga_II_rank;
	}

	public double getOverfitSeverity() {
		return overfitSeverity;
	}

	public ArrayList<ProgramElement> getProgram() {
		return program;
	}

	public void setSizeOverride(boolean sizeOverride) {
		this.sizeOverride = sizeOverride;
	}

	public void setComputedSize(int computedSize) {
		this.computedSize = computedSize;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public void setRank(int rank) {
		this.nsga_II_rank = rank;
	}

	public void setOverfitSeverity(double overfitSeverity) {
		this.overfitSeverity = overfitSeverity;
	}

	public void setTrainingDataOutputs(double[] trainingDataOutputs) {
		this.trainingDataOutputs = trainingDataOutputs;
	}

	public void setValidationDataOutputs(double[] validationDataOutputs) {
		this.validationDataOutputs = validationDataOutputs;
	}

	public void setUnseenDataOutputs(double[] unseenDataOutputs) {
		this.unseenDataOutputs = unseenDataOutputs;
	}
}

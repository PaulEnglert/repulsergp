package programElements;

public class Sine extends Operator {

	private static final long serialVersionUID = 7L;

	public Sine() {
		super(1);
	}

	public double performOperation(double... arguments) {
		return Math.sin(arguments[0]);
	}

	public String toString() {
		return "sin(";
	}

}
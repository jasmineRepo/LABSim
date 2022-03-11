package labsim.model.decisions;


import labsim.model.Person;


/**
 *
 * CLASS TO FACILITATE EVALUATION OF PROBABILITIES AND VALUES FOR ISOLATED EVENTS
 *
 */
public class LocalExpectations {


    /**
     * ATTRIBUTES
     */
    double[] probabilities;         // array of probabilities
    double[] values;                // array of values


    /**
     * CONSTRUCTOR FOR DEFAULT INDICATOR VARIABLE (TRUE = 1.0)
     * @param person object to evaluate probability from probit regression
     * @param regression regression equation to evaluate
     */
    public LocalExpectations(Person person, ManagerRegressions.RegressionNames regression) {
        evaluateIndicator(person, 1.0, 0.0, regression);
    }
    public LocalExpectations(Person person, double value_true, double value_false, ManagerRegressions.RegressionNames regression) {
        evaluateIndicator(person, value_true, value_false, regression);
    }
    public LocalExpectations(double value_true, double value_false, double probability_true) {
        evaluateIndicator(value_true, value_false, probability_true);
    }


    /**
     * CONSTRUCTOR FOR GAUSSIAN DISTRIBUTION
     * @param expectation of Gaussian distribution
     * @param standard_deviation of Gaussian distribution
     */
    public LocalExpectations(double expectation, double standard_deviation, double min_value, double max_value) {
        probabilities = new double[Parameters.PTS_IN_QUADRATURE];
        values = new double[Parameters.PTS_IN_QUADRATURE];
        for (int ii=0; ii<Parameters.PTS_IN_QUADRATURE; ii++) {
            probabilities[ii] = Parameters.quadrature.weights[ii];
            double value = expectation + standard_deviation * Parameters.quadrature.abscissae[ii];
            value = Math.min(value, max_value);
            value = Math.max(value, min_value);
            values[ii] = value;
        }
    }


    /**
     * WORKER METHODS
     */
    private void evaluateIndicator(Person person, double value_true, double value_false, ManagerRegressions.RegressionNames regression) {
        values = new double[2];
        probabilities = new double[2];
        double prob = ManagerRegressions.getProbability(person, regression);
        values[0] = value_false;
        values[1] = value_true;
        probabilities[0] = (1 - prob);
        probabilities[1] = prob;
    }

    private void evaluateIndicator(double value_true, double value_false, double probability_true) {
        values = new double[2];
        probabilities = new double[2];
        values[0] = value_false;
        values[1] = value_true;
        probabilities[0] = (1 - probability_true);
        probabilities[1] = probability_true;
    }
}

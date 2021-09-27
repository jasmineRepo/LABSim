package labsim.model.decisions;


import java.security.InvalidParameterException;


/**
 *
 * CLASS TO ENCAPSULATE SOLUTION TO UTILITY OPTIMISATION
 *
 */
public class UtilityMaximisation {


    /**
     * ATTRIBUTES
     */
    double optimised_utility;   // numerical approximation for value function
    double[] controls;          // numerical approximation for control variables


    /**
     * CONSTRUCTOR
     */
    public UtilityMaximisation(Grid value_function, States states, Expectations expectations, double emp1_pr, double emp2_pr) {

        // update expectations for combination of discrete control variables
        expectations.updateForDiscreteControls(emp1_pr, emp2_pr);

        // initiate assumed utility function
        CESUtility function = new CESUtility(value_function, expectations);

        // initialise controls for optimisation problem
        int number_controls = 1;     // for consumption

        // initialise arrays for continuous controls
        double[] target = new double[number_controls];
        double[] lower_bounds = new double[number_controls];
        double[] upper_bounds = new double[number_controls];

        // add in discrete control variables
        if (states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            if (Parameters.FLAG_IO_EMPLOYMENT1) {
                number_controls++;          // for principal employment
            }
            if (states.getCohabitation()==1) {
                if (Parameters.FLAG_IO_EMPLOYMENT2) {
                    number_controls++;      // for secondary employment
                }
            }
        }
        controls = new double[number_controls];

        // populate control arrays
        int dim = 0;
        if (expectations.cash_on_hand <= Parameters.MIN_CONSUMPTION) {
            target[dim] = Parameters.MIN_CONSUMPTION;
        } else {
            lower_bounds[dim] = Parameters.MIN_CONSUMPTION;
            upper_bounds[dim] = expectations.cash_on_hand;
            target[dim] = lower_bounds[dim] * 0.8 +upper_bounds[dim] * 0.2;
        }
        dim++;


        // **********************************
        // pass for minimisation
        // **********************************
        Minimiser problem = new Minimiser(lower_bounds, upper_bounds, target, function);
        problem.minimise();


        // pack for delivery
        optimised_utility = - problem.minimised_value;

        // pack consumption solution
        dim = 0;
        controls[dim] = problem.target[dim] / expectations.cash_on_hand;
        dim++;

        if (dim != problem.target.length) {
            throw new InvalidParameterException("minimisation function has not delivered results for anticipated number of continuous controls");
        }

        // allow for employment controls
        if (states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            if (Parameters.FLAG_IO_EMPLOYMENT1) {
                controls[dim] = emp1_pr;
                dim++;
            }
            if (states.getCohabitation()==1) {
                if (Parameters.FLAG_IO_EMPLOYMENT2) {
                    controls[dim] = emp2_pr;
                    dim++;
                }
            }
        }
    }
}

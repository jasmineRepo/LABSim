package labsim.model.decisions;

import java.security.InvalidParameterException;


/**
 * CLASS TO MANAGE EVALUATION OF NESTED CES INTERTEMPORAL UTILITY FUNCTION
 */
public class CESUtility implements IEvaluation {


    /**
     * ATTRIBUTES
     */
    Expectations expectations;      // expectations initialised and updated to account for discrete control variables
    Grid value_function;            // storage of preceding solutions for value function

    // CES utility options
    public static double epsilon = 0.6;                                      // elasticity of substitution between equivalised consumption and leisure within each year
    public static double alpha_singles = 2.2 * 0.01756;                       // utility price of leisure for single adults
    public static double alpha_couples = 2.2 * 0.47 * 0.01756;                // utility price of leisure for couples
    public static double gamma = 1.55;                                       // (constant) coefficient of risk aversion equal to inverse of intertemporal elasticity
    public static double zeta0_singles = 1000;                               // warm-glow bequests parameter for singles - additive
    public static double zeta1_singles = 0;                                  // warm-glow bequests parameter for singles - slope
    public static double zeta0_couples = 1000;                               // warm-glow bequests parameter for couples - additive
    public static double zeta1_couples = 0;                                  // warm-glow bequests parameter for couples - slope
    public static double delta_singles = 0.97;                               // exponential intertemporal discount factor for singles
    public static double delta_couples = 0.93;                               // exponential intertemporal discount factor for couples

    /**
     * CONSTRUCTOR
     */
    public CESUtility(Grid value_function, Expectations expectations) {

        // initialise data for evaluation of expectations
        this.value_function = value_function;
        this.expectations = expectations;
    }


    /*
     * WORKER METHODS
     */


    /**
     * METHOD TO EVALUATE EXPECTED LIFETIME UTILITY
     *
     * @param args arguments of utility function
     * @return expected lifetime utility, assuming optimising behaviour in the future
     */
    @Override
    public double evaluate(double[] args) {

        if (args.length != 1) {
            throw new InvalidParameterException("CESUtility function not supplied with expected number of arguments");
        }

        // evaluate within period utility
        double consumption_annual = args[0];
        double consumption_component = Math.pow(consumption_annual/expectations.equivalence_scale, 1 - 1/epsilon);
        double leisure_component = Math.pow(expectations.leisure_time, 1 - 1/epsilon);
        double price_of_leisure;
        if (expectations.cohabitation) {
            price_of_leisure = Math.pow(alpha_couples, 1/epsilon);
        } else {
            price_of_leisure = Math.pow(alpha_singles, 1/epsilon);
        }
        double period_utility = Math.pow(consumption_component + price_of_leisure * leisure_component, (1-gamma)/(1-1/epsilon));

        // adjust expectations array
        int dim;
        double numeraire, grid_value;
        for (States states : expectations.anticipated) {
            dim = 0;
            // allow for liquid wealth
            numeraire = expectations.liquid_wealth + expectations.disposable_income_annual - consumption_annual;
            grid_value = Math.log(numeraire + Parameters.C_LIQUID_WEALTH);
            if (expectations.age_index < value_function.scale.axes.length) {
                grid_value = Math.max(grid_value, value_function.scale.axes[expectations.age_index][0][1]);
                grid_value = Math.min(grid_value, value_function.scale.axes[expectations.age_index][0][2]);
            }
            states.states[dim] = grid_value;
        }

        // evaluate expected utility
        double expected_utility = 0;
        double v_expected;
        double zeta0, zeta1;
        if (expectations.cohabitation) {
            zeta0 = zeta0_couples;
            zeta1 = zeta1_couples;
        } else {
            zeta0 = zeta0_singles;
            zeta1 = zeta1_singles;
        }
        double bequest;
        if (expectations.anticipated.length>0) {
            for (int ii=0; ii<expectations.anticipated.length; ii++) {
                if (expectations.probability[ii] > 1.0E-9) {
                    if ( 1.0 - expectations.mortality_probability > 1.0E-9 ) {
                        v_expected = value_function.interpolateAll(expectations.anticipated[ii], true);
                        expected_utility += expectations.probability[ii] *
                                (1- expectations.mortality_probability) * Math.pow(v_expected, 1 - gamma);
                    }
                    if (expectations.mortality_probability > 1.0E-9 && zeta1 > 0) {
                        bequest = Math.max(0, Math.exp(expectations.anticipated[ii].states[0])-Parameters.C_LIQUID_WEALTH);
                        expected_utility += expectations.probability[ii] *
                                        expectations.mortality_probability * Math.pow(zeta1 * (zeta0 + bequest), 1 - gamma);
                    }
                }
            }
        }

        // evaluate total utility for passing to minimisation function
        double discount_factor;
        if (expectations.cohabitation) {
            discount_factor = delta_couples;
        } else {
            discount_factor = delta_singles;
        }
        double total_utility = Math.pow(period_utility + discount_factor * expected_utility, 1/(1-gamma));

        // return
        return - total_utility;
    }
}

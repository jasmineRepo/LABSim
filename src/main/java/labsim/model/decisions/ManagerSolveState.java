package labsim.model.decisions;


import labsim.model.LABSimModel;

/**
 * CLASS TO MANAGE EVALUATION OF NUMERICAL SOLUTIONS FOR SPECIFIC STATE COMBINATION
 *
 * MANAGER EVALUATES THE FEASIBLE DISCRETE CONTROLS, AND OBTAINS SOLUTIONS FOR ALL
 * CONTINUOUS CONTROLS FOR EACH FEASIBLE COMBINATION OF DISCRETE CONTROLS. THE SOLUTION
 * FOR THE STATE COMBINATION IS THEN TAKEN AS THE SOLUTION TO THE COMBINATION OF DISCRETE
 * CONTROLS WITH THE HIGHEST VALUE FUNCTION
 */
public class ManagerSolveState {


    /**
     * ENTRY POINT FOR MANAGER
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     * @param states the state combination for consideration
     *
     * THE MANAGER IS 'run' FROM ManagerSolveGrids
     */
    public static void run(LABSimModel model, Grids grids, States states) {

        // instantiate expectations object
        Expectations invariant_expectations = new Expectations(model, states);

        // identify discrete control options
        double emp1_start = 0;
        double emp1_end = 0;
        double emp1_step = 1;
        double emp2_start = 0;
        double emp2_end = 0;
        double emp2_step = 1;
        if (states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            // principal earner
            if (Parameters.FLAG_IO_EMPLOYMENT1) {
                emp1_end = 1;
                if (Parameters.options_employment1 > 1) {
                    emp1_step = 1 / (double)(Parameters.options_employment1 - 1);
                }
            } else if (Parameters.FLAG_WAGE_OFFER1) {
                emp1_end = 1;
            } else {
                emp1_start = 1;
                emp1_end = 1;
            }
            if (states.getCohabitation()==1) {
                // secondary earner
                if (Parameters.FLAG_IO_EMPLOYMENT2) {
                    emp2_end = 1;
                    if (Parameters.options_employment2 > 1) {
                        emp2_step = 1 / (double)(Parameters.options_employment2 - 1);
                    }
                } else if (Parameters.FLAG_WAGE_OFFER2) {
                    emp2_end = 1;
                } else {
                    emp2_start = 1;
                    emp2_end = 1;
                }
            }
        }

        // collect solutions to all discrete control options
        UtilityMaximisation solution_max = null;
        UtilityMaximisation solution_max_emp1 = null;
        UtilityMaximisation solution_max_emp2 = null;
        for (double emp1_pr=emp1_start; emp1_pr<=(emp1_end+1.0E-7); emp1_pr+=emp1_step) {
            for (double emp2_pr=emp2_start; emp2_pr<=(emp2_end+1.0E-7); emp2_pr+=emp2_step) {

                // obtain local copy of expectations
                Expectations expectations = new Expectations(invariant_expectations);

                // evaluate solution for current control combination
                UtilityMaximisation solution_here = new UtilityMaximisation(grids.value_function, states, expectations, emp1_pr, emp2_pr);

                // check for wage offer solutions for both principal and secondary earner
                if (emp1_end > emp1_start && Parameters.FLAG_WAGE_OFFER1 &&
                        emp2_end > emp2_start && Parameters.FLAG_WAGE_OFFER2 &&
                        emp1_pr < 1.0E-5 && emp2_pr < 1.0E-5) {
                    States target_states = new States(states);
                    target_states.setWageOffer1(0);
                    target_states.setWageOffer2(0);
                    grids.populate(target_states, solution_here);
                }

                // check wage offer solutions for principal earner
                if (emp1_end > emp1_start && Parameters.FLAG_WAGE_OFFER1 && emp1_pr < 1.0E-5) {
                    if (solution_max_emp1==null) {
                        solution_max_emp1 = solution_here;
                    } else {
                        if (solution_max_emp1.optimised_utility < solution_here.optimised_utility) {
                            solution_max_emp1 = solution_here;
                        }
                    }
                }

                // check wage offer solutions for secondary earner
                if (emp2_end > emp2_start && Parameters.FLAG_WAGE_OFFER2 && emp2_pr < 1.0E-5) {
                    if (solution_max_emp2==null) {
                        solution_max_emp2 = solution_here;
                    } else {
                        if (solution_max_emp2.optimised_utility < solution_here.optimised_utility) {
                            solution_max_emp2 = solution_here;
                        }
                    }
                }

                // track state optimum
                if (solution_max==null) {
                    solution_max = solution_here;
                } else {
                    if (solution_max.optimised_utility < solution_here.optimised_utility) {
                        solution_max = solution_here;
                    }
                }
            }
        }

        // save wage offer solutions for principal earner
        if (emp1_end > emp1_start && Parameters.FLAG_WAGE_OFFER1) {
            States target_states = new States(states);
            target_states.setWageOffer1(0);
            grids.populate(target_states, solution_max_emp1);
        }

        // save wage offer solutions for secondary earner
        if (emp2_end > emp2_start && Parameters.FLAG_WAGE_OFFER2) {
            States target_states = new States(states);
            target_states.setWageOffer2(0);
            grids.populate(target_states, solution_max_emp2);
        }

        // save state optimum
        grids.populate(states, solution_max);
    }
}

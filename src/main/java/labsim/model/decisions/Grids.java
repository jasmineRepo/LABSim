package labsim.model.decisions;

import java.security.InvalidParameterException;


/**
 *
 * CLASS TO DEFINE GRIDS THAT STORE INTERTEMPORAL OPTIMISATION SOLUTIONS
 *
 * MODULES OF THE CLASS FACILITATE INPUT AND EXTRACTION OF OPTIMISED DATA
 * AS WELL AS DESCRIPTIVE DETAIL OF DATA
 *
 * THE GRIDS ARE DEFINED AS ONE-DIMENSIONAL VECTORS
 * ELEMENTS OF EACH VECTOR ARE ORGANISED TO REFLECT IMPLICIT DIMENSIONAL AXES
 * SEE DEFINITION OF THE AXES ATTRIBUTE FOR DETAILS
 *
 */
public class Grids {


    /**
     * ATTRIBUTES
     */
    GridScale scale;           // object describing dimensionality of grid
    Grid value_function;       // grid to store value function
    Grid consumption;          // grid to store utility maximising consumption decisions (proportion of cash on hand)
    Grid employment1;          // grid to store utility maximising employment decisions (proportion time spent working by principal income earner)
    Grid employment2;          // grid to store utility maximising employment decisions (proportion time spent working by secondary income earner)


    /**
     * CONSTRUCTOR
     */
    public Grids() {

        // constructor variables
        scale = new GridScale();

        /*
         * INITIALISE GRID VECTORS
         */
        long grid_size = scale.grid_dimensions[scale.sim_life_span-1][3] + scale.grid_dimensions[scale.sim_life_span-1][2];
        value_function = new Grid(scale, grid_size);
        consumption = new Grid(scale, grid_size);
        grid_size = scale.grid_dimensions[Parameters.MAX_AGE_EMPLOYMENT - labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE + 1][3];
        if (Parameters.FLAG_IO_EMPLOYMENT1) employment1 = new Grid(scale, grid_size);
        if (Parameters.FLAG_IO_EMPLOYMENT2) employment2 = new Grid(scale, grid_size);
    }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO ADD CONTROL OPTIMISATION SOLUTION TO THE GRID ARRAYS
     * @param states the state combination for which a solution applies
     * @param solution the solution obtained for the respective state combination
     */
    public void populate(States states, UtilityMaximisation solution) {

        // evaluate state index
        long grid_index = states.returnGridIndex();

        // populate grid storage arrays
        value_function.put(grid_index, solution.optimised_utility);
        int control_counter = 0;
        consumption.put(grid_index, solution.controls[control_counter]);
        control_counter++;
        if (states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            if (Parameters.FLAG_IO_EMPLOYMENT1) {
                if (control_counter < solution.controls.length) {
                    employment1.put(grid_index, solution.controls[control_counter]);
                    control_counter++;
                } else {
                    throw new InvalidParameterException("problem populating solutions for control variable");
                }
            }
            if (states.getCohabitation()==1) {
                if (Parameters.FLAG_IO_EMPLOYMENT2) {
                    if (control_counter < solution.controls.length) {
                        employment2.put(grid_index, solution.controls[control_counter]);
                        control_counter++;
                    } else {
                        throw new InvalidParameterException("problem populating solutions for control variable");
                    }
                }
            }
        }
    }
}

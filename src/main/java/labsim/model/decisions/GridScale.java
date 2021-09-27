package labsim.model.decisions;


/**
 *
 * CLASS TO DEFINE GRID AXES
 *
 */
public class GridScale {


    /**
     * ATTRIBUTES
     */
    int sim_life_span;         // maximum number of periods to make intertemporal optimised decisions
    int number_of_states;      // number of state variables
    long[][] grid_dimensions;  // vector storing summary references for grid dimensions - see constructor for definition
    double[][][] axes;         // vector storing detailed description of grid axes - see constructor for definition


    /**
     * CONSTRUCTOR
     */
    public GridScale() {

        // constructor variables
        int dim_index, age_hh;
        double value;

        // sim_life_span
        sim_life_span = Parameters.MAX_IO_AGE - labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE + 1;

        // number_of_states
        number_of_states = 1;                                   // liquid wealth
        number_of_states++;                                     // wage potential
        if (Parameters.flag_health) number_of_states++;         // health status
        number_of_states++;                                     // birth year
        if (Parameters.FLAG_WAGE_OFFER1) number_of_states++;    // wage offer of principal earner (1 = receive wage offer)
        if (Parameters.FLAG_RETIREMENT) number_of_states++;     // retirement status
        if (Parameters.flag_health) number_of_states++;         //disability status
        if (Parameters.flag_region) number_of_states++;         //region
        if (Parameters.flag_education) number_of_states++;      //student
        if (Parameters.flag_education) number_of_states++;      //education
        number_of_states += Parameters.NUMBER_BIRTH_AGES;       //dependent children
        number_of_states++;                                     //cohabitation (1 = cohabitating)
        number_of_states++;                                     //gender (1 = female)
        axes = new double[sim_life_span][number_of_states][5];
        grid_dimensions = new long[sim_life_span][7];

        /*
         * POPULATE AXES
         *
         * AXES IS AN ARRAY OF DESCRIPTIVE PARAMETERS FOR THE AXES OF THE DECISION GRID
         *      AXES[aa][ii][0] - denotes no of points in dimension ii at age aa
         *      AXES[aa][ii][1] - denotes minimum in dimension ii at age aa
         *      AXES[aa][ii][2] - denotes maximum in dimension ii at age aa
         *      AXES[aa][ii][3] - indicator for continuous state variables (discrete=0, ambiguous=0.5, continuous=1)
         *                        ambiguous is treated as discrete for solutions and continuous for population projections
         *      AXES[aa][ii][4] - indicator for whether state set within inner loop
         *                          axes[aa][ii][4] = 0 indicates the outer-most loop for age index aa
         *                          axes[aa][ii][4] = 1 indicates the inner-most loop for age index aa
         *
         * THE ORDER OF AXES FOR THE DECISION GRID IS AS FOLLOWS (from lowest to highest index):
         *      liquid wealth (w)
         *      wage potential (y)
         *      health status (h)
         *      ... 1 ...
         *      birth year (b)
         *      wage offer (wo)
         *      ... 2 ...
         *      disability status (d)
         *      region (r)
         *      student status (s)
         *      highest education attainment (e)
         *      dependent children (k)
         *      cohabitation (c)
         *      gender (g)
         *      age (a)
         *
         * THE ORDER OF CHARACTERISTICS FOLLOWS A SET OF RULES:
         *      1) continuous variables at top, discrete variables at bottom
         *          a) this is needed to facilitate application of interpolation routines
         *      2) new continuous variables should be inserted immediately above "... 1 ..."
         *      3) new discrete variables should be inserted immediately below "... 2 ..."
         *
         * THE INTERPOLATION ROUTINES ASSUME THAT CONTINUOUS STATES ARE GROUPED CONTIGUOUSLY
         * THE IO SOLUTION ROUTINES ASSUME THAT INNER CHARACTERISTICS ARE ALSO GROUPED CONTIGUOUSLY
         *
         * EXOGENOUS STATES GROUPED AT THE BOTTOM FACILITATES EFFICIENT EVALUATION OF EXPECTATIONS
         * ABOUT PARALLELISED CODE LOOPS IN ManagerSolveGrids
         *
         * THE ORDERING DESCRIBED ABOVE FACILITATES USE OF FLAGS AND MODULAR CODE BLOCS TO TURN STATES ON AND OFF
         *      1) continuous variables are indexed from the top
         *          a) except birth year, which is a discrete variable in the decision solutions and a continuous variable when projecting the population
         *              i) wage offers are "special" discrete variables as they do not require separate solutions to be evaluated
         *      2) discrete variables are indexed from the bottom
         */
        for (int aa = 0; aa < sim_life_span; aa++) {

            age_hh = labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE + aa;
            dim_index = 0;

            // liquid wealth
            axes[aa][dim_index][0] = Parameters.PTS_LIQUID_WEALTH;
            if (age_hh<=Parameters.AGE_DEBT_DRAWDOWN) {
                axes[aa][dim_index][1] = Math.log(Parameters.MIN_LIQUID_WEALTH + Parameters.C_LIQUID_WEALTH);
            } else if (age_hh<Parameters.MAX_AGE_DEBT) {
                value = Parameters.MIN_LIQUID_WEALTH * (Parameters.MAX_AGE_DEBT - age_hh) /
                        (Parameters.MAX_AGE_DEBT - Parameters.AGE_DEBT_DRAWDOWN);
                axes[aa][dim_index][1] = Math.log(value + Parameters.C_LIQUID_WEALTH);
            } else {
                axes[aa][dim_index][1] = Math.log(Parameters.C_LIQUID_WEALTH);
            }
            axes[aa][dim_index][2] = Math.log(Parameters.MAX_LIQUID_WEALTH + Parameters.C_LIQUID_WEALTH);
            axes[aa][dim_index][3] = 1;
            axes[aa][dim_index][4] = 1;
            dim_index++;

            // wage potential
            if (age_hh <= Parameters.MAX_AGE_EMPLOYMENT) {
                axes[aa][dim_index][0] = Parameters.PTS_WAGE_POTENTIAL;
                axes[aa][dim_index][1] = Math.log(Parameters.MIN_WAGE_POTENTIAL + Parameters.C_WAGE_POTENTIAL);
                axes[aa][dim_index][2] = Math.log(Parameters.MAX_WAGE_POTENTIAL + Parameters.C_WAGE_POTENTIAL);
                axes[aa][dim_index][3] = 1;
                axes[aa][dim_index][4] = 1;
                dim_index++;
            }

            ///////////////////  END OF INNER STATES /////////////////////

            // health status
            if (Parameters.flag_health) {
                axes[aa][dim_index][0] = Parameters.PTS_HEALTH;
                axes[aa][dim_index][1] = Parameters.MIN_HEALTH;
                axes[aa][dim_index][2] = Parameters.MAX_HEALTH;
                axes[aa][dim_index][3] = 1;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            ///////////////////  END OF CONTINUOUS STATES /////////////////////

            // birth year
            grid_dimensions[aa][6] = dim_index;
            axes[aa][dim_index][0] = Parameters.pts_birth_year;
            axes[aa][dim_index][1] = Parameters.min_birth_year;
            axes[aa][dim_index][2] = Parameters.max_birth_year;
            axes[aa][dim_index][3] = 0.5;
            axes[aa][dim_index][4] = 0;
            dim_index++;

            // wage offer of principal earner (1 = receive wage offer)
            if (age_hh <= Parameters.MAX_AGE_EMPLOYMENT && Parameters.FLAG_WAGE_OFFER1) {
                axes[aa][dim_index][0] = 2;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            ///////////////////  START OF DISCRETE STATES /////////////////////

            // retirement status
            if (Parameters.FLAG_RETIREMENT && age_hh >= Parameters.MIN_AGE_RETIREMENT && age_hh <= Parameters.MAX_AGE_EMPLOYMENT) {
                axes[aa][dim_index][0] = 2;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //disability status
            if (Parameters.flag_health) {
                axes[aa][dim_index][0] = 2;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //region
            if (Parameters.flag_region) {
                axes[aa][dim_index][0] = Parameters.PTS_REGION;
                axes[aa][dim_index][1] = 1;
                axes[aa][dim_index][2] = Parameters.PTS_REGION;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //student
            if (age_hh <= Parameters.MAX_AGE_STUDENT && Parameters.flag_education) {
                axes[aa][dim_index][0] = Parameters.PTS_STUDENT;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = Parameters.PTS_STUDENT - 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //education
            if (Parameters.flag_education) {
                axes[aa][dim_index][0] = Parameters.PTS_EDUCATION;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = Parameters.PTS_EDUCATION - 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //dependent children
            for (int ii = 0; ii < Parameters.NUMBER_BIRTH_AGES; ii++) {
                if (age_hh >= Parameters.BIRTH_AGE[ii] && age_hh < (Parameters.BIRTH_AGE[ii] + labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                    axes[aa][dim_index][0] = Parameters.MAX_BIRTHS[ii] + 1;
                    axes[aa][dim_index][1] = 0;
                    axes[aa][dim_index][2] = Parameters.MAX_BIRTHS[ii];
                    axes[aa][dim_index][3] = 0;
                    axes[aa][dim_index][4] = 0;
                    dim_index++;
                }
            }

            //cohabitation (1 = cohabitating)
            if (age_hh <= Parameters.MAX_AGE_COHABITATION) {
                axes[aa][dim_index][0] = 2;
                axes[aa][dim_index][1] = 0;
                axes[aa][dim_index][2] = 1;
                axes[aa][dim_index][3] = 0;
                axes[aa][dim_index][4] = 0;
                dim_index++;
            }

            //gender (1 = female)
            axes[aa][dim_index][0] = 2;
            axes[aa][dim_index][1] = 0;
            axes[aa][dim_index][2] = 1;
            axes[aa][dim_index][3] = 0;
            axes[aa][dim_index][4] = 0;
            dim_index++;
        }

        /*
         * POPULATE GRID_DIMENSIONS
         *
         * GRID_DIMENSIONS IS AN ARRAY OF SUMMARY STATISTICS USED TO ASSIST LOOPS THROUGH THE GRIDS
         *      GRID_DIMENSIONS[aa][0] - number of discrete combinations associated with axes on inner loop at age aa
         *      GRID_DIMENSIONS[aa][1] - number of discrete combinations associated with axes on outer loop at age aa
         *      GRID_DIMENSIONS[aa][2] - total size of grid dimensions at age aa
         *      GRID_DIMENSIONS[aa][3] - total number of grid points for all ages up to age aa-1
         *      GRID_DIMENSIONS[aa][4] - number of inner states at age aa
         *      GRID_DIMENSIONS[aa][5] - number of outer states at age aa
         *      GRID_DIMENSIONS[aa][6] - index in axes array of birth year state at age aa
         *
         *          HENCE: GRID_DIMENSIONS[aa][2] = PRODUCT OF GRID_DIMENSIONS[aa][0:1]
         *                 GRID_DIMENSIONS[aa][3] = SUM OF GRID_DIMENSIONS[0:aa-1][2]
         */
        long start_slice_index = 0;
        for (int aa = 0; aa < sim_life_span; aa++) {
            grid_dimensions[aa][0] = innerGridSize(aa);
            grid_dimensions[aa][1] = outerGridSize(aa);
            grid_dimensions[aa][2] = grid_dimensions[aa][0] * grid_dimensions[aa][1];
            grid_dimensions[aa][3] = start_slice_index;
            grid_dimensions[aa][4] = innerGridStates(aa);
            grid_dimensions[aa][5] = outerGridStates(aa);
            start_slice_index += grid_dimensions[aa][2];
        }
    }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO EVALUATE THE NUMBER OF STATES CONSIDERED IN THE INNER LOOP FOR GRID SOLUTIONS AT AGE AA
     * @param aa defines the age index of interest (age = aa + labsim.data.Parameters.AGE_TO_LEAVE_HOME)
     * @return defines the number of states
     */
    public int innerGridStates(int aa) {
        int states, ii;
        states = 0;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] > 0.1) {
                    states += 1;
                }
                ii++;
            } else {
                ii = number_of_states;
            }
        } while (ii < number_of_states);
        return states;
    }

    /**
     * METHOD TO EVALUATE THE NUMBER OF STATES CONSIDERED IN THE OUTER LOOP FOR GRID SOLUTIONS AT AGE AA
     * @param aa defines the age index of interest (age = aa + labsim.data.Parameters.AGE_TO_LEAVE_HOME)
     * @return defines the number of elements in the given grid slice
     */
    public int outerGridStates(int aa) {
        int states, ii;
        states = 0;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] < 0.1) {
                    states += 1;
                }
                ii++;
            } else {
                ii = number_of_states;
            }
        } while (ii < number_of_states);
        return states;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SEGMENT ASSOCIATED WITH ALL INNER STATES AT A GIVEN AGE FOR IO SOLUTIONS
     * @param aa defines the age index of interest (age = aa + labsim.data.Parameters.AGE_TO_LEAVE_HOME)
     * @return defines the number of elements in the given grid slice
     */
    public int innerGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] > 0.1) {
                    size *= (int) Math.round(axes[aa][ii][0]);
                }
                ii++;
            } else {
                ii = number_of_states;
            }
        } while (ii < number_of_states);
        return size;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SEGMENT ASSOCIATED WITH ALL OUTER STATES AT A GIVEN AGE
     * @param aa defines the age index of interest (age = aa + labsim.data.Parameters.AGE_TO_LEAVE_HOME)
     * @return defines the number of elements in the given grid slice
     */
    public int outerGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                if (axes[aa][ii][4] < 0.1) {
                    size *= Math.round(axes[aa][ii][0]);
                }
                ii++;
            } else {
                ii = number_of_states;
            }
        } while (ii < number_of_states);
        return size;
    }

    /**
     * METHOD TO EVALUATE THE SIZE OF THE GRID SLICE ASSOCIATED WITH A GIVEN AGE
     * @param aa defines the age index of interest (age = aa + labsim.data.Parameters.AGE_TO_LEAVE_HOME)
     * @return defines the number of elements in the given grid slice
     */
    public int totalGridSize(int aa) {
        int size, ii;
        size = 1;
        ii = 0;
        do {
            if (axes[aa][ii][0] > 0.1) {
                size *= Math.round(axes[aa][ii][0]);
                ii++;
            } else {
                ii = number_of_states;
            }
        } while (ii < number_of_states);
        return size;
    }

    public int getIndex(Enum<?> axisID, int age_years) {
        // check if return error response (-1)
        if (axisID==Axis.Health && !Parameters.flag_health) return -1;
        if (axisID==Axis.WageOffer1 && (age_years > Parameters.MAX_AGE_EMPLOYMENT || !Parameters.FLAG_WAGE_OFFER1)) return -1;
        if (axisID==Axis.Retirement && (!Parameters.FLAG_RETIREMENT ||
                age_years < Parameters.MIN_AGE_RETIREMENT || age_years > Parameters.MAX_AGE_EMPLOYMENT)) return -1;
        if (axisID==Axis.Disability && !Parameters.flag_health) return -1;
        if (axisID==Axis.Region && !Parameters.flag_region) return -1;
        if (axisID==Axis.Student && (age_years > Parameters.MAX_AGE_STUDENT || !Parameters.flag_education)) return -1;
        if (axisID==Axis.Education && !Parameters.flag_education) return -1;
        if (axisID==Axis.Child && (age_years < Parameters.BIRTH_AGE[0] ||
                age_years > Parameters.BIRTH_AGE[Parameters.NUMBER_BIRTH_AGES-1] + labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)) return -1;

        // run through dimensions to return appropriate response
        // liquid wealth
        int dim_index = 0;
        if (axisID==Axis.LiquidWealth) return dim_index;
        // wage potential
        if (age_years <= Parameters.MAX_AGE_EMPLOYMENT) dim_index++;
        if (axisID==Axis.WagePotential) return dim_index;
        // health status
        if (Parameters.flag_health) dim_index++;
        if (axisID==Axis.Health) return dim_index;
        // birth year
        dim_index++;
        if (axisID==Axis.BirthYear) return dim_index;
        // wage offer of principal earner (1 = receive wage offer)
        if (age_years <= Parameters.MAX_AGE_EMPLOYMENT && Parameters.FLAG_WAGE_OFFER1) dim_index++;
        if (axisID==Axis.WageOffer1) return dim_index;
        // retirement
        if (Parameters.FLAG_RETIREMENT &&
                age_years >= Parameters.MIN_AGE_RETIREMENT &&
                age_years <= Parameters.MAX_AGE_EMPLOYMENT) dim_index++;
        if (axisID==Axis.Retirement) return dim_index;
        // disability
        if (Parameters.flag_health) dim_index++;
        if (axisID==Axis.Disability) return dim_index;
        // region
        if (Parameters.flag_region) dim_index++;
        if (axisID==Axis.Region) return dim_index;
        // student
        if (age_years <= Parameters.MAX_AGE_STUDENT && Parameters.flag_education) dim_index++;
        if (axisID==Axis.Student) return dim_index;
        // education
        if (Parameters.flag_education) dim_index++;
        if (axisID==Axis.Education) return dim_index;
        // dependent children
        for (int ii = 0; ii < Parameters.NUMBER_BIRTH_AGES; ii++) {
            if (age_years >= Parameters.BIRTH_AGE[ii] && age_years < (Parameters.BIRTH_AGE[ii] + labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                dim_index++;
                if (axisID==Axis.Child) return dim_index;
            }
        }
        // cohabitation (1 = cohabitating)
        if (age_years <= Parameters.MAX_AGE_COHABITATION) dim_index++;
        if (axisID==Axis.Cohabitation) return dim_index;
        // gender (1 = female)
        dim_index++;
        if (axisID==Axis.Gender) return dim_index;
        // not recognised
        return -1;
    }
}

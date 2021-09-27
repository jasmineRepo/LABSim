package labsim.model.decisions;

import java.security.InvalidParameterException;


/**
 *
 * CLASS TO STORE DATA FOR GRID ASSOCIATED WITH SINGLE OPTIMISATION VARIABLE
 *
 */
public class Grid {


    /**
     * ATTRIBUTES
     */
    final int MAX_LEN = Integer.MAX_VALUE - 16;
    // MAX_LEN defines the maximum array length permitted for the grid object.  The limits imposed by Java
    // vary by JVM, and are currently due to use of int (4 byte) indexing used for arrays.  The "16" buffer
    // assumed here is arbitrary, accounting for sporadic reports about varying array length constraints.

    long size;         // length of grid array stored here
    GridScale scale;        // object describing dimensionality of grid
    double[] grid;          // array to store variable values at grid ordinates
    double[][] grid_long;   // array to store variable values at grid ordinates, if grid dimensions extend beyond int(4)


    /**
     * CONSTRUCTOR
     */
    public Grid(GridScale scale, long size) {

        this.scale = scale;
        this.size = size;
        if (size <= MAX_LEN) {
            grid = new double[(int)size];
        } else {
            int slices = 1 + (int)((double)size / (double)MAX_LEN);
            grid_long = new double[slices][];
            for (int ii=0; ii<slices; ii++) {
                if (ii==slices-1) {
                    grid_long[ii] = new double[(int)(size%MAX_LEN)];
                } else {
                    grid_long[ii] = new double[MAX_LEN];
                }
            }
        }
    }


    /*
     * WORKING METHODS
     */


    /**
     * METHOD TO ADD VALUE TO GRID STORE
     * @param index storage index for value
     * @param value value to add to store
     */
    public void put(long index, double value) {

        if (grid!=null) {
            grid[(int)index] = value;
        } else {
            int slice = (int)((double)index / (double)MAX_LEN);
            int ii = (int)(index%MAX_LEN);
            grid_long[slice][ii] = value;
        }
    }

    /**
     * METHOD TO GET VALUE FROM GRID STORE
     * @param index storage index for value
     * @return value retrieved from store
     */
    public double get(long index) {
        double value;
        if (grid!=null) {
            value = grid[(int)index];
        } else {
            int slice = (int)((double)index / (double)MAX_LEN);
            int ii = (int)(index%MAX_LEN);
            value = grid_long[slice][ii];
        }
        return value;
    }

    /**
     * METHOD TO RETURN A NUMERICAL APPROXIMATION FOR THE GRID VALUE ASSOCIATED WITH A COMPLETE VECTOR OF STATE
     * CHARACTERISTICS
     *
     * The method begins by identifying the grid slice associated with the supplied combination of discrete states
     * Linear states are then approximated by a linear interpolation method, interpolateContinuous
     *
     * @param supplied full state combination (continuous and discrete)
     * @param solution_call boolean equal to true if call is from the search routine for a maximum to the IO problem
     *                      This flag is used to distinguish treatment of states that are considered discrete for the
     *                      IO solution process and continuous otherwise (e.g. birth year)
     * @return  numerical approximation of grid value
     */
    public double interpolateAll(States supplied, boolean solution_call) {

        // find references to control for discrete state variables
        States copy = new States(supplied);
        int no_states = (int)scale.grid_dimensions[supplied.age_index][4] + (int)scale.grid_dimensions[supplied.age_index][5];
        double continuous_cutoff = 0.3;
        if (solution_call) continuous_cutoff += 0.3;
        boolean flag_all_continuous = true;
        int number_continuous = 0;
        for (int ii=0; ii<no_states; ii++) {
            if (scale.axes[supplied.age_index][ii][3] > continuous_cutoff) {
                // treat as continuous
                number_continuous++;
                copy.states[ii] = scale.axes[supplied.age_index][ii][1];    // set copy value to lower bound
                if (!flag_all_continuous) {
                    throw new InvalidParameterException("continuous states do not appear to have been organised contiguously");
                }
            } else {
                // treat as discrete
                flag_all_continuous = false;
            }
        }
        long starting_index = copy.returnGridIndex();

        // return result
        return interpolateContinuous(supplied, number_continuous, starting_index);
    }

    /**
     * METHOD TO RETURN A NUMERICAL APPROXIMATION FOR THE GRID VALUE ASSOCIATED WITH THE SUBSET OF CONTINUOUS STATE
     * CHARACTERISTICS
     *
     * Method uses linear interpolation methods to approximate value for grid slice associated with continuous
     * state variables, as supplied by the interpolateAll() method.
     *
     * @param supplied state combination to interpolate over
     * @param dimensions number of states to conduct the interpolation over
     * @param starting_index starting index for grid slice of interpolation
     * @return  numerical approximation of grid value
     */
    public double interpolateContinuous(States supplied, int dimensions, long starting_index) {

        //  working variables
        final double tol = Math.ulp(1.0);
        long index_here;
        double value_here, weight_total;
        int[] dims = new int[dimensions];
        int[] offset = new int[dimensions];
        int[] mm = new int[dimensions];
        int[] nn = new int[dimensions];
        int[] dd = new int[dimensions];
        double[] ss = new double[dimensions];
        double[] weight = new double[(int)Math.pow(2,dimensions)];

        //  evaluate interpolation offsets;
        dims[0] = (int)(scale.axes[supplied.age_index][0][0]+tol);
        offset[0] = 1;
        if (dimensions > 1) {
            for (int ii=1; ii<dimensions; ii++) {
                dims[ii] = (int)(scale.axes[supplied.age_index][ii][0]+tol);
                offset[ii] = offset[ii-1] * dims[ii-1];
            }
        }

        // identify reference points
        for (int ii = 0; ii<dimensions; ii++) {
            ss[ii] = (supplied.states[ii] - scale.axes[supplied.age_index][ii][1]) *
                    (scale.axes[supplied.age_index][ii][0]-1) /
                    (scale.axes[supplied.age_index][ii][2] - scale.axes[supplied.age_index][ii][1]);
            mm[ii] = (int)(ss[ii] + tol);
            if ( mm[ii] == (dims[ii]-1) ) {
                // at upper bound - step one backward
                mm[ii] -= 1;
            }
            ss[ii] -= mm[ii];
        }

        // check that point is internal to grid
        for (int ii=0; ii<dimensions; ii++) {
            if (supplied.states[ii] < scale.axes[supplied.age_index][ii][1]-tol) {
                throw new InvalidParameterException("interpolation point below minimum described by grid");
            } else if (supplied.states[ii] > scale.axes[supplied.age_index][ii][2]+tol) {
                throw new InvalidParameterException("interpolation point above maximum described by grid");
            }
        }

        // interpolate over states
        dd[0] = -1;
        double result = 0;
        weight_total = 0;
        for (int ii=0; ii<Math.pow(2, dimensions); ii++) {
            // loop over each test point

            dd[0] += 1;
            int jj = 0;
            while (dd[jj] > 1) {
                dd[jj] = 0;
                jj++;
                dd[jj] += 1;
            }

            // calculate weights and indices
            weight[ii] = 1.0;
            for (jj=0; jj<dimensions; jj++) {
                nn[jj] = mm[jj] + dd[jj];
                weight[ii] *= (1 - Math.abs(dd[jj]-ss[jj]));
            }
            if ( weight[ii] > (1.0/Math.pow(2,dimensions)*1.0E-3)) {
                // take point into consideration

                index_here = 0;
                for (jj = 0; jj<dimensions; jj++) {
                    index_here += nn[jj] * offset[jj];
                }
                index_here += starting_index;
                value_here = get(index_here);
                result += value_here * weight[ii];
                weight_total += weight[ii];
            }
        }

        result /= weight_total;
        return result;
    }
}

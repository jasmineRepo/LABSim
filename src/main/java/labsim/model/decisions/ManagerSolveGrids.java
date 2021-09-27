package labsim.model.decisions;

import labsim.model.LABSimModel;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.IntStream;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SOLUTIONS TO POPULATE INTERTEMPORAL OPTIMISATION GRIDS
 *
 * THE SOLUTION PROCEDURE RUNS THROUGH ALL STATE COMBINATIONS DESCRIBED BY THE GRIDS
 * VIA FOUR NESTED LOOPS
 *
 * THE FIRST (OUTER-MOST) LOOP CONSIDERS SLICES OF THE GRIDS DISTINGUISHED BY AGE
 * THIS IS MOTIVATED BY THE USE OF BACKWARD INDUCTION TECHNIQUES BY THE SOLUTION METHOD
 *
 * THE SECOND LOOP CONSIDERS COMBINATIONS OF CHARACTERISTICS, FOR WHICH EXPECTATIONS
 * ARE EXOGENOUS OF CONTROL VARIABLES
 * THIS ALLOWS EVALUATIONS FOR EXPECTATIONS OVER THESE VARIABLES TO BE DONE ONCE FOR
 * ALL COMBINATIONS OF STATES CONSIDERED IN THE INNER TWO LOOPS
 *      STATES IN THIS LOOP ARE IDENTIFIED BY AXES[aa][ii][4] = 0 OR 0.5
 *
 * THE THIRD LOOP IS PARALLISED TO TAKE FULL ADVANTAGE OF COMPUTING RESOURCES
 *
 * THE FOURTH (INNER-MOST) LOOP IS SEPARATED FROM THE THIRD LOOP ONLY TO ECONOMISE
 * THE OVER-HEAD ASSOCIATED WITH PARALLELISATIONS
 *
 */
public class ManagerSolveGrids {


    /**
     * ENTRY POINT FOR MANAGER
     * @param grids refers to the look-up table that stores IO solutions (the 'grids')
     *
     * THE MANAGER IS 'run' FROM ManagerPopulateGrids
     */
    public static void run(LABSimModel model, Grids grids) {

        // solve grids using backward-induction, working from the last potential period in life
        for (int aa=grids.scale.sim_life_span-1; aa>=0; aa--) {

            Instant before = Instant.now();

            // set age specific working variables
            int inner_dimension = (int)grids.scale.grid_dimensions[aa][0];
            int outer_dimension = (int)grids.scale.grid_dimensions[aa][1];
            int threads = Runtime.getRuntime().availableProcessors();
            int thread_loops = Math.min(threads * 3, inner_dimension);
            int thread_block = inner_dimension / thread_loops;

            // loop over outer dimensions, for which expectations are independent of IO decisions (controls)
            for (int ii_outer=0; ii_outer<outer_dimension; ii_outer++) {

                // identify current state combination for outer states
                States outer_states = new States(grids.scale, aa);
                outer_states.populateOuterGridStates(ii_outer);
                boolean loop_consider = outer_states.checkOuterStateCombination();
                if (loop_consider) {

                    // set year for proceeding period for use in expectations
                    model.setYear(outer_states.getYear()+1);

                    // loop over inner dimensions
                    // this loop is parallelised - it can be made serial by omitting parallel()
                    IntStream.range(0, thread_loops).parallel().forEach(ii_threads -> {
                    //IntStream.range(0, thread_loops).forEach(ii_threads -> {

                        int ii_start = ii_threads * thread_block;
                        int ii_end;
                        if (ii_threads==(thread_loops-1)) {
                            ii_end = inner_dimension;
                        } else {
                            ii_end = (ii_threads + 1) * thread_block;
                        }
                        // loop within parallel segment
                        for (int ii_inner=ii_start; ii_inner<ii_end; ii_inner++) {

                            // identify current state combination and copy expectations
                            States current_states = new States(outer_states);
                            current_states.populateInnerGridStates(ii_inner);
                            ManagerSolveState.run(model, grids, current_states);
                        }
                    });
                }
            }
            Instant after = Instant.now();
            Duration duration = Duration.between(before, after);
            int age_here = labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE + aa;
            System.out.println("calculations for age " + age_here + " completed in " + duration.toMillis() + " ms");
        }
    }
}

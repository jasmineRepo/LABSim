package labsim.model.decisions;

import java.util.Arrays;


/**
 *
 * CLASS TO EVALUATE AND STORE BOUNDARY POINTS OF LINEAR ORDINATE SEARCH CO-ORDINATES
 *
 */
public class BoundaryPoints {


    /**
     * ATTRIBUTES
     */
    boolean boundary_solution;      // indicator that the search vector describes a corner solution
    double[] lower_boundary;        // array to store lower boundary ordinates
    double[] upper_boundary;        // array to store upper boundary ordinates


    /**
     *  CONSTRUCTOR
     *
     * @param ax lower corner of search domain
     * @param cx upper corner of search domain
     * @param bx point on linear search vector
     * @param xi gradient of linear search vector
     */
    public BoundaryPoints(double[] ax, double[] cx, double[] bx, double[] xi) {

        // initialise attributes
        int nn = ax.length;
        lower_boundary = new double[nn];
        upper_boundary = new double[nn];
        boundary_solution = true;

        // start analysis
        int id_omit;
        final double zeps = 1.0E-10;
        double test_val, adj_factor;
        double[] testx = new double[nn];

        // rank dimensions from steepest to least steep gradient
        double[] xi_temp = new double[nn];
        for (int ii=0; ii<nn; ii++) {
            xi_temp[ii] = Math.abs(xi[ii]);
        }
        int[] rank = RankArray.rankDouble(xi_temp);

        // identify limit points
        int lp_id = 1;
        for (int ii=nn-1; ii>=0; ii--) {
            // test from fastest to slowest dimension

            for (int jj = 1; jj <= 2; jj++) {
                // test on ax first and { cx

                if (lp_id <= 2) {
                    // require two limit points for the line

                    if (jj == 1) {
                        // test along dimension ax
                        test_val = ax[rank[ii]];
                    } else {
                        // test along dimension cx
                        test_val = cx[rank[ii]];
                    }
                    if (bx[rank[ii]] == test_val) {
                        // no space to project - bx must denote one limit point (as we are working from fastest to slowest dimension)

                        if (lp_id == 1) {
                            // allocate first limit point identified to Aax
                            lower_boundary = Arrays.copyOf(bx, nn);
                        } else {
                            upper_boundary = Arrays.copyOf(bx, nn);
                        }
                        lp_id = lp_id + 1;
                    } else {
                        // project to boundary point

                        adj_factor = (test_val - bx[rank[ii]]) / xi[rank[ii]];
                        id_omit = 0;
                        for (int kk = 0; kk < nn; kk++) {

                            testx[kk] = bx[kk] + adj_factor * xi[kk];
                            if (Math.abs(testx[kk] - Math.max(ax[kk], cx[kk])) < zeps) {

                                testx[kk] = Math.max(ax[kk], cx[kk]);
                            } else if (Math.abs(testx[kk] - Math.min(ax[kk], cx[kk])) < zeps) {

                                testx[kk] = Math.min(ax[kk], cx[kk]);
                            } else if ((testx[kk] > Math.max(ax[kk], cx[kk])) || (testx[kk] < Math.min(ax[kk], cx[kk]))) {
                                // projected point outside boundary - boundary point defined by alternative limit condition

                                id_omit = 1;
                            }
                        }
                        if (id_omit == 0) {
                            // found a limit point

                            if (lp_id == 1) {

                                lower_boundary = Arrays.copyOf(testx, nn);
                            } else {

                                upper_boundary = Arrays.copyOf(testx, nn);
                            }
                            lp_id = lp_id + 1;
                        }
                    }
                }  // test over limit points
            }  // test over ax and cx
        }  // test over each dimension of search space

        // check if we have a corner solution
        for (int ii=0; ii<nn; ii++) {

            if ( Math.abs(lower_boundary[ii]-upper_boundary[ii]) > 1.0E-5 ) {

                boundary_solution = false;
                break;
            }
        }
    }
}

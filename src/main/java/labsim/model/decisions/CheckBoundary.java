package labsim.model.decisions;

import java.util.Arrays;


/**
 *
 * CHECK FOR MINIMUM AT BOUNDARY OF LINEAR SEARCH DOMAIN
 *
 */
 public class CheckBoundary {


    /**
     * ATTRIBUTES
     */
    boolean boundary_solution = false;      // boolean indicating whether solution identified at boundary
    FunctionEvaluation result;              // minimised solution identified for function by constructor (rough approximation)


    /**
     *
     * CONSTRUCTOR
     *
     * @param ax lower bound of linear search domain
     * @param cx upper bound of linear search domain
     * @param bx initial guess for co-ordinates of function minimum
     * @param function interface access to function for evaluation
     */
    public CheckBoundary(double[] ax, double[] cx, double[] bx, IEvaluation function) {

        // initialise attributes
        final int nn = ax.length;
        result = new FunctionEvaluation(nn);

        // define working variables
        double eps = 3 * Math.ulp(1.0);

        // identify dimension with largest difference, and adopt as reference for adjustments
        int adim = 0;
        if (nn > 1) {
            for (int ii=1; ii<nn; ii++) {
                if ( Math.abs(ax[ii]-cx[ii]) > Math.abs(ax[adim]-cx[adim]) ) {
                    adim = ii;
                }
            }
        }

        // evaluate function values at entry co-ordinates
        double fl = function.evaluate(ax);
        double fm = function.evaluate(bx);
        double fh = function.evaluate(cx);
        double fx = fm;

        // check for alternative boundary types
        if ( (Math.abs(fl-fm) < eps) && (Math.abs(fm-fh) < eps) ) {
            // flat profile - take lower bound

            boundary_solution = true;
            bx = Arrays.copyOf(ax,nn);
            fx = fl;
        } else if ( ((fm+eps) >= fl) || ((fm+eps) >= fh) ) {
            // bx greater than or equal to either boundary, need to perform boundary check

            boundary_solution = true;
            if ((fl + eps) > fh) {
                // reverse ax and cx, so that fl is less than fh

                double[] bbx = Arrays.copyOf(ax, nn);
                ax = Arrays.copyOf(cx, nn);
                cx = Arrays.copyOf(bbx, nn);

                fm = fl;
                fl = fh;
                fh = fm;
            }

            // first run quick check to rule out cx
            int index = 1;
            double[] bbx = Arrays.copyOf(bx, nn);
            while (index <= 2) {

                for (int jj = 0; jj < nn; jj++) {
                    bbx[jj] = bbx[jj] + (cx[jj] - bx[jj]) / 3.0;
                }
                fm = function.evaluate(bbx);
                if ((fm + eps) < fl) {
                    // new boundaries identified

                    boundary_solution = false;
                    ax = Arrays.copyOf(bx, nn);
                    bx = Arrays.copyOf(bbx, nn);
                    fx = fm;
                    index = 3;
                } else {

                    index++;
                }
            }
            if (boundary_solution) {
                // cx ruled out - check ax

                double eps_ad = Math.abs(cx[adim] - ax[adim]);
                double eps_a = Math.abs(bx[adim] - ax[adim]) / eps_ad;
                if (eps_a < 0.05) {
                    // starting bx already very close to ax - check just above ax

                    for (int ii = 0; ii < nn; ii++) {
                        bx[ii] = 0.99 * ax[ii] + 0.01 * cx[ii];
                    }
                    fm = function.evaluate(bx);
                    if ((fm + eps) < fl) {
                        // new starting conditions identified

                        boundary_solution = false;
                        fx = fm;
                    }
                } else {
                    // perform grid search between bx and ax

                    while ((eps_a > 0.05) && (boundary_solution)) {
                        cx = Arrays.copyOf(bx, nn);
                        for (int ii = 0; ii < nn; ii++) {
                            bx[ii] = 0.5 * (ax[ii] + cx[ii]);
                        }
                        eps_a = Math.abs(bx[adim] - ax[adim]) / eps_ad;
                        fm = function.evaluate(bx);
                        if ((fm + eps) < fl) {
                            // new boundaries identified

                            boundary_solution = false;
                            fx = fm;
                        }
                    }
                }
                if (boundary_solution) {
                    // assume lower bound

                    bx = Arrays.copyOf(ax, nn);
                    fx = fl;
                }
            }
        }

        // return
        result.ordinates = Arrays.copyOf(bx,nn);
        result.value = fx;
    }
}

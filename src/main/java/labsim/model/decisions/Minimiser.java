package labsim.model.decisions;

import java.security.InvalidParameterException;
import java.util.Arrays;


/**
 *
 * CLASS TO EVALUATE FUNCTION MINIMISATIONS
 *
 */
public class Minimiser {


    /**
     * ATTRIBUTES
     */
    int nn;                             // number of arguments of function
    double[] lower_bounds;              // lower bounds of continuous control variables to optimise
    double[] upper_bounds;              // upper bounds of continuous control variables to optimise
    double[] target;                    // starting co-ordinates at entry and co-ordinates of minimum at exit
    IEvaluation function;               // function to minimise
    double minimised_value;             // minimised value of function


    /**
     * CONSTRUCTOR
     *
     * @param lower_bounds lower corner of search domain
     * @param upper_bounds upper corner of search domain
     * @param target starting guess for co-ordinates that minimise function
     * @param function interface to function
     */
    public Minimiser(double[] lower_bounds, double[] upper_bounds, double[] target, IEvaluation function) {
        this.lower_bounds = lower_bounds;
        this.upper_bounds = upper_bounds;
        this.target = target;
        this.function = function;
        nn = lower_bounds.length;
    }


    /*
     *  WORKER METHODS
     */


    /**
     * ENTRY POINT FOR MINIMISATION
     */
    public void minimise() {
        FunctionEvaluation result;
        if (nn == 1) {
            result = brent(lower_bounds, upper_bounds, target);
        } else {
            result = powell();
        }
        minimised_value = result.value;
        target = Arrays.copyOf(result.ordinates, nn);
    }

    /**
     *
     * Minimisation routine in multiple dimensions without derivatives
     * Based on Powell's method as recommended by Numerical Recipes, 1987, p. 299.
     *
     */
    private FunctionEvaluation powell() {

        // initialise optimisation parameters
        FunctionEvaluation result = new FunctionEvaluation(nn);
        final int itmax = 200;
        final double ftol = Math.pow(Math.ulp(1.0), 0.5), eps = 1.0E-10, eps_v = Math.ulp(1.0);

        // declare working variables
        int iter, ibig;
        double fx, fret, fp, fe, fptt, del, adjustment_factor, t_val;
        boolean finished;
        double[] pt;
        double[] ptt = new double[nn];
        double[] xit = new double[nn];
        double[][] xi = new double[nn][nn];

        // set starting values
        double[] ax = Arrays.copyOf(lower_bounds, nn);
        double[] bx = Arrays.copyOf(target, nn);
        double[] cx = Arrays.copyOf(upper_bounds, nn);
        for (int ii=0; ii<nn; ii++) {
            xi[ii][ii] = 1;
        }
        fx = function.evaluate(bx);
        fret = fx;

        // start search routine
        finished = false;
        iter = 0;
        while (!finished) {

            iter++;
            fp = fret;
            pt = Arrays.copyOf(bx, nn);
            ibig = 0;
            del = 0;
            for (int ii=0; ii<nn; ii++) {
                for (int jj=0; jj<nn; jj++) {
                    xit[jj] = xi[jj][ii];
                }
                fptt = fret;
                BoundaryPoints boundaries = new BoundaryPoints(ax, cx, bx, xit);
                if (boundaries.boundary_solution) {
                    // obtained a corner solution
                    fx = function.evaluate(bx);
                } else {
                    result = brent(boundaries.lower_boundary, boundaries.upper_boundary, bx);
                    fx = result.value;
                    bx = Arrays.copyOf(result.ordinates,nn);
                }
                fret = fx;
                if ( (fptt-fret) > del ) {
                    del = fptt - fret;
                    ibig = ii;
                }
            }
            if ( 2*(fp - fret) < ftol*(Math.abs(fret)+ Math.abs(fp)+eps) ) {
                finished = true;
            } else {
                adjustment_factor = 1;
                for (int jj=0; jj<nn; jj++) {
                   ptt[jj] = bx[jj] + bx[jj] - pt[jj];      // extrapolated point along direction of average shift
                   xit[jj] = bx[jj] - pt[jj];               // average direction of shift
                   if ( ptt[jj] < ax[jj] ) {
                       adjustment_factor = Math.min(adjustment_factor, (ax[jj]-bx[jj])/(bx[jj]-pt[jj]));
                  } else if ( ptt[jj] > cx[jj] ) {
                       adjustment_factor = Math.min(adjustment_factor, (cx[jj]-bx[jj])/(bx[jj]-pt[jj]));
                   }
                }
                if ( adjustment_factor < 0 ) {
                    throw new InvalidParameterException("problem evaluating adjustment factor in Powell optimisation routine");
               } else if ( Math.abs(adjustment_factor-1) > 1.0E-5 ) {
                    for (int jj=0; jj<nn; jj++) {
                        ptt[jj] = bx[jj] + adjustment_factor * (bx[jj] - pt[jj]);
                    }
                }
                fe = function.evaluate(ptt);
                fptt = fe;
                if ( fptt+eps_v < fp ) {
                    t_val = 2 * (fp - 2*fret + fptt) * Math.pow(fp - fret -del, 2) - del * Math.pow(fp-fptt, 2);
                    if ( t_val < 0 ) {
                        BoundaryPoints boundaries = new BoundaryPoints(ax, cx, bx, xit);
                        if (boundaries.boundary_solution) {
                            // obtained a corner solution
                            fx = function.evaluate(bx);
                        } else {
                            result = brent(boundaries.lower_boundary, boundaries.upper_boundary, bx);
                            fx = result.value;
                            bx = Arrays.copyOf(result.ordinates,nn);
                        }
                        fret = fx;
                        for (int jj=0; jj<nn; jj++) {
                            xi[jj][ibig] = xi[jj][nn-1];
                            xi[jj][nn-1] = xit[jj];
                        }
                    }
                }
            }
            if ( (iter == itmax) && !finished) {
                // failure to converge
                finished = true;
            }
        }
        result.value = fx;
        result.ordinates = Arrays.copyOf(bx, nn);
        return result;
    }

    /**
     *
     * Minimisation routine based on simple grid search
     *
     * @param xx starting point of grid search
     * @param step_number number of steps to span over search domain
     * @return FunctionEvaluation object describing minimised function ordinates and value
     */
    private FunctionEvaluation gridSearch(double[] xx, double[] step_size, int step_number) {

        // initialise return object
        FunctionEvaluation result = new FunctionEvaluation(nn);

        // initialise search vectors
        final double eps = 3 * Math.ulp(1.0);
        double fx = function.evaluate(xx);
        double fu;
        double[] uu = Arrays.copyOf(xx, nn);
        int iter = 1;
        while (iter <= step_number) {

            // update uu
            for (int ii = 0; ii < nn; ii++) {
                uu[ii] = uu[ii] + step_size[ii];
            }

            // perform check
            fu = function.evaluate(uu);
            if ((fu + eps) < fx) {
                // a new optimum has been found
                fx = fu;
                xx = Arrays.copyOf(uu, nn);
            }

            // update
            iter++;
        }

        // return results
        result.ordinates = Arrays.copyOf(xx,nn);
        result.value = fx;
        return result;
    }

    /**
     *
     * Minimisation routine over a single dimension without reference to derivatives
     * Brent solution is based upon Numerical Recipes, 1987, p. 284
     *
     * Code searches for a local minimum using Brent's parabolic interpolation routine.
     * The outer solution is bracketed by ax and cx.
     *
     * Code has been amended to permit linear search over multiple dimensions
     *
     * @param ax notional lower bound co-ordinates
     * @param cx notional upper bound co-ordinates
     * @param bx starting co-ordinates at entry, and minimised co-ordinates at exit
     * @return FunctionEvaluation object describing minimised function ordinates and value
     */
    private FunctionEvaluation brent(double[] ax, double[] cx, double[] bx) {

        // initialise return object
        FunctionEvaluation result = new FunctionEvaluation(nn);

        // initialise optimisation parameters
        final int itmax = 100;
        final double tol = 2* Math.pow(Math.ulp(1.0), 0.25), eps = 3* Math.ulp(1.0), zeps = 1.0E-10, cgold = 0.381966;

        // initialise working variables
        int iter, step_number;
        boolean flat_about_min = false;
        double[] bx0, uu, xx, aa, bb, vv, ww, mm, step_size;
        double ee, etemp, dd, rr, qq, pp, tol1, tol2;
        double fx0, fx, fu, fv, fw;

        // record initial guess
        bx0 = Arrays.copyOf(bx,nn);
        fx0 = function.evaluate(bx0);

        // identify dimension with largest difference, and adopt as reference for adjustments
        int adim = 0;
        if (nn > 1) {
            for (int ii=1; ii<nn; ii++) {
                if ( Math.abs(ax[ii]-cx[ii]) > Math.abs(ax[adim]-cx[adim]) ) {
                    adim = ii;
                }
            }
        }

        // check if initial guess is too close to one of the extremes
        if ( Math.abs(bx[adim]-ax[adim]) < zeps ) {
            for (int ii=0; ii<nn; ii++) {
                bx[ii] = 0.95 * ax[ii] + 0.05 * cx[ii];
            }
        } else if ( Math.abs(bx[adim]-cx[adim]) < zeps ) {
            for (int ii=0; ii<nn; ii++) {
                bx[ii] = 0.05 * ax[ii] + 0.95 * cx[ii];
            }
        }

        // check for boundary solution
        CheckBoundary boundaries = new CheckBoundary(ax, cx, bx, function);
        fx = boundaries.result.value;
        xx = Arrays.copyOf(boundaries.result.ordinates, nn);

        // start search routine
        if ( !boundaries.boundary_solution ) {

            // ensure that ax defines lower boundary
            uu = Arrays.copyOf(ax, nn);
            if ( ax[adim] > cx[adim] ) {
                ax = Arrays.copyOf(cx, nn);
                cx = Arrays.copyOf(uu, nn);
            }

            // initialise search variables
            aa = Arrays.copyOf(ax, nn);
            bb = Arrays.copyOf(cx, nn);
            vv = Arrays.copyOf(bx, nn);
            ww = Arrays.copyOf(vv, nn);
            xx = Arrays.copyOf(vv, nn);
            ee = 0;
            dd = 0;
            fv = fx;
            fw = fx;
            iter = 1;
            mm = new double[nn];
            for (int ii = 0; ii < nn; ii++) {
                mm[ii] = 0.5 * aa[ii] + 0.5 * bb[ii];
            }
            tol1 = tol * Math.abs(xx[adim]) + zeps;
            tol2 = 2.0 * tol1;
            while ( ( Math.abs(xx[adim]-mm[adim]) > (tol2 - 0.5*(bb[adim]-aa[adim])) ) && ( iter <= itmax ) && (!flat_about_min) ) {
                if ( Math.abs(ee) > tol1 ) {
                    // construct a parabolic fit
                    rr = (xx[adim]-ww[adim]) * (fx-fv);
                    qq = (xx[adim]-vv[adim]) * (fx-fw);
                    pp = (xx[adim]-vv[adim])*qq - (xx[adim]-ww[adim])*rr;
                    qq = 2.0 * (qq-rr);
                    if ( qq > 0.0 ) {
                        pp = -pp;
                    }
                    qq = Math.abs(qq);
                    etemp = ee;
                    ee = dd;
                    if ( (Math.abs(pp) >= Math.abs(0.5*qq*etemp)) || (pp <= qq*(aa[adim]-xx[adim])) || (pp >= qq*(bb[adim]-xx[adim])) ) {

                        // take golden section step into larger of two dimensions
                        if ( xx[adim] >= mm[adim] ) {
                            ee = aa[adim]-xx[adim];
                        } else {
                            ee = bb[adim]-xx[adim];
                        }
                        dd = cgold * ee;
                    } else {

                        // take the parabolic step
                        dd = pp / qq;
                        uu[adim] = xx[adim] + dd;
                        if ( ((uu[adim]-aa[adim]) < tol2) || ((bb[adim]-uu[adim]) < tol2) ) {
                            if (mm[adim]-xx[adim]>0) {
                                dd = tol1;
                            } else {
                                dd = -tol1;
                            }
                        }
                    }
                } else {
                    // take golden section step
                    if ( xx[adim] >= mm[adim] ) {
                        ee = aa[adim]-xx[adim];
                    } else {
                        ee = bb[adim]-xx[adim];
                    }
                    dd = cgold * ee;
                }
                // dd is now computed, either from golden section or parabolic fit - compute new function evaluation
                if ( Math.abs(dd) >= tol1 ) {
                    uu[adim] = xx[adim] + dd;
                } else {
                    if (dd>0) {
                        uu[adim] = xx[adim] + tol1;
                    } else {
                        uu[adim] = xx[adim] - tol1;
                    }
                }
                for (int ii=0; ii<nn; ii++) {
                    if ( ii != adim ) {
                        uu[ii] = ax[ii] + (cx[ii]-ax[ii]) * (uu[adim]-ax[adim]) / (cx[adim]-ax[adim]);
                    }
                }
                fu = function.evaluate(uu);
                if ( fu <= fx ) {

                    if ( fu == fx ) {
                        // flat region found - undertake grid search to verify that minimum has been found

                        uu = Arrays.copyOf(xx, nn);
                        step_size = new double[nn];
                        for (int ii = 0; ii < nn; ii++) {
                            step_size[ii] = (cx[ii] - ax[ii]) / 200;
                        }
                        step_number = Math.min(10, (int) ((cx[adim] - uu[adim]) / step_size[adim]));
                        result = gridSearch(uu, step_size, step_number);
                        uu = Arrays.copyOf(result.ordinates,nn);
                        fu = result.value;

                        if ((fu + eps) > fx) {
                            // test below

                            uu = Arrays.copyOf(xx, nn);
                            for (int ii = 0; ii < nn; ii++) {
                                step_size[ii] = -step_size[ii];
                            }
                            step_number = Math.min(10, (int) ((uu[adim] - ax[adim]) / step_size[adim]));
                            result = gridSearch(uu, step_size, step_number);
                            uu = Arrays.copyOf(result.ordinates,nn);
                            fu = result.value;
                        }
                        if ( fu == fx ) {
                            flat_about_min = true;
                        }
                    }
                    if ((fu+eps) < fx) {
                        if( uu[adim] >= xx[adim] ) {
                            aa = Arrays.copyOf(xx,nn);
                        } else {
                            bb = Arrays.copyOf(xx,nn);
                        }
                        vv = Arrays.copyOf(ww,nn);
                        fv = fw;
                        ww = Arrays.copyOf(xx,nn);
                        fw = fx;
                        xx = Arrays.copyOf(uu,nn);
                        fx = fu;
                    }
                } else {

                    if( uu[adim] < xx[adim] ) {
                        aa = Arrays.copyOf(uu,nn);
                    } else {
                        bb = Arrays.copyOf(uu,nn);
                    }
                    if ( (fu <= fw) || (ww[adim] == xx[adim]) ) {
                        vv = Arrays.copyOf(ww,nn);
                        fv = fw;
                        ww = Arrays.copyOf(uu,nn);
                        fw = fu;
                   } else if ( (fu <= fv) || (vv[adim] == xx[adim]) || (vv[adim] == ww[adim]) ) {
                        vv = Arrays.copyOf(uu,nn);
                        fv = fu;
                    }
                }
                iter++;
                if ( iter <= itmax ) {
                    // compute next iteration
                    mm = new double[nn];
                    for (int ii = 0; ii < nn; ii++) {
                        mm[ii] = 0.5 * aa[ii] + 0.5 * bb[ii];
                    }
                    tol1 = tol * Math.abs(xx[adim]) + zeps;
                    tol2 = 2.0 * tol1;
                } else {
                    throw new InvalidParameterException("search algorithm failed to converge");
                }
            }
        }
        if ( fx <= fx0 ) {
            result.ordinates = Arrays.copyOf(xx,nn);
            result.value = fx;
        } else {
            result.ordinates = Arrays.copyOf(bx0,nn);
            result.value = fx0;
        }
        return result;
    }
}

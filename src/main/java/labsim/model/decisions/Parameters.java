package labsim.model.decisions;

import labsim.model.LABSimModel;
import labsim.model.enums.Education;
import labsim.model.enums.Indicator;
import labsim.model.enums.Region;

/**
 * CLASS TO STORE FIXED PARAMETERS FOR INTERTEMPORAL OPTIMISATION DECISIONS
 */
public class Parameters {

    // CONTROLS FOR USER OPTIONS
    public static int options_employment1;                            // number of discrete employment alternatives to consider for principal earner
    public static int options_employment2;                            // number of discrete employment alternatives to consider for secondary earner
    public static int start_year;                                     // first year considered for simulation
    public static boolean flag_health;                                // user option to include health in state space for IO solution
    public static boolean flag_region;                                // user option to indicate region in state space for IO solution
    public static boolean flag_education;                             // user option to indicate student and education in state space for IO solution

    // TIME PARAMETERS
    public static final double FULLTIME_HOURS_WEEKLY = 35;            // hours per year associated with full time work
    public static final double LIVING_HOURS_WEEKLY = 16 * 7;          // total 'living' hours per year
    public static final double WEEKS_PER_YEAR = 52;                   // number of weeks per year

    // DIRECTORY AND STRUCTURE
    public static String grids_directory;                             // directory to read/write grids data

    // GAUSSIAN QUADRATURE
    public static final int PTS_IN_QUADRATURE = 5;                    // number of points used to approximate expectations for normally distributed wage expectations
    public static Quadrature quadrature = new Quadrature(Parameters.PTS_IN_QUADRATURE);

    // AGE
    public static int MAX_IO_AGE = 100;                               // evaluation of labsim.data.Parameters.getMaxAge() returned value 0
    public static final int MORTALITY_MAX_YEAR = 2068;                // maximum year supplied for mortality rates in "projections_mortality.xls"

    // ASSUMED YEARS IN RELATIONSHIP
    public static int DEFAULT_YEARS_MARRIED = 5;                      // used by regression equation to model likelihood of separation
    public static int DEFAULT_AGE_DIFFERENCE = 0;                     // used by regression equation to model likelihood of separation

    // CONSUMPTION PARAMETERS
    public static final double MIN_CONSUMPTION = 5 * 52;              // minimum feasible consumption per year

    // LIQUID WEALTH STATE
    public static final int PTS_LIQUID_WEALTH = 26;                   // number of discrete points used to approximate liquid wealth
    //public static final int PTS_LIQUID_WEALTH = 5;
    public static final int AGE_DEBT_DRAWDOWN = 55;                   // max debt limit reduced to zero in linear progression to max_age_debt
    public static final int MAX_AGE_DEBT = 65;                        // age at which all debt must be repaid
    public static final double MIN_LIQUID_WEALTH = -15000.0;          // lower bound of state-space
    public static final double MAX_LIQUID_WEALTH = 3000000.0;         // upper bound of state-space
    public static final double C_LIQUID_WEALTH = 20000.0;             // state-space summarised by logarithmic scale: w = exp(x) - c; larger c is closer to arithmetic scale
    public static final double R_SAFE_ASSETS = 0.034;                 // return to liquid wealth
    public static final double R_NET_DEBT = 0.084;                    // interest charge on net debt

    // WAGE POTENTIAL STATE
    public static final int MAX_AGE_EMPLOYMENT = 80;
    public static final int PTS_WAGE_POTENTIAL = 26;                  // number of discrete points used to approximate liquid wealth
    //public static final int PTS_WAGE_POTENTIAL = 5;
    public static final double MAX_WAGE_POTENTIAL = 250.0;            // maximum per hour
    public static final double MIN_WAGE_POTENTIAL = 3.0;              // minimum per hour
    public static final double C_WAGE_POTENTIAL = 1.0;                // log scale adjustment (see liquid wealth above)

    // FLAGS TO RECORD OPTIONS FOR EMPLOYMENT
    public static int MONTHS_EMPLOYED_PER_YEAR = 8;                   // used to impute employment history for each year over age 18 years
    public static boolean FLAG_WAGE_OFFER1 = false;                   // flag identifying whether to allow for wage offers in state space for principal earner
    public static double PROBABILITY_WAGE_OFFER1 = 0.97;              // probability of receiving a job offer (1 - probability of unemployment)
    public static boolean FLAG_WAGE_OFFER2 = false;                   // flag identifying whether to allow for wage offers in state space for secondary earner
    public static boolean FLAG_IO_EMPLOYMENT1 = true;                 // flag identifying whether to project employment of principal earner as an intertemporal optimisation decision
    public static boolean FLAG_IO_EMPLOYMENT2 = true;                 // flag identifying whether to project employment of secondary earner as an intertemporal optimisation decision

    // HEALTH STATE
    public static final int PTS_HEALTH = 6;                           // number of discrete points used to approximate health state
    public static final double MAX_HEALTH = 1.0;                      // maximum on indicator range
    public static final double MIN_HEALTH = 0.0;                      // minimum on indicator range
    public static final double DEFAULT_HEALTH = 1.0;                  // minimum on indicator range

    // BIRTH COHORTS
    public static int pts_birth_year;                                 // number of discrete points used to approximate birth years
    public static double max_birth_year;                              // maximum on indicator range
    public static double min_birth_year;                              // minimum on indicator range

    // RETIREMENT STATE
    public static final boolean FLAG_RETIREMENT = false;
    public static final int MIN_AGE_RETIREMENT = 55;

    // DISABILITY STATE
    public static Indicator DEFAULT_DISABILITY = Indicator.False;     // assumed for formulating expectations in absence of explicit value

    // MAXIMUM AGE FOR COHABITATION
    public static int MAX_AGE_COHABITATION = 100;

    // REGION STATE
    public static final int PTS_REGION = 12;                          // number of regions, starting at value 1
    public static final Region DEFAULT_REGION = Region.UKF;           // assumed for formulating expectations in absence of explicit value

    // STUDENT STATE
    public static final int MAX_AGE_STUDENT = 29;                     // maximum age of continuing education
    public static final int PTS_STUDENT = 2;                          // number of 'types' of student, starting at 0 for non-student
    public static final Education EDUCATION_FATHER = Education.Medium;
    public static final Education EDUCATION_MOTHER = Education.Medium;

    // EDUCATION STATE
    public static final int PTS_EDUCATION = 3;                        // number of 'types' of highest education qualification attained, starting at 0 for no education
    public static final Education DEFAULT_EDUCATION = Education.Medium;  // assumed for formulating expectations in absence of explicit value

    // CHILDREN STATE
    public static final int NUMBER_BIRTH_AGES = 3;                    // number of discrete ages at which a woman is assumed to be able to give
    public static final int[] BIRTH_AGE = new int[]{20, 29, 37};      // array listing discrete birth ages
    public static final int[] MAX_BIRTHS = new int[]{2, 2, 2};        // array listing the maximum number of births possible at each birth age
    public static final int MIN_FERTILITY_AGE = 16;                   // minimum age allowed for fertility
    public static final int MAX_FERTILITY_AGE = 45;                   // maximum age allowed for fertility
    public static final int FERTILITY_MAX_YEAR = 2043;                // maximum year supplied for mortality rates in "projections_mortality.xls"
}

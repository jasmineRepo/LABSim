package labsim.model.decisions;

import java.security.InvalidParameterException;

import labsim.model.enums.*;


/**
 *
 * CLASS TO DEFINE A GIVEN COMBINATION OF STATE VARIABLES
 *
 */
public class States {


    /**
     * ATTRIBUTES
     */
    int age_index;          // age index for state
    int age_years;          // age in years for state
    double[] states;        // vector to store combination of state variables (except age), in order as defined for axes in Grids
    GridScale scale;        // dimensional specifications of array used to store states

    /**
     * CONSTRUCTOR
     * @param age_index initialisation variable for age index, used to evaluate age_years
     */
    public States(GridScale scale, int age_index) {

        // initialise object attributes
        this.scale = scale;
        this.age_index = age_index;
        age_years = labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE + age_index;
        if (age_index==scale.sim_life_span) {
            states = new double[1];
        } else {
            states = new double[(int)(scale.grid_dimensions[age_index][4]+scale.grid_dimensions[age_index][5])];
        }
    }

    /**
     * CONSTRUCTOR TO COPY A States OBJECT
     */
    public States(States originalStates) {

        // initialise copy
        age_index = originalStates.age_index;
        age_years = originalStates.age_years;
        states = new double[originalStates.states.length];
        System.arraycopy(originalStates.states, 0, states, 0, originalStates.states.length);
        scale = originalStates.scale;
    }


    /*
     * WORKER METHODS
     */

    /**
     * METHOD TO EVALUATE AGE SPECIFIC INDEX FOR GIVEN SET OF STATE CHARACTERISTICS
     * IF STATE IS NOT PRECISELY ON GRID INDEX, THEN THE NEAREST LOWER INTEGER IS RETURNED
     * @return grid index
     */
    public long returnGridIndex() {

        // working variables
        final double eps = 5 * Math.ulp(1.0);
        long index;
        int counter_ii;
        double index_ii;

        // work through age-specific states
        index = 0;
        counter_ii = 1;
        for (int ii = 0; ii < (int)(scale.grid_dimensions[age_index][4] + scale.grid_dimensions[age_index][5]); ii++) {
            if (states[ii] > scale.axes[age_index][ii][2] + eps) {
                throw new InvalidParameterException("call to interpolate state in excess of grid maximum");
            } else if (states[ii] < scale.axes[age_index][ii][1] - eps) {
                throw new InvalidParameterException("call to interpolate state in excess of grid minimum");
            } else {
                index_ii = (states[ii] - scale.axes[age_index][ii][1]) /
                        (scale.axes[age_index][ii][2] - scale.axes[age_index][ii][1]) *
                        (scale.axes[age_index][ii][0] - 1);
                index += counter_ii * (long)(index_ii+eps);
            }
            counter_ii *= (int)scale.axes[age_index][ii][0];
        }

        // add age index
        index += scale.grid_dimensions[age_index][3];

        // return result
        return index;
    }

    /**
     * METHOD TO POPULATE STATE VALUES FOR OUTER GRID LOOP IN ManagerSolveGrids
     * @param ii_outer outer state index, based on grid axis structure - see Grids Constructor for related detail
     */
    public void populateOuterGridStates(int ii_outer) {

        // working variables
        int no_inner_states = (int)scale.grid_dimensions[age_index][4];
        int no_outer_states = (int)scale.grid_dimensions[age_index][5];
        double xx_min, xx_max, xx_step;
        int[] base = new int[no_outer_states];

        // evaluate base
        for (int ii = 0; ii < no_outer_states; ii++) {
            base[ii] = (int) Math.round(scale.axes[age_index][no_inner_states + ii][0]);
        }

        // evaluate counters
        int[] counters = counterEvaluate(ii_outer, base);

        // evaluate state values
        for (int ii = 0; ii < no_outer_states; ii++) {
            xx_min = scale.axes[age_index][no_inner_states + ii][1];
            xx_max = scale.axes[age_index][no_inner_states + ii][2];
            if (base[ii] > 1) {
                xx_step = (xx_max - xx_min) / (base[ii] - 1);
            } else {
                xx_step = 0;
            }
            states[no_inner_states + ii] = xx_min + xx_step * counters[ii];
        }
    }

    /**
     * METHOD TO POPULATE STATE VALUES FOR INNER GRID LOOP IN ManagerSolveGrids
     * @param ii_inner outer state index, based on grid axis structure - see Grids Constructor for related detail
     */
    public void populateInnerGridStates(int ii_inner) {

        // working variables
        int no_inner_states = (int)scale.grid_dimensions[age_index][4];
        double xx_min, xx_max, xx_step;
        int[] base = new int[no_inner_states];

        // evaluate base
        for (int ii = 0; ii < no_inner_states; ii++) {
            base[ii] = (int) Math.round(scale.axes[age_index][ii][0]);
        }

        // evaluate counters
        int[] counters = counterEvaluate(ii_inner, base);

        // evaluate state values
        for (int ii = 0; ii < no_inner_states; ii++) {
            xx_min = scale.axes[age_index][ii][1];
            xx_max = scale.axes[age_index][ii][2];
            if (base[ii] > 1) {
                xx_step = (xx_max - xx_min) / (base[ii] - 1);
            } else {
                xx_step = 0;
            }
            states[ii] = xx_min + xx_step * counters[ii];
        }
    }

    /**
     * METHOD TO SET VALUE OF WAGE OFFER2 AXIS (IF IT EXISTS)
     */
    public void setWageOffer2(double val) {

        // implement edit
        if (age_years <= Parameters.MAX_AGE_EMPLOYMENT && Parameters.FLAG_WAGE_OFFER2) {
            int dim_index = 0;                               // liquid wealth
            dim_index++;                                     // wage potential
            if (Parameters.flag_health) dim_index++;         // health status
            dim_index++;                                     // birth year
            if (Parameters.FLAG_WAGE_OFFER1) dim_index++;    // wage offer1
            dim_index++;                                     // wage offer2
            states[dim_index] = val;
        }
    }

    /**
     * METHOD TO SET VALUE OF WAGE OFFER1 AXIS (IF IT EXISTS)
     */
    public void setWageOffer1(double val) { states[scale.getIndex(Axis.WageOffer1, age_years)] = val; }

    /**
     * METHOD TO DISAGGREGATE REFERENCE INDEX INTO STATE SPECIFIC COUNTERS
     * @param index reference index
     * @param base  array defining the number of indices used to describe each state
     * @return integer array of state specific counters
     */
    static int[] counterEvaluate(int index, int[] base) {

        // initialise return
        int[] counters = new int[base.length];

        // evaluate counter
        int residual = index;
        for (int ii = 0; ii < base.length; ii++) {

            counters[ii] = residual % base[ii];
            residual = (residual - counters[ii]) / base[ii];
        }

        // return result
        return counters;
    }

    /**
     * METHOD TO VALIDATE WHETHER STATE COMBINATION SHOULD BE PASSED TO NUMERICAL OPTIMISATION ROUTINES
     * @return boolean, true if the state combination should be considered
     */
    boolean checkOuterStateCombination() {

        // initialise return
        boolean loop_consider = true;

        // check if prior to simulated period
        int year = getYear();
        if (year < Parameters.start_year) loop_consider = false;

        // evaluate return
        int wage_offer = getWageOffer();
        if (wage_offer == 0) {

            // skip if no wage offer is received, as numerical solution for this state combination is identical to
            // one of the labour options in the respective state combination where a wage offer is received
            loop_consider = false;
        }

        // return result
        return loop_consider;
    }

    /**
     * METHOD TO EXTRACT WAGE OFFER STATE FROM STATES ARRAY
     * @return the wage offer state if during working lifetime, and -1 otherwise
     */
    int getWageOffer() {
        int wage_offer;
        if (age_years <= Parameters.MAX_AGE_EMPLOYMENT && Parameters.FLAG_WAGE_OFFER1) {
            wage_offer = (int) Math.round(states[scale.getIndex(Axis.WageOffer1,age_years)]);
        } else {
            wage_offer = -1;
        }
        return wage_offer;
    }

    /**
     * METHOD TO EXTRACT COHABITATION STATE FROM STATES ARRAY
     * @return cohabitation status (0 single, 1 cohabitating)
     */
    int getCohabitation() {
        int cohabit;
        if (age_years <= Parameters.MAX_AGE_COHABITATION) {
            cohabit = (int)Math.round(states[scale.getIndex(Axis.Cohabitation,age_years)]);
        } else {
            cohabit = 0;
        }
        return cohabit;
    }

    /**
     * METHOD TO EXTRACT NUMBERS AND AGES OF DEPENDENT CHILDREN FROM STATES ARRAY
     * @return 2D integer array
     * first column reports (notional) age of children from respective birth age
     * second column reports number of dependent children in benefitUnit from respective birth age
     */
    int[][] getChildrenByAge() {

        // initialise return
        int[][] children = new int[Parameters.NUMBER_BIRTH_AGES][2];

        // evaluate return
        int dim_index = states.length - 3;
        for (int ii = Parameters.NUMBER_BIRTH_AGES - 1; ii >= 0; ii--) {
            children[ii][0] = age_years - Parameters.BIRTH_AGE[ii];
            if ((age_years >= Parameters.BIRTH_AGE[ii]) && (age_years < (Parameters.BIRTH_AGE[ii] + labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE))) {
                children[ii][1] = (int) Math.round(states[dim_index]);
                dim_index--;
            } else {
                children[ii][1] = 0;
            }
        }

        // return result
        return children;
    }

    /**
     * METHOD TO GET THE TOTAL NUMBER OF DEPENDENT CHILDREN IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildrenAll() {
        int number_children = 0;
        int[][] children = getChildrenByAge();
        for (int ii=0; ii<Parameters.NUMBER_BIRTH_AGES; ii++) {
            number_children += children[ii][1];
        }

        return number_children;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 3 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren02() {
        int[][] children = getChildrenByAge();
        int children02 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 0 && ints[0] <= 2) {
                children02 += ints[1];
            }
        }
        return children02;
    }

    /**
     * METHOD TO GET THE NUMBER OF DEPENDENT CHILDREN AGED UNDER 18 IN HOUSEHOLD
     * @return integer - number of all children
     */
    int getChildren017() {
        int[][] children = getChildrenByAge();
        int children02 = 0;
        for (int[] ints : children) {
            if (ints[0] >= 0 && ints[0] <= 17) {
                children02 += ints[1];
            }
        }
        return children02;
    }

    /**
     * METHOD TO RETURN YEAR IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getYear() { return age_years + (int)states[scale.getIndex(Axis.BirthYear,age_years)]; }

    /**
     * METHOD TO RETURN GENDER OF REFERENCE PERSON IMPLIED BY STATE COMBINATION
     * @return integer (0=male, 1=female)
     */
    int getGender() { return (int)states[scale.getIndex(Axis.Gender, age_years)]; }

    /**
     * METHOD TO RETURN GEOGRAPHIC REGION IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getRegion() { return (int)states[scale.getIndex(Axis.Region, age_years)]; }

    /**
     * METHOD TO RETURN DISABILITY STATUS IMPLIED BY STATE COMBINATION
     * @return integer (0 not disabled, 1 disabled)
     */
    int getDisability() { return (int)states[scale.getIndex(Axis.Disability, age_years)]; }

    /**
     * METHOD TO RETURN EDUCATION STATUS IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getEducation() { return (int)states[scale.getIndex(Axis.Education, age_years)]; }

    /**
     * METHOD TO RETURN EDUCATION STATUS IMPLIED BY STATE COMBINATION
     * @return integer
     */
    int getStudent() {
        int student = 0;
        if (age_years <= Parameters.MAX_AGE_STUDENT && Parameters.flag_education) {
            student = (int)states[scale.getIndex(Axis.Student, age_years)];
        }
        return student;
    }

    /**
     * METHOD TO RETURN EDUCATION STATUS IMPLIED BY STATE COMBINATION
     * @return integer
     */
    double getHealth() { return (int)states[scale.getIndex(Axis.Health, age_years)]; }

    /**
     * METHOD TO EVALUATE OECD SCALE FROM STATES ARRAY
     * @return the evaulated scale
     */
    double oecdEquivalenceScale() {

        // initialise return
        double scale = 1;

        // evaluate return
        int cohabitation = getCohabitation();
        if (cohabitation == 1) {
            scale += 0.5;
        }
        int[][] children = getChildrenByAge();
        for (int ii = 0; ii < Parameters.NUMBER_BIRTH_AGES; ii++) {
            if (children[ii][1] > 0) {
                if (children[ii][0] > 13) {
                    scale += 0.5 * (double) children[ii][1];
                } else {
                    scale += 0.3 * (double) children[ii][1];
                }
            }
        }

        // evaluate return
        return scale;
    }

    /**
     * METHOD TO IDENTIFY Gender enum CODE USED BY LABSim IMPLIED BY STATE COMBINATION
     * @return Gender
     */
    Gender getGenderCode() {
        Gender code;
        if (getGender()==0) {
            code = Gender.Male;
        } else {
            code = Gender.Female;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Region enum CODE USED BY LABSim IMPLIED BY STATE COMBINATION
     * @return Region
     */
    Region getRegionCode() {
        Region region_code = null;
        if (Parameters.flag_region) {
            int region_id = getRegion();
            for (Region code : Region.values()) {
                if (code.getDrgn1EUROMODvariable()==region_id) {
                    region_code = code;
                }
            }
        }
        if (region_code == null) {
            region_code = Parameters.DEFAULT_REGION;
        }
        return region_code;
    }

    /**
     * METHOD TO IDENTIFY Dhhtp_c4 enum CODE USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * Code describes benefitUnit type
     * @return Gender
     */
    Dhhtp_c4 getHouseholdTypeCode() {
        Dhhtp_c4 code;
        if (getCohabitation() == 0) {
            if (getChildrenAll() == 0) {
                code = Dhhtp_c4.SingleNoChildren;
            } else {
                code = Dhhtp_c4.SingleChildren;
            }
        } else {
            if (getChildrenAll() == 0) {
                code = Dhhtp_c4.CoupleNoChildren;
            } else {
                code = Dhhtp_c4.CoupleChildren;
            }
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Indicator enum CODE FOR DISABILITY USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Indicator
     */
    Indicator getDisabilityCode() {
        Indicator code;
        if (Parameters.flag_health) {
            if (getDisability()==0) {
                code = Indicator.False;
            } else {
                code = Indicator.True;
            }
        } else {
            code = Parameters.DEFAULT_DISABILITY;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Education enum CODE FOR EDUCATION STATUS USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Education
     */
    Education getEducationCode() {
        Education code;
        if (Parameters.flag_education) {
            int state = getEducation();
            if (state==Parameters.PTS_EDUCATION-1) {
                code = Education.High;
            } else {
                if (Parameters.PTS_EDUCATION==2) {
                    code = Parameters.DEFAULT_EDUCATION;
                } else {
                    if (state==1) {
                        code = Education.Medium;
                    } else {
                        code = Education.Low;
                    }
                }
            }
        } else {
            code = Parameters.DEFAULT_EDUCATION;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY Les_c4 enum CODE USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * Code describes whether reference person is long-term sick or disabled
     * @return Education
     */
    Les_c4 getLesCode(double employment) {
        Les_c4 code;
        int student = 0;
        if (Parameters.flag_education) {
            if (age_years <= Parameters.MAX_AGE_STUDENT) {
                student = getStudent();
            }
        }
        if (student > 0) {
            code = Les_c4.Student;
        } else if (employment<0.01) {
            code = Les_c4.NotEmployed;
        } else {
            code = Les_c4.EmployedOrSelfEmployed;
        }
        return code;
    }

    Lesdf_c4 getLesC4Code(double emp1, double emp2) {
        Lesdf_c4 code;
        if ( emp1 > 0 && emp2 > 0) code = Lesdf_c4.BothEmployed;
        else if ( emp1 > 0 ) code = Lesdf_c4.NotEmployedSpouseEmployed;
        else if ( emp2 > 0 ) code = Lesdf_c4.EmployedSpouseNotEmployed;
        else code = Lesdf_c4.BothNotEmployed;
        return code;
    }

    /**
     * METHOD TO IDENTIFY HEALTH MEASURE USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * @return double
     */
    double getHealthCode() {
        double code;
        if (Parameters.flag_health) {
            code = getHealth();
        } else {
            code = Parameters.DEFAULT_HEALTH;
        }
        return code;
    }

    /**
     * METHOD TO IDENTIFY HEALTH MEASURE USED BY LABSim AS IMPLIED BY STATE COMBINATION
     * @return double
     */
    Occupancy getOccupancyCode() {
        Occupancy code;
        if (getCohabitation()==1)
            code = Occupancy.Couple;
        else if (getGender()==0) {
            code = Occupancy.Single_Male;
        } else {
            code = Occupancy.Single_Female;
        }
        return code;
    }

    /**
     * METOD TO RETURN INDICATOR FOR CHILDREN UNDER 3 YEARS OLD
     * @return Indicator
     */
    Indicator getChildrenUnder3Indicator() {
        if (getChildren02() > 0) {
            return Indicator.True;
        } else {
            return Indicator.False;
        }
    }
}

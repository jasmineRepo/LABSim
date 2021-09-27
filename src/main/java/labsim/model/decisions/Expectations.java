package labsim.model.decisions;

import java.security.InvalidParameterException;
import java.util.Arrays;

import labsim.model.BenefitUnit;
import labsim.model.DonorHousehold;
import labsim.model.LABSimModel;
import labsim.model.Person;
import labsim.model.enums.*;
import org.apache.commons.collections4.keyvalue.MultiKey;


/**
 *
 * CLASS TO DEFINE STATE EXPECTATIONS FOR UTILITY MAXIMISATION
 *
 */
public class Expectations {


    /**
     * ATTRIBUTES
     */
    // current period characteristics
    States current_states;          // object describing prevailing state combination from which expectations are projected
    GridScale scale;                // GridScale object defining dimensionality of IO look-up table
    boolean cohabitation;           // true if current state defines cohabitating couple
    double equivalence_scale;       // scale to equivalise for benefitUnit demographics
    double wage_potential_hourly;   // hourly wage rate if working
    double liquid_wealth;           // wealth available to finance consumption
    double available_credit;        // maximum credit available
    double mortality_probability;   // probability of death

    // next period characteristics
    int age_index;                  // age index for state expectations
    int age_years;                  // age in years for expectations
    int number_expected;            // number of state combinations comprising expectations, conditional on survival
    double[] probability;           // vector recording probability associated with each anticipated state combination, conditional on survival
    States[] anticipated;           // vector to store anticipated state combinations

    // responses to controls
    double leisure_time;                // proportion of time spent in leisure
    double disposable_income_annual;    // disposable income
    double cash_on_hand;                // total value of pot that can be used to finance consumption within period

    /*
     * LABSIM OBJECTS FOR EVALUATING MODEL TAX AND BENEFIT AND REGRESSION FUNCTIONS
     *
     * LABSIM MODEL ATTRIBUTES (inherited from the model object used in the constructor)
     *   country
     *   year
     *
     * LABSIM HOUSEHOLD ATTRIBUTES IN NEXT PERIOD (same for all expectations)
     *   dhhtp_c4_lag1
     *   n_children_allAges_lag1
     *   n_children_02_lag1
     *   region
     *   ydses_c5_lag1
     *
     * LABSIM HOUSEHOLD ATTRIBUTES IN NEXT PERIOD (vary over expectations)
     *   n_children_017 (depends on evolving children)
     *   d_children_2under (depends on evolving children)
     *
     * LABSIM PERSON ATTRIBUTES IN NEXT PERIOD (same for all expectations)
     *   dag
     *   dgn
     *   dcpagdf_lag1
     *   dcpyy_lag1
     *   deh_c3_lag1
     *   dehf_c3
     *   dehm_c3
     *   dehsp_c3_lag1
     *   dhe_lag1
     *   dhesp_lag1
     *   dlltsd_lag1
     *   liwwh
     *
     * LABSIM PERSON ATTRIBUTES IN NEXT PERIOD (vary over expectations)
     *   dcpst (depends on evolving relationship status)
     *   dcpst_lag1 (depends on evolving relationship status)
     *   deh_c3 (depends on student transitions)
     *   les_c3_lag1 (depends on employment decision)
     *   lesdf_c4_lag1 (depends on employment decision)
     *   ynbcpdf_dv_lag1 (depends on employment decision)
     *   ypnbihs_dv_lag1 (depends on employment decision)
     *
     * LABSIM DONOR HOUSEHOLD IN CURRENT PERIOD
     *
     */
    // based on current period characteristics (used for evaluating current period tax and benefit payments)
    BenefitUnit current_benefitUnit;
    // based on succeeding period characteristics (used for evaluating state transition probabilities)
    labsim.model.LABSimModel model;   // model object for use when interacting with JAS-mine functions
    BenefitUnit benefitUnit; // benefitUnit object for use when interacting with JAS-mine functions
    labsim.model.Person person;       // person object for use when interacting with JAS-mine functions

    /**
     * CONSTRUCTOR TO POPULATE EXPECTATIONS THAT ARE INVARIANT TO AGENT DECISIONS
     * @param current_states States object for storage of current state combination
     */
    public Expectations(LABSimModel model, States current_states) {

        // prevailing characteristics - based on current_states
        this.current_states = current_states;
        scale = current_states.scale;
        cohabitation = current_states.getCohabitation() == 1;
        equivalence_scale = current_states.oecdEquivalenceScale();
        wage_potential_hourly = Parameters.MIN_WAGE_POTENTIAL;
        if (current_states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            int state_index = scale.getIndex(Axis.WagePotential, current_states.age_years);
            wage_potential_hourly = (Math.exp(current_states.states[state_index]) - Parameters.C_WAGE_POTENTIAL);
        }
        liquid_wealth = Math.exp(current_states.states[0]) - Parameters.C_LIQUID_WEALTH;
        if (current_states.age_years == Parameters.MAX_IO_AGE) {
            available_credit = 0;
            mortality_probability = 1;
        } else {
            available_credit = - (Math.exp(scale.axes[age_index][0][1]) - Parameters.C_LIQUID_WEALTH);
            if (current_states.getYear() < Parameters.MORTALITY_MAX_YEAR) {
                mortality_probability = ((Number) labsim.data.Parameters.getMortalityProjectionsByGenderAgeYear().
                        getValue(current_states.getGenderCode().toString(), current_states.age_years, current_states.getYear())).doubleValue() / 100000.0;
            } else {
                mortality_probability = ((Number) labsim.data.Parameters.getMortalityProjectionsByGenderAgeYear().
                        getValue(current_states.getGenderCode().toString(), current_states.age_years, Parameters.MORTALITY_MAX_YEAR)).doubleValue() / 100000.0;
            }
        }

        // prospective characteristics
        age_index = current_states.age_index + 1;
        age_years = current_states.age_years + 1;
        number_expected = 1;
        probability = new double[number_expected];
        anticipated = new States[number_expected];
        probability[0] = 1;
        anticipated[0] = new States(scale, age_index);
        if (age_years <= Parameters.MAX_IO_AGE) {

            // birth year
            int state_index_m1 = scale.getIndex(Axis.BirthYear,age_years-1);
            int state_index = scale.getIndex(Axis.BirthYear,age_years);
            for (int ii=0; ii<number_expected; ii++) {
                anticipated[ii].states[state_index] = current_states.states[state_index_m1];
            }

            //gender (1 = female)
            state_index_m1 = scale.getIndex(Axis.Gender, age_years-1);
            state_index = scale.getIndex(Axis.Gender, age_years);
            for (int ii=0; ii<number_expected; ii++) {
                anticipated[ii].states[state_index] = current_states.states[state_index_m1];
            }

            // region
            if (Parameters.flag_region) {
                state_index_m1 = scale.getIndex(Axis.Region, age_years-1);
                state_index = scale.getIndex(Axis.Region, age_years);
                for (int ii=0; ii<number_expected; ii++) {
                    anticipated[ii].states[state_index] = current_states.states[state_index_m1];
                }
            }
        }

        // benefitUnit characteristics for identifying donor used to evaluate tax and benefit payments in the current period
        // note slight inconsistency here, as the model object used here has year = current period + 1 (for expectations), but should be
        // set equal to the current period in this context.  Unsure how important this inconsistency will be in practice, but fixing the
        // issue would involve cloning the model object to represent both years
        this.current_benefitUnit = comparitorToIdDonorHousehold(model, current_states, cohabitation, age_years-1, wage_potential_hourly);

        // model object for interacting with regression models - represents states in the succeeding period
        this.model = model;
    }

    /**
     * CONSTRUCTOR TO COPY EXPECTATIONS OBJECT
     * @param invariant_expectations
     */
    public Expectations(Expectations invariant_expectations) {
        // prevailing characteristics - based on current_states
        current_states = invariant_expectations.current_states;
        scale = invariant_expectations.scale;
        cohabitation = invariant_expectations.cohabitation;
        equivalence_scale = invariant_expectations.equivalence_scale;
        wage_potential_hourly = invariant_expectations.wage_potential_hourly;
        liquid_wealth = invariant_expectations.liquid_wealth;
        available_credit = invariant_expectations.available_credit;
        mortality_probability = invariant_expectations.mortality_probability;

        // prospective characteristics
        age_index = invariant_expectations.age_index;
        age_years = invariant_expectations.age_years;
        number_expected = invariant_expectations.number_expected;
        probability = new double[number_expected];
        anticipated = new States[number_expected];
        for (int ii=0; ii<number_expected; ii++) {
            probability[ii] = invariant_expectations.probability[ii];
            anticipated[ii] = new States(invariant_expectations.anticipated[ii]);
        }

        // benefitUnit characteristics for identifying donor used to evaluate tax and benefit payments in the current period
        current_benefitUnit = invariant_expectations.current_benefitUnit;

        // model object for interacting with regression models - represents states in the succeeding period
        model = invariant_expectations.model;

        // benefitUnit object for interacting with regression models - represents states in the succeeding period
        benefitUnit = new BenefitUnit(model);
        benefitUnit.setRegion(current_states.getRegionCode());
        benefitUnit.setDhhtp_c4_lag1(current_states.getHouseholdTypeCode());
        benefitUnit.setYdses_c5_lag1(Ydses_c5.Q3);
        benefitUnit.setN_children_allAges_lag1(current_states.getChildrenAll());
        benefitUnit.setN_children_02_lag1(current_states.getChildren02());

        // person object for interacting with regression models - represents states in the succeeding period
        person = new labsim.model.Person(model, benefitUnit);
        person.setDgn(current_states.getGenderCode());
        person.setDlltsd_lag1(current_states.getDisabilityCode());
        person.setDhe_lag1(current_states.getHealthCode());
        person.setDeh_c3_lag1(current_states.getEducationCode());
        person.setDehf_c3(Parameters.EDUCATION_FATHER);
        person.setDehm_c3(Parameters.EDUCATION_MOTHER);
        person.setDag(age_years);
        person.setLiwwh(age_index * Parameters.MONTHS_EMPLOYED_PER_YEAR);
        person.setIoFlag(true);
        if (cohabitation) {
            person.setDehsp_c3_lag1(current_states.getEducationCode());
            person.setDhesp_lag1(Parameters.DEFAULT_HEALTH);
            person.setDcpyy_lag1(Parameters.DEFAULT_YEARS_MARRIED);
            person.setDcpagdf_lag1(Parameters.DEFAULT_AGE_DIFFERENCE);
        }
    }


    /*
     * WORKER METHODS
     */


    /**
     * METHOD TO UPDATE EXPECTATIONS FOR DISCRETE CONTROL VARIABLES
     * @param emp1_pr proportion of time reference adult spends in employment
     * @param emp2_pr proportion of time spouse spends in employment
     */
    public void updateForDiscreteControls(double emp1_pr, double emp2_pr) {

        // working variables
        int state_index, state_index_m1;


        //********************************************************
        // update current period variables for discrete decisions
        //********************************************************

        // labour and labour income in current period - for supply to tax and benefit function
        double labour_income1_weekly = 0;
        double labour_income2_weekly = 0;
        double labour_hours1_weekly = 0;
        double labour_hours2_weekly = 0;
        leisure_time = 1;
        if (current_states.age_years <= Parameters.MAX_AGE_EMPLOYMENT) {
            state_index = scale.getIndex(Axis.WagePotential,current_states.age_years);
            labour_hours1_weekly = Parameters.FULLTIME_HOURS_WEEKLY * emp1_pr;
            labour_income1_weekly = wage_potential_hourly * labour_hours1_weekly;
            if (cohabitation) {
                labour_hours2_weekly = Parameters.FULLTIME_HOURS_WEEKLY * emp2_pr;
                labour_income2_weekly = wage_potential_hourly * labour_hours2_weekly;
                leisure_time = 1 - (labour_hours1_weekly + labour_hours2_weekly) / (2.0 * Parameters.LIVING_HOURS_WEEKLY);
            } else {
                leisure_time = 1 - labour_hours1_weekly / Parameters.LIVING_HOURS_WEEKLY;
            }
        } else if (emp1_pr>1.0E-5 || emp2_pr>1.0E-5) {
            throw new InvalidParameterException("inconsistent labour decisions supplied for updating expectations");
        }

        // investment income / cost of debt
        double investment_income_annual;
        double investment_income1_annual;
        double investment_income2_annual;
        if (liquid_wealth < 0) {
            investment_income_annual = Parameters.R_NET_DEBT * liquid_wealth;
        } else {
            investment_income_annual = Parameters.R_SAFE_ASSETS * liquid_wealth;
        }
        if (cohabitation) {
            investment_income1_annual = investment_income_annual / 2;
            investment_income2_annual = investment_income1_annual;
        } else {
            investment_income1_annual = investment_income_annual;
            investment_income2_annual = 0.0;
        }

        // call to tax and benefit function
        disposable_income_annual = taxBenefitFunction(labour_hours1_weekly, labour_income1_weekly, investment_income1_annual, labour_hours2_weekly, labour_income2_weekly, investment_income2_annual);

        // cash on hand
        cash_on_hand = liquid_wealth + available_credit + disposable_income_annual;


        //********************************************************
        // update expectations for succeeding period
        //********************************************************
        if (age_years<=Parameters.MAX_IO_AGE) {

            // update objects for interaction with regression models
            person.setLes_c4_lag1(current_states.getLesCode(emp1_pr));
            person.setLesdf_c4_lag1(current_states.getLesC4Code(emp1_pr, emp2_pr));
            person.setYpnbihs_dv_lag1(labour_income1_weekly+investment_income1_annual);
            person.setYnbcpdf_dv_lag1(labour_income1_weekly+investment_income1_annual - labour_income2_weekly+investment_income2_annual);

            // student
            if (age_years <= Parameters.MAX_AGE_STUDENT && Parameters.flag_education) {
                state_index_m1 = scale.getIndex(Axis.Student, age_years - 1);
                state_index = scale.getIndex(Axis.Student, age_years);
                if (current_states.getStudent() == 0) {
                    person.setDeh_c3(current_states.getEducationCode());
                    for (int ii = 0; ii < number_expected; ii++) {
                        anticipated[ii].states[state_index] = current_states.states[state_index_m1];
                    }
                } else {
                    LocalExpectations lexpect = new LocalExpectations(person, ManagerRegressions.RegressionNames.EducationE1a);
                    expandExpectationsAllIndices(state_index, lexpect.probabilities, lexpect.values);
                }
            } else {
                person.setDeh_c3(current_states.getEducationCode());
            }

            // education
            if (Parameters.flag_education) {
                state_index_m1 = scale.getIndex(Axis.Education, age_years - 1);
                state_index = scale.getIndex(Axis.Education, age_years);
                if (current_states.getStudent() == 0) {
                    for (int ii = 0; ii < number_expected; ii++) {
                        anticipated[ii].states[state_index] = current_states.states[state_index_m1];
                    }
                } else {
                    // student in current period - allow for exit from education

                    // set-up probabilities and values
                    int anticipated_here = (int) scale.axes[age_index][state_index][0];
                    double[] probabilities = new double[anticipated_here];
                    double[] values = new double[anticipated_here];
                    double pp = labsim.data.Parameters.getRegEducationE2a().getProbitTransformOfScore(Education.High, person, Person.DoublesVariables.class);
                    probabilities[anticipated_here - 1] = pp;
                    values[anticipated_here - 1] = anticipated_here - 1;
                    if (anticipated_here == 2) {
                        probabilities[0] = (1 - pp);
                    } else {
                        pp = labsim.data.Parameters.getRegEducationE2a().getProbitTransformOfScore(Education.Low, person, Person.DoublesVariables.class);
                        probabilities[0] = pp;
                        values[0] = 0;
                        probabilities[1] = 1 - probabilities[0] - probabilities[2];
                        values[1] = 1;
                    }

                    // update expectations array
                    int number_expected_initial = number_expected;
                    for (int ii = 0; ii < number_expected_initial; ii++) {
                        if (anticipated[ii].getStudent() == 1) {
                            // continuing student
                            anticipated[ii].states[state_index] = current_states.states[state_index_m1];
                        } else {
                            // allow for exit from education
                            expandExpectationsSingleIndex(ii, state_index, probabilities, values);
                        }
                    }
                }
            }

            // health
            if (Parameters.flag_health) {

                // state indices
                state_index = scale.getIndex(Axis.Health, age_years);

                // populate expectations
                double score, rmse;
                double min_value = Parameters.MIN_HEALTH;
                double max_value = Parameters.MAX_HEALTH;
                if (current_states.getStudent()==0) {
                    // not currently student - both student status and education given

                    // set-up linear regression values
                    score = ManagerRegressions.getScore(person, ManagerRegressions.RegressionNames.HealthH1b);
                    rmse = ManagerRegressions.getRmse(ManagerRegressions.RegressionNames.HealthH1b);
                    LocalExpectations nonstudent = new LocalExpectations(score, rmse, min_value, max_value);
                    expandExpectationsAllIndices(state_index, nonstudent.probabilities, nonstudent.values);
                } else {
                    // current student - need to allow for graduation

                    // for continuing students
                    person.setDeh_c3(current_states.getEducationCode());
                    score = ManagerRegressions.getScore(person, ManagerRegressions.RegressionNames.HealthH1a);
                    rmse = ManagerRegressions.getRmse(ManagerRegressions.RegressionNames.HealthH1a);
                    LocalExpectations student = new LocalExpectations(score, rmse, min_value, max_value);

                    // for dis-continuing students
                    rmse = ManagerRegressions.getRmse(ManagerRegressions.RegressionNames.HealthH1b);

                    // begin loop over existing expectations
                    int number_expected_initial = number_expected;
                    for (int ii=0; ii<number_expected_initial; ii++) {
                        if (anticipated[ii].getStudent()==1) {
                            // continuing student
                            expandExpectationsSingleIndex(ii, state_index, student.probabilities, student.values);
                        } else {
                            // exit from education - allow for education change
                            person.setDeh_c3(anticipated[ii].getEducationCode());
                            score = ManagerRegressions.getScore(person, ManagerRegressions.RegressionNames.HealthH1b);
                            LocalExpectations nonstudent = new LocalExpectations(score, rmse, min_value, max_value);
                            expandExpectationsSingleIndex(ii, state_index, nonstudent.probabilities, nonstudent.values);
                        }
                    }
                }
           }

            // disability
            if (Parameters.flag_health) {
                state_index = scale.getIndex(Axis.Disability, age_years);
                indicatorExpectations(state_index, ManagerRegressions.RegressionNames.HealthH2b);
            }

            // cohabitation (1 = cohabitating)
            if (age_years <= Parameters.MAX_AGE_COHABITATION) {
                state_index = scale.getIndex(Axis.Cohabitation, age_years);
                if (cohabitation) {
                    indicatorExpectations(state_index, 0.0, 1.0, ManagerRegressions.RegressionNames.PartnershipU2b);
                } else {
                    indicatorExpectations(state_index, ManagerRegressions.RegressionNames.PartnershipU1b, ManagerRegressions.RegressionNames.PartnershipU1a);
                }
            }

            // dependent children
            state_index_m1 = scale.getIndex(Axis.Child, age_years-1);
            state_index = scale.getIndex(Axis.Child, age_years);
            for (int jj = 0; jj < Parameters.NUMBER_BIRTH_AGES; jj++) {
                if (age_years >= Parameters.BIRTH_AGE[jj] && age_years < (Parameters.BIRTH_AGE[jj] + labsim.data.Parameters.AGE_TO_BECOME_RESPONSIBLE)) {
                    if (age_years == Parameters.BIRTH_AGE[jj]) {
                        // age prior to birth age - number of children uncertain next period

                        int options = (int)scale.axes[age_index][state_index][0];

                        // begin loop over existing expectations
                        int number_expected_initial = number_expected;
                        for (int ii=0; ii<number_expected_initial; ii++) {

                            // update relationship status
                            if (anticipated[ii].getCohabitation()==1) {
                                if (cohabitation) {
                                    person.setDcpst_lag1(Dcpst.Partnered);
                                } else {
                                    person.setDcpst_lag1(Dcpst.SingleNeverMarried);
                                }
                            } else {
                                if (cohabitation) {
                                    person.setDcpst_lag1(Dcpst.PreviouslyPartnered);
                                } else {
                                    person.setDcpst_lag1(Dcpst.SingleNeverMarried);
                                }
                            }

                            // update education status
                            person.setDeh_c3(anticipated[ii].getEducationCode());

                            // expand expectations
                            if (anticipated[ii].getStudent()==1) {
                                expandExpectationsFertility(ii, state_index, jj, options, person, benefitUnit, ManagerRegressions.RegressionNames.FertilityF1a);
                            } else {
                                expandExpectationsFertility(ii, state_index, jj, options, person, benefitUnit, ManagerRegressions.RegressionNames.FertilityF1b);
                            }
                        }
                    } else {
                        for (int ii=0; ii<number_expected; ii++) {
                            anticipated[ii].states[state_index+jj] = current_states.states[state_index_m1+jj];
                        }
                    }
                    state_index_m1 += 1;
                    state_index += 1;
                }
            }

            // wage potential
            if (age_years <= Parameters.MAX_AGE_EMPLOYMENT) {

                // state indices
                state_index = scale.getIndex(Axis.WagePotential, age_years);

                double min_value = Parameters.MIN_WAGE_POTENTIAL;
                double max_value = Parameters.MAX_WAGE_POTENTIAL;
                int number_expected_initial = number_expected;
                for (int ii=0; ii<number_expected_initial; ii++) {

                    // update regression variables
                    benefitUnit.setN_children_017(anticipated[ii].getChildren017());
                    benefitUnit.setD_children_2under(anticipated[ii].getChildrenUnder3Indicator());
                    if (anticipated[ii].getCohabitation()==1) {
                        if (cohabitation) {
                            person.setDcpst(Dcpst.Partnered);
                        } else {
                            person.setDcpst(Dcpst.SingleNeverMarried);
                        }
                    } else {
                        if (cohabitation) {
                            person.setDcpst(Dcpst.PreviouslyPartnered);
                        } else {
                            person.setDcpst(Dcpst.SingleNeverMarried);
                        }
                    }
                    person.setDeh_c3(anticipated[ii].getEducationCode());

                    // update expectations
                    ManagerRegressions.RegressionNames regression;
                    if (current_states.getGenderCode()==Gender.Male) {
                        regression = ManagerRegressions.RegressionNames.WagesMales;
                    } else {
                        regression = ManagerRegressions.RegressionNames.WagesFemales;
                    }
                    double score = ManagerRegressions.getScore(person, regression);
                    double rmse = ManagerRegressions.getRmse(regression);
                    LocalExpectations lexpect = new LocalExpectations(score, rmse, min_value, max_value);
                    for (int jj=0; jj<lexpect.values.length; jj++) {
                        lexpect.values[jj] = Math.log(lexpect.values[jj] + Parameters.C_WAGE_POTENTIAL);
                    }
                    expandExpectationsSingleIndex(ii, state_index, lexpect.probabilities, lexpect.values);
                }
            }

            // wage offer
            if (age_years <= Parameters.MAX_AGE_EMPLOYMENT && Parameters.FLAG_WAGE_OFFER1) {
                LocalExpectations lexpect = new LocalExpectations(1.0, 0.0, Parameters.PROBABILITY_WAGE_OFFER1);
                expandExpectationsAllIndices(state_index, lexpect.probabilities, lexpect.values);
            }

            // check evaluated probabilities
            double prob_chk = 0;
            for (int ii=0; ii<number_expected; ii++) {
                prob_chk += probability[ii];
            }
            if (Math.abs(prob_chk-1) > 1.0E-5) {
                throw new InvalidParameterException("problem with probabilities supplied to outer expectations");
            }
        }
    }

    /**
     * METHOD TO GENERATE HOUSEHOLD OBJECT CHARACTERISTICS FOR IDENTIFYING EUROMOD DONOR
     * used to evaluate tax and benefit payments
     * @return HOUSEHOLD OBJECT
     */
    public BenefitUnit comparitorToIdDonorHousehold(labsim.model.LABSimModel model, States current_states,
                                                    boolean cohabitation, int age_years, double wage_potential_hourly) {

        // define benefitUnit for evaluation
        boolean ref_male = (current_states.getGenderCode()==Gender.Male);
        BenefitUnit benefitUnit = new BenefitUnit(model);
        benefitUnit.setRegion(current_states.getRegionCode());
        benefitUnit.setOccupancy(current_states.getOccupancyCode());
        benefitUnit.setN_children_allAges(current_states.getChildrenAll());
        labsim.model.Person male;
        labsim.model.Person female;
        if (cohabitation) {
            male = new labsim.model.Person();
            female = new labsim.model.Person();
            if (ref_male) {
                male.setDhe(current_states.getHealthCode());
                female.setDhe(Parameters.DEFAULT_HEALTH);
                male.setDlltsd(current_states.getDisabilityCode());
                female.setDlltsd(Parameters.DEFAULT_DISABILITY);
            } else {
                female.setDhe(current_states.getHealthCode());
                female.setDlltsd(current_states.getDisabilityCode());
                male.setDhe(Parameters.DEFAULT_HEALTH);
                male.setDlltsd(Parameters.DEFAULT_DISABILITY);
            }
            male.setDgn(Gender.Male);
            female.setDgn(Gender.Female);
            male.setDag(age_years-1);
            female.setDag(age_years-1);
            male.setPotentialEarnings(wage_potential_hourly);
            female.setPotentialEarnings(wage_potential_hourly);
            benefitUnit.setMale(male);
            benefitUnit.setFemale(female);
        } else {
            if (ref_male) {
                male = new labsim.model.Person();
                male.setDhe(current_states.getHealthCode());
                male.setDlltsd(current_states.getDisabilityCode());
                male.setDgn(Gender.Male);
                male.setDag(age_years-1);
                male.setPotentialEarnings(wage_potential_hourly);
                benefitUnit.setMale(male);
            } else {
                female = new labsim.model.Person();
                female.setDhe(current_states.getHealthCode());
                female.setDlltsd(current_states.getDisabilityCode());
                female.setDgn(Gender.Female);
                female.setDag(age_years-1);
                female.setPotentialEarnings(wage_potential_hourly);
                benefitUnit.setFemale(female);
            }
        }
        return benefitUnit;
    }

    /**
     * METHOD TO CALL TO TAX AND BENEFIT FUNCTION
     * @param labour_hours1_weekly labour hours per week of reference person
     * @param labour_income1_weekly labour income per week of reference person
     * @param investment_income1_annual investment income per year of reference person
     * @param labour_hours2_weekly labour hours per week of spouse (if exists, 0 otherwise)
     * @param labour_income2_weekly labour income per week of spouse (if exists, 0 otherwise)
     * @param investment_income2_annual investment income per year of spouse (if exists, 0 otherwise)
     * @return disposable income per annum of benefitUnit
     */
    public double taxBenefitFunction(double labour_hours1_weekly,
                                     double labour_income1_weekly,
                                     double investment_income1_annual,
                                     double labour_hours2_weekly,
                                     double labour_income2_weekly,
                                     double investment_income2_annual) {
        // define labour state
        Labour labourMale = null;
        Labour labourFemale = null;
        MultiKey<Labour> labourKey;
        if (current_states.getGenderCode()==Gender.Male) {
            labourMale = Labour.convertHoursToLabour((int)labour_hours1_weekly, Gender.Male);
            if (cohabitation) {
                labourFemale = Labour.convertHoursToLabour((int)labour_hours2_weekly, Gender.Female);
                labourKey = new MultiKey<>(labourMale, labourFemale);
            } else {
                labourKey = new MultiKey<>(labourMale, null);
            }
        } else {
            labourFemale = Labour.convertHoursToLabour((int)labour_hours1_weekly, Gender.Female);
            if (cohabitation) {
                labourMale = Labour.convertHoursToLabour((int)labour_hours2_weekly, Gender.Male);
                labourKey = new MultiKey<>(labourMale, labourFemale);
            } else {
                labourKey = new MultiKey<>(labourFemale, null);
            }
        }
        DonorHousehold donorHouse = current_benefitUnit.findMostSimilarEUROMODhouseholdForThisLabour(labourKey, false, true);
        if (donorHouse==null) {
            throw new InvalidParameterException("no donor benefitUnit found for state combination");
        }

        // evaluate gross income
        double gross_income_annual = (labour_income1_weekly + labour_income2_weekly) * Parameters.WEEKS_PER_YEAR +
                investment_income1_annual + investment_income2_annual;

        // evaluate disposable income
        double disposable_income_annual;
        if(donorHouse.getGrossEarnings() != 0.) {
            disposable_income_annual = donorHouse.getDisposableIncomeToGrossEarningsRatio() * gross_income_annual;
        }
        else {
            disposable_income_annual = donorHouse.getDisposableIncome() * 12;
        }

        // return
        return disposable_income_annual;
    }

    /**
     * METHOD TO EXPAND EXPECTATIONS ARRAYS ABOUT A TARGET INDEX
     * @param expand_index the index of the anticipated array taken as a starting point
     * @param state_index the state index of the characteristic populated using the quadrature
     * @param probabilities array of probabilities for expanding expectations
     * @param values array of values for expanding expectations
     */
    private void expandExpectationsSingleIndex(int expand_index, int state_index, double[] probabilities, double[] values) {

        // expand expectations array
        if (probabilities.length > 1) {
            probability = Arrays.copyOf(probability, number_expected+probabilities.length-1);
            anticipated = Arrays.copyOf(anticipated, number_expected+probabilities.length-1);
            for (int ii=0; ii<probabilities.length-1; ii++) {
                probability[number_expected+ii] = probability[expand_index];
                anticipated[number_expected+ii] = new States(anticipated[expand_index]);
            }
        }

        // update expectations arrays
        double prob_chk = 0.0;
        for (int ii=probabilities.length-1; ii>=0; ii--) {
            prob_chk += probabilities[ii];
            if (ii>0) {
                probability[number_expected-1+ii] = probability[number_expected-1+ii] * probabilities[ii];
                anticipated[number_expected-1+ii].states[state_index] = values[ii];
            } else {
                probability[expand_index] = probability[expand_index] * probabilities[ii];
                anticipated[expand_index].states[state_index] = values[ii];
            }
        }

        // check supplied probabilities
        if (Math.abs(prob_chk-1) > 1.0E-5) {
            throw new InvalidParameterException("problem with probabilities supplied to outer expectations");
        }

        // update indices
        number_expected = number_expected + probabilities.length - 1;
    }

    /**
     * METHOD TO EXPAND EXPECTATIONS ARRAYS TO ALLOW FOR FERTILITY BIRTH YEARS
     * @param expand_index the index of the anticipated array taken as a starting point
     * @param state_index the state index for the respective birth year
     * @param birth_year the current birth year
     * @param regression the regression equation used to update probabilities
     */
    private void expandExpectationsFertility(int expand_index, int state_index, int birth_year, int options, Person person, BenefitUnit benefitUnit, ManagerRegressions.RegressionNames regression) {

        // initialise storage arrays
        double[] probabilities = new double[options];
        double[] values = new double[options];
        for (int ii=0; ii<options; ii++) {
            if (ii==0) {
                probabilities[ii] = 1.0;
            } else {
                probabilities[ii] = 0.0;
            }
            values[ii] = ii;
        }

        // identify age pool for birth year
        int age0, age1;
        if (birth_year==0) {
            // youngest year - birth pool extends to minimum age of fertility
            age0 = Parameters.MIN_FERTILITY_AGE;
            age1 = (Parameters.BIRTH_AGE[birth_year] + Parameters.BIRTH_AGE[birth_year+1])/2;
        } else if (birth_year==Parameters.NUMBER_BIRTH_AGES-1) {
            // highest year - birth pool extends to maximum age of fertility
            age0 = (Parameters.BIRTH_AGE[birth_year-1] + Parameters.BIRTH_AGE[birth_year])/2;
            age1 = Parameters.MAX_FERTILITY_AGE;
        } else {
            age0 = (Parameters.BIRTH_AGE[birth_year-1] + Parameters.BIRTH_AGE[birth_year])/2;
            age1 = (Parameters.BIRTH_AGE[birth_year] + Parameters.BIRTH_AGE[birth_year+1])/2;
        }

        // evaluate probabilities
        int children_all = current_states.getChildrenAll();
        int children_02 = current_states.getChildren02();
        for (int age=age0; age<=age1; age++) {
            person.setDag(age);
            for (int ii=options-2; ii>=0; ii--) {
                // ii = number of births here
                int births_here_02 = Math.min(ii, 2);
                benefitUnit.setN_children_allAges_lag1(children_all + ii);
                benefitUnit.setN_children_allAges_lag1(children_02 + births_here_02);
                double proportion_births = ManagerRegressions.getProbability(person, regression);
                probabilities[ii+1] += probabilities[ii] * proportion_births;
                probabilities[ii] *= (1 - proportion_births);
            }
        }

        // expand expectations array
        expandExpectationsSingleIndex(expand_index, state_index, probabilities, values);

        // restore benefitUnit and person characteristics
        person.setDag(age_years);
        benefitUnit.setN_children_allAges_lag1(children_all);
        benefitUnit.setN_children_allAges_lag1(children_02);
    }

    /**
     * METHOD TO EXPAND EXPECTATIONS ARRAYS ABOUT ALL EXISTING INDICES
     * @param state_index the state index of the characteristic populated using the quadrature
     * @param probabilities array of probabilities for inclusion in expectations
     * @param values array of values for inclusion in expectations
     */
    private void expandExpectationsAllIndices(int state_index, double[] probabilities, double[] values) {
        int number_expected_initial = number_expected;
        for (int ii=0; ii<number_expected_initial; ii++) {
            expandExpectationsSingleIndex(ii, state_index, probabilities, values);
        }
    }

    /**
     * OVERLOADED VARIANTS OF METHOD TO LIMIT INPUTS
     */
    private void indicatorExpectations(int state_index, ManagerRegressions.RegressionNames regression) {
        indicatorExpectations(state_index, 1.0, 0.0, regression, regression);
    }
    private void indicatorExpectations(int state_index, double value_true, double value_false, ManagerRegressions.RegressionNames regression) {
        indicatorExpectations(state_index, value_true, value_false, regression, regression);
    }
    private void indicatorExpectations(int state_index, ManagerRegressions.RegressionNames regression1, ManagerRegressions.RegressionNames regression2) {
        indicatorExpectations(state_index, 1.0, 0.0, regression1, regression2);
    }

    /**
     * METHOD TO MANAGE UPDATING OF EXPECTATIONS FOR INDICATOR STATES BASED ON A SINGLE REGRESSION EQUATION
     * differs from over-loaded variant by allowing for different regression equations for students and non-students
     * @param state_index index of indicator state
     * @param regression1 regression equation used to project state for non-students
     * @param regression2 regression equation used to project state for students
     */
    private void indicatorExpectations(int state_index, double value_true, double value_false, ManagerRegressions.RegressionNames regression1, ManagerRegressions.RegressionNames regression2) {

        if (current_states.getStudent()==0) {
            LocalExpectations lexpect = new LocalExpectations(person, value_true, value_false, regression1);
            expandExpectationsAllIndices(state_index, lexpect.probabilities, lexpect.values);
        } else {
            person.setDeh_c3(current_states.getEducationCode());
            LocalExpectations student_expect = new LocalExpectations(person, value_true, value_false, regression2);
            int number_expected_initial = number_expected;
            for (int ii=0; ii<number_expected_initial; ii++) {
                if (anticipated[ii].getStudent()==1) {
                    // continuing student
                    expandExpectationsSingleIndex(ii, state_index, student_expect.probabilities, student_expect.values);
                } else {
                    // exit from education - allow for education change
                    person.setDeh_c3(anticipated[ii].getEducationCode());
                    LocalExpectations lexpect = new LocalExpectations(person, value_true, value_false, regression2);
                    expandExpectationsSingleIndex(ii, state_index, lexpect.probabilities, lexpect.values);
                }
            }
        }
    }
}

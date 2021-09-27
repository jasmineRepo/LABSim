// define package
package labsim.data;

// import Java packages
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// import plug-in packages
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

// import JAS-mine packages
import microsim.data.excel.ExcelAssistant;
import microsim.data.MultiKeyCoefficientMap;
import microsim.statistics.regression.LinearRegression;
import microsim.statistics.regression.LogitRegression;
import microsim.statistics.regression.MultiLogitRegression;
import microsim.statistics.regression.MultiProbitRegression;
import microsim.statistics.regression.ProbitRegression;
import microsim.statistics.regression.RegressionUtils;
import microsim.engine.SimulationEngine;

// import LABOURsim packages
import labsim.model.LABSimModel;
import labsim.model.enums.Country;
import labsim.model.enums.DemandAdjustment;
import labsim.model.enums.Education;
import labsim.model.enums.Gender;
import labsim.model.enums.Region;

import javax.persistence.criteria.CriteriaBuilder;


/**
 *
 * CLASS TO STORE MODEL PARAMETERS FOR GLOBAL ACCESS
 *
 */
public class Parameters {

	// EUROMOD variables


	// Insert here names of EUROMOD variables to use in Person and BenefitUnit tables of the input database
	// This is for the donor population, see below for initial population
	public static final String[] EUROMOD_VARIABLES_PERSON = new String[] {
			"idhh",					//id of household
			"idperson", 			//id of person
			"idfather", 			//id of father
			"idmother", 			//id of mother
			"idpartner", 			//id of partner
			"dag", 					//age
			"dct", 					//country
			"deh", 					//highest education level
			"dgn", 					//gender
			"dwt", 					//household weight
			"les", 					//labour employment status + health status
			"lcs", 					//labour civil servant dummy indicator
			"lhw", 					//hours worked per week
			"yem", 					//employment income
			"yse", 					//self-employment income
//			"tu_bu_uk_HeadID",		//id of the benefit unit UK RMK: Variable names are case sensitive and have to match EM output exactly.
//			"tu_fa_family_it_headid",		//id of the benefit unit IT RMK: Variable names are case sensitive and have to match EM output exactly.
			//Note: The two variables commented out above (tu_bu_uk_HeadID & tu_fa_family_it_headid) identify benefit units in the UK and Italy. Because the names depend on the country, these are defined in a countryBenefitUnitName map and
			//loaded into the donor population depending on the simulated country

			// data from EUROMOD output before tax / benefit transfers, so not affected by EUROMOD policy scenario (monthly time-scale).
			// We just use them calculated from EUROMOD output because EUROMOD has the correct way of aggregating each country's different component definitions
			//These have now been made policy-dependent because EM correctly uprates monetary values, and we want to account for the uprating
//			"ils_earns", 	// total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
//			"ils_origy"		// all gross income from labour, private pensions, investment income, property income, private transfers etc.
	};


	public static final String[] EUROMOD_VARIABLES_PERSON_INITIAL = new String[] {
			"idhh",					//id of household (can contain multiple benefit units)
			"idbenefitunit",		//id of a benefit unit
			"idperson", 			//id of person
			"idfather", 			//id of father
			"idmother", 			//id of mother
			"idpartner", 			//id of partner
			"dag", 					//age
			"dagsp",				//age of spouse
			"dct", 					//country
			"deh_c3", 				//highest education level
			"dehm_c3",				//highest education level of mother
			"dehf_c3",				//highest education level of father
			"dehsp_c3",				//highest education level of partner
			"ded",					//in education dummy
			"der",					//return to education dummy
			"dhe",					//health status
			"dhesp",				//health status of partner
			"dcpst",				//partnership status
			"dcpen",				//enter partnership dummy
			"dcpex",				//exit partnership dummy
			"dcpyy",				//years in partnership
			"dcpagdf",				//partners age difference
			"dnc02",				//number children aged 0-2
			"dnc",					//number children
			"ypnbihs_dv",			//gross personal non-benefit income
			"yptciihs_dv",			//gross personal non-employment non-benefit income
			"ypncp",  				//gross personal capital income
			"ypnoab",				//gross personal pension (public / occupational) income
			"yplgrs_dv",			//gross personal employment income
			"ynbcpdf_dv",			//difference partner income
			"dlltsd",				//long-term sick or disabled
			"sedex",				//year left education
			"ssscp",				//in same-sex partnership
			"sprfm",				//women in fertility range
			"sedag",				//in educational age range
			"stm",					//system variable - year
			"swv",					//system variable - wave
			"dgn", 					//gender
			"dwt", 					//household weight
			"les_c4", 				//labour employment status
			"lessp_c4",				//labour employment status of partner
			"lesdf_c4",				//own and partner employment status
			"lhw", 					//hours worked per week
			"adultchildflag",		//flag indicating adult child living at home in the data
			//"yem", 					//employment income
			//"yse", 					//self-employment income

			//From EUROMOD output data before tax / benefit transfers, so not affected by EUROMOD policy scenario (monthly time-scale).  We just use them calculated from EUROMOD output because EUROMOD has the correct way of aggregating each country's different component definitions
			//"ils_earns", 			//EUROMOD output variable:- total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
			//"ils_origy"			//EUROMOD output variable:- all gross income from labour, private pensions, investment income, property income, private transfers etc.
	};

	//Values potentially affected by EUROMOD policy scenario
	public static final String[] EUROMOD_POLICY_VARIABLES_PERSON = new String[] {
			"ils_sicer",			//EUROMOD output variable:- employer social insurance contributions
			//"ils_sicse",			//EUROMOD output variable:- self-employed social insurance contributions XXX: I don't think this is needed, as it is not added to earnings to get total labour cost in cases of self-employment.
			"ils_dispy",			//Disposable income : from EUROMOD output data after tax / benefit transfers (monthly time-scale)
			"ils_earns",			//EUROMOD output variable:- total labour earnings (employment + self-employment income + potentially other labour earnings like temporary employment, depending on country classification)
			"ils_origy"				//EUROMOD output variable:- all gross income from labour, private pensions, investment income, property income, private transfers etc.
	};

	//For BenefitUnit and DonorHousehold tables
	public static final String[] EUROMOD_VARIABLES_BENEFIT_UNIT = new String[] {
			"idperson",			//Use to handle non-uniqueness of household table rows
			"idhh",				//id of household 
			"idfather",			//id of father, to be used to define (responsible) male of the household (if defined)
			"idmother",			//id of mother, to be used to define (responsible) female of the household (if defined)
			"drgn1", 			//region (NUTS1)
			"dwt" 				//household weight
	};
	
	public static final String[] EUROMOD_VARIABLES_BENEFIT_UNIT_INITIAL = new String[] {
			"idperson",			//Use to handle non-uniqueness of household table rows
			"idhh",				//id of household (can contain multiple benefit units)
			"idbenefitunit",	//id of a benefit unit
			"idfather",			//id of father, to be used to define (responsible) male of the household (if defined)
			"idmother",			//id of mother, to be used to define (responsible) female of the household (if defined)
			"drgn1", 			//region (NUTS1)
			"dwt", 				//household weight
			"dhhtp_c4",			//household composition
			"ydses_c5"			//household income quantile
	};

	public static final String[] EUROMOD_VARIABLES_HOUSEHOLD_INITIAL = new String[] {
			"idperson",			//Use to handle non-uniqueness of household table rows
			"idhh",				//id of household (can contain multiple benefit units)
			"idbenefitunit",	//id of a benefit unit
			"dwt" 				//household weight
	};

	//Labour Demand/Supply Convergence parameters
	public static final double INITIAL_DECAY_FACTOR = 1.;	//Initialisation of field that is used to check whether labour market equilibrium is progressing fast enough (is multiplied by a factor at each iteration when needed, so the factor gets exponentially smaller)
	public static final DemandAdjustment demandAdjustment = DemandAdjustment.PopulationGrowth;		//Choose the method by which we derive the factor applied to the converged labour demand in order to get the initial labour demand at the following timestep 
	public static final double ETA = 0.2;		//(eta in Matteo's document).  If excess labour demand is greater than this value (0.1 == 10%), then iterate convergence procedure again
	public static final double CHI_minus = 0.1;		//Lambda in Matteo's document is adjusted by reducing by lambda -> lambda * (1 - CHI_minus), or increasing lambda by lambda -> lambda * (1 + CHI_plus)
	public static final double CHI_plus = 0.1;		//Lambda in Matteo's document is adjusted by reducing by lambda -> lambda * (1 - CHI_minus), or increasing lambda by lambda -> lambda * (1 + CHI_plus)
	public static final Map<Education, Double> adjustmentFactorByEducation;	//Lambda_s in Matteo's document, where s is the education level
	public static final double initialLambda = ETA/5.;	//Ensure adjustment is smaller than relative excess labour demand threshold
	public static final int MinimumIterationsBeforeTestingConvergenceCriteria = 20;	//Run this number of iterations to accumulate estimates of (aggregate) labour supply (cross) elasticities before testing the convergence criterion (i.e. the norm of (supply * demand elasticities) matrix < 1)
	public static final int MaxConvergenceAttempts = 2 * MinimumIterationsBeforeTestingConvergenceCriteria;		//Allow the equilibrium convergence criterion to fail the test this number of times before potentially terminating the simulation.
	public static final double RateOfConvergenceFactor = 0.9;


	static {
		adjustmentFactorByEducation = new LinkedHashMap<Education, Double>();		//Initialise adjustment factor to the same value for all education levels
		for (Education edu: Education.values()) {
			adjustmentFactorByEducation.put(edu, initialLambda);
		}
    }
	
	public static void resetAdjustmentFactorByEducation() {
		for(Education edu: Education.values()) {
			adjustmentFactorByEducation.put(edu, initialLambda);
		}
	}
	
	public static final double childrenNumDiscrepancyConstraint(double numberOfChildren) {
		if(numberOfChildren <= 1) {
			return 0.;
		}
		else if(numberOfChildren <= 3) {
			return 1.;
		}
		else if(numberOfChildren <= 5) {
			return 2.;
		}
		else {
			return 3.;
		}
	}
	public static final double AgeDiscrepancyConstraint = 10;	//Difference of age must equal of be below this value (so we allow 10 year cumulated age difference)
	public static final TreeSet<Double> EarningsDiscrepancyConstraint = new TreeSet<>(Arrays.asList(0.01, 0.02, 0.03, 0.04, 0.05));	//Proportional difference
	
	//Initial matching differential bounds - the initial bounds that a match must satisfy, before being relaxed
	public static final double UNMATCHED_TOLERANCE_THRESHOLD = 0.1;		//Smallest proportion of a gender left unmatched (we take the minimum of the male proportion and female proportions).  If there are more than this, we will relax the constraints (e.g. the bounds on age difference and potential earnings difference) until this target has been reached
	public static final int MAXIMUM_ATTEMPTS_MATCHING = 10;
	public static final double RELAXATION_FACTOR = 1.5;
	public static final int AGE_DIFFERENCE_INITIAL_BOUND = 5;
	public static final double POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND = 5.;
	
	public static final double WEEKS_PER_MONTH_RATIO = 365.25/(7.*12.);	// = 4.348214286
	public static final int HOURS_IN_WEEK = 24 * 7; //This is used to calculate leisure in labour supply
	//Is it possible for people to start going to the labour module (e.g. age 17) while they are living with parents (until age 18)?
	//Cannot see how its possible if it is the household that decides how much labour to supply.  If someone finishes school at 17, they need to leave home before they can enter the labour market.  So set age for finishing school and leaving home to 18.
	public static final int AGE_TO_BECOME_RESPONSIBLE = 18;			// Min age a person can leave parental home and set-up new house (potentially with partner).
	public static final int MIN_AGE_TO_LEAVE_EDUCATION = 16; // AGE_TO_LEAVE_HOME;				// Minimum age for a person to consider leaving education - note that this doesn't mean the person WILL definitely leave education
	public static final int MAX_AGE_TO_ENTER_EDUCATION = 45;
	public static final int MIN_AGE_COHABITATION = AGE_TO_BECOME_RESPONSIBLE;  	// Min age a person can marry
	public static final int MIN_AGE_TO_ALIGN_HEALTH = AGE_TO_BECOME_RESPONSIBLE;
	public static final int MIN_AGE_TO_HAVE_INCOME = 16; //Minimum age to have non-employment non-benefit income
	public static final int MIN_AGE_TO_RETIRE = 50; //Minimum age to consider retirement
	public static final int MIN_AGE_FLEXIBLE_LABOUR_SUPPLY = 16; //Used when filtering people who can be "flexible in labour supply"
	public static final int MAX_AGE_FLEXIBLE_LABOUR_SUPPLY = 75;
	public static final int MIN_DIFFERENCE_AGE_MOTHER_CHILD_IN_ALIGNMENT = 15; //When assigning chidlren to mothers in the population alignment, specify how much older (at the minimum) the mother must be than the child

	//Initial value for the savings rate and multiplier for capital income:
	public static double SAVINGS_RATE; //This is set in the country-specific part of this file

	//public static int MAX_AGE_IN_EDUCATION;// = MAX_AGE;//30;			// Max age a person can stay in education	//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.
	//public static int MAX_AGE_MARRIAGE;// = MAX_AGE;//75;  			// Max age a person can marry		//Cannot set here, as MAX_AGE is not known yet.  Now set to MAX_AGE in buildObjects in Model class.

	public static final int MIN_AGE = 0;		//We now include new born babies in the simulation
	private static int max_age;		//Could define this explicitly here, however we now obtain this value from the maximum Age specified in the align_popProjections.xlsx (which corresponds to a mortality rate of 1).  This takes place in the loadParameters() method.
	private static final int MIN_YEAR = 2011;		//Check this matches with align_popProjections.xlsx
	private static final int MAX_YEAR = 2060;		//Check this matches with align_popProjections.xlsx
	public static final int MIN_AGE_MATERNITY = 18;  			// Min age a person can give birth
	public static final int MAX_AGE_MATERNITY = 44;  			// Max age a person can give birth

	public static final int BASE_PRICE_YEAR = 2015; //Base price year for inflation-adjustment of EM donor population's monetary variables

	public static double PROB_NEWBORN_IS_MALE = 0.5;
	
	public static final boolean systemOut = true;
	
	//Bootstrap all the regression coefficients if true, or only the female labour participation regressions when false
	public static final boolean bootstrapAll = false;
	
	//Scheduling
	public static final int MODEL_ORDERING = 0;
	public static final int COLLECTOR_ORDERING = 1; //-2
	public static final int OBSERVER_ORDERING = 2; //-1

	//Initialise values specifying domain of original sick probability curves
	public static int femaleMinAgeSick = Integer.MAX_VALUE;
	public static int maleMinAgeSick = Integer.MAX_VALUE;
	public static int femaleMaxAgeSick = Integer.MIN_VALUE;
	public static int maleMaxAgeSick = Integer.MIN_VALUE;
	
	//For use with EUROMOD and h2 input database construction
	public static final String WORKING_DIRECTORY = System.getProperty("user.dir");
	public static final String INPUT_DIRECTORY = WORKING_DIRECTORY + File.separator + "input" + File.separator;
	public static final String INPUT_DIRECTORY_INITIAL_POPULATIONS = INPUT_DIRECTORY + "InitialPopulations" + File.separator; //Path to directory containing initial population for each year
	public static final String EUROMOD_OUTPUT_DIRECTORY = INPUT_DIRECTORY + "EUROMODoutput" + File.separator;
	public static final String EUROMODpolicyScheduleFilename = "EUROMODpolicySchedule";
	public static final String DatabaseCountryYearFilename = "DatabaseCountryYear";
	
	//Headings in Excel file of EUROMOD policy scenarios
	public static final String EUROMODpolicyScheduleHeadingFilename = "Filename";
	public static final String EUROMODpolicyScheduleHeadingScenarioYearBegins = "Policy_Start_Year";
	public static final String EUROMODpolicySchedulePlanHeadingDescription = "Description";
	//Names of donor attributes that depend on EUROMOD policy parameters
	public static final String DISPOSABLE_INCOME_VARIABLE_NAME = "DISPOSABLE_INCOME_MONTHLY";
	public static final String EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME = "EMPLOYER_SOCIAL_INSURANCE_CONTRIBUTION_PER_HOUR";
	public static final String GROSS_EARNINGS_VARIABLE_NAME = "GROSS_EARNINGS_MONTHLY";
	public static final String ORIGINAL_INCOME_VARIABLE_NAME = "ORIGINAL_INCOME_MONTHLY";
	public static final String HOURLY_WAGE_VARIABLE_NAME = "HOURLY_WAGE";
	//public static final String SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME = "SELF_EMPLOY_SOC_INSUR_CONTR_PER_HOUR";
	public static final String HOURS_WORKED_WEEKLY = "HOURS_WORKED_WEEKLY";
	public static final String BENEFIT_UNIT_VARIABLE_NAME = "BU_ID";

	private static String inputFileName;
	private static String initialInputFileName;
	private static MultiKeyMap<Object, Double> populationGrowthRatiosByRegionYear; 


	/////////////////////////////////////////////////////////////////// INITIALISATION OF DATA STRUCTURES //////////////////////////////////
	public static Map<Integer, String> EUROMODpolicySchedule = new TreeMap<Integer, String>();
	private static MultiKeyMap<Object, Double> fertilityRateByRegionYear;
	private static MultiKeyCoefficientMap populationProjections;
	private static MultiKeyCoefficientMap benefitUnitVariableNames;

	//CPI
	private static MultiKeyCoefficientMap yearCPI;
	public static Map<Integer, Double> deflatorCPIMap = new TreeMap<Integer, Double>();


	//Education level projections
	private static MultiKeyCoefficientMap projectionsHighEdu;			//Alignment projections for High Education
	private static MultiKeyCoefficientMap projectionsLowEdu;			//Alignment projections for Medium Education

	//Student share projections for alignment
	private static MultiKeyCoefficientMap studentShareProjections;		//Alignment projections for Student share of population

	//Employment alignment targets
	private static MultiKeyCoefficientMap employmentAlignment;

	//For marriage types:
	private static MultiKeyCoefficientMap marriageTypesFrequency;
	private static Map<Gender, MultiKeyMap<Region, Double>> marriageTypesFrequencyByGenderAndRegion;

	//Mean and covariances for parametric matching
	private static MultiKeyCoefficientMap meanCovarianceParametricMatching;

	private static MultiKeyCoefficientMap fixedRetireAge;
//	private static MultiKeyCoefficientMap rawProbSick;
	private static MultiKeyCoefficientMap unemploymentRates;
//	private static MultiKeyMap probSick;

	//MultivariateNormalDistribution of age and potential earnings differential to use in the parametric partnership process
	private static MultivariateNormalDistribution wageAndAgeDifferentialMultivariateNormalDistribution;


	//Mortality and fertility tables for the intertemporal optimisation model
	private static MultiKeyCoefficientMap mortalityProjectionsByGenderAgeYear; //Load as MultiKeyCoefficientMap as all values are in the Excel file and just need to be accessible
	private static MultiKeyCoefficientMap fertilityProjectionsByYear; //NB: these currently only go up to 2043?


	/////////////////////////////////////////////////////////////////// REGRESSION COEFFICIENTS //////////////////////////////////////////

	//Health
	private static MultiKeyCoefficientMap coeffCovarianceHealthH1a; 
	private static MultiKeyCoefficientMap coeffCovarianceHealthH1b;
	private static MultiKeyCoefficientMap coeffCovarianceHealthH2b; //Prob. long-term sick or disabled
	
	//Education
	private static MultiKeyCoefficientMap coeffCovarianceEducationE1a; 
	private static MultiKeyCoefficientMap coeffCovarianceEducationE1b;
	private static MultiKeyCoefficientMap coeffEducationE2aHigh; //Coefficients for E2a process High outcome of multiprobit
	private static MultiKeyCoefficientMap coeffEducationE2aLow; //Coefficients for E2a process Low outcome of multiprobit (medium is the default)
	
	//Partnership 
	private static MultiKeyCoefficientMap coeffCovariancePartnershipU1a; //Probit enter partnership if in continuous education 
	private static MultiKeyCoefficientMap coeffCovariancePartnershipU1b; //Probit enter partnership if not in continuous education
	private static MultiKeyCoefficientMap coeffCovariancePartnershipU2b; //Probit exit partnership (females)

	//Partnership for Italy
	private static MultiKeyCoefficientMap coeffCovariancePartnershipITU1; //Probit enter partnership for Italy
	private static MultiKeyCoefficientMap coeffCovariancePartnershipITU2; //Probit exit partnership for Italy
	
	//Fertility
	private static MultiKeyCoefficientMap coeffCovarianceFertilityF1a; //Probit fertility if in continuous education
	private static MultiKeyCoefficientMap coeffCovarianceFertilityF1b; //Probit fertility if not in continuous education

	//Fertility for Italy
	private static MultiKeyCoefficientMap coeffCovarianceFertilityF1; //Probit fertility for Italy

	//Income 
	private static MultiKeyCoefficientMap coeffCovarianceIncomeI1a; //Linear regression non-employment non-benefit income if in continuous education
	private static MultiKeyCoefficientMap coeffCovarianceIncomeI1b; //Linear regression non-employment non-benefit income if not in continuous education
	private static MultiKeyCoefficientMap coeffCovarianceIncomeI3a; //Capital income if in continuous education
	private static MultiKeyCoefficientMap coeffCovarianceIncomeI3b; //Capital income if not in continuous education
	private static MultiKeyCoefficientMap coeffCovarianceIncomeI3c; //Pension income for those aged over 50 who are not in continuous education

	//Wages
	private static MultiKeyCoefficientMap coeffCovarianceWagesMales;
	private static MultiKeyCoefficientMap coeffCovarianceWagesFemales;

	//Labour Market
	private static MultiKeyCoefficientMap coeffCovarianceEmploymentSelectionMales;
	private static MultiKeyCoefficientMap coeffCovarianceEmploymentSelectionFemales;	
	
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityMales;
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityFemales;
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityMalesWithDependent; //For use with couples where only male is flexible in labour supply (so has a dependent)
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityFemalesWithDependent;
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACMales; //Adult children, male
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityACFemales; //Adult children, female
	private static MultiKeyCoefficientMap coeffLabourSupplyUtilityCouples;

	//Leaving parental home
	private static  MultiKeyCoefficientMap coeffCovarianceLeaveHomeP1a;

	//Retirement
	private static MultiKeyCoefficientMap coeffCovarianceRetirementR1a;
	private static MultiKeyCoefficientMap coeffCovarianceRetirementR1b;

	///////////////////////////////////////////////////////////STATISTICS FOR VALIDATION/////////////////////////////////////////////
	//Share of students by age
	private static MultiKeyCoefficientMap validationStudentsByAge;

	//Share of students by region
	private static MultiKeyCoefficientMap validationStudentsByRegion;

	//Education level of over 17 year olds
	private static MultiKeyCoefficientMap validationEducationLevel;

	//Education level by age group
	private static MultiKeyCoefficientMap validationEducationLevelByAge;

	//Education level by region
	private static MultiKeyCoefficientMap validationEducationLevelByRegion;

	//Share of couple by region
	private static MultiKeyCoefficientMap validationPartneredShareByRegion;

	//Share of disabled by age
	private static MultiKeyCoefficientMap validationDisabledByAge;

	private static MultiKeyCoefficientMap validationDisabledByGender;

	//Health by age
	private static MultiKeyCoefficientMap validationHealthByAge;

	//Employment by gender
	private static MultiKeyCoefficientMap validationEmploymentByGender;

	//Employment by gender and age
	private static MultiKeyCoefficientMap validationEmploymentByAgeAndGender;

	//Employment by maternity
	private static MultiKeyCoefficientMap validationEmploymentByMaternity;

	//Employment by gender and region
	private static MultiKeyCoefficientMap validationEmploymentByGenderAndRegion;

	private static MultiKeyCoefficientMap validationLabourSupplyByEducation;

	//Activity status
	private static MultiKeyCoefficientMap validationActivityStatus;

	/////////////////////////////////////////////////////////////////// REGRESSION OBJECTS //////////////////////////////////////////

	//Health
	private static LinearRegression regHealthH1a;
	private static LinearRegression regHealthH1b;
	private static ProbitRegression regHealthH2b;
	
	//Education
	private static ProbitRegression regEducationE1a; 
	private static ProbitRegression regEducationE1b; 
	private static MultiProbitRegression<Education> regEducationE2a; 
	
	//Partnership
	private static ProbitRegression regPartnershipU1a;
	private static ProbitRegression regPartnershipU1b;
	private static ProbitRegression regPartnershipU2b;

	private static ProbitRegression regPartnershipITU1;
	private static ProbitRegression regPartnershipITU2;
	
	//Fertility
	private static ProbitRegression regFertilityF1a; 
	private static ProbitRegression regFertilityF1b; 

	private static ProbitRegression regFertilityF1;

	//Income
	private static LinearRegression regIncomeI1a;
	private static LinearRegression regIncomeI1b;
	private static LinearRegression regIncomeI3a;
	private static LinearRegression regIncomeI3b;
	private static LinearRegression regIncomeI3c;
	
	//New regressions from Lia
	private static LogitRegression regSchooling;
	private static MultiLogitRegression<Education> regEducationLevel;
	
	//New simple educ level
	private static MultiLogitRegression<Education> regSimpleEducLevel;
	
	//For Labour market
	private static LinearRegression regWagesMales;
	private static LinearRegression regWagesFemales;
	
	private static LinearRegression regEmploymentSelectionMale;		//To calculate Inverse Mills Ratio for Heckman Two-Step Procedure
	private static LinearRegression regEmploymentSelectionFemale;	//To calculate Inverse Mills Ratio for Heckman Two-Step Procedure
	private static NormalDistribution standardNormalDistribution;	//To sample the inverse mills ratio
	
	private static LinearRegression regLabourSupplyUtilityMales;
	private static LinearRegression regLabourSupplyUtilityFemales;

	private static LinearRegression regLabourSupplyUtilityMalesWithDependent;
	private static LinearRegression regLabourSupplyUtilityFemalesWithDependent;

	private static LinearRegression regLabourSupplyUtilityACMales;
	private static LinearRegression regLabourSupplyUtilityACFemales;
	private static LinearRegression regLabourSupplyUtilityCouples;

	//Leaving parental home
	private static ProbitRegression regLeaveHomeP1a;

	//Retirement
	private static ProbitRegression regRetirementR1a;
	private static ProbitRegression regRetirementR1b;

	private static ProbitRegression regBirthFemales;
	private static ProbitRegression regUnionFemales;
	private static Set<Region> countryRegions;
	private static Map<Region, Double> unemploymentRatesByRegion;


	/**
	 *
	 * METHOD TO LOAD PARAMETERS FOR GIVEN COUNTRY
	 * @param country
	 *
	 */
	public static void loadParameters(Country country) {

		EUROMODpolicySchedule = calculateEUROMODpolicySchedule(country);		
		inputFileName = "population_" + country;
		initialInputFileName = "population_" + country + "_initial"; 
		setCountryRegions(country);
		String countryString = country.toString();


		// scenario parameters
		if (country.equals(Country.IT)) {
			SAVINGS_RATE = 0.056;
		} else {
			SAVINGS_RATE = 0.056;
		}


//		unemploymentRatesByRegion = new LinkedHashMap<>();
//		unemploymentRates = ExcelAssistant.loadCoefficientMap("input/scenario_unemploymentRates.xlsx", countryString, 1, 46);
		fixedRetireAge = ExcelAssistant.loadCoefficientMap("input/scenario_retirementAgeFixed.xlsx", countryString, 1, 2);
		/*
		rawProbSick = ExcelAssistant.loadCoefficientMap("input/scenario_probSick.xls", country.toString(), 2, 1);
		for (Object o: rawProbSick.keySet()) {
			MultiKey mk = (MultiKey)o;
			int age = ((Number)mk.getKey(1)).intValue();
			if (((String)mk.getKey(0)).equals(Gender.Female.toString())) {
				if (age > femaleMaxAgeSick) {
					femaleMaxAgeSick = age;
//				} else if(age < femaleMinAgeSick) {
//					femaleMinAgeSick = age;
				}
			} else {
				// gender in multikey must be male
				if (age > maleMaxAgeSick) {
					maleMaxAgeSick = age;
//				} else if(age < maleMinAgeSick) {
//					maleMinAgeSick = age;
				}
			}
		}

		probSick = new MultiKeyMap();
		*/

		// alignment parameters
		populationProjections = ExcelAssistant.loadCoefficientMap("input/align_popProjections.xlsx", countryString, 3, 50);

		//Alignment of education levels
		projectionsHighEdu = ExcelAssistant.loadCoefficientMap("input/align_educLevel.xlsx", countryString + "_High", 1, 2);
		projectionsLowEdu = ExcelAssistant.loadCoefficientMap("input/align_educLevel.xlsx", countryString + "_Low", 1, 2);

		studentShareProjections = ExcelAssistant.loadCoefficientMap("input/align_student_under30.xlsx", countryString, 1, 40);

		//Employment alignment
		employmentAlignment = ExcelAssistant.loadCoefficientMap("input/align_employment.xlsx", countryString, 2, 40);

		//Marriage types frequencies:
		marriageTypesFrequency = ExcelAssistant.loadCoefficientMap("input/marriageTypes2.xlsx", countryString, 2, 1);
		marriageTypesFrequencyByGenderAndRegion = new LinkedHashMap<Gender, MultiKeyMap<Region, Double>>();	//Create a map of maps to store the frequencies

		//Mortality rates:
		mortalityProjectionsByGenderAgeYear = ExcelAssistant.loadCoefficientMap("input/projections_mortality.xlsx", countryString + "_MortalityByGenderAgeYear", 2, 59);
		//Fertility rates:
		fertilityProjectionsByYear = ExcelAssistant.loadCoefficientMap("input/projections_fertility.xlsx", countryString + "_FertilityByYear", 1, 71);

		//CPI
		yearCPI = ExcelAssistant.loadCoefficientMap("input/scenario_CPI.xlsx", countryString, 1, 1);


		for (Object key: yearCPI.keySet()) {
			int year = key.hashCode();
//			int startYear = ((LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName())).getStartYear();

			double CPICurrentYear = ((Number) yearCPI.getValue(key)).doubleValue();
			double CPIBaseYear = ((Number) yearCPI.getValue(BASE_PRICE_YEAR)).doubleValue();

			double deflator = CPIBaseYear / CPICurrentYear;

			deflatorCPIMap.put(year, deflator);



		}



		/*
		//Below an example of how the mortality and fertility rates can be accessed:
		for (Gender gender : Gender.values()) {
			for (int age = 0; age <= 100; age++) {
				for (int year = 1981; year <= 2068; year++) {
					double value = ((Number) Parameters.getMortalityProjectionsByGenderAgeYear().getValue(gender.toString(), age, year)).doubleValue();
					System.out.println(gender + " " + age + " " + year + " has mortality rate: " + value);
				}
			}
		}

		for (int year = 1973; year <= 2043; year++) {
			double value = ((Number) Parameters.getFertilityProjectionsByYear().getValue("Value", year)).doubleValue();
			System.out.println(year + " Fertility rate is " + value);
		}
		 */

		/*
		for (Gender gender : Gender.values()) {
			marriageTypesFrequencyByGenderAndRegion.put(gender, MultiKeyMap.multiKeyMap(new LinkedMap<>()));
			
			for (Region region : Parameters.getCountryRegions()) {
				marriageTypesFrequencyByGenderAndRegion.get(gender).put
			}
		}
		*/

		//Load country specific data
		int columnsWagesMales = -1;
		int columnsWagesFemales = -1;
		int columnsEmploymentSelectionMales = -1;
		int columnsEmploymentSelectionFemales = -1;
		int columnsLabourSupplyUtilityMales = -1;
		int columnsLabourSupplyUtilityFemales = -1;
		int columnsLabourSupplyUtilityMalesWithDependent = -1;
		int columnsLabourSupplyUtilityFemalesWithDependent = -1;
		int columnsLabourSupplyUtilityACMales = -1;
		int columnsLabourSupplyUtilityACFemales = -1;
		int columnsLabourSupplyUtilityCouples = -1;
		int columnsHealthH1a = -1;
		int columnsHealthH1b = -1; 
		int columnsHealthH2b = -1; 
		int columnsEducationE1a = -1; 
		int columnsEducationE1b = -1; 
		int columnsEducationE2a = -1; 
		int columnsEducationE2aCovariance = -1;
		int columnsPartnershipU1a = -1;
		int columnsPartnershipU1b = -1;
		int columnsPartnershipU2b = -1;
		int columnsPartnershipU1 = -1;
		int columnsPartnershipU2 = -1;
		int columnsFertilityF1a = -1;
		int columnsFertilityF1b = -1;
		int columnsFertilityF1 = -1;
		int columnsIncomeI1a = -1;
		int columnsIncomeI1b = -1;
		int columnsIncomeI3a = -1;
		int columnsIncomeI3b = -1;
		int columnsIncomeI3c = -1;
		int columnsLeaveHomeP1a = -1;
		int columnsRetirementR1a = -1;
		int columnsRetirementR1b = -1;
		if(country.equals(Country.IT)) {
			columnsWagesMales = 12;
			columnsWagesFemales = 12;
			columnsEmploymentSelectionMales = 14;
			columnsEmploymentSelectionFemales = 14;
			columnsLabourSupplyUtilityMales = 11;
			columnsLabourSupplyUtilityFemales = 11;
			columnsLabourSupplyUtilityMalesWithDependent = 10;
			columnsLabourSupplyUtilityFemalesWithDependent = 9;
			columnsLabourSupplyUtilityACMales = 12;
			columnsLabourSupplyUtilityACFemales = 9;
			columnsLabourSupplyUtilityCouples = 17;
			columnsHealthH1a = 16; //1 for coeffs + 15 for var-cov matrix
			columnsHealthH1b = 23;
			columnsHealthH2b = 24;
			columnsEducationE1a = 14;
			columnsEducationE1b = 19;
			columnsEducationE2a = 1;
			columnsEducationE2aCovariance = 26;
			columnsPartnershipU1 = 21;
			columnsPartnershipU2 = 27;
			columnsFertilityF1 = 22;
			columnsIncomeI1a = 14;
			columnsIncomeI1b = 23;
			columnsIncomeI3a = 14;
			columnsIncomeI3b = 23;
			columnsIncomeI3c = 24;
			columnsLeaveHomeP1a = 19;
			columnsRetirementR1a = 19;
			columnsRetirementR1b = 24;
		}
		else if(country.equals(Country.UK)) {
			columnsWagesMales = 19;
			columnsWagesFemales = 19;
			columnsEmploymentSelectionMales = 21;
			columnsEmploymentSelectionFemales = 21;
			columnsLabourSupplyUtilityMales = 6;
			columnsLabourSupplyUtilityFemales = 6;
			columnsLabourSupplyUtilityMalesWithDependent = 6;
			columnsLabourSupplyUtilityFemalesWithDependent = 6;
			columnsLabourSupplyUtilityACMales = 6;
			columnsLabourSupplyUtilityACFemales = 6;
			columnsLabourSupplyUtilityCouples = 12;
			columnsHealthH1a = 23; //1 for coefficients + 22 for covariance
			columnsHealthH1b = 30; //1 for coefficients + 29 for covariance 
			columnsHealthH2b = 29; //1 for coefficients + 28 for covariance 
			columnsEducationE1a = 21; 
			columnsEducationE1b = 27; 
			columnsEducationE2a = 1;
			columnsEducationE2aCovariance = 40;
			columnsPartnershipU1a = 24;
			columnsPartnershipU1b = 28;
			columnsPartnershipU2b = 35;
			columnsFertilityF1a = 14;
			columnsFertilityF1b = 29; 
			columnsIncomeI1a = 20;
			columnsIncomeI1b = 32;
			columnsIncomeI3a = 14;
			columnsIncomeI3b = 23;
			columnsIncomeI3c = 24;
			columnsLeaveHomeP1a = 26;
			columnsRetirementR1a = 26;
			columnsRetirementR1b = 31;
		}
		else throw new IllegalArgumentException("Country not recognised in Parameters.loadParameters()!");

		//The Raw maps contain the estimates and covariance matrices, from which we bootstrap at the start of each simulation

		
		//Heckman model employment selection
		coeffCovarianceEmploymentSelectionMales = ExcelAssistant.loadCoefficientMap("input/reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_Males", 1, columnsEmploymentSelectionMales);
		coeffCovarianceEmploymentSelectionFemales = ExcelAssistant.loadCoefficientMap("input/reg_employmentSelection.xlsx", countryString + "_EmploymentSelection_Females", 1, columnsEmploymentSelectionFemales);

		// Wages
		coeffCovarianceWagesMales = ExcelAssistant.loadCoefficientMap("input/reg_wages.xlsx", countryString + "_Wages_Males", 1, columnsWagesMales);
		coeffCovarianceWagesFemales = ExcelAssistant.loadCoefficientMap("input/reg_wages.xlsx", countryString + "_Wages_Females", 1, columnsWagesFemales);
		
		//Labour Supply coefficients from Zhechun's estimates on the EM input data
		coeffLabourSupplyUtilityMales = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_Single_Males", 1, columnsLabourSupplyUtilityMales);
		coeffLabourSupplyUtilityFemales = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_Single_Females", 1, columnsLabourSupplyUtilityFemales);
		coeffLabourSupplyUtilityMalesWithDependent = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_Males_With_Dep", 1, columnsLabourSupplyUtilityMalesWithDependent);
		coeffLabourSupplyUtilityFemalesWithDependent = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_Females_With_Dep", 1, columnsLabourSupplyUtilityFemalesWithDependent);
		coeffLabourSupplyUtilityACMales = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_SingleAC_Males", 1, columnsLabourSupplyUtilityACMales);
		coeffLabourSupplyUtilityACFemales = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_SingleAC_Females", 1, columnsLabourSupplyUtilityACFemales);
		coeffLabourSupplyUtilityCouples = ExcelAssistant.loadCoefficientMap("input/reg_labourSupplyUtility.xlsx", countryString + "_Couples", 1, columnsLabourSupplyUtilityCouples);

		//Health
		coeffCovarianceHealthH1a = ExcelAssistant.loadCoefficientMap("input/reg_health.xlsx", countryString + "_H1a", 1, columnsHealthH1a);
		coeffCovarianceHealthH1b = ExcelAssistant.loadCoefficientMap("input/reg_health.xlsx", countryString + "_H1b", 1, columnsHealthH1b);
		coeffCovarianceHealthH2b = ExcelAssistant.loadCoefficientMap("input/reg_health.xlsx", countryString + "_H2b", 1, columnsHealthH2b);
		
		//Education
		coeffCovarianceEducationE1a = ExcelAssistant.loadCoefficientMap("input/reg_education.xlsx", countryString + "_E1a", 1, columnsEducationE1a);
		coeffCovarianceEducationE1b = ExcelAssistant.loadCoefficientMap("input/reg_education.xlsx", countryString + "_E1b", 1, columnsEducationE1b);
		coeffEducationE2aHigh = ExcelAssistant.loadCoefficientMap("input/reg_education.xlsx", countryString + "_E2a_High", 1, columnsEducationE2a);
		coeffEducationE2aLow = ExcelAssistant.loadCoefficientMap("input/reg_education.xlsx", countryString + "_E2a_Low", 1, columnsEducationE2a);
		Map<Education, MultiKeyCoefficientMap> coeffEducationE2aHighLowMap = new LinkedHashMap<Education, MultiKeyCoefficientMap>();
		coeffEducationE2aHighLowMap.put(Education.High, coeffEducationE2aHigh);
		coeffEducationE2aHighLowMap.put(Education.Low, coeffEducationE2aLow);
		MultiKeyCoefficientMap educationE2aHighLowCovariance = ExcelAssistant.loadCoefficientMap("input/reg_education.xlsx", countryString + "_E2a_HighLow_Covariance", 1, columnsEducationE2aCovariance);
		
		//Partnership
		if (country.equals(Country.UK)) {
			coeffCovariancePartnershipU1a = ExcelAssistant.loadCoefficientMap("input/reg_partnership.xlsx", countryString + "_U1a", 1, columnsPartnershipU1a);
			coeffCovariancePartnershipU1b = ExcelAssistant.loadCoefficientMap("input/reg_partnership.xlsx", countryString + "_U1b", 1, columnsPartnershipU1b);
			coeffCovariancePartnershipU2b = ExcelAssistant.loadCoefficientMap("input/reg_partnership.xlsx", countryString + "_U2b", 1, columnsPartnershipU2b);
		}
		else if (country.equals(Country.IT)) {
			coeffCovariancePartnershipITU1 = ExcelAssistant.loadCoefficientMap("input/reg_partnership.xlsx", countryString + "_U1", 1, columnsPartnershipU1);
			coeffCovariancePartnershipITU2 = ExcelAssistant.loadCoefficientMap("input/reg_partnership.xlsx", countryString + "_U2", 1, columnsPartnershipU2);
		}


		//Partnership - parameters for matching based on wage and age differential
		meanCovarianceParametricMatching = ExcelAssistant.loadCoefficientMap("input/scenario_parametricMatching.xlsx", countryString, 1, 1);
		
		//Fertility
		if (country.equals(Country.UK)) {
			coeffCovarianceFertilityF1a = ExcelAssistant.loadCoefficientMap("input/reg_fertility.xlsx", countryString + "_F1a", 1, columnsFertilityF1a);
			coeffCovarianceFertilityF1b = ExcelAssistant.loadCoefficientMap("input/reg_fertility.xlsx", countryString + "_F1b", 1, columnsFertilityF1b);
		}
		else if (country.equals(Country.IT)) {
			coeffCovarianceFertilityF1 = ExcelAssistant.loadCoefficientMap("input/reg_fertility.xlsx", countryString + "_F1", 1, columnsFertilityF1);
		}
		//Income
		coeffCovarianceIncomeI1a = ExcelAssistant.loadCoefficientMap("input/reg_income.xlsx", countryString + "_I1a", 1, columnsIncomeI1a);
		coeffCovarianceIncomeI1b = ExcelAssistant.loadCoefficientMap("input/reg_income.xlsx", countryString + "_I1b", 1, columnsIncomeI1b);
		coeffCovarianceIncomeI3a = ExcelAssistant.loadCoefficientMap("input/reg_income.xlsx", countryString + "_I3a", 1, columnsIncomeI3a);
		coeffCovarianceIncomeI3b = ExcelAssistant.loadCoefficientMap("input/reg_income.xlsx", countryString + "_I3b", 1, columnsIncomeI3b);
		coeffCovarianceIncomeI3c = ExcelAssistant.loadCoefficientMap("input/reg_income.xlsx", countryString + "_I3c", 1, columnsIncomeI3c);

		//Leaving parental home
		coeffCovarianceLeaveHomeP1a = ExcelAssistant.loadCoefficientMap("input/reg_leaveParentalHome.xlsx", countryString + "_P1a", 1, columnsLeaveHomeP1a);

		//Retirement
		coeffCovarianceRetirementR1a = ExcelAssistant.loadCoefficientMap("input/reg_retirement.xlsx", countryString + "_R1a", 1, columnsRetirementR1a);
		coeffCovarianceRetirementR1b = ExcelAssistant.loadCoefficientMap("input/reg_retirement.xlsx", countryString + "_R1b", 1, columnsRetirementR1b);
		
		
		//Bootstrap the coefficients
		if(bootstrapAll) {
			coeffCovarianceHealthH1a = RegressionUtils.bootstrap(coeffCovarianceHealthH1a); //Note that this overrides the original coefficient map with bootstrapped values
			coeffCovarianceHealthH1b = RegressionUtils.bootstrap(coeffCovarianceHealthH1b);
			coeffCovarianceHealthH2b = RegressionUtils.bootstrap(coeffCovarianceHealthH2b);
			coeffCovarianceEducationE1a = RegressionUtils.bootstrap(coeffCovarianceEducationE1a);
			coeffCovarianceEducationE1b = RegressionUtils.bootstrap(coeffCovarianceEducationE1b);
			coeffEducationE2aHighLowMap = RegressionUtils.bootstrapMultinomialRegression(coeffEducationE2aHighLowMap, educationE2aHighLowCovariance, Education.class);
			coeffCovarianceIncomeI1a = RegressionUtils.bootstrap(coeffCovarianceIncomeI1a);
			coeffCovarianceIncomeI1b = RegressionUtils.bootstrap(coeffCovarianceIncomeI1b);
			coeffCovarianceIncomeI3a = RegressionUtils.bootstrap(coeffCovarianceIncomeI3a);
			coeffCovarianceIncomeI3b = RegressionUtils.bootstrap(coeffCovarianceIncomeI3b);
			coeffCovarianceIncomeI3c = RegressionUtils.bootstrap(coeffCovarianceIncomeI3c);
			coeffCovarianceEmploymentSelectionMales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionMales);
			coeffCovarianceEmploymentSelectionFemales = RegressionUtils.bootstrap(coeffCovarianceEmploymentSelectionFemales);
			coeffCovarianceWagesMales = RegressionUtils.bootstrap(coeffCovarianceWagesMales);
			coeffCovarianceWagesFemales = RegressionUtils.bootstrap(coeffCovarianceWagesFemales);
			coeffLabourSupplyUtilityMales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityMales);
			coeffLabourSupplyUtilityFemales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityFemales);
			coeffLabourSupplyUtilityMalesWithDependent = RegressionUtils.bootstrap(coeffLabourSupplyUtilityMalesWithDependent);
			coeffLabourSupplyUtilityFemalesWithDependent = RegressionUtils.bootstrap(coeffLabourSupplyUtilityFemalesWithDependent);
			coeffLabourSupplyUtilityACMales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityACMales);
			coeffLabourSupplyUtilityACFemales = RegressionUtils.bootstrap(coeffLabourSupplyUtilityACFemales);
			coeffLabourSupplyUtilityCouples = RegressionUtils.bootstrap(coeffLabourSupplyUtilityCouples);
			coeffCovarianceLeaveHomeP1a = RegressionUtils.bootstrap(coeffCovarianceLeaveHomeP1a);
			coeffCovarianceRetirementR1a = RegressionUtils.bootstrap(coeffCovarianceRetirementR1a);
			coeffCovarianceRetirementR1b = RegressionUtils.bootstrap(coeffCovarianceRetirementR1b);

			//Specification of some processes depends on the country:

			if (country.equals(Country.UK)) {
				coeffCovariancePartnershipU1a = RegressionUtils.bootstrap(coeffCovariancePartnershipU1a);
				coeffCovariancePartnershipU1b = RegressionUtils.bootstrap(coeffCovariancePartnershipU1b);
				coeffCovariancePartnershipU2b = RegressionUtils.bootstrap(coeffCovariancePartnershipU2b);
				coeffCovarianceFertilityF1a = RegressionUtils.bootstrap(coeffCovarianceFertilityF1a);
				coeffCovarianceFertilityF1b = RegressionUtils.bootstrap(coeffCovarianceFertilityF1b);
			} else if (country.equals(Country.IT)) {
				coeffCovariancePartnershipITU1 = RegressionUtils.bootstrap(coeffCovariancePartnershipITU1);
				coeffCovariancePartnershipITU2 = RegressionUtils.bootstrap(coeffCovariancePartnershipITU2);
				coeffCovarianceFertilityF1 = RegressionUtils.bootstrap(coeffCovarianceFertilityF1);
			}
		}
			
 
		//Health
		regHealthH1a = new LinearRegression(coeffCovarianceHealthH1a);
		regHealthH1b = new LinearRegression(coeffCovarianceHealthH1b);
		regHealthH2b = new ProbitRegression(coeffCovarianceHealthH2b);
		
		//Education
		regEducationE1a = new ProbitRegression(coeffCovarianceEducationE1a); 
		regEducationE1b = new ProbitRegression(coeffCovarianceEducationE1b);
		//TODO: Process E2a uses Medium education as the default level, while previously Low was the default. Make sure this is ok.
		regEducationE2a = new MultiProbitRegression<Education>(coeffEducationE2aHighLowMap);
		
		//Partnership
		if (country.equals(Country.UK)) {
			regPartnershipU1a = new ProbitRegression(coeffCovariancePartnershipU1a);
			regPartnershipU1b = new ProbitRegression(coeffCovariancePartnershipU1b);
			regPartnershipU2b = new ProbitRegression(coeffCovariancePartnershipU2b);
		} else if (country.equals(Country.IT)) {
			regPartnershipITU1 = new ProbitRegression(coeffCovariancePartnershipITU1);
			regPartnershipITU2 = new ProbitRegression(coeffCovariancePartnershipITU2);
		}

		//Fertility
		if (country.equals(Country.UK)) {
			regFertilityF1a = new ProbitRegression(coeffCovarianceFertilityF1a);
			regFertilityF1b = new ProbitRegression(coeffCovarianceFertilityF1b);
		} else if (country.equals(Country.IT)) {
			regFertilityF1 = new ProbitRegression(coeffCovarianceFertilityF1);
		}

		//Income
		regIncomeI1a = new LinearRegression(coeffCovarianceIncomeI1a);
		regIncomeI1b = new LinearRegression(coeffCovarianceIncomeI1b);
		regIncomeI3a = new LinearRegression(coeffCovarianceIncomeI3a);
		regIncomeI3b = new LinearRegression(coeffCovarianceIncomeI3b);
		regIncomeI3c = new LinearRegression(coeffCovarianceIncomeI3c);

		
		//XXX: Note: the model used for selection in Heckman procedure is a Probit, but to obtain Inverse Mills Ratio, linear prediction needs to be obtained - so linear regression used here
		regEmploymentSelectionMale = new LinearRegression(coeffCovarianceEmploymentSelectionMales);	
		regEmploymentSelectionFemale = new LinearRegression(coeffCovarianceEmploymentSelectionFemales);
		standardNormalDistribution = new NormalDistribution();
		
		//Wages
		regWagesMales = new LinearRegression(coeffCovarianceWagesMales);
		regWagesFemales = new LinearRegression(coeffCovarianceWagesFemales);
		
		//Labour Supply regressions from Zhechun's estimates on the EM input data
		regLabourSupplyUtilityMales = new LinearRegression(coeffLabourSupplyUtilityMales);
		regLabourSupplyUtilityFemales = new LinearRegression(coeffLabourSupplyUtilityFemales);
		regLabourSupplyUtilityMalesWithDependent = new LinearRegression(coeffLabourSupplyUtilityMalesWithDependent);
		regLabourSupplyUtilityFemalesWithDependent = new LinearRegression(coeffLabourSupplyUtilityFemalesWithDependent);
		regLabourSupplyUtilityACMales = new LinearRegression(coeffLabourSupplyUtilityACMales);
		regLabourSupplyUtilityACFemales = new LinearRegression(coeffLabourSupplyUtilityACFemales);
		regLabourSupplyUtilityCouples = new LinearRegression(coeffLabourSupplyUtilityCouples);

		//Leaving parental home
		regLeaveHomeP1a = new ProbitRegression(coeffCovarianceLeaveHomeP1a);

		//Retirement
		regRetirementR1a = new ProbitRegression(coeffCovarianceRetirementR1a);
		regRetirementR1b = new ProbitRegression(coeffCovarianceRetirementR1b);

		//Max age no longer obtained from initial population, but back to population projections
		//Obtain the maximum Age modelled, from the ages used in the population projections 
		try {
			for (Object multiKey : populationProjections.keySet()) {		//This provides a way of obtaining the maximum possible Age from the align_popProjections.xlsx input file.
				final Object ageKey = ((MultiKey) multiKey).getKey(2);
				final Integer Age = Integer.parseInt(ageKey.toString());
				if(Age > max_age) {
					max_age = Age;		
				}				
			} 	
		} catch (NumberFormatException nfe) {
			throw new NumberFormatException("NumberFormatException: " + nfe.getMessage());
		}	


		//Create the age and wage differential MultivariateNormalDistribution for partnership formation, using means and var-cov matrix loaded from Excel
		double[] means = {((Number) meanCovarianceParametricMatching.getValue("mean_dag_diff")).doubleValue(), ((Number) meanCovarianceParametricMatching.getValue("mean_wage_diff")).doubleValue()};
		double[][] covariances = { {((Number) meanCovarianceParametricMatching.getValue("var_dag_diff")).doubleValue(), ((Number) meanCovarianceParametricMatching.getValue("cov_dag_wage_diff")).doubleValue()} , {((Number) meanCovarianceParametricMatching.getValue("cov_dag_wage_diff")).doubleValue(), ((Number) meanCovarianceParametricMatching.getValue("var_wage_diff")).doubleValue()}};
		wageAndAgeDifferentialMultivariateNormalDistribution = getMultivariateNormalDistribution(means, covariances);


//		MAX_AGE_IN_EDUCATION = max_age;
//		MAX_AGE_MARRIAGE = max_age;
		calculateFertilityRatesFromProjections();
		calculatePopulationGrowthRatiosFromProjections();

		/////////////////////////////////////////////////POPULATE STATISTICS FOR VALIDATION/////////////////////////////
		//Students by Age
		validationStudentsByAge = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_studentsByAge", 1, 10);

		//Students by Region
		validationStudentsByRegion = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_studentsByRegion", 1, 6);

		//Education level of over 17 year olds
		validationEducationLevel = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_educationLevel", 1, 3);

		//Education level by age group
		validationEducationLevelByAge = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_educationLevelByAge", 1, 24);

		//Education level by region
		validationEducationLevelByRegion = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_educationLevelByRegion", 1, 15);

		//Partnered BU share by region
		validationPartneredShareByRegion = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_partneredBUShareByRegion", 1, 6);

		//Disabled by age
		validationDisabledByAge = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_disabledByAgeGroup", 1, 6);

		validationDisabledByGender = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_disabledByGender", 1, 2);

		//Health by age
		validationHealthByAge = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_healthByAgeGroup", 1, 6);

		//Employment by gender
		validationEmploymentByGender = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_employmentByGender", 1, 2);

		//Employment by age and gender
		validationEmploymentByAgeAndGender = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_employmentByGenderAndAge", 1, 18);

		//Employment by maternity
		validationEmploymentByMaternity = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_employmentByMaternity", 1, 3);

		//Employment by gender and region
		validationEmploymentByGenderAndRegion = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_employmentByGenderAndRegion", 1, 10);

		//Labour supply by education
		validationLabourSupplyByEducation = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_labourSupplyByEducation", 1, 3);

		//Activity status
		validationActivityStatus = ExcelAssistant.loadCoefficientMap("input/validation_statistics.xlsx", countryString + "_activityStatus", 1, 3);

	}

	/**
	 *
	 * Update the probability of sickness by age profile for those old enough to work, catering 
	 * for the change (increase) in retirement age in future years, by 'stretching' the 
	 * (approximated) curves for males and females using the instructions below.
	 * 
	 * From Matteo:
	 * " In every year we will evolve this fraction together with the state retirement age. 
	 * This means that the fraction of sick people at the minimum age (18) and at the maximum age 
	 * (the state retirement age) remain the same, and the fitted curve is scaled accordingly."
	 *
	 *	This means that if the fraction of people with bad health at age 60 is x% when state 
	 *	retirement age is 65, after state retirement age increases to 66 that fraction will be met 
	 *	at age 60/65*66 = 61 (rounded).
	 *
	 *	I don't mind using a cubic rather than a quadratic spline, try it out if you wish 
	 *	(differences are small). We can also use the actual numbers, I have used a spline just 
	 *	to smooth out the profiles.
	 *
	 */

	/*
	public static void updateProbSick(int year) {

		//Only need to update prob of sick profile if retirement years have changed, so check
		//For males
		boolean retirementYearsHaveChanged = ((Number)fixedRetireAge.getValue(year, Gender.Male.toString())) != ((Number)fixedRetireAge.getValue(year-1, Gender.Male.toString()));
		//For females - if case for males is already true, the boolean will always be true
		retirementYearsHaveChanged = retirementYearsHaveChanged || ((Number)fixedRetireAge.getValue(year, Gender.Female.toString())) != ((Number)fixedRetireAge.getValue(year-1, Gender.Female.toString()));
				
		if(retirementYearsHaveChanged) {
			LinkedHashMap<Gender, Integer> retirementAge = new LinkedHashMap<>();
			retirementAge.put(Gender.Male, ((Number)fixedRetireAge.getValue(year, Gender.Male.toString())).intValue());
			retirementAge.put(Gender.Female, ((Number)fixedRetireAge.getValue(year, Gender.Female.toString())).intValue());
			
			probSick.clear();
			for(Gender gender: Gender.values()) {
				int minAge = Parameters.MIN_AGE_TO_LEAVE_EDUCATION;
				probSick.put(gender, minAge, rawProbSick.getValue(gender.toString(), minAge));
			}
			
			for(Object o: rawProbSick.keySet()) {
				MultiKey mk = (MultiKey)o;
				String gender = (String) mk.getKey(0);
				int rawAge = (int) mk.getKey(1);
				int adjustedAge;
				if(gender.equals(Gender.Male.toString())) {
					adjustedAge = (int)(rawAge * retirementAge.get(Gender.Male)/(double)maleMaxAgeSick);
					probSick.put(Gender.Male, adjustedAge, rawProbSick.get(mk));
				}
				else {
					adjustedAge = (int)(rawAge * retirementAge.get(Gender.Female)/(double)femaleMaxAgeSick);
					probSick.put(Gender.Female, adjustedAge, rawProbSick.get(mk));
				}
										
			}
			
			//Fill in any gaps in age, due to shift in ages, filling with linear interpolation (arithmetic 
			//average of neighbouring ages)
			for(Gender gender: Gender.values()) {
				for(int i = Parameters.MIN_AGE_TO_LEAVE_EDUCATION; i < retirementAge.get(gender); i++) {
					if(!probSick.containsKey(gender, i)) {
						double youngerVal = ((Number)probSick.get(gender, i-1)).doubleValue();
						double olderVal = ((Number)probSick.get(gender, i+1)).doubleValue();
						probSick.put(gender, i, 0.5*(youngerVal + olderVal));	//Insert arithmetic average between ages
					}
				}
			}
			
		}
	}

	public static void updateUnemploymentRate(int year) {
//		unemploymentRatesByRegion.clear();
		for(Region region: countryRegions) {
			unemploymentRatesByRegion.put(region, ((Number)unemploymentRates.getValue(region.toString(), year)).doubleValue());
		}
	}
*/
	public static NormalDistribution getStandardNormalDistribution() {
		return standardNormalDistribution;
	}

	//For a given vector of means and a variance-covariance matrix, return a MultivariateNormalDistribution that can be sampled from using sample() method
	public static MultivariateNormalDistribution getMultivariateNormalDistribution(double[] means, double[][] covariances) {
		MultivariateNormalDistribution multivariateNormalDistribution = new MultivariateNormalDistribution(means, covariances);
		return multivariateNormalDistribution;
	}


	private static void calculateFertilityRatesFromProjections() {
		
		int endYear = ((LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName())).getEndYear();
		int startYear = ((LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName())).getStartYear();
		
		fertilityRateByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		
		for(int year = startYear; year <= endYear; year++) {			
			for(Region region : countryRegions) {
				double projectedNumFertileWomen = 0.;
				for(int age = MIN_AGE_MATERNITY; age <= MAX_AGE_MATERNITY; age++) {
//					System.out.println("region " + region.toString() + ", age " + age + ", year " + year);
					double num = ((Number)populationProjections.getValue(Gender.Female.toString(), region.toString(), age, year)).doubleValue();
					projectedNumFertileWomen += num;
				}

				double numNewBorn = 0.;
				for(Gender gender: Gender.values()) {
					numNewBorn += ((Number)populationProjections.getValue(gender.toString(), region.toString(), 0, year)).doubleValue();		//Number of people aged 0 in projected years
				}
			
				if(projectedNumFertileWomen <= 0.) {
					throw new IllegalArgumentException("Projected Number of Females of Fertile Age is not positive!");
				} 
				else {	
					fertilityRateByRegionYear.put(region, year, numNewBorn / projectedNumFertileWomen);
				}
			}			
		}
	}
	
	
	private static void calculatePopulationGrowthRatiosFromProjections() {
		
		int endYear = ((LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName())).getEndYear();
		int startYear = ((LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName())).getStartYear();
		
		populationGrowthRatiosByRegionYear = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		
		for(int year = startYear+1; year <= endYear; year++) {		//Year is the latter year, i.e. growth ratio for year t is Pop(t)/Pop(t-1)			
			for(Region region : countryRegions) {
				double numberOfPeopleInRegionThisYear = 0.;
				double numberOfPeopleInRegionPreviousYear = 0.;
				for(Gender gender: Gender.values()) {					
					for(int age = 0; age <= max_age; age++) {
						numberOfPeopleInRegionThisYear += ((Number)populationProjections.getValue(gender.toString(), region.toString(), age, year)).doubleValue();
						numberOfPeopleInRegionPreviousYear += ((Number)populationProjections.getValue(gender.toString(), region.toString(), age, year-1)).doubleValue();
					}
				}
				populationGrowthRatiosByRegionYear.put(region, year, numberOfPeopleInRegionThisYear / numberOfPeopleInRegionPreviousYear);
			}			
		}
	}
	
	public static TreeMap<Integer, String> calculateEUROMODpolicySchedule(Country country) {
		//Load current values for policy description and initiation year
		MultiKeyCoefficientMap currentEUROMODpolicySchedule = ExcelAssistant.loadCoefficientMap("input" + File.separator + EUROMODpolicyScheduleFilename + ".xlsx", country.toString(), 1, 2);
		TreeMap<Integer, String> newEUROMODpolicySchedule = new TreeMap<>();

		for(Object o: currentEUROMODpolicySchedule.keySet()) {
			MultiKey k = (MultiKey)o;
			if(k.getKey(0) != null) {
				String name = k.getKey(0).toString();
				if(name != null) {
					String policyStartYearString = currentEUROMODpolicySchedule.getValue(name, EUROMODpolicyScheduleHeadingScenarioYearBegins).toString(); 
			    	if(policyStartYearString != null && !policyStartYearString.isEmpty()) {
			    		Integer policyStartYear = Integer.parseInt(policyStartYearString);
			    		if(newEUROMODpolicySchedule.containsKey(policyStartYear)) {
			    			throw new IllegalArgumentException("ERROR - there is more than one EUROMOD policy scenario with the same policy start year of " + policyStartYear + "!");
			    		}
			    		if(policyStartYear >= Parameters.getMin_Year() && policyStartYear <= Parameters.getMax_Year()) {
			    			newEUROMODpolicySchedule.put(policyStartYear, name.split(".txt")[0]);
			    		}
			    	}
				}
			}
		}		
		if(newEUROMODpolicySchedule.isEmpty()) {
			throw new IllegalArgumentException("ERROR - there are no EUROMOD policies with valid \'" + EUROMODpolicyScheduleHeadingScenarioYearBegins + "\' values specified in worksheet \'" + country + "\' of the " + EUROMODpolicyScheduleFilename + ".xlsx file!  "
					+ "Please ensure there is at least one EUROMOD policy with a start year between " + Parameters.getMin_Year() + " and " + Parameters.getMax_Year() + "!");
		}
		return newEUROMODpolicySchedule;
	}
	
	public static String getEUROMODpolicyForThisYear(int year, Map<Integer, String> scenarioPlan) {		
		String policyName;
		policyName = scenarioPlan.get(year);
		if(policyName == null) {	//Then, no EUROMOD policy begins in that year.  Instead, must apply the prevailing policy.			
			for(Integer policyStartYear: scenarioPlan.keySet()) {	//Should iterate in order of increasing value of integer
				if(policyStartYear <= year) {		//In case the start year of the whole simulation is later than the first entry, assign but iterate again and check the following year.  Stop when the policyStartYear is greater than the year 
					policyName = scenarioPlan.get(policyStartYear);
				}
				else {
					if(policyName == null) {		//Check for case when the earliest policyStartYear is later than the current year.  Need to apply the policy with the earliest policyStartYear anyway, just so there is a policy to apply!
						policyName = scenarioPlan.get(policyStartYear);
					}
					break;		//Stop if policyStartYear is greater than year
				}
			}						
		}		
		return policyName;		
	}
	
	public static void setCountryRegions(Country country) {
		countryRegions = new LinkedHashSet<Region>();
		for(Region region : Region.values()) {			//TODO: restrict this to only regions in the simulated country
			if(region.toString().startsWith(country.toString())) {			//Only assess the relevant regions for the country
				countryRegions.add(region);				//Create a set of only relevant regions that we can use below TODO: This should be done in the Parameters class, once and for all!
			}
		}
	}

	//Benefit Unit ID is different in different countries of EUROMOD. This method creates a map with name of the country and name of the benefit unit variable. It is used to create input database in the
	//SQLdataParser class.
	public static LinkedHashMap<Country, String> countryBenefitUnitNames = new LinkedHashMap<>();


	public static void setCountryBenefitUnitName() {

		//Benefit unit variable has different name in each country. This method loads the correct name of the benefit unit variable from Excel file system_bu_names.xlsx in the input folder.
		benefitUnitVariableNames = ExcelAssistant.loadCoefficientMap("input/system_bu_names.xlsx", "Names", 1, 1);

	}

	//-----------------------------------------------------------------------------------------------------
	// Access methods
	//-----------------------------------------------------------------------------------------------------

	public static MultiKeyCoefficientMap getBenefitUnitVariableNames() { return benefitUnitVariableNames; }

	public static MultiKeyCoefficientMap getPopulationProjections() {
		return populationProjections;
	}

	public static MultiKeyCoefficientMap getStudentShareProjections() { return studentShareProjections; }

	public static MultiKeyCoefficientMap getHighEducationRateInYear() { return projectionsHighEdu; }

	public static MultiKeyCoefficientMap getLowEducationRateInYear() {
		return projectionsLowEdu;
	}

	public static MultiLogitRegression<Education> getRegEducationLevel() {
		return regEducationLevel;
	}


	public static LinearRegression getRegHealthH1a() {
		return regHealthH1a;
	}

	public static LinearRegression getRegHealthH1b() {
		return regHealthH1b;
	}

	public static ProbitRegression getRegHealthH2b() {
		return regHealthH2b;
	}

	public static ProbitRegression getRegEducationE1a() {
		return regEducationE1a;
	}

	public static ProbitRegression getRegEducationE1b() {
		return regEducationE1b;
	}

	public static MultiProbitRegression<Education> getRegEducationE2a() {
		return regEducationE2a;
	}

	public static ProbitRegression getRegPartnershipU1a() {
		return regPartnershipU1a;
	}

	public static ProbitRegression getRegPartnershipU1b() {
		return regPartnershipU1b;
	}

	public static ProbitRegression getRegPartnershipU2b() {
		return regPartnershipU2b;
	}

	public static ProbitRegression getRegPartnershipITU1() {
		return regPartnershipITU1;
	}

	public static ProbitRegression getRegPartnershipITU2() {
		return regPartnershipITU2;
	}

	public static ProbitRegression getRegFertilityF1a() {
		return regFertilityF1a;
	}

	public static ProbitRegression getRegFertilityF1b() {
		return regFertilityF1b;
	}

	public static ProbitRegression getRegFertilityF1() {
		return regFertilityF1;
	}

	public static LinearRegression getRegIncomeI1a() {
		return regIncomeI1a;
	}

	public static LinearRegression getRegIncomeI1b() {
		return regIncomeI1b;
	}

	public static LinearRegression getRegIncomeI3a() { return regIncomeI3a; }

	public static LinearRegression getRegIncomeI3b() { return regIncomeI3b; }

	public static LinearRegression getRegIncomeI3c() { return regIncomeI3c; }

	public static int getMaxAge() {
		return max_age;
	}
	
	
	public static Set<Region> getCountryRegions() {
		return countryRegions;
	}

	public static MultiKeyMap getFertilityRateByRegionYear() {
		return fertilityRateByRegionYear;
	}

	public static LinearRegression getRegWagesMales() {
		return regWagesMales;
	}

	public static LinearRegression getRegWagesFemales() {
		return regWagesFemales;
	}

	public static String getInputFileName() {
		return inputFileName;
	}

	public static void setInputFileName(String inputFileName) {
		Parameters.inputFileName = inputFileName;
	}
	
	public static String getInitialInputFileName() {
		return initialInputFileName;
	}

	public static void setInitialInputFileName(String initialInputFileName) {
		Parameters.initialInputFileName = initialInputFileName;
	}

	public static LinearRegression getRegLabourSupplyUtilityCouples() {
		return regLabourSupplyUtilityCouples;
	}

	public static LinearRegression getRegLabourSupplyUtilityFemales() {
		return regLabourSupplyUtilityFemales;
	}

	public static LinearRegression getRegLabourSupplyUtilityMales() {
		return regLabourSupplyUtilityMales;
	}

	public static LinearRegression getRegLabourSupplyUtilityFemalesWithDependent() {
		return regLabourSupplyUtilityFemalesWithDependent;
	}

	public static LinearRegression getRegLabourSupplyUtilityMalesWithDependent() {
		return regLabourSupplyUtilityMalesWithDependent;
	}

	public static LinearRegression getRegLabourSupplyUtilityACMales() {
		return regLabourSupplyUtilityACMales;
	}

	public static LinearRegression getRegLabourSupplyUtilityACFemales() {
		return regLabourSupplyUtilityACFemales;
	}

	public static LinearRegression getRegEmploymentSelectionMale() {
		return regEmploymentSelectionMale;
	}

	public static LinearRegression getRegEmploymentSelectionFemale() {
		return regEmploymentSelectionFemale;
	}

	public static MultiKeyMap<Object, Double> getPopulationGrowthRatiosByRegionYear() {
		return populationGrowthRatiosByRegionYear;
	}


	public static DemandAdjustment getDemandAdjustment() {
		return demandAdjustment;
	}

	public static int getMin_Year() {
		return MIN_YEAR;
	}

	public static int getMax_Year() {
		return MAX_YEAR;
	}

	public static String getEUROMODpolicyForThisYear(int year) {
		return getEUROMODpolicyForThisYear(year, EUROMODpolicySchedule);
	}

	public static int getFixedRetireAge(int year, Gender gender) {
		return ((Number)fixedRetireAge.getValue(year, gender.toString())).intValue();
	}


	/*
	public static MultiKeyMap getProbSick() {
		return probSick;
	}
	*/
	/*
	public static double getUnemploymentRatesForRegion(Region region) {
		return unemploymentRatesByRegion.get(region);
	}
	*/

	public static MultiKeyCoefficientMap getMarriageTypesFrequency() {
		return marriageTypesFrequency;
	}

	public static MultiKeyCoefficientMap getEmploymentAlignment() {
		return employmentAlignment;
	}

	public static MultiKeyCoefficientMap getCoeffCovarianceHealthH1a() { return coeffCovarianceHealthH1a; }

	public static MultiKeyCoefficientMap getCoeffCovarianceHealthH1b() { return coeffCovarianceHealthH1b; }

	public static MultiKeyCoefficientMap getCoeffCovarianceWagesMales() { return coeffCovarianceWagesMales; }

	public static MultiKeyCoefficientMap getCoeffCovarianceWagesFemales() { return coeffCovarianceWagesFemales; }

	public static MultiKeyCoefficientMap getMortalityProjectionsByGenderAgeYear() { return mortalityProjectionsByGenderAgeYear; }

	public static MultiKeyCoefficientMap getFertilityProjectionsByYear() { return fertilityProjectionsByYear; }

	public static MultivariateNormalDistribution getWageAndAgeDifferentialMultivariateNormalDistribution() {
		return wageAndAgeDifferentialMultivariateNormalDistribution;
	}

	public static ProbitRegression getRegLeaveHomeP1a() {
		return regLeaveHomeP1a;
	}

	public static ProbitRegression getRegRetirementR1a() {
		return regRetirementR1a;
	}

	public static ProbitRegression getRegRetirementR1b() {
		return regRetirementR1b;
	}

	///////////////////////////////////////////GETTERS FOR VALIDATION///////////////////////////////////////
	public static MultiKeyCoefficientMap getValidationStudentsByAge() {
		return validationStudentsByAge;
	}

	public static MultiKeyCoefficientMap getValidationStudentsByRegion() {
		return validationStudentsByRegion;
	}

	public static MultiKeyCoefficientMap getValidationEducationLevel() {
		return validationEducationLevel;
	}

	public static MultiKeyCoefficientMap getValidationEducationLevelByAge() {
		return validationEducationLevelByAge;
	}

	public static MultiKeyCoefficientMap getValidationEducationLevelByRegion() {
		return validationEducationLevelByRegion;
	}

	public static MultiKeyCoefficientMap getValidationPartneredShareByRegion() {
		return validationPartneredShareByRegion;
	}

	public static MultiKeyCoefficientMap getValidationDisabledByAge() {
		return validationDisabledByAge;
	}

	public static MultiKeyCoefficientMap getValidationDisabledByGender() {
		return validationDisabledByGender;
	}

	public static MultiKeyCoefficientMap getValidationHealthByAge() {
		return validationHealthByAge;
	}

	public static MultiKeyCoefficientMap getValidationEmploymentByGender() {
		return validationEmploymentByGender;
	}

	public static MultiKeyCoefficientMap getValidationEmploymentByAgeAndGender() {
		return validationEmploymentByAgeAndGender;
	}

	public static MultiKeyCoefficientMap getValidationEmploymentByMaternity() {
		return validationEmploymentByMaternity;
	}

	public static MultiKeyCoefficientMap getValidationEmploymentByGenderAndRegion() {
		return validationEmploymentByGenderAndRegion;
	}

	public static MultiKeyCoefficientMap getValidationLabourSupplyByEducation() {
		return validationLabourSupplyByEducation;
	}

	public static MultiKeyCoefficientMap getValidationActivityStatus() {
		return validationActivityStatus;
	}
}

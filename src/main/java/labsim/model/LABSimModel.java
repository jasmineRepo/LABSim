// define package
package labsim.model;

// import Java packages
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// import plug-in packages
import labsim.experiment.LABSimCollector;
import labsim.model.decisions.ManagerPopulateGrids;
import labsim.model.enums.*;
import microsim.alignment.outcome.ResamplingAlignment;
import microsim.event.*;
import microsim.event.EventListener;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.apache.commons.lang3.time.StopWatch;

// import JAS-mine packages
import microsim.alignment.outcome.AlignmentOutcomeClosure;
import microsim.annotation.GUIparameter;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.db.DatabaseUtils;
import microsim.engine.AbstractSimulationManager;
import microsim.engine.SimulationEngine;
import microsim.matching.IterativeRandomMatching;
import microsim.matching.IterativeSimpleMatching;
import microsim.matching.MatchingClosure;
import microsim.matching.MatchingScoreClosure;

// import LABOURsim packages
import labsim.data.Parameters;
import labsim.data.SQLdataParser;
import labsim.data.filters.FertileFilter;


/**
 *
 * CLASS TO MANAGE SIMULATION PROJECTIONS
 *
 */
public class LABSimModel extends AbstractSimulationManager implements EventListener {

	// default simulation parameters
	private static Logger log = Logger.getLogger(LABSimModel.class);

//	@GUIparameter(description = "Country to be simulated")
	private Country country; // = Country.UK;
	
	@GUIparameter(description = "Simulated population size (base year)")
	private Integer popSize = 20000;

	@GUIparameter(description = "Simulation first year [valid range 2011-2017]")
	private Integer startYear = Parameters.getMin_Year();

	@GUIparameter(description = "Simulation ends at year [valid range 2011-2050]")
	private Integer endYear = 2050;

//	@GUIparameter(description = "Fix year used in the regressions to one specified below")
	private boolean fixTimeTrend = true;

	@GUIparameter(description = "Fix year used in the regressions to")
	private Integer timeTrendStopsIn = 2017;

	private Integer timeTrendStopsInMonetaryProcesses = 2017; // For monetary processes, time trend always continues to 2017 (last observed year in the estimation sample) and then values are grown at the growth rate read from Excel
	
//	@GUIparameter(description="Age at which people in initial population who are not employed are forced to retire")
//	private Integer ageNonWorkPeopleRetire = 65;	//The problem is that it is difficult to find donor benefitUnits for non-zero labour supply for older people who are in the Nonwork category but not Retired.  They should, in theory, still enter the Labour Market Module, but if we cannot find donor benefitUnits, how should we proceed?  We avoid this problem by defining that people over the age specified here are retired off if they have activity_status equal to Nonwork.
	
//	@GUIparameter(description="Minimum age for males to retire")
//	private Integer minRetireAgeMales = 45;
//
//	@GUIparameter(description="Maximum age for males to retire")
//	private Integer maxRetireAgeMales = 75;
//
//	@GUIparameter(description="Minimum age for females to retire")
//	private Integer minRetireAgeFemales = 45;
//
//	@GUIparameter(description="Maximum age for females to retire")
//	private Integer maxRetireAgeFemales = 75;

	@GUIparameter(description = "Fix random seed?")
	private Boolean fixRandomSeed = true;

	@GUIparameter(description = "If random seed is fixed, set to this number")
	private Long randomSeedIfFixed = 1L;

	@GUIparameter(description = "Time window in years for (in)security index calculation")
	private Integer sIndexTimeWindow = 5;

	@GUIparameter(description = "Value of risk aversion parameter (alpha)")
	private Double sIndexAlpha = 2.;

	@GUIparameter(description = "Value of discount factor (delta)")
	private Double sIndexDelta = 0.98;

	//TODO: This should take a different value for each country, but not sure how to set it. The data comes from here: https://data.oecd.org/hha/household-savings.htm
	@GUIparameter(description = "Value of saving rate (s). The default value is based on average % of household disposable income saved between 2000 - 2019 reported by the OECD")
	private Double savingRate = 0.056;

//	@GUIparameter(description = "Force recreation of input database based on the data provided by the population_[country].csv file")
//	private boolean refreshInputDatabase = false;		//Tables can be constructed in GUI dialog in launch, before JAS-mine GUI appears.  However, if skipping that, and manually altering the EUROMODpolicySchedule.xlsx file, this will need to be set to true to build new input database before simulation is run (though the new input database will only be viewable in the output/input/input.h2.db file).

	@GUIparameter(description = "If true, set initial earnings from data in input population, otherwise, set using the wage equation regression estimates")
	private boolean initialisePotentialEarningsFromDatabase = false;
	
//	@GUIparameter(description = "If unchecked, will expand population and not use weights")
	private boolean useWeights = false;
	
	@GUIparameter(description = "If unchecked, will use the standard matching method")
//	private boolean useSBAMMatching = false;
	private UnionMatchingMethod unionMatchingMethod = UnionMatchingMethod.ParametricNoRegion;


//	@GUIparameter(description = "If checked, will align fertility")
	private boolean alignFertility = true;

//	@GUIparameter(description = "tick to enable intertemporal optimised consumption and labour decisions")
	private boolean enableIntertemporalOptimisations = false;

//	@GUIparameter(description = "tick to use behavioural solutions saved by a previous simulation")
	private boolean useSavedBehaviour = false;

//	@GUIparameter(description = "if using behavioural solutions from a previous simulation, provide name of saved file here")
	private String fileNameForSavedBehaviour = "";

//	@GUIparameter(description = "the number of employment options from which a household's principal wage earner can choose")
	private Integer employmentOptionsOfPrincipalWorker = 3;

//	@GUIparameter(description = "the number of employment options from which a household's secondary wage earner can choose")
	private Integer employmentOptionsOfSecondaryWorker = 3;

//	@GUIparameter(description = "whether to include student and education status in state space for IO behavioural solutions")
	private boolean intertemporalResponsesToEducationStatus = false;

//	@GUIparameter(description = "whether to include health and disability in state space for IO behavioural solutions")
	private boolean intertemporalResponsesToHealthStatus = false;

//	@GUIparameter(description = "whether to include geographic region in state space for IO behavioural solutions")
	private boolean intertemporalResponsesToRegion = false;

	private boolean alignEducation = false; //Set to true to align level of education

	private boolean alignInSchool = false; //Set to true to align share of students among 16-29 age group

	private boolean alignCohabitation = true; //Set to true to align share of couples (cohabiting individuals)

	private boolean alignEmployment = false; //Set to true to align employment share

    public boolean addRegressionStochasticComponent = true; //If set to true, and regression contains ResStanDev variable, will evaluate the regression score including stochastic part, and omits the stochastic component otherwise.

	public boolean fixRegressionStochasticComponent = true; // If true, only draw stochastic component once and use the same value throughout the simulation. Currently applies to wage equations.

	public boolean commentsOn = true;

	public boolean debugCommentsOn = true;

	public boolean donorFinderCommentsOn = true;

	@GUIparameter(description = "If checked, will use Covid-19 labour supply module")
	public boolean labourMarketCovid19On = true; // Set to true to use reduced-form labour market module for years affected by Covid-19 (2020, 2021)

	private int ordering = Parameters.MODEL_ORDERING;	//Used in Scheduling of model events.  Schedule model events at the same time as the collector and observer events, but a lower order, so will be fired before the collector and observer have updated.

	private Set<Person> persons;
	
	private MultiKeyMap<Object, LinkedHashSet<Person>> personsByGenderRegionAndAge;
	
	//For marriage matching - types based on region and gender: 
	//private Map<Gender, LinkedHashMap<Region, Double>> marriageTargetsGenderRegion;
	private MultiKeyMap<Object, Double> marriageTargetsByGenderAndRegion;
	
	private LinkedHashMap<String, Double>  marriageTargetsByKey;
	
	private long elapsedTime;

	private int year;

	private Set<BenefitUnit> benefitUnits;
	
	private Set<DonorHousehold> euromodOutputHouseholds;

	private Set<Household> households; //Set of households
	
	private Map<Gender, LinkedHashMap<Region, Set<Person>>> personsToMatch;
	
	private LinkedHashMap<String, Set<Person>> personsToMatch2; 

	private double ageDiffBound = Parameters.AGE_DIFFERENCE_INITIAL_BOUND;

	private double potentialEarningsDiffBound = Parameters.POTENTIAL_EARNINGS_DIFFERENCE_INITIAL_BOUND;
	
	private double scalingFactor;
	
	private Map<Long, LinkedHashSet<DonorPerson>> euromodOutputPersonsByHouseholdId;

//	private String EUROMODpolicyNameForThisYear;

	private MultiKeyMap<Object, Double> donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute;

	private Map<Long, Double> initialHoursWorkedWeekly;

	private LabourMarket labourMarket;

//	private Set<BenefitUnit> householdsList;

//	private Set<Person> personsList;
	
	public int tmpPeopleAssigned = 0;
	
	private Set<Person> clonedPersonsPopulationAlignment; //Create a set to store cloned persons
	
	private Set<Person> clonedChildren = new LinkedHashSet<Person>(); //Create a set to store cloned children

	public int lowEd = 0;
	public int medEd = 0;
	public int highEd = 0;
	public int nothing = 0;

	Map<String, Double> policyNameIncomeMedianMap = new LinkedHashMap<>(); // Initialise a <String, Double> map to store names of policies and median incomes

	private Tests tests;

	/**
	 *
	 * CONSTRUCTOR FOR SIMULATION PROJECTIONS
	 * @param country
	 * @param startYear
	 *
	 */
	public LABSimModel(Country country, int startYear) {
		super();
		this.country = country;
		this.startYear = startYear;
	}

	public LABSimModel(Country country) {
		super();
		this.country = country;
	}


	/**
	 *
	 * METHOD TO BUILD THE MODEL SO THAT IT CAN BE EXECUTED
	 * 
	 * This method is launched by JAS-mine when you press the 
	 * 'Build simulation model' button of the GUI 
	 *
	 */
	@Override
	public void buildObjects() {		

		// set seed for random number generator
		if (fixRandomSeed) {
			SimulationEngine.getRnd().setSeed(randomSeedIfFixed);
		}

		// load model parameters
		Parameters.loadParameters(country);
        log.debug("Parameters loaded.");


        // set start year for simulation
        year = startYear;
        // EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);

		//Display current contry and start year in the console
		System.out.println("Country: " + country + ". Start year of the simulation: " + startYear);

		// time check
		elapsedTime = System.currentTimeMillis();

        // create country-specific tables in the input database and parse the EUROMOD policy scenario data for initializing the donor population
        inputDatabaseInteraction();		

    //    System.out.println("Time to initialise database " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");

		// create donor population objects from tables in input database
		Map<Long, DonorPerson> euromodOutputPersons = createDonorPopulationDataStructures();

	//	System.out.println("Time to create donor population1 " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");

		// creates initial population (Person and BenefitUnit objects) based on data in input database.
		// Note that the population may be cropped to simulate a smaller population depending on user choices in the GUI.
		createInitialPopulationDataStructures();

		initialiseDonorIncomeStatistics(); // Calculate median income for each policy in the donor population and income threshold to use while matching donors

		//	System.out.println("Time to create donor population2 " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");

		// population alignment
		// only creates MAX_AGE + 1 - MIN_AGE entries, will not keep track of persons that become older than MAX_AGE
		personsByGenderRegionAndAge = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		for (Gender gender : Gender.values()) {
			for (Region region: Parameters.getCountryRegions()) {
				for (int age = Parameters.MIN_AGE; age <= Parameters.getMaxAge(); age++) {
					personsByGenderRegionAndAge.put(gender, region, age, new LinkedHashSet<Person>());
				}
			}
		}

	//	System.out.println("Time to initialise alignment structures " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");

		// matching for marriage unions
		personsToMatch = new LinkedHashMap<Gender, LinkedHashMap<Region, Set<Person>>>();
		for (Gender gender: Gender.values()) {
			personsToMatch.put(gender, new LinkedHashMap<Region, Set<Person>>());
			for (Region region: Region.values()) {
				personsToMatch.get(gender).put(region, new LinkedHashSet<Person>());
			}
		}
		
		personsToMatch2 = new LinkedHashMap<String, Set<Person>>();
		for (Gender gender : Gender.values()) {
			for (Region region : Region.values()) {
				for(Education education: Education.values()) {
					for(int ageGroup = 0; ageGroup <= 6; ageGroup++) { //Age groups with numerical values are created in Person class setAdditionalFieldsInInitialPopulation() method and must match Excel marriageTypes2.xlsx file. 
						String tmpKey = gender + " " + region + " " + education + " " + ageGroup;
						personsToMatch2.put(tmpKey, new LinkedHashSet<Person>());
					}
				}
			}
		}
		
		// For SBAM matching, initial targets:
		//marriageTargetsGenderRegion = new LinkedHashMap<Gender, LinkedHashMap<Region,Double>>();
		marriageTargetsByGenderAndRegion = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		for (Gender gender : Gender.values()) {
			for (Region region : Parameters.getCountryRegions()) {
				double tmpTarget = personsToMatch.get(gender).get(region).size();
				marriageTargetsByGenderAndRegion.put(gender, region, tmpTarget);
			}
		}
		
		marriageTargetsByKey = new LinkedHashMap<String, Double>();

	//	System.out.println("Time to initialise SBAM marriage structures " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		
		// LabourMarket constructor
//		Parameters.updateUnemploymentRate(year);
		// labourMarket = new LabourMarket(persons, benefitUnits, euromodOutputHouseholds, resetLabourDemand);
		labourMarket = new LabourMarket(persons, benefitUnits, euromodOutputHouseholds);

		// injection point to allow for intertemporal optimisation decisions
		// this needs to be after the labourMarket object is instantiated, to permit evaluation of taxes and benefits
		if (enableIntertemporalOptimisations) {
			ManagerPopulateGrids.run(this, useSavedBehaviour, fileNameForSavedBehaviour, employmentOptionsOfPrincipalWorker,
					employmentOptionsOfSecondaryWorker, intertemporalResponsesToHealthStatus, intertemporalResponsesToRegion,
					intertemporalResponsesToEducationStatus, startYear, endYear);
		}

		// earnings potential
		if (!initialisePotentialEarningsFromDatabase) {
			initialisePotentialEarningsByWageEquationAndEmployerSocialInsurance();
		}
		euromodOutputPersons = null;

		for (Person person: persons) {
		//	person.setYptciihs_dv(0);
			person.updateNonEmploymentIncome();
		}

		// initialise data that feature in the observer's charts
		for (BenefitUnit benefitUnit : benefitUnits) {
			if (benefitUnit.getAtRiskOfWork()) {
				// given current wage equation coefficients, benefitUnits update their supply of labour
				// This sets the benefitUnit fields such as disposable benefitUnit income, needed for the collector's equivalised
				// benefitUnit disposable income calculations that feature in the observer's charts.
				benefitUnit.updateLabourSupply();
			}
			else {
				// Benefit units not at risk of work do not enter the process of updating labour supply above, but disposable income based on their capital and pension income still needs to be calculated. This is done in the updateDisposableIncomeIfNotAtRiskOfWork process.
				benefitUnit.updateDisposableIncomeIfNotAtRiskOfWork();
			}
			// updateAggregateStatistics(benefitUnit, aggregateWeeklyGrossEarningsByEducation, aggregateWeeklyLabourCostsByEducation, aggregateWeeklyLabourSupplyByEducation);
		}

		/*
		for (Person person : persons) {
			person.updateVariables();
			person.updateNonEmploymentIncome();
			person.calculateConsumption();
		}
		*/


		// calculate the scaling factor for population alignment
		double popSizeBaseYear = 0;

		// iterate through gender, age, region to get the total population size
		for (Gender gender : Gender.values()) {
			for (Region region : Parameters.getCountryRegions()) {
				for (int age = Parameters.MIN_AGE; age < Parameters.getMaxAge(); age++) {
					popSizeBaseYear += ((Number)Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue();
				}
			}
		}
		scalingFactor = (double)popSizeBaseYear / (double)persons.size();


		System.out.println("Scaling factor is " + scalingFactor);

		//Set up tests class
		tests = new Tests();

		// finalise
		log.debug("Time to build objects: " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		System.out.println("Time to complete initialisation " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		elapsedTime = System.currentTimeMillis();


	}


	/**
	 *
	 * METHOD TO PROJECT THE POPULATION THROUGH TIME
	 *
	 * This method is run once for each simulated year after
	 * the simulation is launched by JAS-mine when you press the
	 * 'Start simulation' button of the GUI. This method defines 
	 * the order that simulated processes are executed. There are
	 * three key categories of processes:
	 *   Processes	 			These are processes applicable to the simulation population in aggregate
	 *	 Person.Processes		Defined in Person Class of model package. These modules define processes specific to simulated 'individuals'
	 *	 BenefitUnit.Processes	Defined in BenefitUnit Class of model package. These modules define processes specific to simulated 'benefitUnits'
	 *
	 * All processes are initialised as 'Events' within the JAS-mine simulation engine
	 *
	 */
	@Override
	public void buildSchedule() {

		EventGroup yearlySchedule = new EventGroup();
		
		yearlySchedule.addEvent(this, Processes.UpdateParameters);
//		yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);

		//1 - DEMOGRAPHIC MODULE
		// A: Ageing 
		
		yearlySchedule.addCollectionEvent(persons, Person.Processes.Ageing, false);		//Read only mode as agents are removed when they become older than Parameters.getMAX_AGE();

		yearlySchedule.addEvent(this, Processes.CheckForEmptyBenefitUnits);

		// B: Population Alignment - adjust population to projections by Gender and Age, and creates new population for minimum age
		yearlySchedule.addEvent(this, Processes.PopulationAlignment);

		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.Update);
//		yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);

		// C: Health Alignment - redrawing alignment used adjust state of individuals to projections by Gender and Age
		//Turned off for now as health determined below based on individual characteristics
//		yearlySchedule.addEvent(this, Processes.HealthAlignment); 
//		yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);		

		// D: Check whether persons have reached retirement Age
		yearlySchedule.addCollectionEvent(persons, Person.Processes.ConsiderRetirement, false);		//Read only mode as agents are removed if living in a household where all responsible adults are retired.
//		yearlySchedule.addEvent(this, Processes.Timer);

		//2 - EDUCATION MODULE
		// A: Check In School - check whether still in education, and if leaving school, reset Education Level
		yearlySchedule.addCollectionEvent(persons, Person.Processes.InSchool);
//		yearlySchedule.addEvent(this, Processes.Timer);

		// B: In School alignment
		yearlySchedule.addEvent(this, Processes.InSchoolAlignment);			// Only use if unusual distributions emerge, such as in Ireland and Sweden.

		yearlySchedule.addCollectionEvent(persons, Person.Processes.LeavingSchool); //Moved here so it happens after alignment

		// C: Align the level of education if required
		yearlySchedule.addEvent(this, Processes.EducationLevelAlignment);

		// 3 A: Homeownership status
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.Homeownership);

		//4 - HEALTH MODULE
		// 4A: Update Health - determine health (continuous) based on regression models: done here because health depends on education
		yearlySchedule.addCollectionEvent(persons, Person.Processes.Health);

		// 4B: Update mental health - determine (continuous) mental health level based on regression models
		yearlySchedule.addCollectionEvent(persons, Person.Processes.HealthMentalHM1); //Step 1 of mental health
		
		//5 - HOUSEHOLD COMPOSITION MODULE: Decide whether to enter into a union (marry / cohabitate), and then perform union matching (marriage) between a male and female

		// A: Update potential earnings so that as up to date as possible to decide partner in union matching.
		yearlySchedule.addCollectionEvent(persons, Person.Processes.UpdatePotentialEarnings);

		// B: Consider whether in consensual union (cohabiting)

		yearlySchedule.addCollectionEvent(persons, Person.Processes.ConsiderCohabitation);
		yearlySchedule.addEvent(this, Processes.ConsiderCohabitationAlignment);
		
		// C:	Marriage
		yearlySchedule.addEvent(this, Processes.UnionMatching);
//		yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);
//		yearlySchedule.addEvent(this, Processes.Timer);

		//6 - MATERNITY MODULE
		// A: For females, align to fertility targets and if chosen, give birth
		yearlySchedule.addEvent(this, Processes.FertilityAlignment);		//Align to fertility rates implied by projected population statistics.
		yearlySchedule.addCollectionEvent(persons, Person.Processes.GiveBirth, false);		//Cannot use read-only collection schedule as new born children cause concurrent modification exception.  Need to specify false in last argument of Collection event. 
//		yearlySchedule.addEvent(this, Processes.CheckForEmptyHouseholds);
//		yearlySchedule.addEvent(this, Processes.Timer);


		//7 - LABOUR MARKET MODULE
		yearlySchedule.addEvent(this, Processes.LabourMarketUpdate);

		//Assign benefit status to individuals in benefit units, from donors
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.ReceivesBenefits);

		// For benefit units not at risk of work, follow the process below to update disposable income
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.UpdateDisposableIncomeNotAtRiskOfWork);
//		yearlySchedule.addEvent(this, Processes.Timer);

		//8 - UPDATE CONSUMPTION FOR THE SECURITY INDEX
		yearlySchedule.addCollectionEvent(persons, Person.Processes.CalculateConsumption);

		//8B - UPDATE EQUIVALISED DISPOSABLE INCOME AND CALCULATE CHANGE SINCE LAST YEAR
		yearlySchedule.addCollectionEvent(benefitUnits, BenefitUnit.Processes.CalculateChangeInEDI);

		//4 C: - UPDATE MENTAL HELATH: STEP 2: modify the outcome of Step 1 depending on individual's exposures
		//TODO: Consider if collector event updating at risk of poverty status should be called here
		yearlySchedule.addCollectionEvent(persons, Person.Processes.HealthMentalHM2); //Step 2 of mental health. Depends on 6B above.

		//9 - END OF YEAR PROCESSES
		yearlySchedule.addEvent(this, Processes.CheckForEmptyBenefitUnits); //Check all household before the end of the year
		yearlySchedule.addEvent(tests, Tests.Processes.RunTests); //Run tests
		yearlySchedule.addEvent(this, Processes.UpdateYear);

		getEngine().getEventQueue().scheduleRepeat(yearlySchedule, startYear, ordering, 1.);
		
		//For termination of simulation
		int orderEarlier = -1;			//Set less than order so that this is called before the yearlySchedule in the endYear.
		SystemEvent end = new SystemEvent(SimulationEngine.getInstance(), SystemEventType.End);
		getEngine().getEventQueue().scheduleOnce(end, endYear+1, orderEarlier);
//		getEngine().getEventQueue().scheduleOnce(new SingleTargetEvent(this, Processes.Stop), endYear, orderEarlier);
		
		log.debug("Time to build schedule " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
		elapsedTime = System.currentTimeMillis();

	}


	/**
	 *
	 * METHOD DEFINING PROCESSES APPLICABLE TO THE SIMULATED POPULATION IN AGGREGATE
	 *
	 */
	public enum Processes {

		UnionMatching,
		LabourMarketUpdate,

		//Alignment Processes
		FertilityAlignment, 
		PopulationAlignment,
		ConsiderCohabitationAlignment,
//		HealthAlignment,
		InSchoolAlignment,
		EducationLevelAlignment,

		//Other processes
//		Stop, 
		Timer, 
		UpdateParameters,
		UpdateYear,
		CheckForEmptyBenefitUnits,
	}

	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {

			case PopulationAlignment:
				if (!useWeights) {
					populationAlignmentUnweighted();
				} else {
					populationAlignment();
				}
				if (commentsOn) log.info("Population alignment complete.");
				break;
			case ConsiderCohabitationAlignment:
				if (alignCohabitation && year == getStartYear()) {
					considerCohabitationAlignment();
				}
				if (commentsOn) log.info("Cohabitation alignment complete.");
				break;
//			case HealthAlignment:
//				healthAlignment();
//				if (commentsOn) log.info("Health alignment complete.");
//				break;
			case UnionMatching:
				if(unionMatchingMethod.equals(UnionMatchingMethod.SBAM)) {
					unionMatchingSBAM();
				}
				else if (unionMatchingMethod.equals(UnionMatchingMethod.Parametric)) {
					unionMatching();
				}
				else {
					unionMatching();
					unionMatchingNoRegion(); //Run matching again relaxing regions this time
				}
				if (commentsOn) log.info("Union matching complete.");
				break;
			case FertilityAlignment:
				if(alignFertility) {
					fertility(); //First determine which individuals should give birth according to our processes
					fertilityAlignment(); //Then align to meet the numbers implied by population projections by region
				} else {
					fertility(); 
				}
				if (commentsOn) log.info("Fertility alignment complete.");
				break;
			case InSchoolAlignment:
				if (alignInSchool) {
					inSchoolAlignment();
					System.out.println("Proportion of students will be aligned.");
				}
				break;
			case EducationLevelAlignment:
				if (alignEducation) {
					educationLevelAlignment();
					System.out.println("Education levels will be aligned.");
				}
				break;
			case LabourMarketUpdate:
				labourMarket.update(year);
				if (alignEmployment) {
					employmentAlignment(); //Align employment share
				}
				if (commentsOn) log.info("Labour market update complete.");
	//		case Stop:
	//			log.debug("Simulation completed");
	//			log.debug("Simulation completed " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
	//			getEngine().pause();
	//			break;
			case Timer:
				printElapsedTime();
				break;
			case UpdateParameters:
				updateParameters();
				if (commentsOn) log.info("Update Parameters Complete.");
				break;
			case UpdateYear:
				if (commentsOn) log.info("It's the New Year's Eve of " + year);
				Person.countOK = 0;
				Person.countNaN = 0;
				Person.countOKover32 = 0;
				year++;
				break;
			case CheckForEmptyBenefitUnits:

				List<BenefitUnit> irresponsibleHouses = new ArrayList<BenefitUnit>();
				for (BenefitUnit benefitUnit: benefitUnits) {
					if (benefitUnit.getMale() == null && benefitUnit.getFemale() == null) {
						irresponsibleHouses.add(benefitUnit);
					}
				}
				for (BenefitUnit house: irresponsibleHouses) {
					log.warn("house " + house.getKey().getId() + ", children size " + house.getChildren().size() + ", otherMembers size " + house.getOtherMembers().size() + " house size " + house.calculateSize());
					for (Person person: house.getOtherMembers()) {
						log.warn("person " + person.getKey().getId() + ", age " + person.getDag() + ", is in otherMembers");
					}
					for (Person person: house.getChildren()) {
						log.warn("person " + person.getKey().getId() + ", age " + person.getDag() + ", is in children");
					}
				}
				
				for (BenefitUnit benefitUnit : benefitUnits) {
					if (benefitUnit.getSize() <= 0) {
						removeBenefitUnit(benefitUnit);
					}
				}

				for (BenefitUnit bu : irresponsibleHouses) {
					Set<Person> personsInIrresponsibleHouse = new LinkedHashSet<>();

					for (Person child : bu.getChildren()) {
						personsInIrresponsibleHouse.add(child);
					}

					for (Person other : bu.getOtherMembers()) {
						personsInIrresponsibleHouse.add(other);
					}

					for (Person person : personsInIrresponsibleHouse) {
							bu.removePerson(person);
					}
					removeBenefitUnit(bu);

				}
				
				break;
			default:
				break;
		}
	}


	/**
	 *
	 * METHODS IMPLEMENTING PROCESS LEVEL COMPUTATIONS
	 *
	 */


	/**
	 *
	 * PROCESS - POPULATION ALIGNMENT WHERE POPULATION IS WEIGHTED
	 *
	 */
	@SuppressWarnings("unchecked")
	private void populationAlignment() {
		
		MultiKeyMap<Object, Double> weightsByGenderRegionAndAge = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		
		//Update Age index of personsByGenderRegionAndAge
		for (Gender gender : Gender.values()) {
			for (Region region: Parameters.getCountryRegions()) {
				for (int age = Parameters.MIN_AGE; age <= Parameters.getMaxAge(); age++) {
					((Set<Person>) personsByGenderRegionAndAge.get(gender, region, age)).clear();
				}
			}
		}
		for (Person person : persons) {
//			log.debug("Gender, " + person.getGender() + ", Region, " + person.getRegion() + ", Age, " + person.getAge());
			((Set<Person>) personsByGenderRegionAndAge.get(person.getDgn(), person.getRegion(), person.getDag())).add(person);		//Update
		}

		//Calculate Weights
		for (Gender gender : Gender.values()) {
			for (Region region: Parameters.getCountryRegions()) {
				for (int age = Parameters.MIN_AGE; age <= Parameters.getMaxAge(); age++) {
					double weight = ((Number)Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue() / ((double)((Set<Person>) personsByGenderRegionAndAge.get(gender, region, age)).size());
					weightsByGenderRegionAndAge.put(gender, region, age, weight);
//					log.debug("Gender " + gender + ", Region " + region + ", Age " + age + ", Weight " + weight);
				}
			}
		}

		// re-weight simulation
		for (Person person : persons) {
			Gender gender = person.getDgn();
			Region region = person.getRegion();
			int age = person.getDag();
			person.setWeight(((Number)weightsByGenderRegionAndAge.get(gender, region, age)).doubleValue());
//			log.debug("Person " + person.getKey().getId() + ", gender " + gender + ", region " + region + ", age " + age + " has Weight = " + person.getWeight());
		}
	}


	/**
	 *
	 * PROCESS - POPULATION ALIGNMENT WHERE POPULATION IS UNWEIGHTED
	 *
	 */
	@SuppressWarnings("unchecked")

	private void populationAlignmentUnweighted() {


		//Clear the set of cloned individuals (this is used to keep track of cloned individuals to create benefitUnits). These are added in createOrRemovePersons method.
		clonedPersonsPopulationAlignment = new LinkedHashSet<Person>();
		
		Set<Person> clonedMothers = new LinkedHashSet<Person>(); //Create a set to store cloned mothers added from persons set
		Set<Person> potentialMothers = new LinkedHashSet<>(); //Create a set to store not-cloned mothers from persons set
		
		//Update Age index of personsByGenderRegionAndAge
		for (Gender gender : Gender.values()) {
			for (Region region : Parameters.getCountryRegions()) {
				for (int age = Parameters.MIN_AGE; age < Parameters.getMaxAge(); age++) {
					((Set<Person>) personsByGenderRegionAndAge.get(gender, region, age)).clear();
				}
			}
		}
		
		/*
		for(BenefitUnit household : benefitUnits) {
			if (household.getRegion() == null) {
				System.out.println("Region missing for HH " + household.getKey().getId());
			}
		}
		*/
		
		for (Person person : persons) {
				((Set<Person>) personsByGenderRegionAndAge.get(person.getDgn(), person.getBenefitUnit().getRegion(), person.getDag())).add(person);		//Update
		}

		//Not performing population alignment for the population of age 0 because this is handled through fertility alignment
		int minimumAge = Parameters.MIN_AGE;
		if(minimumAge == 0) {
			minimumAge = 1;
		}

		//Creating new persons by gender, region, age
		for (Gender gender : Gender.values()) {
			for (Region region : Parameters.getCountryRegions()) {
				for (int age = minimumAge; age < Parameters.getMaxAge(); age++) {
					double doubleTargetByGenderRegionAndAge = ((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue() / scalingFactor;
					int targetByGenderRegionAndAge = (int) Math.round(doubleTargetByGenderRegionAndAge);
//					int targetByGenderRegionAndAge = (int) (((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue() / scalingFactor);
					
					int numPersonsInSimOfThisGenderRegionAndAge = personsByGenderRegionAndAge.get(gender, region, age).size();
//					System.out.println("For gender: " + gender + "region: " + region + "and age: " + age +"there are " + numPersons + "people. Target for that group is " + targetByGenderRegionAndAge);
					//If no persons to copy, the age can be relaxed to find someone similar. But what if alignment suggests we set the number of people with some characteristics to 0? Should we still align in such case?
					if (targetByGenderRegionAndAge > 0) { //The condition was added to only align where possible, but instead should relax age within +- 5 years until a person to copy is found.
						createOrRemovePersons(gender, region, age, numPersonsInSimOfThisGenderRegionAndAge, targetByGenderRegionAndAge);
					}
					
					
				}
			}
		}
		
		
		/*After individuals have been added / removed, consider the household structure:
		 * 1. Individuals added in the population alignment process have a cloned flag set to true
		 * 2. Cloning creates a set of single individuals and children - we need to create benefitUnits
		 * 3. If cloned individual was partnered, put straight into personsToMatch set, skipping consider cohabitation process
		 * 4. If individual was single, follow normal consider cohabitation process
		 * 5. Cloned children should be assigned to benefitUnits with cloned mothers who had children in the original data, in such a way that first the 1st child gets assigned to all benefitUnits
		 * 		who have at least 1 child, then the 2nd, etc. 
		 *  
		 */
		
		/*
		Iterator<Person> testit = persons.iterator();
    	while (testit.hasNext()) {
			Person person = (Person) testit.next();
			System.out.println("HHID is " + person.getHouseholdId() + "PID is " + person.getKey().getId() + ". Original ID was " + person.getOriginalID() + " and original HHID was " + person.getOriginalHHID() + "Partner ID is " + person.getPartnerId() + "Mother Id is " + person.getMotherId() + "Father Id is " + person.getFatherId() + "Gender is " + person.getGender() + "Region of household is " + person.getHousehold().getRegion() + "Number of children in the HH " + person.getHousehold().getChildren().size() +
								"Responsible male " + person.getHousehold().getMaleId() + " Responsible female " + person.getHousehold().getFemaleId() + " Cloned? " + person.isClonedFlag());
		}
		*/
		
    	//For each of the cloned individuals decide if they should go to cohabitation, personsToMatch, or are children
		
    			int tmpClonedPersonsRemaining = clonedPersonsPopulationAlignment.size();
				int tmpClonedMothersChildrenSum = 0; //Store sum of original children of cloned mothers

				Set<Person> tmpPersonsWithPartner = new LinkedHashSet<>();

    			for(Person person : clonedPersonsPopulationAlignment) {
    				if(person.getDag() >= Parameters.MIN_AGE_COHABITATION) { //If above age to create HH, consider which process should be followed

    					//Temporary test set to hold persons who had a partner before they were cloned in the population alignment
    					if (person.getOriginalPartner() != null) {
    						tmpPersonsWithPartner.add(person);
						}

    					if(person.getOriginalHHStatus().equals(Household_status.Couple) && person.getDag() <= Parameters.getFixedRetireAge(year, person.getDgn())) {
    						personsToMatch.get(person.getDgn()).get(person.getBenefitUnit().getRegion()).add(person); //If cloned person was in a couple and is below retirement age, skip considerCohabitation and add directly to matching pool
    						tmpClonedPersonsRemaining --; 
    					} 
    					
    					else if(person.getOriginalHHStatus().equals(Household_status.Single)) {
    						//Singles go to considerCohabitation according to the yearly schedule
    						tmpClonedPersonsRemaining --;
    					}
    				} 
    				else if(person.getDag() < Parameters.MIN_AGE_COHABITATION) {

    					clonedChildren.add(person);
    					tmpClonedPersonsRemaining--;
    				}
    			}

    			//Populate the set of cloned mothers based on cloned status, gender, and number of original children
				for(Person person : persons) {
					//If cloned person is a female and had originally more children than currently, add to a set of potential mothers
					if(person.isClonedFlag() && person.getDgn().equals(Gender.Female) && person.getDag() >= Parameters.MIN_AGE_COHABITATION && (person.getOriginalNumberChildren() > person.getBenefitUnit().getN_children_allAges())) {
						clonedMothers.add(person);
						tmpClonedMothersChildrenSum += person.getOriginalNumberChildren();
					}
					else if (person.getDgn().equals(Gender.Female) && person.getDag() >= Parameters.MIN_AGE_COHABITATION && person.getBenefitUnit().getN_children_allAges() > 0) {
						potentialMothers.add(person);
					}
				}

				//If the sum of original children of cloned mothers is lower than the number of cloned children, add some "normal" (not cloned) mothers to the set
				int tmpDifferenceChildrenMothers = clonedChildren.size() - tmpClonedMothersChildrenSum;

				TreeSet<Integer> randomIndices = new TreeSet<>();
				int tmpCount = 0;

				while (tmpCount < tmpDifferenceChildrenMothers) {
					if (randomIndices.add(SimulationEngine.getRnd().nextInt(potentialMothers.size()))) {
						tmpCount++; //This could be the count of children of added mothers instead of just number of mothers
					}
					if (tmpCount >= potentialMothers.size()) {
						break;
					}
				}

				List<Person> potentialMothersList = new LinkedList<>(potentialMothers);
				//Add the number of randomly selected "normal" mothers to the set of cloned mothers so cloned children can be assigned
				randomIndices.forEach(index -> clonedMothers.add(potentialMothersList.get(potentialMothers.size() - 1 - index)));

    			
    			//Sort the list of cloned mothers on the original number of children:
    			
    			List<Person> clonedMothersList = new LinkedList<Person>(clonedMothers);
    			List<Person> clonedChildrenList = new LinkedList<Person>(clonedChildren);
    			
    			clonedMothersList.sort(Comparator.comparing(Person::getOriginalNumberChildren));
    			ListIterator<Person> clonedChildrenIterator = clonedChildrenList.listIterator();
    			ListIterator<Person> clonedMothersIterator = clonedMothersList.listIterator();

    			/*
    			It seems that there are some unassigned cloned children, because the adjsutment is much larger than the number of cloned mothers.
    			If the origial number of children that the cloned mothers have is smaller than the number of cloned children to assign,
    			add some not-cloned mothers to the set of cloned mothers to whom children can be assigned?

    			 */
    			
    			while(clonedChildrenIterator.hasNext()) {

					Person child = clonedChildrenIterator.next();
					Person mother = null;

    				if (clonedMothersIterator.hasNext()) { //If there are mothers remaining on the list take next
    					mother = clonedMothersIterator.next();
					} else { //Otherwise, reset the iterator to the beginning
    					clonedMothersIterator = clonedMothersList.listIterator();
						if (clonedMothersIterator.hasNext()) {
							mother = clonedMothersIterator.next();
						} else { //If nothing left, break out of the while loop
							break;
						}
					}



    				
//    				System.out.println("Will attempt to add child " + child.getKey().getId() + "aged " + child.getAge() + " To mother with ID " + mother.getKey().getId() 
//   									+ " From HH " + mother.getHousehold().getKey().getId() + " Which currently has " + mother.getHousehold().getN_children_allAges());

					if (mother.getDag() >= child.getDag()+Parameters.MIN_AGE_MATERNITY) {

						child.setId_mother(mother.getKey().getId());
						child.setBenefitUnit(mother.getBenefitUnit());
						child.setId_father(mother.getId_partner());
						mother.getBenefitUnit().addChild(child);
//    				mother.getHousehold().initializeFields();
						mother.getBenefitUnit().updateChildrenFields();
						mother.getBenefitUnit().updateOccupancy();

//    				System.out.println("Child " + child.getKey().getId() + " has been added to HH " + child.getHousehold().getKey().getId() + " and mother " + child.getMotherId());

						if (mother.getBenefitUnit().getN_children_allAges() >= mother.getOriginalNumberChildren()) {
							clonedMothersIterator.remove();
							clonedMothers.remove(mother);
//    					System.out.println("There are " + clonedMothersList.size() + " cloned mothers left");
						}

						persons.add(child);
						clonedChildrenIterator.remove();
						clonedChildren.remove(child);
					}

    			}
    			
//    			System.out.println("Number of unassigned cloned people remaining = " + tmpClonedPersonsRemaining);
    			
    			
    	/*
		Iterator<BenefitUnit> testitHH = benefitUnits.iterator();
		while(testitHH.hasNext()) {
			BenefitUnit household = (BenefitUnit) testitHH.next();
			System.out.println("HHID is " + household.getKey().getId());
		}
		*/

		
//		System.out.println("Unweighted population alignment done");
		
	}
	
	//Method to add or remove desired number of individuals of some gender, region, and age:
	@SuppressWarnings("unchecked")
	protected int createOrRemovePersons(Gender gender, Region region, int age, int numPersonsInSimOfThisGenderRegionAndAge, int targetNum) {
		if(numPersonsInSimOfThisGenderRegionAndAge <= 0) {
//			throw new IllegalArgumentException("Number of " + gender.toString() + "s of Age " + age + " in simulation is not positive!  Impossible to clone to create more persons of same Age and Gender!");
		} else if (targetNum <= 0) {  
			throw new IllegalArgumentException("Target number of " + gender.toString() + "s of Age " + age + "from region " + region.toString() + " is not positive!  This would lead to lack of population for this Age and Gender in this Region, making it impossible to clone from this demography class in future!");
		}
		int changeInAgentsOfThisGenderAndAge = 0;		
		int numPersonsInSimMinusTarget = numPersonsInSimOfThisGenderRegionAndAge - targetNum;
		
		List<Person> personsByGenderRegionAndAgeList = new LinkedList<>(personsByGenderRegionAndAge.get(gender, region, age));
		
		//First deal with the case of too many people

		int difference = 0;
		/*
		if (year <= startYear) {
			difference = 1;
		}
		*/
		if (numPersonsInSimMinusTarget > difference) {
			
			TreeSet<Integer> randomIndices = new TreeSet<Integer>();
			int tmpCount = 0;
			boolean test;
			while (tmpCount < numPersonsInSimMinusTarget) {
				do {
					test = randomIndices.add(SimulationEngine.getRnd().nextInt(numPersonsInSimOfThisGenderRegionAndAge));
					if (test) {
						tmpCount++;
					}
				} while(!test);
			}
			
			for(Integer randomIndex: randomIndices) {

				Person person = personsByGenderRegionAndAgeList.get(numPersonsInSimOfThisGenderRegionAndAge - 1 - randomIndex);

//				Person person = ((List<Person>) personsByGenderRegionAndAge.get(gender, region, age)).get(numPersonsInSimOfThisGenderRegionAndAge - 1 - randomIndex);
				if (removePerson(person)) {
					//If person being removed was in a couple, partner should be rematched
					Person partnerOfRemovedPerson = person.getPartner();
					if (partnerOfRemovedPerson != null) {
						personsToMatch.get(partnerOfRemovedPerson.getDgn()).get(partnerOfRemovedPerson.getRegion()).add(partnerOfRemovedPerson);
					}

					BenefitUnit benefitUnit = person.getBenefitUnit();
					benefitUnit.removePerson(person); //Removing person from benefitUnit checks for occupancy and will delete benefitUnit if empty
					changeInAgentsOfThisGenderAndAge--;
				}
			}
		} else if (numPersonsInSimMinusTarget < 0) { //If there are too few persons in the simulation, create new persons
			
			//If new persons are to be created but there is no one to copy from add more people by relaxing age condition
			if ((numPersonsInSimOfThisGenderRegionAndAge <= 0) && (targetNum > 0)) { //If the number of people in the simulation is not positive, but the target is positive
//				System.out.println("For " + gender + "living in " + region + "of age " + age + "there are " + numPersonsInSimOfThisGenderRegionAndAge + "persons in the simulation. Target is " + targetNum + "Need to add people.");
				
				//Create a temporary list of people to add
				List<Person> tmpPersonsToAdd = new ArrayList<Person>();
				
				int i = 1;
				int ageTolerance = 20; //Relax age by +-5. If no persons to copy from found despite that, will throw an exception.
				
				//Relax age by 1 year each time until successfully added some people to the temporary list of people to add
				while (i <= ageTolerance) {
//					System.out.println("Relaxing age by " + i);
					boolean success = false;
					if(personsByGenderRegionAndAge.get(gender, region, age+i) != null) {
						success = tmpPersonsToAdd.addAll(personsByGenderRegionAndAge.get(gender, region, age+i));
					}

					if (success && tmpPersonsToAdd.size() >= targetNum) {
//						System.out.println("Added persons with age higher by" + i);
						//Successfully added some people so no need to try to add more
						break;
					} else if (age - i >= 0) { //Cannot try to add people with negative age
						boolean success2 = tmpPersonsToAdd.addAll(personsByGenderRegionAndAge.get(gender, region, age-i));
						if (success2 && tmpPersonsToAdd.size() >= targetNum) {
//							System.out.println("Added persons with age lower by " + i);
							break;
						}
					}
					i++;
				}
				
//				System.out.println("There are now " + tmpPersonsToAdd.size() + "people on the list");
				if (tmpPersonsToAdd.size() == 0) {
					throw new IllegalArgumentException("Despite relaxing age couldn't find people to copy from - consider increasing the population size.");
				}
				personsByGenderRegionAndAgeList.addAll(tmpPersonsToAdd); //Add all the new people to the original list of people with desired characteristics
				numPersonsInSimOfThisGenderRegionAndAge += tmpPersonsToAdd.size(); //Increase the number of people of desired characteristic by the newly found persons with relaxed age
			}
			
			
			
			ArrayList<Integer> randomIndices = new ArrayList<Integer>(((int) - numPersonsInSimMinusTarget)+1);
			int tmpcount = 0;
			while(tmpcount < -numPersonsInSimMinusTarget) {
				randomIndices.add(SimulationEngine.getRnd().nextInt(numPersonsInSimOfThisGenderRegionAndAge)); //Problem is there are groups with 0 people in the simulation and a positive target - how to deal with that?
				tmpcount++;
			}
			
			for (Integer randomIndex : randomIndices) {
				
//				Person person = ((List<Person>) personsByGenderRegionAndAge.get(gender, region, age)).get(randomIndex);
				Person person = personsByGenderRegionAndAgeList.get(randomIndex);
//				System.out.println("Person is " + person.getKey().getId() + "of region " + person.getRegion());
				Person newPerson = new Person(person, true); //Pass a true flag to use a constructor for alignment
				newPerson.setDag(age); //Added 04/08/2020: set age to what the alignment is for, not the actual copied age?
				newPerson.updateVariables();

				//Create a most basic new person and fill in the information from old person here?
//				Person newPerson = new Person(Person.personIdCounter++);

				if(newPerson.getDag() < Parameters.AGE_TO_BECOME_RESPONSIBLE) { //If cloned person is below age to set up a HH/BU, they should not set up a new HH/BU
					changeInAgentsOfThisGenderAndAge++;
					newPerson.setDed(Indicator.True);
					newPerson.setLes_c4(Les_c4.Student);
//					System.out.println("Added a person "+ newPerson.getKey().getId() +" without household, since person is a child");
					clonedPersonsPopulationAlignment.add(newPerson); //Add only to cloned persons and add to persons set when mother has been assigned
					personsByGenderRegionAndAgeList.add(newPerson);
				
			} else {
				BenefitUnit newBenefitUnit = new BenefitUnit(newPerson, person.getRegion()); //Create a new benefit unit based on person's old benefit unit
				newPerson.setBenefitUnit(newBenefitUnit);
				newBenefitUnit.initializeFields();
//				newBenefitUnit.updateChildrenFields();
				newBenefitUnit.updateOccupancy();

				//New benefit unit must have a household created for it as well
				Household newHousehold = new Household();
				newHousehold.addBenefitUnitToHousehold(newBenefitUnit);

//            	System.out.println("PID was " + person.getKey().getId() + "and the one set in constructor is  " + newPerson.getOriginalID() + ". New HHID is " + newPerson.getHouseholdId() + "and new PID is " + newPerson.getKey().getId() + " Partner ID is " + newPerson.getPartnerId() + 
//						" Size of the children set in new HH is " + newBenefitUnit.getChildren().size());
				
//				boolean success = ((List<Person>) personsByGenderRegionAndAge.get(gender, region, age)).add(newPerson);
				boolean success = personsByGenderRegionAndAgeList.add(newPerson); //TODO: Is this correct? Should the new person be added to the list from which we sample?
				if(success && addPerson(newPerson) && benefitUnits.add(newBenefitUnit) && households.add(newHousehold)) {
					changeInAgentsOfThisGenderAndAge++;
//					System.out.println("Added a person "+ newPerson.getKey().getId() +" and household");
//					System.out.println("There are now " + persons.size() + "persons and " + benefitUnits.size() + "benefitUnits in the simulation");
					clonedPersonsPopulationAlignment.add(newPerson);
				}
			}
		}
			
	}
		
		return changeInAgentsOfThisGenderAndAge;
	}
	


	/**
	 *
	 * PROCESS - HEALTH ALIGNMENT OF SIMULATED POPULATION
	 *
	 */
	/*
	//TODO: The health alignment might have to be handled differently with continuous health
	private void healthAlignment() {
		
		for (Gender gender: Gender.values()) {
			for (int age = Parameters.MIN_AGE_TO_ALIGN_HEALTH; age <= Parameters.getFixedRetireAge(year, gender); age++) {
				
				//Target proportion
				double proportionWithBadHealth = ((Number)Parameters.getProbSick().get(gender, age)).doubleValue();
				
				//People to be aligned in gender-age specific cell
				Set<Person> personsWithGenderAndAge = new LinkedHashSet<Person>();
				for (Region region: Parameters.getCountryRegions()) {
					personsWithGenderAndAge.addAll(personsByGenderRegionAndAge.get(gender,  region,  age));
				}
				
				//Align
				new ResamplingWeightedAlignment<Person>().align(
						personsWithGenderAndAge, 
						null,
						new AlignmentOutcomeClosure<Person>() {

							@Override
							public boolean getOutcome(Person agent) {
								return agent.getDhe() == 1.; //TODO: Check the new continuous health status 
							}

							@Override
							public void resample(Person agent) {	
								//Swap health status
								if (agent.getDhe() > 1.) {
									agent.setDhe(1.);
								} else {
									agent.setDhe(3.); //TODO: What numerical value should correspond to "good" health?
								}
							}
							
						},
						proportionWithBadHealth
				);
			}						
		}
	}
	*/
	
	/* 
	 * unionMatchingSBAM implements a marriage matching method presented by Stephensen 2013.
	 */

	int partnershipsCreated = 0;
	int malesUnmatched = 0;
	int femalesUnmatched = 0;

	@SuppressWarnings("unchecked")
	private void unionMatchingSBAM() {

		int malesToBePartnered = 0;
		int femalesToBePartnered = 0;
		partnershipsCreated = 0;

		for (Person p : persons) {
			if (p.isToBePartnered() == true) {
				if (p.getDgn().equals(Gender.Male)) {
					malesToBePartnered++;
				}
				else if (p.getDgn().equals(Gender.Female)) {
					femalesToBePartnered++;
				}
			}
		}

//		System.out.println("Number of males to be partnered is " + malesToBePartnered + " , number of females to be partnered is " + femalesToBePartnered);


		/* If adjustZeroEntries = true, zero frequencies are set to a very small number (1.e-6) for combinations of types that theoretically could occur. This are unlikely to result in any actual matches,
		 * as they are set to nearest integer, but allow the matches we are interested in to be adjusted. (One possibility is to introduce matching with probability equal to the frequency for such combinations). 
		 */
		boolean adjustZeroEntries = true;
		
		//1. Load distribution of marriages observed in Excel using ExcelLoader from marriageTypes2.xlsx file. Store a copy in marriageTypesToAdjust, which is 
		// a MultiKeyCoefficientMap - it has 2 string keys that identify a value. 1st key is person type, 2nd key is partner type, value is the number of marriages between these types
		// observed in the data. 
		MultiKeyCoefficientMap marriageTypesToAdjustMap = Parameters.getMarriageTypesFrequency().clone(); //Clone the original map loaded from Excel to adjust frequencies on a copy
		
		//Create a set of keys on which the types are defined: currently Gender, Region, Education, Age Group
		Set<MultiKey> keysMultiKeySet = new LinkedHashSet<MultiKey>();
		Set<String> keysStringSet = new LinkedHashSet<String>();
		
		for(Gender gender : Gender.values()) {
			for(Region region : Parameters.getCountryRegions()) {
				
				
				Set<Person> tmpPersonsSet = new LinkedHashSet<Person>(); 
				tmpPersonsSet.addAll(personsToMatch.get(gender).get(region)); //Using currently defined process for cohabitation, add all people who want to match to a set
				
				
				//The set of people who want to match can be further divided based on observables, e.g. we include Education. This must match the Excel file with frequencies, also in order of variables
				for (Education education : Education.values()) {
					for(int ageGroup = 0; ageGroup <= 11; ageGroup++) {
					
					
					
					Set<Person> tmpPersonsSet2 = new LinkedHashSet<Person>(); //Add to this set people from tmpPersonsSet selected on further observables
					String tmpKeyString = gender + " " + region + " " + education + " " + ageGroup; //MultiKey defined above, but for most methods we use a composite String key instead as MultiKeyMap has a limit of keys
					
					for (Person person : tmpPersonsSet) {
						if (person.getDeh_c3().equals(education) && person.getAgeGroup() == ageGroup) {
							tmpPersonsSet2.add(person); //If education level matches add person to the set
						}
					}
					
					personsToMatch2.put(tmpKeyString, tmpPersonsSet2); //Add a key and set of people to set of persons to match. Each key corresponds to a set of people of certain Gender, Region, and Education who want to match
					
					//Now add the number of people to match for gender, region, education as target
					double tmpTargetDouble = personsToMatch2.get(tmpKeyString).size();

					//Create a set containing row keys of marriageTypesToAdjust:
					Set<String> tmpKeysStringSet = new LinkedHashSet<String>();
					
					MapIterator frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
					while (frequenciesIterator.hasNext()) {
						 frequenciesIterator.next();
						 MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
						 String key0String = tmpKeyMultiKey.getKey(0).toString();
						 tmpKeysStringSet.add(key0String); //The only types not in the set should be those that don't have any matches in the data
					}
					
					
					if(tmpKeysStringSet.contains(tmpKeyString)) { //Check if the target is contained in frequencies from the data - if not, 0 entries cannot be adjusted anyway
						marriageTargetsByKey.put(tmpKeyString, tmpTargetDouble); //Update marriageTargetByKey
						
						MultiKey tmpKeyMultiKey = new MultiKey(gender, region, education, ageGroup);
						keysMultiKeySet.add(tmpKeyMultiKey); //Add MultiKey to set of keys
						keysStringSet.add(tmpKeyString);
						
					}
					
					
				}
			}
				
				
				
			}
		}

		
		//For sparse matrix with only few positive entries, convergence is not very good. One way to deal with it is to use very small numbers instead of 0 for matches that 
		//theoretically could occur? (i.e. no same sex matches, and no cross-region matches) but were not observed in the data. 
		if(adjustZeroEntries) {
			for (MultiKey key1 : keysMultiKeySet) { //For each row in the frequency matrix
				Gender gender1 = (Gender) key1.getKey(0);
				Region region1 = (Region) key1.getKey(1);
				Education education1 = (Education) key1.getKey(2);
				int ageGroup1 = (int) key1.getKey(3);
				
				String key1String = gender1 + " " + region1 + " " + education1 + " " + ageGroup1;
				
//				System.out.println();
				
				for(MultiKey key2 : keysMultiKeySet) { //For each column 
				
					Gender gender2 = (Gender) key2.getKey(0);
					Region region2 = (Region) key2.getKey(1);
					Education education2 = (Education) key2.getKey(2);
					int ageGroup2 = (int) key2.getKey(3);
					
					String key2String = gender2 + " " + region2 + " " + education2 + " " + ageGroup2;
					
					if(marriageTypesToAdjustMap.get(key1String, key2String) != null) { //Value present, do nothing
						
					} else if(!key1String.equals(key2String))  { //Null value, if not the same type, set to small number
							marriageTypesToAdjustMap.put(key1String, key2String, 1.e-6);
							} 
//							else {
//								marriageTypesToAdjust.put(key1String, key2String, 0.); //Same sex and cross-region matches to have 0 frequency -> this is now handled in the matching closure, by not matching such couples
//							}
					
//					System.out.print(marriageTypesToAdjust.get(key1String, key2String) + " ");
					
				}
		}
	}
		
		
		//Iterate as on a matrix until the cumulative difference between frequencies in marriageTypesToAdjust and the targets from marriageTargetsByKey is smaller than the specified precision:
		int tmpCountInt = 0;
		double errorDouble = Double.MAX_VALUE;
		double precisionDouble = 1.e-1;
				
		while ((errorDouble >= precisionDouble) && tmpCountInt < 10)  { //100 iteration should be enough for the algorithm to converge, but this can be relaxed
				
		errorDouble = 0.;
		
		//These maps will hold row and column sums (updated in each iteration)
		LinkedHashMap<String, Double> rowSumsMap = new LinkedHashMap<String, Double>(); 
		LinkedHashMap<String, Double> colSumsMap = new LinkedHashMap<String, Double>();
		
		//These maps will hold row and column multipliers (updated in each iteration, and defined as Target/Sum_of_frequencies) 
		LinkedHashMap<String, Double> rowMprMap = new LinkedHashMap<String, Double>();
		LinkedHashMap<String, Double> colMprMap = new LinkedHashMap<String, Double>();
		
		
		//Instead of iterating through rows and columns, go through every element of the map and add to the row / col sum depending on key1 and key2
		//marriageTypesToAdjust is a map, where key is a MultiKey with two values (Strings): first value identifies one type, second value identifies second type, value stores the frequency of matches. 
		//Instead of iterating through rows and columns, can iterate through each cell of the map and add it to rowSum (and later on to colSum).
		MapIterator frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
		
		while (frequenciesIterator.hasNext()) {
			frequenciesIterator.next();
			MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey(); //Get MultiKey identifying each cell (mk.getKey(0) is row, mk.getKey(1) is column)
			
			double tmpValueDouble = 0.;
			if (rowSumsMap.get(tmpKeyMultiKey.getKey(0).toString()) == null) { //If null value in rowSumsMap, then just put the current value, otherwise add 
				tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
			} else {
				tmpValueDouble = rowSumsMap.get(tmpKeyMultiKey.getKey(0).toString()) + ((Number) frequenciesIterator.getValue()).doubleValue();
			}
			
			//To get row sums add value to a map where key0 is the key
			rowSumsMap.put(tmpKeyMultiKey.getKey(0).toString(), tmpValueDouble);

		}
		
		//Get target by key and divide by row sum for that key to get row multiplier, same for column later on
		marriageTargetsByKey.keySet().iterator().forEachRemaining(key -> rowMprMap.put(key, marriageTargetsByKey.get(key)/rowSumsMap.get(key)));
		
		//After the first iteration, rowSum might = 0 which means division is undefined resulting in null rowMpr entry - adjust to 0 if that happens
		
		rowMprMap.keySet().iterator().forEachRemaining(key -> {
			if(rowMprMap.get(key).isNaN()) {
				rowMprMap.put(key, 0.);
			}
			if(rowMprMap.get(key).isInfinite()) {
				rowMprMap.put(key, 0.);
			}
		});
		
		
		//Now knowing the row multiplier, multiply entries in the frequency map (marriageTypesToAdjust)
		frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
		while (frequenciesIterator.hasNext()) {
			 frequenciesIterator.next();
			 MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
				
			 double tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
			 tmpValueDouble *= rowMprMap.get(tmpKeyMultiKey.getKey(0).toString());	
			 frequenciesIterator.setValue(tmpValueDouble);
			
		}
		
		//Have to repeat for columns:
		frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
		while (frequenciesIterator.hasNext()) {
			frequenciesIterator.next();
			MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
			
			double tmpValueDouble = 0.;
			if (colSumsMap.get(tmpKeyMultiKey.getKey(1).toString()) == null) {
				tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
			} else {
				tmpValueDouble = colSumsMap.get(tmpKeyMultiKey.getKey(1).toString()) + ((Number) frequenciesIterator.getValue()).doubleValue();
			}
			
			//To get column sums add value to a map where key1 is the key
			colSumsMap.put(tmpKeyMultiKey.getKey(1).toString(), tmpValueDouble);
		}
		
		
		marriageTargetsByKey.keySet().iterator().forEachRemaining(key -> colMprMap.put(key, marriageTargetsByKey.get(key)/colSumsMap.get(key)));
	
		//As for rows, make sure multipliers are defined
		
		colMprMap.keySet().iterator().forEachRemaining(key -> {
			if(colMprMap.get(key).isNaN()) {
				colMprMap.put(key, 0.);
			}
			if(colMprMap.get(key).isInfinite()) {
				colMprMap.put(key, 0.);
			}
		});
		
		//Now knowing the col multiplier, multiply entries in the map with frequencies
		frequenciesIterator = marriageTypesToAdjustMap.mapIterator();
		while (frequenciesIterator.hasNext()) {
			 frequenciesIterator.next();
			 MultiKey tmpKeyMultiKey = (MultiKey) frequenciesIterator.getKey();
				
			 double tmpValueDouble = ((Number) frequenciesIterator.getValue()).doubleValue();
			 tmpValueDouble *= colMprMap.get(tmpKeyMultiKey.getKey(1).toString());
			 frequenciesIterator.setValue(tmpValueDouble);
		}
		
		//Calculate error as the cumulative difference between targets and row and column sums
		for (String key : marriageTargetsByKey.keySet()) {
			errorDouble += Math.abs(marriageTargetsByKey.get(key) - rowSumsMap.get(key));
			errorDouble += Math.abs(marriageTargetsByKey.get(key) - colSumsMap.get(key));
		}
		
//		System.out.println("Error is " + error + " and iteration is " + tmpCount);
//		System.out.print(".");
		tmpCountInt++;
		
		}
		
		
		//Print out adjusted frequencies
		marriageTypesToAdjustMap.keySet().iterator().forEachRemaining(key -> System.out.println(key + "=" + marriageTypesToAdjustMap.get(key)));
		
		
		/*
		 * Use matching method provided with JAS-mine:
		 * 
		 */
		for(String key : keysStringSet) {

			for(String keyOther : keysStringSet) { 
				
				//Get number of people that should be matched for key, keyOther combination
				int tmpTargetInt;
				if(marriageTypesToAdjustMap.get(key, keyOther) != null) {
				tmpTargetInt = (int) Math.round(((Number) marriageTypesToAdjustMap.getValue(key, keyOther)).doubleValue());
				} else {
				tmpTargetInt = 0;
				}
				
				//Check if for the combination of key, keyOther matches should be formed:
				if (tmpTargetInt > 0) {
					
					double initialSizeQ1Double = personsToMatch2.get(key).size(); //Number of people to match ("row")
					Set<Person> unmatchedQ1Set = new LinkedHashSet<Person>(); //Empty set to store people to match
					unmatchedQ1Set.addAll(personsToMatch2.get(key)); //Add people to match 
					
//					unmatchedQ1.stream().iterator().forEachRemaining(persontodisp -> System.out.println("PID " + persontodisp.getKey().getId() + " HHID " + persontodisp.getHousehold().getKey().getId()));
					
//					System.out.println("Matching "+ initialSizeQ1 +"  persons from " + key + " to " + keyOther);
					
					double initialSizeQ2Double = personsToMatch2.get(keyOther).size(); //Number of people to match with ("column")
					Set<Person> unmatchedQ2FullSet = new HashSet<Person>(); // Empty set to store people to match with (note that HashSet does not preserve order, so we will sample at random from it)
					unmatchedQ2FullSet.addAll(personsToMatch2.get(keyOther)); //Add people to match with 
					
					Set<Person> unmatchedQ2Set = new LinkedHashSet<Person>();
					
					//Keep only the number of people in unmatchedQ2 that is equal to the adjusted number of matches to create from marriageTypesToAdjust:
					Iterator<Person> unmatchedQ2FullSetIterator = unmatchedQ2FullSet.iterator();
					for(int n = 0; n < tmpTargetInt && unmatchedQ2FullSetIterator.hasNext(); n++) {
						Person person = unmatchedQ2FullSetIterator.next();
						unmatchedQ2Set.add(person);
					}
					
//					System.out.println("Currently matching " + key + " with " + keyOther + ". The target is " + tmpTarget + " and there are " + unmatchedQ1.size() + 
//										" people in Q1 and " + unmatchedQ2.size() + " in Q2. (Originally Q2 had " + unmatchedQ2full.size() + " people.");
//					unmatchedQ2.stream().iterator().forEachRemaining(persontodisp -> System.out.println("PID " + persontodisp.getKey().getId() + " HHID " + persontodisp.getHousehold().getKey().getId()));
					
					Pair<Set<Person>, Set<Person>> unmatchedSetsPair = new Pair<>(unmatchedQ1Set, unmatchedQ2Set);
//					System.out.println("People in Q1 = " + unmatched.getFirst().size() + " People in Q2 = " + unmatched.getSecond().size());
					

					unmatchedSetsPair = IterativeSimpleMatching.getInstance().matching(unmatchedSetsPair.getFirst(), null, null, unmatchedSetsPair.getSecond(), null, 
							
							//This closure calculates the score for potential couple
							new MatchingScoreClosure<Person>() {
								@Override
								public Double getValue(Person male, Person female) {
									
									return SimulationEngine.getRnd().nextDouble(); //Random matching score
								}
							}, 
							
							new MatchingClosure<Person>() {
								@Override
								public void match(Person p1, Person p2) {
									
									//If two people have the same gender or different region, simply don't match and do nothing?
									if(p1.getDgn().equals(p2.getDgn()) || !p1.getRegion().equals(p2.getRegion())) {
//										throw new IllegalArgumentException("Error - both parties to match have the same gender!");
									}
									else {
										p1.setPartner(p2);
										p2.setPartner(p1);
									//	p1.setHousehold_status(Household_status.Couple);
									//	p2.setHousehold_status(Household_status.Couple);
										p1.setDcpyy(0); //Set years in partnership to 0
										p2.setDcpyy(0);
										
										//Update household
										p1.setupNewBenefitUnit(true);		//All the lines below are executed within the setupNewHome() method for both p1 and p2.  Note need to have partner reference before calling setupNewHome!

										p1.setToBePartnered(false); //Probably could be removed
										p2.setToBePartnered(false);
										personsToMatch2.get(key).remove(p1); //Remove matched persons and keep everyone else in the matching queue
										personsToMatch2.get(keyOther).remove(p2);
										personsToMatch.get(p1.getDgn()).get(p1.getRegion()).remove(p1);
										personsToMatch.get(p2.getDgn()).get(p2.getRegion()).remove(p2);
										partnershipsCreated++;
										
										
//										System.out.println("Matched " + p1.getGender()+ " " + p1.getKey().getId() + " with " + p2.getGender() + " " + p2.getKey().getId() + 
//															" into a new HH " + p1.getHousehold().getKey().getId() + ("HHID for " + p2.getKey().getId() + " should match: " + p2.getHousehold().getKey().getId()) + 
//																"New HH occupancy is " + p1.getHousehold_status() + " Children set size " + p1.getHousehold().getChildren().size());
									
									}
								}
								
							});
					
					
				}
			}

			Set<Person> unmatchedSet = new LinkedHashSet<>();
			unmatchedSet.addAll(personsToMatch2.get(key));
			for (Person unmatchedPerson : unmatchedSet) {
				if (unmatchedPerson.getDgn().equals(Gender.Male)) {
					malesUnmatched++;
				}
				else if (unmatchedPerson.getDgn().equals(Gender.Female)) {
					femalesUnmatched++;
				}

			}
/*
			personsToMatch2.get(key).clear();
			for(Gender gender: Gender.values()) {
				for(Region region : Parameters.getCountryRegions()) {
					personsToMatch.get(gender).get(region).clear();
				}
			}
*/
		}

//		System.out.println("Total over all years of unmatched males is " + malesUnmatched + " and females " + femalesUnmatched);
				
		for (BenefitUnit benefitUnit : benefitUnits) {
			benefitUnit.updateOccupancy();
			}
		
		
	}

	
	
	//Implement Matching Based On Earning Potential differential AND age differential (option C in Lia & Matteo's document 'BenefitUnit formation')


	/**
	 *
	 * PROCESS - UNION MATCHING OF SIMULATED POPULATION
	 * Matching Based On Earning Potential differential AND age differential
	 * (option C in Lia & Matteo's document 'BenefitUnit formation')
	 *
	 */
	int allMatches = 0;
	int yearMatches = 0;
	int unmatchedSize = 0;

	private void unionMatching() {

			Set<Person> matches = new LinkedHashSet<Person>();

			int countAttempts = 0;
			unmatchedSize = 0;
			for (Region region : Parameters.getCountryRegions()) {
				log.debug("Number of females to match: " + personsToMatch.get(Gender.Female).get(region).size() +
						", number of males to match: " + personsToMatch.get(Gender.Male).get(region).size());
				double initialMalesSize = personsToMatch.get(Gender.Male).get(region).size();
				double initialFemalesSize = personsToMatch.get(Gender.Female).get(region).size();
				Set<Person> unmatchedMales = new LinkedHashSet<Person>();
				Set<Person> unmatchedFemales = new LinkedHashSet<Person>();
				unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
				unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));

//				System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the start");

				Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);
				do {
//				unmatched = IterativeSimpleMatching.getInstance().matching(
					unmatched = IterativeRandomMatching.getInstance().matching(

							unmatched.getFirst(),    //Males.  Allows to iterate (initially it is personsToMatch.get(Gender.Male).get(region))

							null,        //No need for filter sub-population as group is already filtered by gender and region.

							null,    //By not declaring a Comparator, the 'natural ordering' of the Persons will be used to determine the priority with which they get to choose their match.  In the case of Person, there is no natural ordering, so the Matching algorithm randomizes the males, so their priority to choose is random.

							unmatched.getSecond(),    //Females. Allows to iterate (initially it is personsToMatch.get(Gender.Female).get(region))

							null,        //No need for filter sub-population as group is already filtered by gender and region.


							new MatchingScoreClosure<Person>() {
								@Override
								public Double getValue(Person male, Person female) {
									if (!male.getDgn().equals(Gender.Male)) {
										throw new IllegalArgumentException("Error - male in getValue() does not actually have the Male gender type!");
									}
									if (!female.getDgn().equals(Gender.Female)) {
										throw new IllegalArgumentException("Error - female in getValue() does not actually have the Female gender type!");
									}

									//Differentials are defined in a way that (in case we break symmetry later), a higher ageDiff and a higher earningsPotentialDiff favours this person, on the assumption that we all want younger, wealthier partners.  However, it is probably not going to be used as we will probably end up just trying to minimise the square difference between that observed in data and here.
									double ageDiff = male.getDag() - female.getDag();            //If male.getDesiredAgeDiff > 0, favours younger women
									double potentialEarningsDiff = male.getPotentialEarnings() - female.getPotentialEarnings();        //If female.getDesiredEarningPotential > 0, favours wealthier men
									double earningsMatch = (potentialEarningsDiff - female.getDesiredEarningsPotentialDiff());
									double ageMatch = (ageDiff - male.getDesiredAgeDiff());

									if (ageMatch < ageDiffBound && earningsMatch < potentialEarningsDiffBound
									) {
										// Score currently based on an equally weighted measure.  The Iterative (Simple and Random) Matching algorithm prioritises matching to the potential partner that returns the lowest score from this method (therefore, on aggregate we are trying to minimize the value below).
										return earningsMatch * earningsMatch + ageMatch * ageMatch;

									} else
										return Double.POSITIVE_INFINITY;        //Not to be included in possible partners
								}
							},

							new MatchingClosure<Person>() {
								@Override
								public void match(Person p1, Person p2) {        //The SimpleMatching.getInstance().matching() assumes the first collection in the argument (males in this case) is also the collection that the first argument of the MatchingClosure.match() is sampled from.
									//						log.debug("Person " + p1.getKey().getId() + " marries person " + p2.getKey().getId());
									if (p1.getDgn().equals(p2.getDgn())) {
										throw new IllegalArgumentException("Error - both parties to match have the same gender!");
									} else {
										p1.setPartner(p2);
										p2.setPartner(p1);
										p1.setHousehold_status(Household_status.Couple);
										p2.setHousehold_status(Household_status.Couple);
										p1.setDcpyy(0); //Set years in partnership to 0
										p2.setDcpyy(0);

										//Update household
										p1.setupNewBenefitUnit(true);        //All the lines below are executed within the setupNewHome() method for both p1 and p2.  Note need to have partner reference before calling setupNewHome!

										unmatchedMales.remove(p1); //Remove matched people from unmatched sets (but keep those who were not matched so they can try next year)
										unmatchedFemales.remove(p2);
										personsToMatch.get(p1.getDgn()).get(region).remove(p1);
										personsToMatch.get(p2.getDgn()).get(region).remove(p2);
										matches.add(p1);

									}
								}
							}
					);

					//Relax differential bounds for next iteration (in the case where there has not been a high enough proportion of matches)
					ageDiffBound *= Parameters.RELAXATION_FACTOR; //TODO: Should the bounds be relaxed permanently as it is the case now? Or only for the current year or year-region for example?
					potentialEarningsDiffBound *= Parameters.RELAXATION_FACTOR;
					countAttempts++;
//					System.out.println("unmatched males proportion " + unmatchedMales.size() / (double) initialMalesSize);
//					System.out.println("unmatched females proportion " + unmatchedFemales.size() / (double) initialFemalesSize);
				} while ((Math.min((unmatchedMales.size() / (double) initialMalesSize), (unmatchedFemales.size() / (double) initialFemalesSize)) > Parameters.UNMATCHED_TOLERANCE_THRESHOLD) && (countAttempts < Parameters.MAXIMUM_ATTEMPTS_MATCHING));

//				System.out.println("There are (overall stock of)" + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the end. Number of matches made for " + region + " is " + matches.size());


				for (Gender gender : Gender.values()) {
					//Turned off to allow unmatched people try again next year without the need to go through considerCohabitation process
//				personsToMatch.get(gender).get(region).clear();		//Nothing happens to unmatched people.  The next time they considerCohabitation, they will (probabilistically) have the opportunity to enter the matching pool again.
					unmatchedSize += personsToMatch.get(gender).get(region).size();
				}
			}

			yearMatches = matches.size();
			allMatches += matches.size();
//			System.out.println("Total number of matches made in the year " + matches.size() + " and total number of matches in all years is " + allMatches);

		if (commentsOn) log.debug("Marriage matched.");
 		for (BenefitUnit benefitUnit : benefitUnits) {
			benefitUnit.updateOccupancy();
		}			
	}

	/**
	 * PROCESS - UNION MATCHING WITH REGION RELAXED
	 *
	 */

	private void unionMatchingNoRegion() {
		int countAttempts = 0;

		double initialMalesSize = 0.;
		double initialFemalesSize = 0.;
		Set<Person> unmatchedMales = new LinkedHashSet<Person>();
		Set<Person> unmatchedFemales = new LinkedHashSet<Person>();

		Set<Person> matches = new LinkedHashSet<Person>();

		for (Region region : Parameters.getCountryRegions()) {
			initialMalesSize += personsToMatch.get(Gender.Male).get(region).size();
			initialFemalesSize += personsToMatch.get(Gender.Female).get(region).size();
			unmatchedMales.addAll(personsToMatch.get(Gender.Male).get(region));
			unmatchedFemales.addAll(personsToMatch.get(Gender.Female).get(region));
		}

//		System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the start");

		Pair<Set<Person>, Set<Person>> unmatched = new Pair<>(unmatchedMales, unmatchedFemales);

		do {
//				unmatched = IterativeSimpleMatching.getInstance().matching(
			unmatched = IterativeRandomMatching.getInstance().matching(

					unmatched.getFirst(),    //Males.  Allows to iterate (initially it is personsToMatch.get(Gender.Male).get(region))

					null,        //No need for filter sub-population as group is already filtered by gender and region.

					null,    //By not declaring a Comparator, the 'natural ordering' of the Persons will be used to determine the priority with which they get to choose their match.  In the case of Person, there is no natural ordering, so the Matching algorithm randomizes the males, so their priority to choose is random.

					unmatched.getSecond(),    //Females. Allows to iterate (initially it is personsToMatch.get(Gender.Female).get(region))

					null,        //No need for filter sub-population as group is already filtered by gender and region.


					new MatchingScoreClosure<Person>() {
						@Override
						public Double getValue(Person male, Person female) {
							if (!male.getDgn().equals(Gender.Male)) {
								throw new IllegalArgumentException("Error - male in getValue() does not actually have the Male gender type!");
							}
							if (!female.getDgn().equals(Gender.Female)) {
								throw new IllegalArgumentException("Error - female in getValue() does not actually have the Female gender type!");
							}

							//Differentials are defined in a way that (in case we break symmetry later), a higher ageDiff and a higher earningsPotentialDiff favours this person, on the assumption that we all want younger, wealthier partners.  However, it is probably not going to be used as we will probably end up just trying to minimise the square difference between that observed in data and here.
							double ageDiff = male.getDag() - female.getDag();            //If male.getDesiredAgeDiff > 0, favours younger women
							double potentialEarningsDiff = male.getPotentialEarnings() - female.getPotentialEarnings();        //If female.getDesiredEarningPotential > 0, favours wealthier men
							double earningsMatch = (potentialEarningsDiff - female.getDesiredEarningsPotentialDiff());
							double ageMatch = (ageDiff - male.getDesiredAgeDiff());

							if (ageMatch < ageDiffBound && earningsMatch < potentialEarningsDiffBound
							) {
								// Score currently based on an equally weighted measure.  The Iterative (Simple and Random) Matching algorithm prioritises matching to the potential partner that returns the lowest score from this method (therefore, on aggregate we are trying to minimize the value below).
								return earningsMatch * earningsMatch + ageMatch * ageMatch;

							} else
								return Double.POSITIVE_INFINITY;        //Not to be included in possible partners
						}
					},

					new MatchingClosure<Person>() {
						@Override
						public void match(Person p1, Person p2) {        //The SimpleMatching.getInstance().matching() assumes the first collection in the argument (males in this case) is also the collection that the first argument of the MatchingClosure.match() is sampled from.
							//						log.debug("Person " + p1.getKey().getId() + " marries person " + p2.getKey().getId());
							Region originalRegionP2 = p2.getRegion();
							if (!p1.getRegion().equals(p2.getRegion())) { //If persons to match have different regions, move female to male
								p2.setRegion(p1.getRegion());
//								System.out.println("Region changed");
							}
							if (p1.getDgn().equals(p2.getDgn())) {
								throw new IllegalArgumentException("Error - both parties to match have the same gender!");
							}
							else {
								p1.setPartner(p2);
								p2.setPartner(p1);
								p1.setHousehold_status(Household_status.Couple);
								p2.setHousehold_status(Household_status.Couple);
								p1.setDcpyy(0); //Set years in partnership to 0
								p2.setDcpyy(0);

								//Update household
								p1.setupNewBenefitUnit(true);        //All the lines below are executed within the setupNewHome() method for both p1 and p2.  Note need to have partner reference before calling setupNewHome!

								unmatchedMales.remove(p1); //Remove matched people from unmatched sets (but keep those who were not matched so they can try next year)
								unmatchedFemales.remove(p2);
								personsToMatch.get(p1.getDgn()).get(p1.getRegion()).remove(p1);
								personsToMatch.get(p2.getDgn()).get(originalRegionP2).remove(p2);
								matches.add(p1);

							}
						}
					}
			);

			//Relax differential bounds for next iteration (in the case where there has not been a high enough proportion of matches)
			ageDiffBound *= Parameters.RELAXATION_FACTOR;
			potentialEarningsDiffBound *= Parameters.RELAXATION_FACTOR;
			countAttempts++;
//			System.out.println("unmatched males proportion " + unmatchedMales.size() / (double) initialMalesSize);
//			System.out.println("unmatched females proportion " + unmatchedFemales.size() / (double) initialFemalesSize);
		} while ((Math.min((unmatchedMales.size() / (double) initialMalesSize), (unmatchedFemales.size() / (double) initialFemalesSize)) > Parameters.UNMATCHED_TOLERANCE_THRESHOLD) && (countAttempts < Parameters.MAXIMUM_ATTEMPTS_MATCHING));


		allMatches += matches.size();
//		System.out.println("There are " + unmatchedMales.size() + " unmatched males and " + unmatchedFemales.size() + " unmatched females at the end. Number of matches made " + matches.size() + " and total number of matches in all years is " + allMatches);
	}


	/**
	 * PROCESS - ALIGN THE SHARE OF COHABITATING INDIVIDUALS IN THE SIMULATED POPULATION
	 *
	 */

	private void considerCohabitationAlignment() {

		//Create a list of individuals who are allowed to enter a partnership for whom alignment should be performed
		int numPersonsWhoCanBePartnered = 0;
		int numPersonsToBePartnered = 0;
		ArrayList<Person> personsWhoCanBePartnered = new ArrayList<>();
		for (Person person : persons) {
			if (person.getDag() >= Parameters.MIN_AGE_COHABITATION && person.getPartner() == null) {
				numPersonsWhoCanBePartnered++;
				personsWhoCanBePartnered.add(person);
				if (person.isToBePartnered()) {
					numPersonsToBePartnered++;
				}
			}
		}

		int targetNumberToBePartnered = (int) ( (double) numPersonsWhoCanBePartnered * 0.3); // - numPersonsToBePartnered;

		if ((targetNumberToBePartnered - numPersonsToBePartnered) > 0) {
			new ResamplingAlignment<Person>().align(
					personsWhoCanBePartnered,
					null,
					new AlignmentOutcomeClosure<Person>() {
						@Override
						public boolean getOutcome(Person agent) {
							return agent.isToBePartnered();
						}

						@Override
						public void resample(Person agent) {
							agent.setToBePartnered(true);
							personsToMatch.get(agent.getDgn()).get(agent.getBenefitUnit().getRegion()).add(agent);
						}
					},
					targetNumberToBePartnered);
		}


	}


	/**
	 * PROCESS - ALIGN THE SHARE OF EMPLOYED IN THE SIMULATED POPULATION
	 */

	private void employmentAlignment() {

		//Create a nested map to store persons by gender and region
		LinkedHashMap<Gender, LinkedHashMap<Region, Set<Person>>> personsByGenderAndRegion;
		personsByGenderAndRegion = new LinkedHashMap<Gender, LinkedHashMap<Region, Set<Person>>>();

		EnumSet<Region> regionEnumSet = null;
		if (country.equals(Country.IT)) {
			regionEnumSet = EnumSet.of(Region.ITC, Region.ITH, Region.ITI, Region.ITF, Region.ITG);
		} else if (country.equals(Country.UK)) {
			regionEnumSet = EnumSet.of(Region.UKC, Region.UKD, Region.UKE, Region.UKF, Region.UKG, Region.UKH, Region.UKI, Region.UKJ, Region.UKK, Region.UKL, Region.UKM, Region.UKN);
		}

		for (Gender gender : Gender.values()) {
			personsByGenderAndRegion.put(gender, new LinkedHashMap<Region, Set<Person>>());
			for (Region region : regionEnumSet) {
				personsByGenderAndRegion.get(gender).put(region, new LinkedHashSet<Person>());
			}
		}

		//Iterate over persons and add them to the nested map above
		for (Person person : persons) {
			if (person.getDag() >= 18 && person.getDag() <= 64) {
				personsByGenderAndRegion.get(person.getDgn()).get(person.getRegion()).add(person);
			}
		}

		//For all gender and region combinations, compare the share of employed persons with the alignment target
		for (Gender gender : Gender.values()) {
			for (Region region : regionEnumSet) {
				double numberEmployed = 0;
				Set<Person> personsToIterateOver = personsByGenderAndRegion.get(gender).get(region);

				for (Person person : personsToIterateOver) {
					numberEmployed += person.getEmployed();
				}

				double sizeSimulatedSet = personsToIterateOver.size();

				double shareEmployedSimulated = numberEmployed/sizeSimulatedSet;
				double shareEmployedTargeted = ((Number) Parameters.getEmploymentAlignment().getValue(gender.toString(), region.toString(), year)).doubleValue();

				int targetNumberEmployed = (int) (shareEmployedTargeted*sizeSimulatedSet);




				//Simulated share of employment exceeds projections => move some individuals at random to non-employment
				if ((int) numberEmployed > targetNumberEmployed) {
					new ResamplingAlignment<Person>().align(
							personsToIterateOver,
							null,
							new AlignmentOutcomeClosure<Person>() {
								@Override
								public boolean getOutcome(Person person) {
									return person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed);
								}

								@Override
								public void resample(Person person) {
									person.setLes_c4(Les_c4.NotEmployed);
									person.setLabourSupplyWeekly(Labour.ZERO);
								}
							},
							targetNumberEmployed);
				}
			}
		}

	}

	/**
	 * PROCESS - ALIGN THE SHARE OF STUDENTS IN THE SIMULATED POPULATION
	 *
	 */

	private void inSchoolAlignment() {



		int numStudents = 0;
		int num16to29 = 0;
		ArrayList<Person> personsLeavingSchool = new ArrayList<Person>();
		for (Person person : persons) {
			if (person.getDag() > 15 && person.getDag() < 30) { //Could introduce separate alignment for different age groups, but this is more flexible as it depends on the regression process within the larger alignment target
				num16to29++;
				if (person.getLes_c4().equals(Les_c4.Student)) {
					numStudents++;
				}
				if (person.isToLeaveSchool()) { //Only those who leave school for the first time have toLeaveSchool set to true
					personsLeavingSchool.add(person);
				}
			}

		}

//		int targetNumberOfPeopleLeavingSchool = numStudents - (int)( (double)num16to29 * 0.33);

		int targetNumberOfPeopleLeavingSchool = numStudents - (int)( (double)num16to29 * ((Number) Parameters.getStudentShareProjections().getValue(country.toString(), year)).doubleValue() );

		System.out.println("Number of students < 30 is " + numStudents + " Persons set to leave school " + personsLeavingSchool.size() + " Number of people below 30 " + num16to29
							+ " Target number of people leaving school " + targetNumberOfPeopleLeavingSchool);

		if (targetNumberOfPeopleLeavingSchool <= 0) {
			for(Person person : personsLeavingSchool) {
				person.setToLeaveSchool(false);					//Best case scenario is to prevent anyone from leaving school in this year as the target share of students is higher than the number of students.  Although we cannot match the target, this is the nearest we can get to it.
				if(Parameters.systemOut) {
					System.out.println("target number of school leavers is not positive.  Force all school leavers to stay at school.");
				}
			}
		}
		else if (targetNumberOfPeopleLeavingSchool < personsLeavingSchool.size()) {
			if(Parameters.systemOut) {
				System.out.println("Schooling alignment: target number of students is " + targetNumberOfPeopleLeavingSchool);
			}
			new ResamplingAlignment<Person>().align(
					personsLeavingSchool,
					null,
					new AlignmentOutcomeClosure<Person>() {
						@Override
						public boolean getOutcome(Person agent) {
							return agent.isToLeaveSchool();
						}

						@Override
						public void resample(Person agent) {
							agent.setToLeaveSchool(false);
						}
					},
					targetNumberOfPeopleLeavingSchool);

			int numPostAlign = 0;
			for(Person person : persons) {
				if(person.isToLeaveSchool()) {
					numPostAlign++;
				}
			}
			System.out.println("Schooling alignment: aligned number of students is " + numPostAlign);
		}
	}










	/**
	 *
	 * PROCESS - EDUCATION LEVEL ALIGNMENT OF SIMULATED POPULATION
	 *
	 */

	private void educationLevelAlignment() {

		HashMap<Gender, ArrayList<Person>> personsLeavingEducation = new HashMap<Gender, ArrayList<Person>>();
		for(Gender gender : Gender.values()) {
			personsLeavingEducation.put(gender, new ArrayList<Person>());
		}


		for(Person person : persons) {
			if(person.isToLeaveSchool()) {
				personsLeavingEducation.get(person.getDgn()).add(person);
			}
		}

		for(Gender gender : Gender.values()) {

			//Check pre-aligned population for education level statistics
			int numPersonsOfThisGenderWithLowEduPreAlignment = 0, numPersonsOfThisGenderWithHighEduPreAlignment = 0, numPersonsOfThisGender = 0;
			for(Person person : persons) {
				if( person.getDgn().equals(gender) && person.getDag() >= 16 && person.getDag() <= 45) {		//Alignment projections are based only on persons younger than 66 years old
					if (person.isToLeaveSchool()) { //Align only people leaving school?
						if(person.getDeh_c3() != null) {
							if (person.getDeh_c3().equals(Education.Low)) {
								numPersonsOfThisGenderWithLowEduPreAlignment++;
							} else if (person.getDeh_c3().equals(Education.High)) {
								numPersonsOfThisGenderWithHighEduPreAlignment++;
							}
							numPersonsOfThisGender++;
						}
					}
				}
			}

			//Calculate alignment targets
			//High Education
			double highEducationRateTarget = ((Number)Parameters.getHighEducationRateInYear().getValue(year, gender.toString())).doubleValue();
			int numPersonsWithHighEduAlignmentTarget = (int) (highEducationRateTarget * (double)numPersonsOfThisGender);
			//Medium Education
			double lowEducationRateTarget = ((Number)Parameters.getLowEducationRateInYear().getValue(year, gender.toString())).doubleValue();
//			int numPersonsWithLowEduAlignmentTarget = (int) (proportionInitialPopWithMediumEdu * numPersonsOfThisGender);		//Based on initial population - this ensures that proportion of medium educated people can never decrease below initial values
			int numPersonsWithLowEduAlignmentTarget = (int) (lowEducationRateTarget * (double)numPersonsOfThisGender);
			if(Parameters.systemOut) {
				System.out.println("Gender " + gender + ", highEduRateTarget, " + highEducationRateTarget + ", lowEduRateTarget, " + lowEducationRateTarget);
			}
			//Sort the list of school leavers by age
			Collections.shuffle(personsLeavingEducation.get(gender), SimulationEngine.getRnd());		//To remove any source of bias in borderline cases because the first subset of school leavers of same age are assigned a higher education level.  (I.e. if education level is deemed to be associated with age, so that higher ages are assigned higher education levels, then if the boundary between high and medium education levels is e.g. at the people aged 27, the first few people aged 27 will be assigned a high education level and the rest will have medium (or low) education levels.  To avoid any sort of regularity in the iteration order of school leavers, we shuffle here.
			Collections.sort(personsLeavingEducation.get(gender),
					(Comparator<Person>) (arg0, arg1) -> {
						return arg1.getDag() - arg0.getDag();	//Sort school leavers by descending order in age
					});

			//Perform alignment
			int countHigh = 0, countLow = 0;
			for(Person schoolLeaver : personsLeavingEducation.get(gender)) {		//This tries to maintain the naturally generated number of school-leavers with medium education, so that an increase in the number of school-leavers with high education is achieved through a reduction in the number of school-leavers with low education.  However, in the event that the number of school-leavers with either high or medium education are more than the total number of school leavers (in this year), we end up having no school leavers with low education and we have to reduce the number of school leavers with medium education

				if (schoolLeaver.getDeh_c3().equals(Education.Medium)) {
					if(numPersonsOfThisGenderWithHighEduPreAlignment + countHigh < numPersonsWithHighEduAlignmentTarget) {				//Only align if number of people in population with high education is too low.
							schoolLeaver.setEducation(Education.High);			//As the personsLeavingEducation list is sorted by descending age, the oldest people leaving education are assigned to have high education levels
							countHigh++;
					}
					else if(numPersonsOfThisGenderWithLowEduPreAlignment + countLow< numPersonsWithLowEduAlignmentTarget) {
							schoolLeaver.setEducation(Education.Low);		//When the number of high education level people have been assigned, the next oldest people are assigned to have medium education levels
							countLow++;
					}
				}

				else if (schoolLeaver.getDeh_c3().equals(Education.High)) {
					if (numPersonsOfThisGenderWithHighEduPreAlignment + countHigh > numPersonsWithHighEduAlignmentTarget) { //If too many people with high education
							schoolLeaver.setEducation(Education.Medium);
							countHigh--;
					}
				}

				else if (schoolLeaver.getDeh_c3().equals(Education.Low)) {
					 if (numPersonsOfThisGenderWithLowEduPreAlignment + countLow > numPersonsWithLowEduAlignmentTarget) {
							schoolLeaver.setEducation(Education.Medium);
							countLow--;
					}
				}

//				System.out.println(schoolLeaver.getAge() + ", " + schoolLeaver.getEducation().toString());		//Test
			}
			personsLeavingEducation.get(gender).clear();	//Clear for re-use in the next year

			if(Parameters.systemOut) {
				//Check result of alignment
				int countHighEdPeople = 0, countMediumEdPeople = 0;
				for(Person person : persons) {
					if( person.getDgn().equals(gender) && (person.getDag() <= 65) ) {		//Alignment projections are based only on persons younger than 66 years old
						if (person.isToLeaveSchool()) {
							if(person.getDeh_c3() != null) {
								if(person.getDeh_c3().equals(Education.High)) {
									countHighEdPeople++;
								}
								else if(person.getDeh_c3().equals(Education.Medium)) {
									countMediumEdPeople++;
								}
							}
						}
					}
				}
				System.out.println("Year is " + year);
				System.out.println("Gender " + gender + ", Proportions of High Edu " + ((double)countHighEdPeople/(double)numPersonsOfThisGender) + ", Medium Edu " + ((double)countMediumEdPeople/(double)numPersonsOfThisGender));
			}
		}
	}


	/**
	 *
	 * PROCESS - FERTILITY ALIGNMENT OF SIMULATED POPULATION
	 *
	 */
	private void fertilityAlignment() {

		//With new fertility alignment for the target number instead of fertility rate

		for (Region region: Parameters.getCountryRegions()) {
			double fertilityRate = ((Number)Parameters.getFertilityRateByRegionYear().get(region, year)).doubleValue();

			int numberNewbornsProjected = 0;
			for (Gender gender : Gender.values()) {
				numberNewbornsProjected += ((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), 0, year)).doubleValue();
			}

			int simNewborns = 0;
			for (Person person : persons) {
				if (person.getRegion().equals(region) && person.isToGiveBirth()) {
					simNewborns++;
				}
			}

			int targetNumberNewborns = (int) Math.round(numberNewbornsProjected/scalingFactor);

			new ResamplingAlignment<Person>().align(
					getPersons(),
					new FertileFilter<Person>(region),
					new AlignmentOutcomeClosure<Person>() {
						@Override
						public boolean getOutcome(Person agent) {
							//Note: fertility method runs before the alignment
							return agent.isToGiveBirth(); //Returns either true or false to be used by the closure getOutcome
						}

						@Override
						public void resample(Person agent) {
							if (!agent.isToGiveBirth()) {
								agent.setToGiveBirth(true);
							}
							else {
								agent.setToGiveBirth(false);
							}
						}
					},
					targetNumberNewborns,//align to this number of newborns per region
					20
			);

/*
			//Type of alignment performed depends on whether weights are used, or not
			AbstractProbabilityAlignment<Person> alignmentProcedure; 
			
			if(useWeights) { //If using weights use Logit Scaling Binary Weighted Alignment
				alignmentProcedure = new LogitScalingBinaryWeightedAlignment<Person>(); 
			}
			else { //Otherwise, use unweighted Logit Scaling Binary Alignment
				alignmentProcedure = new LogitScalingBinaryAlignment<Person>();
			}
			
			alignmentProcedure.align(
					getPersons(), //Collection to align: persons from person set (but note the filter applied below)
					new FertileFilter<Person>(region), 		//New restriction is that the Female needs to have a partner to be 'fertile' (i.e. considered in the fertility alignment process). This filters the collection specified above. 
					new AlignmentProbabilityClosure<Person>() { //a piece of code that i) for each element of the filtered collection computes a probability for the event (in the case that the alignment method is aligning probabilities, as in the SBD algorithm)

						@Override
						public double getProbability(Person agent) { //i) calculate probability for each element of the filtered collection
							if(agent.getDag() <= 29 && agent.getLes_c3().equals(Les_c4.Student) && agent.isLeftEducation() == false) { //If age below or equal to 29 and in continuous education follow process F1a
								return Parameters.getRegFertilityF1a().getProbability(agent, Person.DoublesVariables.class);
							}
							else { //Otherwise if not in continuous education, follow process F1b 
								return Parameters.getRegFertilityF1b().getProbability(agent, Person.DoublesVariables.class); 
							}
						}

						@Override
						public void align(Person agent, double alignedProabability) {
							if ( RegressionUtils.event( alignedProabability, SimulationEngine.getRnd() ) ) { //RegressionUtils.event samples an event where all events have equal probability
								agent.setToGiveBirth(true);
							} else agent.setToGiveBirth(false);
						}
					},
					fertilityRate //the share or number of elements in the filtered collection that are expected to experience the transition. In this case, fertility rate by region and year
			);
*/
		}
	}
	
	/**
	 * 
	 * PROCESS TO DETERMINE FERTILITY WITHOUT ALIGNMENT TO TARGET FERTILITY RATES
	 * 
	 */
	private void fertility() {
		for (Region region: Parameters.getCountryRegions()) { //Select fertile persons from each region and determine if they give birth
			List<Person> fertilePersons = new ArrayList<Person>();
			CollectionUtils.select(getPersons(), new FertileFilter<Person>(region), fertilePersons);
			
			for (Person person : fertilePersons) {
//				System.out.println("Person ID " + person.getKey().getId() + " Region " + person.getRegion() + " Gender " + person.getDgn() + " Age " + person.getDag() + " Activity status " + person.getLes_c3() + " Left education " + person.isLeftEducation() + " Partner " + person.getPartnerId());

				if (country.equals(Country.UK)) {

					if (person.getDag() <= 29 && person.getLes_c4().equals(Les_c4.Student) && person.isLeftEducation() == false) { //If age below or equal to 29 and in continuous education follow process F1a
						person.setToGiveBirth(Parameters.getRegFertilityF1a().event(person, Person.DoublesVariables.class)); //If regression event true, give birth
//					System.out.println("Followed process F1a");
					} else { //Otherwise if not in continuous education, follow process F1b
						person.setToGiveBirth(Parameters.getRegFertilityF1b().event(person, Person.DoublesVariables.class)); //If regression event true, give birth
//					System.out.println("Followed process F1b");
					}
				}
				else if (country.equals(Country.IT)) { //In Italy, there is a single fertiltiy process
					person.setToGiveBirth(Parameters.getRegFertilityF1().event(person, Person.DoublesVariables.class));
				}
			}
				
		}
				
	}
	


	/**
	 *
	 * UPDATE MODEL PARAMETERS TO REFLECT YEAR
	 *
 	 */
	private void updateParameters() {
//		Parameters.updateProbSick(year);		//Make any adjustments to the sickness probability profile by age depending on retirement age
//		Parameters.updateUnemploymentRate(year);
	}


	/**
	 *
	 * REPORT ELAPSED TIME TO LOG
	 *
	 */
	private void printElapsedTime() {
		log.debug("Year: " + year + ", Elapsed time: " + (System.currentTimeMillis() - elapsedTime)/1000. + " seconds.");
	}


	/**
	 *
	 * CREATE INPUT DATABASE TABLES BASED ON .txt FILES CREATED IN GUI DIALOG FROM EUROMOD
	 *
	 */
	private void inputDatabaseInteraction() {
 
		Connection conn = null;
		Statement stat = null;
        try {        		        
        	Class.forName("org.h2.Driver");
	        conn = DriverManager.getConnection("jdbc:h2:"+DatabaseUtils.databaseInputUrl, "sa", "");
	        
//        	//Create input database from input file (population_[country].csv)    
//			if(refreshInputDatabase) {
//				SQLdataParser.createDatabaseTablesFromCSVfile(country, Parameters.getInputFileName(), startYear, conn);
//			}
			
			//Create database tables to be used in simulation from country-specific tables
            String[] tableNames = new String[]{"PERSON", "DONORPERSON", "BENEFITUNIT", "DONORHOUSEHOLD", "HOUSEHOLD"};
            String[] tableNamesInitial = new String[]{"PERSON", "BENEFITUNIT", "HOUSEHOLD"};
            String[] tableNamesDonor = new String[]{"DONORPERSON", "DONORHOUSEHOLD"};
            stat = conn.createStatement();
            for(String tableName: tableNamesDonor) {
	            stat.execute("DROP TABLE IF EXISTS " + tableName);
	            stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country);
            }
			for(String tableName: tableNamesInitial) {
				stat.execute("DROP TABLE IF EXISTS " + tableName);
				stat.execute("CREATE TABLE " + tableName + " AS SELECT * FROM " + tableName + "_" + country + "_" + startYear); // Load the country-year specific initial population from all available in tables of the database
			}

			List<String> policyDependentAttributes = new ArrayList<>();
            for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
            	
            	String dispString = Parameters.DISPOSABLE_INCOME_VARIABLE_NAME + "_" + policyName;
            	policyDependentAttributes.add(dispString);            	
            	
            	String emplInsurString = Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + policyName;
            	policyDependentAttributes.add(emplInsurString);            
            	
//            	String selfEmplString = Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + policyName;
//            	policyDependentAttributes.add(selfEmplString);

				String earnString = Parameters.GROSS_EARNINGS_VARIABLE_NAME + "_" + policyName;
				policyDependentAttributes.add(earnString);

				String origIncomeString = Parameters.ORIGINAL_INCOME_VARIABLE_NAME + "_" + policyName;
				policyDependentAttributes.add(origIncomeString);

				String hourlyWageString = Parameters.HOURLY_WAGE_VARIABLE_NAME + "_" + policyName;
				policyDependentAttributes.add(hourlyWageString);

				String ils_benmtString = Parameters.ILS_BENMT_NAME + "_" + policyName;
				policyDependentAttributes.add(ils_benmtString);

				String ils_benntString = Parameters.ILS_BENNT_NAME + "_" + policyName;
				policyDependentAttributes.add(ils_benntString);

            }
            String policyDependentAttributesString = SQLdataParser.stringAppender(policyDependentAttributes);
            String query = "SELECT ID, " + Parameters.HOURS_WORKED_WEEKLY + ", " + policyDependentAttributesString + " FROM DONORPERSON";
            ResultSet rs = stat.executeQuery(query);
            donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute = MultiKeyMap.multiKeyMap(new LinkedMap<>());
            initialHoursWorkedWeekly = new LinkedHashMap<Long, Double>();
            while (rs.next()) {
                
                long id = rs.getLong("ID");
                for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
                	
                	String dispString = Parameters.DISPOSABLE_INCOME_VARIABLE_NAME + "_" + policyName;
                	donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.DISPOSABLE_INCOME_VARIABLE_NAME, rs.getDouble(dispString));            	
                	
                	String emplInsurString = Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + policyName;
                	donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME, rs.getDouble(emplInsurString));
                	                	
//                	String selfEmplString = Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + policyName;
//                	donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME, rs.getDouble(selfEmplString));

					String earnString = Parameters.GROSS_EARNINGS_VARIABLE_NAME + "_" + policyName;
					donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.GROSS_EARNINGS_VARIABLE_NAME, rs.getDouble(earnString));

					String origIncomeString = Parameters.ORIGINAL_INCOME_VARIABLE_NAME + "_" + policyName;
					donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.ORIGINAL_INCOME_VARIABLE_NAME, rs.getDouble(origIncomeString));

					String hourlyWageString = Parameters.HOURLY_WAGE_VARIABLE_NAME + "_" + policyName;
					donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.HOURLY_WAGE_VARIABLE_NAME, rs.getDouble(hourlyWageString));

					String ils_benmtString = Parameters.ILS_BENMT_NAME + "_" + policyName;
					donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.ILS_BENMT_NAME, rs.getDouble(ils_benmtString));

					String ils_benntString = Parameters.ILS_BENNT_NAME + "_" + policyName;
					donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute.put(id, policyName, Parameters.ILS_BENNT_NAME, rs.getDouble(ils_benntString));

                }
                initialHoursWorkedWeekly.put(id, rs.getDouble(Parameters.HOURS_WORKED_WEEKLY));
                
            }
            //Add hours from initial population which is now different than donor
            String query2 = "SELECT ID, " + Parameters.HOURS_WORKED_WEEKLY + " FROM PERSON";
            ResultSet rs2 = stat.executeQuery(query2);
            while (rs2.next()) {
				initialHoursWorkedWeekly.put(rs2.getLong("ID"), rs2.getDouble(Parameters.HOURS_WORKED_WEEKLY));
			}
            
            
        } 
        catch(ClassNotFoundException|SQLException e){
        	if(e instanceof ClassNotFoundException) {
	    		 log.debug( "ERROR: Class not found: " + e.getMessage() + "\nCheck that the input.h2.db "
	        		+ "exists in the input folder.  If not, unzip the input.h2.zip file and store the resulting "
	        		+ "input.h2.db in the input folder!\n");
	    	}
	    	else {
	    		 throw new IllegalArgumentException("SQL Exception thrown! " + e.getMessage());
	    	}            
        }
		finally {
			try {
				  if (stat != null) { stat.close(); }
				  if (conn != null) { conn.close(); }
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	
	
	//----------------------------------------------------
	//
	// CREATE DONOR POPULATION FROM INPUT DATABASE TABLES 
	//
	//----------------------------------------------------	
	private Map<Long, DonorPerson> createDonorPopulationDataStructures() {

        @SuppressWarnings("unchecked")
		List<DonorPerson> euromodOutputPersonList = (List<DonorPerson>) DatabaseUtils.loadTable(DonorPerson.class);       		//RHS returns an ArrayList
        if(euromodOutputPersonList.isEmpty()) {
        	throw new IllegalStateException("Error - there are no Donor Persons from input.h2 database!");
        }        
        Map<Long, DonorPerson> euromodOutputPersons = new LinkedHashMap<Long, DonorPerson>();
        for(DonorPerson donor: euromodOutputPersonList) {
        	euromodOutputPersons.put(donor.getKey().getId(), donor);
        }
        if(euromodOutputPersons.size() != euromodOutputPersonList.size()) {
        	throw new IllegalStateException("ERROR - euromodOutputPersonList and euromodOutputPersons set have different sizes!");
        }
        euromodOutputPersonList = null;  
        for(DonorPerson person: euromodOutputPersons.values()) {
        	person.initializeMapAttributes();
        }
        donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute = null;	//Once donor persons have had their map attributes initialized, there is no need to maintain this MultiKeyMap, so release memory
		
        //Map to household id for ease of finding persons and add to set to ensure uniqueness
        euromodOutputPersonsByHouseholdId = new LinkedHashMap<Long, LinkedHashSet<DonorPerson>>();     
        for(DonorPerson person: euromodOutputPersons.values()) {
//			Long householdId = person.getHouseholdId(); //Use this line if using household Ids and not benefit unit Ids
			Long benefitUnitId = person.getBenefitUnitId(); //IDs of household in household table are now based on the benefit unit Id, so this has been changed from household id to benefitUnit Id
        	LinkedHashSet<DonorPerson> personsInHousehold = euromodOutputPersonsByHouseholdId.get(benefitUnitId);
        	if(personsInHousehold == null) {
        		personsInHousehold = new LinkedHashSet<DonorPerson>();
        	}
        	personsInHousehold.add(person);
        	euromodOutputPersonsByHouseholdId.put(benefitUnitId, personsInHousehold);
        }
        
		@SuppressWarnings("unchecked")
		List<DonorHousehold> euromodOutputHouseholdList = (List<DonorHousehold>) DatabaseUtils.loadTable(DonorHousehold.class);	//RHS returns an ArrayList
        if(euromodOutputHouseholdList.isEmpty()) {
        	throw new IllegalStateException("Error - there are no Donor Households from input.h2 database!");
        }        
		euromodOutputHouseholds = new LinkedHashSet<DonorHousehold>(euromodOutputHouseholdList); 	//Ensure no duplication
		for(DonorHousehold house: euromodOutputHouseholds) {
        	house.initializeAttributes();
		}
		euromodOutputHouseholdList = null;
        
//        //Have now removed the DonorHousehold table in the input database due to the issue of duplicate objects.  
//        // This new way should be more efficient anyway...
//        //We programmatically create the set of DonorHousehold objects
//    	euromodOutputHouseholds = new LinkedHashSet<>();
//        for(Long houseId: euromodOutputPersonsByHouseholdId.keySet()) {
//        	DonorHousehold house = new DonorHousehold(houseId);
//        	house.initializeAttributes();
//        	euromodOutputHouseholds.add(house);
//        }
        
        
        
        
        
		log.debug("Donor population loaded from input database.");
		
		return euromodOutputPersons;
	}
	
	
	
	//----------------------------------------------------------
	//
	//	CREATE INPUT POPULATION FROM INPUT DATABASE DATA
	//
	//----------------------------------------------------------
	
	/**
	 * Note that this method makes minimal assumptions on the input database data.  It does NOT assume the data
	 * is from EUROMOD data files.  This makes the simulation very flexible with regards to input database data,
	 * but it does mean that the method is not optimised for speed. 
	 *
	 */
	private void createInitialPopulationDataStructures() {
					
        //Load data from the input database
		//Persons
        @SuppressWarnings("unchecked")
		List<Person> inputPersonList = (List<Person>) DatabaseUtils.loadTable(Person.class);       		//RHS returns an ArrayList
        if(inputPersonList.isEmpty()) {
        	throw new IllegalStateException("Error - there are no Persons for country " + country + " in the input.h2 database!");
        }

        //Benefit units
        @SuppressWarnings("unchecked")
		List<BenefitUnit> inputBenefitUnitList = (List<BenefitUnit>) DatabaseUtils.loadTable(BenefitUnit.class);	//RHS returns an ArrayList
        if(inputBenefitUnitList.isEmpty()) {
        	throw new IllegalStateException("Error - there are no Benefit Units for country " + country + " in the input.h2 database!");
        }

        //Households
		@SuppressWarnings("unchecked")
		List<Household> inputHouseholdList = (List<Household>) DatabaseUtils.loadTable(Household.class);
		if(inputHouseholdList.isEmpty()) {
			throw new IllegalStateException("Error - there are no Households for country " + country + " in the input.h2 database!");
		}
        
        log.debug("Initial population loaded from input database.");        
        
        //Remove any duplicate entries by adding List entries to a Set, where equals is defined in terms of id of Person                
        Set<Person> inputPersonSet = new LinkedHashSet<Person>(inputPersonList);			//LinkedHashSet to ensure identical outcome for simulation with same random seed
        if(inputPersonSet.size() != inputPersonList.size()) {
        	throw new IllegalStateException("ERROR - initialPersonList and fullPersonSet have different sizes!");
        }

        Set<Household> inputHouseholdSet = new LinkedHashSet<>(inputHouseholdList);
        Set<BenefitUnit> inputBUSet = new LinkedHashSet<>(inputBenefitUnitList);

        //Define a set of household Ids
		Set<Long> inputHouseholdIdsSet = new LinkedHashSet<>();

      	//Initialise set of persons and benefitUnits in which to simulate - used both for weighted and unweighted populations

        persons = new LinkedHashSet<Person>();
        benefitUnits = new LinkedHashSet<BenefitUnit>();
        households = new LinkedHashSet<Household>(); //Also initialise set of families to which benefitUnits belong
        
        int minAgeInInitialPopulation = Integer.MAX_VALUE;
		int maxAgeInInitialPopulation = Integer.MIN_VALUE;
        
        double aggregatePersonsWeight = 0.;			//Aggregate Weight of simulated individuals (a weighted sum of the simulated individuals)
        double aggregateHouseholdsWeight = 0.;		//Aggregate Weight of simulated benefitUnits (a weighted sum of the simulated households)

		//To avoid iterating over the list of all benefitUnits for each household, and then all persons for each benefitUnit, create maps that will map from:
		// 1) household Id => all benefitUnits in the household
		// 2) benefitUnit => all persons in the benefitUnit
		//Implement these as TreeMaps, which is ordered on keys
		TreeMap<Long, LinkedHashSet<BenefitUnit>> hhBUMap = new TreeMap<>();
		TreeMap<Long, LinkedHashSet<Person>> BUPersonMap = new TreeMap<>();

		LinkedList<BenefitUnit> inputBUListIterator = new LinkedList<>(inputBUSet); //Convert set of input BUs to a list to iterate over, which allows removal from the iteration when matched to a household
		LinkedList<Person> inputPersonListIterator = new LinkedList<>(inputPersonSet); //Convert set of input persons to a list to iterate over, which allows removal from the iteration when matched to a benefit unit

		//Iterate over all households and BUs and add to the hhBUMap
		for (Household hh : inputHouseholdSet) { //For each household from the database
			Long hhId = hh.getId(); //Get Id
			LinkedHashSet<BenefitUnit> setOfBUToAdd = new LinkedHashSet<>(); //Create empty set of BenefitUnits that belong to it

			Iterator<BenefitUnit> buIterator = inputBUListIterator.iterator(); //Explicitly create an iterator to allow removal of matched BUs in the loop
			while (buIterator.hasNext()) { //Get next BU
				BenefitUnit bu = buIterator.next();
				if (bu.getId_household() == hhId) { //If BenefitUnit belongs to the household in the outer iterator, add to setOfBUToAdd
					setOfBUToAdd.add(bu);
					buIterator.remove(); //Remove from the list (but not the original inputBUSet) to speed up subsequent iterations
				}
			}

			//Should now have household's Id and a set of all benefitUnits that belong to it to add to the map
			hhBUMap.put(hhId, setOfBUToAdd);
		}


		//Iterate over all BUs and persons and add to the BUPersonMap
		for (BenefitUnit bu : inputBUSet) {
			Long buId = bu.getId();
			LinkedHashSet<Person> setOfPersonsToAdd = new LinkedHashSet<>();

			Iterator<Person> personIterator = inputPersonListIterator.iterator(); //Explicitly create an iterator to allow removal of matched persons in the loop
			while (personIterator.hasNext()) { //Get next person
				Person person = personIterator.next();
				if (person.getId_benefitUnit() == buId) { //If person belongs to the benefit unit, add to setOfPersonsToAdd
					setOfPersonsToAdd.add(person);
					personIterator.remove(); //Remove from the list (but not the original inputPersonSet to speed up subsequent iterations
				}
			}

			BUPersonMap.put(buId, setOfPersonsToAdd);
		}


        //Expand population, sample, and remove weights:
        if (!useWeights) {
        	
        	
        	//Time execution of expansion
        	StopWatch stopwatch = new StopWatch();
        	stopwatch.start();
        	
        	System.out.println("Will expand the initial population to have " + popSize + " individuals and not use weights.");
        	//Desired number of individual in the simulation is set in popSize
        	//1. Create a list of benefitUnits and expand according to weight:
        	List<Household> expandedHouseholdList = new LinkedList<>();
        	double totalWeightCount = 0;
        	for (Household house : inputHouseholdList) { //For each household in the initial database of households
//        		System.out.println("BenefitUnit id is " + house.getKey().getId() + "and weight is " + house.getWeight());
        		totalWeightCount = totalWeightCount + house.getDwt();

				for (int i = 0; i < house.getDwt() / 10 ; i++) { //Get weight of the household and copy the household until weight / 100 is reached
					expandedHouseholdList.add(house);
				}
			}
        	
        	//Check that expanded list has as many households as the sum of original weights
//        	System.out.println("Total weight of added household is " + totalWeightCount + "and number of benefitUnits on expanded list is " + expandedHouseholdList.size());
        	
        	//2. Shuffle expanded list and sample the benefitUnits by iterating through them:
        	Collections.shuffle(expandedHouseholdList, SimulationEngine.getRnd());

        	//Takes about 3 second to get here so expanding the list not very time consuming. 
        	//Have to iterate through households but until desired number of individuals is reached. That means I don't know the number of households in advance.
        	ListIterator<Household> expandedHouseholdsIterator = expandedHouseholdList.listIterator();
        	
        	
//        	householdsList = new LinkedHashSet<BenefitUnit>();
//       	personsList = new LinkedHashSet<Person>();
        	
        	int count = 0;
        	double hhweight = 0;
        	
        	while (expandedHouseholdsIterator.hasNext()) {
				Household household = (Household) expandedHouseholdsIterator.next();
				Household newHousehold = new Household(); //Create a new household

				//Get BUs in this household
				LinkedHashSet<BenefitUnit> BUInThisHousehold = hhBUMap.get(household.getId());

				for (BenefitUnit bu : BUInThisHousehold) { //For each BU belonging to this household
					BenefitUnit newBenefitUnit = new BenefitUnit(bu); //Create a new BU using the copy constructor
					Set<Person> newBenefitUnitMembers = newBenefitUnit.getOtherMembers(); //Create a set to store BU members
					LinkedHashSet<Person> personsInThisBU = BUPersonMap.get(bu.getId()); //Get persons in this BU

					//For each person belonging to this BU
					for (Person person : personsInThisBU) {
						Person newPerson = new Person(person); //Note: the reason for copying BenefitUnits, Persons, and Household is that the same household can be sampled more than once in the expansion.
						newPerson.setBenefitUnit(newBenefitUnit); //Assign new person to new benefit unit
					//	newPerson.setId_household(newHousehold.getId()); //Update the household id of the new person => not needed as done when assigning the benefit unit to a household below
						newPerson.setWeight(1); //Set weight to 1 to remove weights
						newBenefitUnitMembers.add(newPerson); //Add to the set of BU members

						int age = newPerson.getDag();
						if(age < minAgeInInitialPopulation) {
							minAgeInInitialPopulation = age;
						}
						if(age > maxAgeInInitialPopulation) {
							maxAgeInInitialPopulation = age;
						}

						persons.add(newPerson); //Add new person to the set of persons in the simulation
						count++; //Increase person count by 1 for each added person
						aggregatePersonsWeight += person.getWeight();

					}

					newBenefitUnit.setWeight(1); //Set weight to 1 to remove weights
					newBenefitUnit.setHousehold(newHousehold); //Assign the benefit unit to the household
					newHousehold.addBenefitUnitToHousehold(newBenefitUnit); //Assign the new benefit unit to the new household. Note that this calls setHousehold() method in the newBenefitUnit.
					benefitUnits.add(newBenefitUnit); //Add new benefit unit to the set of benefit units in the simulation
//
// 					//TODO: 18/03/2022: Does the person need to be added to the benefit unit explicitly somewhere in this method? (Example code below)
// 					if(house.getMaleId() != null && house.getMaleId().equals(id)) {
//            		house.addResponsiblePerson(person);
//            		System.out.println("Run1");
//            	}
//            	else if(house.getFemaleId() != null && house.getFemaleId().equals(id)) {
//            		house.addResponsiblePerson(person);
//            		System.out.println("Run2");
//            	}
//        		else if(house.getMaleId() != null && house.getMaleId().equals(person.getFatherId())) {
//        			house.getChildren().add(person);			//Check if person's father is male responsible for accomodation - if so, add to Children set
//        			System.out.println("Run3");
//        		}
//        		else if(house.getFemaleId() != null && house.getFemaleId().equals(person.getMotherId())) {
//        			house.getChildren().add(person);			//Check if person's father is male responsible for accomodation - if so, add to Children set
//        			System.out.println("Run4");
//        		}
//            	else {
//            		house.getOtherMembers().add(person);		//The default relationship in the house
//            		System.out.println("Run5");
//            	}

					//Now iterate through a list of all benefitUnit members and set partner, mother, and father id:
					for (Person person : newBenefitUnitMembers) {
						if (person.getId_partner() != null) {
							if (person.getPartner() == null) {
								for (Person other : newBenefitUnitMembers) {
									if (person.getId_partner().equals(other.getId_original())) {
										if (!other.getId_partner().equals(person.getId_original())) {
											throw new IllegalArgumentException("Error - For person " + person.getKey().getId() + " with partnerId " + person.getId_partner() + ", their supposed partner whose id is " + other.getKey().getId() + " has a partnerId " + other.getId_partner() + " which is not equal to the first person.  This implies an unreciprocated partnership (love triangle etc.) in the data!");
										}
										if (person.getDgn().equals(other.getDgn())) { //Check for same-sex relationships
											//Split up same-sex couples (assign null to partner and partnerId)
											person.setPartner(null);
											other.setPartner(null);
											break;
										} else {
//	            						System.out.println("Match found");
											//Replace partner ID with new ID by setting partner
											person.setPartner(other);
											other.setPartner(person);
										}
									}
								}
							}
							if(person.getPartner() == null && person.getId_partner() != null) {		//Check for missing partner
								//Note that for same-sex couples, we have split them up above, however we have also set partnerId to null, hence the exception below should not be thrown.
								throw new IllegalArgumentException("Error - For person " + person.getKey().getId() + " with benefitUnitId " + person.getId_benefitUnit() + ", could not find partner " + person.getId_partner() + " in same benefitUnit " + bu.getKey().getId() + "!");
							}
						}

						if (person.getId_mother() != null) {
							for (Person other : newBenefitUnitMembers) {
								if (person.getId_mother().equals(other.getId_original())) {
									//Replace mother ID with new ID
									person.setId_mother(other.getKey().getId());
									if (other.getDgn().equals(Gender.Male)) {
										System.out.println("Warning: Male assigned the role of the mother for HH ID " + person.getId_benefitUnit());
									}
								}
							}
						}

						if (person.getId_father() != null) {
							for (Person other : newBenefitUnitMembers) {
								if (person.getId_father().equals(other.getId_original())) {
									//Replace father ID with new ID
									person.setId_father(other.getKey().getId());
									if (other.getDgn().equals(Gender.Female)) {
										System.out.println("Warning: Female assigned the role of the father for HH ID " + person.getId_benefitUnit());
									}
								}
							}
						}

					}

					newBenefitUnit.calculateSize();
				}

				households.add(newHousehold); //Add new household to the set of households

				if (count >= popSize) { //Check if desired simulated population size reached, if yes break out of the loop
					break;
				}

		}


        	stopwatch.stop();
        	System.out.println("Time elapsed " + stopwatch.getTime() + " miliseconds");
        	
        	
        	/*
        	Iterator<Person> testit = persons.iterator();
        	while (testit.hasNext()) {
				Person person = (Person) testit.next();
				System.out.println("HHID is " + person.getHouseholdId() + "PID is " + person.getKey().getId() + ". Original ID was " + person.getOriginalID() + " and original HHID was " + person.getOriginalHHID() + "Partner ID is " + person.getPartnerId() + "Mother Id is " + person.getMotherId() + "Father Id is " + person.getFatherId() + "Gender is " + person.getGender() + "Region of household is " + person.getHousehold().getRegion() + "Number of children in the HH " + person.getHousehold().getChildren().size());
			}
			*/
			
//        	System.out.println("Number of benefitUnits is " + benefitUnits.size() + " and number of individuals is " + persons.size());
//       	System.out.println("Aggregate HH weight is " + hhweight);
        	
        	//Clean up
        	expandedHouseholdList = null;
        	System.gc();
        	
        } else 
        {
        
        //-------------------------------------------------------------------------------
        //Reconstruct the desired structure of the input population from the input tables
        //-------------------------------------------------------------------------------
        
        if (popSize > inputPersonList.size()) {
        	log.debug("Required sample size reduced from " + popSize + " to " + inputPersonList.size() + "(the maximum population size available).");
        	popSize = inputPersonList.size();
        }


    	//Add benefit units to benefitUnits set
        Set<BenefitUnit> benefitUnitSet = new LinkedHashSet<BenefitUnit>(inputBenefitUnitList);
        List<BenefitUnit> benefitUnitListNoDuplicates = new ArrayList<BenefitUnit>(benefitUnitSet);		//Want to shuffle to remove bias, but cannot do that with a Set (as no ordering, so convert to and iterate over an ArrayList)

        

        // Introduce cut-off to control population size, keeping household structure 
        //as in the original sample.  This is done by scanning through the list of people, adding the 
        //house they belong to to the benefitUnits set, and adding the other household members to the persons set.
        //The process stops when the desired population size has been reached.  Note that there is an added complication
        //in that now we have weighted agents, what measure are we using to calculate the 'population size' - one based
        //on the number of individuals added from the original input database sample, or the weighted number of individuals
        //that represent a larger amount of the population?
        int countPersons = 0;							//Aggregate number of simulated individuals (unweighted - so this is the actual number of simulated objects that undergo the microsimulation process at the start of the simulation)
         
 
        
		        
//        Set<Long> householdIdsNoDuplicates = new LinkedHashSet<>();
//        for(Person person: fullPersonSet) {
//        	householdIdsNoDuplicates.add(person.getHouseholdId());        	        	
//        }
//        List<Long> householdIds = new ArrayList<>(householdIdsNoDuplicates);
//        Collections.shuffle(householdIds, SimulationEngine.getRnd());								//Shuffle to remove any bias as we make a cut-off and remove any excess.
//        ListIterator<Long> hIdIter = householdIds.listIterator();

		//Add houses to benefitUnits set
		Collections.shuffle(benefitUnitListNoDuplicates, SimulationEngine.getRnd());				//Shuffle to remove any bias as we make a cut-off and remove any excess.
        ListIterator<BenefitUnit> h = benefitUnitListNoDuplicates.listIterator();
        
//    	while(hIdIter.hasNext()) {					//Potentially many people to one house, so iterate through houses in the outer loop and people in the inner loop
//    		Long houseId = hIdIter.next();
//    		BenefitUnit house = new BenefitUnit(houseId);
        while(h.hasNext()) {					//Potentially many people to one house, so iterate through houses in the outer loop and people in the inner loop
    		BenefitUnit house = h.next();
    		benefitUnits.add(house);
    		aggregateHouseholdsWeight += house.getWeight();
    		
//    		Set<Person> members = new LinkedHashSet<Person>();		//Create a set of persons who live in the same household
    		Set<Person> members = house.getOtherMembers();		//Create a set of persons who live in the same household
    		//Add people to house and persons set

    		Iterator<Person> p = inputPersonSet.iterator();
            while (p.hasNext()) {
                Person person = p.next(); 
                if(person.getId_benefitUnit() == house.getKey().getId()) {
                	person.setBenefitUnit(house);
                	members.add(person);						//Create shorter set consisting only of people in the same household
                	persons.add(person);						//Add person to persons set to create a set of the desired sample size
                	p.remove();									//Remove persons already added in order to shorten future iterations
                	countPersons++;								//Increment the simulated person counter
                	aggregatePersonsWeight += person.getWeight();
                	
        			int age = person.getDag();
        			if(age < minAgeInInitialPopulation) {
        				minAgeInInitialPopulation = age;
        			} 
        			if(age > maxAgeInInitialPopulation) {
        				maxAgeInInitialPopulation = age;
        			}				

                }
            }
            
            //Add people to the household and attempt to establish partnerships between people, and add 
            // responsible male and female from the father/mother ids (note that there be more than two 
            // generations, so more than one mother id or father id).  Add children to the household if
            // their father id matches the male id, or if their mother id matches the female id.  For 
            // persons originally with the same household with their parents, who are not the male/female
            // of the household, they will end up in the otherMembers, along with other household members
            // who are not children of the male/female.
            for(Person person : members) { 
            	long id = person.getKey().getId();
            	     	
            	//Assign partners
            	if(person.getId_partner() != null) {
            		if(person.getPartner() == null) {
            			for(Person other : members) {            		
    	            		if(person.getId_partner().equals(other.getKey().getId())) {
    	            			if(!other.getId_partner().equals(id)) {		//Check for love triangles
    	                    		throw new IllegalArgumentException("Error - For person " + person.getKey().getId() + " with partnerId " + person.getId_partner() + ", their supposed partner whose id is " + other.getKey().getId() + " has a partnerId " + other.getId_partner() + " which is not equal to the first person.  This implies an unreciprocated partnership (love triangle etc.) in the data!");
    	            			}    	            			
    	            			if(person.getDgn().equals(other.getDgn())) {		//Check for same sex relationships
    	            				//Option of handling same-sex partnerships - Because we model benefitUnits as intrinsically having a responsible male and female, who make decisions on giving birth, we need to split up same-sex couples.
    	            				//NO WE DON'T AT THE MOMENT, I THINK WE CAN RELY ON THE PROCESSES BELOW TO MOVE ONE OF THE EX-PARTNERS OUT OF THE HOME: We check if they are living with parents.  Whoever has a parent who is the responsible male or female of the household will stay in the household, otherwise parents follow the related partner in either staying or moving house, and children stay in the household with the oldest of the partners.  In the unlikely event that the each person in the couple has one of their parents as a responsible male or female, we merely split the couple up (i.e. assign their partner Id to null)

    	            				//Split up same-sex couples (assign null to partner and partnerId)
    	            				person.setPartner(null);
    	            				other.setPartner(null);
    	            				break;
    	            			}
    	            			else {		//Heterosexual partnerships
	    	            			person.setPartner(other);
	    	            			other.setPartner(person);
    	            			}
    	            		}
    	            	}
            		}
            		if(person.getPartner() == null && person.getId_partner() != null) {		//Check for missing partner
            			//Note that for homosexual couples, we have split them up above, however we have also set partnerId to null, hence the exception below should not be thrown.
            			throw new IllegalArgumentException("Error - For person " + person.getKey().getId() + " with householdId " + person.getId_benefitUnit() + ", could not find partner " + person.getId_partner() + " in same household " + house.getKey().getId() + "!");
            		}
            	}

            }
            house.calculateSize();		//Calculate how many simulated (unweighted) persons live in the household
            
            //Terminate process when the desired population size is reached.  But what is the appropriate population size - the unweighted persons or the weighted persons?
           	if(countPersons >= popSize) break;
    	} 
		log.debug("Min Age in the model is " + Parameters.MIN_AGE + ", Max Age in the model is " + Parameters.getMaxAge());
		log.debug("Min Age in the initial population is " + minAgeInInitialPopulation + ", Max Age in the initial population is " + maxAgeInInitialPopulation);

        }
        
        //Below the code is the same for both weighted and unweighted population, iterating through person and benefitUnits lists.
        
        Set<BenefitUnit> newBUsCreated = new LinkedHashSet<BenefitUnit>();				//Create a set of BUs that are newly created by the following process, to add to benefitUnits afterwards (as there is no .add(newBU) method for (set) Iterator, only ListIterator).

        //Handle case where benefitUnits have no responsible male or female.
        //Try to reconstruct a family structure, or create new benefitUnits
        //if there are multiple adults.  Send children to the house of their 
        //mother, or if no mother, their father (or if no father, then promote 
        //them to be responsible male or female of a house on their own).
        Iterator<BenefitUnit> buIter = benefitUnits.iterator();
        while(buIter.hasNext()) {
        	BenefitUnit bu = buIter.next();
        	
//        	//The line below should now always be true
//			if(house.getMale() == null && house.getFemale() == null) {		//(Because of the raw data.)  In this case, all people will end up in otherMembers, so try to reconstruct the family structure by looking at fatherId and motherId
				
        		//After changes made above, all persons start off in household's otherMembers set.
				Set<Person> members = bu.getOtherMembers();		//Cannot have any persons in children set without male or female being non-null, so all persons must be in otherMembers
				Set<Person> otherMembersToRemove = new LinkedHashSet<Person>();		//Create a set of people to move from otherMembers after iteration through (cannot do concurrently as will get an exception)
				Set<BenefitUnit> benefitUnitsWithPotentialParents = new LinkedHashSet<BenefitUnit>();
				benefitUnitsWithPotentialParents.add(bu);		//Include original bu as well as new benefitUnits
				
				
				//Try to build a household around a partnership (if it exists)
				//TODO: Decide whether the following process is too distortive of the population.  A simpler way is used in 
				// the DonorHousehold initializeAttributes method in order to create a household structure in the least
				// distortive way (because it is true static data that we don't want to distort, as there is no need to distort
				// the real data).  One question in particular is the value of the 'otherMembers' field during the simulation;
				// is it worth maintaining, given our assumptions?

				
				//Look for a couple to lead this house as responsible male and female
				//Any additional couple in the house will result in a new house being
				//created with the couple as responsible male and female of that household
				Iterator<Person> pIter = members.iterator();
				while(pIter.hasNext()) {
					Person person = pIter.next();
					Person partner = person.getPartner();

					
					//XXX: What if the partner is already the responsible male or female?  
					// They will not be in the members list! Does this need to assume all 
					// persons start off in the otherMembers collection, or both partners
					// in a partnerships are the responsible adults in the household (because of the way that we initialize
					// the input database, this means that all mother and father ids in one house must be partnerships.
					if(partner == null? false : members.contains(partner)) {		//If partner is in the members set, establish a household as responsible male and female.  Note that as we remove one of the partners as we iterate through members (in the line below), this means that when the iteration reaches the partner, it will skip it so that we do not repeat.

						if(bu.getMale() == null && bu.getFemale() == null) {		//This means, this is the first couple we have found in otherMembers, so will promote them to the responsible male or female of the house
							bu.addResponsibleCouple(person, partner);
						}
						else {		//(The original) house must have had more than one partnership, so we need to create a new household to house the extra couples
							//Note, the partner is handled in the setupNewHome method below
							BenefitUnit newBU = person.setupNewBenefitUnit(false);		//Non-automatic updates, need to manually add to household set and remove people
//							houseIter.add(newHouse);				//No such thing as an .add() method for Iterator, only ListIterator, so need to manually add to benefitUnits set after the iteration
							newBUsCreated.add(newBU);
							benefitUnitsWithPotentialParents.add(newBU);
							
						}
						pIter.remove();			//Remove from otherMembers set and promote to responsible male or female
						otherMembersToRemove.add(partner);		//To avoid concurrency modification exception, remove after iteration through members set
					}
				}
				for(Person person : otherMembersToRemove) {		//Finally remove members who have been moved to responsible male or female roles but could not before because of concurrency modification exception possibilities.
					if(!members.remove(person)) {
						throw new IllegalArgumentException("Person " + person.getKey().getId() + " not removed from otherMembers set of house " + bu.getKey().getId() + "!");
					}
				}
				otherMembersToRemove.clear();
				
				//Now deal with singles.  If they are over the age to leave home, they will become 
				// a responsible male or female, depending on their gender.  They will
				// set-up a new household as the responsible male or female if there 
				// is already a couple who are the responsible male and female in the
				// original household.
				Iterator<Person> pIterSingles = members.iterator();		//Now only single people are left in otherMembers set.
				while(pIterSingles.hasNext()) {
					Person singlePerson = pIterSingles.next();
					if(singlePerson.getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE) {

						if(bu.getMale() == null && bu.getFemale() == null) {		//This means that no partnerships were living in this house (only single people), and this is the first single person iterated in otherMembers, so will promote it to the responsible male or female of the house
							bu.addResponsiblePerson(singlePerson);
						}
						else {
							BenefitUnit newBU = singlePerson.setupNewBenefitUnit(false);		//Non-automatic updates, need to manually add to household set and remove people
//							houseIter.add(newHouse);			//No such thing as an .add() method for Iterator, only ListIterator, so need to manually add to benefitUnits set after the iteration
							newBUsCreated.add(newBU);
							benefitUnitsWithPotentialParents.add(newBU);
						}
						pIterSingles.remove();		//Remove from otherMembers set
					}
				}
				
				
				//Now only 'children', i.e. those too young to leave home should be left.  Try to find their parents.
				/* The child will live with its mother if the child has a non-null idmother, otherwise it will 
				 * live with its father (if the child has a non-null idfather), otherwise it will be an orphan.  
				 * Note that we only look for the child's mother or father in the original household and all new 
				 * benefitUnits that have been created by people who were in the original household but were not able
				 * to be the responsible male or female in the original house.  So, if the child did have mother and/or
				 * father ids, but the mother or father were not in the same household in the original input data, the 
				 * child will be an orphan.
				 */
				Iterator<Person> pIterChildren = members.iterator();
//				BenefitUnit fatherHouse = null;
				while(pIterChildren.hasNext()) {
					Person child = pIterChildren.next();
					
					//Now check for parents in the householdsWithPotentialParents set.
					if(child.getId_mother() != null) {				//If parent exists, child will live with mother by default, and only if no mother exists will the child live with the father
						for(BenefitUnit benefitUnit : benefitUnitsWithPotentialParents) {
							
							boolean moveToMotherHouse = false;
							if(benefitUnit.getId_female() != null && child.getId_mother().equals(benefitUnit.getId_female())) {
								moveToMotherHouse = true;
							}
							else {
								//There exists a benefitUnit in the UK (idhh = 661) where three generations live, where the grandchild is 1 year old, the mother of the grandchild is a single mum of 17, who lives with her mother - a single mum of 52.  So while the 17 year old is assigned to the benefitUnit of her mother, the grandchild is left in otherMembers because we are only checking for parenthood of the responsible adults.  Need to check for all members of the benefitUnit whether they are the mother!
								for(Person potentialTeenageMother: benefitUnit.getChildren()) {
									if(child.getId_mother().equals(potentialTeenageMother.getKey().getId())) {
										moveToMotherHouse = true;
									}
								}
								
								if(moveToMotherHouse == false) {		//Finally, if haven't found mother yet in this house, check otherMembers in case mother is one of them
									for(Person potentialMother: benefitUnit.getOtherMembers()) {
										if(child.getId_mother().equals(potentialMother.getKey().getId())) {
											moveToMotherHouse = true;
										}
									}
								}								
							}

							if(moveToMotherHouse == true) {
								pIterChildren.remove();
								benefitUnit.addChild(child);
								child.setBenefitUnit(benefitUnit);
//								fatherHouse = null;			//In case the father is found before the mother, reset to null so that we do not re-assign the child to the father's house.
								break;	//Need to remove break now, to ensure we update the d_children values for father
							}
						}
					}

					else if(child.getId_father() != null) {
//							if(child.getFatherId() != null) {
//								
//								if(household.getMaleId() != null && child.getFatherId().equals(household.getMaleId())) {							
//									fatherHouse = household;
//								}
//								else {
//									for(Person potentialTeenageFather: household.getChildren()) {
//										if(child.getFatherId().equals(potentialTeenageFather.getKey().getId())) {
//											fatherHouse = household;
//										}
//									}
//									
//									if(fatherHouse == null) {		//Finally, if haven't found father yet in this house, check otherMembers in case father is one of them
//										for(Person potentialFather: household.getOtherMembers()) {
//											if(child.getFatherId().equals(potentialFather.getKey().getId())) {
//												fatherHouse = household;
//											}
//										}
//									}								
//								}							
//							}		
//						}
//						if(fatherHouse != null) {
//							pIterChildren.remove();
//							fatherHouse.addChild(child);
//							child.setHousehold(fatherHouse);
//							fatherHouse = null;
//						}
						
						for(BenefitUnit benefitUnit : benefitUnitsWithPotentialParents) {
							
							boolean moveToFatherHouse = false;
							if(benefitUnit.getId_male() != null && child.getId_father().equals(benefitUnit.getId_male())) {
								moveToFatherHouse = true;
							}
							else {
								//There exists a benefitUnit in the UK (idhh = 661) where three generations live, where the grandchild is 1 year old, the mother of the grandchild is a single mum of 17, who lives with her mother - a single mum of 52.  So while the 17 year old is assigned to the benefitUnit of her mother, the grandchild is left in otherMembers because we are only checking for parenthood of the responsible adults.  Need to check for all members of the benefitUnit whether they are the mother!
								for(Person potentialTeenageFather: benefitUnit.getChildren()) {
									if(child.getId_father().equals(potentialTeenageFather.getKey().getId())) {
										moveToFatherHouse = true;
									}
								}
								
								if(moveToFatherHouse == false) {		//Finally, if haven't found mother yet in this house, check otherMembers in case mother is one of them
									for(Person potentialFather: benefitUnit.getOtherMembers()) {
										if(child.getId_father().equals(potentialFather.getKey().getId())) {
											moveToFatherHouse = true;
										}
									}
								}								
							}

							if(moveToFatherHouse == true) {
								pIterChildren.remove();
								benefitUnit.addChild(child);
								child.setBenefitUnit(benefitUnit);
//								fatherHouse = null;			//In case the father is found before the mother, reset to null so that we do not re-assign the child to the father's house.
								break;	//Need to remove break now, to ensure we update the d_children values for father
							}
						}
					}
					else {		//child has no mother or father
						if(bu.getMale() == null && bu.getFemale() == null) {		//This means that no partnerships were living in this house, nor single people over the age to leave the parental home (only single young people), and this is the first single person iterated in otherMembers, so will promote it to the responsible male or female of the house
							throw new IllegalStateException("ERROR: Cannot find mother or father of child " + child.getKey().getId() + " with age " + child.getDag() + " in household " + bu.getKey().getId() + ", despite having a non-null parent!");
//							house.addResponsiblePerson(child);
//							pIterChildren.remove();
						}
					}
				}
				
				
				//Leftover members are people under the age to move out who do not have mother or father Ids, i.e. orphans (there are 47 entries in the Italian EUROMOD data)
				//Originally, we left them in their original home, with the idea that they would move out when they reach the appropriate age.  However, as they were not listed as children, they were left in the household when a responsible adult left the home (e.g. because they got married).  
				//This resulted in there being benefitUnits with no male and female, which inevitable caused null pointer exceptions.  Therefore, we consider that the data is missing on their parents, but because they are already in a home with a responsible adult,
				//we decide to add these people to children. To do this, it is necessary to assign them a motherId or fatherId.
				Iterator<Person> pIterOrphans = members.iterator();
				while(pIterOrphans.hasNext()) {
					Person orphan = pIterOrphans.next();
//					if(orphan.getAge() < Parameters.AGE_TO_LEAVE_HOME && orphan.getMotherId() == null && orphan.getFatherId() == null) {	//Do one final check (although this may not actually be necessary)
					
						//We check in the orphanGiveParent method to ensure it is only applied to orphans.
						orphan.orphanGiveParent();					//Give orphan a motherId or fatherId based on the female or male fields of the household (assuming female and male are non-null and over the age to move out of home, i.e. an adult).
						pIterOrphans.remove();						//Remove orphan from otherMembers set
						orphan.getBenefitUnit().getChildren().add(orphan);		//Add orphan to children set (cannot use addChild() as it checks ids of mother and father, so directly add to children set).
//					}
				}
//			}				
			// else there exists a male and female id already
		}
        benefitUnits.addAll(newBUsCreated);		//Add newly created BenefitUnit objects to benefitUnits set
		
//		int minAgeInInitialPopulation = Integer.MAX_VALUE;
//		int maxAgeInInitialPopulation = Integer.MIN_VALUE;
//		
//		for (Person person : persons) {
//
//			//The code below is never called, because we have already checked for partnerships and always made them responsible male and female, regardless of age.
//			//When a person is responsible but their partner is not AND no-one of the partner's gender is responsible, promote the parter to be the responsible adult so as to save having to create a new household etc. when leaveHome() is called just to do this simple special case.
//			Person partner = person.getPartner();
//			if(person.isResponsible() && partner != null && !partner.isResponsible()) {
//				
//				if(person.getHouseholdId() != partner.getHouseholdId()) {
//					throw new IllegalArgumentException("person " + person.getKey().getId() + " has different householdId " + person.getHouseholdId() + " to it's partner " + partner.getKey().getId() + ", who has householdId " + partner.getHouseholdId());
//				}
//				if(person.getGender().equals(Gender.Female) && person.getHousehold().getMale() == null) {
//					person.getHousehold().removePerson(partner);		//Remove from otherMembers, where the person must be
//					person.getHousehold().setMale(partner);
//					
//				}
//				else if(person.getGender().equals(Gender.Male) && person.getHousehold().getFemale() == null) {
//					person.getHousehold().removePerson(partner);		//Remove from otherMembers, where the person must be
//					person.getHousehold().setFemale(partner);
//				}
//			}

//			int age = person.getAge();
//			if(age < minAgeInInitialPopulation) {
//				minAgeInInitialPopulation = age;
//			} 
//			if(age > maxAgeInInitialPopulation) {
//				maxAgeInInitialPopulation = age;
//			}
//			
////			if(age > getMaxRetireAge(person.getGender())) {// && !person.getActivity_status().equals(Les_c4.Retired)) {
//			if(age >= Parameters.getFixedRetireAge(year, person.getGender())) {
//				person.setActivity_status(Les_c4.Retired);
//			}						
//		}
//		
//		log.debug("Min Age in the model is " + Parameters.MIN_AGE + ", Max Age in the model is " + Parameters.getMaxAge());
//		log.debug("Min Age in the initial population is " + minAgeInInitialPopulation + ", Max Age in the initial population is " + maxAgeInInitialPopulation);
		
//		// Partition population by Gender, Region and Age
//		personsByGenderRegionAndAge = MultiKeyMap.multiKeyMap(new LinkedMap<>());
//		for(Gender gender : Gender.values()) {
//			for(Region region: Parameters.getCountryRegions()) {
//				for(int age = Parameters.MIN_AGE; age <= Parameters.getMaxAge(); age++) {
//					personsByGenderRegionAndAge.put(gender, region, age, new LinkedHashSet<Person>());		//Only creates MAX_AGE + 1 - MIN_AGE entries, will not keep track of persons that become older than MAX_AGE
//				}
//			}
//		}
		
        System.out.println();
		for (Person person : persons) {

			//Unnecessary as already taken into account above
//			//To make data consistent with the assumptions of the model, after a certain age, move out of household if not the responsible male or female of the household (or if their partner is also not responsible) - this means aged parents whose children are the responsible male or female of the household will also be forced to move out.
//			if(person.getAge() >= Parameters.AGE_TO_LEAVE_HOME) {
//		        if(!person.isResponsible()) {
//		        	person.setupNewHome(true);
//		        }
//			}			
//
//			if(person.getHousehold() == null) {
//				throw new IllegalArgumentException("Person does not have a household!");
//			}
						
//			Gender gender = person.getGender();
//			Region region = person.getRegion();
//			int age = person.getAge();
//			if(age==Parameters.MIN_AGE) {
//				((Set<Person>) personsByGenderRegionAndAge.get(gender, region, age)).add(person);	//Need these values for calculating probability of being in a specific region for people of minimum age 
//			}
//			
//			if(person.getAge() >= ageNonWorkPeopleRetire && person.getActivity_status().equals(Les_c4.Nonwork)) {
//				person.setActivity_status(Les_c4.Retired);			//XXX: This is hack to solve the problem of no donor benefitUnits for non-zero labour supply for old people who have 'Nonwork' status.  I suspect the population data is either incorrect, or different in defining retired status for these people.
//			}			
			
			person.setAdditionalFieldsInInitialPopulation();
			
//			System.out.println("Number of Children per Person,,,,,," + person.getHousehold().getChildren().size());
		}
		initialHoursWorkedWeekly = null;		//Now values have been used to initialise initial population, set to null so garbage collector can reclaim memory

		Set<BenefitUnit> emptyBUs = new LinkedHashSet<>();
		for(BenefitUnit benefitUnit: benefitUnits) {
			if(benefitUnit.getMale() == null && benefitUnit.getFemale() == null) {
				emptyBUs.add(benefitUnit);
//				throw new IllegalArgumentException("BenefitUnit " + house.getKey().getId() + " has no responsible male and female!  The size of children is " + house.getChildren().size() + " and the size of otherMembers is " + house.getOtherMembers().size());
			} else {
				benefitUnit.calculateSize();		//TODO: Adding this here has fixed the problem with 0 size houses in benefitUnits set persisting throughout the simulation.  However, I don't know where exactly the issue was being caused.  It must be that we are adding / removing people to benefitUnits without properly incrementing / decrementing the size field of the household, but I don't know where exactly this occurs during buildObjects.
				benefitUnit.initializeFields();
			}

		}

		//Initialize fields in newly created households
		for (Household household : households) {
			household.initializeFields();
		}


    	log.info("Number of simulated individuals (persons.size()) is " + persons.size() + " living in " + benefitUnits.size() + " simulated benefitUnits.");
    	log.info("Representative size of population is " + aggregatePersonsWeight + " living in " + aggregateHouseholdsWeight + " representative benefitUnits.");

	}

	
	//Requires Labour Market to be initialised and EUROMOD policy scenario for start year to be specified, hence it is called after creating the Labour Market object
	private void initialisePotentialEarningsByWageEquationAndEmployerSocialInsurance() {
		for(Person person: persons) {
			person.updatePotentialEarnings();	//XXX: We override the initialisation of persons' earnings using the estimated wage equation (this may be necessary to ensure the proportional change in unit labour cost is not ridiculously large ~10^100, due to discrepancy between the earnings data in the input database population and the estimated earnings from the wage equation of Bargain et al.  Only once the current discrepancy is fixed (hopefully using our own wage equation), can we start to re-initialise using values in the input data).
		}
	}
	
	
	//Method to remove persons:
	@SuppressWarnings("unchecked")
	public boolean removePerson(Person person) {
		if (person.getDag() <= Parameters.getMaxAge()) {
			
			List<Person> personsByGenderRegionAndAgeList = new ArrayList<Person>(personsByGenderRegionAndAge.get(person.getDgn(), person.getRegion(), person.getDag()));
			boolean removeSuccessful = personsByGenderRegionAndAgeList.remove(person);
//			boolean removeSuccessful = ((List<Person>) personsByGenderRegionAndAge.get(person.getGender(), person.getRegion(), person.getAge())).remove(person);
			return removeSuccessful && persons.remove(person);
		} else {
			return persons.remove(person);
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	public boolean addPerson(Person person) {
		if ( (person.getDag() >= Parameters.MIN_AGE) && (person.getDag() <= Parameters.getMaxAge()) ) {
			List<Person> personsByGenderRegionAndAgeList = new ArrayList<Person>(personsByGenderRegionAndAge.get(person.getDgn(), person.getRegion(), person.getDag()));
//			boolean addSuccessful = ((List<Person>) personsByGenderRegionAndAge.get(person.getGender(), person.getRegion(), person.getAge())).add(person);
			boolean addSuccessful = personsByGenderRegionAndAgeList.add(person);
			return addSuccessful && persons.add(person);
		} else {
			return persons.add(person);
		}
	}


	/*
	initialiseDonorIncomeStatistics() calculates, for each policy, the median income and the threshold to use when deciding whether to use the disposable-to-gross ratio or donor's disposable income directly, in the donor matching procedure.
	 */
	private void initialiseDonorIncomeStatistics() {

		// Calculate median value of gross income of donor benefit units for a given policy
		for(String policyName: Parameters.EUROMODpolicySchedule.values()) { // Iterate over all EM policies included in the donor population
			List<Double> incomesList = new LinkedList<>();

			for (DonorHousehold donorHousehold : euromodOutputHouseholds) {
				double income = donorHousehold.getGrossIncome(policyName);
				incomesList.add(income);
			}

			double[] arr = incomesList.stream().mapToDouble(d -> d).toArray();
			DescriptiveStatistics statistics = new DescriptiveStatistics(arr); // Use incomesArray to calculate statistics
			policyNameIncomeMedianMap.put(policyName, statistics.getPercentile(50)); // Store median income for a given policy

		}

		// Normalise disposable income of donor benefit units by the median value calculated above
		for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
			for (DonorHousehold donorHousehold : euromodOutputHouseholds) {
				double disposableIncome = donorHousehold.getDisposableIncome(policyName);
				double normalisedDisposableIncome = disposableIncome / policyNameIncomeMedianMap.get(policyName);
				donorHousehold.setDisposableIncomeNormalisedForPolicy(policyName, normalisedDisposableIncome);
			}
		}

	}
	
	
	
	

	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

	public Integer getStartYear() {
		return startYear;
	}

	 public Set<Person> getPersons() {
		 return persons;
	 }

	public Set<BenefitUnit> getBenefitUnits() {
		return benefitUnits;
	}

	public Set<Household> getHouseholds() {
		return households;
	}

	public Integer getEndYear() {
		return endYear;
	}

	public Person getPerson(Long id) {

		for (Person person : persons) {
			if ((person.getKey() != null) && (person.getKey().getId() == id))
				return person;
		}
		throw new IllegalArgumentException("Person with id " + id
				+ " is not present!");
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
		this.country = country;
	}
	
	public Integer getPopSize() {
		return popSize;
	}
	
	public void setPopSize(Integer popSize) {
		this.popSize = popSize;
	}

	public int getYear() {
		return year;
	}
	
//	public Integer getMinRetireAgeMales() {
//		return minRetireAgeMales;
//	}
//
//	public Integer getMinRetireAgeFemales() {
//		return minRetireAgeFemales;
//	}
//
//	public void setMinRetireAgeFemales(Integer minRetireAgeFemales) {
//		this.minRetireAgeFemales = minRetireAgeFemales;
//	}
//
//	public void setMinRetireAgeMales(Integer minRetireAgeMales) {
//		this.minRetireAgeMales = minRetireAgeMales;
//	}
//
//	public int getMinRetireAge(Gender gender) {
//		if(gender.equals(Gender.Female)) {
//			return minRetireAgeFemales;
//		} else {
//			return minRetireAgeMales;
//		}
//	}

//	public Integer getMaxRetireAgeMales() {
//		return maxRetireAgeMales;
//	}
//
//	public void setMaxRetireAgeMales(Integer maxRetireAgeMales) {
//		this.maxRetireAgeMales = maxRetireAgeMales;
//	}

//	public Integer getMaxRetireAgeFemales() {
//		return maxRetireAgeFemales;
//	}
//
//	public void setMaxRetireAgeFemales(Integer maxRetireAgeFemales) {
//		this.maxRetireAgeFemales = maxRetireAgeFemales;
//	}
//
//	public int getMaxRetireAge(Gender gender) {
//		if(gender.equals(Gender.Female)) {
//			return maxRetireAgeFemales;
//		} else {
//			return maxRetireAgeMales;
//		}
//	}

	public void setStartYear(Integer startYear) {
		this.startYear = startYear;
	}

	public void setEndYear(Integer endYear) {
		this.endYear = endYear;
	}

	public Integer getTimeTrendStopsIn() {
		return timeTrendStopsIn;
	}

	public Integer getTimeTrendStopsInMonetaryProcesses() {
		return timeTrendStopsInMonetaryProcesses;
	}

	public boolean isFixTimeTrend() {
		return fixTimeTrend;
	}

	public void setFixTimeTrend(boolean fixTimeTrend) {
		this.fixTimeTrend = fixTimeTrend;
	}

	public void setTimeTrendStopsIn(Integer timeTrendStopsIn) {
		this.timeTrendStopsIn = timeTrendStopsIn;
	}

	public Boolean getFixRandomSeed() {
		return fixRandomSeed;
	}

	public void setFixRandomSeed(Boolean fixRandomSeed) {
		this.fixRandomSeed = fixRandomSeed;
	}

	public Long getRandomSeedIfFixed() {
		return randomSeedIfFixed;
	}

	public void setRandomSeedIfFixed(Long randomSeedIfFixed) {
		this.randomSeedIfFixed = randomSeedIfFixed;
	}

	public Integer getsIndexTimeWindow() {
		return sIndexTimeWindow;
	}

	public void setsIndexTimeWindow(Integer sIndexTimeWindow) {
		this.sIndexTimeWindow = sIndexTimeWindow;
	}

	public Double getsIndexAlpha() {
		return sIndexAlpha;
	}

	public void setsIndexAlpha(Double sIndexAlpha) {
		this.sIndexAlpha = sIndexAlpha;
	}

	public Double getsIndexDelta() {
		return sIndexDelta;
	}

	public void setsIndexDelta(Double sIndexDelta) {
		this.sIndexDelta = sIndexDelta;
	}

	public Double getSavingRate() {
		return savingRate;
	}

	public void setSavingRate(Double savingRate) {
		this.savingRate = savingRate;
	}

	public Map<Gender, LinkedHashMap<Region, Set<Person>>> getPersonsToMatch() {
		return personsToMatch;
	}

	public void removeBenefitUnit(BenefitUnit benefitUnit) {
		if(!benefitUnits.remove(benefitUnit)) {
//			throw new IllegalArgumentException("BenefitUnit " + benefitUnit.getKey().getId() + " could not be removed from benefitUnits set!");
		}
		//Removing benefitUnits should also remove it from the household
		if (benefitUnit.getHousehold() != null) {
			benefitUnit.getHousehold().removeBenefitUnitFromHousehold(benefitUnit);
		}
		benefitUnit = null;
	}

	public void removeHousehold(Household household) {
		households.remove(household);
		household = null;
	}

	public int getNumberOfSimulatedPersons() {
		return persons.size();
	}
	
	public int getNumberOfSimulatedHouseholds() {
		return benefitUnits.size();
	}

	public int getPopulationProjection() {
		double numberOfPeople = 0.;
		for (Gender gender : Gender.values()) {
			for(Region region : Parameters.getCountryRegions()) {
				for(int age = 0; age <= Parameters.getMaxAge(); age++) {
					numberOfPeople += ((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue();
				}
			}
		}
		return (int) numberOfPeople;
	}

	public int getPopulationProjectionByAge0_18() {
		return getPopulationProjectionByAge(0,18);
	}

	public int getPopulationProjectionByAge0() {
		return getPopulationProjectionByAge(0,0);
	}

	public int getPopulationProjectionByAge2_10() {
		return getPopulationProjectionByAge(2,10);
	}

	public int getPopulationProjectionByAge19_25() {
		return getPopulationProjectionByAge(19,25);
	}

	public int getPopulationProjectionByAge40_59() {
		return getPopulationProjectionByAge(40,59);
	}

	public int getPopulationProjectionByAge60_79() {
		return getPopulationProjectionByAge(60,79);
	}

	public int getPopulationProjectionByAge80_100() {
		return getPopulationProjectionByAge(80,100);
	}

	public int getPopulationProjectionByAge(int startAge, int endAge) {
		double numberOfPeople = 0.;
		for (Gender gender : Gender.values()) {
			for (Region region : Parameters.getCountryRegions()) {
				for (int age = startAge; age <= endAge; age++) {
					numberOfPeople += ((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, year)).doubleValue();
				}
			}
		}
		int numberOfPeopleScaled = (int) Math.round(numberOfPeople / scalingFactor);
		return numberOfPeopleScaled;
	}
	
	public double getWeightedNumberOfPersons() {
		double sum = 0.;
		for(Person person : persons) {
			if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
				sum += person.getWeight() * scalingFactor;
			}
			else { //If using weights
				sum += person.getWeight();
			}
		}
		return sum;
	}
	
	public double getWeightedNumberOfHouseholds() {
		double sum = 0.;
		for(BenefitUnit house: benefitUnits) {

			if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
				sum += house.getWeight() * scalingFactor;
			}
			else { //If using weights
				sum += house.getWeight();
			}
		}
		return sum;
	}

	public double getWeightedNumberOfHouseholds80minus() {
		double sum = 0.;
		for(BenefitUnit house: benefitUnits) {

			int maleAge = 0;
			int femaleAge = 0;
			if (house.getMale() != null) {
				if(house.getFemale() != null) {
					 maleAge = house.getMale().getDag();
					 femaleAge = house.getFemale().getDag();
				}
				else {
					 maleAge = house.getMale().getDag();
				}
			}
			else {
				 femaleAge = house.getFemale().getDag();
			}


			if (Math.max(maleAge, femaleAge) <= 80 && Math.min(maleAge, femaleAge) >= 0) {
				if(!useWeights) { //If not using weights everyone has weight 1 multiplied by scaling factor
					sum += house.getWeight() * scalingFactor;
				}
				else { //If using weights
					sum += house.getWeight();
				}
			}
		}
		return sum;
	}

	public Map<Long, LinkedHashSet<DonorPerson>> getEuromodOutputPersonsByHouseholdId() {
		return euromodOutputPersonsByHouseholdId;
	}

//	public Integer getAgeNonWorkPeopleRetire() {
//		return ageNonWorkPeopleRetire;
//	}
//
//	public void setAgeNonWorkPeopleRetire(Integer ageNonWorkPeopleRetire) {
//		this.ageNonWorkPeopleRetire = ageNonWorkPeopleRetire;
//	}

	public MultiKeyMap<Object, Double> getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute() {
		return donorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute;
	}

	public Map<Long, Double> getInitialHoursWorkedWeekly() {
		return initialHoursWorkedWeekly;
	}

//	public boolean isRefreshInputDatabase() {
//		return refreshInputDatabase;
//	}
//
//	public void setRefreshInputDatabase(boolean refreshInputDatabase) {
//		this.refreshInputDatabase = refreshInputDatabase;
//	}

	public boolean isInitialisePotentialEarningsFromDatabase() {
		return initialisePotentialEarningsFromDatabase;
	}

	public void setInitialisePotentialEarningsFromDatabase(boolean initialisePotentialEarningsFromDatabase) {
		this.initialisePotentialEarningsFromDatabase = initialisePotentialEarningsFromDatabase;
	}


	public LabourMarket getLabourMarket() {
		return labourMarket;
	}
	
	public boolean isUseWeights() {
		return useWeights;
	}
	
	public void setUseWeights(boolean useWeights) {
		this.useWeights = useWeights;
	}


	public UnionMatchingMethod getUnionMatchingMethod() {
		return unionMatchingMethod;
	}

	public void setUnionMatchingMethod(UnionMatchingMethod unionMatchingMethod) {
		this.unionMatchingMethod = unionMatchingMethod;
	}

	public boolean isAlignFertility() {
		return alignFertility;
	}


	public void setAlignFertility(boolean alignFertility) {
		this.alignFertility = alignFertility;
	}


	public void setYear(int year) {
		this.year = year;
	}


	public boolean getEnableIntertemporalOptimisations() {
		return enableIntertemporalOptimisations;
	}
	public void setEnableIntertemporalOptimisations(boolean enableIntertemporalOptimisations) {
		this.enableIntertemporalOptimisations = enableIntertemporalOptimisations;
	}


	public boolean getUseSavedBehaviour() {
		return useSavedBehaviour;
	}
	public void setUseSavedBehaviour(boolean useSavedBehaviour) {
		this.useSavedBehaviour = useSavedBehaviour;
	}


	public String getFileNameForSavedBehaviour() { return fileNameForSavedBehaviour; }
	public void setFileNameForSavedBehaviour(String fileNameForSavedBehaviour) {
		this.fileNameForSavedBehaviour = fileNameForSavedBehaviour;
	}

	public int getEmploymentOptionsOfPrincipalWorker() { return employmentOptionsOfPrincipalWorker; }
	public void setEmploymentOptionsOfPrincipalWorker(int employmentOptionsOfPrincipalWorker) {
		this.employmentOptionsOfPrincipalWorker = employmentOptionsOfPrincipalWorker;
	}


	public int getEmploymentOptionsOfSecondaryWorker() { return employmentOptionsOfSecondaryWorker; }
	public void setEmploymentOptionsOfSecondaryWorker(int employmentOptionsOfSecondaryWorker) {
		this.employmentOptionsOfSecondaryWorker = employmentOptionsOfSecondaryWorker;
	}


	public boolean getIntertemporalResponsesToHealthStatus() { return intertemporalResponsesToHealthStatus; }
	public void setIntertemporalResponsesToHealthStatus(boolean intertemporalResponsesToHealthStatus) {
		this.intertemporalResponsesToHealthStatus = intertemporalResponsesToHealthStatus;
	}

	public double getScalingFactor() {
		return scalingFactor;
	}


	public boolean getIntertemporalResponsesToEducationStatus() { return intertemporalResponsesToEducationStatus; }
	public void setIntertemporalResponsesToEducationStatus(boolean intertemporalResponsesToEducationStatus) {
		this.intertemporalResponsesToEducationStatus = intertemporalResponsesToEducationStatus;
	}


	public boolean getIntertemporalResponsesToRegion() { return intertemporalResponsesToRegion; }
	public void setIntertemporalResponsesToRegion(boolean intertemporalResponsesToRegion) {
		this.intertemporalResponsesToRegion = intertemporalResponsesToRegion;
	}

	public Map<String, Double> getPolicyNameIncomeMedianMap() {
		return policyNameIncomeMedianMap;
	}

	/*
	policyNameIncomeMedianMap is calculated before the first year is simulated and therefore contains non-uprated gross income values.
	This is then uprated in this method, before being used in BenefitUnit to decide if a ratio or imputation of disposable income should be used.
	 */
	public Double getUpratedMedianIncomeForCurrentYear() {
		String policyName = Parameters.getEUROMODpolicyForThisYear(year);
		Double value = policyNameIncomeMedianMap.get(policyName) * (double) Parameters.upratingFactorsMap.get(year, policyName);
		return value;
	}

	public boolean isLabourMarketCovid19On() {
		return labourMarketCovid19On;
	}

	public void setLabourMarketCovid19On(boolean labourMarketCovid19On) {
		this.labourMarketCovid19On = labourMarketCovid19On;
	}

	public int getAllMatches() {
		return allMatches;
	}

	public int getUnmatchedSize() {
		return unmatchedSize;
	}

	public int getYearMatches() {
		return yearMatches;
	}


}

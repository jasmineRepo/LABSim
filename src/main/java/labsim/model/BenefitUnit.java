package labsim.model;

import java.util.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;

import labsim.data.filters.ValidHomeownersCSfilter;
import labsim.model.enums.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;

import labsim.data.Parameters;
import labsim.experiment.LABSimCollector;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import microsim.statistics.regression.RegressionUtils;
import labsim.model.enums.Les_c4;

import static java.lang.Math.max;
import static java.lang.StrictMath.min;

@Entity
public class BenefitUnit implements EventListener, IDoubleSource, Weight, Comparable<BenefitUnit> {
	
	@Transient
	private static Logger log = Logger.getLogger(BenefitUnit.class);
	
	@Transient
	private final LABSimModel model;
	
	@Transient
	private final LABSimCollector collector;

	//TODO: Needs to be set above the maximum BenefitUnitId number in the input population to prevent collisions (i.e. the creation of a household with the same ID as one already existing).
	//Note: Set to start at 1 now, as the benefitUnitId in the initial population starts at above 8 million
	//Note that although the input population may follow the convention that the person ID is a related to the household ID, this will be difficult to maintain as new benefitUnits will be created as the population leaves their existing benefitUnits to form new benefitUnits (either when they are 18 and leave home, or when they match with a new partner, for example).  This should not be a problem, as we can maintain the link between person and household by reference.
	@Transient
	public static long benefitUnitIdCounter = 1;		//2701500 is the current maximum in EU-SILC, more like 27,000 for EUROMOD.
	
	@Id
	private final PanelEntityKey key;
	
	@Column(name="idfemale")	//XXX: This column is not present in the household table of the input database
	private Long id_female;
	
	@Transient
	private Person female;		//The female head of the household and the mother of the children
	
	@Column(name="idmale")		//XXX: This column is not present in the household table of the input database
	private Long id_male;

	@Column(name="idhh")
	private Long id_household;

	@Transient
	private Household household;

	@Transient
	private Person male;		//The male head of the household and the (possibly step) father of the children
	
	@Column(name="household_weight")
	private double weight;
	
	@Enumerated(EnumType.STRING)
	Occupancy occupancy;
	
	@Column(name="disposable_household_income_monthly")
	private Double disposableIncomeMonthly;

	@Transient
	private boolean disposableIncomeMonthlyImputedFlag;
	
	@Column(name="equivalised_household_disposable_income_yearly")
	private Double equivalisedDisposableIncomeYearly;

	@Transient
	private Double equivalisedDisposableIncomeYearly_lag1;

	@Transient
	private Double yearlyChangeInLogEDI;

	@Column(name="at_risk_of_poverty")
	private Integer atRiskOfPoverty;		//1 if at risk of poverty, defined by an equivalisedDisposableIncomeYearly < 60% of median household's

	@Transient
	private Integer atRiskOfPoverty_lag1 = atRiskOfPoverty; //TODO: calculate in the data and load through the initial population?

//	@Transient
//	private Map<Gender, Person> responsiblePersons;	//Even though we work with male, female most of the time (for historic reasons), this map should make it easier to inspect the objects in certain situations
	
	@Transient
	private Set<Person> children;
	
	@Transient
	private Set<Person> otherMembers;		//Residual household members who are not children, such as aged parents????
	
	private int size;				//Initialize as 0 and increment / decrement to get value

	@Transient	//Temporarily added as new input database does not contain this information	 
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3under;				//Dummy variable for whether the person has children under 4 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_4_12;				//Dummy variable for whether the person has children between 4 and 12 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
	@Transient
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3under_lag;				//Lag(1) of d_children_3under;
	
	@Transient
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_4_12_lag;				//Lag(1) of d_children_4_12;
	
	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_2under;				//Dummy variable for whether the person has children under 3 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3_6;				//Dummy variable for whether the person has children between 3 and 6 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_7_12;				//Dummy variable for whether the person has children between 7 and 12 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_13_17;				//Dummy variable for whether the person has children between 13 and 17 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Transient	//Temporarily added as new input database does not contain this information
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_18over;				//Dummy variable for whether the person has children over 18 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age0")
	private Integer n_children_0;
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age1")
	private Integer n_children_1;
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age2")
	private Integer n_children_2;
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age3")
	private Integer n_children_3;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age4")
	private Integer n_children_4;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age5")
	private Integer n_children_5;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age6")
	private Integer n_children_6;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age7")
	private Integer n_children_7;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age8")
	private Integer n_children_8;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age9")
	private Integer n_children_9;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age10")
	private Integer n_children_10;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age11")
	private Integer n_children_11;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age12")
	private Integer n_children_12;
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age13")
	private Integer n_children_13;
	
//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age14")
	private Integer n_children_14;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age15")
	private Integer n_children_15;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age16")
	private Integer n_children_16;

//	@Transient	//Temporarily added as new input database does not contain this information
	@Column(name="n_children_age17")
	private Integer n_children_17;

	@Transient
	private Integer n_children_allAges = 0; //Number of children of all ages in the household

	@Transient
	private Integer n_children_allAges_lag1 = 0; //Lag(1) of the number of children of all ages in the household

	@Transient
	private Integer n_children_02 = 0; //Number of children aged 0-2 in the household

    @Transient
    private Integer n_children_017 = 0; //Number of children aged 0-17 in the household
    
	@Transient
	private Integer n_children_02_lag1 = 0; //Lag(1) of the number of children aged 0-2 in the household
	
	@Enumerated(EnumType.STRING)
	private Region region;		//Region of household.  Also used in findDonorHouseholdsByLabour method
	
	//New variables:
	//ydses_c5: household income quantiles
	@Enumerated(EnumType.STRING)
	@Column(name="ydses_c5")
	private Ydses_c5 ydses_c5;

	//ydses_c5_lag1: lag(1) of household income quantiles
	@Enumerated(EnumType.STRING)
	@Transient
	private Ydses_c5 ydses_c5_lag1 = null;
	
	//Used in calculation of ydses_c5
	@Transient
	private double tmpHHYpnbihs_dv_asinh = 0.; 

	//dhhtp_c4: household composition
	@Enumerated(EnumType.STRING)
	@Column(name="dhhtp_c4")
	private Dhhtp_c4 dhhtp_c4;

	//dhhtp_c4_lag1: lag(1) of household composition
	@Enumerated(EnumType.STRING)
	@Transient
	private Dhhtp_c4 dhhtp_c4_lag1 = null;
	
	//Equivalised weight to use with variables that are equivalised by household composition
	@Transient
	private double equivalisedWeight = 1.;

	private String createdByConstructor;

	@Column(name="dhh_owned")
	private boolean dhh_owned; // Is any of the individuals in the benefit unit a homeowner? True / false

	@Transient
	ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale = new ArrayList<>();

	@Transient
	ArrayList<Triple<Les_c7_covid, Double, Integer>> covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale = new ArrayList<>(); // This ArrayList stores monthly values of labour market states and gross incomes, to be sampled from by the LabourMarket class, for the female member of the benefit unit


	public BenefitUnit(LABSimModel model) {
		this.model = model;
		collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
		key  = new PanelEntityKey();
	}

	//Used when constructing the houses in the initial population
	public BenefitUnit() {
		super();
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
		collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
		key  = new PanelEntityKey();		//Sets up key
		
		children = new LinkedHashSet<Person>();
		otherMembers = new LinkedHashSet<Person>();
		size = 0;
//		responsiblePersons = new LinkedHashMap<Gender, Person>();
//		responsiblePersons.put(Gender.Male, male);
//		responsiblePersons.put(Gender.Female, female);		
	}
	

	public BenefitUnit(Long id) {
		super();
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
		collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
		key  = new PanelEntityKey(id);		//Sets up key
		
		children = new LinkedHashSet<Person>();
		otherMembers = new LinkedHashSet<Person>();
		size = 0;

		this.d_children_3under = Indicator.False;
		this.d_children_4_12 = Indicator.False;
		this.d_children_3under_lag = Indicator.False;
		this.d_children_4_12_lag = Indicator.False;
		this.d_children_2under = Indicator.False;
		this.d_children_3_6 = Indicator.False;
		this.d_children_7_12 = Indicator.False;
		this.d_children_13_17 = Indicator.False;
		this.d_children_18over = Indicator.False;
		this.n_children_0 = 0;
		this.n_children_1 = 0;
		this.n_children_2 = 0;
		this.n_children_3 = 0;
		this.n_children_4 = 0;
		this.n_children_5 = 0;
		this.n_children_6 = 0;
		this.n_children_7 = 0;
		this.n_children_8 = 0;
		this.n_children_9 = 0;
		this.n_children_10 = 0;
		this.n_children_11 = 0;
		this.n_children_12 = 0;
		this.n_children_13 = 0;
		this.n_children_14 = 0;
		this.n_children_15 = 0;
		this.n_children_16 = 0;
		this.n_children_17 = 0;
		this.n_children_allAges = 0;
		this.n_children_allAges_lag1 = 0;
		this.n_children_02 = 0;
		this.n_children_02_lag1 = 0;
		this.disposableIncomeMonthly = 0.;
		this.equivalisedDisposableIncomeYearly = 0.;
		this.createdByConstructor = "LongID";
		this.disposableIncomeMonthlyImputedFlag = false;

//		responsiblePersons = new LinkedHashMap<Gender, Person>();
//		responsiblePersons.put(Gender.Male, male);
//		responsiblePersons.put(Gender.Female, female);		
	}

	//////////////////////////////////////////////////////////////////////////
	//
	// Constructors for singles
	//
	//////////////////////////////////////////////////////////////////////////
	
	public BenefitUnit(Person person) {
		this(benefitUnitIdCounter++);

		//Set region of household
		region = person.getRegion();
		children = new LinkedHashSet<Person>();
		otherMembers = new LinkedHashSet<Person>();
		size = 0;
		addResponsiblePerson(person);
		household = person.getBenefitUnit().getHousehold(); // New BU should retain link to household.
		id_household = person.getId_household(); //Retain householdId when setting up a new benefit unit. Add benefit units to household method should override this.
		this.disposableIncomeMonthlyImputedFlag = false;
		this.createdByConstructor = "Singles";

	}
	
	
	//////////////////////////////////////////////////////////////////////////
	//
	// Constructors for couples
	//
	//////////////////////////////////////////////////////////////////////////

	public BenefitUnit(Person p1, Person p2) {
		this(benefitUnitIdCounter++);
		children = new LinkedHashSet<Person>();
		otherMembers = new LinkedHashSet<Person>();
		size = 0;
		region = p1.getRegion();
		if(!p2.getRegion().equals(region)) {
			throw new IllegalStateException("ERROR - region of responsible male and female must match!");
		}
		addResponsibleCouple(p1, p2);
		this.disposableIncomeMonthlyImputedFlag = false;
		this.createdByConstructor = "Couples";
	}
	
	//Below is a "copy constructor" for benefitUnits: it takes an original household as input, changes the ID, copies the rest of the benefit units's properties, and creates a new benefit unit.
	public BenefitUnit(BenefitUnit originalBenefitUnit) {
		this(benefitUnitIdCounter++);
		
		this.log = originalBenefitUnit.log;
//		this.female = household.female; //Do I want to copy these?
//		this.male = household.male;
		this.weight = originalBenefitUnit.weight;
		this.occupancy = originalBenefitUnit.occupancy;
		if (originalBenefitUnit.disposableIncomeMonthly != null) {
			this.disposableIncomeMonthly = originalBenefitUnit.disposableIncomeMonthly;
		}
		else {
			this.disposableIncomeMonthly = 0.;
		}

		if (originalBenefitUnit.equivalisedDisposableIncomeYearly != null) {
			this.equivalisedDisposableIncomeYearly = originalBenefitUnit.equivalisedDisposableIncomeYearly;
		}
		else {
			this.equivalisedDisposableIncomeYearly = 0.;
		}
		if (originalBenefitUnit.equivalisedDisposableIncomeYearly_lag1 != null) {
			this.equivalisedDisposableIncomeYearly_lag1 = originalBenefitUnit.equivalisedDisposableIncomeYearly_lag1;
		}
		this.atRiskOfPoverty = originalBenefitUnit.atRiskOfPoverty;
		if (originalBenefitUnit.atRiskOfPoverty_lag1 != null) {
			this.atRiskOfPoverty_lag1 = originalBenefitUnit.atRiskOfPoverty_lag1;
		}
//		this.children = household.children;
//		this.otherMembers = household.otherMembers;
		this.children = new LinkedHashSet<Person>();
		this.otherMembers = new LinkedHashSet<Person>();
		this.size = originalBenefitUnit.size;
		this.d_children_3under = originalBenefitUnit.d_children_3under;
		this.d_children_4_12 = originalBenefitUnit.d_children_4_12;
		this.d_children_3under_lag = originalBenefitUnit.d_children_3under_lag;
		this.d_children_4_12_lag = originalBenefitUnit.d_children_4_12_lag;
		this.d_children_2under = originalBenefitUnit.d_children_2under;
		this.d_children_3_6 = originalBenefitUnit.d_children_3_6;
		this.d_children_7_12 = originalBenefitUnit.d_children_7_12;
		this.d_children_13_17 = originalBenefitUnit.d_children_13_17;
		this.d_children_18over = originalBenefitUnit.d_children_18over;
		this.n_children_0 = originalBenefitUnit.n_children_0;
		this.n_children_1 = originalBenefitUnit.n_children_1;
		this.n_children_2 = originalBenefitUnit.n_children_2;
		this.n_children_3 = originalBenefitUnit.n_children_3;
		this.n_children_4 = originalBenefitUnit.n_children_4;
		this.n_children_5 = originalBenefitUnit.n_children_5;
		this.n_children_6 = originalBenefitUnit.n_children_6;
		this.n_children_7 = originalBenefitUnit.n_children_7;
		this.n_children_8 = originalBenefitUnit.n_children_8;
		this.n_children_9 = originalBenefitUnit.n_children_9;
		this.n_children_10 = originalBenefitUnit.n_children_10;
		this.n_children_11 = originalBenefitUnit.n_children_11;
		this.n_children_12 = originalBenefitUnit.n_children_12;
		this.n_children_13 = originalBenefitUnit.n_children_12;
		this.n_children_14 = originalBenefitUnit.n_children_14;
		this.n_children_15 = originalBenefitUnit.n_children_15;
		this.n_children_16 = originalBenefitUnit.n_children_16;
		this.n_children_17 = originalBenefitUnit.n_children_17;
		this.n_children_allAges = originalBenefitUnit.n_children_allAges;
		this.n_children_allAges_lag1 = originalBenefitUnit.n_children_allAges_lag1;
		this.n_children_02 = originalBenefitUnit.n_children_02;
		this.n_children_02_lag1 = originalBenefitUnit.n_children_02_lag1;
		this.region = originalBenefitUnit.region;
		this.ydses_c5 = originalBenefitUnit.ydses_c5;
		this.ydses_c5_lag1 = originalBenefitUnit.ydses_c5_lag1;
		this.dhhtp_c4 = originalBenefitUnit.dhhtp_c4;
		this.equivalisedWeight = originalBenefitUnit.equivalisedWeight;
		this.dhh_owned = originalBenefitUnit.dhh_owned;
//		this.householdId = originalBenefitUnit.householdId; //
//		this.household = originalBenefitUnit.household;
		this.disposableIncomeMonthlyImputedFlag = false;
		if (originalBenefitUnit.createdByConstructor != null) {
			this.createdByConstructor = originalBenefitUnit.createdByConstructor;
		}
		else {
			this.createdByConstructor = "CopyConstructor";
		}

	}
	
	public BenefitUnit(Person person, Region region) {
		this(benefitUnitIdCounter++);
		children = new LinkedHashSet<Person>();
		otherMembers = new LinkedHashSet<Person>();
		size = 0;
		this.region = region;
		addResponsiblePerson(person);
		this.weight = 1.;
		this.equivalisedWeight = 1.;
		this.disposableIncomeMonthlyImputedFlag = false;
		this.createdByConstructor = "Regions";
	}
	
	

	// ---------------------------------------------------------------------
	// Event Listener
	// ---------------------------------------------------------------------
	
	
	public enum Processes {
		Update,		//This updates the household fields, such as number of children of a certain age
		CalculateChangeInEDI, //Calculate change in equivalised disposable income
		UpdateDisposableIncomeNotAtRiskOfWork,
		Homeownership,
		ReceivesBenefits,
	}
		
	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		case Update:
			updateChildrenFields();
			updateOccupancy();
			updateComposition(); //Update household composition
			updateIncomeVariables();
			break;
		case CalculateChangeInEDI:
			calculateEquivalisedDisposableIncomeYearly(); //Update BU's EDI
			calculateYearlyChangeInLogEquivalisedDisposableIncome(); //Calculate change in EDI
			break;
		case UpdateDisposableIncomeNotAtRiskOfWork:
			updateDisposableIncomeIfNotAtRiskOfWork();
			break;
		case Homeownership:
			homeownership();
			break;
		case ReceivesBenefits:
			setReceivesBenefitsFlag();
			break;
		}
	}

	protected void initializeFields() {
		
		//TODO: Temporarily (until process defining it is specified) initialise ydses_c5 to Q3 so it's not missing:
		if(ydses_c5 == null) {
			ydses_c5 = Ydses_c5.Q3;
		}

		//TODO: Temporarily (until process defining it is specified) initialise dhhtp_c4 to Single No Children:
		if(dhhtp_c4 == null) {
			dhhtp_c4 = Dhhtp_c4.SingleNoChildren;
		}

		if(male != null) {
			if(female != null) {
				occupancy = Occupancy.Couple;
				region = male.getRegion();
				if(region != female.getRegion()) {
					throw new IllegalStateException("Error - male and female have different region fields!");
				}
			}
			else {
				occupancy = Occupancy.Single_Male;
				region = male.getRegion();
			}
		}
		else if (female != null) {
			occupancy = Occupancy.Single_Female;
			region = female.getRegion();
		}
		else {
			//TODO: Remove such benefit unit?
		}
		

		updateChildrenFields();

		//Here we check what the initial value of d_children lags would be by looking at the raw numbers of children - note we exclude new borns (age 0) and go up to an age 1 year older (i.e. n_children_4 for d_children3under_lag, because n_children_4 would have been n_children_3 in the previous year).
		if(n_children_1 > 0 || n_children_2 > 0 || n_children_3 > 0 || n_children_4 > 0) {
			d_children_3under_lag = Indicator.True;
		}
		else {
			d_children_3under_lag = Indicator.False;
		}
		
		if (n_children_5 > 0 || n_children_6 > 0 || n_children_7 > 0  || n_children_8 > 0 || n_children_9 > 0 || n_children_10 > 0 || n_children_11 > 0 || n_children_12 > 0 || n_children_13 > 0) { 
			d_children_4_12_lag = Indicator.True;
		}
		else {
			d_children_4_12_lag = Indicator.False;
		}
		
		updateIncomeVariables();
		updateOccupancy();
		updateComposition();
		updateHouseholdWeighting();
		calculateEquivalisedDisposableIncomeYearly();
	}
	
	protected void updateChildrenFields() {		//XXX: Should we add calculateSize method to be called by update()?  Or just rely on size being correctly incremented / decremented by add / remove methods?
		
		if(children == null){
			children = new LinkedHashSet<Person>();
		}
		
		if(otherMembers == null) {
			otherMembers = new LinkedHashSet<Person>();
		}
		
		//Define lagged values of children variables
		d_children_3under_lag = d_children_3under;
		d_children_4_12_lag = d_children_4_12;
		n_children_allAges_lag1 = n_children_allAges;
		n_children_02_lag1 = n_children_02;
		//Reset child age variables to update
		n_children_0 = 0;
		n_children_1 = 0;
		n_children_2 = 0;
		n_children_3 = 0;
		n_children_4 = 0;
		n_children_5 = 0;
		n_children_6 = 0;
		n_children_7 = 0;
		n_children_8 = 0;
		n_children_9 = 0;
		n_children_10 = 0;
		n_children_11 = 0;
		n_children_12 = 0;
		n_children_13 = 0;
		n_children_14 = 0;
		n_children_15 = 0;
		n_children_16 = 0;
		n_children_17 = 0;
		n_children_allAges = 0;
		n_children_02 = 0;
        n_children_017 = 0;

		d_children_18over = Indicator.False;
		
		for(Person child: children) {
			
			n_children_allAges++;
			
			if(child.getDag() >= 18) {
				d_children_18over = Indicator.True;	//For Labour Supply Regressions, but should always be false because children leave home when they reach 18 in our model.
			}
			

			switch(child.getDag()) {
				case(0) :
					n_children_0++;
					break;
				case(1) :
					n_children_1++;
					break;
				case(2) :
					n_children_2++;
					break;
				case(3) :
					n_children_3++;
					break;
				case(4) :
					n_children_4++;
					break;
				case(5) :
					n_children_5++;
					break;
				case(6) :
					n_children_6++;
					break;
				case(7) :
					n_children_7++;
					break;
				case(8) :
					n_children_8++;
					break;
				case(9) :
					n_children_9++;
					break;
				case(10) :
					n_children_10++;
					break;
				case(11) :
					n_children_11++;
					break;
				case(12) :
					n_children_12++;
					break;
				case(13) :
					n_children_13++;
					break;
				case(14) :
					n_children_14++;
					break;
				case(15) :
					n_children_15++;
					break;
				case(16) :
					n_children_16++;
					break;
				case(17) :
					n_children_17++;
					break;
			}
			
		}
		
		n_children_02 = n_children_0 + n_children_1 + n_children_2; //Number of children aged 0-2 is the sum of children in each age category
        n_children_017 = n_children_0 + n_children_1 + n_children_2 + n_children_3 + n_children_4 + n_children_5 + n_children_6 + n_children_7 + n_children_8 + n_children_9 + n_children_10 +
                n_children_11 + n_children_12 + n_children_13 + n_children_14 + n_children_15 + n_children_16 + n_children_17;
		
		//New fields for Labour Supply Utility Regression calculation
		if(n_children_0 > 0 || n_children_1 > 0 || n_children_2 > 0) { 
			d_children_2under = Indicator.True;
		}
		else {
			d_children_2under = Indicator.False; // This will be updated if a birth occurs.
		}
		
		if (n_children_3 > 0 || n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0) { 
			d_children_3_6 = Indicator.True;
		}
		else {
			d_children_3_6 = Indicator.False;
		}

		if (n_children_7 > 0 || n_children_8 > 0 || n_children_9 > 0 || n_children_10 > 0 || n_children_11 > 0 || n_children_12 > 0) { 
			d_children_7_12 = Indicator.True;
		}
		else {
			d_children_7_12 = Indicator.False;
		}

		if (n_children_13 > 0 || n_children_14 > 0 || n_children_15 > 0 || n_children_16 > 0 || n_children_17 > 0) { 
			d_children_13_17 = Indicator.True;
		}
		else {
			d_children_13_17 = Indicator.False;
		}
		
		//For fields from previous Labour Force Participation Model
		d_children_3under = d_children_2under;
		if(n_children_3 > 0) {
			d_children_3under = Indicator.True;
		}
		
		d_children_4_12 = d_children_7_12;
		if ( n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0){ 
			d_children_4_12 = Indicator.True;
		}
				
	}


			
	// ---------------------------------------------------------------------
	// Labour Market Interaction
	// ---------------------------------------------------------------------

	protected void updateIncomeVariables() {
		ydses_c5_lag1 = ydses_c5; //Store current value as lag(1) before updating
		atRiskOfPoverty_lag1 = atRiskOfPoverty;
		equivalisedDisposableIncomeYearly_lag1 = equivalisedDisposableIncomeYearly;
		disposableIncomeMonthlyImputedFlag = false;
		//Define process determining ydses_c5 for the household - this is currently done in the updateHouseholdsIncome() in LabourMarket because calculation of quantiles requires data on all benefitUnits

	}

	protected void updateComposition() {
		dhhtp_c4_lag1 = dhhtp_c4; //Store current value as lag(1) before updating


		//Use household occupancy and number of children to set dhhtp_c4
		if (occupancy != null) {
			if(occupancy.equals(Occupancy.Couple)) {
				if(n_children_allAges > 0) {
					dhhtp_c4 = Dhhtp_c4.CoupleChildren; //If household is occupied by a couple and number of children is positive, set dhhtp_c4 to "Couple with Children"
				}
				else dhhtp_c4 = Dhhtp_c4.CoupleNoChildren; //Otherwise, set dhhtp_c4 to "Couple without children"

			}
			else {											//Otherwise, household occupied by a single person
				if(n_children_allAges > 0) {
					dhhtp_c4 = Dhhtp_c4.SingleChildren; //If number of children positive, set dhhtp_c4 to "Single with Children"
				}
				else dhhtp_c4 = Dhhtp_c4.SingleNoChildren; //Otherwise, set dhhtp_c4 to "Single without children"
			}
		} else if (male == null && female == null) {
			//TODO: remove the benefit unit? Is it possible to do so here? Otherwise add to a list of units to remove later
		}

//		System.out.println("HHID: " + getKey().getId() + " Occupancy is " + occupancy + " Number of children " + n_children_allAges + "Size of children set " + children.size() +  " Dhhtp_c4 assigned is " + dhhtp_c4);

	}

	protected void updatePotentialEarnings() {
		
		if(male != null) {
			male.updatePotentialEarnings();
		}
		if(female != null) {
			female.updatePotentialEarnings();
		}	
		
	}


	//For each permutation of Labour (or Labour pairs for couples), find a set of 'donor' benefitUnits similar to this household, but with the specified Labour enum value
	public MultiKeyMap<Labour, DonorHousehold> findDonorHouseholdsByLabour() {		

		updateOccupancy();
		
		MultiKeyMap<Labour, DonorHousehold> mostSimilarEUROMODhouseholdsByLabour = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		
		if(occupancy.equals(Occupancy.Couple)) {		//Need to use both partners individual characteristics to determine similar benefitUnits
			
			//Sometimes one of the occupants of the couple will be retired (or even under the age to work, which is currently the age to leave home).  For this case, the person (not at risk of work)'s labour supply will always be zero, while the other person at risk of work has a choice over the single person Labour Supply set.
			Labour[] labourMaleValues;
			if(male.atRiskOfWork()) {
//				labourMaleValues = Labour.values();
//				labourMaleValues = new Labour[] {Labour.ZERO, Labour.TWENTY}; //Can populate arrays used with values manually and use the same enum for males and females
				labourMaleValues = Labour.returnChoicesAllowedForGender(Gender.Male);
			}
			else {
				labourMaleValues = new Labour[]{Labour.ZERO};
			}
			
			Labour[] labourFemaleValues;
			if(female.atRiskOfWork()) {
//				labourFemaleValues = Labour.values();
				labourFemaleValues = Labour.returnChoicesAllowedForGender(Gender.Female);
			}
			else {
				labourFemaleValues = new Labour[]{Labour.ZERO};
			}
			
			for(Labour labourMale: labourMaleValues) {        		 
            	for(Labour labourFemale: labourFemaleValues) {
            		MultiKey<Labour> labourKey = new MultiKey<>(labourMale, labourFemale);
            		DonorHousehold mostSimilarHouse = findMostSimilarEUROMODhouseholdForThisLabour(labourKey);
            		if(mostSimilarHouse != null) {			//Ignore labour cases where no donor household can be found (this is reasonable, considering the fact that there may, for example, be no donor benefitUnits of highly educated benefitUnits working 0 hours per week (at a particular age, for a particular work sector, in a particular region, with a particular number of children etc...)
            			mostSimilarEUROMODhouseholdsByLabour.put(labourKey, mostSimilarHouse);
            		}
            	}
			}
		}
		else {		//For single benefitUnits, no need to check for at risk of work (i.e. retired, sick or student activity status), as this has already been done when passing this household to the labour supply module (see first loop over benefitUnits in LabourMarket#update()).
			if (occupancy.equals(Occupancy.Single_Male)) {
				for (Labour labour : Labour.returnChoicesAllowedForGender(Gender.Male)) {
					MultiKey<Labour> labourKey = new MultiKey<>(labour, null);
					DonorHousehold mostSimilarHouse = findMostSimilarEUROMODhouseholdForThisLabour(labourKey);
					if(mostSimilarHouse != null) {
						mostSimilarEUROMODhouseholdsByLabour.put(labour, null, mostSimilarHouse);
					}
					}
			}
			else { //Must be single female
				for (Labour labour : Labour.returnChoicesAllowedForGender(Gender.Female)) {
					MultiKey<Labour> labourKey = new MultiKey<>(labour, null);
					DonorHousehold mostSimilarHouse = findMostSimilarEUROMODhouseholdForThisLabour(labourKey);
					if(mostSimilarHouse != null) {
						mostSimilarEUROMODhouseholdsByLabour.put(null, labour, mostSimilarHouse);
					}
				}
			}
		}
				
		if(mostSimilarEUROMODhouseholdsByLabour.isEmpty()) {
			String e = "Error - there are no donor benefitUnits for any labour permutations for household " + key.getId() + " in region " + region.getName() + " (" + region + ") with " + children.size() + " children and occupants: ";
			if(male != null) {
				e += "male " + male.getKey().getId() + ", age " + male.getDag() + ", health status " + male.getHealthStatusConversion() + ", potential earnings " + male.getPotentialEarnings() + ", ";
			}
			if(female != null) {
				e += "female " + female.getKey().getId() + ", age " + female.getDag() + ", health status " + female.getHealthStatusConversion() + ", potential earnings " + female.getPotentialEarnings() + ", ";
			}
			throw new IllegalStateException(e);
		}
		return mostSimilarEUROMODhouseholdsByLabour;
	}		
	

	/**
	 * Finds the most similar EUROMOD donor household for the labourKey argument (either a 
	 * single Labour value for a single household occupancy, 
	 * or a MultiKey[Labour, Labour] values in the case that this household is occupied by a couple).
	 *
	 * The method uses Lexicographic Optimisation: we will initially attempt to match all the required 
	 * household characeristics, however if there are no EUROMOD donor benefitUnits that fulfill this requirement,
	 * the method will relax (in the sequence key3 then key2) different characteristics until at least one donor
	 * household has been found.  If, after relaxing key3 and key2, there are still no matching
	 * donor EUROMOD benefitUnits, the method throws an exception.
	 * 
	 * In the event that there are multiple equally similar donor benefitUnits,
	 * this method will sample one at random.
	 *
	 * 
	 * @param labourKey - either a Labour enum (for single occupant household), or a MultiKey[Labour, Labour] for couple occupancy benefitUnits.
	 *  
	 * @return the EUROMOD household deemed most similar to this household, as measured by the characteristics of region, key2 and key3 
	 * (where region is essential, and key2 and/or key3 may be relaxed if necessary in order to find matching benefitUnits).
	 */
	private DonorHousehold findMostSimilarEUROMODhouseholdForThisLabour(MultiKey<Labour> labourKey) {
		return findMostSimilarEUROMODhouseholdForThisLabour(labourKey, true, true);
	}
	public DonorHousehold findMostSimilarEUROMODhouseholdForThisLabour(MultiKey<Labour> labourKey, boolean child_update, boolean consider_min_difference) {

		// working variables
		boolean check_age = false;
		boolean check_health = false;
		boolean check_children = false;
		if (child_update) updateChildrenFields();
		DonorKeys keys = new DonorKeys(this, labourKey);

		// check through multikeys
		Set<DonorHousehold> mostSimilarEUROMODhouseholds = model.getLabourMarket().getEuromodHouseholdsByMultiKeys().get(keys.labourKey, keys.key1, keys.key2, keys.key3, keys.key4);
		if (mostSimilarEUROMODhouseholds != null)
			mostSimilarEUROMODhouseholds = fineTuneDonorSearch(mostSimilarEUROMODhouseholds, check_age, consider_min_difference, check_health, check_children);
		if (mostSimilarEUROMODhouseholds == null) {
			check_age = true;
			mostSimilarEUROMODhouseholds = model.getLabourMarket().getEuromodHouseholdsByMultiKeys().get(keys.labourKey, keys.key1, keys.key2, keys.key3);
			if (mostSimilarEUROMODhouseholds != null)
				mostSimilarEUROMODhouseholds = fineTuneDonorSearch(mostSimilarEUROMODhouseholds, check_age, consider_min_difference, check_health, check_children);
		}
		if (mostSimilarEUROMODhouseholds == null) {
			mostSimilarEUROMODhouseholds = model.getLabourMarket().getEuromodHouseholdsByMultiKeys().get(keys.labourKey, keys.key1, keys.key2);
			if (mostSimilarEUROMODhouseholds != null)
				mostSimilarEUROMODhouseholds = fineTuneDonorSearch(mostSimilarEUROMODhouseholds, check_age, consider_min_difference, check_health, check_children);
		}
		if (mostSimilarEUROMODhouseholds == null) {
			check_children = true;
			mostSimilarEUROMODhouseholds = model.getLabourMarket().getEuromodHouseholdsByMultiKeys().get(keys.labourKey, keys.key1);
			if (mostSimilarEUROMODhouseholds != null)
				mostSimilarEUROMODhouseholds = fineTuneDonorSearch(mostSimilarEUROMODhouseholds, check_age, consider_min_difference, check_health, check_children);
		}
		if (mostSimilarEUROMODhouseholds == null) {
			check_health = true;
			mostSimilarEUROMODhouseholds = model.getLabourMarket().getEuromodHouseholdsByMultiKeys().get(keys.labourKey);
			if (mostSimilarEUROMODhouseholds != null)
				mostSimilarEUROMODhouseholds = fineTuneDonorSearch(mostSimilarEUROMODhouseholds, check_age, consider_min_difference, check_health, check_children);
		}

		// identify return
		if(mostSimilarEUROMODhouseholds == null) {
			if(model.donorFinderCommentsOn)
				log.debug("No donor benefitUnits satisfying the constraints have been found for household " + key.getId() + " and labour " + labourKey + "!");
			return null;
		}
		else {
			if(model.donorFinderCommentsOn)
				log.debug("most similar EUROMOD benefitUnits size " + mostSimilarEUROMODhouseholds.size());
			return RegressionUtils.event(mostSimilarEUROMODhouseholds.toArray(new DonorHousehold[mostSimilarEUROMODhouseholds.size()]), SimulationEngine.getRnd());
		}
	}

	private Set<DonorHousehold> fineTuneDonorSearch(Set<DonorHousehold> mostSimilarEUROMODhouseholds, boolean check_age, boolean consider_min_difference, boolean check_health, boolean check_children) {
		if (check_health)
			mostSimilarEUROMODhouseholds = minimiseHealthStatusDifference(mostSimilarEUROMODhouseholds, true);
		if (check_children && mostSimilarEUROMODhouseholds != null)
			mostSimilarEUROMODhouseholds = minimiseNumberOfChildrenDifference(mostSimilarEUROMODhouseholds);
		// minimise potential earnings difference of occupant(s) to donor benefitUnits, searching progressively further away if cannot find benefitUnits that satisfy subsequent age optimisation.
		if (mostSimilarEUROMODhouseholds != null) {
			TreeMap<Double, LinkedHashSet<DonorHousehold>> housesWithinPotentialEarningsThresholds = minimisePotentialEarningsDifference(mostSimilarEUROMODhouseholds, true, false);
			if (housesWithinPotentialEarningsThresholds != null) {
				for (LinkedHashSet<DonorHousehold> householdsWithinThreshold : housesWithinPotentialEarningsThresholds.values()) {
					if (!householdsWithinThreshold.isEmpty()) {
						if (check_age) {
							mostSimilarEUROMODhouseholds = limitAgeDifference(householdsWithinThreshold, consider_min_difference, true, 80);            //First minimise age difference of occupant(s) to donor benefitUnits
						} else {
							mostSimilarEUROMODhouseholds = householdsWithinThreshold;
						}
						if (mostSimilarEUROMODhouseholds != null) {
							break;
						}
					}
				}
			}
		}
		return mostSimilarEUROMODhouseholds;
	}

	private double getModifiedProportionalDifferenceSquared(double thisHouseValue, double donorHouseValue) {
		double denominator = thisHouseValue;
		if(thisHouseValue < 1.e-2) {
			denominator++;			//We modify the proportional difference to handle small denominator errors by incrementing by 1 (e.g. for case where number of children or earnings is 0).  
		}
		double proportionDiff = (donorHouseValue - thisHouseValue) / denominator;
		return proportionDiff * proportionDiff;
	}

	private double getDifferenceSquared(double thisHouseValue, double donorHouseValue) {
		double diff = (donorHouseValue - thisHouseValue);
		return diff * diff;
	}

	
	/**
	 * Find a set of donor EUROMOD benefitUnits that match the Health Status of this household's occupants.
	 *  
	 * @param relevantHouseholdSet - the set of donor houses to assess for similarity to this household
	 * @param ignoreGender -  boolean to specify whether to allow donor benefitUnits with similar structure (i.e. couple or single) but opposing gender.
	 * @return the set of benefitUnits that are minimise the difference between this household and the donor household's variable(s) of interest.
	 */
	private Set<DonorHousehold> minimiseHealthStatusDifference(Set<DonorHousehold> relevantHouseholdSet, boolean ignoreGender/*, boolean ignoreDiscrepancyConstraint*/) {
		
		Set<DonorHousehold> mostSimilarEUROMODhouseholdsByVariable = new LinkedHashSet<DonorHousehold>();
		Set<DonorHousehold> mostSimilarEUROMODhouseholdsByVariableOneMatch = new LinkedHashSet<DonorHousehold>();
		HealthStatus femaleVar = null, maleVar = null;
		if(female != null) { //*************************************Change variable here************************************
			if(female.getDlltsd().equals(Indicator.True)) { //If long-term sick or disabled, consider to be in poor health
				femaleVar = HealthStatus.Poor;
			} else {
				femaleVar = HealthStatus.Good;
			}
		}
		if(male != null) { //*************************************Change variable here************************************
			if(male.getDlltsd().equals(Indicator.True)) {
				maleVar = HealthStatus.Poor;
			} else {
				maleVar = HealthStatus.Good;
			}
		}
		
		for(DonorHousehold donorHouse : relevantHouseholdSet) {
			
			HealthStatus donorVarMale = null, donorVarFemale = null;
			DonorPerson donorFemale = null, donorMale = null;
			if(donorHouse.getFemale() != null) {
				donorFemale = donorHouse.getFemale();
				donorVarFemale = donorFemale.getHealthStatus();		//*************************************Change variables here************************************
			}
			if(donorHouse.getMale() != null) {
				donorMale = donorHouse.getMale();
				donorVarMale = donorMale.getHealthStatus();			//*************************************Change variables here************************************
			}
			
			if(occupancy.equals(Occupancy.Couple) && donorHouse.getOccupancy().equals(Occupancy.Couple)) {
				if(femaleVar.equals(donorVarFemale) && maleVar.equals(donorVarMale)) {				//Both occupants match
					mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
				}
				else if (femaleVar.equals(donorVarFemale) || maleVar.equals(donorVarMale)) {		//Only one occupant matches
					mostSimilarEUROMODhouseholdsByVariableOneMatch.add(donorHouse);
				}
				
				if(ignoreGender) {
					if(femaleVar.equals(donorVarMale) && maleVar.equals(donorVarFemale)) {				//Both occupants match
						mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
					}
					else if (femaleVar.equals(donorVarMale) || maleVar.equals(donorVarFemale)) {		//Only one occupant matches
						mostSimilarEUROMODhouseholdsByVariableOneMatch.add(donorHouse);
					}
				}
			}
			else if(!occupancy.equals(Occupancy.Couple) && !donorHouse.getOccupancy().equals(Occupancy.Couple)) {
				if(female != null && donorFemale != null) {
					if(femaleVar.equals(donorVarFemale)) {				//Single occupants match
						mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
					}
				}
				else if(male != null && donorMale != null) {
					if(maleVar.equals(donorVarMale)) {					//Single occupants match
						mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
					}
				}
				
				if(ignoreGender) {
					if(female != null && donorMale != null) {
						if(femaleVar.equals(donorVarMale)) {					//Single occupants match
							mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
						}
					}
					else if(male != null && donorFemale != null) {
						if(maleVar.equals(donorVarFemale)) {					//Single occupants match
							mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
						}
					}
				}
			}
		}
		
		if(!mostSimilarEUROMODhouseholdsByVariable.isEmpty()) {
			return mostSimilarEUROMODhouseholdsByVariable;			//Return full occupant matching benefitUnits (two people for couple benefitUnits, or single match for single benefitUnits)
		}
		else if(mostSimilarEUROMODhouseholdsByVariableOneMatch != null && !mostSimilarEUROMODhouseholdsByVariableOneMatch.isEmpty()) {			//Else if there are no dual occupant matches, return one occupant matches for Couple benefitUnits (i.e. we can match one occupant's variable, but not the other occupant's.)
			return mostSimilarEUROMODhouseholdsByVariableOneMatch;			
		}
		else {		//Couldn't find any matches, so just return null so that we can try again with a relevantHouseholdSet that has fewer constraints 
			return null;
		}		
	}	


	/**
	 * Find a set of donor EUROMOD benefitUnits that have the minimum difference with the number of children of this household's occupants.
	 *  
	 * @param relevantHouseholdSet - the set of donor houses to assess for similarity to this household
	 * @return the set of benefitUnits that are minimise the difference between this household and the donor household's variable(s) of interest
	 */
	private Set<DonorHousehold> minimiseNumberOfChildrenDifference(Set<DonorHousehold> relevantHouseholdSet/*, boolean ignoreDiscrepancyConstraint*/) {

		Set<DonorHousehold> mostSimilarEUROMODhouseholdsByVariable = new LinkedHashSet<DonorHousehold>();	
		double var = (double) n_children_allAges;
		
		double discrepancyConstraint = Parameters.childrenNumDiscrepancyConstraint(var);
		double smallestDifferenceSq =  discrepancyConstraint * discrepancyConstraint;		//The sum of square differences between this household and the donor household's relevant variable(s) must be below this value to be included.
		for(DonorHousehold donorHouse : relevantHouseholdSet) {
			double donorVar = donorHouse.getChildren().size();
			double diffSq = getDifferenceSquared(var, donorVar);
			if(diffSq <= smallestDifferenceSq) {
				mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
			}
		}
		
		if(!mostSimilarEUROMODhouseholdsByVariable.isEmpty()) {
			return mostSimilarEUROMODhouseholdsByVariable;
		}
		else { //PB: If empty, start relaxing the discrepancy constraint
			double relaxationFactor = 1.5;
			while (mostSimilarEUROMODhouseholdsByVariable.isEmpty()) {
				discrepancyConstraint = (discrepancyConstraint + 1) * relaxationFactor;
				smallestDifferenceSq = discrepancyConstraint * discrepancyConstraint;
				for(DonorHousehold donorHouse : relevantHouseholdSet) {
					double donorVar = donorHouse.getChildren().size();
					double diffSq = getDifferenceSquared(var, donorVar);
					if(diffSq <= smallestDifferenceSq) {
						mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
					}
				}
			}
			if (!mostSimilarEUROMODhouseholdsByVariable.isEmpty()) return mostSimilarEUROMODhouseholdsByVariable;
			else return null;
		}

	}

	/**
	 * Find a set of donor EUROMOD benefitUnits that have the minimum difference with the age of this household's occupants.
	 *  
	 * @param relevantHouseholdSet - the set of donor houses to assess for similarity to this household
	 * @param ignoreGender -  boolean to specify whether to allow donor benefitUnits with similar structure (i.e. couple or single) but opposing gender.
	 * @return the set of benefitUnits that are minimise the difference between this household and the donor household's variable(s) of interest.
	 */
	private Set<DonorHousehold> limitAgeDifference(LinkedHashSet<DonorHousehold> relevantHouseholdSet, boolean consider_min_difference, boolean ignoreGender, Integer ageTopCode) {
		
		Set<DonorHousehold> mostSimilarEUROMODhouseholdsByVariable = new LinkedHashSet<DonorHousehold>();
		Double femaleVar = null, maleVar = null;
		if(female != null) {
			femaleVar = (double) Math.min(ageTopCode, female.getDag());
		}
		if(male != null) {
			maleVar = (double) Math.min(ageTopCode, male.getDag());
		}
		Double minDiffVal = null;
		DonorHousehold minDiffHouse = null;

		for(DonorHousehold donorHouse : relevantHouseholdSet) {

			Double donorMaleVar = null, donorFemaleVar = null;
			if(donorHouse.getFemale() != null) donorFemaleVar = (double) Math.min(ageTopCode, donorHouse.getFemale().getDag());
			if(donorHouse.getMale() != null) donorMaleVar = (double) Math.min(ageTopCode, donorHouse.getMale().getDag());

			double diffHere = getDiffHere(ignoreGender, maleVar, femaleVar, donorMaleVar, donorFemaleVar);

			// track minimum
			if (minDiffVal == null || diffHere < minDiffVal) {
				minDiffVal = diffHere;
				minDiffHouse = donorHouse;
			}

			// check thresholds
			if(diffHere <= Parameters.AgeDiscrepancyConstraint * Parameters.AgeDiscrepancyConstraint) {
				mostSimilarEUROMODhouseholdsByVariable.add(donorHouse);
			}
		}
		if (consider_min_difference) {
			mostSimilarEUROMODhouseholdsByVariable.add(minDiffHouse);
		}
		if(!mostSimilarEUROMODhouseholdsByVariable.isEmpty()) {
			return mostSimilarEUROMODhouseholdsByVariable;
		}
		else {
			return null;
		}
	}

	private double getDiffHere(boolean ignoreGender, Double maleVar, Double femaleVar, Double donorMaleVar, Double donorFemaleVar) {
		double diff;
		if ( maleVar != null && femaleVar != null) {
			// penalise any potential donor without ages for both members of couple
			if (donorMaleVar == null) donorMaleVar = -100.0;
			if (donorFemaleVar == null) donorFemaleVar = -100.0;
			if (ignoreGender) {
				diff = getDifferenceSquared(max(maleVar, femaleVar), max(donorMaleVar, donorFemaleVar));
				diff += getDifferenceSquared(min(maleVar, femaleVar), min(donorMaleVar, donorFemaleVar));
			} else {
				diff = getDifferenceSquared(maleVar, donorMaleVar);
				diff += getDifferenceSquared(femaleVar, donorFemaleVar);
			}
			diff /= 2.;
		} else {
			if (maleVar != null) {
				if (donorMaleVar == null && donorFemaleVar != null && ignoreGender) {
					diff = getDifferenceSquared(maleVar, donorFemaleVar);
				} else {
					if (donorMaleVar == null) donorMaleVar = - 100.0;
					diff = getDifferenceSquared(maleVar, donorMaleVar);
				}
			} else {
				if (donorFemaleVar == null && donorMaleVar != null && ignoreGender) {
					diff = getDifferenceSquared(femaleVar, donorMaleVar);
				} else {
					if (donorFemaleVar == null) donorFemaleVar = - 100.0;
					diff = getDifferenceSquared(femaleVar, donorFemaleVar);
				}
			}
		}
		return diff;
	}


	/**
	 * Find a set of donor EUROMOD benefitUnits that have the minimum difference with the potential earnings of this household's occupants.
	 *  
	 * @param relevantHouseholdSet - the set of donor houses to assess for similarity to this household
	 * @param ignoreGender -  boolean to specify whether to allow donor benefitUnits with similar structure (i.e. couple or single) but opposing gender.
	 * @param onlySmallestThreshold - boolean to specify whether to return only the household set that satisfies the smallest discrepancy threshold, or if it's empty then return null.
	 * @return the set of benefitUnits that minimise the difference between this household and the donor household's variable(s) of interest
	 */
	private TreeMap<Double, LinkedHashSet<DonorHousehold>> minimisePotentialEarningsDifference(Set<DonorHousehold> relevantHouseholdSet, boolean ignoreGender, boolean onlySmallestThreshold) {

		TreeMap<Double, LinkedHashSet<DonorHousehold>> mostSimilarEUROMODhouseholdsByVariable = new TreeMap<Double, LinkedHashSet<DonorHousehold>>();		//A set of most similar benefitUnits by potential earnings - will choose one at random if there are more than one equally similar benefitUnits in findMostSimilarEUROMODhouseholdForThisLabour(Object labourKey) method
		TreeSet<Double> discrepancyThresholdSet;
		if(onlySmallestThreshold) {
			discrepancyThresholdSet = new TreeSet<>(Arrays.asList(Parameters.EarningsDiscrepancyConstraint.first()));
		}
		else {
			discrepancyThresholdSet = Parameters.EarningsDiscrepancyConstraint;
		}
		for(Double discrepancyThreshold : discrepancyThresholdSet) {
			mostSimilarEUROMODhouseholdsByVariable.put(discrepancyThreshold * discrepancyThreshold, new LinkedHashSet<DonorHousehold>());
		}
		
		Double femaleVar = null, maleVar = null;
		if(female != null) {
			femaleVar = female.getPotentialEarnings();		//*************************************Change variable here************************************
		}
		if(male != null) {
			maleVar = male.getPotentialEarnings();		//*************************************Change variables here************************************
		}		

		for(DonorHousehold donorHouse : relevantHouseholdSet) {
			
			double diffSq = 0., diffSqOppositeGender = 0.;
			Double donorVarMale = null, donorVarFemale = null;
			DonorPerson donorFemale = null, donorMale = null;
			if(donorHouse.getFemale() != null) {
				donorFemale = donorHouse.getFemale();
				donorVarFemale = donorFemale.getHourlyWageGross();		//*************************************Change variables here************************************
			}
			if(donorHouse.getMale() != null) {
				donorMale = donorHouse.getMale();
				donorVarMale = donorMale.getHourlyWageGross();		//*************************************Change variables here************************************
			}
			
			//Use proportional differences as we allow a larger variation in earnings for larger salaries.  Those with lower salaries must be matched more precisely.
			if(occupancy.equals(Occupancy.Couple) && donorHouse.getOccupancy().equals(Occupancy.Couple)) {
				diffSq += getModifiedProportionalDifferenceSquared(maleVar, donorVarMale);
				diffSq += getModifiedProportionalDifferenceSquared(femaleVar, donorVarFemale);	
				diffSq /= 2.;								//Take average per person for consistency with single occupant household case
				for(Double discrepancyThresholdSq : mostSimilarEUROMODhouseholdsByVariable.keySet()) {
					if(diffSq <= discrepancyThresholdSq) {
						Set<DonorHousehold> householdsWithinDiscrepancyThreshold = mostSimilarEUROMODhouseholdsByVariable.get(discrepancyThresholdSq);
						householdsWithinDiscrepancyThreshold.add(donorHouse);
					}
				}
				
				if(ignoreGender) {
					diffSqOppositeGender += getModifiedProportionalDifferenceSquared(maleVar, donorVarFemale);
					diffSqOppositeGender += getModifiedProportionalDifferenceSquared(femaleVar, donorVarMale);
					diffSqOppositeGender /= 2.;				//Take average per person for consistency with single occupant household case
					for(Double discrepancyThresholdSq : mostSimilarEUROMODhouseholdsByVariable.keySet()) {
						if(diffSqOppositeGender <= discrepancyThresholdSq) {
							Set<DonorHousehold> householdsWithinDiscrepancyThreshold = mostSimilarEUROMODhouseholdsByVariable.get(discrepancyThresholdSq);
							householdsWithinDiscrepancyThreshold.add(donorHouse);
						}
					}
				}
			}
			else if(!occupancy.equals(Occupancy.Couple) && !donorHouse.getOccupancy().equals(Occupancy.Couple)) {
				if(female != null && donorFemale != null) {
					diffSq = getModifiedProportionalDifferenceSquared(femaleVar, donorVarFemale);
				}
				else if(male != null && donorMale != null) {
					diffSq = getModifiedProportionalDifferenceSquared(maleVar, donorVarMale);
				}
				for(Double discrepancyThresholdSq : mostSimilarEUROMODhouseholdsByVariable.keySet()) {
					if(diffSq <= discrepancyThresholdSq) {
						Set<DonorHousehold> householdsWithinDiscrepancyThreshold = mostSimilarEUROMODhouseholdsByVariable.get(discrepancyThresholdSq);
						householdsWithinDiscrepancyThreshold.add(donorHouse);
					}
				}
				
				if(ignoreGender) {
					if(female != null && donorMale != null) {
						diffSqOppositeGender = getModifiedProportionalDifferenceSquared(femaleVar, donorVarMale);
					}
					else if(male != null && donorFemale != null) {
						diffSqOppositeGender = getModifiedProportionalDifferenceSquared(maleVar, donorVarFemale);
					}
					for(Double discrepancyThresholdSq : mostSimilarEUROMODhouseholdsByVariable.keySet()) {
						if(diffSqOppositeGender <= discrepancyThresholdSq) {
							Set<DonorHousehold> householdsWithinDiscrepancyThreshold = mostSimilarEUROMODhouseholdsByVariable.get(discrepancyThresholdSq);
							householdsWithinDiscrepancyThreshold.add(donorHouse);
						}
					}
				}
			}
		}
		
		boolean empty = true;
		TreeMap<Double, LinkedHashSet<DonorHousehold>> mostSimilarEUROMODhouseholdsByDiscrepancyThresholdOnPotentialEarnings = new TreeMap<>();	//Make map of discrepancy thresholds, not their square, in order to return.

		for(Double discrepancyThreshold : discrepancyThresholdSet) {
			LinkedHashSet<DonorHousehold> householdsWithinDiscrepancyThreshold = mostSimilarEUROMODhouseholdsByVariable.get(discrepancyThreshold * discrepancyThreshold);
			if(model.donorFinderCommentsOn)
				log.debug("discrepancy " + discrepancyThreshold + ", number of valid houses " + householdsWithinDiscrepancyThreshold.size());
			mostSimilarEUROMODhouseholdsByDiscrepancyThresholdOnPotentialEarnings.put(discrepancyThreshold, householdsWithinDiscrepancyThreshold);
			if(!householdsWithinDiscrepancyThreshold.isEmpty()) {
				empty = false;
			}
		}

		if(!empty) {
			return mostSimilarEUROMODhouseholdsByDiscrepancyThresholdOnPotentialEarnings; 	
		}
		else {
			return null;		//Else is no houses satisfy any of the discrepancy thresholds, then return null
		}
	}	
	
	
	private void addDonorHouseIfMostSimilar(double diffSq, double smallestDifferenceSq, DonorHousehold donorHouse, Set<DonorHousehold> mostSimilarEUROMODhouseholds) {

		if(diffSq < smallestDifferenceSq) {
			smallestDifferenceSq = diffSq;
			mostSimilarEUROMODhouseholds.clear();		//A new nearest BenefitUnit exists, so clear the previous nearest Households.
			mostSimilarEUROMODhouseholds.add(donorHouse);
		}
		else if(diffSq == smallestDifferenceSq) {
			mostSimilarEUROMODhouseholds.add(donorHouse);
		}
	}


	/*
		convertGrossToDisposable method takes a donor household and gross income to convert and:
		i) determines the ratio of gross to disposable income to use
		ii) determines whether the ratio should be applied, or donor's disposable income returned
		ii.i) sets flag indicating whether disposable income is created using the ratio or imputed from the donor directly (in which case it is normalised, and has to be multiplied by the median gross income of simulated benefit units afterwards)
		iii) return Double value of disposable income
	 */
	protected Double convertGrossToDisposable(DonorHousehold donorHousehold, Double grossIncomeToConvert) {
		double disposableIncomeToReturn;
		double donorGrossIncome = donorHousehold.getGrossIncome(); // Gross income of the donor
		double donorDisposableIncome = donorHousehold.getDisposableIncome();
		double ratio = donorHousehold.getDisposableToGrossIncomeRatio(); // Get ratio of disposable to gross income from the donor
		// If donor's gross income is too small, or the ratio too large => don't use the conversion but use disposable income directly
		// getMedianIncomeForCurrentYear() retrieves the median of donor income given the policy name applicable to the current year
		// If donor's gross income is bigger than 10% of the median gross income in the donor population (for a given policy) and the ratio is smaller than Parameters.MAX_EM_DONOR_RATIO, use ratio to convert simulated gross income into net. Otherwise, impute disposable income directly from the donor.
		if (donorGrossIncome > Parameters.PERCENTAGE_OF_MEDIAN_EM_DONOR*model.getUpratedMedianIncomeForCurrentYear() && ratio <= Parameters.MAX_EM_DONOR_RATIO) {
			disposableIncomeToReturn = ratio * grossIncomeToConvert;
			setDisposableIncomeMonthlyImputedFlag(false);
		} else {
			disposableIncomeToReturn = donorDisposableIncome; // Note that this value is uprated to each simulated year
			setDisposableIncomeMonthlyImputedFlag(true);
	}
		return disposableIncomeToReturn;
	}

	public void setReceivesBenefitsFlag() {
		MultiKeyMap<Labour, DonorHousehold> donorHouseholdsByLabourPairs = findDonorHouseholdsByLabour();

		DonorHousehold donorHousehold;

		switch (occupancy) {
			case Couple:
				donorHousehold = donorHouseholdsByLabourPairs.get(male.getLabourSupplyWeekly(), female.getLabourSupplyWeekly());
				if (donorHousehold != null) {
					male.setReceivesBenefitsFlag(donorHousehold.getMale().getReceivesBenefitsFlag());
					female.setReceivesBenefitsFlag(donorHousehold.getFemale().getReceivesBenefitsFlag());
				}
				break;
			case Single_Male:
				donorHousehold = donorHouseholdsByLabourPairs.get(male.getLabourSupplyWeekly(), null);
				if (donorHousehold != null) {
					switch (donorHousehold.getOccupancy()) {
						case Single_Male:
							male.setReceivesBenefitsFlag(donorHousehold.getMale().getReceivesBenefitsFlag());
							break;
						case Single_Female:
							male.setReceivesBenefitsFlag(donorHousehold.getFemale().getReceivesBenefitsFlag());
							break;
						default:
							break;
					}
				}
				break;
			case Single_Female:
				donorHousehold = donorHouseholdsByLabourPairs.get(null, female.getLabourSupplyWeekly());
				if (donorHousehold != null) {
					switch (donorHousehold.getOccupancy()) {
						case Single_Male:
							female.setReceivesBenefitsFlag(donorHousehold.getMale().getReceivesBenefitsFlag());
							break;
						case Single_Female:
							female.setReceivesBenefitsFlag(donorHousehold.getFemale().getReceivesBenefitsFlag());
							break;
						default:
							break;
					}
				}
				break;
			default:
				throw new IllegalStateException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());
		}
	}

	/*
	updateDisposableIncomeIfNotAtRiskOfWork process is used to calculate disposable income for benefit units in which no individual is at risk of work, and which therefore do not enter the updateLabourSupply process.
	There are two cases to consider: i) single benefit units, ii) couples where no individual is at risk of work
	 */
	protected void updateDisposableIncomeIfNotAtRiskOfWork() {
		if (!getAtRiskOfWork()) {
			//Get donor benefitUnits from EUROMOD - the most similar benefitUnits for our criteria, matched by:
			// BenefitUnit characteristics: occupancy, region, number of children;
			// Individual characteristics (potentially for each partner): gender, education, number of hours worked (binned in classes), work sector, health, age;
			// and with minimum difference between gross (market) income.
			MultiKeyMap<Labour, DonorHousehold> donorHouseholdsByLabourPairs = findDonorHouseholdsByLabour();

			//Then get the [zero, null] or [null, zero] or [zero, zero] donors depending on the household composition - but no need to iterate through all of them, as BUs not at risk of work don't choose hours to max. utility.
			if (occupancy.equals(Occupancy.Couple)) {
				MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, Labour.ZERO);
				DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
				double simulatedIncomeToConvert = Math.sinh(male.getYptciihs_dv()) + Math.sinh(female.getYptciihs_dv());
				disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
			}
			else if (occupancy.equals(Occupancy.Single_Male)) {
				MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, null);
				DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
				double simulatedIncomeToConvert = Math.sinh(male.getYptciihs_dv());
				disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
			}
			else if (occupancy.equals(Occupancy.Single_Female)){
				MultiKey<? extends Labour> labourKey = new MultiKey<>(null, Labour.ZERO);
				DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
				double simulatedIncomeToConvert = Math.sinh(female.getYptciihs_dv());
				disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
			}
			else {
				throw new IllegalStateException("Benefit Unit with the following ID has no recognised occupancy: " + getKey().getId());
			}

			calculateBUIncome();
		}
	}

	protected void updateMonthlyLabourSupplyCovid19() {

		// This method calls on predictCovidTransition() method to set labour market state, gross income, and work hours for each month.
		// After 12 months, this should result in an array with 12 values, from which the LabourMarket class can sample one that is representative of the labour market state for the whole year.

		if (occupancy.equals(Occupancy.Couple)) {
			if (male != null && male.atRiskOfWork()) {
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(male); // predictCovidTransition() applies transition models to predict transition and returns new labour market state, gross income, and work hours
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple); // Add the state-gross income-work hours triple to the ArrayList keeping track of all monthly values
			}
			if (female != null && female.atRiskOfWork()) {
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(female);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.add(stateGrossIncomeWorkHoursTriple);
			}
		} else if (occupancy.equals(Occupancy.Single_Male) || occupancy.equals(Occupancy.Single_Female)) {
			//Consider only one person, of either gender, for the transition regressions
			Person person;
			if (occupancy.equals(Occupancy.Single_Male) && male.atRiskOfWork()) {
				person = male;
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(person);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.add(stateGrossIncomeWorkHoursTriple);
			} else if (occupancy.equals(Occupancy.Single_Female) && female.atRiskOfWork()) {
				person = female;
				Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = predictCovidTransition(person);
				covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.add(stateGrossIncomeWorkHoursTriple);
			}
		}
			else {System.out.println("Warning: Incorrect occupancy for benefit unit " + getKey().getId());}
	}

	/*
	chooseRandomMonthlyTransitionAndGrossIncome() method selected a random value out of all the monthly transitions (which contains state to which individual transitions, gross income, and work hours), finds donor benefit unit and calculates disposable income

	 */
	protected void chooseRandomMonthlyOutcomeCovid19() {

		MultiKeyMap<Labour, DonorHousehold> donorHouseholdsByLabourPairs = findDonorHouseholdsByLabour();

		if (occupancy.equals(Occupancy.Couple)) {
			if(male.atRiskOfWork()) { //If male has flexible labour supply
				if(female.atRiskOfWork()) { //And female has flexible labour supply
					if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0 && covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
						int randomIndex = model.getEngine().getRandom().nextInt(min(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size(), covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size())); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
						Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);
						Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);

						male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
						male.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueMale.getLeft()));
						female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
						female.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueFemale.getLeft()));

						// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
						male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight(), Gender.Male)); // Convert predicted work hours to labour enum and update male's value
						female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight(), Gender.Female)); // Convert predicted work hours to labour enum and update female's value
						double simulatedIncomeToConvert = selectedValueMale.getMiddle() + selectedValueFemale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female

						// Find best donor and convert gross income to disposable
						MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), female.getLabourSupplyWeekly());
						DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
						disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
					}
				}
				else if (!female.atRiskOfWork()) { //Male has flexible labour supply, female doesn't
					if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
						int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size()); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
						Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);

						male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
						male.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueMale.getLeft()));

						// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
						male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight(), Gender.Male)); // Convert predicted work hours to labour enum and update male's value
						double simulatedIncomeToConvert = selectedValueMale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female

						// Find best donor and convert gross income to disposable
						MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), Labour.ZERO);
						DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
						disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
					}
				}
			}
			else if(female.atRiskOfWork() && !male.atRiskOfWork()) { //Male not at risk of work - female must be at risk of work since only benefitUnits at risk are considered here
				if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
					int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size()); // Get random int which indicates which monthly value to use. Use smaller value in case male and female lists were of different length.
					Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);

					female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
					female.setLes_c4(Les_c4.convertLes_c7_To_Les_c4(selectedValueFemale.getLeft()));

					// Predicted hours need to be converted back into labour so a donor benefit unit can be found. Then, gross income can be converted to disposable.
					female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight(), Gender.Female)); // Convert predicted work hours to labour enum and update female's value
					double simulatedIncomeToConvert = selectedValueFemale.getMiddle(); // Benefit unit's gross income to convert is the sum of incomes (labour and capital included) of male and female, but in this case male is not at risk of work

					// Find best donor and convert gross income to disposable
					MultiKey<? extends Labour> labourKey = new MultiKey<>(Labour.ZERO, female.getLabourSupplyWeekly());
					DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
					disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
				}
			}
			else throw new IllegalArgumentException("None of the partners are at risk of work! HHID " + getKey().getId());



		} else if (occupancy.equals(Occupancy.Single_Male)) {
			if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size() > 0) {
				int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.size()); // Get random int which indicates which monthly value to use
				Triple<Les_c7_covid, Double, Integer> selectedValueMale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale.get(randomIndex);
				male.setLes_c7_covid(selectedValueMale.getLeft()); // Set labour force status for male
				male.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueMale.getRight(), Gender.Male));
				double simulatedIncomeToConvert = selectedValueMale.getMiddle();

				// Find best donor and convert gross income to disposable
				MultiKey<? extends Labour> labourKey = new MultiKey<>(male.getLabourSupplyWeekly(), null);
				DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
				disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
			}

		} else if (occupancy.equals(Occupancy.Single_Female)) {
			if (covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size() > 0) {
				int randomIndex = model.getEngine().getRandom().nextInt(covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.size());
				Triple<Les_c7_covid, Double, Integer> selectedValueFemale = covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale.get(randomIndex);
				female.setLes_c7_covid(selectedValueFemale.getLeft()); // Set labour force status for female
				female.setLabourSupplyWeekly(Labour.convertHoursToLabour(selectedValueFemale.getRight(), Gender.Female));
				double simulatedIncomeToConvert = selectedValueFemale.getMiddle();

				// Find best donor and convert gross income to disposable
				MultiKey<? extends Labour> labourKey = new MultiKey<>(null, female.getLabourSupplyWeekly());
				DonorHousehold donorHousehold = donorHouseholdsByLabourPairs.get(labourKey);
				disposableIncomeMonthly = convertGrossToDisposable(donorHousehold, simulatedIncomeToConvert);
			}

		} else {System.out.println("Warning: Incorrect occupancy for benefit unit " + getKey().getId());}

		calculateBUIncome();
	}

	/*
	predictCovidTransition() operates at individual level, while typically (in the non-covid labour supply module) income is calculated at the benefit unit level.
	It returns gross income at individual level, which is then totaled in the updateLabourSupplyCovid19() method above and gross income of the benefit unit and converted to disposable at the benefit unit level in the updateLabourSupplyCovid19() method above.
	*/

	private Triple<Les_c7_covid, Double, Integer> predictCovidTransition(Person person) {

		if (person.getLes_c7_covid_lag1() == null) {
			person.initialise_les_c6_from_c4();
			person.setCovidModuleGrossLabourIncome_lag1(person.getCovidModuleGrossLabourIncome_Baseline());
		}


		// Define variables:
		Les_c7_covid stateFrom = person.getLes_c7_covid_lag1();
		Les_c7_covid stateTo = stateFrom; // Labour market state to which individual transitions. Initialise to stateFrom value if the outcome is "no changes"
		int newWorkHours; // Predicted work hours. Initialise to previous value if available, or hours from labour enum.
		Labour newLabour;
		if (person.getNewWorkHours_lag1() != null) {
			newWorkHours = person.getNewWorkHours_lag1();
		} else {
			newWorkHours = person.getLabourSupplyWeekly().getHours(); // Note: prediction for hours is in logs, needs to be transformed to levels
			person.setNewWorkHours_lag1(newWorkHours);
		}
		double grossMonthlyIncomeToReturn = 0; // Gross income to return to updateLabourSupplyCovid19() method

		// Transitions from employment
		if (stateFrom.equals(Les_c7_covid.Employee)) {
			Les_transitions_E1 transitionTo = Parameters.getRegC19LS_E1().eventType(person, Person.DoublesVariables.class, Les_transitions_E1.class);
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (transitionTo.equals(Les_transitions_E1.SelfEmployed) || transitionTo.equals(Les_transitions_E1.SomeChanges)) {

				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_E1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else if (transitionTo.equals(Les_transitions_E1.FurloughedFull)) {
				// If furloughed, don't change hours of work initialised at the beginning
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_E1.FurloughedFlex)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_E2b().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else { // Else "no changes" = employee. Use initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

		// Transitions from furlough full
		} else if (stateFrom.equals(Les_c7_covid.FurloughedFull)) {
			Les_transitions_FF1 transitionTo = Parameters.getRegC19LS_FF1().eventType(person, Person.DoublesVariables.class, Les_transitions_FF1.class);
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

			if (transitionTo.equals(Les_transitions_FF1.Employee)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.FurloughedFlex)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2c().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FF1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

		// Transitions from furlough flex
		} else if (stateFrom.equals(Les_c7_covid.FurloughedFlex)) {
			Les_transitions_FX1 transitionTo = Parameters.getRegC19LS_FX1().eventType(person, Person.DoublesVariables.class, Les_transitions_FX1.class);
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c7 covid method from the enum to convert the outcome to the les c7 scale and update the variable

			if (transitionTo.equals(Les_transitions_FX1.Employee)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2b().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FX1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_FX1.FurloughedFull)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_F2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv()); // 80% of earnings they would have had working normal hours, hence hours predicted as for employed in the line above
			} else if (transitionTo.equals(Les_transitions_FX1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} else { // Else remains furloughed. Use 80% of initialisation value for stateTo and newWorkHours and fill gross monthly income
				grossMonthlyIncomeToReturn = 0.8 * Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			}

		// Transitions from self-employment
		} else if (stateFrom.equals(Les_c7_covid.SelfEmployed)) {
			Les_transitions_S1 transitionTo = Parameters.getRegC19LS_S1().eventType(person, Person.DoublesVariables.class, Les_transitions_S1.class);
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (transitionTo.equals(Les_transitions_S1.Employee) || transitionTo.equals(Les_transitions_S1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_S2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());

				// If transition to is self-employed (i.e. continues in self-employment), and earnings have decreased (gross monthly income lower than lag1 of gross monthly income, obtained from person.getCovidModuleGrossLabourIncome_lag1), predict probabiltiy of SEISS
				if (transitionTo.equals(Les_transitions_S1.SelfEmployed) && grossMonthlyIncomeToReturn < person.getCovidModuleGrossLabourIncome_lag1()) {
					// SEISS probability and effect on income
					boolean receivesSEISS = Parameters.getRegC19LS_S3().event(person, Person.DoublesVariables.class);
					if (receivesSEISS) {
						person.setCovidModuleReceivesSEISS(Indicator.True);
						grossMonthlyIncomeToReturn = 0.8 * person.getCovidModuleGrossLabourIncome_lag1();
					}
				}

			} else if (transitionTo.equals(Les_transitions_S1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			} // No else here as new work hours and gross income are predicted if remains self-employed (above)

		// Transitions from non-employment
		} else if (stateFrom.equals(Les_c7_covid.NotEmployed)) {
			Les_transitions_U1 transitionTo = Parameters.getRegC19LS_U1().eventType(person, Person.DoublesVariables.class, Les_transitions_U1.class);
			stateTo = transitionTo.convertToLes_c7_covid();
			person.setLes_c7_covid(stateTo); // Use convert to les c6 covid method from the enum to convert the outcome to the les c6 scale and update the variable

			if (transitionTo.equals(Les_transitions_U1.Employee) || transitionTo.equals(Les_transitions_U1.SelfEmployed)) {
				newWorkHours = (Labour.convertHoursToLabour(exponentiateAndConstrainWorkHoursPrediction(Parameters.getRegC19LS_U2a().getScore(person, Person.DoublesVariables.class)), person.getDgn())).getHours();
				grossMonthlyIncomeToReturn = Parameters.WEEKS_PER_MONTH_RATIO * (person.getPotentialEarnings()*newWorkHours) + Math.sinh(person.getYptciihs_dv());
			} else if (transitionTo.equals(Les_transitions_U1.NotEmployed)) {
				newWorkHours = 0;
				grossMonthlyIncomeToReturn = 0;
			}

		} else {
		//	System.out.println("Warning: Person " + person.getKey().getId() + " entered Covid-19 transitions process, but doesn't have correct starting labour market state which was " + stateFrom);
		}

		Triple<Les_c7_covid, Double, Integer> stateGrossIncomeWorkHoursTriple = Triple.of(stateTo, grossMonthlyIncomeToReturn, newWorkHours); // Triple contains outcome labour market state after transition, gross income, and work hours
		person.setCovidModuleGrossLabourIncome_lag1(grossMonthlyIncomeToReturn); // used as a regressor in the Covid-19 regressions
		person.setNewWorkHours_lag1(newWorkHours); // newWorkHours is not a state variable of a person and is only used from month to month in the covid module, so set lag here
		person.setLes_c7_covid_lag1(stateTo); // Update lagged value of monthly labour market state
		return stateGrossIncomeWorkHoursTriple;
	}

	private double exponentiateAndConstrainWorkHoursPrediction(double workHours) {
		double workHoursConverted = Math.exp(workHours);
		if (workHoursConverted < 0) {
			return 0.;
		} else return workHoursConverted;
	}

	protected void updateLabourSupply() {

//	if (model.getCountry() == Country.UK)

//		//Update potential earnings
		updatePotentialEarnings();		//XXX: Note that only benefitUnits at risk of work are updated here.  The full set of benefitUnits' potential earnings are updated outside of the labour market module in the marriage matching module.
				
		//Get donor benefitUnits from EUROMOD - the most similar benefitUnits for our criteria, matched by:
		// BenefitUnit characteristics: occupancy, region, number of children;
		// Individual characteristics (potentially for each partner): gender, education, number of hours worked (binned in classes), work sector, health, age;
		// and with minimum difference between gross (market) income.
		MultiKeyMap<Labour, DonorHousehold> donorHouseholdsByLabourPairs = findDonorHouseholdsByLabour();	
		
		//Calculate Labour Supply Utility regression scores
		MultiKeyMap<Labour, Double> disposableIncomeMonthlyByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		MultiKeyMap<Labour, Double> labourSupplyUtilityExponentialRegressionScoresByLabourPairs = MultiKeyMap.multiKeyMap(new LinkedMap<>());
		
		//Sometimes one of the occupants of the couple will be retired (or even under the age to work, which is currently the age to leave home).  For this case, the person (not at risk of work)'s labour supply will always be zero, while the other person at risk of work has a choice over the single person Labour Supply set.
		if(occupancy.equals(Occupancy.Couple)) {
			for(MultiKey<? extends Labour> map: donorHouseholdsByLabourPairs.keySet()) { //PB: for each possible discrete number of hours
				DonorHousehold donorHouse = donorHouseholdsByLabourPairs.get(map); //PB: get donor household
				//Sets values for regression score calculation
				male.setLabourSupplyWeekly(map.getKey(0));
				female.setLabourSupplyWeekly(map.getKey(1));				
				log.debug("household " + key.getId() + ", male labour supply " + male.getLabourSupplyWeekly() + ", female labour supply " + female.getLabourSupplyWeekly() + ", male potential earnings " + male.getPotentialEarnings() + ", female potential earnings " + female.getPotentialEarnings());

				//Earnings are composed of the labour income and non-benefit non-employment income Yptciihs_dv() (this is monthly, so no need to multiply by WEEKS_PER_MONTH_RATIO)
				double simulatedIncomeToConvert = Parameters.WEEKS_PER_MONTH_RATIO * (male.getPotentialEarnings()*male.getLabourSupplyWeekly().getHours() + female.getPotentialEarnings()*female.getLabourSupplyWeekly().getHours()) + Math.sinh(male.getYptciihs_dv()) + Math.sinh(female.getYptciihs_dv());
				disposableIncomeMonthly = convertGrossToDisposable(donorHouse, simulatedIncomeToConvert);

				//Note that only benefitUnits at risk of work are considered, so at least one partner is at risk of work
				
				double exponentialRegressionScore = 0.;
				if(male.atRiskOfWork()) { //If male has flexible labour supply
					if(female.atRiskOfWork()) { //And female has flexible labour supply
						//Follow utility process for couples
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityCouples().getScore(this, BenefitUnit.Regressors.class));
					}
					else if (!female.atRiskOfWork()) { //Male has flexible labour supply, female doesn't
						//Follow utility process for single males for the UK
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityMalesWithDependent().getScore(this, BenefitUnit.Regressors.class));
						//In Italy, this should follow a separate set of estimates. One way is to differentiate between countries here; another would be to add a set of estimates for both countries, but for the UK have the same number as for singles
						//Introduced a new category of estimates, Males/Females with Dependent to be used when only one of the couple is flexible in labour supply. In Italy, these have a separate set of estimates; in the UK they use the same estimates as "independent" singles
					}
				}
				else if(female.atRiskOfWork() && !male.atRiskOfWork()) { //Male not at risk of work - female must be at risk of work since only benefitUnits at risk are considered here
					//Follow utility process for single female
					exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityFemalesWithDependent().getScore(this, BenefitUnit.Regressors.class));
				}
				else throw new IllegalArgumentException("None of the partners are at risk of work! HHID " + getKey().getId()); 
				
				disposableIncomeMonthlyByLabourPairs.put(map, disposableIncomeMonthly);
				labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(map, exponentialRegressionScore); //XXX: Adult children could contribute their income to the hh, but then utility would have to be joint for a household with adult children, and they couldn't be treated separately as they are at the moment?
				
//				System.out.println("For donor household disposable income is " + donorHouse.getDisposableIncomeToGrossEarningsRatio());
			}
		}
		else {
			if(occupancy.equals(Occupancy.Single_Male)) {

				for(MultiKey<? extends Labour> map: donorHouseholdsByLabourPairs.keySet()) {
					DonorHousehold donorHouse = donorHouseholdsByLabourPairs.get(map);
					
					male.setLabourSupplyWeekly(map.getKey(0));
					double simulatedIncomeToConvert = Parameters.WEEKS_PER_MONTH_RATIO * (male.getPotentialEarnings()*male.getLabourSupplyWeekly().getHours()) + Math.sinh(male.getYptciihs_dv());
					disposableIncomeMonthly = convertGrossToDisposable(donorHouse, simulatedIncomeToConvert);

//					log.debug("BenefitUnit disposableIncomeMonthly " + disposableIncomeMonthly);
//					double exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityMales().getScore(this, BenefitUnit.Regressors.class));
					double exponentialRegressionScore;
					if (male.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for male adult children
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityACMales().getScore(this, BenefitUnit.Regressors.class));
					} else {
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityMales().getScore(this, BenefitUnit.Regressors.class));
					}
					
					disposableIncomeMonthlyByLabourPairs.put(map, disposableIncomeMonthly);
					labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(map, exponentialRegressionScore);

				}
			}
			else if (occupancy.equals(Occupancy.Single_Female)) {		//Occupant must be a single female
				for(MultiKey<? extends Labour> map: donorHouseholdsByLabourPairs.keySet()) {
					DonorHousehold donorHouse = donorHouseholdsByLabourPairs.get(map);
					
					female.setLabourSupplyWeekly(map.getKey(1));
					double simulatedIncomeToConvert = Parameters.WEEKS_PER_MONTH_RATIO * (female.getPotentialEarnings()*female.getLabourSupplyWeekly().getHours()) + Math.sinh(female.getYptciihs_dv());
					disposableIncomeMonthly = convertGrossToDisposable(donorHouse, simulatedIncomeToConvert);

//					double exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityFemales().getScore(this, BenefitUnit.Regressors.class));
					double exponentialRegressionScore;
					if (female.getAdultChildFlag() == 1) { //If adult children use labour supply estimates for female adult children
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityACFemales().getScore(this, BenefitUnit.Regressors.class));
					} else {
						exponentialRegressionScore = Math.exp(Parameters.getRegLabourSupplyUtilityFemales().getScore(this, BenefitUnit.Regressors.class));
					}
					
					disposableIncomeMonthlyByLabourPairs.put(map, disposableIncomeMonthly);
					labourSupplyUtilityExponentialRegressionScoresByLabourPairs.put(map, exponentialRegressionScore);
				}
			}			
		}

		if(labourSupplyUtilityExponentialRegressionScoresByLabourPairs.isEmpty()) {
			System.out.print("\nlabourSupplyUtilityExponentialRegressionScoresByLabourPairs for household " + key.getId() + " with occupants ");
			if(male != null) {
				System.out.print("male : " + male.getKey().getId() + ", ");
			}
			if(female != null) {
				System.out.print("female : " + female.getKey().getId() + ", ");
			}
			System.out.print("is empty!");
		}
		//Sample labour supply from possible labour (pairs of) values
		MultiKey<? extends Labour> labourSupplyChoice = RegressionUtils.event(labourSupplyUtilityExponentialRegressionScoresByLabourPairs, false);
		if(model.debugCommentsOn) {
			log.debug("labour supply choice " + labourSupplyChoice);
		}
		if(occupancy.equals(Occupancy.Couple)) {
			male.setLabourSupplyWeekly(labourSupplyChoice.getKey(0));
			female.setLabourSupplyWeekly(labourSupplyChoice.getKey(1));
		}
		else {
			if(occupancy.equals(Occupancy.Single_Male)) {
				male.setLabourSupplyWeekly(labourSupplyChoice.getKey(0));
			}
			else {		//Occupant must be single female
				female.setLabourSupplyWeekly(labourSupplyChoice.getKey(1));
			}
		}
		disposableIncomeMonthly = disposableIncomeMonthlyByLabourPairs.get(labourSupplyChoice);

		//Update gross income variables for the household and all occupants:
		calculateBUIncome();

		
	}
		

	/////////////////////////////////////////////////////////////////////////////////
	//
	//	Other Methods
	//
	////////////////////////////////////////////////////////////////////////////////


	protected void calculateBUIncome() {

		calculateEquivalisedWeight(); //Calculates equivalised weight to use with HH-income variable

		/*
		 * This method updates income variables for responsible persons in the household and household
		 *
		 * BenefitUnit income quintiles is made up of labour, pension, miscellaneous (see comment for definition), Trade Union / Friendly Society Payment and maintenace or alimony .
		 * Gross household income combination of spouses income if partnered or personal income if single. Adjusted for household composition using OECD-modified scale and inflation.
		 * Normalised using inverse hyperbolic sine.
		 *
		 * labour income = yplgrs_dv at person level
		 * non-employment non-benefit income = yptciihs_dv (from income process in the simulation)
		 * non-benefit income = yptciihs_dv + yplgrs_dv (non-employment non-benefit income from regression + labour income from wages and LS should give non-benefit income)
		 * ydses should be based on non-benefit income, ypnbihs_dv, which is emp income + non-emp non-ben income
		 *
		 * 1. Get yptciihs_dv
		 * 2. Get yplgrs_dv
		 * 3. Update ypnbihs_dv
		 * 4. Update ydses_c5
		 *
		 */

		//TODO: Use Math.sinh() functions to get the antilog of asinh() values. Maybe use Math.asinh() function from apache commons math3 to calculate the asinh, instead of doing it manually

				if(getOccupancy().equals(Occupancy.Couple)) {

					Person male = getMale();
					Person female = getFemale();

					if(male != null && female != null) {
						//Male and female non-benefit income is the sum of non-employment non-benefit income and hours of work * hourly wage * weeks per month (as income is monthly in the UKHLS)
						double labourEarningsMale = male.getLabourSupplyWeekly().getHours() * male.getPotentialEarnings() * Parameters.WEEKS_PER_MONTH_RATIO; //Level of monthly labour earnings
						male.setYplgrs_dv(Math.log(labourEarningsMale + Math.sqrt(labourEarningsMale * labourEarningsMale + 1.0))); //This follows asinh transform of labourEarnings
						double YpnbihsMale = labourEarningsMale + Math.sinh(male.getYptciihs_dv()); //In levels
						male.setYpnbihs_dv(Math.log(YpnbihsMale + Math.sqrt(YpnbihsMale * YpnbihsMale + 1.0))); //Set asinh transformed

						male.setCovidModuleGrossLabourIncome_Baseline(YpnbihsMale); // Used in the Covid-19 labour supply module

						double labourEarningsFemale = female.getLabourSupplyWeekly().getHours() * female.getPotentialEarnings() * Parameters.WEEKS_PER_MONTH_RATIO; //Level of monthly labour earnings
						female.setYplgrs_dv(Math.log(labourEarningsFemale + Math.sqrt(labourEarningsFemale * labourEarningsFemale + 1.0))); //This follows asinh transform of labourEarnings
						double YpnbihsFemale = labourEarningsFemale + Math.sinh(female.getYptciihs_dv()); //In levels
						female.setYpnbihs_dv(Math.log(YpnbihsFemale + Math.sqrt(YpnbihsFemale * YpnbihsFemale + 1.0))); //Set asinh transformed

						female.setCovidModuleGrossLabourIncome_Baseline(YpnbihsFemale); // Used in the Covid-19 labour supply module

						//BenefitUnit income is the sum of male and female non-benefit income
						double tmpHHYpnbihs_dv = (YpnbihsMale + YpnbihsFemale) / equivalisedWeight; //Equivalised
						setTmpHHYpnbihs_dv_asinh(Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0))); //Asinh transformation of HH non-benefit income
//						System.out.println("HHID " + getKey().getId() + " Occupancy " + getOccupancy() + " Female LS " + female.getLabourSupplyWeekly().getHours() + " Wages " + female.getPotentialEarnings() +
//								" Yplgrs_Dv " + female.getYplgrs_dv() + "Ypnbihs_dv " + female.getYpnbihs_dv() + " Yptciihs_dv " + female.getYptciihs_dv() + " Male LS " + male.getLabourSupplyWeekly().getHours() + " Wages " + male.getPotentialEarnings() +
//								" Yplgrs_Dv " + male.getYplgrs_dv() + "Ypnbihs_dv " + male.getYpnbihs_dv() + " Yptciihs_dv " + male.getYptciihs_dv() +  " Ydses " + getTmpHHYpnbihs_dv_asinh() + " ### " + " x is " + tmpHHYpnbihs_dv + " sqrt(x) " + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0) + " asinh(x) " + Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0)));

						//Based on the percentiles calculated by the collector, assign household to one of the quintiles of (equivalised) income distribution
						if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
							if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p20()) {
								ydses_c5 = Ydses_c5.Q1;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p40()) {
								ydses_c5 = Ydses_c5.Q2;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p60()) {
								ydses_c5 = Ydses_c5.Q3;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p80()) {
								ydses_c5 = Ydses_c5.Q4;
							}
							else {
								ydses_c5 = Ydses_c5.Q5;
							}

						}

					}
				}
				else if(getOccupancy().equals(Occupancy.Single_Male)) {

					Person male = getMale();

					if(male != null) {

						double labourEarningsMale = male.getLabourSupplyWeekly().getHours() * male.getPotentialEarnings() * Parameters.WEEKS_PER_MONTH_RATIO; //Level of monthly labour earnings
						male.setYplgrs_dv(Math.log(labourEarningsMale + Math.sqrt(labourEarningsMale * labourEarningsMale + 1.0))); //This follows asinh transform of labourEarnings
						double YpnbihsMale = labourEarningsMale + Math.sinh(male.getYptciihs_dv()); //In levels
						male.setYpnbihs_dv(Math.log(YpnbihsMale + Math.sqrt(YpnbihsMale * YpnbihsMale + 1.0))); //Set asinh transformed

						male.setCovidModuleGrossLabourIncome_Baseline(YpnbihsMale); // Used in the Covid-19 labour supply module

						//BenefitUnit income is the male non-benefit income
						double tmpHHYpnbihs_dv = YpnbihsMale / equivalisedWeight; //Equivalised
						setTmpHHYpnbihs_dv_asinh(Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0))); //Asinh transformation of HH non-benefit income
//						System.out.println("HHID " + getKey().getId() + " Occupancy " + getOccupancy() + " Male LS " + male.getLabourSupplyWeekly().getHours() + " Wages " + male.getPotentialEarnings() +
//								" Yplgrs_Dv " + male.getYplgrs_dv() + "Ypnbihs_dv " + male.getYpnbihs_dv() + " Yptciihs_dv " + male.getYptciihs_dv() +  " Ydses " + getTmpHHYpnbihs_dv_asinh() + " ### " + " x is " + tmpHHYpnbihs_dv + " sqrt(x) " + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0) + " asinh(x) " + Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0)));

						if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
							if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p20()) {
								ydses_c5 = Ydses_c5.Q1;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p40()) {
								ydses_c5 = Ydses_c5.Q2;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p60()) {
								ydses_c5 = Ydses_c5.Q3;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p80()) {
								ydses_c5 = Ydses_c5.Q4;
							}
							else {
								ydses_c5 = Ydses_c5.Q5;
							}
						}


					}

				}
				else {

					Person female = getFemale();

					if(female != null) {

						//If not a couple nor a single male, occupancy must be single female
						double labourEarningsFemale = female.getLabourSupplyWeekly().getHours() * female.getPotentialEarnings() * Parameters.WEEKS_PER_MONTH_RATIO; //Level of monthly labour earnings
						female.setYplgrs_dv(Math.log(labourEarningsFemale + Math.sqrt(labourEarningsFemale * labourEarningsFemale + 1.0))); //This follows asinh transform of labourEarnings
						double YpnbihsFemale = labourEarningsFemale + Math.sinh(female.getYptciihs_dv()); //In levels
						female.setYpnbihs_dv(Math.log(YpnbihsFemale + Math.sqrt(YpnbihsFemale * YpnbihsFemale + 1.0))); //Set asinh transformed

						female.setCovidModuleGrossLabourIncome_Baseline(YpnbihsFemale); // Used in the Covid-19 labour supply module

						//BenefitUnit income is the female non-benefit income
						double tmpHHYpnbihs_dv = YpnbihsFemale / equivalisedWeight; //Equivalised
						setTmpHHYpnbihs_dv_asinh(Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0))); //Asinh transformation of HH non-benefit income
//						System.out.println("HHID " + getKey().getId() + " Occupancy " + getOccupancy() + " Female LS " + female.getLabourSupplyWeekly().getHours() + " Wages " + female.getPotentialEarnings() +
//								" Yplgrs_Dv " + female.getYplgrs_dv() + "Ypnbihs_dv " + female.getYpnbihs_dv() + " Yptciihs_dv " + female.getYptciihs_dv() +  " Ydses " + getTmpHHYpnbihs_dv_asinh() + " ### " + " x is " + tmpHHYpnbihs_dv + " sqrt(x) " + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0) + " asinh(x) " + Math.log(tmpHHYpnbihs_dv + Math.sqrt(tmpHHYpnbihs_dv * tmpHHYpnbihs_dv + 1.0)));

						if(collector.getStats() != null) { //Collector only gets initialised when simulation starts running
							if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p20()) {
								ydses_c5 = Ydses_c5.Q1;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p40()) {
								ydses_c5 = Ydses_c5.Q2;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p60()) {
								ydses_c5 = Ydses_c5.Q3;
							}
							else if(tmpHHYpnbihs_dv_asinh <= collector.getStats().getYdses_p80()) {
								ydses_c5 = Ydses_c5.Q4;
							}
							else {
								ydses_c5 = Ydses_c5.Q5;
							}
						}

				}
			}
		}


	private void updateHouseholdWeighting() {
		if(female == null) {
			weight = male.getWeight();
		}
		else if(male == null) {
			weight = female.getWeight();
		}
		else {
			weight = (female.getWeight() + male.getWeight()) * 0.5;		//TODO: Arithmetic average - is this correct?			
		}
	}

	public void addResponsiblePerson(Person person) {

		if(person.getDgn().equals(Gender.Female)) {
			setFemale(person);
			if(male != null) {
				occupancy = Occupancy.Couple;
			}
			else occupancy = Occupancy.Single_Female;
		}
		else {
			setMale(person);
			if(female != null) {
				occupancy = Occupancy.Couple;
			}
			else occupancy = Occupancy.Single_Male;
		}
		updateHouseholdWeighting();
		size = size + 1;
		if (household != null) {
			household.updateSize();
		}
	}
	
	public void addResponsibleCouple(Person person, Person partner) {		
		if(!person.getRegion().equals(partner.getRegion())) {
			throw new IllegalArgumentException("Error - couple belong to two different regions!");
		}
		
		if(person.getDgn().equals(Gender.Female)) {
			setFemale(person);
			setMale(partner);
		}
		else {
			setMale(person);
			setFemale(partner);
		}

		occupancy = Occupancy.Couple;
		updateHouseholdWeighting();
		size += 2;

		if (household != null) {
			household.updateSize();
		}

		
		
	}
	
	public boolean addChild(Person person) {
		
		size = size + 1;
		if (household != null) {
			household.updateSize();
		}
		return children.add(person);
		
		//Can no longer do the below check, as we need to include children of teenage parents (i.e. those parents whose age < age to move out of home) in the children set, instead of the otherMembers set.
//		//This check means that you cannot add person to the children set without the household already having at least one of the person's parents as the responsible male or female of the house
//		if( (female != null && person.getMotherId() != null && person.getMotherId().equals(female.getKey().getId())) ||
//			(male != null && person.getFatherId() != null && person.getFatherId().equals(male.getKey().getId())) ) {
//			size = size + 1;
//			return children.add(person);
//		}
//		else {
//			throw new IllegalArgumentException("Error - when adding Child " + person.getKey().getId() 
//					+ " to household " + this.getKey().getId() 
//					+ " neither the child's mother Id " + person.getMotherId() 
//					+ " or father Id " + person.getFatherId()
//					+ " match the id of the responsible mother " + female.getKey().getId() 
//					+ " or father " + male.getKey().getId() + " respectively!"
//					);
//		}
	}
	
	public boolean addOtherMember(Person person) {		//XXX: Is this ever used?  What persons are put in otherMembers during the simulation?  Or is it only ever used with the initial population?  

		if(!person.getRegion().equals(getRegion())) {
			throw new IllegalArgumentException("Error - trying to add person " + person.getKey().getId() + " from region " + person.getRegion() + " to household " + key.getId() + " in region " + getRegion());
		}
		
		//TODO: Assume people in other members cannot have mother or father ids - we need to ensure that when persons 'leaveHome' when they reach a certain age, their mother and father ids are set to null 
		if(person.getId_mother() != null || person.getId_father() != null) {
			throw new IllegalArgumentException("Error - person being added to otherMembers has father or mother id not null.  Consider why the person is being added to a household as 'otherMember' despite having non-null mother or father.  Should the person either have mother or father ids set to null already?");
		}
		size = size + 1;
		if (household != null) {
			household.updateSize();
		}
		return otherMembers.add(person);
		
	}
	
	public void removePerson(Person person) {
		size = size - 1;		
		if(female == person) {
			setFemale(null);
		}
		else if(male == person) {
			setMale(null);
		}
		else if(!children.remove(person) && !otherMembers.remove(person)) {
//			throw new IllegalArgumentException("Error - person " + person.getKey().getId() + " who has householdId " + person.getHouseholdId() + " and lives in household " + person.getHousehold().getKey().getId() + " does not live in BenefitUnit " + this.getKey().getId()  + " so cannot be removed!"
//					+ "\nHousehold maleId " + maleId + " femaleId " + femaleId + " size of children " + children.size() + " size of others " + otherMembers.size()
//					);
		}		
		
		/*
		if(male == null && female == null) {
			if(size == 0) {
				System.out.println("HHID " + getKey().getId() + " has size 0. Will try to remove HH.");
				model.removeHousehold(this);
			}
		}
		*/

		if(male == null && female == null) {
//			if(children.isEmpty() && otherMembers.isEmpty()) {		//Could replace this with a check on size, i.e. if(size == 0), remove the household...
				Set<Person> personsInBU = new LinkedHashSet<>();
				personsInBU.addAll(children);
				personsInBU.addAll(otherMembers);

				for (Person personToRemove : personsInBU) {
					model.removePerson(personToRemove);
				}

//				children = null;
				children.clear();
//				otherMembers = null;
				otherMembers.clear();
				model.removeBenefitUnit(this);				// Remove benefit unit from model as empty
//			}

		}
		else updateHouseholdWeighting();		//Still one responsible household member left, so update HouseholdWeighting to reflect this

		if (household != null) {
			household.updateSize();
		}
	}

	
	// -------------------------------------------------------------------------------------------------------------
	// implements IDoubleSource for use with Regression classes - for use in DonorHousehold, not BenefitUnit objects
	// -------------------------------------------------------------------------------------------------------------
	
	public enum Regressors {

		IncomeSquared,
		HoursMaleSquared,
		HoursFemaleSquared,
		HoursMaleByIncome,
		HoursFemaleByIncome,
		HoursMaleByHoursFemale,
		Income,
		IncomeByAge,
		IncomeByAgeSquared,
		IncomeByNumberChildren,
		HoursMale,
		HoursMaleByAgeMale,
		HoursMaleByAgeMaleSquared,
		HoursMaleByNumberChildren,
		HoursMaleByDelderly,
		HoursMaleByDregion,
		HoursFemale,
		HoursFemaleByAgeFemale,
		HoursFemaleByAgeFemaleSquared,
		HoursFemaleByDchildren2under,
		HoursFemaleByDchildren3_6,
		HoursFemaleByDchildren7_12,
		HoursFemaleByDchildren13_17,
		HoursFemaleByDelderly,
		HoursFemaleByDregion,
		
		FixedCostMaleByNumberChildren,
		FixedCostMaleByDchildren2under,
		
		FixedCostFemaleByNumberChildren,
		FixedCostFemaleByDchildren2under,
		FixedCostByHighEducation,
		
		//New set of regressors for LS models from Zhechun:
		IncomeDiv100, 							//Disposable monthly income from donor household divided by 100
		IncomeSqDiv10000,						//Income squared divided by 10000
		IncomeDiv100_MeanPartnersAgeDiv100,		//Income divided by 100 interacted with mean age of male and female in the household divided by 100
		IncomeDiv100_MeanPartnersAgeSqDiv10000, 	//Income divided by 100 interacted with square of mean age of male and female in the household divided by 100
		IncomeDiv100_NChildren017, 				//Income divided by 100 interacted with the number of children aged 0-17
		IncomeDiv100_DChildren2Under,			//Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
		MaleLeisure,							//24*7 - labour supply weekly for male
		MaleLeisureSq,
		MaleLeisure_IncomeDiv100,
		MaleLeisure_MaleAgeDiv100,				//Male Leisure interacted with age of male
		MaleLeisure_MaleAgeSqDiv10000,
		MaleLeisure_NChildren017,
		MaleLeisure_DChildren2Under,
		MaleLeisure_MaleDeh_c3_Low,
		MaleLeisure_MaleDeh_c3_Medium,
		MaleLeisure_UKC,
		MaleLeisure_UKD,
		MaleLeisure_UKE,
		MaleLeisure_UKF,
		MaleLeisure_UKG,
		MaleLeisure_UKH,
		MaleLeisure_UKJ,
		MaleLeisure_UKK,
		MaleLeisure_UKL,
		MaleLeisure_UKM,
		MaleLeisure_UKN,
		MaleLeisure_MaleAge50Above, 		//Male leisure interacted with dummy for age >= 50
		MaleLeisure_FemaleLeisure,			//Male leisure interacted with female leisure
		FemaleLeisure,							//24*7 - labour supply weekly for Female
		FemaleLeisureSq,
		FemaleLeisure_IncomeDiv100,
		FemaleLeisure_FemaleAgeDiv100,				//Female Leisure interacted with age of Female
		FemaleLeisure_FemaleAgeSqDiv10000,
		FemaleLeisure_NChildren017,
		FemaleLeisure_DChildren2Under,
		FemaleLeisure_FemaleDeh_c3_Low,
		FemaleLeisure_FemaleDeh_c3_Medium,
		FemaleLeisure_UKC,
		FemaleLeisure_UKD,
		FemaleLeisure_UKE,
		FemaleLeisure_UKF,
		FemaleLeisure_UKG,
		FemaleLeisure_UKH,
		FemaleLeisure_UKJ,
		FemaleLeisure_UKK,
		FemaleLeisure_UKL,
		FemaleLeisure_UKM,
		FemaleLeisure_UKN,
		FemaleLeisure_FemaleAge50Above, 		//Female leisure interacted with dummy for age >= 50
		FixedCostMale,
		FixedCostMale_NorthernRegions,
		FixedCostMale_SouthernRegions,
		FixedCostFemale,
		FixedCostFemale_NorthernRegions,
		FixedCostFemale_SouthernRegions,
		FixedCostMale_NChildren017,
		FixedCostMale_DChildren2Under,
		FixedCostFemale_NChildren017,
		FixedCostFemale_DChildren2Under,

		MaleHoursAbove40,
		FemaleHoursAbove40,

		//Additional regressors for single female or single male benefitUnits:
		
		MaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
		MaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
		MaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
		MaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
		FixedCostMale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
		FixedCostMale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student
		
		FemaleLeisure_DChildren1317, //Male leisure interacted with dummy for presence of children aged 13-17
		FemaleLeisure_DChildren712,  //Male leisure interacted with dummy for presence of children aged 7 - 12
		FemaleLeisure_DChildren36,   //Male leisure interacted with dummy for presence of children aged 3 - 6
		FemaleLeisure_DChildren017,  //Male leisure interacted with dummy for presence of children aged 0 - 17
		FixedCostFemale_Dlltsdsp,    //Fixed cost interacted with dummy for partner being long-term sick or disabled
		FixedCostFemale_Lesspc3_Student, //Fixed cost interacted with dummy for partner being a student

		//Other
		Homeownership_D, // Indicator: does the benefit unit own home?
	}

	public double getDoubleValue(Enum<?> variableID) {

		switch ((Regressors) variableID) {
		
		//New set of regressors for LS models from Zhechun: Couples: 
		case IncomeDiv100: 							//Disposable monthly income from donor household divided by 100
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
		case IncomeSqDiv10000:						//Income squared divided by 10000
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-4;
		case IncomeDiv100_MeanPartnersAgeDiv100:		//Income divided by 100 interacted with mean age of male and female in the household divided by 100
			if(female == null) {		//Single so no need for mean age
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * 1.e-4;
			}
			else if(male == null) {
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * 1.e-4;
			}
			else {		//Must be a couple, so use mean age
				double meanAge = (female.getDag() + male.getDag()) * 0.5;
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-4;
			}
		case IncomeDiv100_MeanPartnersAgeSqDiv10000: 	//Income divided by 100 interacted with square of mean age of male and female in the household divided by 10000
			if(female == null) {		//Single so no need for mean age
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * male.getDag() * 1.e-6;
			}
			else if(male == null) {
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * female.getDag() * 1.e-6;
			}
			else {		//Must be a couple, so use mean age
				double meanAge = (female.getDag() + male.getDag()) * 0.5;
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-6;
			}
		case IncomeDiv100_NChildren017: 				//Income divided by 100 interacted with the number of children aged 0-17
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * n_children_017 * 1.e-2;
		case IncomeDiv100_DChildren2Under:			//Income divided by 100 interacted with dummy for presence of children aged 0-2 in the household
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * d_children_2under.ordinal() * 1.e-2;
		case MaleLeisure:							//24*7 - labour supply weekly for male
			return Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours();
		case MaleLeisureSq:
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
		case MaleLeisure_IncomeDiv100:
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
		case MaleLeisure_MaleAgeDiv100:				//Male Leisure interacted with age of male
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * male.getDag() * 1.e-2;
		case MaleLeisure_MaleAgeSqDiv10000:
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * male.getDag() * male.getDag() * 1.e-4;
		case MaleLeisure_NChildren017:
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * n_children_017;
		case MaleLeisure_DChildren2Under:
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * d_children_2under.ordinal();
		case MaleLeisure_MaleDeh_c3_Low:
			if(male.getDeh_c3().equals(Education.Low)) {
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
			} else return 0.;
		case MaleLeisure_MaleDeh_c3_Medium:
			if(male.getDeh_c3().equals(Education.Medium)) {
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
			} else return 0.;
		case MaleLeisure_UKC:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKC)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKD:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKD)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKE:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKE)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKF:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKF)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKG:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKG)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKH:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKH)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKJ:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKJ)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKK:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKK)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKL:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKL)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKM:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKM)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_UKN:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKN)) {
					return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case MaleLeisure_MaleAge50Above:
			if (male.getDag() >= 50) {
				return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours());
			} else return 0.;
		case MaleLeisure_FemaleLeisure:			//Male leisure interacted with female leisure
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
		case FemaleLeisure:							//24*7 - labour supply weekly for Female
			return Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours();
		case FemaleLeisureSq:
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
		case FemaleLeisure_IncomeDiv100:
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-2;
		case FemaleLeisure_FemaleAgeDiv100:				//Female Leisure interacted with age of Female
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * female.getDag() * 1.e-2;
		case FemaleLeisure_FemaleAgeSqDiv10000:
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * female.getDag() * female.getDag() * 1.e-4;
		case FemaleLeisure_NChildren017:
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * n_children_017;
		case FemaleLeisure_DChildren2Under:
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * d_children_2under.ordinal();
		case FemaleLeisure_FemaleDeh_c3_Low:
			if(female.getDeh_c3().equals(Education.Low)) {
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
			} else return 0.;
		case FemaleLeisure_FemaleDeh_c3_Medium:
			if(female.getDeh_c3().equals(Education.Medium)) {
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
			} else return 0.;
		case FemaleLeisure_UKC:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKC)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKD:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKD)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKE:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKE)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKF:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKF)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKG:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKG)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKH:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKH)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKJ:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKJ)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKK:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKK)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKL:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKL)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKM:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKM)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_UKN:
			if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKN)) {
					return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
				} else return 0.;
			} else throw new IllegalArgumentException("Error - the region used in regression doesn't match the country in the simulation!");
		case FemaleLeisure_FemaleAge50Above:
			if (female.getDag() >= 50) {
				return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours());
			} else return 0.;
		//Note: In the previous version of the model, Fixed Cost was returning -1 to match the regression coefficients
		case FixedCostMale:
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				return 1.;
			}
			else return 0.;
		case FixedCostMale_NorthernRegions:
			if(male.getLabourSupplyWeekly().getHours() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
				return 1.;
			}
			else return 0.;
		case FixedCostMale_SouthernRegions:
			if(male.getLabourSupplyWeekly().getHours() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
				return 1.;
			}
			else return 0.;
		case FixedCostFemale:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				return 1.;
			}
			else return 0.;
		case FixedCostFemale_NorthernRegions:
			if(female.getLabourSupplyWeekly().getHours() > 0 && (region.equals(Region.ITC) || region.equals(Region.ITH))) {
				return 1.;
			}
			else return 0.;
		case FixedCostFemale_SouthernRegions:
			if(female.getLabourSupplyWeekly().getHours() > 0 && (region.equals(Region.ITF) || region.equals(Region.ITG))) {
				return 1.;
			}
			else return 0.;
		case FixedCostMale_NChildren017:
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				return n_children_017;
			}
			else return 0.;
		case FixedCostMale_DChildren2Under:
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				return d_children_2under.ordinal();
			}
			else return 0.;
		case FixedCostFemale_NChildren017:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				return n_children_017;
			}
			else return 0.;
		case FixedCostFemale_DChildren2Under:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				return d_children_2under.ordinal();
			}
			else return 0.;
		case MaleHoursAbove40:
			if (male.getLabourSupplyWeekly().getHours() >= 40) {
				return 1.;
			}
			else return 0.;
		case FemaleHoursAbove40:
			if (female.getLabourSupplyWeekly().getHours() >= 40) {
				return 1.;
			}
			else return 0.;
		//Additional regressors for single female or single male benefitUnits:
		//Note: couples in which one person is not at risk of work have utility set according to the process for singles
		case MaleLeisure_DChildren1317: //Male leisure interacted with dummy for presence of children aged 13-17
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * d_children_13_17.ordinal();
		case MaleLeisure_DChildren712:  //Male leisure interacted with dummy for presence of children aged 7 - 12
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * d_children_7_12.ordinal();
		case MaleLeisure_DChildren36:   //Male leisure interacted with dummy for presence of children aged 3 - 6
			return (Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours()) * d_children_3_6.ordinal();
		case MaleLeisure_DChildren017:  //Male leisure interacted with dummy for presence of children aged 0 - 17
			if(n_children_017 > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
				return Parameters.HOURS_IN_WEEK - male.getLabourSupplyWeekly().getHours();
			} else return 0.;
		//The following two regressors refer to a partner in single LS model - this is for those with an inactive partner, but not everyone will have a partner so check for nulls
		case FixedCostMale_Dlltsdsp:    //Fixed cost interacted with dummy for partner being long-term sick or disabled
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				if(female != null) {
				return female.getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
				} else return 0.;
			}
			else return 0.;
		case FixedCostMale_Lesspc3_Student: //Fixed cost interacted with dummy for partner being a student
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				if(female != null && female.getLes_c4().equals(Les_c4.Student)) {
					return 1.; //Partner must be female - if a student, return 1 
				} else return 0.;
			}
			else return 0.;
			
		case FemaleLeisure_DChildren1317: //Male leisure interacted with dummy for presence of children aged 13-17
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * d_children_13_17.ordinal();
		case FemaleLeisure_DChildren712:  //Male leisure interacted with dummy for presence of children aged 7 - 12
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * d_children_7_12.ordinal();
		case FemaleLeisure_DChildren36:   //Male leisure interacted with dummy for presence of children aged 3 - 6
			return (Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours()) * d_children_3_6.ordinal();
		case FemaleLeisure_DChildren017:  //Male leisure interacted with dummy for presence of children aged 0 - 17
			if(n_children_017 > 0) { //Instead of creating a new variable, use number of children aged 0 - 17
				return Parameters.HOURS_IN_WEEK - female.getLabourSupplyWeekly().getHours();
			} else return 0.;
		case FixedCostFemale_Dlltsdsp:    //Fixed cost interacted with dummy for partner being long-term sick or disabled
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				if(male != null) {
				return male.getDlltsd().ordinal(); //==1 if partner is long-term sick or disabled
				} else return 0.;
			}
			else return 0.;
		case FixedCostFemale_Lesspc3_Student:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				if(male != null && male.getLes_c4().equals(Les_c4.Student)) {
					return 1.; //Partner must be male - if a student, return 1 
				} else return 0.;
			}
			else return 0.;
			
			
		
		//Values are divided by powers of 10, as in the tables of Bargain et al. (2014) Working Paper
		case IncomeSquared:		//Income is disposable income, inputed from 'donor' benefitUnits in EUROMOD
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-4;
		case HoursMaleSquared:
			return male.getLabourSupplyWeekly().getHours() * male.getLabourSupplyWeekly().getHours();
		case HoursFemaleSquared:
			return female.getLabourSupplyWeekly().getHours() * female.getLabourSupplyWeekly().getHours();
		case HoursMaleByIncome:
			return male.getLabourSupplyWeekly().getHours() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
		case HoursFemaleByIncome:
			return female.getLabourSupplyWeekly().getHours() * getDisposableIncomeMonthlyUpratedToBasePriceYear() * 1.e-3;
		case HoursMaleByHoursFemale:
			return male.getLabourSupplyWeekly().getHours() * female.getLabourSupplyWeekly().getHours() * 1.e-3;
		case Income:
			return getDisposableIncomeMonthlyUpratedToBasePriceYear();
		case IncomeByAge:		//Use mean age for couples
			if(female == null) {		//Single so no need for mean age
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * 1.e-1;
			}
			else if(male == null) {
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * 1.e-1;
			}
			else {		//Must be a couple, so use mean age
				double meanAge = (female.getDag() + male.getDag()) * 0.5;
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * 1.e-1;
			}
		case IncomeByAgeSquared:		//Use mean age for couples
			if(female == null) {		//Single so no need for mean age
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * male.getDag() * male.getDag() * 1.e-2;
			}
			else if(male == null) {
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * female.getDag() * female.getDag() * 1.e-2;
			}
			else {		//Must be a couple, so use mean age
				double meanAge = (female.getDag() + male.getDag()) * 0.5;
				return getDisposableIncomeMonthlyUpratedToBasePriceYear() * meanAge * meanAge * 1.e-2;
			}
		case IncomeByNumberChildren:
			return getDisposableIncomeMonthlyUpratedToBasePriceYear() * children.size();
		case HoursMale:
			return male.getLabourSupplyWeekly().getHours();
		case HoursMaleByAgeMale:
			return male.getLabourSupplyWeekly().getHours() * male.getDag() * 1.e-1;
		case HoursMaleByAgeMaleSquared:
			return male.getLabourSupplyWeekly().getHours() * male.getDag() * male.getDag() * 1.e-2;
		case HoursMaleByNumberChildren:
			return male.getLabourSupplyWeekly().getHours() * children.size();
		case HoursMaleByDelderly:		//Appears only in Single Males regression, not Couple.
			return 0.;		//Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
		case HoursMaleByDregion:
			if(model.getCountry().equals(Country.IT)) {
				if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {		//For South Italy (Sud) and Islands (Isole)
					return male.getLabourSupplyWeekly().getHours();
				}
				else return 0.;
			}
			else if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKI)) {		//For London
					return male.getLabourSupplyWeekly().getHours();
				}
				else return 0.;
			}
			else throw new IllegalArgumentException("Error - household " + this.getKey().getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");
			
		case HoursFemale:		
			return female.getLabourSupplyWeekly().getHours();
		case HoursFemaleByAgeFemale:
			return female.getLabourSupplyWeekly().getHours() * female.getDag() * 1.e-1;
		case HoursFemaleByAgeFemaleSquared:
			return female.getLabourSupplyWeekly().getHours() * female.getDag() * female.getDag() * 1.e-2;
		case HoursFemaleByDchildren2under:
			return female.getLabourSupplyWeekly().getHours() * d_children_2under.ordinal();
		case HoursFemaleByDchildren3_6:		
			return female.getLabourSupplyWeekly().getHours() * d_children_3_6.ordinal();
		case HoursFemaleByDchildren7_12:	
			return female.getLabourSupplyWeekly().getHours() * d_children_7_12.ordinal();
		case HoursFemaleByDchildren13_17:	
			return female.getLabourSupplyWeekly().getHours() * d_children_13_17.ordinal();
		case HoursFemaleByDelderly:		
			return 0.;		//Our model doesn't take account of elderly (as people move out of parental home when 18 years old, and we do not provide a mechanism for parents to move back in.
		case HoursFemaleByDregion:		//Value of hours are already taken into account by multiplying regression coefficients in Parameters class
			if(model.getCountry().equals(Country.IT)) {
				if(getRegion().equals(Region.ITF) || getRegion().equals(Region.ITG)) {		//For South Italy (Sud) and Islands (Isole)
					return female.getLabourSupplyWeekly().getHours();
				}
				else return 0.;
			}
			else if(model.getCountry().equals(Country.UK)) {
				if(getRegion().equals(Region.UKI)) {		//For London
					return female.getLabourSupplyWeekly().getHours();
				}
				else return 0.;
			}
			else throw new IllegalArgumentException("Error - household " + this.getKey().getId() + " has region " + getRegion() + " which is not yet handled in DonorHousehold.getDoubleValue()!");
			
	//The following regressors for FixedCosts appear as negative in the Utility regression, and so are multiplied by a factor of -1 below. 
		//The following regressors only apply when the male hours worked is greater than 0 

		case FixedCostMaleByNumberChildren:
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				return - children.size();		//Return negative as costs appear negative in utility function equation
			}
			else return 0.;
			
		case FixedCostMaleByDchildren2under:
			if(male.getLabourSupplyWeekly().getHours() > 0) {
				return - d_children_2under.ordinal();		//Return negative as costs appear negative in utility function equation
			}
			else return 0.;
			
			//The following regressors only apply when the female hours worked is greater than 0

			
		case FixedCostFemaleByNumberChildren:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				return - children.size();		//Return negative as costs appear negative in utility function equation
			}
			else return 0.;

		case FixedCostFemaleByDchildren2under:
			if(female.getLabourSupplyWeekly().getHours() > 0) {
				return - d_children_2under.ordinal();		//Return negative as costs appear negative in utility function equation 
			}
			else return 0.;

			//Only appears in regressions for Singles not Couples.  Applies when the single person in the household has hours worked > 0
		case FixedCostByHighEducation:
			if(female == null) {		//For single males
				if(male.getLabourSupplyWeekly().getHours() > 0) {
					return male.getDeh_c3().equals(Education.High) ? -1. : 0.;
				}
				else return 0.;
			}
			else if (male == null) {	//For single females
				if(female.getLabourSupplyWeekly().getHours() > 0) {
					return female.getDeh_c3().equals(Education.High) ? -1. : 0.;
				}
				else return 0.;
			}
			else throw new IllegalArgumentException("Error - FixedCostByHighEducation regressor should only be called for Households containing single people (with or without children), however household " + key.getId() + " has a couple, with male " + male.getKey().getId() + " and female " + female.getKey().getId());
		case Homeownership_D:
			return isDhh_owned()? 1. : 0.;
		default:
			throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in DonorHousehold.getDoubleValue");

		}
	}

		
	
	
	////////////////////////////////////////////////////////////////////////////////
	//
	//	Override equals and hashCode to make unique BenefitUnit determined by Key.getId()
	//
	////////////////////////////////////////////////////////////////////////////////

	 @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof BenefitUnit)) {
            return false;
        }

        BenefitUnit h = (BenefitUnit) o;

        boolean idIsEqual = new EqualsBuilder()
                .append(key.getId(), h.key.getId())		//Add more fields to compare to check for equality if desired
                .isEquals();
        
//	        if(idIsEqual) {
//		        //Throw an exception if there are household objects with the same id but different other fields as this should not
//		        //be possible and suggests a problem with the input data, i.e. the input database with household information would
//		        //have people in the same household, but the attributes (fields) of the household objects are different, despite 
//		        //supposedly being the same household
//		        //TODO: When finished designing household, ensure all fields are included below.
//		        boolean allFieldsAreEqual = new EqualsBuilder()
//		        		.append(key.getId(), h.key.getId())
//		        		.append(size, h.size)
//		        		.append(femaleId, h.femaleId)
//		        		.append(female, h.female)
//		        		.append(maleId, h.maleId)
//		        		.append(male, h.male)
//		        		.append(weight, h.household_weight)
//		        		.append(children, h.children)
//		        		.append(otherMembers, h.otherMembers)
//	//	        		.append(householdLabourSupply, h.householdLabourSupply)
//	//	        		.append(grossIncome, h.grossIncome)
//		        		.append(disposableIncomeMonthly, h.disposableIncomeMonthly)
//		        		.isEquals();
//		        if(!allFieldsAreEqual) {
//		        	throw new IllegalArgumentException("Error - there are multiple household objects with the same id " + key.getId() + " but different fields!");
//		        }
//	        }	   
        
        return idIsEqual;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(key.getId())
//	                .append(size)
//	        		.append(femaleId)
//	        		.append(female)
//	        		.append(maleId)
//	        		.append(male)
//	        		.append(weight)
//	        		.append(children)
//	        		.append(otherMembers)
//	        		.append(householdLabourSupply)
//	        		.append(grossIncome)
//	        		.append(disposableIncome)
                .toHashCode();
    }
	
    @Override
    public int compareTo(BenefitUnit o) {
    	BenefitUnit h = (BenefitUnit) o;
    	return (int) (this.key.getId() - h.key.getId());
    }
	
	
	
	////////////////////////////////////////////////////////////////////////////////
	//
	//	Other methods
	//
	////////////////////////////////////////////////////////////////////////////////

	
	public int calculateSize() { 
		size = 0;
		if(male != null) {
			size = size + 1;
		}
		if(female != null) {
			size = size + 1;
		}
		size += children.size();
		size += otherMembers.size();
		return size;
	}

	public void newBornUpdate() {		//For use in birth process
		n_children_0++;
		d_children_2under = Indicator.True;
		d_children_3under = Indicator.True;
	}

	public void updateOccupancy() {
		if(female == null) {
			if (male == null) {
				//TODO: BenefitUnit without responsible adults - how to handle?
//				System.out.println("Empty hh!");
			} else if (male.getPartner() == null) {
				occupancy = Occupancy.Single_Male;
			} else if (male.getBenefitUnit().getKey().getId() == male.getPartner().getBenefitUnit().getKey().getId()) { //TODO: If female is null but male in the household has a partner, and they have the same household id, the female should be set to that partner? This can be updated here?
				setFemale(male.getPartner());
				occupancy = Occupancy.Couple;
				size = calculateSize(); //Update household size if responsible male / female were updated
			} else { //If male has a partner but with different household id, remove partner
				occupancy = Occupancy.Single_Male;
				male.setPartner(null);
			}
		}

		else if(male == null) {
			if (female == null) {
//				System.out.println("Empty hh!");
			} else if (female.getPartner() == null) {
				occupancy = Occupancy.Single_Female;
			} else if (female.getBenefitUnit().getKey().getId() == female.getPartner().getBenefitUnit().getKey().getId()) {
				setMale(female.getPartner());
				occupancy = Occupancy.Couple;
				size = calculateSize();
			} else {
				occupancy = Occupancy.Single_Female;
				female.setPartner(null);
			}
		}

		else if (female != null && male != null){
			occupancy = Occupancy.Couple;
		}
	}

	/*
	 * If any single member of a household is at risk of work, the household is at risk of work
	 */
	public boolean getAtRiskOfWork() {
		boolean atRiskOfWork = false;
		
		if(female != null) {
			atRiskOfWork = female.atRiskOfWork();
		}
		
		if(atRiskOfWork == false && male != null) {		//Can skip checking if atRiskOfWork is true already 
			atRiskOfWork = male.atRiskOfWork();
		}
		
		return atRiskOfWork;
		
	}

	protected void homeownership() {

		ValidHomeownersCSfilter filter = new ValidHomeownersCSfilter();
		if(filter.isFiltered(this)) {

			if (occupancy.equals(Occupancy.Couple)) {
				boolean male_homeowner = Parameters.getRegHomeownershipHO1a().event(male, Person.DoublesVariables.class);
				boolean female_homeowner = Parameters.getRegHomeownershipHO1a().event(female, Person.DoublesVariables.class);

				male.setDhh_owned(male_homeowner);
				female.setDhh_owned(female_homeowner);

				if (!male_homeowner && !female_homeowner) { //If neither person in the BU is a homeowner, BU not classified as owning home
					setDhh_owned(false);
				} else {
					setDhh_owned(true);
				}

			} else if (occupancy.equals(Occupancy.Single_Female)) {
				boolean female_homeowner = Parameters.getRegHomeownershipHO1a().event(female, Person.DoublesVariables.class);

				female.setDhh_owned(female_homeowner);
				setDhh_owned(female_homeowner);

			} else if (occupancy.equals(Occupancy.Single_Male)) {
				boolean male_homeowner = Parameters.getRegHomeownershipHO1a().event(male, Person.DoublesVariables.class);

				male.setDhh_owned(male_homeowner);
				setDhh_owned(male_homeowner);

			} else {
				throw new IllegalArgumentException("Benefit unit " + getKey().getId() + " has incorrect occupancy.");
			}
		}
	}

	/*
	 * For variables that are equivalised, this method calculates the household's equivalised weight to use with them
	 *
	 */
	public void calculateEquivalisedWeight() {
		//Equivalence scale gives a weight of 1.0 to the first adult; 
		//0.5 to the second and each subsequent person aged 14 and over; 
		//0.3 to each child aged under 14.

		if(occupancy.equals(Occupancy.Couple)) {
			equivalisedWeight = 1.5;		//1 for the first person, 0.5 for the second of the couple.
		}
		else equivalisedWeight = 1.;		//Must be a single responsible adult
		
		for(Person child : children) {
			if(child.getDag() < 14) {
				equivalisedWeight += 0.3;
			}
			else {
				equivalisedWeight += 0.5;
			}
		}
		
		for(Person other : otherMembers) {
			if(other.getDag() < 14) {
				equivalisedWeight += 0.3;
			}
			else {
				equivalisedWeight += 0.5;
			}
		}
	}

	public double calculateEquivalisedDisposableIncomeYearly() {

		calculateEquivalisedWeight();

		if(disposableIncomeMonthly != null && Double.isFinite(disposableIncomeMonthly)) {
			equivalisedDisposableIncomeYearly = (disposableIncomeMonthly / equivalisedWeight) * 12;
		}
		else equivalisedDisposableIncomeYearly = 0.;
		return equivalisedDisposableIncomeYearly;
	}


	/*
    	This method calculates the change in benefit unit's equivalised disposable income for use with Step 2 of mental health determination
    */
	private double calculateYearlyChangeInLogEquivalisedDisposableIncome() {
		double yearlyChangeInLogEquivalisedDisposableIncome = 0.;
		if (equivalisedDisposableIncomeYearly != null && equivalisedDisposableIncomeYearly_lag1 != null && equivalisedDisposableIncomeYearly >= 0. && equivalisedDisposableIncomeYearly_lag1 >= 0.) {
			// Note that income is uprated to the base price year, as specified in parameters class, as the estimated change uses real income change
			// +1 added as log(0) is not defined
			yearlyChangeInLogEquivalisedDisposableIncome = Math.log(equivalisedDisposableIncomeYearly*getUpratingFactorToBasePriceYear()+1) - Math.log(equivalisedDisposableIncomeYearly_lag1*getUpratingFactorToBasePriceYear()+1);
		}
		yearlyChangeInLogEDI = yearlyChangeInLogEquivalisedDisposableIncome;
		return yearlyChangeInLogEquivalisedDisposableIncome;
	}

	//No need to simulate responsible adults if responsible adults have retired (what happens to their children?  Cannot remove household because it contains children)
	public void removeResponsibleAdultsFromSimulationIfAllRetired() {

		//Remove household from simulation if all responsible adults are retired
		if(occupancy.equals(Occupancy.Couple)) {
			if(male.getLes_c4().equals(Les_c4.Retired) &&
					female.getLes_c4().equals(Les_c4.Retired)) {
				model.getPersons().remove(male);
				model.getPersons().remove(female);
				removePerson(male);
				removePerson(female);
			}
		}
		else {
			Person single;
			if(male != null) {
				single = male;
			}
			else {
				single = female;
			}
			if(single.getLes_c4().equals(Les_c4.Retired)) {
				model.getPersons().remove(single);
				removePerson(single);
			}
		}
	}
	
	
	////////////////////////////////////////////////////////////////////////////////
	//
	//	Access Methods
	//
	////////////////////////////////////////////////////////////////////////////////


    /**
	 * 
	 * Returns a defensive copy of the field.
	 * The caller of this method can do anything they want with the
	 * returned Key object, without affecting the internals of this
	 * class in any way.
	 * 
	 */
	public PanelEntityKey getKey() {
		return new PanelEntityKey(key.getId());
	}

	public void setFemale(Person female) {
		if(female != null) {
			if(this.female != null) {
				throw new IllegalArgumentException("BenefitUnit " + this.getKey().getId() + " already has a female.  Remove existing female before adding another one!");
			}
			this.id_female = female.getKey().getId();
			if(!female.getDgn().equals(Gender.Female)) {
				throw new IllegalArgumentException("Person " + female.getKey().getId() + " does not have gender = Female, so cannot be the responsible female of the househoold!");
			}
		}
		else {		//female must be null then
			id_female = null;
		}
		this.female = female;
	}

	public void setMale(Person male) {
		if(male != null) {
			if(this.male != null) {
				throw new IllegalArgumentException("BenefitUnit " + this.getKey().getId() + " already has a male.  Remove existing male before adding another one!");
			}
			this.id_male = male.getKey().getId();
			if(!male.getDgn().equals(Gender.Male)) {
				throw new IllegalArgumentException("Person " + male.getKey().getId() + " does not have gender = Male, so cannot be the responsible male of the household!");
			}
		}
		else {		//male must be null then
			id_male = null;
		}
		this.male = male;
	}

	public Household getHousehold() {
		return household;
	}

	public void setHousehold(Household household) {
		this.household = household;
		if (household == null) {
			this.id_household = null;
		} else {
			this.id_household = household.getId();
		}

	}

	public Person getFemale() {
		return female;
	}

	public Person getMale() {
		return male;
	}

	public Region getRegion() {		
		return region;
	}
	
	public void setRegion(Region region) {
		this.region = region;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}
	
	public Set<Person> getChildren() {
		return children;
	}

	public Set<Person> getOtherMembers() {
		return otherMembers;
	}

	public Long getId_female() {
		return id_female;
	}

	public Long getId_male() {
		return id_male;
	}

	public Indicator getD_children_3under() {
		return d_children_3under;
	}

	public Indicator getD_children_4_12() {
		return d_children_4_12;
	}

	public Indicator getD_children_2under() {
		return d_children_2under;
	}

	public Indicator getD_children_3_6() {
		return d_children_3_6;
	}

	public Indicator getD_children_7_12() {
		return d_children_7_12;
	}

	public Indicator getD_children_13_17() {
		return d_children_13_17;
	}
	
	public Indicator getD_children_18over() {
		return d_children_18over;
	}

	public Indicator getD_children_3under_lag() {
		return d_children_3under_lag;
	}

	public Indicator getD_children_4_12_lag() {
		return d_children_4_12_lag;
	}

	public Integer getN_children_0() {
		return n_children_0;
	}

	public Integer getN_children_1() {
		return n_children_1;
	}

	public Integer getN_children_2() {
		return n_children_2;
	}

	public Integer getN_children_3() {
		return n_children_3;
	}

	public Integer getN_children_4() {
		return n_children_4;
	}

	public Integer getN_children_5() {
		return n_children_5;
	}

	public Integer getN_children_6() {
		return n_children_6;
	}

	public Integer getN_children_7() {
		return n_children_7;
	}

	public Integer getN_children_8() {
		return n_children_8;
	}

	public Integer getN_children_9() {
		return n_children_9;
	}

	public Integer getN_children_10() {
		return n_children_10;
	}

	public Integer getN_children_11() {
		return n_children_11;
	}

	public Integer getN_children_12() {
		return n_children_12;
	}

	public Integer getN_children_13() {
		return n_children_13;
	}

	public Integer getN_children_14() {
		return n_children_14;
	}

	public Integer getN_children_15() {
		return n_children_15;
	}

	public Integer getN_children_16() {
		return n_children_16;
	}

	public Integer getN_children_17() {
		return n_children_17;
	}

	public Integer getN_children_allAges() {
		return n_children_allAges;
	}


	public void setN_children_allAges(Integer n_children_allAges) {
		this.n_children_allAges = n_children_allAges;
	}

	public Integer getN_children_allAges_lag1() {
		return n_children_allAges_lag1;
	}


	public Integer getN_children_02() {
		return n_children_02;
	}


	public Integer getN_children_02_lag1() {
		return n_children_02_lag1;
	}

    public Integer getN_children_017() {
        return n_children_017;
    }

	public Double getEquivalisedDisposableIncomeYearly() {
		return equivalisedDisposableIncomeYearly;
	}

	public Integer getAtRiskOfPoverty() {
		return atRiskOfPoverty;
	}

	public Integer getAtRiskOfPoverty_lag1() {
		return atRiskOfPoverty_lag1;
	}


	public void setAtRiskOfPoverty(Integer atRiskOfPoverty) {
		this.atRiskOfPoverty = atRiskOfPoverty;
	}

	public Double getDisposableIncomeMonthly() {
		return disposableIncomeMonthly;
	}

//	public Map<Gender, Person> getResponsiblePersons() {
//		return responsiblePersons;
//	}

	public Occupancy getOccupancy() {
		return occupancy;
	}
	
	public int getCoupleOccupancy() {

		if(occupancy.equals(Occupancy.Couple)) {
			return 1;
		}
		else return 0;

	}
	
	public long getId() {
		return key.getId();
	}

	public int getSize() {
		return size;
	}


	public Ydses_c5 getYdses_c5() {
		return ydses_c5;
	}


	public Ydses_c5 getYdses_c5_lag1() {
		return ydses_c5_lag1;
	}

	public double getTmpHHYpnbihs_dv_asinh() {
		return tmpHHYpnbihs_dv_asinh;
	}

	public void setTmpHHYpnbihs_dv_asinh(double tmpHHYpnbihs_dv_asinh) {
		this.tmpHHYpnbihs_dv_asinh = tmpHHYpnbihs_dv_asinh;
	}

	public Dhhtp_c4 getDhhtp_c4() {
		return dhhtp_c4;
	}


	public Dhhtp_c4 getDhhtp_c4_lag1() {
		return dhhtp_c4_lag1;
	}

	public void setN_children_allAges_lag1(int n_children_allAges_lag1) { this.n_children_allAges_lag1 =  n_children_allAges_lag1; }

	public void setN_children_02_lag1(int n_children_02_lag1) { this.n_children_02_lag1 =  n_children_02_lag1; }

	public void setOccupancy(Occupancy occupancy) { this.occupancy =  occupancy; }

	public void setN_children_017(Integer n_children_017) { this.n_children_017 = n_children_017; }

	public void setD_children_2under(Indicator d_children_2under) { this.d_children_2under = d_children_2under; }

	public long getId_household() {
		return id_household;
	}

	public void setDhhtp_c4_lag1(Dhhtp_c4 dhhtp_c4_lag1) {
		this.dhhtp_c4_lag1 = dhhtp_c4_lag1;
	}
	public void setYdses_c5_lag1(Ydses_c5 ydses_c5_lag1) {
		this.ydses_c5_lag1 = ydses_c5_lag1;
	}

	public void setId_household(long id_household) {
		this.id_household = id_household;
	}

	public Double getYearlyChangeInLogEDI() {
		return yearlyChangeInLogEDI;
	}

	public boolean isDhh_owned() {
		return dhh_owned;
	}

	public void setDhh_owned(boolean dhh_owned) {
		this.dhh_owned = dhh_owned;
	}

	public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale() {
		return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleFemale;
	}

	public ArrayList<Triple<Les_c7_covid, Double, Integer>> getCovid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale() {
		return covid19MonthlyStateAndGrossIncomeAndWorkHoursTripleMale;
	}

	public double isDisposableIncomeMonthlyImputedFlag() {
		return disposableIncomeMonthlyImputedFlag? 1. : 0.;
	}

	public void setDisposableIncomeMonthlyImputedFlag(boolean disposableIncomeMonthlyImputedFlag) {
		this.disposableIncomeMonthlyImputedFlag = disposableIncomeMonthlyImputedFlag;
	}

	public double getUpratingFactorToBasePriceYear() {
		// 1 / (uprating_factor_from_2017) produces uprating factor from any given year to 2017
		return (1 / Parameters.upratingFactorForMonetaryValuesMap.get(model.getYear())); //Get uprating factor for a given simulated year (given the year for which wage equation was estimated as specified in Parameters.BASE_PRICE_YEAR
	}

	// Uprate disposable income from level of prices in any given year to 2017, as utility function was estimated on 2017 data
	public double getDisposableIncomeMonthlyUpratedToBasePriceYear() {
		return disposableIncomeMonthly* getUpratingFactorToBasePriceYear();
	}

	public boolean isDecreaseInYearlyEquivalisedDisposableIncome() {
		return (equivalisedDisposableIncomeYearly != null && equivalisedDisposableIncomeYearly_lag1 != null && equivalisedDisposableIncomeYearly < equivalisedDisposableIncomeYearly_lag1);
	}

}

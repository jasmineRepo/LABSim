package labsim.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;

import labsim.model.enums.*;
import microsim.statistics.Series;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.log4j.Logger;

import labsim.data.Parameters;
import microsim.agent.Weight;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import microsim.statistics.IIntSource;
import microsim.statistics.regression.LinearRegression;
import microsim.statistics.regression.RegressionUtils;

@Entity
public class Person implements EventListener, IDoubleSource, IIntSource, Weight, Comparable<Person>
{
//	@Column(name="idperson")
//	public long idPerson;
	
	@Transient
	private static Logger log = Logger.getLogger(Person.class);
	
	//Italy and UK EU-SILC data has max id number of 270150003
	//EUROMOD data has max id around 2,700,000 for Italy and just over 2 million for UK
	@Transient
	public static long personIdCounter = 1;			//Could perhaps initialise this to one above the max key number in initial population, in the same way that we pull the max Age information from the input files.
	
	@Transient
	private final LABSimModel model;
	
	@Id
	private PanelEntityKey key;
	
	private int dag; //Age

	@Transient
	private boolean ioFlag = false; // true if a dummy person instantiated for IO decision solution
	
	@Transient
	private int dag_sq = dag*dag; //Age squared
	
	@Enumerated(EnumType.STRING)
	private Gender dgn; 
	
	@Enumerated(EnumType.STRING)
	private Education deh_c3; //Education level
	
	@Enumerated(EnumType.STRING)
	@Transient
	private Education deh_c3_lag1 = null; //Lag(1) of education level
	
	@Enumerated(EnumType.STRING)
	private Education dehm_c3; //Mother's education level
	
	@Enumerated(EnumType.STRING)
	private Education dehf_c3; //Father's education level 
	
	@Enumerated(EnumType.STRING)
	private Education dehsp_c3; //Partner's education
	
	@Enumerated(EnumType.STRING)
	@Transient
	private Education dehsp_c3_lag1 = null; //Lag(1) of partner's education
	
	@Enumerated(EnumType.STRING)
	private Indicator ded; 
	
	@Enumerated(EnumType.STRING)
	private Indicator der; 
	
	@Enumerated(EnumType.STRING)
	private Household_status household_status;
	
	@Transient
	private Household_status household_status_lag = null;		//Lag(1) of household_status

	@Enumerated(EnumType.STRING)
	private Les_c4 les_c4; //Activity (employment) status

	@Enumerated(EnumType.STRING)
	private Les_c7_covid les_c7_covid; //Activity (employment) status used in the Covid-19 models

	@Transient
	private Les_c4 les_c4_lag1 = null;		//Lag(1) of activity_status

	@Transient
	private Les_c7_covid les_c7_covid_lag1 = null; //Lag(1) of 7-category activity status

	@Enumerated(EnumType.STRING)
	private Les_c4 lessp_c4;
	
	@Transient
	private Les_c4 activity_status_partner_lag = null; //Lag(1) of partner activity status
	
	@Enumerated(EnumType.STRING)
	private Lesdf_c4 lesdf_c4; //Own and partner's activity status
	
	@Transient
	@Enumerated(EnumType.STRING)
	private Lesdf_c4 lesdf_c4_lag1; //Lag(1) of own and partner's activity status

	@Transient
	private Integer liwwh = 0; //Work history in months (number of months in employment) (Note: this is monthly in EM, but simulation updates annually so increment by 12 months). 
	
	@Enumerated(EnumType.STRING)
	private Dcpst dcpst;
	
	@Transient
	@Enumerated(EnumType.STRING)
	private Dcpst dcpst_lag1;
	
	@Enumerated(EnumType.STRING)
	private Indicator dcpen;
	
	@Enumerated(EnumType.STRING)
	private Indicator dcpex;
	
	@Enumerated(EnumType.STRING)
	private Indicator dlltsd;	//Long-term sick or disabled if = 1
	
	@Enumerated(EnumType.STRING)
	@Transient
	private Indicator dlltsd_lag1 = null; //Lag(1) of long-term sick or disabled 
	
	//Sedex is an indicator for leaving education in that year
	@Enumerated(EnumType.STRING)
	private Indicator sedex; 
	
	@Enumerated(EnumType.STRING)
	private Indicator partnership_samesex;
	
	@Enumerated(EnumType.STRING)
	private Indicator women_fertility;
	
	@Enumerated(EnumType.STRING)
	private Indicator education_inrange;
	
	
//	@Transient
//	private double deviationFromMeanRetirementAge;				//Set on initialisation?

	@Enumerated(EnumType.STRING)
	private Indicator adultchildflag;

	@Transient
	private boolean toGiveBirth = false;
	
	@Transient
	private boolean toLeaveSchool = false;


	@Transient
	private boolean toBePartnered = false;

	@Transient
	private Person partner;
	
	@Column(name="idpartner")
	private Long id_partner;		//Note, must not use primitive long, as long cannot hold 'null' value, i.e. if the person has no partner
	
	@Transient
	private Long id_partner_lag1 = null;
	
	//EUROMOD's DWT variable - demographic weight applies to the benefitUnit, rather than the individual.
	//So initialize the person weights with the benefitUnit weights and let evolve seperately from
	//the benefitUnit weights (conditional on the update rules applied to benefitUnit weights, which
	//may be derived from personal weights, e.g. taking the average of the weights of the 
	//responsible male and female of the benefitUnit).
	//Note that personal weight of new born child will equal that of the mother.
	@Column(name="person_weight")
	private double weight;			// This value is to create (re)weights for two-person benefitUnits

	@Column(name="idmother")
	private Long id_mother;
	
	@Column(name="idfather")
	private Long id_father;
	
	@Transient
	private BenefitUnit benefitUnit;
	
	@Column(name=Parameters.BENEFIT_UNIT_VARIABLE_NAME)
	private long id_benefitUnit;

	@Column(name="idhh")
	@Transient
	private long id_household;

	@Column(name="dhe")
	private Double dhe = 0.; //Continuous variable for health

	@Column(name="dhm")
	private Double dhm; //Mental health GHQ-12 Likert scale

	@Transient
	private Double dhm_lag1 = 0.; //Lag(1) of dhm

	@Transient
	private Double dhe_lag1 = 0.; //Lag(1) of dhe
	
	@Column(name="dhesp")
	private Double dhesp; 
	
	@Transient
	private Double dhesp_lag1; //Lag(1) of partner's health status

	@Column(name="dhh_owned")
	private boolean dhh_owned; // Person is a homeowner, true / false

	@Transient
	private boolean receivesBenefitsFlag; // Does person receive benefits

	@Transient
	private boolean receivesBenefitsFlag_L1; // Lag(1) of whether person receives benefits

//	@Column(name="unit_labour_cost")	// Initialised with value: (ils_earns + ils_sicer) / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data, and ils_sicer is the monthly employer social insurance contributions
//	private double unitLabourCost;		//Hourly labour cost.  Updated as potentialEarnings + donorHouse.ils
	
	@Column(name="labour_supply_weekly")
	private Labour labourSupplyWeekly;			//Number of hours of labour supplied each week

	@Transient
	private Labour labourSupplyWeekly_L1; // Lag(1) (previous year's value) of weekly labour supply
	
	@Column(name="hours_worked_weekly")
	private Integer hoursWorkedWeekly;			//Only for initialisation of labourSupplyWeekly (for aggregate supply / demand / total cost statistics at the start of the simulation). This is set to null after initialization.
	
//	Potential earnings is the gross hourly wage an individual can earn while working
//	and is estimated, for each individual, on the basis of observable characteristics as
//	age, education, civil status, number of children, etc. Hence, potential earnings
//	is a separate process in the simulation, and it is computed for every adult
//	individual in the simulated population, in each simulated period.
	@Column(name="potential_earnings_hourly")
	private double potentialEarnings;		//Is hourly rate.  Initialised with value: ils_earns / (4.34 * lhw), where lhw is the weekly hours a person worked in EUROMOD input data

	@Transient
	private Series.Double yearlyEquivalisedDisposableIncomeSeries;

	@Column(name="equivalised_consumption_yearly")
	private Double yearlyEquivalisedConsumption;

	@Transient
	private Series.Double yearlyEquivalisedConsumptionSeries;

	@Column(name="s_index") //Alternatively could be kept in a Series, which would allow access to previous values. But more efficient to persist a single value to CSV / database
	private Double sIndex = Double.NaN;

	@Column(name="s_index_normalised")
	private Double sIndexNormalised = Double.NaN;

	@Transient
	private LinkedHashMap<Integer, Double> sIndexYearMap;

	@Column(name="dcpyy")
	private Integer dcpyy; //Number of years in partnership
	
	@Transient
	private Integer dcpyy_lag1; //Lag(1) of number of years in partnership
	
	@Column(name="dcpagdf")
	private Integer dcpagdf; //Difference between ages of partners in union (note: this allows negative values and is the difference between own age and partner's age)
	
	@Transient
	private Integer dcpagdf_lag1 = null; //Lag(1) of difference between ages of partners in union
	
	@Column(name="ypnbihs_dv")
	private Double ypnbihs_dv; //Gross personal non-benefit income
	
	@Transient
	private Double ypnbihs_dv_lag1 = 0.; //Lag(1) of gross personal non-benefit income
	
	@Column(name="yptciihs_dv")
	private double yptciihs_dv;

	@Column(name="ypncp")
	private double ypncp; //Capital income

	@Column(name="ypnoab")
	private double ypnoab; //Pension income

	@Transient
	private double ypncp_lag1 = 0.; //Lag(1) of capital income

	@Transient
	private double ypncp_lag2 = 0.; //Lag(2) of capital income

	@Transient
	private double ypnoab_lag1 = 0.; //Lag(1) of pension income

	@Transient
	private double ypnoab_lag2 = 0.; //Lag(2) of pension income

	@Transient
	private double yptciihs_dv_lag1 = 0.; //Lag(1) of gross personal non-benefit non-employment income
	
	@Transient
	private double yptciihs_dv_lag2 = 0.; //Lag(2) of gross personal non-benefit non-employment income
	
	@Transient
	private double yptciihs_dv_lag3 = 0.; //Lag(3) of gross personal non-benefit non-employment income
	
	@Column(name="yplgrs_dv")
	private double yplgrs_dv; //Gross personal employment income
	
	@Transient
	private double yplgrs_dv_lag1 = 0.; //Lag(1) of gross personal employment income

	@Transient
	private double yplgrs_dv_lag2 = 0.; //Lag(2) of gross personal employment income
	
	@Transient
	private double yplgrs_dv_lag3 = 0.; //Lag(3) of gross personal employment income
	
	@Column(name="ynbcpdf_dv")
	private Double ynbcpdf_dv; //Difference between own and partner's gross personal non-benefit income 
	
	@Transient
	private Double ynbcpdf_dv_lag1 = null; //Lag(1) of difference between own and partner's gross personal non-benefit income
	
	//For matching process
	@Transient
	private double desiredAgeDiff;
	
	@Transient
	private double desiredEarningsPotentialDiff;

	@Transient
	private Long id_original;

	@Transient
	private Long id_bu_original;

	@Transient
	private Long id_hh_original;

	@Transient
	private Person originalPartner;
	
	@Transient
	private int ageGroup;

//	private int personType;
	
	@Transient
	private boolean clonedFlag;

	public boolean isBornInSimulation() {
		return bornInSimulation;
	}

	public void setBornInSimulation(boolean bornInSimulation) {
		this.bornInSimulation = bornInSimulation;
	}

	@Transient
	private boolean bornInSimulation; //Flag to keep track of newborns
	
	//This is set to true at the point when individual leaves education and never reset. So if true, individual has not always been in continuous education.
	@Transient
	private boolean leftEducation = false; 
	
	//This is set to true at the point when individual leaves partnership and never reset. So if true, individual has been / is in a partnership
	@Transient
	private boolean leftPartnership = false; 
	
	@Transient
	private int originalNumberChildren;
	
	@Transient
	private Household_status originalHHStatus;

	@Transient
	private Integer newWorkHours_lag1 = null; // Define a variable to keep previous month's value of work hours to be used in the Covid-19 module

	@Transient
	private double covidModuleGrossLabourIncome_lag1 = 0;

	@Transient
	private Indicator covidModuleReceivesSEISS = Indicator.False;

	@Transient
	private double covidModuleGrossLabourIncome_Baseline = 0;

	@Column(name = "covidModuleBaselinePayXt5")
	private Quintiles covidModuleGrossLabourIncomeBaseline_Xt5;

  @Transient
	private Double wageRegressionRandomComponent = null;
	
	//TODO: Remove when no longer needed.  Used to calculate mean score of employment selection regression.
	public static double scoreMale = 0.;
	public static double scoreFemale = 0.;
	public static double countMale = 0.;
	public static double countFemale = 0.;
	public static double inverseMillsRatioMaxMale = Double.MIN_VALUE;
	public static double inverseMillsRatioMinMale = Double.MAX_VALUE;
	public static double inverseMillsRatioMaxFemale = Double.MIN_VALUE;
	public static double inverseMillsRatioMinFemale = Double.MAX_VALUE;
	public static int countOK = 0;
	public static int countNaN = 0;
	public static int countOKover32;


	public Person(LABSimModel model, BenefitUnit benefitUnit) {
		this.model = model;
		this.benefitUnit = benefitUnit;
	}

	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------

	//Used when loading the initial population from the input database
	public Person() {
		super();
//		key = new PanelEntityKey(idPerson);
		key = new PanelEntityKey();
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
	}
	
	public Person(Long id) {
		super();
		key = new PanelEntityKey(id);
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
		clonedFlag = false;
	}
	
	//For use with creating new people at the minimum Age who enter the simulation during UpdateMaternityStatus after fertility has been aligned 
	public Person(Gender gender, Person mother) {
		this();	
		key = new PanelEntityKey(personIdCounter++);

		this.dgn = gender;
		this.id_mother = mother.getKey().getId();
		this.id_father = mother.getPartner().getKey().getId();
		this.dehm_c3 = mother.getDeh_c3();
		this.dehf_c3 = mother.getPartner().getDeh_c3();
		this.dcpen = Indicator.False;
		this.dcpex = Indicator.False;
		this.dlltsd = Indicator.False;
		this.dlltsd_lag1 = Indicator.False;
		this.women_fertility = Indicator.False;
		this.id_benefitUnit = mother.getId_benefitUnit();
		this.benefitUnit = mother.benefitUnit;
		this.benefitUnit.setHousehold(mother.getBenefitUnit().getHousehold());
		this.dag = 0;
		this.weight = mother.getWeight();			//Newborn has same weight as mother (the number of newborns will then be aligned in fertility alignment)
		this.dhe = 4.;			//TODO: Default is to be born with very good health - should this be modelled in a different way? (4 is the median health for < 18 persons in Cara's data)
		this.dhm = 9.;			//Set to median for under 18's as a placeholder
		this.dhe_lag1 = dhe;
		this.dhm_lag1 = dhm;
		this.deh_c3 = Education.Low;
		this.les_c4 = Les_c4.Student;				//Set lag activity status as Student, i.e. in education from birth
		this.leftEducation = false;
		this.les_c4_lag1 = les_c4;
		this.les_c7_covid = Les_c7_covid.Student;
		this.les_c7_covid_lag1 = les_c7_covid;
		this.household_status = Household_status.Parents;
		this.labourSupplyWeekly = Labour.ZERO;			//Will be updated in Labour Market Module when the person stops being a student
		this.labourSupplyWeekly_L1 = Labour.ZERO;
		this.hoursWorkedWeekly = labourSupplyWeekly.getHours();
		this.id_household = mother.getBenefitUnit().getId_household();
//		setDeviationFromMeanRetirementAge();			//This would normally be done within initialisation, but the line above has been commented out for reasons given...
		yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
		yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
		yearlyEquivalisedConsumption = 0.;
		sIndexYearMap = new LinkedHashMap<Integer, Double>();
		this.bornInSimulation = true;
		this.dhh_owned = false;
		this.receivesBenefitsFlag = false;
		this.receivesBenefitsFlag_L1 = receivesBenefitsFlag;
		updateVariables();

	}
	
	//Below is a "copy constructor" for persons: it takes an original person as input, changes the ID, copies the rest of the person's properties, and creates a new person.  
	public Person (Person originalPerson) {
		this(personIdCounter++);

		this.id_hh_original = originalPerson.id_household;
		this.id_bu_original = originalPerson.id_benefitUnit;
		this.id_original = originalPerson.key.getId();
		
		this.dag = originalPerson.dag;
		this.ageGroup = originalPerson.ageGroup;
		this.dgn = originalPerson.dgn;
		this.deh_c3 = originalPerson.deh_c3;

		if (originalPerson.deh_c3_lag1 != null) { //If original person misses lagged level of education, assign current level of education
			this.deh_c3_lag1 = originalPerson.deh_c3_lag1;
		} else {
			this.deh_c3_lag1 = deh_c3;
		}

		this.dehf_c3 = originalPerson.dehf_c3;
		this.dehm_c3 = originalPerson.dehm_c3;
		this.dehsp_c3 = originalPerson.dehsp_c3;
		this.dehsp_c3_lag1 = originalPerson.deh_c3_lag1;

		if (originalPerson.dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //If under age to leave education, set flag for being in education to true
			this.ded = Indicator.True;
		} else {
			this.ded = originalPerson.ded;
		}

		this.der = originalPerson.der;
		this.dcpyy = originalPerson.dcpyy;
		this.dcpagdf = originalPerson.dcpagdf;
		this.household_status = originalPerson.household_status;
		this.household_status_lag = originalPerson.household_status_lag;
		this.les_c4 = originalPerson.les_c4;

		if (originalPerson.les_c4_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
			this.les_c4_lag1 = originalPerson.les_c4_lag1;
		} else {
			this.les_c4_lag1 = les_c4;
		}

		this.les_c7_covid = originalPerson.les_c7_covid;
		if (originalPerson.les_c7_covid_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
			this.les_c7_covid_lag1 = originalPerson.les_c7_covid_lag1;
		} else {
			this.les_c7_covid_lag1 = les_c7_covid;
		}

		this.lesdf_c4 = originalPerson.lesdf_c4;
		this.lessp_c4 = originalPerson.lessp_c4;
		this.activity_status_partner_lag = originalPerson.activity_status_partner_lag;
		this.dcpst = originalPerson.dcpst;
		this.dcpen = originalPerson.dcpen;
		this.dcpex = originalPerson.dcpex;
		this.ypnbihs_dv = originalPerson.ypnbihs_dv;
		this.ypnbihs_dv_lag1 = originalPerson.ypnbihs_dv_lag1;
		this.yptciihs_dv = originalPerson.yptciihs_dv;
		this.yplgrs_dv = originalPerson.yplgrs_dv;
		this.ynbcpdf_dv = originalPerson.ynbcpdf_dv;
		this.dlltsd = originalPerson.dlltsd;
		this.dlltsd_lag1 = originalPerson.dlltsd_lag1;
		this.sedex = originalPerson.sedex;
		this.partnership_samesex = originalPerson.partnership_samesex;
		this.women_fertility = originalPerson.women_fertility;
		this.education_inrange = originalPerson.education_inrange;
		this.toGiveBirth = originalPerson.toGiveBirth;
		this.toLeaveSchool = originalPerson.toLeaveSchool;
		this.partner = originalPerson.partner;
		this.id_partner = originalPerson.id_partner;
		this.weight = originalPerson.weight;
		this.id_mother = originalPerson.id_mother;
		this.id_father = originalPerson.id_father;
//		this.benefitUnit = person.benefitUnit;
//		this.householdId = person.householdId;
		this.dhe = originalPerson.dhe;
		this.dhm = originalPerson.dhm;

		if (originalPerson.dhe_lag1 != 0.0) { //If original person misses lagged level of health, assign current level of health as lagged value
			this.dhe_lag1 = originalPerson.dhe_lag1;
		} else {
			this.dhe_lag1 = originalPerson.dhe;
		}

		if (originalPerson.dhm_lag1 != 0.0) {
			this.dhm_lag1 = originalPerson.dhm_lag1;
		} else {
			this.dhm_lag1 = originalPerson.dhm;
		}

		if (originalPerson.labourSupplyWeekly_L1 != null) {
			this.labourSupplyWeekly_L1 = originalPerson.labourSupplyWeekly_L1;
		} else {
			this.labourSupplyWeekly_L1 = originalPerson.labourSupplyWeekly;
		}

		this.dhesp = originalPerson.dhesp; //Is it fine to assign here?
		this.dhesp_lag1 = originalPerson.dhesp_lag1; 
		this.hoursWorkedWeekly = originalPerson.hoursWorkedWeekly;
//		this.unitLabourCost = originalPerson.unitLabourCost;
		this.labourSupplyWeekly = originalPerson.labourSupplyWeekly;
		this.potentialEarnings = originalPerson.potentialEarnings;
		this.desiredAgeDiff = originalPerson.desiredAgeDiff;
		this.desiredEarningsPotentialDiff = originalPerson.desiredEarningsPotentialDiff;
		this.scoreMale = originalPerson.scoreMale;
		this.scoreFemale = originalPerson.scoreFemale;
		this.countMale = originalPerson.countMale;
		this.countFemale = originalPerson.countFemale;
		this.inverseMillsRatioMaxMale = originalPerson.inverseMillsRatioMaxMale;
		this.inverseMillsRatioMinMale  = originalPerson.inverseMillsRatioMinMale;
		this.inverseMillsRatioMaxFemale = originalPerson.inverseMillsRatioMaxFemale;
		this.inverseMillsRatioMinFemale = originalPerson.inverseMillsRatioMinFemale;
		this.countOK = originalPerson.countOK;
		this.countNaN = originalPerson.countNaN;
		this.countOKover32 = originalPerson.countOKover32;

		this.adultchildflag = originalPerson.adultchildflag;
		yearlyEquivalisedDisposableIncomeSeries = new Series.Double(this, DoublesVariables.EquivalisedIncomeYearly);
		yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
		yearlyEquivalisedConsumption = originalPerson.yearlyEquivalisedConsumption;
		sIndexYearMap = new LinkedHashMap<Integer, Double>();
		this.dhh_owned = originalPerson.dhh_owned;
		this.receivesBenefitsFlag = originalPerson.receivesBenefitsFlag;
		this.receivesBenefitsFlag_L1 = originalPerson.receivesBenefitsFlag_L1;
		
	}


	//As above, but used in population alignment. In this case new persons are created as single, so not all properties can be copied, e.g. spouse's education level
	public Person (Person originalPerson, boolean alignmentTrueFalse) {
		this(personIdCounter++);

		this.id_hh_original = originalPerson.id_household;
		this.id_bu_original = originalPerson.id_benefitUnit;
		this.id_original = originalPerson.key.getId();
		
		this.dag = originalPerson.dag;
		this.dag_sq = originalPerson.dag_sq;
		this.dgn = originalPerson.dgn;
		this.deh_c3 = originalPerson.deh_c3;
		this.deh_c3_lag1 = originalPerson.deh_c3_lag1;
		this.dehf_c3 = originalPerson.dehf_c3;
		this.dehm_c3 = originalPerson.dehm_c3;
		this.ded = originalPerson.ded;
		this.der = originalPerson.der;
		this.dcpyy = 0; //Set number of years in partnership to 0 since we create these as single
		this.dcpyy_lag1 = 0;
		this.household_status = Household_status.Single;
		this.household_status_lag = Household_status.Single;
		this.les_c4 = originalPerson.les_c4;

		if (originalPerson.les_c4_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
			this.les_c4_lag1 = originalPerson.les_c4_lag1;
		} else {
			this.les_c4_lag1 = les_c4;
		}

		this.les_c7_covid = originalPerson.les_c7_covid;
		if (originalPerson.les_c7_covid_lag1 != null) { //If original persons misses lagged activity status, assign current activity status
			this.les_c7_covid_lag1 = originalPerson.les_c7_covid_lag1;
		} else {
			this.les_c7_covid_lag1 = les_c7_covid;
		}

		if (originalPerson.labourSupplyWeekly_L1 != null) {
			this.labourSupplyWeekly_L1 = originalPerson.labourSupplyWeekly_L1;
		} else {
			this.labourSupplyWeekly_L1 = originalPerson.labourSupplyWeekly;
		}

		this.dcpst = Dcpst.SingleNeverMarried;
		this.dcpst_lag1 = Dcpst.SingleNeverMarried;
		this.dcpen = Indicator.False;
		this.dcpex = Indicator.False;
		this.ypnbihs_dv = originalPerson.ypnbihs_dv;
		this.ypnbihs_dv_lag1 = originalPerson.ypnbihs_dv_lag1;
		this.yptciihs_dv = originalPerson.yptciihs_dv;
		this.toGiveBirth = false;
		this.dhe = originalPerson.dhe;
		this.dhm = originalPerson.dhm;

		if (originalPerson.dhe_lag1 != 0.0) { //If original person misses lagged level of health, assign current level of health as lagged value
			this.dhe_lag1 = originalPerson.dhe_lag1;
		} else {
			this.dhe_lag1 = originalPerson.dhe;
		}

		if (originalPerson.dhm_lag1 != 0.0) {
			this.dhm_lag1 = originalPerson.dhm_lag1;
		} else {
			this.dhm_lag1 = originalPerson.dhm;
		}

		this.dlltsd = originalPerson.dlltsd;
		this.dlltsd_lag1 = originalPerson.dlltsd_lag1;
		this.sedex = originalPerson.sedex;
		this.women_fertility = originalPerson.women_fertility;
		this.education_inrange = originalPerson.education_inrange;
//		this.unitLabourCost = originalPerson.unitLabourCost;
		this.labourSupplyWeekly = originalPerson.labourSupplyWeekly;
		this.hoursWorkedWeekly = originalPerson.hoursWorkedWeekly;
		this.potentialEarnings = originalPerson.potentialEarnings;
		this.desiredAgeDiff = originalPerson.desiredAgeDiff;
		this.desiredEarningsPotentialDiff = originalPerson.desiredEarningsPotentialDiff;
		this.weight = 1.;
		this.clonedFlag = true;
		this.originalNumberChildren = originalPerson.getBenefitUnit().getN_children_allAges();
		this.originalHHStatus = originalPerson.household_status; //Keep track of original HH status to decide what happens to the cloned person with benefitUnit formation
		this.ageGroup = originalPerson.ageGroup;
		this.originalPartner = originalPerson.partner;
		this.adultchildflag = originalPerson.adultchildflag;
		this.yearlyEquivalisedDisposableIncomeSeries = originalPerson.yearlyEquivalisedDisposableIncomeSeries;
		this.yearlyEquivalisedConsumptionSeries = new Series.Double(this, DoublesVariables.EquivalisedConsumptionYearly);
		this.yearlyEquivalisedConsumption = originalPerson.yearlyEquivalisedConsumption;
		this.sIndexYearMap = originalPerson.sIndexYearMap;
		this.dhh_owned = originalPerson.dhh_owned;
		this.receivesBenefitsFlag = originalPerson.receivesBenefitsFlag;
		this.receivesBenefitsFlag_L1 = originalPerson.receivesBenefitsFlag_L1;

	}
	
	// ---------------------------------------------------------------------
	// Initialisation methods
	// ---------------------------------------------------------------------
	
	public void setAdditionalFieldsInInitialPopulation() {			
		
		
		if (!model.isUseWeights()) {
			labourSupplyWeekly = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(id_original).intValue(), getDgn()); //TODO: Why is this initialised in this way instead of the simpler commented out way below?
		} else {
			labourSupplyWeekly = Labour.convertHoursToLabour(model.getInitialHoursWorkedWeekly().get(key.getId()).intValue(), getDgn());
		}

		receivesBenefitsFlag_L1 = receivesBenefitsFlag;

		labourSupplyWeekly_L1 = labourSupplyWeekly;

		//Assign to age groups for SBAM matching procedure:
		if(model.getUnionMatchingMethod().equals(UnionMatchingMethod.SBAM)) { //If using SBAM matching
			updateAgeGroup();
		}
		
//		labourSupplyWeekly = Labour.convertHoursToLabour(hoursWorkedWeekly, getDgn());		//Will be updated in Labour Market Module unless the person is retired or a student.
		hoursWorkedWeekly = null;	//Not to be updated as labourSupplyWeekly contains this information.
		initializeHouseholdStatus();		
		updateVariables();


//		//Sample deviationFromMeanRetirementAge until person in initial population is not immediately retired off 
//		//(don't want to change their activity status immediately).  This is because we need to sample from the conditional 
//		//distribution of retirement age given that the person has not yet retired (which is equivalent to a truncated normal 
//		//distribution with the same mean and variance as the unconditional distribution).
//		if(!activity_status.equals(Les_c4.Retired)) {
//			if(age >= model.getMinRetireAge(gender)) {		//No need to check whether student as the deviationFromMeanRetirementAge is truncated so that it is always above min retirement Age, which is 45.
//				while(age >= deviationFromMeanRetirementAge + ((Number)Parameters.getRetirementAge().getValue(model.getYear(), gender.toString() + "_Mean")).doubleValue()) {
//					setDeviationFromMeanRetirementAge();								
//				}
////				log.debug("age " + age + " retirement age " + (deviationFromMeanRetirementAge + ((Number)Parameters.getRetirementAge().getValue(model.getYear(), gender.toString() + "_Mean")).doubleValue()));
//			}
//		}
				
	}
	
	//This method assign people to age groups used to define types in the SBAM matching procedure
	private void updateAgeGroup() {
		if(dag < 18) {
			ageGroup = 0;
			model.tmpPeopleAssigned++;
		} else if(dag >= 18 && dag < 21) {
			ageGroup = 1;
			model.tmpPeopleAssigned++;
		} else if(dag >= 21 && dag < 24) {
			ageGroup = 2;
			model.tmpPeopleAssigned++;
		} else if(dag >= 24 && dag < 27) {
			ageGroup = 3;
			model.tmpPeopleAssigned++;
		} else if(dag >= 27 && dag < 30) {
			ageGroup = 4;
			model.tmpPeopleAssigned++;
		} else if(dag >= 30 && dag < 33) {
			ageGroup = 5;
			model.tmpPeopleAssigned++;
		} else if(dag >= 33 && dag < 36) {
			ageGroup = 6;
			model.tmpPeopleAssigned++;
		} else if(dag >= 36 && dag < 40) {
			ageGroup = 7;
			model.tmpPeopleAssigned++;
		} else if(dag >= 40 && dag < 45) {
			ageGroup = 8;
			model.tmpPeopleAssigned++;
		} else if(dag >= 45 && dag < 55) {
			ageGroup = 9;
			model.tmpPeopleAssigned++;
		} else if(dag >= 55 && dag < 65) {
			ageGroup = 10;
			model.tmpPeopleAssigned++;
		} else if(dag >= 65) {
			ageGroup = 11;
			model.tmpPeopleAssigned++;
		} else {
			System.out.println("Could not assign age group!");
		}
	}
		
	private void initializeHouseholdStatus() {
		
		if(partner != null) {
			household_status = Household_status.Couple;
		}
		else if(id_mother != null || id_father != null) {
			household_status = Household_status.Parents;
		}
		else household_status = Household_status.Single;
		
	}


//	private void setDeviationFromMeanRetirementAge() {		//Modelled as Gender but not time-dependent
//		
//		deviationFromMeanRetirementAge = SimulationEngine.getRnd().nextGaussian() 
//				* ((Number) Parameters.getRetirementAge().getValue(model.getYear(), gender.toString() + "_StandardError")).doubleValue();
//		
//	}
	

	// ---------------------------------------------------------------------
	// Event Listener
	// ---------------------------------------------------------------------


	public enum Processes {
		Ageing,
		CalculateConsumption,
		ConsiderCohabitation,
		ConsiderRetirement,
		GiveBirth,
		Health,
		HealthMentalHM1, 				//Predict level of mental health on the GHQ-12 Likert scale (Step 1)
		HealthMentalHM2,				//Modify the prediction from Step 1 by applying increments / decrements for exposure
		InSchool,
		LeavingSchool,
		UpdatePotentialEarnings,		//Needed to union matching and labour supply
	}
		
	@Override
	public void onEvent(Enum<?> type) {
		switch ((Processes) type) {
		case Ageing:
//			log.debug("Ageing for person " + this.getKey().getId());
			ageing();		
			break;
		case CalculateConsumption:
			calculateConsumption();
			break;
		case ConsiderCohabitation:
//			log.debug("BenefitUnit Formation for person " + this.getKey().getId());
			considerCohabitation();
			break;
		case ConsiderRetirement:
			considerRetirement();
			break;
		case GiveBirth:
//			log.debug("Check whether to give birth for person " + this.getKey().getId());
			GiveBirth();
			break;
		case Health:
//			log.debug("Health for person " + this.getKey().getId());
			health();
			break;
		case HealthMentalHM1:
			healthMentalHM1();
			break;
		case HealthMentalHM2:
			healthMentalHM2();
			break;
		case InSchool:
//			log.debug("In Education for person " + this.getKey().getId());
			inSchool();
			break;
		case LeavingSchool:
			leavingSchool();
			break;
		case UpdatePotentialEarnings:
//			System.out.println("Update wage equation for person " + this.getKey().getId() + " with age " + age + " with activity_status " + activity_status + " and activity_status_lag " + activity_status_lag + " and toLeaveSchool " + toLeaveSchool + " with education " + education);
			updatePotentialEarnings();
			updateNonEmploymentIncome(); //Also update non-employment non-benefit income here
			break;
		}
	}
	
	
	// ---------------------------------------------------------------------
	// Processes
	// ---------------------------------------------------------------------

	private void ageing() {
		

//		System.out.println("Before ageing process: HHID is " + this.getHouseholdId() + "PID is " + this.getKey().getId() + ". Original ID was " + this.getOriginalID() + " and original HHID was " + this.getOriginalHHID() + "Responsible male is " + this.getHousehold().getMaleId() + " Responsible female is " + this.getHousehold().getFemaleId() + "Partner ID is " + this.getPartnerId() + "Mother Id is " + this.getMotherId() + "Father Id is " + this.getFatherId() + "Gender is " + this.getGender() + "Region of benefitUnit is " + this.getHousehold().getRegion() + "Number of children in the HH " + this.getHousehold().getChildren().size() + " Age is " + this.getAge());
			
		dag++;
		dag_sq = dag*dag;
		if(dag > Parameters.getMaxAge()) {
			model.getPersons().remove(this);		//No need to remove person from PersonsByGenderAndAge as person will 'age' out of it automatically.
			benefitUnit.removePerson(this);			//Will remove benefitUnit from model if it is empty
		}

		else if(dag == Parameters.AGE_TO_BECOME_RESPONSIBLE) {
				setupNewBenefitUnit(true);
				considerLeavingHome();
		} else if (dag > Parameters.AGE_TO_BECOME_RESPONSIBLE && adultchildflag!=null && adultchildflag.equals(Indicator.True)) {
			considerLeavingHome();
		}

		//Update years in partnership (before the lagged value is updated)
		dcpyy_lag1 = dcpyy; //Update lag value outside of updateVariables() method
		if(partner != null) {
			if(id_partner.equals(id_partner_lag1)) {
				dcpyy++; 
			}
			else dcpyy = 0; 
		}
		else dcpyy = 0; //If no partner, set years in partnership to 0 TODO: or should this be set to null? 
			
		//Update variables
		updateVariables();	//This also sets the lagged values
		updateAgeGroup(); //Update ageGroup as person ages 
		
		
	}


	//This process should be applied to those at the age to become responsible / leave home OR above if they have the adultChildFlag set to True (i.e. people can move out, but not move back in).
	private void considerLeavingHome() {
		//For those who are moving out, evaluate whether they should have stayed with parents and if yes, set the adultchildflag to true
		boolean toLeaveHome = Parameters.getRegLeaveHomeP1a().event(this, Person.DoublesVariables.class); //If true, should leave home
		if (les_c4.equals(Les_c4.Student)) {
			adultchildflag = Indicator.True; //Students not allowed to leave home to match filtering conditon
		}
		else {
			if (!toLeaveHome) { //If at the age to leave home but regression outcome is negative, person has adultchildflag set to true (although they still set up a new benefitUnit in the simulation, it's treated differently in the labour supply)
				adultchildflag = Indicator.True;
			}
			else {
				adultchildflag = Indicator.False;
				setupNewHousehold(true); //If person leaves home, they set up a new household
				//TODO: Household status and similar variables should be updated automatically. Here or elsewhere?
			}
		}
	}

	private void considerRetirement() {
		if (dag >= Parameters.MIN_AGE_TO_RETIRE && !les_c4.equals(Les_c4.Retired) && !les_c4_lag1.equals(Les_c4.Retired)) {
			boolean toRetire = false;
			if (partner != null) { //Follow process R1b (couple) for retirement
				toRetire = Parameters.getRegRetirementR1b().event(this, Person.DoublesVariables.class);
			} else { //Follow process R1a (single) for retirement
				toRetire = Parameters.getRegRetirementR1a().event(this, Person.DoublesVariables.class);
			}

			if (toRetire) {
				setLes_c4(Les_c4.Retired);
				labourSupplyWeekly = Labour.ZERO;
//				benefitUnit.removeResponsibleAdultsFromSimulationIfAllRetired(); //TODO: What happens if there were children living with parents who both retired? (Should we promote them to responsible adults in this method?)
			}
		}
	}

	/*
	This method corresponds to Step 1 of the mental health evaluation: predict level of mental health on the GHQ-12 Likert scale based on observable characteristics
	 */
	protected void healthMentalHM1() {
		if (dag >= 16) {
			double dhmPrediction = Parameters.getRegHealthHM1().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("HM1");
			dhm = constrainHealthEstimate(dhmPrediction);
		}
	}

	/*
	This method corresponds to Step 2 of the mental health evaluation: increment / decrement the outcome of Step 1 depending on exposures that individual experienced.
	Filtering: only applies to those with Age>=16 & Age<=64. Different estimates for males and females.
	 */
	protected void healthMentalHM2() {

		double dhmPrediction;
		if (dag >= 25 && dag <= 64) {
			if (getDgn().equals(Gender.Male)) {
				dhmPrediction = Parameters.getRegHealthHM2Males().getScore(this, Person.DoublesVariables.class);
				dhm = constrainHealthEstimate(dhmPrediction+dhm);
			} else if (getDgn().equals(Gender.Female)) {
				dhmPrediction = Parameters.getRegHealthHM2Females().getScore(this, Person.DoublesVariables.class);
				dhm = constrainHealthEstimate(dhmPrediction+dhm);
			} else System.out.println("healthMentalHM2 method in Person class: Person has no gender!");
		}
	}

	/*
	Mental health on the GHQ-12 scale has no meaning outside of the original values between 0 and 36. If the predicted value is outside, limit it to fall within these values
	 */
	protected Double constrainHealthEstimate(Double dhm) {
		if (dhm < 0.) {
			dhm = 0.;
		} else if (dhm > 36.) {
			dhm = 36.;
		}
		return dhm;
	}

	//TODO: Should continuous health status and long-term sickness / disability be related somehow? 
	//Health process defines health using H1a or H1b process
	protected void health() {		
		//If age is between 16 - 29 and individual has always been in education, follow process H1a:
		if((dag >= 16 && dag <= 29) && les_c4.equals(Les_c4.Student) && leftEducation == false) {
//			System.out.println("Persid " + getKey().getId() + " Aged " + dag + " Activity status " + les_c4 + " Who left education? " + leftEducation + " assigned to process H1a " + " Gender " + dgn + " BenefitUnit income qtile: " + getHousehold().getYdses_c5() + " BenefitUnit income qtile lag: " + getHousehold().getYdses_c5_lag1() +
//					" BenefitUnit composition is " + getHousehold().getDhhtp_c4() + " Lag1 of benefitUnit composition is " + getHousehold().getDhhtp_c4_lag1() + "cloned? " + isClonedFlag());
			dhe = Parameters.getRegHealthH1a().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("H1a");
//			System.out.println("year is " + model.getYear() + "Persid " + getKey().getId() + " Was assigned dhe " + dhe + " And lagged dhe is " + dhe_lag1 + " In the H1a process");
		}
		
		//If age is over 16 and individual is not in continuous education, follow process H1b and H2b: 
//		else if(dag >= 16 && ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student))) {
		//If above 16 and not entered the previous process (i.e. not 16 - 29 and in continuous education) follow process H1b:
		else if(dag >= 16) {
//			System.out.println("Persid " + getKey().getId() + " Aged " + dag + " Activity status " + les_c4 + " Who left education? " + leftEducation + " assigned to process H1b " + " Gender " + dgn + " BenefitUnit income qtile: " + getHousehold().getYdses_c5() + " BenefitUnit income qtile lag: " + getHousehold().getYdses_c5_lag1() +
//					" BenefitUnit composition is " + getHousehold().getDhhtp_c4() + " Lag1 of benefitUnit composition is " + getHousehold().getDhhtp_c4_lag1() + " Disabled status: " + dlltsd + " And lagged disabled status: " + dlltsd_lag1);
			dhe = Parameters.getRegHealthH1b().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("H1b");
//			System.out.println("Persid " + getKey().getId() + " Was assigned dhe " + dhe + " And lagged dhe is " + dhe_lag1 + " In the H1b process");
			
			//If age is over 16 and individual is not in continuous education, also follow process H2b to calculate the probability of long-term sickness / disability:
			boolean becomeLTSickDisabled = Parameters.getRegHealthH2b().event(this, Person.DoublesVariables.class); //If true, person becomes disabled. If false, person is not-disabled.
			//TODO: Do we want to allow long-term sick or disabled to recover? 
			if(becomeLTSickDisabled == true) {
				dlltsd = Indicator.True;
			} else if(becomeLTSickDisabled == false) {
				dlltsd = Indicator.False;
			}
//			System.out.println("Persid " + getKey().getId() + " was determined to be disabled? " + becomeLTSickDisabled + " Dlltsd is: " + dlltsd + " And lag1 was: " + dlltsd_lag1);
		}


		//Children's health status is not modelled (but also not used), if missing assign the average value observed in the initial population for that age group:
//		if (dag <= 16 && dhe == 0.0) {
//			dhe = 4.;
//			dhe_lag1 = 4.;
//		}
		//Bound health status between 1 and 5:
		if (dag > 16 && dhe < 1.) {
			dhe = 1.;
		}
		else if (dhe > 5.) {
			dhe = 5.;
		}

		
	}





	protected void inSchool() {
		
		//Min age to leave education set to 16 (from 18 previously) but note that age to leave home is 18.
		if(les_c4.equals(Les_c4.Retired) || dag < Parameters.MIN_AGE_TO_LEAVE_EDUCATION || dag > Parameters.MAX_AGE_TO_ENTER_EDUCATION) {		//Only apply module for persons who are old enough to consider leaving education, but not retired
			return;
		}
		
		//If age is between 16 - 29 and individual has always been in education, follow process E1a:
		else if(les_c4.equals(Les_c4.Student) && !leftEducation && dag >= Parameters.MIN_AGE_TO_LEAVE_EDUCATION) { //leftEducation is initialised to false and updated to true when individual leaves education (and never reset).
			if (dag <= 29) {
				double toStayAtSchoolScore = Parameters.getRegEducationE1a().getScore(this, Person.DoublesVariables.class);
				toLeaveSchool = !Parameters.getRegEducationE1a().event(this, Person.DoublesVariables.class); //If event is true, stay in school.  If event is false, leave school.
			} else if (dag > 30){
				toLeaveSchool = true; //Hasn't left education until 30 - force out
			}
		}

		//If age is between 16 - 45 and individual has not continuously been in education, follow process E1b:
		//Either individual is currently a student and has left education at some point in the past (so returned) or individual is not a student so has not been in continuous education:
		else if(dag <= 45 && (!les_c4.equals(Les_c4.Student) || leftEducation)) { //leftEducation is initialised to false and updated to true when individual leaves education for the first time (and never reset).
			//TODO: If regression outcome of process E1b is true, set activity status to student and der (return to education indicator) to true?
			if(Parameters.getRegEducationE1b().event(this, Person.DoublesVariables.class)) { //If event is true, re-enter education.  If event is false, leave school
				
//				System.out.println("Persid " + getKey().getId() + " Aged: " + dag + " With activity status: " + les_c4 + " Assigned to the E1b process");
				
				setLes_c4(Les_c4.Student);
				setDer(Indicator.True);
				setDed(Indicator.True);
				labourSupplyWeekly = Labour.ZERO; //Assume no part-time work while studying
				//TODO: Note that individuals re-entering education do not have a new level of education set. (They don't "leave school")
			}
			else if (les_c4.equals(Les_c4.Student)){ //If activity status is student but regression to be in education was evaluated to false, remove student status
				setLes_c4(Les_c4.NotEmployed);
				setDed(Indicator.False);
				toLeaveSchool = true; //Test what happens if people who returned to education leave again
			}
		}
		else if (dag > 45 && les_c4.equals(Les_c4.Student)) { //People above 45 shouldn't be in education, so if someone re-entered at 45 in previous step, force out
			setLes_c4(Les_c4.NotEmployed);
			setDed(Indicator.False);
		}
		

		
	}	
	
	protected void leavingSchool() {
		if(toLeaveSchool) {
			setEducationLevel(); //If individual leaves school follow process E2a to assign level of education
			setSedex(Indicator.True); //Set variable left education (sedex) if leaving school
			setDed(Indicator.False); //Set variable in education (ded) to false if leaving school
			setLeftEducation(true); //This is not reset and indicates if individual has ever left school - used with health process
			setLes_c4(Les_c4.NotEmployed); //Set activity status to NotEmployed when leaving school to remove Student status
		}
	}


	protected void considerCohabitation() {

		toBePartnered = false;

		if (model.getCountry().equals(Country.UK)) {

			//Apply only to individuals above age defined in MIN_AGE_MARRIAGE
			if (dag >= Parameters.MIN_AGE_COHABITATION) {
				//If not in partnership, follow U1a or U1b to determine probability of entering partnership
				if (partner == null) {
					//Follow process U1a for individuals aged MIN_AGE_MARRIAGE-29 who are in continuous education:
					if (dag <= 29 && les_c4.equals(Les_c4.Student) && leftEducation == false) {
						toBePartnered = Parameters.getRegPartnershipU1a().event(this, Person.DoublesVariables.class);
						if (toBePartnered) { //If true, look for a partner
							model.getPersonsToMatch().get(dgn).get(getRegion()).add(this); //Will look for partner in model's unionMatching process
						}
					}
					//Follow process U1b for individuals who are not in continuous education:
					else if ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {
						toBePartnered = Parameters.getRegPartnershipU1b().event(this, Person.DoublesVariables.class);
						if (toBePartnered) {
							model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
						}
					}
				}

				//If in partnership, follow U2b to determine the probability of exiting partnership by female member of the couple not in education
				//Note: this implies a 0 probability of splitting for couples in which female is in continuous education
				else if (partner != null) {
					if (dgn.equals(Gender.Female) && ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student))) {
						if (Parameters.getRegPartnershipU2b().event(this, Person.DoublesVariables.class)) { //If true, leave partner
							leavePartner();
						}
					}
				}
			}
		}
		else if (model.getCountry().equals(Country.IT)) {
			if (dag >= Parameters.MIN_AGE_COHABITATION) {
				//If not in partnership, follow U1a or U1b to determine probability of entering partnership
				if (partner == null) {
					//Follow process U1 for individuals who are not in continuous education:
					if ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {
						toBePartnered = Parameters.getRegPartnershipITU1().event(this, Person.DoublesVariables.class);
						if (toBePartnered) {
							model.getPersonsToMatch().get(dgn).get(getRegion()).add(this);
						}
					}
				}

				//If in partnership, follow U2 to determine the probability of exiting partnership by female member of the couple not in education
				//Note: this implies a 0 probability of splitting for couples in which female is in continuous education
				else if (partner != null) {
					if (dgn.equals(Gender.Female) && ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student))) {
						if (Parameters.getRegPartnershipITU2().event(this, Person.DoublesVariables.class)) { //If true, leave partner
							leavePartner();
						}
					}
				}
			}
		}
	}

	
	private void GiveBirth() {				//To be called once per year after fertility alignment

		if(toGiveBirth) {		//toGiveBirth is determined by fertility alignment (in the model class)			
			
			boolean babyMale = RegressionUtils.event(Parameters.PROB_NEWBORN_IS_MALE);
			Gender babyGender = babyMale? Gender.Male : Gender.Female;
			
			//Give birth to new person and add them to benefitUnit.
			Person child = new Person(babyGender, this);
			model.getPersons().add(child);
			benefitUnit.addChild(child);
			
			//Update maternity status
			benefitUnit.newBornUpdate();			//Update benefitUnit fields related to new born children
			toGiveBirth = false;						//Reset boolean for next year
		} 	
	}


	protected void updatePotentialEarnings() {

		LinearRegression regWages;
		double logPotentialEarnings;

		// Added an option of drawing the random component only once per simulation. To do that, check if the variable is null - if not, it needs to be drawn. Otherwise, use its value.
		if (model.fixRegressionStochasticComponent) {

			if (getWageRegressionRandomComponent() == null) {
				if (getDgn().equals(Gender.Male)) {
					setWageRegressionRandomComponent(getRandomGaussianAndRMSEProductForRegression("Wages_Males"));
				} else {
					setWageRegressionRandomComponent(getRandomGaussianAndRMSEProductForRegression("Wages_Females"));
				}
			}

			if(dgn.equals(Gender.Male)) {
				regWages = Parameters.getRegWagesMales();

			} else {
				regWages = Parameters.getRegWagesFemales();
			}

			logPotentialEarnings = regWages.getScore(this, Person.DoublesVariables.class) + getWageRegressionRandomComponent();
		} else {

			if (dgn.equals(Gender.Male)) {
				logPotentialEarnings = Parameters.getRegWagesMales().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("Wages_Males");
			} else {
				logPotentialEarnings = Parameters.getRegWagesFemales().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("Wages_Females");
			}
		}

		// Uprate and set level of potential earnings
		double upratedLevelPotentialEarnings = Math.exp(logPotentialEarnings)* getUpratingFactorForMonetaryValues();
		setPotentialEarnings(upratedLevelPotentialEarnings);

		if (model.debugCommentsOn) {
			log.debug("logPotentialEarnings: " + logPotentialEarnings + ", potentialEarnings: " + potentialEarnings);
		}
	}

	public double getUpratingFactorForMonetaryValues() {
		return Parameters.upratingFactorForMonetaryValuesMap.get(model.getYear()); //Get uprating factor for a given simulated year (given the year for which wage equation was estimated as specified in Parameters.BASE_PRICE_YEAR
	}
	
	protected void updateNonEmploymentIncome() {

		// Uprating factor is only used for years later than 2017 to account for growth. Set to 1 as default for other years.
		double upratingFactor = 1;
		if (model.getYear() > model.getTimeTrendStopsInMonetaryProcesses()) {
			upratingFactor = getUpratingFactorForMonetaryValues();
		}

		// Capital income and pensions are monthly

			if (dag >= Parameters.MIN_AGE_TO_HAVE_INCOME) {
				if (dag <= 29 && les_c4.equals(Les_c4.Student) && leftEducation == false) {
					boolean hasCapitalIncome = Parameters.getRegIncomeI3a_selection().event(this, Person.DoublesVariables.class); // If true, individual receives capital income ypncp. Amount modelled in the next step.
					if (hasCapitalIncome) {
						double capinclevel = Math.sinh(Parameters.getRegIncomeI3a().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("I3a"))*upratingFactor;
						ypncp = Math.log(capinclevel + Math.sqrt(capinclevel * capinclevel + 1.0)); //Capital income amount
					}
					else ypncp = 0.; //If no capital income, set amount to 0
				}
				else if ((les_c4.equals(Les_c4.Student) && leftEducation == true) || !les_c4.equals(Les_c4.Student)) {
					boolean hasCapitalIncome = Parameters.getRegIncomeI3b_selection().event(this, Person.DoublesVariables.class); // If true, individual receives capital income ypncp. Amount modelled in the next step.
					if (hasCapitalIncome) {
						double capinclevel = Math.sinh(Parameters.getRegIncomeI3b().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("I3b"))*upratingFactor;
						ypncp = Math.log(capinclevel + Math.sqrt(capinclevel * capinclevel + 1.0)); //Capital income amount
					}
					else ypncp = 0.; //If no capital income, set amount to 0
				}
				if (les_c4.equals(Les_c4.Retired)) { // Retirement decision is modelled in the retirement process. Here only the amount of pension income for retired individuals is modelled.
					double pensioninclevel = Math.sinh(Parameters.getRegIncomeI3c().getScore(this, Person.DoublesVariables.class) + getRandomGaussianAndRMSEProductForRegression("I3c"))*upratingFactor;
					ypnoab = Math.log(pensioninclevel + Math.sqrt(pensioninclevel * pensioninclevel + 1.0));
					if (ypnoab < 0 ) {
						ypnoab = 0.;
					}
				}

				double capital_income_multiplier = model.getSavingRate()/Parameters.SAVINGS_RATE;
				double yptciihs_dv_tmp_level = capital_income_multiplier*(Math.sinh(ypncp) + Math.sinh(ypnoab)); //Multiplied by the capital income multiplier, defined as chosen savings rate divided by the long-term average (specified in Parameters class)
				yptciihs_dv = Math.log(yptciihs_dv_tmp_level + Math.sqrt(yptciihs_dv_tmp_level * yptciihs_dv_tmp_level + 1.0)); //Non-employment non-benefit income is the sum of capital income and, for retired individuals, pension income.

			}
	}
	

	// ---------------------------------------------------------------------
	// Other Methods
	// ---------------------------------------------------------------------

	
	private boolean becomeResponsibleInHouseholdIfPossible() {
		if(dgn.equals(Gender.Male)) {
			if(benefitUnit.getMale() == null) {
				benefitUnit.setMale(this);	//This person is male, so becomes responsible male of benefitUnit
				return benefitUnit.getMale() == this;			//Should return true;
			}
			else return false;
//			else throw new IllegalArgumentException("ERROR - BenefitUnit " + benefitUnit.getKey().getId() + " already has a responsible male!  Cannot set resonsible male as person " + key.getId());
		}
		else {	//This person is female
			if(benefitUnit.getFemale() == null) {
				benefitUnit.setFemale(this);	//This person is female, so becomes responsible female of benefitUnit
				return benefitUnit.getFemale() == this;		//Should return true;
			}
			else return false;
//			else throw new IllegalArgumentException("ERROR - BenefitUnit " + benefitUnit.getKey().getId() + " already has a responsible female!  Cannot set resonsible female as person " + key.getId());
		}
	}

	public boolean atRiskOfWork() {

		/*
        Person "flexible in labour supply" must meet the following conditions:
        age >= 16 and <= 75
        not a student or retired
        not disabled
         */

		if(dag < Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY)
			return false;
		if(dag > Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY)
			return false;
		if (les_c4.equals(Les_c4.Retired))
			return false;
		if(les_c4.equals(Les_c4.Student))
			return false;
		if(dlltsd.equals(Indicator.True))
			return false;
		
		//For cases where the participation equation used for the Heckmann Two-stage correction of the wage equation results in divide by 0 errors.
		//These people will not work for any wage (their activity status will will be set to Nonwork in the Labour Market Module
		double inverseMillsRatio = getDoubleValue(DoublesVariables.InverseMillsRatio);
		if(!Double.isFinite(inverseMillsRatio)) {
			return false;
		}
		
		return true;		//Else return true
	}


	// Assign education level to school leavers using MultiProbitRegression
	// Note that persons are now assigned a Low education level by default at birth (to prevent null pointer exceptions when persons become old enough to marry while still being a student
	// (we now allow students to marry, given they can re-enter school throughout their lives).
	// The module only applies to students who are leaving school (activityStatus == Student and toLeaveSchool == true) - see inSchool()
	//TODO: Follow process E2a to assign education level
	private void setEducationLevel() {

//		if( activity_status.equals(Les_c4.Student) && toLeaveSchool) {
			
			//TODO: Need to see if it is possible to change MultiProbitRegression, so that we don't have to pass the Education class below, as it could
			// potentially cause problems if this variable would ever be set as a different class to the type that MultiProbitRegression has been 
			// initialised with (i.e. the T type in the classes).
			
		
//			System.out.println("Person id " + getKey().getId() + "Dgn " + dgn + "Dag " + dag + "Dag_sq " + dag_sq + "Dehm" +
//								dehm_c3 + " Dehf " + dehf_c3);

			Education newEducationLevel = Parameters.getRegEducationE2a().eventType(this, Person.DoublesVariables.class, Education.class);

//			System.out.println("Persid " + getKey().getId() + " Aged: " + dag + " With activity status: " + les_c4 + " Was set to leave school?  " + toLeaveSchool +  " Predicted education level "
//					+ " by process E2a is " + newEducationLevel + " And previous level was " + deh_c3);
			
			//Education has been set to Low by default for all new born babies, so it should never be null.
			//This is because we no longer prevent people in school to get married, given that people can re-enter education throughout their lives.
			//Note that by not filtering out students, we must assign a low education level by default to persons at birth to prevent a null pointer exception when new born persons become old enough to marry if they have not yet left school because 
			//their education level has not yet been assigned.			

//			System.out.println("Person with " + "age " + dag + " age sq " + dag_sq + " gender " + dgn + " mother ed " + dehm_c3 + " father ed " + dehf_c3 + " region " + getHousehold().getRegion() + " was assigned educ level " + newEducationLevel);

			if (newEducationLevel.equals(Education.Low)) {
				model.lowEd++;
			}
			else if (newEducationLevel.equals(Education.Medium)) {
				model.medEd++;
			}
			else if (newEducationLevel.equals(Education.High)) {
				model.highEd++;
			}
			else {
				model.nothing++;
			}


			if(deh_c3 != null) {
				if(newEducationLevel.ordinal() > deh_c3.ordinal()) {		//Assume Education level cannot decrease after re-entering school.
					deh_c3 = newEducationLevel;
				}
			}
			else {
				deh_c3 = newEducationLevel;
			}
			

//			System.out.println("Education level is " + education + " new education level is " + newEducationLevel);
//			education = newEducationLevel;
			
//		}
	}


	protected void calculateConsumption() {
		double income = 0.;

		if (benefitUnit.getEquivalisedDisposableIncomeYearly() != null) {
			income = benefitUnit.getEquivalisedDisposableIncomeYearly();
		}


		if (getLes_c4().equals(Les_c4.Retired)) {
			setYearlyEquivalisedConsumption(income);
		} else if (income > 0){
			setYearlyEquivalisedConsumption((1-model.getSavingRate())*income);
		}
		else {
			setYearlyEquivalisedConsumption(0.);
		}
	}

	protected void updateVariables() {
		
		//Reset flags to default values
		toLeaveSchool = false;
		toGiveBirth = false;
		sedex = Indicator.False; //Reset left education variable
		der = Indicator.False; //Reset return to education indicator
		ded = Indicator.False; //Reset in education variable
	
		
		//Lagged variables
		//Dcpyy_lag1 is updated in ageing() process
		les_c4_lag1 = les_c4;
		les_c7_covid_lag1 = les_c7_covid;
		household_status_lag = household_status;
		dhe_lag1 = dhe; //Update lag(1) of health
		dhm_lag1 = dhm; //Update lag(1) of mental health
		dlltsd_lag1 = dlltsd; //Update lag(1) of long-term sick or disabled status
		deh_c3_lag1 = deh_c3; //Update lag(1) of education level
		ypnbihs_dv_lag1 = ypnbihs_dv; //Update lag(1) of gross personal non-benefit income
		dehsp_c3_lag1 = dehsp_c3; //Update lag(1) of partner's education status
		dhesp_lag1 = dhesp; //Update lag(1) of partner's health
		ynbcpdf_dv_lag1 = ynbcpdf_dv; //Lag(1) of difference between own and partner's gross personal non-benefit income
		id_partner_lag1 = id_partner; //Lag(1) of partner's ID
		dcpagdf_lag1 = dcpagdf; //Lag(1) of age difference between partners
		lesdf_c4_lag1 = lesdf_c4; //Lag(1) of own and partner's activity status
		dcpst_lag1 = dcpst; //Lag(1) of partnership status 
		
		yplgrs_dv_lag3 = yplgrs_dv_lag2; //Lag(3) of gross personal employment income
		yplgrs_dv_lag2 = yplgrs_dv_lag1; //Lag(2) of gross personal employment income
		yplgrs_dv_lag1 = yplgrs_dv; //Lag(1) of gross personal employment income
		
		yptciihs_dv_lag3 = yptciihs_dv_lag2; //Lag(3) of gross personal non-employment non-benefit income
		yptciihs_dv_lag2 = yptciihs_dv_lag1; //Lag(2) of gross personal non-employment non-benefit income
		yptciihs_dv_lag1 = yptciihs_dv; //Lag(1) of gross personal non-employment non-benefit income

		ypncp_lag2 = ypncp_lag1;
		ypncp_lag1 = ypncp;

		ypnoab_lag2 = ypnoab_lag1;
		ypnoab_lag1 = ypnoab;

		labourSupplyWeekly_L1 = labourSupplyWeekly; // Lag(1) of labour supply
		receivesBenefitsFlag_L1 = receivesBenefitsFlag; // Lag(1) of flag indicating if individual receives benefits
		
		if(les_c4.equals(Les_c4.Student)) {
			labourSupplyWeekly = Labour.ZERO;			//Number of hours of labour supplied each week 			
//			hoursWorkedWeekly = null;			
			potentialEarnings = 0.;
			setDed(Indicator.True); //Indicator if in school set to true
		}

		if (les_c4.equals(Les_c4.Retired)) {
			labourSupplyWeekly = Labour.ZERO;
			potentialEarnings = 0.;
		}

		if (les_c4.equals(Les_c4.NotEmployed)) {
			labourSupplyWeekly = Labour.ZERO;
		}
		
		if(les_c4.equals(Les_c4.EmployedOrSelfEmployed)) {
			//Increment work history by 12 months for those in employment
			//TOOD: I don't think liwwh is used anywhere in the model at the moment, perhaps can be deleted (PB, 01.11.2021)
			liwwh = liwwh+12; 
		}
		
		
		//TODO: Should this be done here? 
		//Update partner's variables  
		if(partner != null) {
			if(partner.getDeh_c3() != null) {
				dehsp_c3 = getPartner().getDeh_c3(); 
			} else dehsp_c3 = null; 
			if(partner.getDhe() != null) {
				dhesp = getPartner().getDhe();
			} else dhesp = null; 
			if(partner.getYpnbihs_dv() != null && ypnbihs_dv != null) {
//				Double ynbcpdf_dv_level = Math.sinh(ypnbihs_dv) - Math.sinh(getPartner().getYpnbihs_dv()); //Calculate the difference in levels between own and partner's gross personal non-benefit income
//				ynbcpdf_dv = Math.log(ynbcpdf_dv_level + Math.sqrt(ynbcpdf_dv_level * ynbcpdf_dv_level + 1.0)); //Asinh transformation
				//Keep as difference between transformed variables to maintain compatibility with estimates
				ynbcpdf_dv = ypnbihs_dv - getPartner().getYpnbihs_dv();
			} else ynbcpdf_dv = null;
			
			dcpagdf = dag - partner.dag; //Calculate the difference between own and partner's age
			
			
			//Determine lesdf_c4 (own and partner activity status) based on own les_c4 and partner's les_c4
			if(les_c4 != null && partner.getLes_c4() != null) {
				if(les_c4.equals(Les_c4.EmployedOrSelfEmployed)) {
					if(partner.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
						lesdf_c4 = Lesdf_c4.BothEmployed; 
					}
					else if(partner.getLes_c4().equals(Les_c4.NotEmployed) || partner.getLes_c4().equals(Les_c4.Student) || partner.getLes_c4().equals(Les_c4.Retired)) {
						lesdf_c4 = Lesdf_c4.EmployedSpouseNotEmployed;
					}
					else lesdf_c4 = null; //TODO: Should we throw an exception?
				}
				else if(les_c4.equals(Les_c4.NotEmployed) || les_c4.equals(Les_c4.Student) || les_c4.equals(Les_c4.Retired)) {
					if(partner.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed)) {
						lesdf_c4 = Lesdf_c4.NotEmployedSpouseEmployed; 
					}
					else if(partner.getLes_c4().equals(Les_c4.NotEmployed) || partner.getLes_c4().equals(Les_c4.Student) || partner.getLes_c4().equals(Les_c4.Retired)) {
						lesdf_c4 = Lesdf_c4.BothNotEmployed;
					}
					else lesdf_c4 = null; //TODO: Should we throw an exception?
				}
			}
			else lesdf_c4 = null;
		} 
		
		//Determine partnership status (dcpst):
		if(partner == null) {
			if(leftPartnership == true) { //No partner, but left partnership in the past
				dcpst = Dcpst.PreviouslyPartnered;
			}
			else dcpst = Dcpst.SingleNeverMarried; //No partner and never left partnership in the past
		}
		else dcpst = Dcpst.Partnered;

		//Draw desired age and wage differential for parametric partnership formation for people above age to get married:
		//TODO: Should it be updated yearly or only if null?
		if (dag >= Parameters.MIN_AGE_COHABITATION) {
			double[] sampledDifferentials = Parameters.getWageAndAgeDifferentialMultivariateNormalDistribution().sample(); //Sample age and wage differential from the bivariate normal distribution
				desiredAgeDiff = sampledDifferentials[0];
				desiredEarningsPotentialDiff = sampledDifferentials[1];
		}

	}

	public void updateBenefitUnit(BenefitUnit benefitUnit) {
		this.benefitUnit.removePerson(this);
		setBenefitUnit(benefitUnit);			//Sets both benefitUnit and householdID
	}

	protected void leavePartner() {
		//Update partner's variables first
		partner.setPartner(null); //Do for the partner as we model the probability of couple splitting based on female leaving the partnership
		partner.setDcpyy(null);
		partner.setDcpex(Indicator.True); 
		partner.setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
		partner.setHousehold_status(Household_status.Single);
		
		setPartner(null);		//Do this before leaveHome to ensure partner doesn't leave home with this person!
		setDcpyy(null); //Set number of years in partnership to null if leaving partner
		setDcpex(Indicator.True); //Set variable indicating leaving the partnership to true
		setLeftPartnership(true); //Set to true if leaves partnership to use with fertility regression, this is never reset
		
		setupNewBenefitUnit(true);
		setupNewHousehold(true);
	
	}

	protected Household setupNewHousehold(boolean automaticUpdateOfHouseholds) {

		Household newHousehold = new Household(); //Set up a new empty household

		if (automaticUpdateOfHouseholds) {
			model.getHouseholds().add(newHousehold);

			//Remove benefitUnit from old household (if exists)
			if (benefitUnit.getHousehold() != null) {
				benefitUnit.getHousehold().removeBenefitUnitFromHousehold(benefitUnit);
			}

		}

		newHousehold.addBenefitUnitToHousehold(this.benefitUnit); //Add benefit unit of the person moving out to the new household
		id_household = newHousehold.getId();

		newHousehold.initializeFields();

		return newHousehold;
	}


	/**
	 * 
	 * @param automaticUpdateOfBenefitUnits - This is a toggle to control the automatic update of the model's set of benefitUnits,
	 *   which would cause concurrent modification exception to be thrown if called within an iteration through benefitUnits.
	 *   
	 *   There are two possible causes of concurrent modification issues: 1) the adding of the new benefitUnit to the model's
	 *   benefitUnits set and 2) the removal of the last person from the benefitUnit when updateHousehold() method is called would
	 *   lead to the automatic removal of the old house from the model's benefitUnits set.
	 *   
	 *   To prevent concurrent modification 
	 *   exception being thrown, set this parameter to false and use the iterator on the benefitUnits to manually add the new
	 *   benefitUnit e.g. (houseIterator.add(newHouse)).  Also the updateHousehold() method will need to be called and the person
	 *   removed from the old house manually outside the iteration through benefitUnits.
	 * @return
	 */
	protected BenefitUnit setupNewBenefitUnit(boolean automaticUpdateOfBenefitUnits) {		//Returns a reference to the newly created BenefitUnit object
		
		BenefitUnit newBU;
		
		//Create new benefitUnit
		if(partner != null) {
			//Note, partner will become responsible adult, even if their age < age to move out of home if they are already living with their partner
			newBU = new BenefitUnit(this, partner);		//Automatically makes this person and their partner the responsible adults in the new house

			//Multigeneration families are not allowed - setting up a new benefit unit with partner requires setting up new household too
			LinkedHashSet<BenefitUnit> benefitUnitsToAddSet = new LinkedHashSet<>();
			benefitUnitsToAddSet.add(newBU);
			Household newHousehold = new Household(benefitUnitsToAddSet);
			model.getHouseholds().add(newHousehold);
		}
		else {
			newBU = new BenefitUnit(this);
			benefitUnit.getHousehold().addBenefitUnitToHousehold(newBU); // Add the newly created BU to the household
		}


		
		Set<Person> childrenToRemoveFromHousehold = new LinkedHashSet<Person>();
		Set<Person> childrenToRemoveFromPartnerHousehold = new LinkedHashSet<Person>();
		
		//Move children.  By default they will follow the mother, but if no mother, they will follow the father
		if(dgn.equals(Gender.Female)) {
			for(Person child: benefitUnit.getChildren()) {
				if(child.getId_mother() != null) {
					if(child.getId_mother().equals(key.getId())) {		//Child could be a sibling, or even this person, because of the way people are originally distributed amongst benefitUnits, preserving the original input data structure!  Need this to ensure only genuine children of this person follow them to a new benefitUnit.
						childrenToRemoveFromHousehold.add(child);		//Concurrent modification exception means that we have to remove children outside of loop over children
						if(child.getId_father() != null && partner != null && !child.getId_father().equals(partner.getKey().getId()) ) {
							child.id_father = null;
//							child.setFatherId(null);				//Shows that child is no longer living with natural father. 
//							child.setFatherId(partner.getKey().getId());	//XXX: Or should we set to partner's id, i.e. step-dad?
						}
					}
					else {
						for(Person teenageMother: benefitUnit.getChildren()) {		//If mother of child is actually one of the children themselves (in the case of a teenage mother living with parents, we also need to move this child)
							if(child.getId_mother().equals(teenageMother.getKey().getId())) {
								childrenToRemoveFromHousehold.add(child);
							}
						}
					}
				}
			}
			if(partner != null) {		//Partner will be male
				BenefitUnit partnerBenefitUnit = partner.getBenefitUnit();
				Set<Person> partnerHouseholdChildren = partnerBenefitUnit.getChildren();
				log.debug("person " + this.getKey().getId() + ", old benefitUnit " + benefitUnit.getKey().getId() + ", new benefitUnit " + newBU.getKey().getId() + ", partner " + partner.getKey().getId() + ", partner benefitUnit " + partner.getId_benefitUnit() + " = " + partner.getBenefitUnit().getKey().getId() + ", partnerHouseholdChildren " + partnerHouseholdChildren);
//				if(partnerHouseholdChildren != null) {
					for(Person child: partnerHouseholdChildren) {
						if(child.getId_mother() == null) {		//Only move if they don't have a mother
							if(child.getId_father().equals(partner.getKey().getId())) {
								childrenToRemoveFromPartnerHousehold.add(child);		//Concurrent modification exception means that we have to remove children outside of loop over children
							}
						}	
					}
//				}
			}
		}
		else {		//Case where this person is male, so potentially the father
			
			/*
			for(Person child: benefitUnit.getChildren()) {
				if(benefitUnit.getFemaleId() != null && child.getKey().getId() == benefitUnit.getFemaleId() || benefitUnit.getMaleId() != null && child.getKey().getId() == benefitUnit.getMaleId()) {
					benefitUnit.getChildren().remove(child); //Concurrent modification exception thrown
				}
			}
			*/
			
				
			for(Person child: benefitUnit.getChildren()) {
				if(child.getId_mother() == null && child.getId_father() != null) {			//Only move if they don't have a mother
					if(child.getId_father().equals(key.getId())) {
						childrenToRemoveFromHousehold.add(child);		//Concurrent modification exception means that we have to remove children outside of loop over children
					}
				}
			}
			
			if(partner != null) {		//Partner will be female, so their children will follow her to the new home
				BenefitUnit partnerBenefitUnit = partner.getBenefitUnit();
				for(Person child: partnerBenefitUnit.getChildren()) {
					if(child.getId_mother() != null) {
						if(child.getId_mother().equals(partner.getKey().getId())) {		//Child could be a sibling, or even this person, because of the way people are originally distributed amongst benefitUnits, preserving the original input data structure!  Need this to ensure only genuine children of this person follow them to a new benefitUnit.
							childrenToRemoveFromPartnerHousehold.add(child);		//Concurrent modification exception means that we have to remove children outside of loop over children
							if(child.getId_father() != null && !child.getId_father().equals(key.getId()) ) {
								child.id_father = null;
//								child.setFatherId(null);				//Shows that child is no longer living with natural father
//								child.setFatherId(partner.getKey().getId());	//XXX: Or should we set to partner's id, i.e. step-dad?
							}
						}
						else {
							for(Person teenageMother: partnerBenefitUnit.getChildren()) {		//If mother of child is actually one of the partner's children themselves (in the case of a teenage mother living with parents, we also need to move this child)
								if(child.getId_mother().equals(teenageMother.getKey().getId())) {
									childrenToRemoveFromPartnerHousehold.add(child);
								}
							}
						}
					}
				}
			}
		}
		
		//Now remove children from benefitUnit or partner benefitUnit, ensuring no duplicate children removed / added
		if(partner == null) {
			for(Person child: childrenToRemoveFromHousehold) {
				benefitUnit.removePerson(child);
				newBU.addChild(child);
			}
		}
		else {
			if(partner.getBenefitUnit() == this.benefitUnit) {		//If already living in the same benefitUnit, take the union of both sets of children
				childrenToRemoveFromHousehold.addAll(childrenToRemoveFromPartnerHousehold);
				for(Person child: childrenToRemoveFromHousehold) {
					benefitUnit.removePerson(child);
					newBU.addChild(child);
				}
			}
			else {			//Partner must have been living in a different benefitUnit
				for(Person child: childrenToRemoveFromHousehold) {
					benefitUnit.removePerson(child);
					newBU.addChild(child);
				}
				for(Person child: childrenToRemoveFromPartnerHousehold) {
					partner.getBenefitUnit().removePerson(child);
					newBU.addChild(child);
				}
			}
			
		}
				
		//Automatic update of collections if required
		//Removing children (above) from the benefitUnit should not lead to the removal of the benefitUnit (due to becoming an empty benefitUnit) because there should still be the responsible male or female (this person or partner) in the benefitUnit, which we shall now remove from their benefitUnit below
		if(automaticUpdateOfBenefitUnits) {	//This is a toggle to prevent automatic update to the model's set of benefitUnits, which would cause concurrent modification exception to be thrown if called within an iteration through benefitUnits.  In this case, set parameter to false and use the iterator on the benefitUnits to add manually e.g. (houseIterator.add(newBU)).
			model.getBenefitUnits().add(newBU);	//This will cause concurrent modification if setupNewHome is called in an iteration through benefitUnits
			
			//Need to remove persons from old benefitUnit first before resetting benefitUnit
			benefitUnit.removePerson(this);			//This will cause concurrent modification if by removing the person from the previous benefitUnit, the benefitUnit becomes empty and is removed from the model.  Need to call this manually outside the iteration through benefitUnits.
			if(partner != null) {
				//This will cause concurrent modification if by removing the person from the previous benefitUnit, the benefitUnit becomes empty and is removed from the model.  Need to call this manually outside the iteration through benefitUnits.
				partner.getBenefitUnit().removePerson(partner);		//This ensures that if the partner had been living in a different house, it is removed from the correct house!
			}
		}
		
		//Reset benefitUnit and set parents Ids to zero as no longer live with parents
		id_mother = null;
		id_father = null;
		setBenefitUnit(newBU);				//Need to do this after removing from previous house to ensure proper removal.
		if(partner != null) {
			partner.id_mother = null;
			partner.id_father = null;
			partner.setBenefitUnit(newBU);		//Need to do this after removing from previous house to ensure proper removal.
		}
		else {
			household_status = Household_status.Single;		//If leaving parents or partner, reset to Single.
		}
		for(Person child: newBU.getChildren()) {
			child.setBenefitUnit(newBU);
		}
		
		newBU.initializeFields();
		newBU.setRegion(benefitUnit.getRegion());
		return newBU;
	}
	
	 	@Override
	    public int compareTo(Person o) {
	    	Person p = (Person) o;
	    	return (int) (this.getId_benefitUnit() - p.getId_benefitUnit());
	    }

	/*
	This method takes les_c4 (which is a more aggregated version of labour force activity statuses) and returns les_c6 version.
	Used when setting initial values of les_c6 from existing les_c4.
	*/
	public void initialise_les_c6_from_c4() {
		if (les_c4.equals(Les_c4.EmployedOrSelfEmployed)) {
			les_c7_covid = Les_c7_covid.Employee;
		} else if (les_c4.equals(Les_c4.NotEmployed)) {
			les_c7_covid = Les_c7_covid.NotEmployed;
		} else if (les_c4.equals(Les_c4.Student)) {
			les_c7_covid = Les_c7_covid.Student;
		} else if (les_c4.equals(Les_c4.Retired)) {
			les_c7_covid = Les_c7_covid.Retired;
		}

		//In the first period the lagged value will be equal to the contemporaneous value
		if (les_c7_covid_lag1 == null) {
			les_c7_covid_lag1 = les_c7_covid;
		}
	}


	//-----------------------------------------------------------------------------------
	// IIntSource implementation for the CrossSection.Integer objects in the collector
	//-----------------------------------------------------------------------------------
	
	public enum IntegerVariables {			//For cross section of Collector			
		isEmployed,
		isNotEmployed,
		isRetired,
		isStudent,
		isNotEmployedOrRetired,
		isToBePartnered,
	}
	
	public int getIntValue(Enum<?> variableID) {
		
		switch ((IntegerVariables) variableID) {
		
		case isEmployed:
			if (les_c4 == null) return 0;		//For inactive people, who don't participate in the labour market
			else if (les_c4.equals(Les_c4.EmployedOrSelfEmployed)) return 1;
			else return 0;		//For unemployed case

		case isNotEmployed:
			if (les_c4 == null) return 0;
			else if (les_c4.equals(Les_c4.NotEmployed)) return 1;
			else return 0;

		case isRetired:
			if (les_c4 == null) return 0;
			else if (les_c4.equals(Les_c4.Retired)) return 1;
			else return 0;

		case isStudent:
			if (les_c4 == null) return 0;
			else if (les_c4.equals(Les_c4.Student)) return 1;
			else return 0;

		case isNotEmployedOrRetired:
			if (les_c4 == null) return 0;
			else if (les_c4.equals(Les_c4.NotEmployed) || les_c4.equals(Les_c4.Retired)) return 1;
			else return 0;

		case isToBePartnered:
			return (isToBePartnered())? 1 : 0;

		default:
			throw new IllegalArgumentException("Unsupported variable " + variableID.name() + " in Person#getIntValue");
		}
	}
	
	
	// ---------------------------------------------------------------------
	// implements IDoubleSource for use with Regression classes
	// ---------------------------------------------------------------------	
	
	public enum DoublesVariables {
		
		//Variables:
		GrossEarningsYearly,
		GrossLabourIncomeMonthly,
		
		//Regressors:
		Age,
		AgeSquared,
		AgeCubed,
		LnAge,
		Age23to25,						// Indicator for whether the person is in the Age category (see below for definition)
		Age26to30,						// Indicator for whether the person is in the Age category (see below for definition)
		Age21to27,						// Indicator for whether the person is in the Age category (see below for definition)
		Age28to30,						// Indicator for whether the person is in the Age category (see below for definition)
		ChildcareSpendingRegional,
		ChildcareTotal,
		Cohort,
		Constant, 						// For the constant (intercept) term of the regression
		D_children_2under,				// Indicator (dummy variables for presence of children of certain ages in the benefitUnit)
		D_children_3_6,
		D_children_7_12,
		D_children_13_17,
		D_children_18over,				//Currently this will return 0 (false) as children leave home when they are 18
		Dnc_L1, 						//Lag(1) of number of children of all ages in the benefitUnit
		Dnc02_L1, 						//Lag(1) of number of children aged 0-2 in the benefitUnit
		Dnc017, 						//Number of children aged 0-17 in the benefitUnit
		Dcrisis,
		Dgn,							//Gender: returns 1 if male
		Dag,
		Dag_sq,
		Dcpyy_L1, 						//Lag(1) number of years in partnership 
		Dcpagdf_L1, 					//Lag(1) of age difference between partners
		Dcpst_Single,					//Single never married
		Dcpst_Partnered,				//Partnered
		Dcpst_PreviouslyPartnered,		//Previously partnered
		Dcpst_Single_L1, 				//Lag(1) of partnership status is Single
		Dcpst_PreviouslyPartnered_L1,   //Lag(1) of partnership status is previously partnered
		Dhe,							//Health status
		Dhe_L1, 						//Health status lag(1)
		Dhm,							//Mental health status
		Dhm_L1,							//Mental health status lag(1)
		Dhesp_L1, 						//Lag(1) of partner's health status 
		Deh_c3_High,
		Deh_c3_Medium,
		Deh_c3_Medium_L1, 				//Education level lag(1) equals medium
		Deh_c3_Low,
		Deh_c3_Low_L1,					//Education level lag(1) equals low
		Dehm_c3_High,					//Mother's education == High indicator
		Dehm_c3_Medium,					//Mother's education == Medium indicator
		Dehm_c3_Low,					//Mother's education == Low indicator
		Dehf_c3_High,					//Father's education == High indicator
		Dehf_c3_Medium,					//Father's education == Medium indicator
		Dehf_c3_Low,					//Father's education == Low indicator
		Dehsp_c3_Medium_L1,				//Partner's education == Medium at lag(1)
		Dehsp_c3_Low_L1,				//Partner's education == Low at lag(1)
		Dhhtp_c4_CoupleChildren_L1,
		Dhhtp_c4_CoupleNoChildren_L1,
		Dhhtp_c4_SingleNoChildren_L1,
		Dhhtp_c4_SingleChildren_L1,
		Dlltsd,							//Long-term sick or disabled
		Dlltsd_L1,						//Long-term sick or disabled lag(1)
		FertilityRate,
		Female,
		InverseMillsRatio,
		Ld_children_3under,
		Ld_children_4_12,
		Lemployed,
		Lnonwork,
		Lstudent,
		Lunion,
		Les_c3_Student_L1,
		Les_c3_NotEmployed_L1,
		Les_c3_Employed_L1,
		Les_c3_Sick_L1,					//This is based on dlltsd
		Lessp_c3_Student_L1,			//Partner variables
		Lessp_c3_NotEmployed_L1,
		Lessp_c3_Sick_L1,
		Retired,
		Lesdf_c4_EmployedSpouseNotEmployed_L1, 					//Own and partner's activity status lag(1)
		Lesdf_c4_NotEmployedSpouseEmployed_L1,
		Lesdf_c4_BothNotEmployed_L1, 
		Liwwh,									//Work history in months
		NumberChildren,
		NumberChildren_2under,
		OnleaveBenefits,
		OtherIncome,
		Parents,
		PartTimeRate,
		PartTime_AND_Ld_children_3under,			//Interaction term conditional on if the person had a child under 3 at the previous time-step
		ResStanDev,
		Single,
		Single_kids,
		Sfr, 										//Scenario : fertility rate This retrieves the fertility rate by region and year to use in fertility regression
		Union,
		Union_kids,
		Year,										//Year as in the simulation, e.g. 2009
		Year_transformed,							//Year - 2000
		Year_transformed_monetary,					//Year-2000 that stops in 2017, for use with monetary processes
		Ydses_c5_Q2_L1, 							//HH Income Lag(1) 2nd Quantile 
		Ydses_c5_Q3_L1,								//HH Income Lag(1) 3rd Quantile
		Ydses_c5_Q4_L1,								//HH Income Lag(1) 4th Quantile
		Ydses_c5_Q5_L1,								//HH Income Lag(1) 5th Quantile
		Ypnbihs_dv_L1,								//Gross personal non-benefit income lag(1)
		Ypnbihs_dv_L1_sq,							//Square of gross personal non-benefit income lag(1)
		Ynbcpdf_dv_L1, 								//Lag(1) of difference between own and partner's gross personal non-benefit income
		Yptciihs_dv_L1,								//Lag(1) of gross personal non-employment non-benefit income
		Yptciihs_dv_L2,								//Lag(2) of gross personal non-employment non-benefit income
		Yptciihs_dv_L3,								//Lag(3) of gross personal non-employment non-benefit income
		Ypncp_L1,									//Lag(1) of capital income
		Ypncp_L2,									//Lag(2) of capital income
		Ypnoab_L1,									//Lag(1) of pension income
		Ypnoab_L2,									//Lag(2) of pension income
		Yplgrs_dv_L1,								//Lag(1) of gross personal employment income
		Yplgrs_dv_L2,								//Lag(2) of gross personal employment income
		Yplgrs_dv_L3,								//Lag(3) of gross personal employment income
		Reached_Retirement_Age,						//Indicator whether individual is at or above retirement age
		Reached_Retirement_Age_Sp,					//Indicator whether spouse is at or above retirement age
		Reached_Retirement_Age_Les_c3_NotEmployed_L1, //Interaction term for being at or above retirement age and not employed in the previous year
		EquivalisedIncomeYearly, 							//Equivalised income for use with the security index
		EquivalisedConsumptionYearly,
		sIndex,
		sIndexNormalised,

		//New enums for the mental health Step 1 and 2:
		EmployedToUnemployed,
		UnemployedToEmployed,
		PersistentUnemployed,
		NonPovertyToPoverty,
		PovertyToNonPoverty,
		PersistentPoverty,
		RealIncomeChange, //Note: the above return a 0 or 1 value, but income variables will return the change in income or 0
		RealIncomeDecrease_D,
		D_Econ_benefits,
		D_Home_owner,

		//New enums to handle the covariance matrices
		Ld_children_3underIT,
		Ld_children_4_12IT,
		LactiveIT,
		EduHighIT,
		LunionIT,
		EduMediumIT,

		ITC,			//Italy
		ITF,
		ITG,
		ITH,
		ITI,

		UKC,				//UK
		UKD,
		UKE,
		UKF,
		UKG,
		UKH,
		UKI,
		UKJ,
		UKK,
		UKL,
		UKM,
		UKN,
		UKmissing,

		// Covid-19 labour market module regressors below:
		Dgn_Dag,
		Employmentsonfullfurlough,
		Employmentsonflexiblefurlough,
		CovidTransitionsMonth,
		Lhw_L1,
		Dgn_Lhw_L1,
		Covid19GrossPayMonthly_L1,
		Covid19ReceivesSEISS_L1,
		Les_c7_Covid_Furlough_L1,
		Blpay_Q2,
		Blpay_Q3,
		Blpay_Q4,
		Blpay_Q5,
		Dgn_baseline,
		}
	
	public double getDoubleValue(Enum<?> variableID) {
		
		switch ((DoublesVariables) variableID) {
		
		case GrossEarningsYearly:
			return getGrossEarningsYearly();
		case GrossLabourIncomeMonthly:
			return getCovidModuleGrossLabourIncome_Baseline();
		//Regressors
		case Age:
//			log.debug("age");
			return (double) dag;
		case Dag:
			return (double) dag;
		case Dag_sq:
			return (double) dag*dag;
		case AgeSquared:
//			log.debug("age sq");
			return (double) dag * dag;
		case AgeCubed:
//			log.debug("age cub");
			return (double) dag * dag * dag;
		case LnAge:
			return Math.log(dag);
		case Age23to25:
//			log.debug("age 23 to 25");
			if(dag >= 23 && dag <= 25) {
				 return 1.;
			 }
			 else return 0.;
		case Age26to30:
//			log.debug("age 26 to 30");
			 if(dag >= 26 && dag <= 30) {
				 return 1.;
			 }
			 else return 0.;
		case Age21to27: 
//			log.debug("age 21 to 27");
			 if(dag >= 21 && dag <= 27) {
				 return 1.;
			 }
			 else return 0.;
		case Age28to30: 
//			log.debug("age 28 to 30");
			 if(dag >= 28 && dag <= 30) {
				 return 1.;
			 }
			 else return 0.;
		case Constant:	
//			log.debug("constant");
			return 1.;
		case Dcpyy_L1:
			if(dcpyy_lag1 != null) {
				return (double) dcpyy_lag1;
			}
			else return 0.; 
		case Dcpagdf_L1:
			if(dcpagdf_lag1 != null) {
				return (double) dcpagdf_lag1;
			}
			else return 0.;
		case Dcpst_Single:
			if(dcpst != null) {
				return dcpst.equals(Dcpst.SingleNeverMarried)? 1. : 0.;
			} else return 0.;
		case Dcpst_Partnered:
			if(dcpst != null) {
				return dcpst.equals(Dcpst.Partnered)? 1. : 0.;
			} else return 0.;
		case Dcpst_PreviouslyPartnered:
			if(dcpst != null) {
				return dcpst.equals(Dcpst.PreviouslyPartnered)? 1. : 0.;
			} else return 0.;
		case Dcpst_Single_L1: 	
			if(dcpst_lag1 != null) {
				return dcpst_lag1.equals(Dcpst.SingleNeverMarried)? 1. : 0.;
			}
			else return 0.;
		case Dcpst_PreviouslyPartnered_L1:
			if(dcpst_lag1 != null) {
				return dcpst_lag1.equals(Dcpst.PreviouslyPartnered)? 1. : 0.;
			}
			else return 0.;
		case D_children_2under:				// Indicator (dummy variables for presence of children of certain ages in the benefitUnit)
			if (benefitUnit.getD_children_2under() != null) {
				return (double) benefitUnit.getD_children_2under().ordinal();
			} else return 0.;
		case D_children_3_6:
			if (benefitUnit.getD_children_3_6() != null) {
				return (double) benefitUnit.getD_children_3_6().ordinal();
			}
			else return 0.;
		case D_children_7_12:
			if (benefitUnit.getD_children_7_12() != null) {
				return (double) benefitUnit.getD_children_7_12().ordinal();
			}
			else return 0.;
		case D_children_13_17:
			if (benefitUnit.getD_children_13_17() != null) {
				return (double) benefitUnit.getD_children_13_17().ordinal();
			}
			else return 0.;
		case D_children_18over:
			if (benefitUnit.getD_children_18over() != null) {
				return (double) benefitUnit.getD_children_18over().ordinal();	//Currently this will always return 0 (false) as children leave home when they are 18 years old
			}
			else return 0.;
		case Dnc_L1:
			if (benefitUnit.getN_children_allAges_lag1() != null) {
				return (double) benefitUnit.getN_children_allAges_lag1();
			}
			else return 0.;
		case Dnc02_L1:
			if (benefitUnit.getN_children_02_lag1() != null) {
				return (double) benefitUnit.getN_children_02_lag1();
			}
			else return 0.;
		case Dnc017:
			return (double) benefitUnit.getN_children_017();
		case Dgn: 
			if(dgn.equals(Gender.Male)) {
				return 1.;
			} 
			else return 0.;
		case Dhe:
			return (double) dhe;
		case Dhe_L1:
			return (double) dhe_lag1;
		case Dhm:
			return (double) dhm;
		case Dhm_L1:
			if (dhm_lag1 != null && dhm_lag1 >= 0.) {
				return (double) dhm_lag1;
			}
			else return 0.;
		case Dhesp_L1:
			if(dhesp_lag1 != null) {
				return (double) dhesp_lag1; //Lag(1) of partner's health status
			}
			else return 0.;
		case Deh_c3_High:
//			log.debug("edu high");
			if(deh_c3 != null) {
				return deh_c3.equals(Education.High)? 1. : 0.;			//Returns 1 if true (education level is High), 0 otherwise
			}
			else return 0.;
		case Deh_c3_Medium:
//			log.debug("edu medium");
			if(deh_c3 != null) {
				return deh_c3.equals(Education.Medium)? 1. : 0.;			//Returns 1 if true (education level is Medium), 0 otherwise
			}
			else return 0.;
		case Deh_c3_Medium_L1:
			if(deh_c3_lag1 != null) {
				return deh_c3_lag1.equals(Education.Medium)? 1. : 0.; 		//Return 1 if lag(1) of education is medium
			}
			else return 0.;
		case Deh_c3_Low:
//			log.debug("edu medium");
			if(deh_c3 != null) {
				return deh_c3.equals(Education.Low)? 1. : 0.;			//Returns 1 if true (education level is Low), 0 otherwise
			}
			else return 0.;
		case Deh_c3_Low_L1:
			if(deh_c3_lag1 != null) {
				return deh_c3_lag1.equals(Education.Low)? 1. : 0.;		//Return 1 if lag(1) of education is low
			}
			else return 0.;
		case Dehm_c3_High:					//Mother's education == High indicator
			if(dehm_c3 != null) {
				return dehm_c3.equals(Education.High)? 1. : 0.;
			}
			else return 0.;
		case Dehm_c3_Medium:				//Mother's education == Medium indicator
			if(dehm_c3 != null) {
				return dehm_c3.equals(Education.Medium)? 1. : 0.;
			}
			else return 0.;
		case Dehm_c3_Low:					//Mother's education == Low indicator
			if(dehm_c3 != null) {
				return dehm_c3.equals(Education.Low)? 1. : 0.;
			}
			else return 0.;
		case Dehf_c3_High:					//Father's education == High indicator
			if(dehf_c3 != null) {
				return dehf_c3.equals(Education.High)? 1. : 0.;
			}
			else return 0.;
		case Dehf_c3_Medium:				//Father's education == Medium indicator
			if(dehf_c3 != null) {
				return dehf_c3.equals(Education.Medium)? 1. : 0.;
			}
			else return 0.;
		case Dehf_c3_Low:					//Father's education == Low indicator
			if(dehf_c3 != null) {
				return dehf_c3.equals(Education.Low)? 1. : 0.;
			}
			else return 0.;
		case Dehsp_c3_Medium_L1:
			if(dehsp_c3_lag1 != null) {
				return dehsp_c3_lag1.equals(Education.Medium)? 1. : 0.;
			}
			else return 0.;
		case Dehsp_c3_Low_L1:
			if(dehsp_c3_lag1 != null) {
				return dehsp_c3_lag1.equals(Education.Low)? 1. : 0.;
			}
			else return 0.;
		case Dhhtp_c4_CoupleChildren_L1:
			if (getBenefitUnit().getDhhtp_c4_lag1() != null) {
				return getBenefitUnit().getDhhtp_c4_lag1().equals(Dhhtp_c4.CoupleChildren)? 1. : 0.;
			}
			else return 0.;
		case Dhhtp_c4_CoupleNoChildren_L1:
			if (getBenefitUnit().getDhhtp_c4_lag1() != null) {
				return getBenefitUnit().getDhhtp_c4_lag1().equals(Dhhtp_c4.CoupleNoChildren)? 1. : 0.;
			}
			else return 0.;
		case Dhhtp_c4_SingleNoChildren_L1:
			if (getBenefitUnit().getDhhtp_c4_lag1() != null) {
				return getBenefitUnit().getDhhtp_c4_lag1().equals(Dhhtp_c4.SingleNoChildren)? 1. : 0.;
			}
			else return 0.;
		case Dhhtp_c4_SingleChildren_L1:
			if (getBenefitUnit().getDhhtp_c4_lag1() != null) {
				return getBenefitUnit().getDhhtp_c4_lag1().equals(Dhhtp_c4.SingleChildren)? 1. : 0.;
			}
			else return 0.;
		case Dlltsd:
			if(dlltsd != null) {
			return dlltsd.equals(Indicator.True)? 1. : 0.;
			}
			else return 0.;
		case Dlltsd_L1:
			if(dlltsd_lag1 != null) {
			return dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
			}
			else return 0.;
		case FertilityRate:
//			log.debug("fertility");
			if ( ioFlag ) {
				if (model.getYear() < labsim.model.decisions.Parameters.FERTILITY_MAX_YEAR) {
					return ((Number) Parameters.getFertilityProjectionsByYear().getValue("Value", model.getYear())).doubleValue();
				} else {
					return ((Number) Parameters.getFertilityProjectionsByYear().getValue("Value", labsim.model.decisions.Parameters.FERTILITY_MAX_YEAR)).doubleValue();
				}
			} else {
				return 1000*((Number)Parameters.getFertilityRateByRegionYear().get(this.getRegion(), model.getYear())).doubleValue(); //We calculate the rate per woman, but the standard to report (and what is used in the estimates) is per 1000 hence multiplication
			}
		case Female:
//			log.debug("female");
			return dgn.equals(Gender.Female)? 1. : 0.;			
		case InverseMillsRatio:
			double score;
			if(dgn.equals(Gender.Male)) {
				score = Parameters.getRegEmploymentSelectionMale().getScore(this, Person.DoublesVariables.class);
//				scoreMale  += score;
//				countMale++;
			}
			else {		//For females
				score = Parameters.getRegEmploymentSelectionFemale().getScore(this, Person.DoublesVariables.class);
//				scoreFemale += score;
//				countFemale++;
			}
			//Use BigDecimal to handle small number divisions (to avoid divide by zero numerical errors).
			//XXX: Probably don't benefit from use BigDecimal, so perhaps change back to double if to improve efficiency
			double inverseMillsRatio; //IMR is the PDF(x) / CDF(x) 
			double denom = Parameters.getStandardNormalDistribution().cumulativeProbability(score);
			if(denom != 0.) {
				String numString = Double.toString(Parameters.getStandardNormalDistribution().density(score));
				String denomString = Double.toString(denom);
				BigDecimal bigNum = new BigDecimal(numString);
				BigDecimal bigDenom = new BigDecimal(denomString);
				BigDecimal result = bigNum.divide(bigDenom, RoundingMode.HALF_EVEN);
				inverseMillsRatio = result.doubleValue();
				log.debug("big num " + bigNum + ", big denom " + bigDenom + " result " + result);
				countOK++;
				if(dag > 32) {
					countOKover32++;
				}
			}
			else {
				inverseMillsRatio = Double.NaN;		//Divide by zero error.  Will be Infinity if numerator is non-zero, or NaN if numerator is zero.  We just set to NaN here for simplicity / efficiency
				countNaN++;
			}
//			//Use logs to handle small number divide by zero numerical errors.
//			double logNum = Math.log(Parameters.getStandardNormalDistribution().density(score));
//			double logDenom = Math.log(Parameters.getStandardNormalDistribution().cumulativeProbability(score));
//			double inverseMillsRatio = Math.exp(logNum - logDenom);
			if(Double.isFinite(inverseMillsRatio)) {
				if(dgn.equals(Gender.Male)) {
					if(inverseMillsRatio > inverseMillsRatioMaxMale) {
						inverseMillsRatioMaxMale = inverseMillsRatio;
					}
					else if(inverseMillsRatio < inverseMillsRatioMinMale) {
						inverseMillsRatioMinMale = inverseMillsRatio;
					}
				}
				else {
					if(inverseMillsRatio > inverseMillsRatioMaxFemale) {
						inverseMillsRatioMaxFemale = inverseMillsRatio;
					}
					else if(inverseMillsRatio < inverseMillsRatioMinFemale) {
						inverseMillsRatioMinFemale = inverseMillsRatio;
					}					
				}
			}
			else {		//Inverse Mills Ratio is not finite!
				log.debug("inverse Mills ratio is not finite, return 0 instead!!!   IMR: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + dag + ", gender: " + dgn + ", education " + deh_c3 + ", activity_status from previous time-step " + les_c4);
				return 0.;
			}
//			if(model.debugCommentsOn) {
				log.debug("inverse Mills ratio: " + inverseMillsRatio + ", score: " + score/* + ", num: " + num + ", denom: " + denom*/ + ", age: " + dag + ", gender: " + dgn + ", education " + deh_c3 + ", activity_status from previous time-step " + les_c4);
//			}
			return inverseMillsRatio;		//XXX: Currently only returning non-zero IMR if it is finite	
		case Ld_children_3under:
//			log.debug("Ld child 3 under");
			return benefitUnit.getD_children_3under_lag().ordinal();
		case Ld_children_4_12:
//			log.debug("Ld child 4-12");
			return benefitUnit.getD_children_4_12_lag().ordinal();
		case Lemployed:
//			log.debug("Lemployed");
			if(les_c4_lag1 != null) {		//Problem will null pointer exceptions for those who are inactive and then become active as their lagged employment status is null!
				return les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)? 1. : 0.;
			} else {
				return 0.;			//A person who was not active but has become active in this year should have an employment_status_lag == null.  In this case, we assume this means 0 for the Employment regression, where Lemployed is used.
			}
		case Lnonwork:
			return (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired))? 1.: 0.;
		case Lstudent:
//			log.debug("Lstudent");
			return les_c4_lag1.equals(Les_c4.Student)? 1. : 0.;
		case Lunion:
//			log.debug("Lunion");
			return household_status_lag.equals(Household_status.Couple)? 1. : 0.;
		case Les_c3_Student_L1:
			if (les_c4_lag1 !=null) {
				return les_c4_lag1.equals(Les_c4.Student)? 1. : 0.;
			} else { return 0.0; }
		case Les_c3_NotEmployed_L1:
			if (les_c4_lag1 !=null) {
				return (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired))? 1. : 0.;
			} else { return 0.0; }
		case Les_c3_Employed_L1:
			if (les_c4_lag1 != null) {
				return les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed)? 1. : 0.;
		} else { return 0.0;}
		case Les_c3_Sick_L1:
			if (dlltsd_lag1 != null) {
				return dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
			}
			else {return 0.0; }
		case Lessp_c3_Student_L1:
			if (partner != null && partner.les_c4_lag1 != null) {
				return partner.les_c4_lag1.equals(Les_c4.Student)? 1. : 0.;
			} else { return 0.;}
		case Lessp_c3_NotEmployed_L1:
			if (partner != null && partner.les_c4_lag1 != null) {
				return (partner.les_c4_lag1.equals(Les_c4.NotEmployed) || partner.les_c4_lag1.equals(Les_c4.Retired))? 1. : 0.;
			} else { return 0.;}
		case Lessp_c3_Sick_L1:
			if (partner != null && partner.dlltsd_lag1 != null) {
				return partner.dlltsd_lag1.equals(Indicator.True)? 1. : 0.;
			} else { return 0.;}
		case Retired:
			if (les_c4 != null) {
				return les_c4.equals(Les_c4.Retired)? 1. : 0.;
			} else return 0.;
		case Lesdf_c4_EmployedSpouseNotEmployed_L1: 					//Own and partner's activity status lag(1)
			if(lesdf_c4_lag1 != null) {
				return lesdf_c4_lag1.equals(Lesdf_c4.EmployedSpouseNotEmployed)? 1. : 0.;
			} else {
				return 0.;
			}
		case Lesdf_c4_NotEmployedSpouseEmployed_L1:
			if(lesdf_c4_lag1 != null) {
				return lesdf_c4_lag1.equals(Lesdf_c4.NotEmployedSpouseEmployed)? 1. : 0.;
			} else {
				return 0.;
			}
		case Lesdf_c4_BothNotEmployed_L1: 
			if(lesdf_c4_lag1 != null) {
				return lesdf_c4_lag1.equals(Lesdf_c4.BothNotEmployed)? 1. : 0.;
			} else {
				return 0.;
			}
		case Liwwh:
			return (double) liwwh; 
		case NumberChildren:
			return (double) benefitUnit.getChildren().size();
		case NumberChildren_2under:
			int count = benefitUnit.getN_children_0() + benefitUnit.getN_children_1() + benefitUnit.getN_children_2();
			return count;
		case OtherIncome:			// "Other income corresponds to other benefitUnit incomes divided by 10,000." (From Bargain et al. (2014).  From employment selection equation.
			return 0.;				// Other incomes "correspond to partner's and other family members' income as well as capital income of various sources."
		case Parents:
			return household_status.equals(Household_status.Parents)? 1. : 0.;
        case ResStanDev:        //Draw from standard normal distribution will be multiplied by the value in the .xls file, which represents the standard deviation
            //If model.addRegressionStochasticComponent set to true, return a draw from standard normal distribution, if false return 0.
            return getRandomGaussianIfStochasticComponentOn();
		case Single:
			return household_status.equals(Household_status.Single)? 1. : 0.;
		case Single_kids:		//TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
			if(household_status.equals(Household_status.Single)) {
				if(benefitUnit.getChildren().size()>0) {			//XXX: Perhaps we need to check that children is not null first, if children is only initialised when new born persons are added to the children set.
					return 1;
				}
				else return 0;
			}
			else return 0;
		case Union:
			return household_status.equals(Household_status.Couple)? 1. : 0.;
		case Union_kids:		//TODO: Is this sufficient, or do we need to take children aged over 12 into account as well?
			if(household_status.equals(Household_status.Couple)) {
				if(benefitUnit.getChildren().size()>0) {			//XXX: Perhaps we need to check that children is not null first, if children is only initialised when new born persons are added to the children set.
					return 1;
				}
				else return 0;
			}
			else return 0;
		case Year:
			if (model.isFixTimeTrend()) {
				return (double) model.getTimeTrendStopsIn();
			}
			else {
				return (double) model.getYear();
			}
		case Year_transformed:
			if (model.isFixTimeTrend() && model.getYear() >= model.getTimeTrendStopsIn()) {
				return (double) model.getTimeTrendStopsIn() - 2000;
			}
			else {
				return (double) model.getYear() - 2000;
			}
		case Year_transformed_monetary:
			if (model.getYear() >= model.getTimeTrendStopsInMonetaryProcesses()) {
				return (double) model.getTimeTrendStopsInMonetaryProcesses() - 2000;
			}
			else {
				return (double) model.getYear() - 2000;
			}
		case Ydses_c5_Q2_L1:
			if(getBenefitUnit().getYdses_c5_lag1() != null && getBenefitUnit().getYdses_c5_lag1().equals(Ydses_c5.Q2)) {
				return 1;
			}
			else return 0;
		case Ydses_c5_Q3_L1:
			if(getBenefitUnit().getYdses_c5_lag1() != null && getBenefitUnit().getYdses_c5_lag1().equals(Ydses_c5.Q3)) {
				return 1;
			}
			else return 0;
		case Ydses_c5_Q4_L1:
			if(getBenefitUnit().getYdses_c5_lag1() != null && getBenefitUnit().getYdses_c5_lag1().equals(Ydses_c5.Q4)) {
				return 1;
			}
			else return 0;
		case Ydses_c5_Q5_L1:
			if(getBenefitUnit().getYdses_c5_lag1() != null && getBenefitUnit().getYdses_c5_lag1().equals(Ydses_c5.Q5)) {
				return 1;
			}
			else return 0;
		case Ypnbihs_dv_L1:
			if(ypnbihs_dv_lag1 != null) {
				return (double) ypnbihs_dv_lag1;
			} else return 0.; 
		case Ypnbihs_dv_L1_sq:
			if(ypnbihs_dv_lag1 != null) {
				return (double) ypnbihs_dv_lag1*ypnbihs_dv_lag1;
			} else return 0.;
		case Ynbcpdf_dv_L1:
			if(ynbcpdf_dv_lag1 != null) {
				return (double) ynbcpdf_dv_lag1;
			} else return 0.;
		case Yptciihs_dv_L1:
			return (double) yptciihs_dv_lag1;
		case Yptciihs_dv_L2:
			return (double) yptciihs_dv_lag2;
		case Yptciihs_dv_L3:
			return (double) yptciihs_dv_lag3;
		case Ypncp_L1:
			return (double) ypncp_lag1;
		case Ypncp_L2:
			return (double) ypncp_lag2;
		case Ypnoab_L1:
			return (double) ypnoab_lag1;
		case Ypnoab_L2:
			return (double) ypnoab_lag2;
		case Yplgrs_dv_L1:
			return (double) yplgrs_dv_lag1;
		case Yplgrs_dv_L2:
			return (double) yplgrs_dv_lag2;
		case Yplgrs_dv_L3:
			return (double) yplgrs_dv_lag3;
			//New enums to handle covariance matrices that are aggregated
		case Ld_children_3underIT:
			return model.getCountry().equals(Country.IT) ? benefitUnit.getD_children_3under_lag().ordinal() : 0.;
		case Ld_children_4_12IT:			
			return model.getCountry().equals(Country.IT) ? benefitUnit.getD_children_4_12_lag().ordinal() : 0.;
		case LunionIT:
			return (household_status_lag.equals(Household_status.Couple) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;
		case EduMediumIT:
			return (deh_c3.equals(Education.Medium) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;
		case EduHighIT:
			return (deh_c3.equals(Education.High) && (getRegion().toString().startsWith(Country.IT.toString())))? 1. : 0.;

		case Reached_Retirement_Age:
			int retirementAge;
			if (dgn.equals(Gender.Female)) {
				retirementAge = Parameters.getFixedRetireAge(model.getYear(), Gender.Female);
			} else {
				retirementAge = Parameters.getFixedRetireAge(model.getYear(), Gender.Male);
			}
			return (dag >= retirementAge)? 1. : 0.;
		case Reached_Retirement_Age_Sp:
			int retirementAgePartner;
			if (partner != null) {
				if (partner.dgn.equals(Gender.Female)) {
					retirementAgePartner = Parameters.getFixedRetireAge(model.getYear(), Gender.Female);
				} else {
					retirementAgePartner = Parameters.getFixedRetireAge(model.getYear(), Gender.Male);
				}
				return (partner.dag >= retirementAgePartner)? 1. : 0.;
			} else {return 0.;}
		case Reached_Retirement_Age_Les_c3_NotEmployed_L1: //Reached retirement age and was not employed in the previous year
			if (dgn.equals(Gender.Female)) {
				retirementAge = Parameters.getFixedRetireAge(model.getYear(), Gender.Female);
			} else {
				retirementAge = Parameters.getFixedRetireAge(model.getYear(), Gender.Male);
			}
			return ((dag >= retirementAge) && (les_c4_lag1.equals(Les_c4.NotEmployed) || les_c4_lag1.equals(Les_c4.Retired)))? 1. : 0.;
		case EquivalisedIncomeYearly:
			if (getBenefitUnit().getEquivalisedDisposableIncomeYearly() != null) {
				return getBenefitUnit().getEquivalisedDisposableIncomeYearly();
			}
			else return -9999.99;
		case EquivalisedConsumptionYearly:
			if (yearlyEquivalisedConsumption != null) {
				return yearlyEquivalisedConsumption;
			}
			else return -9999.99;
		case sIndex:
			return getsIndex();
		case sIndexNormalised:
			return getsIndexNormalised();

		//New enums for the mental health Step 1 and 2:
		case EmployedToUnemployed:
			return (les_c4_lag1.equals(Les_c4.EmployedOrSelfEmployed) && les_c4.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False))? 1. : 0.;
		case UnemployedToEmployed:
			return (les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd_lag1.equals(Indicator.False) && les_c4.equals(Les_c4.EmployedOrSelfEmployed))? 1. : 0.;
		case PersistentUnemployed:
			return (les_c4.equals(Les_c4.NotEmployed) && les_c4_lag1.equals(Les_c4.NotEmployed) && dlltsd.equals(Indicator.False) && dlltsd_lag1.equals(Indicator.False))? 1. : 0.;
		case NonPovertyToPoverty:
			if (benefitUnit.getAtRiskOfPoverty() != null && benefitUnit.getAtRiskOfPoverty_lag1() != null) {
				return (benefitUnit.getAtRiskOfPoverty_lag1() == 0 && benefitUnit.getAtRiskOfPoverty() == 1)? 1. : 0.;
			} else return 0.;
		case PovertyToNonPoverty:
			if (benefitUnit.getAtRiskOfPoverty() != null && benefitUnit.getAtRiskOfPoverty_lag1() != null) {
				return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 0)? 1. : 0.;
			} else return 0.;
		case PersistentPoverty:
			if (benefitUnit.getAtRiskOfPoverty() != null && benefitUnit.getAtRiskOfPoverty_lag1() != null) {
				return (benefitUnit.getAtRiskOfPoverty_lag1() == 1 && benefitUnit.getAtRiskOfPoverty() == 1)? 1. : 0.;
			} else return 0.;
		case RealIncomeChange:
			return (benefitUnit.getYearlyChangeInLogEDI());
		case RealIncomeDecrease_D:
			return (benefitUnit.isDecreaseInYearlyEquivalisedDisposableIncome())? 1. : 0.;
		case D_Econ_benefits:
			return isReceivesBenefitsFlag_L1()? 1. : 0.;
		case D_Home_owner:
			return getBenefitUnit().isDhh_owned()? 1. : 0.; // Evaluated at the level of a benefit unit. If required, can be changed to individual-level homeownership status.
		//Regional indicators (dummy variables)			
		case ITC:				//Italy
			return (getRegion().equals(Region.ITC)) ? 1. : 0.;
		case ITF:
			return (getRegion().equals(Region.ITF)) ? 1. : 0.;
		case ITG:
			return (getRegion().equals(Region.ITG)) ? 1. : 0.;
		case ITH:
			return (getRegion().equals(Region.ITH)) ? 1. : 0.;
		case ITI:
			return (getRegion().equals(Region.ITI)) ? 1. : 0.;

		//UK
		case UKC:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKC)) ? 1. : 0.;
			}
			else return 0.;
		case UKD:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKD)) ? 1. : 0.;
			}
			else return 0.;
		case UKE:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKE)) ? 1. : 0.;
			}
			else return 0.;
		case UKF:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKF)) ? 1. : 0.;
			}
			else return 0.;
		case UKG:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKG)) ? 1. : 0.;
			}
			else return 0.;
		case UKH:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKH)) ? 1. : 0.;
			}
			else return 0.;
		case UKI:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKI)) ? 1. : 0.;
			}
			else return 0.;
		case UKJ:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKJ)) ? 1. : 0.;
			}
			else return 0.;
		case UKK:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKK)) ? 1. : 0.;
			}
			else return 0.;
		case UKL:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKL)) ? 1. : 0.;
			}
			else return 0.;
		case UKM:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKM)) ? 1. : 0.;
			}
			else return 0.;
		case UKN:
			if (getRegion() != null) {
				return (getRegion().equals(Region.UKN)) ? 1. : 0.;
			}
			else return 0.;
		case UKmissing:
			return 0.;		//For our purpose, all our simulated people have a region, so this enum value is always going to be 0 (false).
//			return (getRegion().equals(Region.UKmissing)) ? 1. : 0.;		//For people whose region info is missing.  The UK survey did not record the region in the first two waves (2006 and 2007, each for 4 years). For all those individuals we have gender, education etc but not region. If we exclude them we lose a large part of the UK sample, so this is the trick to keep them in the estimates.

		// Regressors used in the Covid-19 labour market module below:
		case Dgn_Dag:
			if(dgn.equals(Gender.Male)) {
				return (double) dag;
			}
			else return 0.;
		case Employmentsonfullfurlough:
			return Parameters.getEmploymentsFurloughedFullForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), model.getYear());
		case Employmentsonflexiblefurlough:
			return Parameters.getEmploymentsFurloughedFlexForMonthYear(model.getLabourMarket().getCovid19TransitionsMonth(), model.getYear());
		case CovidTransitionsMonth:
			model.getLabourMarket().getMonthForRegressor();
		case Lhw_L1:
			if (getNewWorkHours_lag1() != null) {
				return getNewWorkHours_lag1();
			}
			else return 0.;
		case Dgn_Lhw_L1:
			if (getNewWorkHours_lag1() != null && dgn.equals(Gender.Male)) {
				return getNewWorkHours_lag1();
			}
			else return 0.;
		case Covid19GrossPayMonthly_L1:
			return getCovidModuleGrossLabourIncome_lag1();
		case Covid19ReceivesSEISS_L1:
			return (getCovidModuleReceivesSEISS().equals(Indicator.True)) ? 1. : 0.;
		case Les_c7_Covid_Furlough_L1:
			return (getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFlex) || getLes_c7_covid_lag1().equals(Les_c7_covid.FurloughedFull)) ? 1. : 0.;
		case Blpay_Q2:
			return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q2)) ? 1. : 0.;
		case Blpay_Q3:
			return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q3)) ? 1. : 0.;
		case Blpay_Q4:
			return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q4)) ? 1. : 0.;
		case Blpay_Q5:
			return (getCovidModuleGrossLabourIncomeBaseline_Xt5().equals(Quintiles.Q5)) ? 1. : 0.;
		case Dgn_baseline:
			return 0.;
		default:
			throw new IllegalArgumentException("Unsupported regressor " + variableID.name() + " in Person#getDoubleValue");
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
	        if (!(o instanceof Person)) {
	            return false;
	        }

	        Person p = (Person) o;

	        boolean idIsEqual = new EqualsBuilder()
	                .append(key.getId(), p.key.getId())		//Add more fields to compare to check for equality if desired
	                .isEquals();
	        
//	        if(idIsEqual) {		//Check other fields
//		        //Throw an exception if there are person objects with the same id but different other fields as this should not
//		        //be possible and suggests a problem with the input data
//		        boolean allFieldsAreEqual = new EqualsBuilder()
//		        		.append(key.getId(), p.key.getId())
//		        		.append(age, p.age)
//						.append(gender, p.gender)
//						.append(education, p.education)
//						.append(household_status, p.household_status)
//						.append(household_status_lag, p.household_status_lag)
//						.append(activity_status, p.activity_status)
//						.append(activity_status_lag, p.activity_status_lag)
//						.append(deviationFromMeanRetirementAge, p.deviationFromMeanRetirementAge)	        		
//						.append(potentialEarnings, p.potentialEarnings)
//		        		.append(desiredAgeDiff, p.desiredAgeDiff)
//		        		.append(desiredEarningsPotentialDiff, p.desiredEarningsPotentialDiff)
//		        		.append(durationInCouple, p.durationInCouple)
//		        		.append(partner, p.partner)
//		        		.append(partnerId, p.partnerId)
//		        		.append(weight, p.weight)
//		        		.append(motherId, p.motherId)
//		        		.append(fatherId, p.fatherId)
//		        		.append(benefitUnit, p.benefitUnit)
//		        		.append(householdId, p.householdId)
//		        		.append(labourSupplyWeekly, p.labourSupplyWeekly)
//		        		.append(toGiveBirth, p.toGiveBirth)
//		        		.append(toLeaveSchool, p.toLeaveSchool)
//		        		.append(unitLabourCost, p.unitLabourCost)
//		        		.append(workSector, p.workSector)
//		        		.isEquals();
//		        if(!allFieldsAreEqual) {
//		        	throw new IllegalArgumentException("Error - there are multiple Person objects with the same id " + key.getId() + " but different fields!");
//		        }
//	        }
	        
	        return idIsEqual;
//	        return allFieldsAreEqual;
	    }

	    @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(key.getId())
//					.append(activity_status)
//					.append(activity_status_lag)
//	        		.append(age)
//	        		.append(desiredAgeDiff)
//	        		.append(desiredEarningsPotentialDiff)
//					.append(deviationFromMeanRetirementAge)
//	        		.append(durationInCouple)
//					.append(education)
//	        		.append(fatherId)
//					.append(gender)
//	        		.append(benefitUnit)
//	        		.append(household_status)
//	        		.append(household_status_lag)
//	        		.append(householdId)
//	        		.append(labourSupplyWeekly)
//	        		.append(motherId)
//	        		.append(partner)
//	        		.append(partnerId)
//					.append(potentialEarnings)
//	        		.append(toGiveBirth)
//	        		.append(toLeaveSchool)
//	        		.append(unitLabourCost)
//	        		.append(weight)
//	        		.append(workSector)
	                .toHashCode();
	    }
	
	
	
	// ---------------------------------------------------------------------
	// Access methods
	// ---------------------------------------------------------------------

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

     public int getPersonCount() {
     	return 1;
     }

	public int getDag() {
		return dag;
	}

	public void setDag(Integer dag) {
		this.dag = dag;
		this.dag_sq = dag*dag;
	}

	public Gender getDgn() {
		return dgn;
	}

	public void setDgn(Gender dgn) {
		this.dgn = dgn;
	}

	public Les_c4 getLes_c4() {
		return les_c4;
	}
		
	
	public int getStudent()
	{
		if (les_c4 != null) {
			return les_c4.equals(Les_c4.Student)? 1 : 0;
		}
		else {
			return 0;
		}
	}

	public void setLes_c4(Les_c4 les_c4) {
		this.les_c4 = les_c4;
	}

	public void setLes_c7_covid(Les_c7_covid les_c7_covid) { this.les_c7_covid = les_c7_covid; }

	public Les_c7_covid getLes_c7_covid() { return les_c7_covid; }

	public Les_c4 getLes_c4_lag1() {
		return les_c4_lag1;
	}

	public Les_c7_covid getLes_c7_covid_lag1() { return les_c7_covid_lag1; }

	public void setLes_c7_covid_lag1(Les_c7_covid les_c7_covid_lag1) {
		 this.les_c7_covid_lag1 = les_c7_covid_lag1;
	}

	public Les_c4 getLessp_c4() {
		return lessp_c4;
	}

	public void setLessp_c4(Les_c4 lessp_c4) {
		this.lessp_c4 = lessp_c4;
	}

	public Les_c4 getActivity_status_partner_lag() {
		return activity_status_partner_lag;
	}

	public void setActivity_status_partner_lag(Les_c4 activity_status_partner_lag) {
		this.activity_status_partner_lag = activity_status_partner_lag;
	}

	public Household_status getHousehold_status() {
		return household_status;
	}

	public void setHousehold_status(Household_status household_status) {
		if(this.household_status != null) {
			if(household_status.equals(Household_status.Parents) && (this.household_status.equals(Household_status.Couple) || this.household_status.equals(Household_status.Single))) {
				throw new IllegalArgumentException("Person with household_status as couple or single cannot move back to the parental home!");
			}
		}
		this.household_status = household_status;
	}
	
	public int getCohabiting() {
		if(household_status != null) {
			return household_status.equals(Household_status.Couple)? 1 : 0;
		}
		else {
			return 0;
		}
	}

	public Education getDeh_c3() {
		return deh_c3;
	}

	public void setDeh_c3(Education deh_c3) {this.deh_c3 = deh_c3;}

	public void setDeh_c3_lag1(Education deh_c3_lag1) {this.deh_c3_lag1 = deh_c3_lag1;}

	public Education getDehm_c3() {
		return dehm_c3;
	}

	public void setDehm_c3(Education dehm_c3) {
		this.dehm_c3 = dehm_c3;
	}

	public Education getDehf_c3() {
		return dehf_c3;
	}

	public void setDehf_c3(Education dehf_c3) {
		this.dehf_c3 = dehf_c3;
	}

	public Education getEducation_partner() {
		return dehsp_c3;
	}

	public void setEducation_partner(Education education_partner) {
		this.dehsp_c3 = education_partner;
	}

	public Indicator getDed() {
		return ded;
	}

	public void setDed(Indicator ded) {
		this.ded = ded;
	}

	public int getLowEducation() {
		if(deh_c3 != null) {
			if (deh_c3.equals(Education.Low)) return 1;
			else return 0;
		}
		else {
			return 0;
		}
	}
	
	public int getMidEducation() {
		if(deh_c3 != null) {
			if (deh_c3.equals(Education.Medium)) return 1;
			else return 0;
		}
		else {
			return 0;
		}
	}
	
	public int getHighEducation() {
		if(deh_c3 != null) {
			if (deh_c3.equals(Education.High)) return 1;
			else return 0;
		}
		else {
			return 0;
		}
	}
	
	public void setEducation(Education educationlevel) {
		this.deh_c3 = educationlevel;
	}

	public int getGoodHealth() {
		if(dlltsd != null && !dlltsd.equals(Indicator.True)) { //Good / bad health depends on dlltsd (long-term sick or disabled). If true, then person is in bad health.
			return 1;
		}
		else return 0;			
	}
	
	public int getBadHealth() {
		if(dlltsd != null && dlltsd.equals(Indicator.True)) {
			return 1;
		}
		else return 0;
	}
	
	/*
	 * In the initial population, there is continuous health score and an indicator for long-term sickness or disability. In EUROMOD to which we match, health is either 
	 * Good or Poor. This method checks the Dlltsd indicator and returns corresponding HealthStatus to use in matching the EUROMOD donor. 
	 */
	public HealthStatus getHealthStatusConversion() {
		if(dlltsd != null && dlltsd.equals(Indicator.True)) {
			return HealthStatus.Poor; //If long-term sick or disabled, return Poor HealthStatus
		}
		else return HealthStatus.Good; //Otherwise, return Good HealthStatus
	}

	public int getEmployed() {
		if(les_c4.equals(Les_c4.EmployedOrSelfEmployed) ) {
			return 1;
		}
		else return 0;			
	}
	
	public int getNonwork() {
		if(les_c4.equals(Les_c4.NotEmployed)) {
			return 1;
		}
		else return 0;
	}

	public Region getRegion() {
		return benefitUnit.getRegion();		//Now held as benefitUnit information
	}
	
	public void setRegion(Region region) {
		this.benefitUnit.setRegion(region);
	}

	public Household_status getHousehold_status_lag() {
		return household_status_lag;
	}

//	public double getDeviationFromMeanRetirementAge() {
//		return deviationFromMeanRetirementAge;
//	}

	public boolean isToGiveBirth() {
		return toGiveBirth;
	}
	
	public void setToGiveBirth(boolean toGiveBirth_) {
			toGiveBirth = toGiveBirth_;
	}

	public boolean isToLeaveSchool() {
		return toLeaveSchool;
	}

	public void setToLeaveSchool(boolean toLeaveSchool) {
		this.toLeaveSchool = toLeaveSchool;
	}

	public double getWeight() {
		return weight;
	}
	
	public void setWeight(double weight) {
		this.weight = weight;
	}

//	public Set<Person> getChildren() {
//		return benefitUnit.getChildren();
//	}

	public BenefitUnit getBenefitUnit() {
		return benefitUnit;
	}
	
	public void setBenefitUnit(BenefitUnit benefitUnit) {
		this.benefitUnit = benefitUnit;
		id_benefitUnit = benefitUnit.getKey().getId();
	}

	public Person getPartner() {
		return partner;
	}
	
	public void setPartner(Person partner) {
		this.partner = partner;
		if(partner == null) {
			id_partner = null;
		}
		else {
			this.id_partner = partner.getKey().getId();		//Update partnerId to ensure consistency
		}
	}

	public Long getId_partner() {
		return id_partner;
	}


	public Long getId_partner_lag1() {
		return id_partner_lag1;
	}

	public long getId_benefitUnit() {
		return id_benefitUnit;
	}

	public Labour getLabourSupplyWeekly() {
		return labourSupplyWeekly;
	}

	public double getLabourSupplyWeeklyHours() {
		return labourSupplyWeekly.getHours();
	}

	public void setLabourSupplyWeekly(Labour labourSupply) {
		this.labourSupplyWeekly = labourSupply;
	}
		
	public double getLabourSupplyYearly() {
		return labourSupplyWeekly.getHours() * Parameters.WEEKS_PER_MONTH_RATIO * 12;
	} //TODO: add scaling factor multiplication?

	public double getScaledLabourSupplyYearly() {
		return labourSupplyWeekly.getHours() * Parameters.WEEKS_PER_MONTH_RATIO * 12 * model.getScalingFactor();
	}
	
	public double getGrossEarningsWeekly() {
		return potentialEarnings * labourSupplyWeekly.getHours();
	}
	
	public Double getGrossEarningsYearly() {
		Double gew = getGrossEarningsWeekly();
		if(Double.isFinite(gew) && gew > 0.) {
			return gew * Parameters.WEEKS_PER_MONTH_RATIO * 12;
		}
		else return 0.;
//		else return null;
	}
	
	public Integer getAtRiskOfPoverty() {
		return benefitUnit.getAtRiskOfPoverty();
	}
	
	public double getPotentialEarnings() {
		return potentialEarnings;
	}

	public double getDesiredAgeDiff() {
		return desiredAgeDiff;
	}

	public double getDesiredEarningsPotentialDiff() {
		return desiredEarningsPotentialDiff;
	}

	public Long getId_mother() {
		return id_mother;
	}
	
	public void setId_mother(Long id_mother) {
		this.id_mother = id_mother;
	}

	public Long getId_father() {
		return id_father;
	}
	
	public void setId_father(Long id_father) {
		this.id_father = id_father;
	}

	public boolean isResponsible() {
		boolean responsible;
		if(dgn.equals(Gender.Female)) {
			Person female = benefitUnit.getFemale();
			responsible = female != null && female == this; 
		}
		else {
			Person male = benefitUnit.getMale();
			responsible = male != null && male == this;
		}
		return responsible;
	}
	
	public boolean isChild() {
		return benefitUnit.getChildren().contains(this);
	}
	
	public boolean isOtherMember() {
		return benefitUnit.getOtherMembers().contains(this);
	}


	public void orphanGiveParent() { 
		if(dag < Parameters.AGE_TO_BECOME_RESPONSIBLE && id_mother == null && id_father == null) {		//Check if orphan
			Person adoptedMother = benefitUnit.getFemale();
			if(adoptedMother != null) {
				id_mother = adoptedMother.getKey().getId();
			}
			else {
				id_father = benefitUnit.getMale().getKey().getId();		//Adopted father
			}
		}	
		else throw new IllegalArgumentException("ERROR - orphanGiveParent method has been called on a non-orphan!");
	}
	/*
	public double getUnitLabourCost() {
		return unitLabourCost;
	}

	public void setUnitLabourCost(double unitLabourCost) {
		this.unitLabourCost = unitLabourCost;
	}
	*/
	public Double getDhe() {
		return dhe;
	}

	public void setDhe(Double health) {
		this.dhe = health;
	}

	public Double getDhm() { return dhm;}

	public void setDhm(Double dhm) {
		this.dhm = dhm;
	}

	public void setDhe_lag1(Double health) {
		this.dhe_lag1 = health;
	}

	public void setDhm_lag1(Double dhm) {
		this.dhm_lag1 = dhm;
	}

	public Indicator getDer() {
		return der;
	}

	public void setDer(Indicator der) {
		this.der = der;
	}

	public Long getId_original() {
		return id_original;
	}
	
	public Long getId_bu_original() {
		return id_bu_original;
	}

	public int getAgeGroup() {
		return ageGroup;
	}

	public boolean isClonedFlag() {
		return clonedFlag;
	}

	public void setClonedFlag(boolean clonedFlag) {
		this.clonedFlag = clonedFlag;
	}

	public int getOriginalNumberChildren() {
		return originalNumberChildren;
	}

	public Household_status getOriginalHHStatus() {
		return originalHHStatus;
	}

	public Dcpst getDcpst() {
		return dcpst;
	}

	public void setDcpst(Dcpst dcpst) {
		this.dcpst = dcpst;
	}

	public Indicator getDcpen() {
		return dcpen;
	}

	public void setDcpen(Indicator dcpen) {
		this.dcpen = dcpen;
	}

	public Indicator getDcpex() {
		return dcpex;
	}

	public void setDcpex(Indicator dcpex) {
		this.dcpex = dcpex;
	}

	public Indicator getDlltsd() {
		return dlltsd;
	}

	public void setDlltsd(Indicator dlltsd) {
		this.dlltsd = dlltsd;
	}

	public Indicator getDlltsd_lag1() {
		return dlltsd_lag1;
	}

	public void setDlltsd_lag1(Indicator dlltsd_lag1) {
		this.dlltsd_lag1 = dlltsd_lag1;
	}

	public Indicator getSedex() {
		return sedex;
	}

	public void setSedex(Indicator sedex) {
		this.sedex = sedex;
	}

	public boolean isLeftEducation() {
		return leftEducation;
	}

	public void setLeftEducation(boolean leftEducation) {
		this.leftEducation = leftEducation;
	}

	public boolean isLeftPartnership() {
		return leftPartnership;
	}

	public void setLeftPartnership(boolean leftPartnership) {
		this.leftPartnership = leftPartnership;
	}

	public Indicator getPartnership_samesex() {
		return partnership_samesex;
	}

	public void setPartnership_samesex(Indicator partnership_samesex) {
		this.partnership_samesex = partnership_samesex;
	}

	public Indicator getWomen_fertility() {
		return women_fertility;
	}

	public void setWomen_fertility(Indicator women_fertility) {
		this.women_fertility = women_fertility;
	}

	public Indicator getEducation_inrange() {
		return education_inrange;
	}

	public void setEducation_inrange(Indicator education_inrange) {
		this.education_inrange = education_inrange;
	}

	public Integer getDcpyy() {
		return dcpyy;
	}

	public void setDcpyy(Integer dcpyy) {
		this.dcpyy = dcpyy;
	}

	public Integer getDcpyy_lag1() {
		return dcpyy_lag1;
	}

	public Integer getDcpagdf() {
		return dcpagdf;
	}

	public void setDcpagdf(Integer dcpagdf) {
		this.dcpagdf = dcpagdf;
	}

	
	public Integer getDcpagdf_lag1() {
		return dcpagdf_lag1;
	}

	public Double getYpnbihs_dv() {
		return ypnbihs_dv;
	}

	public void setYpnbihs_dv(Double ypnbihs_dv) {
		this.ypnbihs_dv = ypnbihs_dv;
	}

	public double getYpnbihs_dv_lag1() {
		return ypnbihs_dv_lag1;
	}

	public double getYptciihs_dv() {
		return yptciihs_dv;
	}


	public void setYptciihs_dv(double yptciihs_dv) {
		this.yptciihs_dv = yptciihs_dv;
	}

	public double getYptciihs_dv_lag1() {
		return yptciihs_dv_lag1;
	}

	public double getYplgrs_dv() {
		return yplgrs_dv;
	}

	public void setYplgrs_dv(double yplgrs_dv) {
		this.yplgrs_dv = yplgrs_dv;
	}
	
	public double getYplgrs_dv_lag1() {
		return yplgrs_dv_lag1;
	}

	public double getYplgrs_dv_lag2() {
		return yplgrs_dv_lag2;
	}

	public double getYplgrs_dv_lag3() {
		return yplgrs_dv_lag3;
	}

	public Double getYnbcpdf_dv() {
		return ynbcpdf_dv;
	}

	public void setYnbcpdf_dv(Double ynbcpdf_dv) {
		this.ynbcpdf_dv = ynbcpdf_dv;
	}

	public Double getYnbcpdf_dv_lag1() {
		return ynbcpdf_dv_lag1;
	}

	public Lesdf_c4 getLesdf_c4() {
		return lesdf_c4;
	}

	public Lesdf_c4 getLesdf_c4_lag1() {
		return lesdf_c4_lag1;
	}

	public void setLes_c4_lag1(Les_c4 les_c4_lag1) {
		this.les_c4_lag1 = les_c4_lag1;
	}

	public void setLesdf_c4_lag1(Lesdf_c4 lesdf_c4_lag1) {
		this.lesdf_c4_lag1 = lesdf_c4_lag1;
	}

	public void setYpnbihs_dv_lag1(Double ynbcpdf_dv_lag1) {
		this.ynbcpdf_dv_lag1 = ynbcpdf_dv_lag1;
	}

	public void setDehsp_c3_lag1(Education dehsp_c3_lag1) {
		this.dehsp_c3_lag1 = dehsp_c3_lag1;
	}

	public void setDhesp_lag1(Double dhesp_lag1) {
		this.dhesp_lag1 = dhesp_lag1;
	}

	public void setYnbcpdf_dv_lag1(Double ynbcpdf_dv_lag1) {
		this.ynbcpdf_dv_lag1 = ynbcpdf_dv_lag1;
	}

	public void setDcpyy_lag1(Integer dcpyy_lag1) {
		this.dcpyy_lag1 = dcpyy_lag1;
	}

	public void setDcpagdf_lag1(Integer dcpagdf_lag1) {
		this.dcpagdf_lag1 = dcpagdf_lag1;
	}

	public void setDcpst_lag1(Dcpst dcpst_lag1) {
		this.dcpst_lag1 = dcpst_lag1;
	}

	public void setPotentialEarnings(double potentialEarnings) {
		this.potentialEarnings = potentialEarnings;
	}

	public void setLiwwh(Integer liwwh) {
		this.liwwh = liwwh;
	}

	public void setIoFlag(boolean ioFlag) {
		this.ioFlag = ioFlag;
	}

	public boolean isToBePartnered() {
		return toBePartnered;
	}

	public void setToBePartnered(boolean toBePartnered) {
		this.toBePartnered = toBePartnered;
	}

	public int getCoupleOccupancy() {

		if(partner != null) {
			return 1;
		}
		else return 0;
	}

	public int getAdultChildFlag() {
		if (adultchildflag!= null) {
			if (adultchildflag.equals(Indicator.True)) {
				return 1;
			}
			else return 0;
		}
		else return 0;
	}

	public Person getOriginalPartner() {
		return originalPartner;
	}

	public long getId_household() {
		return benefitUnit.getId_household();
	}

	public void setId_household(long id_household) {
		benefitUnit.setId_household(id_household);
	}

	public Series.Double getYearlyEquivalisedDisposableIncomeSeries() {
		return yearlyEquivalisedDisposableIncomeSeries;
	}

	public void setYearlyEquivalisedDisposableIncomeSeries(Series.Double yearlyEquivalisedDisposableIncomeSeries) {
		this.yearlyEquivalisedDisposableIncomeSeries = yearlyEquivalisedDisposableIncomeSeries;
	}

	public Double getYearlyEquivalisedConsumption() {
		return yearlyEquivalisedConsumption;
	}

	public void setYearlyEquivalisedConsumption(Double yearlyEquivalisedConsumption) {
		this.yearlyEquivalisedConsumption = yearlyEquivalisedConsumption;
	}

	public Series.Double getYearlyEquivalisedConsumptionSeries() {
		return yearlyEquivalisedConsumptionSeries;
	}

	public void setYearlyEquivalisedConsumptionSeries(Series.Double yearlyEquivalisedConsumptionSeries) {
		this.yearlyEquivalisedConsumptionSeries = yearlyEquivalisedConsumptionSeries;
	}

	/*
	public Double getsIndex() {
		if (sIndexYearMap.get(model.getYear()-model.getsIndexTimeWindow()) != null) {
			return sIndexYearMap.get(model.getYear() - model.getsIndexTimeWindow());
		} else {
			return Double.NaN;
		}
	}

	public void setsIndex(Double sIndex) {
		sIndexYearMap.put(model.getYear(), sIndex);
	}

	 */

	public Double getsIndex() {
		if (sIndex != null && sIndex > 0. && !sIndex.isInfinite() && (model.getYear() >= model.getStartYear()+model.getsIndexTimeWindow())) {
			return sIndex;
		}
		else return Double.NaN;
	}

	public void setsIndex(Double sIndex) {
		this.sIndex = sIndex;
	}

	public Double getsIndexNormalised() {
		if (sIndexNormalised != null && sIndexNormalised > 0. && !sIndexNormalised.isInfinite() && (model.getYear() >= model.getStartYear()+model.getsIndexTimeWindow())) {
			return sIndexNormalised;
		}
		else return Double.NaN;
	}

	public void setsIndexNormalised(Double sIndexNormalised) {
		this.sIndexNormalised = sIndexNormalised;
	}

	public Map<Integer, Double> getsIndexYearMap() {
		return sIndexYearMap;
	}

	public Integer getNewWorkHours_lag1() {
		return newWorkHours_lag1;
	}

	public void setNewWorkHours_lag1(Integer newWorkHours_lag1) {
		this.newWorkHours_lag1 = newWorkHours_lag1;
	}

	public double getCovidModuleGrossLabourIncome_lag1() {
		return covidModuleGrossLabourIncome_lag1;
	}

	public void setCovidModuleGrossLabourIncome_lag1(double covidModuleGrossLabourIncome_lag1) {
		this.covidModuleGrossLabourIncome_lag1 = covidModuleGrossLabourIncome_lag1;
	}

	public Indicator getCovidModuleReceivesSEISS() {
		return covidModuleReceivesSEISS;
	}

	public void setCovidModuleReceivesSEISS(Indicator covidModuleReceivesSEISS) {
		this.covidModuleReceivesSEISS = covidModuleReceivesSEISS;
	}

	public double getCovidModuleGrossLabourIncome_Baseline() {
		return covidModuleGrossLabourIncome_Baseline;
	}

	public void setCovidModuleGrossLabourIncome_Baseline(double covidModuleGrossLabourIncome_Baseline) {
		this.covidModuleGrossLabourIncome_Baseline = covidModuleGrossLabourIncome_Baseline;
	}

	public Quintiles getCovidModuleGrossLabourIncomeBaseline_Xt5() {
		return covidModuleGrossLabourIncomeBaseline_Xt5;
	}

	public void setCovidModuleGrossLabourIncomeBaseline_Xt5(Quintiles covidModuleGrossLabourIncomeBaseline_Xt5) {
		this.covidModuleGrossLabourIncomeBaseline_Xt5 = covidModuleGrossLabourIncomeBaseline_Xt5;
	}

	public Double getWageRegressionRandomComponent() {
		return wageRegressionRandomComponent;
	}

	public void setWageRegressionRandomComponent(Double wageRegressionRandomComponent) {
		this.wageRegressionRandomComponent = wageRegressionRandomComponent;
	}

	public double getRandomGaussianIfStochasticComponentOn(){
		if(model.addRegressionStochasticComponent) {
			return SimulationEngine.getRnd().nextGaussian();
		} else {
			return 0.;
		}
	}

	/*
	getRandomGaussianAndRMSEProductForRegression return the product of a draw from Gaussian distribution and a RMSE of a given regression model, stored in reg_RMSE Excel file
	*/
	public double getRandomGaussianAndRMSEProductForRegression(String regressionName) {
		return getRandomGaussianIfStochasticComponentOn() * Parameters.getRMSEForRegression(regressionName);
	}

	public boolean isDhh_owned() {
		return dhh_owned;
	}

	public void setDhh_owned(boolean dhh_owned) {
		this.dhh_owned = dhh_owned;
	}

	public boolean isReceivesBenefitsFlag() {
		return receivesBenefitsFlag;
	}

	public void setReceivesBenefitsFlag(boolean receivesBenefitsFlag) {
		this.receivesBenefitsFlag = receivesBenefitsFlag;
	}

	public boolean isReceivesBenefitsFlag_L1() {
		return receivesBenefitsFlag_L1;
	}

	public void setReceivesBenefitsFlag_L1(boolean receivesBenefitsFlag_L1) {
		this.receivesBenefitsFlag_L1 = receivesBenefitsFlag_L1;
	}

	public double getEquivalisedDisposableIncomeYearly() {
		return benefitUnit.getEquivalisedDisposableIncomeYearly();
	}
}
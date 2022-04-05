package labsim.model;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import labsim.data.Parameters;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import labsim.model.enums.Gender;
import labsim.model.enums.Indicator;
import labsim.model.enums.Occupancy;
import labsim.model.enums.Region;

@Entity
public class DonorHousehold {
	
	@Transient
	private final LABSimModel model;

//	@Column(name="idhh")
//	public long idHousehold;
	
	@Id
	private final PanelEntityKey key;	//The key is based on ID in the household SQL table which is based on the benefit unit variable from EUROMOD

	@Transient
	private Occupancy occupancy;
	
	@Column(name="idfemale")
	private Long femaleId;
	
	@Transient
	private DonorPerson female;		//The female head of the household and the mother of the children
	
	@Column(name="idmale")
	private Long maleId;
	
	@Transient
	private DonorPerson male;		//The male head of the household and the (possibly step) father of the children
	
	@Transient
	private Set<DonorPerson> occupants;	//All occupants of the house, including male, female, children and otherMembers
	
	@Transient
	private Set<DonorPerson> children;
	
	@Transient
	private Set<DonorPerson> otherMembers;		//Residual household members who are not children, such as aged parents????
	
//	@Column(name="household_weight")
//	private double household_weight;		//TODO: Do we need this?  Can we remove it?

	@Enumerated(EnumType.STRING)
	private Region region;
		
	private Integer size;

	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3under;				//Dummy variable for whether the person has children under 4 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_4_12;				//Dummy variable for whether the person has children between 4 and 12 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_2under;				//Dummy variable for whether the person has children under 3 years old.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_3_6;				//Dummy variable for whether the person has children between 3 and 6 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.

	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_7_12;				//Dummy variable for whether the person has children between 7 and 12 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
	@Enumerated(EnumType.ORDINAL)
	private Indicator d_children_13_17;				//Dummy variable for whether the person has children between 13 and 17 years old inclusive.  As a string, it has values {False, True} but as ordinal this is mapped to {0, 1}.
	
	@Transient
	@Column(name="n_children_age0")
	private int n_children_0;
	
	@Transient
	@Column(name="n_children_age1")
	private int n_children_1;
	
	@Transient
	@Column(name="n_children_age2")
	private int n_children_2;
	
	@Transient
	@Column(name="n_children_age3")
	private int n_children_3;

	@Transient
	@Column(name="n_children_age4")
	private int n_children_4;

	@Transient
	@Column(name="n_children_age5")
	private int n_children_5;

	@Transient
	@Column(name="n_children_age6")
	private int n_children_6;

	@Transient
	@Column(name="n_children_age7")
	private int n_children_7;

	@Transient
	@Column(name="n_children_age8")
	private int n_children_8;

	@Transient
	@Column(name="n_children_age9")
	private int n_children_9;

	@Transient
	@Column(name="n_children_age10")
	private int n_children_10;

	@Transient
	@Column(name="n_children_age11")
	private int n_children_11;

	@Transient
	@Column(name="n_children_age12")
	private int n_children_12;
	
	@Transient
	@Column(name="n_children_age13")
	private int n_children_13;
	
	@Transient
	@Column(name="n_children_age14")
	private int n_children_14;

	@Transient
	@Column(name="n_children_age15")
	private int n_children_15;

	@Transient
	@Column(name="n_children_age16")
	private int n_children_16;

	@Transient
	@Column(name="n_children_age17")
	private int n_children_17;

	@Transient
	private Map<String, Double> disposableToGrossIncomeRatio;
	
	@Transient
	private Map<String, Double> disposableIncomeToGrossEarningsRatio;	
	
	@Transient
	private Map<String, Double> disposableIncome;

//	@Transient
//	private MultiKeyMap disposableIncomeUprated;
	
	@Transient
//	private double grossEarnings; //When earnings = 0 it is not possible to calculate the income to gross earnings ratio
	private Map<String, Double> grossEarnings;

	@Transient
	private Map<String, Double> grossIncome;
	
	//-------------------------------------------------------------------------------
	//	Constructors
	//-------------------------------------------------------------------------------
	
	//Used when loading the initial set of benefitUnits from the input database
	public DonorHousehold() {
		super();
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
		key  = new PanelEntityKey();		//Sets up key
		children = new LinkedHashSet<DonorPerson>();
		otherMembers = new LinkedHashSet<DonorPerson>();
		disposableToGrossIncomeRatio = new LinkedHashMap<String, Double>();
		disposableIncomeToGrossEarningsRatio = new LinkedHashMap<String, Double>();	
		disposableIncome = new LinkedHashMap<String, Double>();
		grossEarnings = new LinkedHashMap<>();
		grossIncome = new LinkedHashMap<>();
	}	
	
	protected void initializeAttributes() {

		//TODO: Need to set-up references to members, i.e. male, female, children, otherMembers etc.
		//Use EUROMOD naming conventions regarding determination of responsible adult to make this efficient search
		//TODO: Need to fundamentally change the definition of DonorHousehold to consider all members of the same house in 
		//EUROMOD data as potentially contributing income, not just the responsible male and female.  We do not need to match
		//our modelling assumptions in the DonorHousehold class, as it is just a data structure to achieve two things:
		//(1) find the most similar EUROMOD benefitUnits to each of our simulated benefitUnits, for each possible labour supply combination
		//(2) calculate the disposable to gross conversion ratio.  If both the disposable and gross incomes are sums over all 
		// household members, then that should be OK right?  If we were to start missing out members of the DonorHousehold class
		// because they are not deemed the 'responsible male or female' under our definition (and our definition may evolve), then we
		// would start distorting the disposable to gross conversion rate.
		occupants = model.getEuromodOutputPersonsByHouseholdId().get(key.getId());
		
		//Here are the rules of assigning roles of euromod persons within the euromod household:
//		"Defining the head of an assessment unit:
//		The head is the richest member of the unit; if there are two or more equally rich persons, 
//		the oldest is the head; if there are two or more equally rich and equally old persons, the 
//		person with the lowest idperson is the head. "Richest" is defined by the variable or incomelist 
//		indicated by parameter HeadDefInc, which is set to ils_origy (i.e. market incomes) by default. 
//		In fact, if the assessment unit type is SUBGROUP (i.e. an assessment unit smaller than the whole 
//		household), finding the head has to be repeated until all members of the household are assigned 
//		to a unit. That means, firstly (simplifying) the richest person of the household is found as first 
//		head and all persons fulfilling the relations defined by parameter members (i.e. based on the family 
//		relationships included in the survey) are assigned to her/his unit. Then, if any household members are 
//		not yet assigned, the richest person among them is found as the second head and all not yet assigned 
//		persons fulfilling the relations defined by parameter members are assigned to her/his unit. The last 
//		sentence is repeated until all household members are assigned to a unit."
		

		double richestIncome = -Double.MAX_VALUE;
		int richestOldestAge = -Integer.MAX_VALUE;
		long richestOldestLowestId = Long.MAX_VALUE;
		DonorPerson headOfTheHousehold = occupants.iterator().next();		//Initialise with a genuine person, instead of null to avoid having to check null conditions below
		if(headOfTheHousehold == null) {
			throw new IllegalArgumentException("headOfTheHousehold is null!");
		}
		
		for(DonorPerson person: occupants) {
//			log.debug("DonorPerson " + person.getKey().getId());
			double income = person.getOriginalIncomeMonthly(Parameters.getEUROMODpolicyForThisYear(model.getStartYear())); //Original income is now policy dependent, take the one that corresponds to base year policy here
			if(income > richestIncome) {
				richestIncome = income;
				headOfTheHousehold = person;
			}
			else if(income == richestIncome) {
				int age = person.getDag();
				if(age > richestOldestAge) {
					richestOldestAge = age;
					headOfTheHousehold = person;
				}
				else if(age == richestOldestAge) {
					long id = person.getKey().getId();
					if(id < richestOldestLowestId) {
						richestOldestLowestId = id;
						headOfTheHousehold = person;
					}
				}
			}			
		}
		
//		region = headOfTheHousehold.getRegion();
//		weight = headOfTheHousehold.getWeight();
		

//		log.debug("head of the household " + headOfTheHousehold.getKey().getId() + " has gender " + headOfTheHousehold.getGender() + " and house has occupancy " + occupancy);

		//Set the head of household's partner as the responsible adult of the opposite sex (if they have a partner).
		//Add other occupants to either the children set (if their age is under 18) otherwise 'otherMembers'.
		Long partnerId = headOfTheHousehold.getPartnerId();				//Partner references not establish (as more memory efficient to leave as numerical id instead of object references to partner)
		boolean partnerNotFound = true;
		Iterator<DonorPerson> pIter = occupants.iterator();
        while (pIter.hasNext()) {
            DonorPerson person = pIter.next();
        	if(person != headOfTheHousehold) {
				if(partnerId != null && partnerId.equals(person.getKey().getId())) {			//The partner exists and should be in the same household
					if(person.getPartnerId().equals(headOfTheHousehold.getKey().getId())) {
						if(headOfTheHousehold.getDgn().equals(Gender.Female)) {
							male = person;
							maleId = person.getKey().getId();
						}
						else {
							female = person;
							femaleId = person.getKey().getId();
						}
					}
					else {
						throw new IllegalArgumentException("Error - DonorPerson partner identities do not match for DonorPerson " + headOfTheHousehold.getKey().getId() + " with partnerId " + headOfTheHousehold.getPartnerId() + " and their partner " + person.getKey().getId() + " whose partnerId is " + person.getPartnerId() + "!");
					}
					partnerNotFound = false;
				}
				else {		//Person is not partner of head of the household
					if(person.getDag() < Parameters.AGE_TO_BECOME_RESPONSIBLE) {		//To be consistent with the definition of children in our simulation, consider this person a child if they are under the age to leave home
						children.add(person);
					}
					else {
						otherMembers.add(person);
					}
				}
        	}
		}
		
		if(partnerId != null && partnerNotFound) {
			partnerId = null;
//			throw new IllegalStateException("Error - DonorPerson " + headOfTheHousehold.getKey().getId() + " with partnerId " + partnerId + " could not be found in the same DonorHousehold " + key.getId() + "!");
		}

		updateChildrenFields();

//		Double sumOfOccupantsMonthlyGrossEarnings = 0.;	//(Self) Employment related income
//		Double sumOfOccupantsMonthlyOriginalIncome = 0.;	//All gross income including (self) employment related income, private pensions, investment income, property income, private transfers etc.
		Map<String, Double> sumOfOccupantsMonthlyDisposableIncome = new LinkedHashMap<String, Double>();		//Income after tax / benefits transfer
		Map<String, Double> sumOfOccupantsMonthlyGrossEarnings = new LinkedHashMap<>();			//(Self) Employment related income
		Map<String, Double> sumOfOccupantsMonthlyOriginalIncome = new LinkedHashMap<>();		//All gross income including (self) employment related income, private pensions, investment income, property income, private transfers etc.

		for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
			sumOfOccupantsMonthlyDisposableIncome.put(policyName, 0.);	//Initialize
			sumOfOccupantsMonthlyGrossEarnings.put(policyName, 0.);
			sumOfOccupantsMonthlyOriginalIncome.put(policyName, 0.);
		}

		for(DonorPerson occupant: occupants) {
//			if(occupant.getAge() >= Parameters.getAgeToLeaveHome()) {		//Should we restrict by age, so that only those over the age to leave home work and earn an income?  This would make it more similar to our own model, however it would distort the true statistics!	
//				sumOfOccupantsMonthlyGrossEarnings += occupant.getMonthlyEarningsGross();
//				sumOfOccupantsMonthlyOriginalIncome += occupant.getOriginalIncomeMonthly();
				for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
					Double previousValDispInc = sumOfOccupantsMonthlyDisposableIncome.get(policyName);
					sumOfOccupantsMonthlyDisposableIncome.put(policyName, previousValDispInc + occupant.getDisposableIncomeMonthly(policyName));		//Policy dependent, so must iterate through all policies.

					Double previousValGrossEarnings = sumOfOccupantsMonthlyGrossEarnings.get(policyName);
					sumOfOccupantsMonthlyGrossEarnings.put(policyName, previousValGrossEarnings + occupant.getMonthlyEarningsGross(policyName));

					Double previousValOrigIncome = sumOfOccupantsMonthlyOriginalIncome.get(policyName);
					sumOfOccupantsMonthlyOriginalIncome.put(policyName, previousValOrigIncome + occupant.getOriginalIncomeMonthly(policyName));

				}
//			}
//			if(!occupant.getRegion().equals(region)) {
//				throw new IllegalStateException("ERROR - the DonorPerson has a different region to that of the DonorHousehold!");
//			}
		}
		
		/*
		 * It's possible that the sum of income or earnings is 0, in which case we cannot calculate the ratio. In that case, set ratio to 1, so we use the earnings from the simulation directly,
		 * without the adjustment. 
		 */

		// Note: "within-policy-year" ratios are now used, for which the deflator shouldn't change anything so it is not applied (deflating both numerator and denominator produces
		// the same ratio as calculating it without the deflation factor). However, we still need to apply it to monetary variables in levels,
		// as they are used in the minimum-distance matching of simulated households with EM-donor households.

		for (Object key : Parameters.EUROMODpolicySchedule.keySet()) {
			String policyName = Parameters.EUROMODpolicySchedule.get(key);

//			int deflateFromYear = Math.min((Integer) key, model.getYear()); //Deflate EM monetary variables from min(policy year, observed year) to base year (Parameters.BASE_PRICE_YEAR).
//			double deflatorCPI = Parameters.deflatorCPIMap.get(key);
//			System.out.println(deflatorCPI);

//		for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
			if(sumOfOccupantsMonthlyOriginalIncome.get(policyName) != 0.0) {
				disposableToGrossIncomeRatio.put(policyName, (sumOfOccupantsMonthlyDisposableIncome.get(policyName) / sumOfOccupantsMonthlyOriginalIncome.get(policyName)));
			}
			else {
				disposableToGrossIncomeRatio.put(policyName, 0.0);
			}
			
			if(sumOfOccupantsMonthlyGrossEarnings.get(policyName) != 0.0) {
				disposableIncomeToGrossEarningsRatio.put(policyName, (sumOfOccupantsMonthlyDisposableIncome.get(policyName) / sumOfOccupantsMonthlyGrossEarnings.get(policyName)));
			}
			else {
				disposableIncomeToGrossEarningsRatio.put(policyName, 0.0);
			}

			// Values below are deflated to the base year level (specified in Parameters class), for example 2015.

			//If earnings = 0 it is not possible to calculate the ratio above. In that case, use disposable income directly. Values are uprated in the getDisposableIncome method.
			disposableIncome.put(policyName, sumOfOccupantsMonthlyDisposableIncome.get(policyName));

			// Values of disposableIncomeUprated are uprated from system year of each policy to each simulated year, instead of one baseline like above
			/*
			for (int yearSimulated = Parameters.getMin_Year(); yearSimulated <= Parameters.getMax_Year(); yearSimulated++) {
				double upratingFactor = (double) Parameters.upratingFactorsMap.get(yearSimulated, policyName); //Get uprating factor for a given simulated year and given policy
				disposableIncomeUprated.put(yearSimulated, policyName, sumOfOccupantsMonthlyDisposableIncome.get(policyName)*upratingFactor); //Disposable income for a given policy uprated to each possible simulated year
			}
			 */

			//Earnings are independent of the policy so enough to keep in a double => Not anymore, all the monetary variables are policy-dependent now
//			grossEarnings = sumOfOccupantsMonthlyGrossEarnings;
			grossEarnings.put(policyName, sumOfOccupantsMonthlyGrossEarnings.get(policyName));

			grossIncome.put(policyName, sumOfOccupantsMonthlyOriginalIncome.get(policyName));
			
//			System.out.println("Donor HHID " + getKey().getId() + "Policy name: " + policyName + "Sum of occupants monthly disposable income " + sumOfOccupantsMonthlyDisposableIncome.get(policyName) + "Sum of occupants earnings " + sumOfOccupantsMonthlyGrossEarnings + "Sum of occupants monthly original income " + sumOfOccupantsMonthlyOriginalIncome + 
//					"dispIncomeToGrossEarningsRatio " + disposableIncomeToGrossEarningsRatio.get(policyName) + "dispToGrossIncomeRatio " + disposableToGrossIncomeRatio.get(policyName) + "Number of occupants " + occupants.size());
		}
		size = occupants.size();

		if(headOfTheHousehold.getDgn().equals(Gender.Female)) {
			female = headOfTheHousehold;
			femaleId = headOfTheHousehold.getKey().getId();
			if(female.getPartnerId() != null && male != null) {
				occupancy = Occupancy.Couple;
			}
			else occupancy = Occupancy.Single_Female;
		}
		else {
			male = headOfTheHousehold;
			maleId = headOfTheHousehold.getKey().getId();
			if(male.getPartnerId() != null && female != null) {
				occupancy = Occupancy.Couple;
			}
			else occupancy = Occupancy.Single_Male;
		}
	
	}
	
	protected void updateChildrenFields() {		//XXX: Should we add calculateSize method to be called by update()?  Or just rely on size being correctly incremented / decremented by add / remove methods?
		
		//Set child age
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
		
		for(DonorPerson child: children) {
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
					
				default:
					throw new IllegalArgumentException("Unsupported child age " + child.getDag() + " in DonorHousehold.updateChildrenFields()!");
			}
			
		}
		
		if(n_children_1 > 0 || n_children_2 > 0 || n_children_3 > 0) 
			d_children_3under = Indicator.True;
		else d_children_3under = Indicator.False; // This will be updated if a birth occurs.
		
		if ( n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0 || n_children_7 > 0  || n_children_8 > 0 || n_children_9 > 0 || n_children_10 > 0 || n_children_11 > 0 || n_children_12 > 0 ) 
			d_children_4_12 = Indicator.True;
		else d_children_4_12 = Indicator.False;
		
		//New fields for Labour Supply Utility Regression calculation
		if(n_children_1 > 0 || n_children_2 > 0) 
			d_children_2under = Indicator.True;
		else d_children_2under = Indicator.False; // This will be updated if a birth occurs.
		
		if (n_children_3 > 0 || n_children_4 > 0 || n_children_5 > 0 || n_children_6 > 0) 
			d_children_3_6 = Indicator.True;
		else d_children_3_6 = Indicator.False;

		if (n_children_7 > 0 || n_children_8 > 0 || n_children_9 > 0 || n_children_10 > 0 || n_children_11 > 0 || n_children_12 > 0) 
			d_children_7_12 = Indicator.True;
		else d_children_7_12 = Indicator.False;

		if (n_children_13 > 0 || n_children_14 > 0 || n_children_15 > 0 || n_children_16 > 0 || n_children_17 > 0) 
			d_children_13_17 = Indicator.True;
		else d_children_13_17 = Indicator.False;
		
	}

				
	////////////////////////////////////////////////////////////////////////////////
	//
	//	Override equals and hashCode to make unique DonorHousehold determined by Key.getId()
	//
	////////////////////////////////////////////////////////////////////////////////

	 @Override
	    public boolean equals(Object o) {

	        if (o == this) return true;
	        if (!(o instanceof DonorHousehold)) {
	            return false;
	        }

	        DonorHousehold h = (DonorHousehold) o;

	        boolean idIsEqual = new EqualsBuilder()
	                .append(key.getId(), h.key.getId())		//Add more fields to compare to check for equality if desired
	                .isEquals();

	        if(idIsEqual) {
		        //Throw an exception if there are household objects with the same id but different other fields as this should not
		        //be possible and suggests a problem with the input data, i.e. the input database with household information would
		        //have people in the same household, but the attributes (fields) of the household objects are different, despite 
		        //supposedly being the same household
		        //TODO: When finished designing household, ensure all fields are included below.
		        boolean allFieldsAreEqual = new EqualsBuilder()
		        		.append(key.getId(), h.key.getId())
//		        		.append(idHousehold, h.idHousehold)     	
						.append(occupancy, h.occupancy)
						.append(femaleId, h.femaleId)
						.append(female, h.female)
						.append(maleId, h.maleId)
						.append(male, h.male)
						.append(occupants, h.occupants)
						.append(children, h.children)
						.append(otherMembers, h.otherMembers)
//						.append(household_weight, h.household_weight)
						.append(region, h.region)
						.append(size, h.size)
						.append(d_children_3under, h.d_children_3under)
						.append(d_children_4_12, h.d_children_4_12)
						.append(d_children_2under, h.d_children_2under)
						.append(d_children_3_6, h.d_children_3_6)
						.append(d_children_7_12, h.d_children_7_12)
						.append(d_children_13_17, h.d_children_13_17)	            		            
						.append(n_children_0, h.n_children_0)
						.append(n_children_1, h.n_children_1)
						.append(n_children_2, h.n_children_2)
						.append(n_children_3, h.n_children_3)
						.append(n_children_4, h.n_children_4)
						.append(n_children_5, h.n_children_5)
						.append(n_children_6, h.n_children_6)
						.append(n_children_7, h.n_children_7)
						.append(n_children_8, h.n_children_8)
						.append(n_children_9, h.n_children_9)
						.append(n_children_10, h.n_children_10)
						.append(n_children_11, h.n_children_11)
						.append(n_children_12, h.n_children_12)
						.append(n_children_13, h.n_children_13)
						.append(n_children_14, h.n_children_14)
						.append(n_children_15, h.n_children_15)
						.append(n_children_16, h.n_children_16)
						.append(n_children_17, h.n_children_17)	  
						.append(disposableToGrossIncomeRatio, h.disposableToGrossIncomeRatio)
						.append(disposableIncomeToGrossEarningsRatio, h.disposableIncomeToGrossEarningsRatio)
		        		.isEquals();
		        if(!allFieldsAreEqual) {
		        	throw new IllegalArgumentException("Error - there are multiple household objects with the same id " + key.getId() + " but different fields!");
		        }
	        }	   
	        
	        return idIsEqual;
//	        return allFieldsAreEqual;
	    }

	    @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(key.getId())
//	                .append(idHousehold)	            	
//	                .append(occupancy)
//	                .append(femaleId)
//	                .append(female)
//	                .append(maleId)
//	                .append(male)
//	                .append(occupants)
//	                .append(children)
//	                .append(otherMembers)
//	                .append(household_weight)
//	                .append(region)
//	                .append(size)
//	                .append(d_children_3under)
//	                .append(d_children_4_12)
//	                .append(d_children_2under)
//	                .append(d_children_3_6)
//	                .append(d_children_7_12)
//	                .append(d_children_13_17)	            		            
//	                .append(n_children_0)
//	                .append(n_children_1)
//	                .append(n_children_2)
//	                .append(n_children_3)
//	                .append(n_children_4)
//	                .append(n_children_5)
//	                .append(n_children_6)
//	                .append(n_children_7)
//	                .append(n_children_8)
//	                .append(n_children_9)
//	                .append(n_children_10)
//	                .append(n_children_11)
//	                .append(n_children_12)
//	                .append(n_children_13)
//	                .append(n_children_14)
//	                .append(n_children_15)
//	                .append(n_children_16)
//	                .append(n_children_17)	  
//	                .append(disposableToGrossIncomeRatio)
//	                .append(disposableIncomeToGrossEarningsRatio)	                
	                .toHashCode();
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

	public DonorPerson getFemale() {
		return female;
	}

	public DonorPerson getMale() {
		return male;
	}

	public Region getRegion() {		
		return region;
	}

//	public double getHousehold_weight() {
//		return household_weight;
//	}
	
	public Set<DonorPerson> getChildren() {
		return children;
	}

	public Set<DonorPerson> getOtherMembers() {
		return otherMembers;
	}
	
	public Long getFemaleId() {
		return femaleId;
	}

	public Long getMaleId() {
		return maleId;
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

	public int getN_children_0() {
		return n_children_0;
	}

	public int getN_children_1() {
		return n_children_1;
	}

	public int getN_children_2() {
		return n_children_2;
	}

	public int getN_children_3() {
		return n_children_3;
	}

	public int getN_children_4() {
		return n_children_4;
	}

	public int getN_children_5() {
		return n_children_5;
	}

	public int getN_children_6() {
		return n_children_6;
	}

	public int getN_children_7() {
		return n_children_7;
	}

	public int getN_children_8() {
		return n_children_8;
	}

	public int getN_children_9() {
		return n_children_9;
	}

	public int getN_children_10() {
		return n_children_10;
	}

	public int getN_children_11() {
		return n_children_11;
	}

	public int getN_children_12() {
		return n_children_12;
	}

	public int getN_children_13() {
		return n_children_13;
	}

	public int getN_children_14() {
		return n_children_14;
	}

	public int getN_children_15() {
		return n_children_15;
	}

	public int getN_children_16() {
		return n_children_16;
	}

	public int getN_children_17() {
		return n_children_17;
	}

	public Occupancy getOccupancy() {
		return occupancy;
	}


	public double getDisposableToGrossIncomeRatio(String euromodPolicyYear) {
		return disposableToGrossIncomeRatio.get(euromodPolicyYear);
	}

	public double getDisposableToGrossIncomeRatio() {
		return disposableToGrossIncomeRatio.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}


	public double getDisposableIncomeToGrossEarningsRatio(String euromodPolicyYear) {
		return disposableIncomeToGrossEarningsRatio.get(euromodPolicyYear);
	}
	
	public double getDisposableIncomeToGrossEarningsRatio() {
		return disposableIncomeToGrossEarningsRatio.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}
	
	public double getDisposableIncome(String euromodPolicyName) {
		return disposableIncome.get(euromodPolicyName);
	}

	public double getGrossEarnings(String euromodPolicyName) {
		 return grossEarnings.get(euromodPolicyName);
	}

	public double getGrossIncome(String euromodPolicyYear) {
		return grossIncome.get(euromodPolicyYear);
	}

	/*
	public double getDisposableIncome() {
		return disposableIncome.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}
	*/

	public double getUpratingFactor() {
		return (double) Parameters.upratingFactorsMap.get(model.getYear(), model.getLabourMarket().getEUROMODpolicyNameForThisYear()); //Get uprating factor for a given simulated year and policy that applies in that year
	}

	public double getDisposableIncome() {
		return getDisposableIncome(model.getLabourMarket().getEUROMODpolicyNameForThisYear())*getUpratingFactor();
	}

	public double getGrossEarnings() {
		return getGrossEarnings(model.getLabourMarket().getEUROMODpolicyNameForThisYear())*getUpratingFactor();
	}

	public double getGrossIncome() {
     	return getGrossIncome(model.getLabourMarket().getEUROMODpolicyNameForThisYear())*getUpratingFactor();
	}


	public Set<DonorPerson> getOccupants() {
		return occupants;
	}

	public int getSize() {
		return size;
	}
	
}

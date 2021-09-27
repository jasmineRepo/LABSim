package labsim.model;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Transient;

import labsim.model.enums.Les_c4;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import labsim.data.Parameters;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import labsim.model.enums.Education;
import labsim.model.enums.Gender;
import labsim.model.enums.HealthStatus;

@Entity
public class DonorPerson
{
//	@Column(name="idperson")
//	public long idPerson;
	
	@Transient
	private final LABSimModel model;
	
	@Id
	private PanelEntityKey key;
	
	private int dag;

	@Column(name=Parameters.BENEFIT_UNIT_VARIABLE_NAME)
	private Long benefitUnitId;
	
	@Enumerated(EnumType.STRING)
	private Gender dgn;
	
	@Enumerated(EnumType.STRING)
	private Education education;
	
	@Enumerated(EnumType.STRING)
	private Les_c4 activity_status;

	@Column(name="idpartner")
	private Long partnerId;		//Note, must not use primitive long, as long cannot hold 'null' value, i.e. if the person has no partner

	@Column(name="idmother")
	private Long motherId;

	@Column(name="idfather")
	private Long fatherId;

	@Column(name="idhh")
	private long householdId;
	
	@Enumerated(EnumType.STRING)
	@Column(name="health")
	private HealthStatus healthStatus;
	
	@Column(name="hours_worked_weekly")
	private int hoursWorkedWeekly;
		
//	@Column(name="earnings_monthly")		//The EUROMOD output variable 'ils_earns', the (gross) monthly income from employment / self-employment related earnings.
//	private double earningsMonthlyGross;
	
//	@Column(name="original_income_monthly")			//The EUROMOD output variable 'ils_origy', the gross monthly income from all source not just work, including private pension, investment income, property income, private transfers received.  This is before taxes and benefits redistribute the wealth.
//	private double originalIncomeMonthly;
	
//	@Column(name="hourly_wage")			// ils_earns / (weeksPerMonth * lhw), where lhw is the weekly hours a person worked in EUROMOD input data, and ils_earns is the monthly earnings from EUROMOD output data (the summation of employment income, self-employment income and possibly other employment related earnings depending on the country)
//	private double hourlyWageGross;		//To be compared with the simulated Person objects' 'potentialEarnings' field
	
	
	//The variables are output from EUROMOD and so now can take on potentially several values, one for each EUROMOD policy scenario

	@Transient
	private Map<String, Double> earningsMonthlyGross;

	@Transient
	private Map<String, Double> originalIncomeMonthly;

	@Transient
	private Map<String, Double> hourlyWageGross;

	@Transient
	private Map<String, Double> disposableIncomeMonthly;
	
	@Transient
	private Map<String, Double> employerSocialInsurancePerHour;
	
//	@Transient
//	private Map<String, Double> selfEmploySocialInsurancePerHour;
	
	// ---------------------------------------------------------------------
	// Constructors
	// ---------------------------------------------------------------------

	//Used when loading the initial population from the input database
	public DonorPerson() {
		super();
//		key = new PanelEntityKey(idPerson);
		key = new PanelEntityKey();
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
		disposableIncomeMonthly = new LinkedHashMap<String, Double>();
		employerSocialInsurancePerHour = new LinkedHashMap<String, Double>();
		earningsMonthlyGross = new LinkedHashMap<>();
		originalIncomeMonthly = new LinkedHashMap<>();
		hourlyWageGross = new LinkedHashMap<>();
//		selfEmploySocialInsurancePerHour = new LinkedHashMap<String, Double>();
//		initializeMapAttributes();
   	}
	
	public void initializeMapAttributes() {
		//This is how we deal with variable number of attributes - as many as there are EUROMOD policies
        for(String policyName: Parameters.EUROMODpolicySchedule.values()) {
            disposableIncomeMonthly.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.DISPOSABLE_INCOME_VARIABLE_NAME));
    		employerSocialInsurancePerHour.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME));
//    		selfEmploySocialInsurancePerHour.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME));
        	earningsMonthlyGross.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.GROSS_EARNINGS_VARIABLE_NAME));
        	originalIncomeMonthly.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.ORIGINAL_INCOME_VARIABLE_NAME));
        	hourlyWageGross.put(policyName, model.getDonorPersonEUROMODpolicyDependentVariablesByIdPolicyNameAndAttribute().get(key.getId(), policyName, Parameters.HOURLY_WAGE_VARIABLE_NAME));
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
	        if (!(o instanceof DonorPerson)) {
	            return false;
	        }

	        DonorPerson p = (DonorPerson) o;

	        boolean idIsEqual = new EqualsBuilder()
	                .append(key.getId(), p.key.getId())		//Add more fields to compare to check for equality if desired
	                .isEquals();
	        
//	        if(idIsEqual) {
//		        //Throw an exception if there are household objects with the same id but different other fields as this should not
//		        //be possible and suggests a problem with the input data, i.e. the input database with household information would
//		        //have people in the same household, but the attributes (fields) of the household objects are different, despite 
//		        //supposedly being the same household
//		        boolean allFieldsAreEqual = new EqualsBuilder()
//		        		.append(key.getId(), p.key.getId())
//		        		.append(age, p.age)
//						.append(gender, p.gender)
//						.append(education, p.education)
//						.append(activity_status, p.activity_status)	        		
//		        		.append(partnerId, p.partnerId)
//		        		.append(motherId, p.motherId)
//		        		.append(fatherId, p.fatherId)
//		        		.append(householdId, p.householdId)
//		        		.append(hourlyWageGross, p.hourlyWageGross)
//		        		.append(idPerson, p.idPerson)
//	       				.append(workSector, p.workSector)
//		        		.append(hoursWorkedWeekly, p.hoursWorkedWeekly)
//		        		.append(disposableIncomeMonthly, p.disposableIncomeMonthly)
//		        		.append(earningsMonthlyGross, p.earningsMonthlyGross)
//		        		.append(originalIncomeMonthly, p.originalIncomeMonthly)
//		        		.append(employerSocialInsurancePerHour, p.employerSocialInsurancePerHour)	        		
//		        		.append(selfEmploySocialInsurancePerHour, p.selfEmploySocialInsurancePerHour)
//		        		.isEquals();
//		        
//		        if(!allFieldsAreEqual) {
//		        	throw new IllegalArgumentException("Error - there are multiple Person objects with the same id " + key.getId() + " but different fields!");
//		        }
//	 		}
	        
	        return idIsEqual;
//	        return allFieldsAreEqual;
	    }

	    @Override
	    public int hashCode() {
	        return new HashCodeBuilder(17, 37)
	                .append(key.getId())
//	        		.append(age)
//					.append(gender)
//					.append(education)
//					.append(activity_status)	        		
//	        		.append(partnerId)
//	        		.append(motherId)
//	        		.append(fatherId)
//	        		.append(householdId)
//	        		.append(hourlyWageGross)
//	        		.append(idPerson)
//       				.append(workSector)
//	        		.append(hoursWorkedWeekly)
//	        		.append(disposableIncomeMonthly)
//	        		.append(earningsMonthlyGross)
//	        		.append(originalIncomeMonthly)
//	        		.append(employerSocialInsurancePerHour)	        		
//	        		.append(selfEmploySocialInsurancePerHour)
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
	     
	public int getDag() {
		return dag;
	}

	public Gender getDgn() {
		return dgn;
	}

	public Les_c4 getActivity_status() {
		return activity_status;
	}		

	public Education getEducation() {
		return education;
	}
	
	public Long getPartnerId() {
		return partnerId;
	}

	public long getHouseholdId() {
		return householdId;
	}

	public Long getBenefitUnitId() {
		return benefitUnitId;
	}

	public void setBenefitUnitId(Long benefitUnitId) {
		this.benefitUnitId = benefitUnitId;
	}

	public Long getMotherId() {
		return motherId;
	}

	public Long getFatherId() {
		return fatherId;
	}

	public int getHoursWorkedWeekly() {
		return hoursWorkedWeekly;
	}
	
	public double getHourlyWageGross() {
		return hourlyWageGross.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}

	public double getHourlyWageGross(String euromodPolicyName) {
     	return hourlyWageGross.get(euromodPolicyName);
	}

	public double getMonthlyEarningsGross() {
		return earningsMonthlyGross.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}
	public double getMonthlyEarningsGross(String euromodPolicyName) {
		return earningsMonthlyGross.get(euromodPolicyName);
	}

	public double getOriginalIncomeMonthly() {
     	return originalIncomeMonthly.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}

	public double getOriginalIncomeMonthly(String euromodPolicyName) {
     	return originalIncomeMonthly.get(euromodPolicyName);
	}

	public double getDisposableIncomeMonthly() {		
		return disposableIncomeMonthly.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}
	
	public double getDisposableIncomeMonthly(String euromodPolicyName) {		
		return disposableIncomeMonthly.get(euromodPolicyName);
	}

	public double getEmployerSocialInsurancePerHour() {
		return employerSocialInsurancePerHour.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
	}

//	public double getSelfEmploySocialInsurancePerHour() {
//		return selfEmploySocialInsurancePerHour.get(model.getLabourMarket().getEUROMODpolicyNameForThisYear());
//	}

	public HealthStatus getHealthStatus() {
		return healthStatus;
	}

	
}
package labsim.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import labsim.model.enums.*;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.collections4.map.MultiKeyMap;

import labsim.data.Parameters;
//import experiment.LABSimObserver;
//import microsim.data.MultiKeyCoefficientMap;
import microsim.engine.SimulationEngine;
//import microsim.statistics.IDoubleSource;
//import microsim.statistics.ILongSource;


//Significantly simplified LabourMarket module. Should just contain simple labour supply for now and matching with euromod donor population.
public class LabourMarket {

	private final LABSimModel model;

//	private final LABSimObserver observer; //Not used but maybe we want to graph labour supply and earnings etc. in the observer

	private String EUROMODpolicyNameForThisYear;
	
	private MultiKeyMap<Object, LinkedHashSet<DonorHousehold>> euromodHouseholdsByMultiKeys; /*`euromodHouseholdsByMultiKeys' is a data structure in the LabourMarket class that is used
	to store the donor benefitUnits and quickly find the appropriate sub-population in the labour
	supply module.*/
	
	private LinkedHashMap<Region, Set<BenefitUnit>> benefitUnitsByRegion; //Don't think we care about regions, but maybe benefitUnits are matched by region?

	private Set<BenefitUnit> benefitUnitsAllRegions;

	private Map<Region, Double> disposableIncomesByRegion;

	Set<Person> persons;
	Set<BenefitUnit> benefitUnits;

	//Constructor:
	LabourMarket(Set<Person> persons, Set<BenefitUnit> benefitUnits, Set<DonorHousehold> euromodOutputHouseholds) {
		
		model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
//		observer = (LABSimObserver) SimulationEngine.getInstance().getManager(LABSimObserver.class.getCanonicalName());	//To allow intra-time-step updates of convergence plots
		this.persons = persons;
		this.benefitUnits = benefitUnits;
		EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(model.getYear());
		
		benefitUnitsByRegion = new LinkedHashMap<Region, Set<BenefitUnit>>();		//For use in labour market module
		benefitUnitsAllRegions = new LinkedHashSet<>();
		
		for(Region region : Parameters.getCountryRegions()) {
			benefitUnitsByRegion.put(region, new LinkedHashSet<BenefitUnit>());
		}
		

        
		/* euromodHouseholdsByMultiKeys is a data structure to store EUROMOD benefitUnits and quickly find the appropriate sub-population in the labour supply module.
		 * Note that the donor benefitUnits will be added to more than one LinkedHashSet (the value of the MultiKeyMap).  This is to enable
		 * a varying level of requirement to match simulated benefitUnits to donor benefitUnits.  For example, if there is no such
		 * donor household that matches all the required characteristics of the simulated household, then we relax (ignore) some 
		 * characteristics and subsequently perform a search for the household with the nearest characteristics that were ignored.  
		 * We relax in groups of characteristics, where the higher the key number (e.g. key3 is higher than key1),
		 * the less priority we give to matching exactly such characteristics in the donor benefitUnits, and therefore we will ignore
		 * keys with higher numbers first (so ignore key3, try searching for donor benefitUnits with just key1 and key2, if still no
		 * donor household matches, ignore key2 and just use key1 to match.  LabourKey is essential for the match and is not relaxed. 
		 * When there are two people in the household, by convention we list the male first, so e.g. 
		 * 
		 * The multiple keys in the MultiKeyMap are as follows:
		 *	LabourKey: MultiKey<Labour> - By convention, in the case of a couple the male keys go first, so it will be MultiKey[Labour_Male, Labour_Female] in the case of a couple occupying the household, or just Labour in the case of a single person (whose gender is stored in key2). 
		 *  
		 *  key1: containing the primary characteristics of EUROMOD person and household for the labour supply module:
		 *  	Region of the household,		 *  		
		 *  		 
		 *  key2: Object containing secondary characteristics for the labour supply module: 	
		 *  	Health status
		 *
		 *  key3: Object containing tertiary characteristics for the labour supply module:
		 *  	Integer Number of Children.
		 *  
		 *  key4: Object containing 4th most important characteristics for the labour supply module:
		 *  	Integer Age for singles or MultiKey<Integer>[Age_Male, Age_Female] for couples.  This is the first characteristic to relax
		 *  	if no exact matching donor benefitUnits can be found.
		 *  
		 *  Note that the difference between the potential earnings of the simulated responsible persons in the household and
		 *  the gross hourly wage will also be minimised within the program, if there are more than one most similar donor benefitUnits, as determined
		 *  by the characteristics in the keys. 
		 */
		euromodHouseholdsByMultiKeys = MultiKeyMap.multiKeyMap(new LinkedMap<>());
        for(DonorHousehold house: euromodOutputHouseholds) {

        	DonorKeys keys = new DonorKeys(house);

        	LinkedHashSet<DonorHousehold> relevantHouseholdSet0 = euromodHouseholdsByMultiKeys.get(keys.labourKey);
        	LinkedHashSet<DonorHousehold> relevantHouseholdSet1 = euromodHouseholdsByMultiKeys.get(keys.labourKey, keys.key1);
        	LinkedHashSet<DonorHousehold> relevantHouseholdSet2 = euromodHouseholdsByMultiKeys.get(keys.labourKey, keys.key1, keys.key2);
        	LinkedHashSet<DonorHousehold> relevantHouseholdSet3 = euromodHouseholdsByMultiKeys.get(keys.labourKey, keys.key1, keys.key2, keys.key3);
        	LinkedHashSet<DonorHousehold> relevantHouseholdSet4 = euromodHouseholdsByMultiKeys.get(keys.labourKey, keys.key1, keys.key2, keys.key3, keys.key4);

        	//If relevantHouseholdSets do not exist, then create and put in the euromodHouseholdsByMultiKeys MultiKeyMap.
        	if(relevantHouseholdSet0 == null) {
        		relevantHouseholdSet0 = new LinkedHashSet<DonorHousehold>();
        		euromodHouseholdsByMultiKeys.put(keys.labourKey, relevantHouseholdSet0);
        	}
        	if(relevantHouseholdSet1 == null) {
        		relevantHouseholdSet1 = new LinkedHashSet<DonorHousehold>();
        		euromodHouseholdsByMultiKeys.put(keys.labourKey, keys.key1, relevantHouseholdSet1);
        	}
        	if(relevantHouseholdSet2 == null) {
        		relevantHouseholdSet2 = new LinkedHashSet<DonorHousehold>();
        		euromodHouseholdsByMultiKeys.put(keys.labourKey, keys.key1, keys.key2, relevantHouseholdSet2);
        	}
        	if(relevantHouseholdSet3 == null) {
        		relevantHouseholdSet3 = new LinkedHashSet<DonorHousehold>();
        		euromodHouseholdsByMultiKeys.put(keys.labourKey, keys.key1, keys.key2, keys.key3, relevantHouseholdSet3);
        	}
        	if(relevantHouseholdSet4 == null) {
        		relevantHouseholdSet4 = new LinkedHashSet<DonorHousehold>();
        		euromodHouseholdsByMultiKeys.put(keys.labourKey, keys.key1, keys.key2, keys.key3, keys.key4, relevantHouseholdSet4);
        	}
        	//Add house to each MultiKey set to allow for easy search at different levels
        	relevantHouseholdSet0.add(house);		// match to labourKey
        	relevantHouseholdSet1.add(house);		// match to labourKey and key1
        	relevantHouseholdSet2.add(house);		// match to labourKey and key1, key2
        	relevantHouseholdSet3.add(house); 		// match to labourKey and key1, key2, key3
        	relevantHouseholdSet4.add(house); 		// match to labourKey and key1, key2, key3, key4
        }


		for(Labour labourPerson1 : Labour.returnChoicesAllowedForGender(Gender.Male)) {
			MultiKey<Labour> labourKey = new MultiKey<>(labourPerson1, null);
			LinkedHashSet<DonorHousehold> labourKeyMatchedHouseholdSet = euromodHouseholdsByMultiKeys.get(labourKey);
			if(labourKeyMatchedHouseholdSet == null) {
				throw new IllegalStateException("Error - There are no EUROMOD benefitUnits that match the labourKey: " + labourKey);
			}
			for(Labour labourPerson2 : Labour.returnChoicesAllowedForGender(Gender.Female)) {
				labourKey = new MultiKey<>(labourPerson1, labourPerson2);
				labourKeyMatchedHouseholdSet = euromodHouseholdsByMultiKeys.get(labourKey);
				if(labourKeyMatchedHouseholdSet == null) {
					throw new IllegalStateException("Error - There are no EUROMOD benefitUnits that match the labourKey: " + labourKey);
				}
			}
		}
		//Now check for single females
		for(Labour labourPerson1 : Labour.returnChoicesAllowedForGender(Gender.Female)) {
			MultiKey<Labour> labourKey = new MultiKey<>(labourPerson1, null);
			LinkedHashSet<DonorHousehold> labourKeyMatchedHouseholdSet = euromodHouseholdsByMultiKeys.get(labourKey);
			if(labourKeyMatchedHouseholdSet == null) {
				throw new IllegalStateException("Error - There are no EUROMOD benefitUnits that match the labourKey: " + labourKey);
			}
		}

	}
	
	
	protected void update(int year) {		//Regions are considered separately
		EUROMODpolicyNameForThisYear = Parameters.getEUROMODpolicyForThisYear(year);	//Update EUROMOD policy to apply to this year
		
		//Update householdsByRegion
		for(Region region: Parameters.getCountryRegions()) {
			benefitUnitsByRegion.get(region).clear();			//Clear to allow updating
		}

		benefitUnitsAllRegions.clear();

		for(BenefitUnit benefitUnit : benefitUnits) {
			if(benefitUnit.getAtRiskOfWork()) { //Update HHs at risk of work, i.e. either of the adults are not under age, retired, student, in bad health
				benefitUnitsByRegion.get(benefitUnit.getRegion()).add(benefitUnit);		//This is the collection of benefitUnits that will enter the labour market
				benefitUnitsAllRegions.add(benefitUnit);
			}								
		}
		
//		for(Region region : Parameters.getCountryRegions()) {		//Regions are considered separately in labour market module
			
//			Set<BenefitUnit> benefitUnitsInRegion = benefitUnitsByRegion.get(region);

			
			//Run the Labour Supply Module (S.2 in Algorithm 1 of Matteo's Labour Market Module document)
			
			//Update Labour Supply 
			for(BenefitUnit benefitUnit : benefitUnitsAllRegions) {
				//Given current wage equation coefficients, benefitUnits update their supply of labour
				benefitUnit.updateLabourSupply();
			}
			
			Map<Education, Double> potentialEarningsByEdu = new LinkedHashMap<Education, Double>();
			Map<Education, Integer> countByEdu = new LinkedHashMap<Education, Integer>();
			
			for(Education ed: Education.values()) {
				potentialEarningsByEdu.put(ed, 0.);
				countByEdu.put(ed, 0);
			}
			
			for(BenefitUnit house: benefitUnitsAllRegions) {
				if(house.getMale() != null) {
					Person single = house.getMale();
					Education ed = single.getDeh_c3();
					double newVal = house.getMale().getPotentialEarnings();
					potentialEarningsByEdu.put(ed, potentialEarningsByEdu.get(ed) + newVal);
					int oldCount = countByEdu.get(ed);
					countByEdu.put(ed, oldCount + 1);
				}
				if(house.getFemale() != null) {
					Person single = house.getFemale();
					Education ed = single.getDeh_c3();
					double newVal = house.getFemale().getPotentialEarnings();
					potentialEarningsByEdu.put(ed, potentialEarningsByEdu.get(ed) + newVal);
					int oldCount = countByEdu.get(ed);
					countByEdu.put(ed, oldCount + 1);
				}						
			}
			
			/*
			//Now calculate using different list
			for(Education edu: Education.values()) {
				countByEdu.put(edu, 0);
				potentialEarningsByEdu.put(edu, 0.);
			}
			for(Person single: persons) {
				if(single.getRegion().equals(region)) {
					Education ed = single.getDeh_c3();
					double newVal = single.getPotentialEarnings();
					potentialEarningsByEdu.put(ed, potentialEarningsByEdu.get(ed) + newVal);
					int oldCount = countByEdu.get(ed);
					countByEdu.put(ed, oldCount + 1);
				}
			}

			
			if(year == model.getStartYear()) {
				if(Parameters.getDemandAdjustment().equals(DemandAdjustment.IncomeGrowth)) {
					double income = 0.;
					for(BenefitUnit house: benefitUnitsByRegion.get(region)) {
						income += house.getDisposableIncomeMonthly();				
					}
					disposableIncomesByRegion.put(region, income);		//Initialise, will be used to adjust labour demand initialisation on the next time-step
				}
			}
			*/
//		}
		
		for(BenefitUnit house: benefitUnits) {
			if(house.getMale() != null) {
				Person male = house.getMale();
				if(male.getLabourSupplyWeekly().getHours() > 0) {
					male.setLes_c4(Les_c4.EmployedOrSelfEmployed);
				}
				else if (!male.getLes_c4().equals(Les_c4.Student) && !male.getLes_c4().equals(Les_c4.Retired)){
					//No need to reset Retiree status
					male.setLes_c4(Les_c4.NotEmployed);
				}
			}
			if(house.getFemale() != null) {
				Person female = house.getFemale();
				if(female.getLabourSupplyWeekly().getHours() > 0) {
					female.setLes_c4(Les_c4.EmployedOrSelfEmployed);
				}
				else if (!female.getLes_c4().equals(Les_c4.Student) && !female.getLes_c4().equals(Les_c4.Retired)){ //No need to reset Retiree status
					female.setLes_c4(Les_c4.NotEmployed);
				}
			}
		}


		
System.out.println("Finished labour market module in " + year);
	}
	

	///////////////////////////////////////////////////////////////////////////////////////
	//
	//	Other Methods
	//
	///////////////////////////////////////////////////////////////////////////////////////


	//-------------------------------------------------------------------------
	//	Access Methods
	//-------------------------------------------------------------------------



	public MultiKeyMap<Object, LinkedHashSet<DonorHousehold>> getEuromodHouseholdsByMultiKeys() {
		return euromodHouseholdsByMultiKeys;
	}

	public String getEUROMODpolicyNameForThisYear() {
		return EUROMODpolicyNameForThisYear;
	}

	
}

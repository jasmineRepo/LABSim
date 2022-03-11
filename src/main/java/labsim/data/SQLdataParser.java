package labsim.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import labsim.model.enums.Country;
import labsim.model.enums.Region;

public class SQLdataParser {

	public static void createDonorDatabaseTablesFromCSVFile(Country country, String inputFilename, int startYear, Connection conn) {
		SQLdataParser.parse(Parameters.INPUT_DIRECTORY + inputFilename + ".csv", inputFilename, true, conn, country, startYear);
	}

	public static void createDatabaseTablesFromCSVfile(Country country, String inputFilename, String initialInputFilename, int startYear, int endYear, Connection conn) {

			//Construct tables for Simulated Persons & Households (initial population)
			//Pass the initial population file to SQL parser and set initial = true
		//  SQLdataParser.parse(Parameters.INPUT_DIRECTORY + initialInputFilename + ".csv", initialInputFilename, false, conn, country, startYear, true);
			for (int year = startYear; year <= endYear; year++) {
				SQLdataParser.parse(Parameters.INPUT_DIRECTORY_INITIAL_POPULATIONS + initialInputFilename + "_" + year + ".csv", initialInputFilename, false, conn, country, year);
			}

	        //Construct tables for Simulated Persons & Households
//	        SQLdataParser.parse(Parameters.INPUT_DIRECTORY + inputFilename + ".csv", inputFilename, false, conn, country, startYear, false);

			//Construct tables for Donor Persons & Households
			SQLdataParser.parse(Parameters.INPUT_DIRECTORY + inputFilename + ".csv", inputFilename, true, conn, country, startYear);
	}

	//CREATE PERSON AND HOUSEHOLD TABLES IN INPUT DATABASE BY USING SQL COMMANDS ON EUROMOD POPULATION DATA
	//donorTables set to true means that this method is being used to create donor population tables, 
	//as opposed to the initial population for simulation
	private static void parse(String inputFileLocation, String inputFileName, boolean donorTables, Connection conn, Country country, int startyear) {
		
		//If initial set to true, do the following:
		if (!donorTables) {
			
			//Set name of tables
			String personTable = "person_" + country + "_" + startyear;
			String benefitUnitTable = "benefitUnit_" + country + "_" + startyear;
			String householdTable = "household_" + country + "_" + startyear;
			
//			Map<Integer, String> euromodPolicySchedule = Parameters.calculateEUROMODpolicySchedule(country);
			
//			String policyNameForStartYear = Parameters.getEUROMODpolicyForThisYear(startyear, euromodPolicySchedule);
			
			//Ensure no duplicate column names
			Set<String> inputPersonColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.EUROMOD_VARIABLES_PERSON_INITIAL));
			Set<String> inputBenefitUnitColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.EUROMOD_VARIABLES_BENEFIT_UNIT_INITIAL));
			Set<String> inputHouseholdColumnNameSet = new LinkedHashSet<>(Arrays.asList(Parameters.EUROMOD_VARIABLES_HOUSEHOLD_INITIAL));

			Statement stat = null;
		    try {
		        stat = conn.createStatement();
		        stat.execute(
		        		//SQL statements creating database tables go here
		        		//Refresh table
		        		"DROP TABLE IF EXISTS " + inputFileName + ";"
		        		+ "CREATE TABLE " + inputFileName + " AS SELECT * FROM CSVREAD(\'" + inputFileLocation + "\');"
		        		+ "DROP TABLE IF EXISTS " + personTable + ";"
		        		+ "CREATE TABLE " + personTable + " AS (SELECT " + stringAppender(inputPersonColumnNamesSet) + " FROM " + inputFileName + ");"
		        		//Add id column
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN idperson RENAME TO id;"
		        		//Add rest of PanelEntityKey
		        		+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
		        		+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
		        		
		        		//Rename EUROMOD variables
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dag RENAME TO age;"
		        		//Age of partner
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dagsp RENAME TO age_partner;"
		        		
		        		//Reclassify EUROMOD variables - may need to change data structure type otherwise SQL conversion error, so create new column of the correct type, map data from old column and drop old column
		        		//Country
						+ "ALTER TABLE " + personTable + " ADD country VARCHAR_IGNORECASE;"
						+ "UPDATE " + personTable + " SET country = \'" + country + "\' WHERE dct = " + country.getEuromodCountryCode() + ";"
						+ "ALTER TABLE " + personTable + " DROP COLUMN dct;"
						
		        		//Education
		        		+ "ALTER TABLE " + personTable + " ADD education VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education = 'Low' WHERE deh_c3 = 3;"
		        		+ "UPDATE " + personTable + " SET education = 'Medium' WHERE deh_c3 = 2;"
		        		+ "UPDATE " + personTable + " SET education = 'High' WHERE deh_c3 = 1;"
		        		//Note: Have to consider missing values as children don't have a level of education before they leave school
		        		+ "UPDATE " + personTable + " SET education = 'Low' WHERE deh_c3 = -9;" 
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN deh_c3;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education RENAME TO deh_c3;"
		        		
		        		//Education mother
		        		+ "ALTER TABLE " + personTable + " ADD education_mother VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_mother = 'Low' WHERE dehm_c3 = 3;"
		        		+ "UPDATE " + personTable + " SET education_mother = 'Medium' WHERE dehm_c3 = 2;"
		        		+ "UPDATE " + personTable + " SET education_mother = 'High' WHERE dehm_c3 = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dehm_c3;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_mother RENAME TO dehm_c3;"
		        		
		        		//Education father
		        		+ "ALTER TABLE " + personTable + " ADD education_father VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_father = 'Low' WHERE dehf_c3 = 3;"
		        		+ "UPDATE " + personTable + " SET education_father = 'Medium' WHERE dehf_c3 = 2;"
		        		+ "UPDATE " + personTable + " SET education_father = 'High' WHERE dehf_c3 = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dehf_c3;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_father RENAME TO dehf_c3;"
		        		
		        		//Education partner
		        		+ "ALTER TABLE " + personTable + " ADD education_partner VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_partner = 'Low' WHERE dehsp_c3 = 3;"
		        		+ "UPDATE " + personTable + " SET education_partner = 'Medium' WHERE dehsp_c3 = 2;"
		        		+ "UPDATE " + personTable + " SET education_partner = 'High' WHERE dehsp_c3 = 1;"
		        		//Note: Have to consider missing values as for single persons partner's education is undefined
		        		+ "UPDATE " + personTable + " SET education_partner = null WHERE dehsp_c3 = -9;" 
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dehsp_c3;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_partner RENAME TO dehsp_c3;"
		        		
		        		//In education dummy (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD education_in VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_in = 'False' WHERE ded = 0;"
		        		+ "UPDATE " + personTable + " SET education_in = 'True' WHERE ded = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN ded;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_in RENAME TO ded;"
		        		
		        		//Return to education dummy (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD education_return VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_return = 'False' WHERE der = 0;"
		        		+ "UPDATE " + personTable + " SET education_return = 'True' WHERE der = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN der;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_return RENAME TO der;"
		        		
		        		//Gender
		        		+ "ALTER TABLE " + personTable + " ADD gender VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET gender = 'Female' WHERE dgn = 0;"
		        		+ "UPDATE " + personTable + " SET gender = 'Male' WHERE dgn = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dgn;"           
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN gender RENAME TO dgn;"
		        		
		        		//Weights
						+"ALTER TABLE " + personTable + " ALTER COLUMN dwt RENAME TO person_weight;"
						
		        		//Labour Market Economic Status
		        		+ "ALTER TABLE " + personTable + " ADD activity_status VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET activity_status = 'EmployedOrSelfEmployed' WHERE les_c4 = 1;"
		        		+ "UPDATE " + personTable + " SET activity_status = 'Student' WHERE les_c4 = 2;"
		        		+ "UPDATE " + personTable + " SET activity_status = 'NotEmployed' WHERE les_c4 = 3;"
						+ "UPDATE " + personTable + " SET activity_status = 'Retired' WHERE les_c4 = 4;"
		        		//Children below 16 are set to students in the data and children who are 16 or 17 and not students are set to not employed so there should not be missing values
//		        		+ "UPDATE " + personTable + " SET activity_status = 'Missing' WHERE les_c3 = -9;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN les_c4;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status RENAME TO les_c4;"
		        		
		        		//Partner's Labour Market Economic Status
		        		+ "ALTER TABLE " + personTable + " ADD activity_status_partner VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET activity_status_partner = 'EmployedOrSelfEmployed' WHERE lessp_c4 = 1;"
		        		+ "UPDATE " + personTable + " SET activity_status_partner = 'Student' WHERE lessp_c4 = 2;"
		        		+ "UPDATE " + personTable + " SET activity_status_partner = 'NotEmployed' WHERE lessp_c4 = 3;"
						+ "UPDATE " + personTable + " SET activity_status_partner = 'Retired' WHERE lessp_c4 = 4;"
		        		//Null values because not everyone has a partner
		        		+ "UPDATE " + personTable + " SET activity_status_partner = null WHERE lessp_c4 = -9;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN lessp_c4;"
						+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status_partner RENAME TO lessp_c4;"
		        		
		        		//Own and partner's Labour Market Economic Status
		        		+ "ALTER TABLE " + personTable + " ADD activity_status_couple VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET activity_status_couple = 'BothEmployed' WHERE lesdf_c4 = 1;"
		        		+ "UPDATE " + personTable + " SET activity_status_couple = 'EmployedSpouseNotEmployed' WHERE lesdf_c4 = 2;"
		        		+ "UPDATE " + personTable + " SET activity_status_couple = 'NotEmployedSpouseEmployed' WHERE lesdf_c4 = 3;"
		        		+ "UPDATE " + personTable + " SET activity_status_couple = 'BothNotEmployed' WHERE lesdf_c4 = 4;"
		        		//Null values because not everyone has a partner
		        		+ "UPDATE " + personTable + " SET activity_status_couple = null WHERE lesdf_c4 = -9;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN lesdf_c4;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN activity_status_couple RENAME TO lesdf_c4;"
		        		
		        		//Health is continuous: 
//		        		+"ALTER TABLE " + personTable + " ALTER COLUMN dhe RENAME TO health;"
		        		
		        		//Partner's health is continuous: 
//		        		+"ALTER TABLE " + personTable + " ALTER COLUMN dhesp RENAME TO health_partner;"
		        		
		        		/*
		        		//Health status
						+ "ALTER TABLE " + personTable + " ADD health VARCHAR_IGNORECASE;"
			        	+ "UPDATE " + personTable + " SET health = 'Poor' WHERE dhe = 1;"
			        	+ "UPDATE " + personTable + " SET health = 'Fair' WHERE dhe = 2;"
			        	+ "UPDATE " + personTable + " SET health = 'Good' WHERE dhe = 3;"
			        	+ "UPDATE " + personTable + " SET health = 'VeryGood' WHERE dhe = 4;"
			        	+ "UPDATE " + personTable + " SET health = 'Excellent' WHERE dhe = 5;"
			        	//Null values because of children 
			        	+ "UPDATE " + personTable + " SET health = 'Missing' WHERE dhe = -9;"
			        	+ "ALTER TABLE " + personTable + " DROP COLUMN dhe;"
		            	
			        	//Health status of partner
						+ "ALTER TABLE " + personTable + " ADD health_partner VARCHAR_IGNORECASE;"
			        	+ "UPDATE " + personTable + " SET health_partner = 'Poor' WHERE dhesp = 1;"
			        	+ "UPDATE " + personTable + " SET health_partner = 'Fair' WHERE dhesp = 2;"
			        	+ "UPDATE " + personTable + " SET health_partner = 'Good' WHERE dhesp = 3;"
			        	+ "UPDATE " + personTable + " SET health_partner = 'Very_good' WHERE dhesp = 4;"
			        	+ "UPDATE " + personTable + " SET health_partner = 'Excellent' WHERE dhesp = 5;"
			        	//Null values because of single persons 
			        	+ "UPDATE " + personTable + " SET health_partner = null WHERE dhesp = -9;"
			        	+ "ALTER TABLE " + personTable + " DROP COLUMN dhesp;"
			        	*/
		        		
			        	//Partnership status
			        	+ "ALTER TABLE " + personTable + " ADD partnership_status VARCHAR_IGNORECASE;"
			        	+ "UPDATE " + personTable + " SET partnership_status = 'Partnered' WHERE dcpst = 1;"
			        	+ "UPDATE " + personTable + " SET partnership_status = 'SingleNeverMarried' WHERE dcpst = 2;"
			        	+ "UPDATE " + personTable + " SET partnership_status = 'PreviouslyPartnered' WHERE dcpst = 3;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dcpst;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_status RENAME TO dcpst;"
		        		
		        		//Enter partnership dummy (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD partnership_enter VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET partnership_enter = 'False' WHERE dcpen = 0;"
		        		+ "UPDATE " + personTable + " SET partnership_enter = 'True' WHERE dcpen = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dcpen;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_enter RENAME TO dcpen;" 
		        		
		        		//Exit partnership dummy (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD partnership_exit VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET partnership_exit = 'False' WHERE dcpex = 0;"
		        		+ "UPDATE " + personTable + " SET partnership_exit = 'True' WHERE dcpex = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dcpex;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN partnership_exit RENAME TO dcpex;" 
		        		
		        		//Years in partnership
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dcpyy RENAME TO partnership_years;"
		        		
		        		//Age difference in couple
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dcpagdf RENAME TO partnership_age_diff;"
		        		
		        		//Number of children aged 0-2
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dnc02 RENAME TO children_02_number;"
		        		
		        		//Number of children
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dnc RENAME TO children_number;"
		        		
		        		//INCOME: Gross personal non-benefit income
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN ypnbihs_dv RENAME TO income_personal_nonbenefit;"
		        		
		        		//INCOME : Gross personal non-employment, non-benefit income
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yptciihs_dv RENAME TO income_personal_nonbenefit_nonemployment;"
		        		
		        		//INCOME : Gross personal employment income
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yplgrs_dv RENAME TO income_personal_employment;"
		        		
		        		//INCOME : Difference between own and partner gross personal non-benefit income
//		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN ynbcpdf_dv RENAME TO income_difference_partner;"
		        		
		        		//DEMOGRAPHIC: Long-term sick or disabled (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD sick_longterm VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET sick_longterm = 'False' WHERE dlltsd = 0;"
		        		+ "UPDATE " + personTable + " SET sick_longterm = 'True' WHERE dlltsd = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN dlltsd;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN sick_longterm RENAME TO dlltsd;"
		        		
		        		//SYSTEM: Year left education (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD education_left VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_left = 'False' WHERE sedex = 0;"
		        		+ "UPDATE " + personTable + " SET education_left = 'True' WHERE sedex = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN sedex;"
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN education_left RENAME TO sedex;" //Getting data conversion error trying to directly change values of sedex
		        		
		        		//SYSTEM: In same-sex partnership (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD partnership_samesex VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET partnership_samesex = 'False' WHERE ssscp = 0;"
		        		+ "UPDATE " + personTable + " SET partnership_samesex = 'True' WHERE ssscp = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN ssscp;"
		        		
		        		//SYSTEM: Women in fertility range (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD women_fertility VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET women_fertility = 'False' WHERE sprfm = 0;"
		        		+ "UPDATE " + personTable + " SET women_fertility = 'True' WHERE sprfm = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN sprfm;"
		        		
		        		//SYSTEM: In educational age range (to be used with Indicator enum when defined in Person class)
		        		+ "ALTER TABLE " + personTable + " ADD education_inrange VARCHAR_IGNORECASE;"
		        		+ "UPDATE " + personTable + " SET education_inrange = 'False' WHERE sedag = 0;"
		        		+ "UPDATE " + personTable + " SET education_inrange = 'True' WHERE sedag = 1;"
		        		+ "ALTER TABLE " + personTable + " DROP COLUMN sedag;"

						//Adult child flag:
						+ "ALTER TABLE " + personTable + " ADD adult_child VARCHAR_IGNORECASE;"
						+ "UPDATE " + personTable + " SET adult_child = 'False' WHERE adultchildflag = 0;"
						+ "UPDATE " + personTable + " SET adult_child = 'True' WHERE adultchildflag = 1;"
						+ "ALTER TABLE " + personTable + " DROP COLUMN adultchildflag;"
						+ "ALTER TABLE " + personTable + " ALTER COLUMN adult_child RENAME TO adultchildflag;"

						//Homeownership
						+ "ALTER TABLE " + personTable + " ADD dhh_owned_add VARCHAR_IGNORECASE;"
						+ "UPDATE " + personTable + " SET dhh_owned_add = 'False' WHERE dhh_owned = 0;"
						+ "UPDATE " + personTable + " SET dhh_owned_add = 'True' WHERE dhh_owned = 1;"
						+ "ALTER TABLE " + personTable + " DROP COLUMN dhh_owned;"
						+ "ALTER TABLE " + personTable + " ALTER COLUMN dhh_owned_add RENAME TO dhh_owned;"

								//SYSTEM : Year
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN stm RENAME TO system_year;"
		        		
		        		//SYSTEM : Data collection wave
		        		+ "ALTER TABLE " + personTable + " ALTER COLUMN swv RENAME TO system_wave;"
		        		
			    		+ "ALTER TABLE " + personTable + " ALTER COLUMN lhw RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";"
			    		+ "ALTER TABLE " + personTable + " ADD potential_earnings_hourly NUMERIC DEFAULT 0;"            					
//	            		+ "ALTER TABLE " + personTable + " ADD unit_labour_cost NUMERIC DEFAULT 0;"
//	            		+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_earns NUMERIC;"
//	            		+ "UPDATE " + personTable + " SET potential_earnings_hourly = ils_earns / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;"		//EUROMODmodellingConventions.pdf: "all income and expenditure data must be expressed in monthly terms", therefore we divide by 4.348214286 * lhw to make this hourly expenditure.
	            		
	            		
//            			+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_sicer_" + policyNameForStartYear + " NUMERIC;"		//Monthly employer social insurance contribution
//            			+ "UPDATE " + personTable + " SET unit_labour_cost = (ils_earns + ils_sicer_" + policyNameForStartYear + " ) / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;"		//EUROMODmodellingConventions.pdf: "all income and expenditure data must be expressed in monthly terms", therefore we divide by 4.348214286 * lhw to make this hourly expenditure.
            			+ "ALTER TABLE " + personTable + " ADD work_sector VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.     
//    					+ "ALTER TABLE " + personTable + " ALTER COLUMN yem NUMERIC;"
//   	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yse NUMERIC;"            		

    	        		//TODO: Check whether we should re-install the check of activity_status = 'Employed' for definitions below, and potentially add a 'Null' value to handle cases where people are not employed.
 //   	        		+ "UPDATE " + personTable + " SET work_sector = 'Self_Employed' WHERE abs(yem) < abs(yse);"		//Size of earnings derived from self-employment income (including declared self-employment losses) is larger than employment income (or loss - although while yse is sometimes negative, I'm not sure if yem is ever negative), so define as self-employed. 
 //   	        		+ "UPDATE " + personTable + " SET work_sector = 'Public_Employee' WHERE lcs = 1;"		//Lastly, regardless of yem or yse, if lcs = 1, indicates person is s civil servant so overwrite any value with 'Public_Employee' work_sector value.            		
    	        		
    	//        		+ "UPDATE " + personTable + " SET earnings = yem + yse;"		//Now use EUROMOD output ils_earns, which includes more than just yem + yse, depending on the country (e.g. in Italy there is a temporary employment field yemtj 'income from co.co.co.').
 //   	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yem RENAME TO employment_income;"
 //   	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yse RENAME TO self_employment_income;"	
//    	        		+ "ALTER TABLE " + personTable + " DROP COLUMN lcs;"
    	        		
    	        		//Set ids to null where zero (EUROMOD sets empty/blank data as 0)
//    	        		+ "UPDATE " + personTable + " SET id = null WHERE id = 0;"
//    	        		+ "UPDATE " + personTable + " SET idhh = null WHERE idhh = 0;"
    	        		+ "UPDATE " + personTable + " SET idpartner = null WHERE idpartner = -9;"
    	        		+ "UPDATE " + personTable + " SET idmother = null WHERE idmother = -9;"
    	        		+ "UPDATE " + personTable + " SET idfather = null WHERE idfather = -9;"

						//Rename idbenefitunit to BU_ID
						+ "ALTER TABLE " + personTable + " ALTER COLUMN idbenefitunit RENAME TO " + Parameters.BENEFIT_UNIT_VARIABLE_NAME + ";"

						//Id of the household is loaded from the input population without any modification as idhh

    	        		//Re-order by id
    	        		+ "SELECT * FROM " + personTable + " ORDER BY id;"
    	        		+ "");
		        
		        	//BenefitUnit table:
	    			stat.execute(

	        		"DROP TABLE IF EXISTS " + benefitUnitTable + ";"
	        		//Create household table with columns representing EUROMOD variables listed in Parameters class EUROMOD_VARIABLES_HOUSEHOLD set. 
	        		+ "CREATE TABLE " + benefitUnitTable + " AS (SELECT " + stringAppender(inputBenefitUnitColumnNamesSet) + " FROM " + inputFileName + ");"
	        		+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
	        		+ "ALTER TABLE " + benefitUnitTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
	        		
					+ "ALTER TABLE " + benefitUnitTable + " ADD region VARCHAR_IGNORECASE;"
	    		);

	    			stat.execute(
				  "DROP TABLE IF EXISTS " + householdTable + ";"
					+ "CREATE TABLE " + householdTable + " AS (SELECT " + stringAppender(inputHouseholdColumnNameSet) + " FROM " + inputFileName + ");"
					+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
					+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
				    + "ALTER TABLE " + householdTable + " DROP COLUMN idperson;"
					+ "ALTER TABLE " + householdTable + " ALTER COLUMN idhh RENAME TO id;"
				    + "ALTER TABLE " + householdTable + " ADD size NUMERIC;"
				    + "UPDATE " + householdTable + " SET size = -1;"
				    + "SELECT * FROM " + householdTable + " ORDER BY id;"
					);
	        	
		    		//Region - See Region class for mapping definitions and sources of info
		    		Parameters.setCountryRegions(country);
	            	for(Region region: Parameters.getCountryRegions()) {
	            		stat.execute(
	    	            		"UPDATE " + benefitUnitTable + " SET region = '" + region + "' WHERE drgn1 = " + region.getDrgn1EUROMODvariable() + ";"
	    	            	);
	            	}
	            	
	            	stat.execute(
	    	        		"ALTER TABLE " + benefitUnitTable + " DROP COLUMN drgn1;"
	    	        		+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idfather;"
	        	    	    + "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idmother;"
	        	    	    + "ALTER TABLE " + benefitUnitTable + " DROP COLUMN idperson;"
	    	        		
	    	        		//Rename EUROMOD variables
	    	        		+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN dwt RENAME TO household_weight;"
	    	        		
	    	        		//BenefitUnit composition
	    	        		+ "ALTER TABLE " + benefitUnitTable + " ADD household_composition VARCHAR_IGNORECASE;"
	    	        		+ "UPDATE " + benefitUnitTable + " SET household_composition = 'CoupleNoChildren' WHERE dhhtp_c4 = 1;"
	    	        		+ "UPDATE " + benefitUnitTable + " SET household_composition = 'CoupleChildren' WHERE dhhtp_c4 = 2;"
	    	        		+ "UPDATE " + benefitUnitTable + " SET household_composition = 'SingleNoChildren' WHERE dhhtp_c4 = 3;"
	    	        		+ "UPDATE " + benefitUnitTable + " SET household_composition = 'SingleChildren' WHERE dhhtp_c4 = 4;"
	    	        		+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN dhhtp_c4;"
	    	        		+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN household_composition RENAME TO dhhtp_c4;"
	    	        		
	    	        		//INCOME: BenefitUnit income - quintiles
							+ "ALTER TABLE " + benefitUnitTable + " ADD household_income_qtiles VARCHAR_IGNORECASE;"
							+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q1' WHERE ydses_c5 = 1;"
							+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q2' WHERE ydses_c5 = 2;"
							+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q3' WHERE ydses_c5 = 3;"
							+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q4' WHERE ydses_c5 = 4;"
							+ "UPDATE " + benefitUnitTable + " SET household_income_qtiles = 'Q5' WHERE ydses_c5 = 5;"
							+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN ydses_c5;"
							+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN household_income_qtiles RENAME TO ydses_c5;"

							//Homeownership
							+ "ALTER TABLE " + benefitUnitTable + " ADD dhh_owned_add VARCHAR_IGNORECASE;"
							+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'False' WHERE dhh_owned = 0;"
							+ "UPDATE " + benefitUnitTable + " SET dhh_owned_add = 'True' WHERE dhh_owned = 1;"
							+ "ALTER TABLE " + benefitUnitTable + " DROP COLUMN dhh_owned;"
							+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN dhh_owned_add RENAME TO dhh_owned;"

									//Set zeros to null where relevant
//	    	        		+ "UPDATE " + benefitUnitTable + " SET idhh = null WHERE idhh = 0;"
//	    	        		+ "UPDATE " + benefitUnitTable + " SET idmale = null WHERE idmale = 0;"
//	    	        		+ "UPDATE " + benefitUnitTable + " SET idfemale = null WHERE idfemale = 0;"

	    	        		//Rename id column
	    	        		+ "ALTER TABLE " + benefitUnitTable + " ALTER COLUMN idbenefitunit RENAME TO id;"
	    	        		
	    	        		//Re-order by id
	        	        	+ "SELECT * FROM " + benefitUnitTable + " ORDER BY id;"
	            		
	    	    		
	                				
	                		+"ALTER TABLE " + benefitUnitTable + " ADD size NUMERIC;"
	    					+ "UPDATE " + benefitUnitTable + " SET size = -1;"		//Give initial size an invalid number to prevent confusion with real size.  The household objects will update the size and persist it to the database, so only the initial database does not have the correct size.
	    				);
	                	
	                	
	    	        	//Remove duplicate rows in household tables (as they are derived from persons, there is one row per person, so for households with N people, there would be N rows with same data)
	    	        	stat.execute(
	    	        		"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + benefitUnitTable + " ORDER BY ID;"
	    	        		+ "DROP TABLE IF EXISTS " + benefitUnitTable + ";"
	    	        		+ "ALTER TABLE NEW RENAME TO " + benefitUnitTable + ";"
	    	        		);

						stat.execute(
								"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + householdTable + " ORDER BY ID;"
							+ "DROP TABLE IF EXISTS " + householdTable + ";"
							+ "ALTER TABLE NEW RENAME TO " + householdTable + ";"
							);
	    	        		
	    	    		stat.execute("DROP TABLE IF EXISTS " + inputFileName + ";");
		        
		    } catch(Exception e){
	       	//	 throw new IllegalArgumentException("SQL Exception thrown!" + e.getMessage());
	       		 e.printStackTrace();
		    }
		    finally {
		        try {
		        	if(stat != null)
		        		stat.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		} else { //else proceed as before so EM population_UK.csv is the input file now.

		// Set names of donor tables
		String	personTable = "DonorPerson_" + country;
		String	householdTable = "DonorHousehold_" + country;

		
		Map<Integer, String> euromodPolicySchedule = Parameters.calculateEUROMODpolicySchedule(country);
		
		String policyNameForStartYear = Parameters.getEUROMODpolicyForThisYear(startyear, euromodPolicySchedule);
		
		//Ensure no duplicate column names
		Set<String> inputPersonColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.EUROMOD_VARIABLES_PERSON));
		for(String filename: euromodPolicySchedule.values()) {
			for(String variable: Parameters.EUROMOD_POLICY_VARIABLES_PERSON) {
				inputPersonColumnNamesSet.add(variable + "_" + filename);
			}
		}
		Set<String> inputHouseholdColumnNamesSet = new LinkedHashSet<String>(Arrays.asList(Parameters.EUROMOD_VARIABLES_BENEFIT_UNIT));


		inputPersonColumnNamesSet.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));
		inputHouseholdColumnNamesSet.add((String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()));

		Statement stat = null;
	    try {
	        stat = conn.createStatement();
	        stat.execute(
	        		//Refresh table
	        		"DROP TABLE IF EXISTS " + inputFileName + ";"
	        		//Create new database table by reading in from population_country .txt file
	        		+ "CREATE TABLE " + inputFileName + " AS SELECT * FROM CSVREAD(\'" + inputFileLocation + "\');"		//Data to parse is stored in population_[country].csv file, so comma-separated file format, not .txt is used.
	        		//Create person table with columns representing EUROMOD variables listed in Parameters class EUROMOD_VARIABLES_PERSON set.
	        		
	        		//---------------------------------------------------------------------------
	        		//	Person and DonorPerson tables
	        		//---------------------------------------------------------------------------
	        		
	        		+ "DROP TABLE IF EXISTS " + personTable + ";"
	        		+ "CREATE TABLE " + personTable + " AS (SELECT " + stringAppender(inputPersonColumnNamesSet) + " FROM " + inputFileName + ");"
	        		//Add id column
	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN idperson RENAME TO id;"

	        		//Add rest of PanelEntityKey
	        		+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
	        		+ "ALTER TABLE " + personTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
	        		
	        		//Rename EUROMOD variables
//	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN dag RENAME TO age;"
	        		            		
	        		//Reclassify EUROMOD variables - may need to change data structure type otherwise SQL conversion error, so create new column of the correct type, map data from old column and drop old column
	        		//Country
					+ "ALTER TABLE " + personTable + " ADD country VARCHAR_IGNORECASE;"
					+ "UPDATE " + personTable + " SET country = \'" + country + "\' WHERE dct = " + country.getEuromodCountryCode() + ";"
					+ "ALTER TABLE " + personTable + " DROP COLUMN dct;"
					
	        		//Education
	        		+ "ALTER TABLE " + personTable + " ADD education VARCHAR_IGNORECASE;"
	        		+ "UPDATE " + personTable + " SET education = 'Low' WHERE deh < 2;"
	        		+ "UPDATE " + personTable + " SET education = 'Medium' WHERE deh >= 2 AND deh < 5;"
	        		+ "UPDATE " + personTable + " SET education = 'High' WHERE deh = 5;"
	        		+ "ALTER TABLE " + personTable + " DROP COLUMN deh;"
	
	        		//Gender
	        		+ "ALTER TABLE " + personTable + " ADD gender VARCHAR_IGNORECASE;"
	        		+ "UPDATE " + personTable + " SET gender = 'Female' WHERE dgn = 0;"
	        		+ "UPDATE " + personTable + " SET gender = 'Male' WHERE dgn = 1;"
	        		+ "ALTER TABLE " + personTable + " DROP COLUMN dgn;"  
	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN gender RENAME TO dgn;"
	        		
	        		
	        		//Weights
					+"ALTER TABLE " + personTable + " ALTER COLUMN dwt RENAME TO person_weight;"
					
	        		//Labour Market Economic Status
	        		+ "ALTER TABLE " + personTable + " ADD activity_status VARCHAR_IGNORECASE;"
	        		+ "UPDATE " + personTable + " SET activity_status = 'EmployedOrSelfEmployed' WHERE les >= 1 AND les <= 3;"
	        		+ "UPDATE " + personTable + " SET activity_status = 'Student' WHERE les = 0 OR les = 6;"
	        		+ "UPDATE " + personTable + " SET activity_status = 'Retired' WHERE les = 4;"
	        		+ "UPDATE " + personTable + " SET activity_status = 'NotEmployed' WHERE les = 5 OR les >= 7;"

					+ "ALTER TABLE " + personTable + " ADD health VARCHAR_IGNORECASE;"
		        	+ "UPDATE " + personTable + " SET health = 'Good' WHERE les != 8;"
		        	+ "UPDATE " + personTable + " SET health = 'Poor' WHERE les = 8;"
//		        	+ "UPDATE " + personTable + " SET health = 'Missing' WHERE les IS null;"
		        	+ "ALTER TABLE " + personTable + " DROP COLUMN les;"
	            			        		
		    		+ "ALTER TABLE " + personTable + " ALTER COLUMN lhw RENAME TO " + Parameters.HOURS_WORKED_WEEKLY + ";");
		    		//XXX: Could set " + Parameters.HOURS_WORKED_WEEKLY + ", earnings, labour cost etc. to 0 if retired.  However, the data does not conform - see idperson 101, who is retired pensioner aged 80, but who declares lhw = 40 i.e. works 40 hours per week and has a sizeable earnings and employer social contributions.
	
	        		if(donorTables) {		//Adjust variables that only exist in the output .text files of EUROMOD
//	            		stat.execute(
	            					            				
		            		//Original Income
//		            		"ALTER TABLE " + personTable + " ALTER COLUMN ils_origy RENAME TO original_income_monthly;"
		            		
		            		//Earnings
//		            		+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_earns RENAME TO earnings_monthly;"
//		            		+ "ALTER TABLE " + personTable + " ADD hourly_wage NUMERIC DEFAULT 0;"
//		            		+ "ALTER TABLE " + personTable + " ALTER COLUMN earnings_monthly NUMERIC;"
//		            		+ "UPDATE " + personTable + " SET hourly_wage = earnings_monthly / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;");		//EUROMODmodellingConventions.pdf: "all income and expenditure data must be expressed in monthly terms", therefore we divide by 4.348214286 * lhw to make this hourly wage, where 4.348214286 = 365.25/(12*7) is the week to month conversion rate as specified in Parameters class.

            			for(String name: euromodPolicySchedule.values()) {		//Adjust variables that are potentially affected by EUROMOD policy scenario (i.e. a different value can occur if certain tax / benefit policies are altered)
		            		stat.execute(
		            				
			            		//Disposable Income

									"ALTER TABLE " + personTable + " ADD hourly_wage_" + name + " NUMERIC DEFAULT 0;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_earns_" + name + " NUMERIC;"
								+ "UPDATE " + personTable + " SET hourly_wage_" + name + " = ils_earns_" + name + " / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN hourly_wage_" + name + " RENAME TO " + Parameters.HOURLY_WAGE_VARIABLE_NAME + "_" + name + ";"

								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_dispy_" + name + " RENAME TO " + Parameters.DISPOSABLE_INCOME_VARIABLE_NAME + "_" + name + ";"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_earns_" + name + " RENAME TO " + Parameters.GROSS_EARNINGS_VARIABLE_NAME + "_" + name + ";"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_origy_" + name + " RENAME TO " + Parameters.ORIGINAL_INCOME_VARIABLE_NAME + "_" + name + ";"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_benmt_" + name + " RENAME TO " + Parameters.ILS_BENMT_NAME + "_" + name + ";"
								+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_bennt_" + name + " RENAME TO " + Parameters.ILS_BENNT_NAME + "_" + name + ";"

			            		//Employer Social Insurance Contribution
			            		+ "ALTER TABLE " + personTable + " ADD " + Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + name + " NUMERIC DEFAULT 0;"	            		
			            		+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_sicer_" + name + " NUMERIC;"		//Monthly employer social insurance contribution
			            		+ "UPDATE " + personTable + " SET " + Parameters.EMPLOYER_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + name + " = ils_sicer_" + name + " / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;"		//EUROMODmodellingConventions.pdf: "all income and expenditure data must be expressed in monthly terms", therefore we divide by 4.348214286 * lhw to make this hourly expenditure.
			            		            		
//			            		//Self-Employed Social Insurance Contributions
//			            		+ "ALTER TABLE " + personTable + " ADD " + Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + name + " NUMERIC DEFAULT 0;"
//			            		+ "ALTER TABLE " + personTable + " ALTER COLUMN ils_sicse_" + name + " NUMERIC;"
//			            		+ "UPDATE " + personTable + " SET " + Parameters.SELF_EMPLOY_SOCIAL_INSURANCE_VARIABLE_NAME + "_" + name + " = ils_sicse_" + name + " / (" + Parameters.WEEKS_PER_MONTH_RATIO + " * " + Parameters.HOURS_WORKED_WEEKLY + ") WHERE " + Parameters.HOURS_WORKED_WEEKLY + " > 0;"		//EUROMODmodellingConventions.pdf: "all income and expenditure data must be expressed in monthly terms", therefore we divide by 4.348214286 * lhw to make this hourly expenditure.
			            		
			            		+ "ALTER TABLE " + personTable + " DROP COLUMN ils_sicer_" + name + ";"
//			    	    	    + "ALTER TABLE " + personTable + " DROP COLUMN ils_sicse_" + name + ";"
			            		);
		            	}
	            		
	    			}
	        		
	        		//Set zeros to null where relevant
	        		stat.execute(
			        		
	        		//Use yem and yse to define workStatus for non-civil-servants, i.e. those with lcs = 0?
	        		//If the person has absolute self-employment income > absolute employment income, define workStatus enum as Self_Employed as self-employment income or loss has a bigger effect on personal wealth than employment income (or loss).
	        		"ALTER TABLE " + personTable + " ADD work_sector VARCHAR_IGNORECASE DEFAULT 'Private_Employee';"		//Here we assume by default that people are employed - this is because the MultiKeyMaps holding households have work_sector as a key, and cannot handle null values for work_sector. TODO: Need to check that this assumption is OK.     
					+ "ALTER TABLE " + personTable + " ALTER COLUMN yem NUMERIC;"
	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yse NUMERIC;"            		

	        		//TODO: Check whether we should re-install the check of activity_status = 'Employed' for definitions below, and potentially add a 'Null' value to handle cases where people are not employed.
	        		+ "UPDATE " + personTable + " SET work_sector = 'Self_Employed' WHERE abs(yem) < abs(yse);"		//Size of earnings derived from self-employment income (including declared self-employment losses) is larger than employment income (or loss - although while yse is sometimes negative, I'm not sure if yem is ever negative), so define as self-employed. 
	        		+ "UPDATE " + personTable + " SET work_sector = 'Public_Employee' WHERE lcs = 1;"		//Lastly, regardless of yem or yse, if lcs = 1, indicates person is s civil servant so overwrite any value with 'Public_Employee' work_sector value.            		
	        		
	//        		+ "UPDATE " + personTable + " SET earnings = yem + yse;"		//Now use EUROMOD output ils_earns, which includes more than just yem + yse, depending on the country (e.g. in Italy there is a temporary employment field yemtj 'income from co.co.co.').
	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yem RENAME TO employment_income;"
	        		+ "ALTER TABLE " + personTable + " ALTER COLUMN yse RENAME TO self_employment_income;"	
	        		+ "ALTER TABLE " + personTable + " DROP COLUMN lcs;"
	        		
	        		//Set ids to null where zero (EUROMOD sets empty/blank data as 0)
	        		+ "UPDATE " + personTable + " SET id = null WHERE id = 0;"
	        		+ "UPDATE " + personTable + " SET idhh = null WHERE idhh = 0;"
	        		+ "UPDATE " + personTable + " SET idpartner = null WHERE idpartner = 0;"
	        		+ "UPDATE " + personTable + " SET idmother = null WHERE idmother = 0;"
	        		+ "UPDATE " + personTable + " SET idfather = null WHERE idfather = 0;"

					+ "ALTER TABLE " + personTable + " ALTER COLUMN " + (String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()) + " RENAME TO " + Parameters.BENEFIT_UNIT_VARIABLE_NAME + " ;"
					+ "UPDATE " + personTable + " SET " + Parameters.BENEFIT_UNIT_VARIABLE_NAME + " = null WHERE " + Parameters.BENEFIT_UNIT_VARIABLE_NAME + " = 0;"
	        		
	        		//Re-order by id
	        		+ "SELECT * FROM " + personTable + " ORDER BY id;"
	        		
					+ "");

	        		
	        		
	        	//----------------------------------------------------------------------------
	        	//	BenefitUnit and DonorHousehold tables
	        	//----------------------------------------------------------------------------
	        		
	    		stat.execute(

	        		"DROP TABLE IF EXISTS " + householdTable + ";"
	        		//Create household table with columns representing EUROMOD variables listed in Parameters class EUROMOD_VARIABLES_HOUSEHOLD set. 
	        		+ "CREATE TABLE " + householdTable + " AS (SELECT " + stringAppender(inputHouseholdColumnNamesSet) + " FROM " + inputFileName + ");"
	        		
	        		
//	        		//Label male and female of the household, based on their role as father or mother
	        		//XXX: We have stopped the setting of male/female ids based on role as father/mother
	        		// for 2 reasons: 1) There may be a father and mother of a child living in the same 
	        		// house but having broken up - they could no longer be defined as partners!?!  (though
	        		// I haven't checked whether this is allowed in the EUROMOD specifications, it is safer
	        		// to assume that it is possible and take it into account in our design). 2) Currently,
	        		// the household tables have semi-duplicate entries, given that they are constructed 
	        		// from the individual persons, so if there are 5 people in a house, there are 5 rows
	        		// in the household table and each person can have a different mother/father id. In 
	        		// order to remove duplicates, in our program we convert the list of households to 
	        		// a set, which will remove duplicate households (as measured by their ids only).
	        		// This is a fairly random way in obtaining one household from the many, and means
	        		// that the way in which we assign the male/female of the household is fairly random.
	        		// It would be better to start with the null male/female ids and build up programmatically
	        		// the relationships within Java.  Hence we assign only null values to male/female.
//					+ "ALTER TABLE " + householdTable + " ADD idmale NUMERIC;"
//					+ "ALTER TABLE " + householdTable + " ADD idfemale NUMERIC;"
//					+ "UPDATE " + householdTable + " SET idmale = idfather;"
//					+ "UPDATE " + householdTable + " SET idfemale = idmother;"
//					+ "UPDATE " + householdTable + " SET idmale = NULL;"
//					+ "UPDATE " + householdTable + " SET idfemale = NULL;"
					
					
	        		//Add rest of PanelEntityKey
	        		+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_time INT DEFAULT " + startyear + ";"
	        		+ "ALTER TABLE " + householdTable + " ADD COLUMN simulation_run INT DEFAULT 0;"
	        		
					+ "ALTER TABLE " + householdTable + " ADD region VARCHAR_IGNORECASE;"
	    		);
	        	
	    		//Region - See Region class for mapping definitions and sources of info
	    		Parameters.setCountryRegions(country);
            	for(Region region: Parameters.getCountryRegions()) {
            		stat.execute(
    	            		"UPDATE " + householdTable + " SET region = '" + region + "' WHERE drgn1 = " + region.getDrgn1EUROMODvariable() + ";"
    	            	);
            	}

            	stat.execute(
	        		"ALTER TABLE " + householdTable + " DROP COLUMN drgn1;"	  
	        		+ "ALTER TABLE " + householdTable + " DROP COLUMN idfather;"
    	    	    + "ALTER TABLE " + householdTable + " DROP COLUMN idmother;"
    	    	    + "ALTER TABLE " + householdTable + " DROP COLUMN idperson;"
	        		
	        		//Rename EUROMOD variables
	        		+ "ALTER TABLE " + householdTable + " ALTER COLUMN dwt RENAME TO household_weight;"

					//Rename id column
//	        		+ "ALTER TABLE " + householdTable + " ALTER COLUMN idhh RENAME TO id;"
					//Rename benefit unit id to household id

					+ "ALTER TABLE " + householdTable + " ALTER COLUMN " + (String) Parameters.getBenefitUnitVariableNames().getValue(country.getCountryName()) + " RENAME TO id;"

	        		//Set zeros to null where relevant
	        		+ "UPDATE " + householdTable + " SET idhh = null WHERE idhh = 0;"
//	        		+ "UPDATE " + householdTable + " SET idmale = null WHERE idmale = 0;"
//	        		+ "UPDATE " + householdTable + " SET idfemale = null WHERE idfemale = 0;"
					+ "UPDATE " + householdTable + " SET id = null WHERE id = 0;"

	        		//Re-order by id
    	        	+ "SELECT * FROM " + householdTable + " ORDER BY id;"
        		);
	    		
            	
	        	//Remove duplicate rows in household tables (as they are derived from persons, there is one row per person, so for households with N people, there would be N rows with same data)
	        	stat.execute(
	        		"CREATE TABLE NEW AS SELECT DISTINCT * FROM " + householdTable + " ORDER BY ID;"
	        		+ "DROP TABLE IF EXISTS " + householdTable + ";"
	        		+ "ALTER TABLE NEW RENAME TO " + householdTable + ";"
	        		);
	        		
	    		stat.execute("DROP TABLE IF EXISTS " + inputFileName + ";");		//Finally, drop the original EUROMOD input data
	    		
	    } catch(SQLException e){
       		 throw new IllegalArgumentException("SQL Exception thrown!" + e.getMessage());
	    }
	    finally {
	        try {
	        	if(stat != null)
	        		stat.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	}
}

	public static String stringAppender(Collection<String> strings) {
		Iterator<String> iter = strings.iterator();
		StringBuilder sb = new StringBuilder();
		while (iter.hasNext()) {
			sb
	//		.append("'")
			.append(iter.next())
	//		.append("'")
			;
			if (iter.hasNext())
			sb.append(",");
		}
		return sb.toString();
	}

}
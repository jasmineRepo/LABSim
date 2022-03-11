// define package
package labsim.experiment;

// import Java packages
import labsim.data.Parameters;
import microsim.data.MultiKeyCoefficientMap;
import microsim.data.excel.ExcelAssistant;
import microsim.engine.MultiRun;
import microsim.engine.SimulationEngine;
import microsim.gui.shell.MultiRunFrame;
import labsim.model.LABSimModel;
import labsim.model.enums.Country;

import java.io.File;

public class LABSimMultiRun extends MultiRun {

	public static boolean executeWithGui = true;

	private static int maxNumberOfRuns = 2;

	private static String countryString;
	private static int startYear;

	private Long counter = 0L;
	
	private Long randomSeed = 600L;

	/**
	 *
	 * 	MAIN PROGRAM ENTRY FOR MULTI-SIMULATION
	 *
	 */
	public static void main(String[] args) {

		//Adjust the country and year to the value read from Excel, which is updated when the database is rebuilt. Otherwise it will set the country and year to the last one used to build the database
		MultiKeyCoefficientMap lastDatabaseCountryAndYear = ExcelAssistant.loadCoefficientMap("input" + File.separator + Parameters.DatabaseCountryYearFilename + ".xlsx", "Data", 1, 1);
		if (lastDatabaseCountryAndYear.keySet().stream().anyMatch(key -> key.toString().equals("MultiKey[IT]"))) {
			countryString = "Italy";
		} else {
			countryString = "United Kingdom";
		}
		String valueYear = lastDatabaseCountryAndYear.getValue(Country.UK.getCountryFromNameString(countryString).toString()).toString();
		startYear = Integer.parseInt(valueYear);

		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-n")){ // These options are for use from the command line
				
				try {
					maxNumberOfRuns = Integer.parseInt(args[i + 1]);
			    } catch (NumberFormatException e) {
			        System.err.println("Argument " + args[i + 1] + " must be an integer reflecting the maximum number of runs.");
			        System.exit(1);
			    }
				
				i++;
			}
			else if (args[i].equals("-g")){
				executeWithGui = Boolean.parseBoolean(args[i + 1]);
				i++;
			}
			else if (args[i].equals("-c")){				//Set country by arguments here
				countryString = args[i+1];				
				i++;
			}
			
		}
		
		SimulationEngine engine = SimulationEngine.getInstance();
		
		LABSimMultiRun experimentBuilder = new LABSimMultiRun();
//		engine.setBuilderClass(LABSimMultiRun.class);			//This works but is deprecated
		engine.setExperimentBuilder(experimentBuilder);					//This replaces the above line... but does it work?
		engine.setup();													//Do we need this?  Worked fine without it...

		if (executeWithGui)
			new MultiRunFrame(experimentBuilder, "LABSim MultiRun", maxNumberOfRuns);
		else
			experimentBuilder.start();
	}

	@Override
	public void buildExperiment(SimulationEngine engine) {
		LABSimModel model = new LABSimModel(Country.IT.getCountryFromNameString(countryString), startYear);
//		LABSimModel model = new LABSimModel();
		setCountry(model);		//Set country based on input arguments.
		model.setRandomSeedIfFixed(randomSeed);
		engine.addSimulationManager(model);
		
		LABSimCollector collector = new LABSimCollector(model);
		engine.addSimulationManager(collector);
		
//		LABSimObserver observer = new LABSimObserver(model, collector);		//Not needed for MultiRun?
//		engine.addSimulationManager(observer);

		
	}

	private void setCountry(LABSimModel model) {
		if(countryString.equalsIgnoreCase("Italy")) {
			model.setCountry(Country.IT);
		}
		else if(countryString.equalsIgnoreCase("United Kingdom")) {
			model.setCountry(Country.UK);
		}
		else throw new RuntimeException("countryString is not set to an appropriate string!");
	}
	
	@Override
	public boolean nextModel() {
		randomSeed++;
		counter++;

		if(counter < maxNumberOfRuns) {
			return true;
		}
		else return false;
	}

	@Override
	public String setupRunLabel() {
		return randomSeed.toString();
	}

}

package labsim.model.decisions;


import java.io.File;

import labsim.model.LABSimModel;


/**
 * CLASS TO MANAGE IDENTIFICATION OF LOOK-UP TABLE FOR INTERTEMPORAL OPTIMISATION DECISIONS
 *
 * THE LOOK-UP TABLE IS REFERRED TO THROUGHOUT AS THE 'GRIDS'
 */
public class ManagerPopulateGrids {


    /**
     * ENTRY POINT FOR MANAGER
     * THE MANAGER IS 'run' FROM LABSimStart, FOLLOWING USER OPTIONS SELECTED THROUGH THE GUI
     *
     * @param use_saved_grids boolean that indicates whether grids should be populated from disk
     * @param grids_file_name defines file name to use for grids solution
     */
    public static void run(LABSimModel model, boolean use_saved_grids, String grids_file_name, Integer employmentOptionsOfPrincipalWorker,
                           Integer employmentOptionsOfSecondaryWorker, boolean optimisedBehaviourToRespondToHealthStatus,
                           boolean intertemporalResponsesToRegion, boolean intertemporalResponsesToEducationStatus,
                           Integer startYear, Integer endYear ) {

        // directory structure
        Parameters.grids_directory = labsim.data.Parameters.WORKING_DIRECTORY;
        Parameters.grids_directory = Parameters.grids_directory +
                File.separator + "output" + File.separator + "grids" + File.separator + grids_file_name;

        // set-up behavioural parameters
        Parameters.options_employment1 = employmentOptionsOfPrincipalWorker;
        Parameters.options_employment2 = employmentOptionsOfSecondaryWorker;
        Parameters.flag_health = optimisedBehaviourToRespondToHealthStatus;
        Parameters.flag_region = intertemporalResponsesToRegion;
        Parameters.flag_education = intertemporalResponsesToEducationStatus;
        Parameters.min_birth_year = startYear - 80;
        Parameters.max_birth_year = endYear - 20;
        Parameters.start_year = startYear;
        Parameters.pts_birth_year = 1 + (int)((Parameters.max_birth_year - Parameters.min_birth_year) / 20 + 0.5);

        // initiate the decision grids
        Grids grids = new Grids();

        // populate the decision grids
        if (use_saved_grids) {
            // use saved intertemporal optimisations
            ManagerFileGrids.read(grids);
        } else {
            // need to solve for intertemporal optimisations
            Integer year0 = model.getYear();
            model.addRegressionStochasticComponent = false;
            ManagerSolveGrids.run(model, grids);
            model.addRegressionStochasticComponent = true;
            model.setYear(year0);

            // check whether need to save populated grids
            ManagerFileGrids.write(grids);
        }
    }
}

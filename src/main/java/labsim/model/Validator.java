package labsim.model;

import labsim.data.Parameters;
import labsim.experiment.LABSimCollector;
import labsim.experiment.LABSimObserver;
import labsim.model.enums.Gender;
import labsim.model.enums.Region;
import microsim.engine.SimulationEngine;
import microsim.statistics.IDoubleSource;



public class Validator implements IDoubleSource {

    private final LABSimModel model;
    private final LABSimCollector collector;
    private final LABSimObserver observer;

    Number value;

    // ---------------------------------------------------------------------
    // CONSTRUCTOR
    // ---------------------------------------------------------------------
    public Validator() {
        super();
        model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
        collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
        observer = (LABSimObserver) SimulationEngine.getInstance().getManager(LABSimObserver.class.getCanonicalName());
    }

    // ---------------------------------------------------------------------
    // Methods used by the validator
    // ---------------------------------------------------------------------
    public int getPopulationProjectionByAge(int startAge, int endAge) {
        double numberOfPeople = 0.;
        for (Gender gender : Gender.values()) {
            for (Region region : Parameters.getCountryRegions()) {
                for (int age = startAge; age <= endAge; age++) {
                    numberOfPeople += ((Number) Parameters.getPopulationProjections().getValue(gender.toString(), region.toString(), age, model.getYear())).doubleValue();
                }
            }
        }
        int numberOfPeopleScaled = (int) Math.round(numberOfPeople / model.getScalingFactor());
        return numberOfPeopleScaled;
    }


    // ---------------------------------------------------------------------
    // implements IDoubleSource for use with the Observer
    // ---------------------------------------------------------------------


    public enum DoublesVariables {
        populationProjectionsByAge_0_18,
        populationProjectionsByAge_0_0,
        populationProjectionsByAge_2_10,
        populationProjectionsByAge_11_15,
        populationProjectionsByAge_19_25,
        populationProjectionsByAge_40_59,
        populationProjectionsByAge_60_79,
        populationProjectionsByAge_80_100,
        studentsByAge_15_19,
        studentsByAge_20_24,
        studentsByAge_25_29,
        studentsByAge_30_34,
        studentsByAge_35_39,
        studentsByAge_40_59,
        studentsByAge_60_79,
        studentsByAge_80_100,
        studentsByAge_All,
        studentsByRegion_ITC,
        studentsByRegion_ITH,
        studentsByRegion_ITI,
        studentsByRegion_ITF,
        studentsByRegion_ITG,
        studentsByRegion_All,
        educationLevelHigh,
        educationLevelMedium,
        educationLevelLow,
        educationLevelHighByAge_20_29,
        educationLevelHighByAge_30_39,
        educationLevelHighByAge_40_49,
        educationLevelHighByAge_50_59,
        educationLevelMediumByAge_20_29,
        educationLevelMediumByAge_30_39,
        educationLevelMediumByAge_40_49,
        educationLevelMediumByAge_50_59,
        educationLevelLowByAge_20_29,
        educationLevelLowByAge_30_39,
        educationLevelLowByAge_40_49,
        educationLevelLowByAge_50_59,
        educationLevelLowByRegion_ITC,
        educationLevelLowByRegion_ITH,
        educationLevelLowByRegion_ITI,
        educationLevelLowByRegion_ITF,
        educationLevelLowByRegion_ITG,
        educationLevelHighByRegion_ITC,
        educationLevelHighByRegion_ITH,
        educationLevelHighByRegion_ITI,
        educationLevelHighByRegion_ITF,
        educationLevelHighByRegion_ITG,
        partneredShare_ITC,
        partneredShare_ITH,
        partneredShare_ITI,
        partneredShare_ITF,
        partneredShare_ITG,
        partneredShare_All,
        disabledFemale,
        disabledMale,
        disabledFemale_0_49,
        disabledMale_0_49,
        disabledFemale_50_74,
        disabledMale_50_74,
        disabledFemale_75_100,
        disabledMale_75_100,
        healthFemale_0_49,
        healthMale_0_49,
        healthFemale_50_74,
        healthMale_50_74,
        healthFemale_75_100,
        healthMale_75_100,
        employmentMale,
        employmentFemale,
        employmentMaleByAge_20_29,
        employmentMaleByAge_30_39,
        employmentMaleByAge_40_49,
        employmentMaleByAge_50_59,
        employmentFemaleByAge_20_29,
        employmentFemaleByAge_30_39,
        employmentFemaleByAge_40_49,
        employmentFemaleByAge_50_59,
        employmentFemaleChild_0_5,
        employmentFemaleChild_6_18,
        employmentFemaleNoChild, //
        employed_female_ITC,
        employed_male_ITC,
        employed_female_ITH,
        employed_male_ITH,
        employed_female_ITI,
        employed_male_ITI,
        employed_female_ITF,
        employed_male_ITF,
        employed_female_ITG,
        employed_male_ITG,
        labour_supply_High,
        labour_supply_Medium,
        labour_supply_Low,
        activityStatus_Employed,
        activityStatus_NotEmployedRetired,
        activityStatus_Student
        }

    @Override
    public double getDoubleValue(Enum<?> variableID) {

        switch ((Validator.DoublesVariables) variableID) {

            case populationProjectionsByAge_0_18:
                return getPopulationProjectionByAge(0,18);
            case populationProjectionsByAge_0_0:
                return getPopulationProjectionByAge(0,0);
            case populationProjectionsByAge_2_10:
                return getPopulationProjectionByAge(2,10);
            case populationProjectionsByAge_11_15:
                return getPopulationProjectionByAge(11,15);
            case populationProjectionsByAge_19_25:
                return getPopulationProjectionByAge(19,25);
            case populationProjectionsByAge_40_59:
                return getPopulationProjectionByAge(40,59);
            case populationProjectionsByAge_60_79:
                return getPopulationProjectionByAge(60,79);
            case populationProjectionsByAge_80_100:
                return getPopulationProjectionByAge(80,100);
            case studentsByAge_15_19:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_15_19"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_20_24:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_20_24"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_25_29:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_25_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_30_34:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_30_34"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_35_39:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_35_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_40_59:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_40_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_60_79:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_60_79"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_80_100:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_80_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByAge_All:
                value = ((Number) Parameters.getValidationStudentsByAge().getValue(model.getYear()-1, "ageGroup_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITC:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITH:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITI:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITF:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_ITG:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case studentsByRegion_All:
                value = ((Number) Parameters.getValidationStudentsByRegion().getValue(model.getYear()-1, "region_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHigh:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_high"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMedium:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_med"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLow:
                value = ((Number) Parameters.getValidationEducationLevel().getValue(model.getYear()-1, "educ_low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_20_29:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_30_39:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_40_49:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByAge_50_59:
                 value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_high_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_20_29:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_30_39:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_40_49:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelMediumByAge_50_59:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_med_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_20_29:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_30_39:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_40_49:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByAge_50_59:
                value = ((Number) Parameters.getValidationEducationLevelByAge().getValue(model.getYear()-1, "educ_low_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelLowByRegion_ITG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_low_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITC:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITH:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITI:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITF:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case educationLevelHighByRegion_ITG:
                value = ((Number) Parameters.getValidationEducationLevelByRegion().getValue(model.getYear()-1, "educ_high_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITC:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITH:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITI:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITF:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_ITG:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case partneredShare_All:
                value = ((Number) Parameters.getValidationPartneredShareByRegion().getValue(model.getYear()-1, "partnered_All"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale:
                value = ((Number) Parameters.getValidationDisabledByGender().getValue(model.getYear()-1, "dlltsd_female"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale:
                value = ((Number) Parameters.getValidationDisabledByGender().getValue(model.getYear()-1, "dlltsd_male"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_0_49:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_0_49:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_50_74:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_50_74:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledFemale_75_100:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_female_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case disabledMale_75_100:
                value = ((Number) Parameters.getValidationDisabledByAge().getValue(model.getYear()-1, "disabled_male_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_0_49:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_0_49:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_0_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_50_74:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_50_74:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_50_74"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthFemale_75_100:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_female_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case healthMale_75_100:
                value = ((Number) Parameters.getValidationHealthByAge().getValue(model.getYear()-1, "health_male_75_100"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMale:
                value = ((Number) Parameters.getValidationEmploymentByGender().getValue(model.getYear()-1, "employed_Male"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemale:
                value = ((Number) Parameters.getValidationEmploymentByGender().getValue(model.getYear()-1, "employed_Female"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_20_29:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_30_39:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_40_49:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentMaleByAge_50_59:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_male_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_20_29:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_20_29"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_30_39:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_30_39"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_40_49:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_40_49"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleByAge_50_59:
                value = ((Number) Parameters.getValidationEmploymentByAgeAndGender().getValue(model.getYear()-1, "employed_female_50_59"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleChild_0_5:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_with_child_0_5"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleChild_6_18:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_with_child_6_18"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employmentFemaleNoChild:
                value = ((Number) Parameters.getValidationEmploymentByMaternity().getValue(model.getYear()-1, "emp_without_child"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITC:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITC"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITH:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITH"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITI:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITI"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITF:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITF"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_female_ITG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_female_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case employed_male_ITG:
                value = ((Number) Parameters.getValidationEmploymentByGenderAndRegion().getValue(model.getYear()-1, "employed_male_ITG"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_High:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_High"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_Medium:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_Medium"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case labour_supply_Low:
                value = ((Number) Parameters.getValidationLabourSupplyByEducation().getValue(model.getYear()-1, "labour_supply_Low"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_Employed:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_employed"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_NotEmployedRetired:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_notemployedretired"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
            case activityStatus_Student:
                value = ((Number) Parameters.getValidationActivityStatus().getValue(model.getYear()-1, "as_student"));
                if (value != null) {
                    return value.doubleValue();
                } else return Double.NaN; //If value missing, returning Double.NaN will plot a gap
        }

        return 0;
    }
}

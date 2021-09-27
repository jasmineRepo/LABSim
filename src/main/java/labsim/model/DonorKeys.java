package labsim.model;


import labsim.model.enums.Gender;
import labsim.model.enums.Labour;
import labsim.model.enums.Occupancy;
import labsim.model.enums.Region;
import org.apache.commons.collections4.keyvalue.MultiKey;


/**
 *
 * CLASS TO DEFINE KEYS USED TO OBTAIN DONOR HOUSEHOLDS FROM EUROMOD
 *
 */
public class DonorKeys {


    /**
     * ATTRIBUTES
     * keys should be in descending order of importance for identifying tax anb benefits payments
     */
    MultiKey<Labour> labourKey;		// labour hours per week of adult member(s)
    Object key1;                    // health status of adult member(s) (MultiKey Value because in a couple there are two values)
    Integer key2;                   // number of dependent children
    Region key3;                    // region of residence
    Integer ageTopCode = 80;        // simulation parameter
    Object key4;                    // age of adult member(s) (MultiKey Value because in a couple there are two values)


    /**
     * CONSTRUCTOR FOR DONOR HOUSEHOLDS (FROM EUROMOD)
     */
    public DonorKeys(DonorHousehold house) {
        key2 = house.getChildren().size();
        key3 = house.getRegion();
        if(house.getOccupancy().equals(Occupancy.Couple)) {

            DonorPerson male = house.getMale();
            DonorPerson female = house.getFemale();

            // for labour
            int hoursWorkedMale = male.getHoursWorkedWeekly();
            Labour labourMale = Labour.convertHoursToLabour(hoursWorkedMale, Gender.Male);
            int hoursWorkedFemale = female.getHoursWorkedWeekly();
            Labour labourFemale = Labour.convertHoursToLabour(hoursWorkedFemale, Gender.Female);
            labourKey = new MultiKey<>(labourMale, labourFemale);

            // for health
            key1 = new MultiKey<Object>(male.getHealthStatus(), female.getHealthStatus());

            // for age
            Integer maleAge = Math.min(ageTopCode, male.getDag());
            Integer femaleAge = Math.min(ageTopCode, female.getDag());
            key4 = new MultiKey<Integer>(maleAge, femaleAge);
        } else {

            DonorPerson single;
            if(house.getOccupancy().equals(Occupancy.Single_Female)) {
                single = house.getFemale();
            }
            else {		//Must be single male
                single = house.getMale();
            }

            // for labour
            int hoursWorked = single.getHoursWorkedWeekly();
            labourKey = new MultiKey<> (Labour.convertHoursToLabour(hoursWorked, single.getDgn()), null);

            // for health
            key1 = single.getHealthStatus();

            // for age
            key4 = Math.min(ageTopCode, single.getDag());
        }
    }

    /**
     * CONSTRUCTOR FOR LABSIM HOUSEHOLDS
     */
    public DonorKeys(BenefitUnit house, MultiKey<Labour> labourKey) {

        this.labourKey = labourKey;
        key2 = house.getN_children_allAges();
        key3 = house.getRegion();
        if(house.occupancy.equals(Occupancy.Couple)) {
            key1 = new MultiKey<Object>(house.getMale().getHealthStatusConversion(), house.getFemale().getHealthStatusConversion());
            Integer maleAge = Math.min(ageTopCode, house.getMale().getDag());
            Integer femaleAge = Math.min(ageTopCode, house.getFemale().getDag());
            key4 = new MultiKey<Integer>(maleAge, femaleAge);
        }
        else {
            Person single;
            if(house.occupancy.equals(Occupancy.Single_Female)){
                single = house.getFemale();
            } else {
                single = house.getMale();
            }
            key1 = single.getHealthStatusConversion();
            key4 = Math.min(ageTopCode, single.getDag());
        }
    }
}

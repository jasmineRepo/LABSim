package labsim.data.filters;

import labsim.data.Parameters;
import labsim.model.Person;
import labsim.model.enums.Education;
import labsim.model.enums.Indicator;
import labsim.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class FlexibleInLabourSupplyByEducationFilter implements ICollectionFilter {

    private Education education;

    public FlexibleInLabourSupplyByEducationFilter(Education education) {
        super();
        this.education = education;
    }

    @Override
    public boolean isFiltered(Object o) {
        Person person = (Person) o;

        /*
        Person "flexible in labour supply" must meet the following conditions:
        age >= 16 and <= 75
        not a student or retired
        not disabled
         */

        return (person.getDag() >= Parameters.MIN_AGE_FLEXIBLE_LABOUR_SUPPLY && person.getDag() <= Parameters.MAX_AGE_FLEXIBLE_LABOUR_SUPPLY &&
                person.getLes_c4() != Les_c4.Student && person.getLes_c4() != Les_c4.Retired &&
                person.getDlltsd() != Indicator.True &&
                person.getDeh_c3().equals(education));
    }
}

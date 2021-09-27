package labsim.data.filters;

import labsim.data.Parameters;
import labsim.model.Person;
import labsim.model.enums.Gender;
import labsim.model.enums.Indicator;
import labsim.model.enums.Les_c4;
import labsim.model.enums.Region;
import org.apache.commons.collections4.Predicate;

public class FlexibleInLabourSupplyPersonFilter<T extends Person> implements Predicate<T> {


	public FlexibleInLabourSupplyPersonFilter() {
		super();
	}

	@Override
	public boolean evaluate(T person) {

		
		return (person.getDag() >= 18 && person.getDag() <= 64 &&
				person.getLes_c4() != Les_c4.Student && person.getLes_c4() != Les_c4.Retired &&
				person.getDlltsd() != Indicator.True);
	}


}

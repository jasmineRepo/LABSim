package labsim.data.filters;

import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Gender;

public class FemaleCSfilter implements ICollectionFilter{
	
	public boolean isFiltered(Object object) {
		return ( ((Person) object).getDgn().equals(Gender.Female) );
	}
	
}

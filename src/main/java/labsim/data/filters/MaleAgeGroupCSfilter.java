package labsim.data.filters;

import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Gender;

public class MaleAgeGroupCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	
	public MaleAgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDgn().equals(Gender.Male) && (person.getDag() >= ageFrom) && (person.getDag() <= ageTo) );
	}
	
}


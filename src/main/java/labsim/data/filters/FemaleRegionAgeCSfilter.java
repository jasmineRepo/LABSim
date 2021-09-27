package labsim.data.filters;

import labsim.model.Person;
import labsim.model.enums.Gender;
import labsim.model.enums.Region;
import microsim.statistics.ICollectionFilter;

public class FemaleRegionAgeCSfilter implements ICollectionFilter{

	private Region region;
	private int ageFrom;
	private int ageTo;

	public FemaleRegionAgeCSfilter(Region region, int ageFrom, int ageTo) {
		super();
		this.region = region;
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return person.getRegion().equals(region) && person.getDgn().equals(Gender.Female) && person.getDag() >= ageFrom && person.getDag() <= ageTo;
	}
	
}

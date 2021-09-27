package labsim.data.filters;

import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Education;
import labsim.model.enums.Gender;
import labsim.model.enums.Indicator;

public class FemalesWithChildrenAgeGroupEducationCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	private Education edu;
	
	public FemalesWithChildrenAgeGroupEducationCSfilter(int ageFrom, int ageTo, Education edu) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
		this.edu = edu;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDgn().equals(Gender.Female) && 
				(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && 
				( person.getBenefitUnit().getD_children_3under().equals(Indicator.True) || person.getBenefitUnit().getD_children_4_12().equals(Indicator.True) ) &&
				( person.getDeh_c3().equals(edu))
				);
	}
	
}


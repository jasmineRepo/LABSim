package labsim.data.filters;

import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Gender;
import labsim.model.enums.Indicator;

public class FemalesWithChildrenAgeGroupCSfilter implements ICollectionFilter{
	
	private int ageFrom;
	private int ageTo;
	
	public FemalesWithChildrenAgeGroupCSfilter(int ageFrom, int ageTo) {
		super();
		this.ageFrom = ageFrom;
		this.ageTo = ageTo;
	}
	
	public boolean isFiltered(Object object) {
		Person person = (Person) object;
		return ( person.getDgn().equals(Gender.Female) && 
				(person.getDag() >= ageFrom) && (person.getDag() <= ageTo) && 
				( person.getBenefitUnit().getD_children_3under().equals(Indicator.True) || person.getBenefitUnit().getD_children_4_12().equals(Indicator.True) || person.getBenefitUnit().getD_children_13_17().equals(Indicator.True))
				);
	}
	
}


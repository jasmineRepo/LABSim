package labsim.data.filters;

import labsim.model.BenefitUnit;
import labsim.model.Person;
import labsim.model.enums.Les_c4;
import labsim.model.enums.Region;
import microsim.statistics.ICollectionFilter;

/*
For individuals who have completed education and had education level assigned (i.e. excludes students)

 */

public class ValidEducationRegionCSfilter implements ICollectionFilter{

	private Region region;

	public ValidEducationRegionCSfilter(Region region) {
		super();
		this.region = region;
		
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(region) && !person.getLes_c4().equals(Les_c4.Student) && person.getDag() >= 18 && person.getDeh_c3() != null);
		}
		else if(object instanceof BenefitUnit) {
			BenefitUnit benefitUnit = (BenefitUnit) object;
			return benefitUnit.getRegion().equals(region);
		}
		else throw new IllegalArgumentException("Object passed to RegionCSfilter must be of type Person or BenefitUnit!");
	}
	
}

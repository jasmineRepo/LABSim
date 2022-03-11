package labsim.data.filters;

import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Education;
import labsim.model.enums.Region;

public class RegionEducationAtRiskOfWorkCSfilter implements ICollectionFilter{
	
	private Region region;
	private Education education;
	
	public RegionEducationAtRiskOfWorkCSfilter(Region region, Education education) {
		super();
		this.region = region;
		this.education = education;			
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(region) && person.getDeh_c3().equals(education) && person.atRiskOfWork());
		}
		else throw new IllegalArgumentException("Object passed to RegionEducationCSfilter must be of type Person!");
	}			
}

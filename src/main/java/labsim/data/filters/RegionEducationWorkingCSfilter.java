package labsim.data.filters;

import labsim.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;
import labsim.model.Person;
import labsim.model.enums.Education;
import labsim.model.enums.Region;

public class RegionEducationWorkingCSfilter implements ICollectionFilter{
	
	private Region region;
	private Education education;
	
	public RegionEducationWorkingCSfilter(Region region, Education education) {
		super();
		this.region = region;
		this.education = education;			
	}
	
	public boolean isFiltered(Object object) {
		if(object instanceof Person) {
			Person person = (Person) object;
			return (person.getRegion().equals(region) && person.getDeh_c3().equals(education) && person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed) && person.getGrossEarningsYearly() >= 0.);
		}
		else throw new IllegalArgumentException("Object passed to RegionEducationWorkingCSfilter must be of type Person!");
	}			
}

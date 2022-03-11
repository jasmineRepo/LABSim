package labsim.data.filters;

import labsim.model.Person;
import labsim.model.enums.Education;
import labsim.model.enums.Les_c4;
import microsim.statistics.ICollectionFilter;

public class EducationEmployedCSfilter implements ICollectionFilter{

	private Education education;

	public EducationEmployedCSfilter(Education edu) {
		super();
		this.education = edu;
		
	}
	
	public boolean isFiltered(Object object) {
			Person person = (Person) object;
			return (person.getDeh_c3().equals(education) && person.getLes_c4().equals(Les_c4.EmployedOrSelfEmployed));
			
	}
	
}

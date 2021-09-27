package labsim.data.filters;

import org.apache.commons.collections4.Predicate;

import labsim.data.Parameters;
import labsim.model.Person;
import labsim.model.enums.Gender;
import labsim.model.enums.Region;

public class FertileFilter<T extends Person> implements Predicate<T> {
	
	private Region region;
	
	public FertileFilter(Region region) {
		super();
		this.region = region;
	}

	@Override
	public boolean evaluate(T agent) {
		
		int age = agent.getDag();
		
		return (( agent.getDgn().equals(Gender.Female)) &&
				( agent.getRegion().equals(region)) &&
//				( !agent.getLes_c3().equals(Les_c4.Student) || agent.isToLeaveSchool() ) &&	//2 processes for fertility, for those in and out of education - specified in alignment
				( age >= Parameters.MIN_AGE_MATERNITY ) &&
				( age <= Parameters.MAX_AGE_MATERNITY ) &&
				( agent.getPartner() != null)						//This is an additional restriction in the LABsim model
				);
	}


}

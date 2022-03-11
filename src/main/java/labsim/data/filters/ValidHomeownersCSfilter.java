package labsim.data.filters;

import labsim.data.Parameters;
import labsim.model.BenefitUnit;
import labsim.model.Person;
import labsim.model.enums.Les_c4;
import labsim.model.enums.Occupancy;
import microsim.statistics.ICollectionFilter;

public class ValidHomeownersCSfilter implements ICollectionFilter {

	public boolean isFiltered(Object object) {
		BenefitUnit benefitUnit = (BenefitUnit) object;

		boolean returnValue = false;

		if (benefitUnit.getOccupancy().equals(Occupancy.Couple)) {
			returnValue = (benefitUnit.getMale().getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE && benefitUnit.getFemale().getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE);
		} else if (benefitUnit.getOccupancy().equals(Occupancy.Single_Female)) {
			returnValue = (benefitUnit.getFemale().getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE);
		} else if (benefitUnit.getOccupancy().equals(Occupancy.Single_Male)) {
			returnValue = (benefitUnit.getMale().getDag() >= Parameters.AGE_TO_BECOME_RESPONSIBLE);
		}
		return returnValue;
	}
}

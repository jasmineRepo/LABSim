package labsim.model.enums;

public enum Labour {

	//Represents hours of work per week that a Person will supply to firms
	ZERO(0),
	TWENTY(20),
	THIRTY(30),
	THIRTYSIX(36),
	FORTY(40),
	FIFTY(50);
//	SIXTY(60);

	
    private final int hours;

    Labour(int hours)
    {
        this.hours = hours;
    }

    public int getHours()
    {
        return hours;
    }
    
	public static Labour convertHoursToLabour(int hoursWorked, Gender gender) {
    	if (gender.equals(Gender.Female)) {
			if(hoursWorked < 1)
				return Labour.ZERO;
			else if(hoursWorked <= 29)
				return Labour.TWENTY;
			else if(hoursWorked <= 35)
				return Labour.THIRTY;
			else if(hoursWorked <= 39)
				return Labour.THIRTYSIX;
			else return Labour.FORTY;
		}
    	else {
			if(hoursWorked < 1)
				return Labour.ZERO;
			else if(hoursWorked <= 35)
				return Labour.THIRTY;
			else if(hoursWorked <= 39)
				return Labour.THIRTYSIX;
			else if(hoursWorked <= 49)
				return Labour.FORTY;
			else return Labour.FIFTY;
		}
	}

	/*
	Males and females have different set of choices and different rules transforming actual hours into the discretized version. Once transformed, however, they are the same "choice". For example: female working 25 hours will be assigned
	TWENTY while male will be assigned THIRTY as the discretized choice.
	 */

	public static Labour[] returnChoicesAllowedForGender(Gender gender) {
    	Labour[] allowedChoices;
    	if (gender.equals(Gender.Female)) {
    		allowedChoices = new Labour[] {ZERO, TWENTY, THIRTY, THIRTYSIX, FORTY};
		}
    	else {
			allowedChoices = new Labour[] {ZERO, THIRTY, THIRTYSIX, FORTY, FIFTY};
		}
    	return allowedChoices;
	}

}


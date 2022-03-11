package labsim.model.enums;

public enum Labour {

	//Represents hours of work per week that a Person will supply to firms
	ZERO(0),
	TEN(20),
	TWENTY(30),
	THIRTY(36),
	FORTY(40);
//	FIFTY(50);
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
		if(hoursWorked <= 5)
			return Labour.ZERO;
		else if(hoursWorked <= 15)
			return Labour.TEN;
		else if(hoursWorked <= 25)
			return Labour.TWENTY;
		else if(hoursWorked <= 35)
			return Labour.THIRTY;
		else return Labour.FORTY;
	}

	public static Labour convertHoursToLabour(double hoursWorked, Gender gender) {
		if(hoursWorked <= 5)
			return Labour.ZERO;
		else if(hoursWorked <= 15)
			return Labour.TEN;
		else if(hoursWorked <= 25)
			return Labour.TWENTY;
		else if(hoursWorked <= 35)
			return Labour.THIRTY;
		else return Labour.FORTY;
	}

	public static Labour convertHoursToLabour(Double hoursWorked, Gender gender) {
		if(hoursWorked <= 5)
			return Labour.ZERO;
		else if(hoursWorked <= 15)
			return Labour.TEN;
		else if(hoursWorked <= 25)
			return Labour.TWENTY;
		else if(hoursWorked <= 35)
			return Labour.THIRTY;
		else return Labour.FORTY;
	}

	/*
	Males and females have different set of choices and different rules transforming actual hours into the discretized version. Once transformed, however, they are the same "choice". For example: female working 25 hours will be assigned
	TWENTY while male will be assigned THIRTY as the discretized choice.
	 */

	public static Labour[] returnChoicesAllowedForGender(Gender gender) {
    	Labour[] allowedChoices;
		allowedChoices = new Labour[] {ZERO, TEN, TWENTY, THIRTY, FORTY};
    	return allowedChoices;
	}

}


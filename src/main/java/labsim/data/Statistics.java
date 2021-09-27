package labsim.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import microsim.data.db.PanelEntityKey;
import microsim.statistics.IDoubleSource;

@Entity
public class Statistics {

	@Id
	private PanelEntityKey key = new PanelEntityKey(1L);

	@Column(name = "Gini_coefficient_individual_market_income_nationally")
	private double giniMarketIncomeNational;

	@Column(name = "Gini_coefficient_equivalised_household_disposable_income_nationally")
	private double giniEquivalisedHouseholdDisposableIncomeNational;

	@Column(name = "Median_equivalised_household_disposable_income")
	private double medianEquivalisedHouseholdDisposableIncome;
	
	//Percentiles of ydses:
	@Column(name = "Ydses_p20")
	private double ydses_p20;
	
	@Column(name = "Ydses_p40")
	private double ydses_p40;
	
	@Column(name = "Ydses_p60")
	private double ydses_p60;
	
	@Column(name = "Ydses_p80")
	private double ydses_p80;

	//Percentiles of SIndex:
	@Column(name = "SIndex_p50")
	private double sIndex_p50;
	
////	Risk-of-poverty threshold is set at 60% of the national median equivalised household disposable income.
//	@Column(name = "Risk_of_poverty_threshold")
//	private double riskOfPovertyThreshold;
	
	public void setGiniPersonalGrossEarningsNational(double giniMarketIncomeNational) {
		this.giniMarketIncomeNational = giniMarketIncomeNational;
	}
	
	public void setGiniEquivalisedHouseholdDisposableIncomeNational(double giniEquivalisedHouseholdDisposableIncomeNational) {
		this.giniEquivalisedHouseholdDisposableIncomeNational = giniEquivalisedHouseholdDisposableIncomeNational;
	}

	public double getMedianEquivalisedHouseholdDisposableIncome() {
		return medianEquivalisedHouseholdDisposableIncome;
	}

	public void setMedianEquivalisedHouseholdDisposableIncome(double medianEquivalisedHouseholdDisposableIncome) {
		this.medianEquivalisedHouseholdDisposableIncome = medianEquivalisedHouseholdDisposableIncome;
	}
	
	public double getYdses_p20() {
		return ydses_p20;
	}

	public void setYdses_p20(double ydses_p20) {
		this.ydses_p20 = ydses_p20;
	}

	public double getYdses_p40() {
		return ydses_p40;
	}

	public void setYdses_p40(double ydses_p40) {
		this.ydses_p40 = ydses_p40;
	}

	public double getYdses_p60() {
		return ydses_p60;
	}

	public void setYdses_p60(double ydses_p60) {
		this.ydses_p60 = ydses_p60;
	}

	public double getYdses_p80() {
		return ydses_p80;
	}

	public void setYdses_p80(double ydses_p80) {
		this.ydses_p80 = ydses_p80;
	}

	public double getsIndex_p50() {
		return sIndex_p50;
	}

	public void setsIndex_p50(double sIndex_p50) {
		this.sIndex_p50 = sIndex_p50;
	}

//	public double getRiskOfPovertyThreshold() {
//		return riskOfPovertyThreshold;
//	}
//
//	public void setRiskOfPovertyThreshold(double riskOfPovertyThreshold) {
//		this.riskOfPovertyThreshold = riskOfPovertyThreshold;
//	}

}

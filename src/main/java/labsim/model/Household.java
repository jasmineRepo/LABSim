package labsim.model;

import labsim.experiment.LABSimCollector;
import microsim.data.db.PanelEntityKey;
import microsim.engine.SimulationEngine;
import microsim.event.EventListener;
import microsim.statistics.IDoubleSource;
import org.apache.log4j.Logger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/*
Household class is a "wrapper" bundling multiple benefitUnits into one household.
Currently it is used to keep track of adult children who create separate benefitUnits in the simulation for technical reasons, but still live with their parents in "reality".

Persons in the database have idhh field, identifying the household. In the LABSimModel class, we iterate over the list of household ids and create households in the simulation with the matching key. These are then matched with benefit units
from the data, on the basis of the idhh.

 */

@Entity
public class Household implements EventListener, IDoubleSource {

    /*
    VARIABLES
     */

    @Transient
    private static Logger log = Logger.getLogger(Household.class);

    @Transient
    private final LABSimModel model;

    @Transient
    private final LABSimCollector collector;

    @Transient
    public static long householdIdCounter = 1; //Because this is static all instances of a household access and increment the same counter

    @Id
    private final PanelEntityKey key;

    @Column(name="dwt")
    private double dwt = 1.;

    @Column(name ="size")
    private int size; //Number of individuals in the family

//    @Column(name="weight_equivalised")
//    private double weight_equivalised = 0.;

    @Transient
    private Set<BenefitUnit> benefitUnitsInHouseholdSet;

    /*
    CONSTRUCTORS
     */

    public Household() {
        super();

        model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
        collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
        key  = new PanelEntityKey(householdIdCounter++);

        benefitUnitsInHouseholdSet = new LinkedHashSet<BenefitUnit>();
        size = 0;
    }

    //Overloaded constructor taking householdId as argument and setting as Id in the key field
    public Household(long householdId) {
        super();

        model = (LABSimModel) SimulationEngine.getInstance().getManager(LABSimModel.class.getCanonicalName());
        collector = (LABSimCollector) SimulationEngine.getInstance().getManager(LABSimCollector.class.getCanonicalName());
        key  = new PanelEntityKey(householdId);

        benefitUnitsInHouseholdSet = new LinkedHashSet<BenefitUnit>();
        size = 0;
    }

    //Overloaded constructor taking a set of benefit units as input and adding them to the household
    public Household(LinkedHashSet<BenefitUnit> benefitUnitsToAdd) {
        this(); //Refers to the basic constructor Household()

        for(BenefitUnit benefitUnit : benefitUnitsToAdd) {
            addBenefitUnitToHousehold(benefitUnit);
        }
    }

    /*
    METHODS
     */

    protected void initializeFields() {

        //Calculate size
        updateSize();
        //Calculate equivalised weight
        calculateEquivalisedWeight();
    }

    protected void updateSize() {
        int sizeUpdated = 0;
        for (BenefitUnit BU : benefitUnitsInHouseholdSet) {
            sizeUpdated = sizeUpdated + BU.getSize();
        }
        setSize(sizeUpdated);
    }

    //Add a benefitUnit to the household
    public void addBenefitUnitToHousehold(BenefitUnit benefitUnit) {
        benefitUnitsInHouseholdSet.add(benefitUnit);
        // TODO: Benefit Units should contained in the household should update their size here. Or in BU constructors, it should change size
        size = size + benefitUnit.getSize();

        //Whenever a benefit unit is added to household, update occupants household id to match the household
        benefitUnit.setHousehold(this);

    }

    //Remove a benefitUnit from the household
    public void removeBenefitUnitFromHousehold(BenefitUnit benefitUnit) {
        benefitUnitsInHouseholdSet.remove(benefitUnit);
        size = size - benefitUnit.getSize();

        benefitUnit.setHousehold(null);

        //Check for benefit units remaining in the household - if none, remove the household
        if (benefitUnitsInHouseholdSet.size() == 0) {
            model.removeHousehold(this);
        }
    }

    //Calculate household's equivalised weight
    public void calculateEquivalisedWeight(){
        for (BenefitUnit BU : benefitUnitsInHouseholdSet) {

        }
    }

    @Override
    public double getDoubleValue(Enum<?> variableID) {
        return 0;
    }

    @Override
    public void onEvent(Enum<?> anEnum) {

    }

    /*
    GET AND SET METHODS
     */

    public double getDwt() { //Get household weight
        return dwt;
    }

    public long getId() { //Get household ID as set in the simulation. Note that it is different than in the input data.
        return key.getId();
    }

    public int getSize() { //Get total number of people from all benefit units in the household
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }




}

package labsim.model.decisions;


import microsim.data.MultiKeyCoefficientMap;
import labsim.model.Person;

import java.security.InvalidParameterException;


/**
 *
 * CLASS TO MANAGE EVALUATION OF SUPPLEMENTARY DATA FOR INTERACTING WITH JAS-MINE REGRESSION METHODS
 *
 */
public class ManagerRegressions {

    public static double getScore(Person person, Enum<?> regression) {

        double score;
        MultiKeyCoefficientMap map;
        switch ((RegressionNames) regression) {
            case HealthH1a:
                score = labsim.data.Parameters.getRegHealthH1a().getScore(person, labsim.model.Person.DoublesVariables.class);
                break;
            case HealthH1b:
                score = labsim.data.Parameters.getRegHealthH1b().getScore(person, labsim.model.Person.DoublesVariables.class);
                break;
            case WagesMales:
                score = labsim.data.Parameters.getRegWagesMales().getScore(person, labsim.model.Person.DoublesVariables.class);
                break;
            case WagesFemales:
                score = labsim.data.Parameters.getRegWagesFemales().getScore(person, labsim.model.Person.DoublesVariables.class);
                break;
            default:
                throw new InvalidParameterException("RMSE requested for unrecognised regression equation");
        }
        return score;
    }

    public static double getRmse(Enum<?> regression) {

        double rmse;
        MultiKeyCoefficientMap map;
        switch ((RegressionNames) regression) {
            case HealthH1a:
                map = labsim.data.Parameters.getCoeffCovarianceHealthH1a();
                break;
            case HealthH1b:
                map = labsim.data.Parameters.getCoeffCovarianceHealthH1b();
                break;
            case WagesMales:
                map = labsim.data.Parameters.getCoeffCovarianceWagesMales();
                break;
            case WagesFemales:
                map = labsim.data.Parameters.getCoeffCovarianceWagesFemales();
                break;
            default:
                throw new InvalidParameterException("RMSE requested for unrecognised regression equation");
        }
        if (map==null) {
            rmse = 0.0;
        } else {
            rmse = ((Number)map.getValue("ResStanDev", "COEFFICIENT")).doubleValue();
        }
        return rmse;
    }

    public static double getProbability(Person person, Enum<?> regression) {

        double probability;
        switch ((RegressionNames) regression) {
            case EducationE1a:
                probability = labsim.data.Parameters.getRegEducationE1a().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case HealthH2b:
                probability = labsim.data.Parameters.getRegHealthH2b().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case PartnershipU1a:
                probability = labsim.data.Parameters.getRegPartnershipU1a().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case PartnershipU1b:
                probability = labsim.data.Parameters.getRegPartnershipU1b().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case PartnershipU2b:
                probability = labsim.data.Parameters.getRegPartnershipU2b().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case FertilityF1a:
                probability = labsim.data.Parameters.getRegFertilityF1a().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            case FertilityF1b:
                probability = labsim.data.Parameters.getRegFertilityF1b().getProbability(person, labsim.model.Person.DoublesVariables.class);
                break;
            default:
                throw new InvalidParameterException("Probability requested for unrecognised probit regression equation");
        }
        if (probability > 1.0 || probability < 0.0) {
            throw new InvalidParameterException("Problem evaluating probability from probit regression equation");
        }
        return probability;
    }

    public enum RegressionNames {
        EducationE1a,
        HealthH1a,
        HealthH1b,
        HealthH2b,
        PartnershipU1a,
        PartnershipU1b,
        PartnershipU2b,
        FertilityF1a,
        FertilityF1b,
        WagesMales,
        WagesFemales
    }
}

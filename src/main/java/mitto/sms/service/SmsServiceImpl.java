package mitto.sms.service;

import mitto.sms.hibernate.dao.StatsDTO;
import mitto.sms.hibernate.entity.CountryFee;
import mitto.sms.hibernate.entity.Entity;
import mitto.sms.hibernate.entity.SMS;
import mitto.sms.hibernate.repository.CountryFeeRepository;
import mitto.sms.hibernate.repository.SmsRepository;
import mitto.sms.hibernate.repository.SmsRepositoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implementing operation and logic for handling sms data request
 */
@Component("smsService")
public class SmsServiceImpl implements SmsService {

    private final CountryFeeRepository countryFeeRepository;
    private final SmsRepository smsRepository;
    private Boolean countryFeeEnabled = Boolean.FALSE;

    /**
     * Constructor SmsServiceImpl
     * @param smsRepository repository for working with sms entity
     * @param countryFeeRepository repository for working with country fee entity
     */
    @Autowired
    SmsServiceImpl(SmsRepositoryImpl smsRepository, CountryFeeRepository countryFeeRepository) {
        this.smsRepository = smsRepository;
        this.countryFeeRepository = countryFeeRepository;
    }

    private boolean isCountryFeeEnabled() {
        return countryFeeEnabled;
    }

    /**
     * Setting CountryFeeEnabled true enables using extra information stored in CountryFee entity
     * and changing logic for preparing formatted outputs,
     * @param enabled if true - extending top senders with total price
     */
    @Override
    public void setCountryFeeEnabled(boolean enabled) {
        countryFeeEnabled = enabled;
    }

    /**
     * Handle persisting entity
     * @param entity entity to store
     * @return
     */
    @Override
    public boolean saveEntity(Entity entity) {
        if( entity instanceof CountryFee) {
            return saveCountryFee((CountryFee) entity);
        } else if (entity instanceof SMS) {
            return saveSMS((SMS) entity);
        }
        return false;
    }

    private boolean saveCountryFee(CountryFee countryFee) {
        return countryFeeRepository.create(countryFee);
    }

    private boolean saveSMS(SMS sms) {
        Optional<CountryFee> countryFeeReference = countryFeeRepository.findAll().stream()
                .filter(countryFee -> sms.getRecipient().startsWith(countryFee.getCountryCode().toString())).findFirst();
        countryFeeReference.ifPresent(sms::setCountryFee);
        sms.setSuccess(countryFeeReference.isPresent());
        return smsRepository.create(sms);
    }

    /**
     * providing list of formatted top senders (limited and ordered), every item in list is generated by format base on countryFeeEnabled
     * if countryFeeEnabled is False format:
     * @param limit of senders
     * @return ordered list of top {limit} senders stats
     */
    @Override
    public List<StatsDTO> getTopSendersStats(Integer limit) {
        List<StatsDTO>  result;
        if(isCountryFeeEnabled()) {
            result = smsRepository.findTopSendersWithFee(limit);
        } else {
            result =  smsRepository.findTopSenders(limit);
        }
        return result;
    }

    /**
     * providing list of formatted country (ordered)
     * @return ordered list of top {limit} countries stats
     */
    @Override
    public List<StatsDTO> getCountryFeeStats() {
        return smsRepository.getCountryFeeStats();
    }
}

package cz.codecamp.lunchbitch.services.lunchMenuDemandService;


import cz.codecamp.lunchbitch.models.LunchMenuDemand;
import cz.codecamp.lunchbitch.services.triggerAndStorageService.LunchMenuDemandStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LunchMenuDemandServiceImpl implements LunchMenuDemandService{

    private final LunchMenuDemandStorageService lunchMenuDemandStorageService;

    @Autowired
    public LunchMenuDemandServiceImpl(LunchMenuDemandStorageService lunchMenuDemandStorageService) {
        this.lunchMenuDemandStorageService = lunchMenuDemandStorageService;
    }


    @Override
    public void saveLunchMenuPreferences(LunchMenuDemand lunchMenuDemand) {
        lunchMenuDemandStorageService.saveLunchDemandAndTriggerAllSending(lunchMenuDemand);
    }

    @Override
    public void unsubscribeMenuPreferences(LunchMenuDemand lunchMenuDemand) {
        lunchMenuDemandStorageService.deleteLunchMenuDemand(lunchMenuDemand);
    }

    @Override
    public void getLunchMenuPreferences(String email) {
        lunchMenuDemandStorageService.getLunchMenuDemand(email);
    }
}

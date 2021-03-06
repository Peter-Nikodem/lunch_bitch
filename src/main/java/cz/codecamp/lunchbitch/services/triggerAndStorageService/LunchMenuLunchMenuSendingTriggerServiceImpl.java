package cz.codecamp.lunchbitch.services.triggerAndStorageService;

import cz.codecamp.lunchbitch.models.Location;
import cz.codecamp.lunchbitch.models.LunchMenuDemand;
import cz.codecamp.lunchbitch.models.Restaurant;
import cz.codecamp.lunchbitch.services.lunchMenuService.LunchMenuService;
import cz.codecamp.lunchbitch.services.triggerAndStorageService.storage.entities.RestaurantInfoEntity;
import cz.codecamp.lunchbitch.services.triggerAndStorageService.storage.entities.UsersRestaurantSelectionEntity;
import cz.codecamp.lunchbitch.services.triggerAndStorageService.storage.repositories.RestaurantInfoRepository;
import cz.codecamp.lunchbitch.services.triggerAndStorageService.storage.repositories.UsersRestaurantSelectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class LunchMenuLunchMenuSendingTriggerServiceImpl implements LunchMenuSendingTrigger {

	@Autowired
	private UsersRestaurantSelectionRepository restaurantSelectionRepository;

	@Autowired
	private RestaurantInfoRepository restaurantInfoRepository;

	@Autowired
	private LunchMenuService lunchMenuService;

	@Override
	@Transactional
	public List<LunchMenuDemand> onTrigger() {
		List<RestaurantInfoEntity> restaurantInfoEntities = retrieveAllRestaurantInfos();
		List<UsersRestaurantSelectionEntity> restaurantSelectionEntities = retrieveAllRestaurantSelections();
		List<Restaurant> restaurantDtos = convertToRestaurantDtos(restaurantInfoEntities);
		List<LunchMenuDemand> lunchMenuDemands = convertToLunchMenuDemands(restaurantSelectionEntities, restaurantDtos);
		List<String> restaurantIds = extractRestaurantIds(restaurantDtos);
		try {
			return lunchMenuService.lunchMenuDownload(restaurantIds, lunchMenuDemands);
		} catch (IOException e) {
			System.err.println("Solving this is not this service's responsibility.");
			return lunchMenuDemands;
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return lunchMenuDemands;
	}


	private List<RestaurantInfoEntity> retrieveAllRestaurantInfos() {
		return iterableToList(restaurantInfoRepository.findAll());
	}

	private List<UsersRestaurantSelectionEntity> retrieveAllRestaurantSelections() {
		return iterableToList(restaurantSelectionRepository.findAll());
	}

	private List<String> extractRestaurantIds(List<Restaurant> restaurantDtos) {
		return restaurantDtos.stream().map(Restaurant::getId).collect(toList());
	}

	private List<Restaurant> convertToRestaurantDtos(List<RestaurantInfoEntity> restaurantInfoEntities) {
		return restaurantInfoEntities.stream().map(this::convertToRestaurantDto).collect(toList());
	}

	private List<LunchMenuDemand> convertToLunchMenuDemands(List<UsersRestaurantSelectionEntity> restaurantSelections, List<Restaurant> restaurantDtos) {
		Map<String, List<UsersRestaurantSelectionEntity>> restaurantSelectionsByEmails = groupByOrdered(restaurantSelections, UsersRestaurantSelectionEntity::getEmail);
		Map<String, Restaurant> restaurantsByZomatoIds = restaurantDtos.stream().collect(toMap(Restaurant::getId, identity()));
		List<LunchMenuDemand> demands = new ArrayList<>();
		for (String email : restaurantSelectionsByEmails.keySet()) {
			List<UsersRestaurantSelectionEntity> usersSelections = restaurantSelectionsByEmails.get(email);
			List<Restaurant> usersRestaurants = usersSelections
					.stream()
					.map(UsersRestaurantSelectionEntity::getZomatoRestaurantId)
					.map(restaurantsByZomatoIds::get)
					.collect(toList());
			LunchMenuDemand lunchMenuDemand = new LunchMenuDemand();
			lunchMenuDemand.setEmail(email);
			lunchMenuDemand.setRestaurants(usersRestaurants);
			demands.add(lunchMenuDemand);
		}
		return demands;
	}

	private Restaurant convertToRestaurantDto(RestaurantInfoEntity restaurantInfoEntity) {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(restaurantInfoEntity.getZomatoId());
		restaurant.setName(restaurantInfoEntity.getName());

		Location location = new Location();
		location.setAddress(restaurantInfoEntity.getAddress());
		location.setLocality(restaurantInfoEntity.getLocality());
		location.setCity(restaurantInfoEntity.getCity());
		location.setLatitude(restaurantInfoEntity.getLatitude());
		location.setLongitude(restaurantInfoEntity.getLongitude());
		location.setZipcode(restaurantInfoEntity.getZipcode());
		location.setCountryId(restaurantInfoEntity.getCountryId());

		restaurant.setLocation(location);
		return restaurant;
	}

	private <T> List<T> iterableToList(Iterable<T> iterable) {
		ArrayList<T> list = new ArrayList<>();
		iterable.forEach(list::add);
		return Collections.unmodifiableList(list);
	}

	private static <K, V> Map<K, List<V>> groupByOrdered(List<V> list, Function<V, K> keyFunction) {
		return list.stream()
				.collect(Collectors.groupingBy(
						keyFunction,
						LinkedHashMap::new,
						Collectors.toList()
				));
	}

	protected void setLunchMenuService(LunchMenuService service) {
		lunchMenuService = service;
	}

}

package in.shvms.famt_be.service;

import in.shvms.famt_be.dto.LocationDto;
import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import in.shvms.famt_be.repo.LocationRepo;
import io.micrometer.common.lang.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final LocationRepo locationRepo;

    public List<Location> findAll() {
        return locationRepo.findAll();
    }

    public Optional<Location> findById(String id) {
        return locationRepo.findById(id);
    }

    public Location save(LocationDto locationDto) {
        log.info("Saving location: {}", locationDto);
        if (locationDto.parentLocationId() == null) {
            throw new IllegalArgumentException("Parent location id is required while saving location.");
        }
        Location parentLocation = getParentLocation(locationDto.parentLocationId(), locationDto.locationType());
        Location location = new Location(locationDto.locationName(), locationDto.locationType());
        location.setParentLocation(parentLocation);
        location.setId(generateLocationId(location));
        locationRepo.save(location);
        addAsChildInParentLocation(location);
        log.info("Location saved successfully: {}", location);
        return location;
    }
    
    public Optional<Location> update(String id, LocationDto locationDto) {
        log.info("Updating location with id: {} with \n LocationDto: {}", id, locationDto);
        locationRepo.findById(id).map(location -> {
                log.info("Location found: {}", location);
                Location newLocation = save(locationDto);
                newLocation.setParentLocation(locationDto.parentLocationId() != null ? getParentLocation(locationDto.parentLocationId(), locationDto.locationType()) : location.getParentLocation());
                newLocation.setChildLocationIds(locationDto.childLocationIds() != null ? getChildLocations(locationDto.childLocationIds(), locationDto.locationType()) : location.getChildLocationIds());
                log.info("Updated location: {}", newLocation);
                updateParentInChildLocations(newLocation);
                deleteNodeById(id);
                locationRepo.save(newLocation);
                log.info("Location updated successfully: {}", newLocation);
                return Optional.of(newLocation);
        });
        return Optional.empty();
    }

    public Optional<Location> deleteNodeById(String id) {
        log.info("Deleting location with id: {}", id);
        Location location = locationRepo.findById(id).get();
        if(Objects.nonNull(location)){
            removeAsChildInParentLocation(location);
            locationRepo.deleteById(id);
            log.info("Location id: {} deleted successfully", id);
            return Optional.of(location);
        }
        log.info("Location id: {} not found", id);
        return Optional.empty();
    }

    private void updateParentInChildLocations(Location location) {
        locationRepo.findAllById(location.getChildLocationIds()).forEach(childLocation -> {
            childLocation.setParentLocation(location);
            locationRepo.save(childLocation);
            log.debug("Updated parent location id: {} in location id: {}", location.getId(), childLocation.getId());
        });
    }        
    
    private void removeAsChildInParentLocation(Location location) {
        locationRepo.findById(location.getParentLocation().getId()).map(parentLocation ->{
            if (Objects.isNull(parentLocation.getChildLocationIds())) {
                parentLocation.setChildLocationIds(new HashSet<>());
            }
            parentLocation.getChildLocationIds().removeIf(childLocationId -> childLocationId.equals(location.getId()));
            locationRepo.save(parentLocation);
            log.debug("Removed Child Location id: {} from Parent Location id: {}", location.getId(), parentLocation.getId());
            return parentLocation;
        });
    }

    private void addAsChildInParentLocation(Location location) {
        locationRepo.findById(location.getParentLocation().getId()).map(parentLocation ->{
            if (Objects.isNull(parentLocation.getChildLocationIds())) {
                parentLocation.setChildLocationIds(new HashSet<>());
            }
            parentLocation.getChildLocationIds().add(location.getId());
            locationRepo.save(parentLocation);
            log.debug("Added Child Location id: {} to Parent Location id: {}", location.getId(), parentLocation.getId());
            return parentLocation;
        });
    }
    
    private static String generateLocationId(Location location) {
        // Village Bhopati -> VIL_PAR_BHO
        return 
        location.getLocationType().name().substring(0, 3).toLowerCase() + 
        "_" +
        Stream.of(location.getParentLocation().getLocationName().toLowerCase().trim().split(" ")).map(s -> s.substring(0, s.length() > 5 ? 5 : s.length())).collect(Collectors.joining()) +
        "_" +
        Stream.of(location.getLocationName().toLowerCase().trim().split(" ")).map(s -> s.substring(0, s.length() > 5 ? 5 : s.length())).collect(Collectors.joining());
    }

    private Location getParentLocation(@NonNull String parentId, LocationType locationType) {
        Optional<Location> parentLocation = locationRepo.findById(parentId);
        switch (locationType) {
            case LocationType.STATE:
                if (parentLocation.isPresent() && parentLocation.get().getLocationType().equals(LocationType.COUNTRY)) {
                    return parentLocation.get();
                } else {
                    throw new RuntimeException(String.format("Parent location %s, location type COUNTRY not found/Invalid", parentId));
                }
            case LocationType.DISTRICT:
                if (parentLocation.isPresent() && parentLocation.get().getLocationType().equals(LocationType.STATE)) {
                    return parentLocation.get();
                } else {
                    throw new RuntimeException(String.format("Parent location %s, location type STATE not found/Invalid", parentId));
                }
            case LocationType.TOWN:
                if (parentLocation.isPresent() && parentLocation.get().getLocationType().equals(LocationType.DISTRICT)) {
                    return parentLocation.get();
                } else {
                    throw new RuntimeException(String.format("Parent location %s, location type DISTRICT not found/Invalid", parentId));
                }
            case LocationType.VILLAGE:
                if (parentLocation.isPresent() && parentLocation.get().getLocationType().equals(LocationType.TOWN)) {
                    return parentLocation.get();
                } else {
                    throw new RuntimeException(String.format("Parent location %s, location type TOWN not found/Invalid", parentId));
                }
            default:
                throw new RuntimeException("Invalid location type");
        }
    }

    private Set<String> getChildLocations(List<String> childLocationIds, LocationType parentLocationType) {
        return childLocationIds.stream().map(childLocationId -> {
            return getChildLocation(childLocationId, parentLocationType); 
        }).map(Location::getId).collect(Collectors.toSet());
    }
    private Location getChildLocation(@NonNull String childLocationId, LocationType parentLocationType) {
        Optional<Location> childLocation = locationRepo.findById(childLocationId);
        switch (parentLocationType) {
            case LocationType.COUNTRY:
                if (childLocation.isPresent() && childLocation.get().getLocationType().equals(LocationType.STATE)) {
                    return childLocation.get();
                } else {
                    throw new RuntimeException(String.format("Child location %s not found/Invalid", childLocationId));
                }
            case LocationType.STATE:
                if (childLocation.isPresent() && childLocation.get().getLocationType().equals(LocationType.DISTRICT)) {
                    return childLocation.get();
                } else {
                    throw new RuntimeException(String.format("Child location %s not found/Invalid", childLocationId));
                }
            case LocationType.DISTRICT:
                if (childLocation.isPresent() && childLocation.get().getLocationType().equals(LocationType.TOWN)) {
                    return childLocation.get();
                } else {
                    throw new RuntimeException(String.format("Child location %s not found/Invalid", childLocationId));
                }
            case LocationType.TOWN:
                if (childLocation.isPresent() && childLocation.get().getLocationType().equals(LocationType.VILLAGE)) {
                    return childLocation.get();
                } else {
                    throw new RuntimeException(String.format("Child location %s not found/Invalid", childLocationId));
                }
            default:
                throw new RuntimeException("Invalid location type");
        }
    }
    
    public Location getEarthLocation() {
        return locationRepo.findByLocationNameAndLocationType("Earth", LocationType.PLANET)
                .orElseThrow(() -> new RuntimeException("Earth location not found in database"));
    }
}

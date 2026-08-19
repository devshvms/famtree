package in.shvms.famt_be;

import in.shvms.famt_be.dto.LocationDto;
import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;
import in.shvms.famt_be.repositories.neo4j.LocationRepository;
import in.shvms.famt_be.service.LocationService;
import in.shvms.famt_be.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the global location tree: hierarchy rules, generated IDs and the
 * guards that stop a developer from deleting a node other records point at.
 */
class LocationHierarchyIT extends AbstractIntegrationTest {

    private static final String TENANT = "tenant-loc-it";
    private static final String ACTOR = "user-it";

    @Autowired
    private LocationService locationService;

    @Autowired
    private LocationRepository locationRepo;

    /**
     * Seeds the two node types the app refuses to create through its own API
     * (PLANET and COUNTRY are pre-populated by the Cypher script in production).
     */
    @BeforeEach
    void seedRootHierarchy() {
        locationRepo.deleteAll();

        Location earth = new Location();
        earth.setId("E");
        earth.setLocationName("Earth");
        earth.setLocationType(LocationType.PLANET);
        locationRepo.save(earth);

        Location india = new Location();
        india.setId("IN");
        india.setLocationName("India");
        india.setLocationType(LocationType.COUNTRY);
        india.setParentLocation(earth);
        locationRepo.save(india);
    }

    @Test
    void createsStateUnderCountryWithGeneratedId() {
        Location karnataka = locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN"));

        // id format is {type prefix}_{parent abbrev}_{name abbrev}
        assertThat(karnataka.getId()).isEqualTo("sta_india_karna");
        assertThat(locationService.getChildrenOfLocation("IN"))
                .extracting(Location::getLocationName)
                .contains("Karnataka");
    }

    @Test
    void buildsFullHierarchyPath() {
        locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN"));
        Location district = locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Bangalore Urban", LocationType.DISTRICT, "sta_india_karna"));

        assertThat(locationService.getLocationHierarchyPath(district.getId()))
                .isEqualTo("Earth > India > Karnataka > Bangalore Urban");
    }

    @Test
    void rejectsInvalidParentChildTypePairing() {
        // A DISTRICT may only hang off a STATE, never straight off a COUNTRY.
        assertThatThrownBy(() -> locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Bangalore Urban", LocationType.DISTRICT, "IN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be parent of");
    }

    @Test
    void rejectsDuplicateNameUnderSameParent() {
        locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN"));

        assertThatThrownBy(() -> locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void refusesToCreateCountriesThroughTheApi() {
        assertThatThrownBy(() -> locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Atlantis", LocationType.COUNTRY, "E")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesToDeleteALocationThatStillHasChildren() {
        locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN"));
        locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Bangalore Urban", LocationType.DISTRICT, "sta_india_karna"));

        assertThatThrownBy(() -> locationService.deleteLocation(TENANT, ACTOR, "sta_india_karna"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("child locations");
    }

    @Test
    void listsAncestorsOfADeepLocation() {
        locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Karnataka", LocationType.STATE, "IN"));
        Location district = locationService.createLocation(TENANT, ACTOR,
                new LocationDto("Bangalore Urban", LocationType.DISTRICT, "sta_india_karna"));

        assertThat(locationService.getAncestorsOfLocation(district.getId()))
                .extracting(Location::getLocationName)
                .containsExactlyInAnyOrder("Karnataka", "India", "Earth");
    }
}

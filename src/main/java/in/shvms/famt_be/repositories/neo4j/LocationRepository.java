package in.shvms.famt_be.repositories.neo4j;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.LocationType;

@Repository
public interface LocationRepository extends Neo4jRepository<Location, String> {

    List<Location> findAllByLocationType(LocationType locationType);
    
    Optional<Location> findByLocationNameAndLocationType(String locationName, LocationType locationType);
    
    @Query("MATCH (l:Location {locationName: $locationName})-[:PARENT]->(p:Location {id: $parentId}) RETURN l")
    Optional<Location> findByLocationNameAndParentLocationId(String locationName, String parentId);
    
    @Query("MATCH (l:Location {id: $locationId})-[:PARENT*]->(ancestor:Location) RETURN ancestor")
    List<Location> findAllAncestors(String locationId);
    
    @Query("MATCH (l:Location {id: $locationId})<-[:PARENT*]-(descendant:Location) RETURN descendant")
    List<Location> findAllDescendants(String locationId);
    
    @Query("MATCH (l:Location {id: $locationId})<-[:PARENT]-(child:Location) RETURN child")
    List<Location> findDirectChildren(String locationId);
    
    @Query("MATCH (l:Location) WHERE l.locationType = $locationType AND l.id CONTAINS $searchTerm RETURN l")
    List<Location> searchByTypeAndName(LocationType locationType, String searchTerm);
}
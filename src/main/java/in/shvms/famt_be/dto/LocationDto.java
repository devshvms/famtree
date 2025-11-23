package in.shvms.famt_be.dto;

import java.util.List;

import in.shvms.famt_be.entity.LocationType;
import io.micrometer.common.lang.NonNull;

public record LocationDto(
        @NonNull String locationName,
        @NonNull LocationType locationType,
        String parentLocationId, 
        List<String> childLocationIds) {
}

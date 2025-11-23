package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.Human;
import in.shvms.famt_be.entity.Location;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonView {
    private String id;
    private String fName;
    private String lName;
    private String curLocation;

    public static PersonView from(Human h) {
        if (h == null) return null;
        String curLocation = "Not Available";
        Location loc = h.getLivesIn() != null ? h.getLivesIn() : h.getLocationOfBirth();
        if (loc != null) {
            curLocation = loc.getId() +  ", " + loc.getParentLocation().getId();
        }
        return new PersonView(h.getId(), h.getFName(), h.getLName(), curLocation);
    }
}

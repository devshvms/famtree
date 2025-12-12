package in.shvms.famt_be.dto;

import in.shvms.famt_be.entity.Location;
import in.shvms.famt_be.entity.Person;
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

    public static PersonView from(Person h) {
        if (h == null) return null;
        String curLocation = "Not Available";
        Location loc = h.getCurrentResidence();
        if (loc != null) {
            curLocation = loc.getLocationName() +  ", " + loc.getParentLocation().getLocationName();
        }
        return new PersonView(h.getId().toString(), h.getFirstName(), h.getLastName(), curLocation);
    }
}

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
    private String town;

    public static PersonView from(Human h) {
        if (h == null) return null;
        String town = null;
        Location loc = h.getCurrentLocation() != null ? h.getCurrentLocation() : h.getLocationOfBirth();
        if (loc != null) {
            town = loc.getTown();
        }
        return new PersonView(h.getId(), h.getFName(), h.getLName(), town);
    }
}

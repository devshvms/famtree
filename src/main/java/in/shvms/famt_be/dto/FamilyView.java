package in.shvms.famt_be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyView {
    private PersonView person;
    private PersonView spouse; // current spouse if any (first in list)
    private List<PersonView> children = new ArrayList<>();
    // ancestors by level: index 0 = parents (level 1), index 1 = grandparents (level 2), etc.
    private List<List<PersonView>> ancestorsByLevel = new ArrayList<>();
}

package in.shvms.famt_be.controller;

import in.shvms.famt_be.entity.Human;
import in.shvms.famt_be.repo.HumanRepo;
import in.shvms.famt_be.dto.FamilyView;
import in.shvms.famt_be.dto.PersonView;
import in.shvms.famt_be.dto.LinkRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/humans")
public class HumanController {

    private final HumanRepo humanRepo;

    public HumanController(HumanRepo humanRepo) {
        this.humanRepo = humanRepo;
    }

    @GetMapping
    public List<Human> getAll() {
        return humanRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Human> getById(@PathVariable String id) {
        return humanRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Human> create(@RequestBody Human human) {
        if (human.getId() == null) {
            // Let Mongo generate an ObjectId if client didn't provide one
            human.setId(null);
        }
        Human saved = humanRepo.save(human);
        return ResponseEntity.created(URI.create("/api/humans/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Human> update(@PathVariable String id, @RequestBody Human human) {
        return humanRepo.findById(id)
                .map(existing -> {
                    return ResponseEntity.ok(humanRepo.save(human));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        return humanRepo.findById(id)
                .map(existing -> {
                    humanRepo.deleteById(id);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/father")
    public ResponseEntity<Human> setFather(@PathVariable String id, @RequestBody LinkRequest req) {
        if (req == null || req.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return humanRepo.findById(id).map(person ->
            humanRepo.findById(req.getId()).map(father -> {
                person.setBiologicalFather(father);
                return ResponseEntity.ok(humanRepo.save(person));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/mother")
    public ResponseEntity<Human> setMother(@PathVariable String id, @RequestBody LinkRequest req) {
        if (req == null || req.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return humanRepo.findById(id).map(person ->
            humanRepo.findById(req.getId()).map(mother -> {
                person.setBiologicalMother(mother);
                return ResponseEntity.ok(humanRepo.save(person));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/spouse")
    public ResponseEntity<Human> addSpouse(@PathVariable String id, @RequestBody LinkRequest req) {
        if (req == null || req.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return humanRepo.findById(id).map(person ->
            humanRepo.findById(req.getId()).map(spouse -> {
                Set<Human> spouses = person.getSpouses();
                if (spouses == null) spouses = new HashSet<>();
                boolean exists = spouses.stream().anyMatch(h -> h.getId() != null && h.getId().equals(spouse.getId()));
                if (!exists) spouses.add(spouse);
                person.setSpouses(spouses);
                return ResponseEntity.ok(humanRepo.save(person));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/children")
    public ResponseEntity<Human> addChild(@PathVariable String id, @RequestBody LinkRequest req) {
        if (req == null || req.getId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return humanRepo.findById(id).map(parent ->
            humanRepo.findById(req.getId()).map(child -> {
                Set<Human> children = parent.getChildren();
                if (children == null) children = new HashSet<>();
                boolean exists = children.stream().anyMatch(h -> h.getId() != null && h.getId().equals(child.getId()));
                if (!exists) children.add(child);
                parent.setChildren(children);
                return ResponseEntity.ok(humanRepo.save(parent));
            }).orElse(ResponseEntity.notFound().build())
        ).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/family")
    public ResponseEntity<FamilyView> getFamily(
            @PathVariable String id,
            @RequestParam(name = "levels", defaultValue = "1") int levels
    ) {
        return humanRepo.findById(id)
                .map(root -> ResponseEntity.ok(buildFamilyView(root, Math.max(0, levels))))
                .orElse(ResponseEntity.notFound().build());
    }

    private FamilyView buildFamilyView(Human root, int levels) {
        FamilyView fv = new FamilyView();
        fv.setPerson(PersonView.from(root));

        // current spouse: pick the first if any
        if (root.getSpouses() != null && !root.getSpouses().isEmpty()) {
            fv.setSpouse(PersonView.from(root.getSpouses().stream().findFirst().get())); // simplistic current spouse
        }

        // children
        if (root.getChildren() != null) {
            List<PersonView> kids = new ArrayList<>();
            for (Human c : root.getChildren()) {
                kids.add(PersonView.from(c));
            }
            fv.setChildren(kids);
        }

        // ancestors by level (parents = level 1)
        List<List<PersonView>> ancestorsByLevel = new ArrayList<>();
        if (levels > 0) {
            Set<String> visited = new HashSet<>();
            visited.add(root.getId());

            List<Human> currentLevel = new ArrayList<>();
            if (root.getBiologicalFather() != null) currentLevel.add(root.getBiologicalFather());
            if (root.getBiologicalMother() != null) currentLevel.add(root.getBiologicalMother());

            for (int depth = 1; depth <= levels && !currentLevel.isEmpty(); depth++) {
                List<PersonView> viewLevel = new ArrayList<>();
                List<Human> nextLevel = new ArrayList<>();

                for (Human h : currentLevel) {
                    if (h == null) continue;
                    if (h.getId() != null && !visited.add(h.getId())) {
                        continue; // avoid cycles
                    }
                    viewLevel.add(PersonView.from(h));

                    if (h.getBiologicalFather() != null) nextLevel.add(h.getBiologicalFather());
                    if (h.getBiologicalMother() != null) nextLevel.add(h.getBiologicalMother());
                }

                if (!viewLevel.isEmpty()) {
                    ancestorsByLevel.add(viewLevel);
                }
                currentLevel = nextLevel;
            }
        }
        fv.setAncestorsByLevel(ancestorsByLevel);

        return fv;
    }
}

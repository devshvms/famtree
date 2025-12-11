package in.shvms.famt_be.service;

import in.shvms.famt_be.entity.Person;
import in.shvms.famt_be.repositories.neo4j.PersonRepository;
import org.springframework.stereotype.Service;

@Service
public class FamilyTreeService {

    private final PersonRepository personRepository;

    public FamilyTreeService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public Person addPerson(Person person) {
        return personRepository.save(person);
    }
}

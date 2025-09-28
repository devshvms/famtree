package in.shvms.famt_be.entity;

import org.springframework.data.annotation.Id;


public class Location {
    @Id
    String id;
    String town;
    String district;
    String state;
    Country country;
}

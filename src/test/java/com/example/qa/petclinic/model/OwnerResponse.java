package com.example.qa.petclinic.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnerResponse(Integer id, String firstName, String lastName, String address,
                            String city, String telephone, List<Object> pets) {

    public OwnerFields fields() {
        return new OwnerFields(firstName, lastName, address, city, telephone);
    }
}

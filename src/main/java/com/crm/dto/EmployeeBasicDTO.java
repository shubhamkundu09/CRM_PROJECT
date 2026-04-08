package com.crm.dto;

import com.crm.util.DecryptDeserializer;
import com.crm.util.EncryptSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
public class EmployeeBasicDTO {
    @JsonSerialize(using = EncryptSerializer.class)
    @JsonDeserialize(using = DecryptDeserializer.class)
    private Long id;

    private String firstName;
    private String lastName;
    private String email;
}
package com.example.coreapp.party.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePartyRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @NotBlank @Pattern(regexp = "CUSTOMER|SUPPLIER", message = "type must be CUSTOMER or SUPPLIER") String type
) {
}


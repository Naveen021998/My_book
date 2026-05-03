package com.example.coreapp.party.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePartyRequest(
        @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Pattern(regexp = "CUSTOMER|SUPPLIER", message = "type must be CUSTOMER or SUPPLIER") String type
) {
}


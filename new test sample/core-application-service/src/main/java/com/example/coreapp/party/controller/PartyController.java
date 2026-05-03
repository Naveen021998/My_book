package com.example.coreapp.party.controller;

import com.example.coreapp.common.web.PagedResponse;
import com.example.coreapp.party.dto.CreatePartyRequest;
import com.example.coreapp.party.dto.PartyResponse;
import com.example.coreapp.party.dto.UpdatePartyRequest;
import com.example.coreapp.party.service.PartyService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/parties")
public class PartyController {

    private final PartyService partyService;

    public PartyController(PartyService partyService) {
        this.partyService = partyService;
    }

    @PostMapping
    public ResponseEntity<PartyResponse> create(@Valid @RequestBody CreatePartyRequest request) {
        PartyResponse response = partyService.createParty(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<PartyResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(partyService.getParties(page, size, query));
    }

    @GetMapping("/{partyId}")
    public ResponseEntity<PartyResponse> get(@PathVariable long partyId) {
        return ResponseEntity.ok(partyService.getParty(partyId));
    }

    @PatchMapping("/{partyId}")
    public ResponseEntity<PartyResponse> update(@PathVariable long partyId, @Valid @RequestBody UpdatePartyRequest request) {
        return ResponseEntity.ok(partyService.updateParty(partyId, request));
    }

    @DeleteMapping("/{partyId}")
    public ResponseEntity<Void> delete(@PathVariable long partyId) {
        partyService.deleteParty(partyId);
        return ResponseEntity.noContent().build();
    }
}

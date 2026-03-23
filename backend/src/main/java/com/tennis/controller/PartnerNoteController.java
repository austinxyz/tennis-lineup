package com.tennis.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tennis.model.PartnerNote;
import com.tennis.service.PartnerNoteService;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams/{teamId}/partner-notes")
public class PartnerNoteController {

    private final PartnerNoteService partnerNoteService;

    @Autowired
    public PartnerNoteController(PartnerNoteService partnerNoteService) {
        this.partnerNoteService = partnerNoteService;
    }

    @GetMapping
    public ResponseEntity<List<PartnerNote>> list(@PathVariable String teamId) {
        return ResponseEntity.ok(partnerNoteService.list(teamId));
    }

    @PostMapping
    public ResponseEntity<PartnerNote> upsert(@PathVariable String teamId, @RequestBody UpsertRequest req) {
        PartnerNote note = partnerNoteService.upsert(
                teamId,
                req.getPlayer1Id(),
                req.getPlayer2Id(),
                req.getPlayer1Name(),
                req.getPlayer2Name(),
                req.getNote());
        return ResponseEntity.ok(note);
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<PartnerNote> update(
            @PathVariable String teamId,
            @PathVariable String noteId,
            @RequestBody UpdateRequest req) {
        return ResponseEntity.ok(partnerNoteService.update(teamId, noteId, req.getNote()));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable String teamId, @PathVariable String noteId) {
        partnerNoteService.delete(teamId, noteId);
        return ResponseEntity.noContent().build();
    }

    @Data
    @NoArgsConstructor
    static class UpsertRequest {
        @JsonProperty("player1Id")
        private String player1Id;
        @JsonProperty("player2Id")
        private String player2Id;
        @JsonProperty("player1Name")
        private String player1Name;
        @JsonProperty("player2Name")
        private String player2Name;
        @JsonProperty("note")
        private String note;
    }

    @Data
    @NoArgsConstructor
    static class UpdateRequest {
        @JsonProperty("note")
        private String note;
    }
}

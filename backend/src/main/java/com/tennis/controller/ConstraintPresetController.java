package com.tennis.controller;

import com.tennis.model.ConstraintPreset;
import com.tennis.service.ConstraintPresetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ConstraintPresetController {

    private final ConstraintPresetService constraintPresetService;

    @Autowired
    public ConstraintPresetController(ConstraintPresetService constraintPresetService) {
        this.constraintPresetService = constraintPresetService;
    }

    @GetMapping("/api/teams/{id}/constraint-presets")
    public ResponseEntity<List<ConstraintPreset>> listPresets(@PathVariable String id) {
        return ResponseEntity.ok(constraintPresetService.listPresets(id));
    }

    @PostMapping("/api/teams/{id}/constraint-presets")
    public ResponseEntity<ConstraintPreset> createPreset(@PathVariable String id,
                                                          @RequestBody ConstraintPreset preset) {
        return ResponseEntity.ok(constraintPresetService.createPreset(id, preset));
    }

    @DeleteMapping("/api/teams/{id}/constraint-presets/{presetId}")
    public ResponseEntity<Void> deletePreset(@PathVariable String id,
                                              @PathVariable String presetId) {
        constraintPresetService.deletePreset(id, presetId);
        return ResponseEntity.noContent().build();
    }
}

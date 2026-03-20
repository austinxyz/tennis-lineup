package com.tennis.service;

import com.tennis.exception.NotFoundException;
import com.tennis.model.ConfigData;
import com.tennis.model.ConstraintPreset;
import com.tennis.repository.JsonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class ConstraintPresetService {

    private final JsonRepository jsonRepository;

    @Autowired
    public ConstraintPresetService(JsonRepository jsonRepository) {
        this.jsonRepository = jsonRepository;
    }

    public List<ConstraintPreset> listPresets(String teamId) {
        ConfigData config = jsonRepository.readConfig();
        List<ConstraintPreset> presets = config.getConstraintPresets().getOrDefault(teamId, new ArrayList<>());
        return presets.stream()
                .sorted(Comparator.comparing(ConstraintPreset::getCreatedAt).reversed())
                .toList();
    }

    public ConstraintPreset createPreset(String teamId, ConstraintPreset preset) {
        preset.setId("preset-" + System.nanoTime());
        preset.setCreatedAt(Instant.now());

        ConfigData config = jsonRepository.readConfig();
        config.getConstraintPresets()
                .computeIfAbsent(teamId, k -> new ArrayList<>())
                .add(preset);
        jsonRepository.writeConfig(config);

        log.info("Created constraint preset {} for team {}", preset.getId(), teamId);
        return preset;
    }

    public void deletePreset(String teamId, String presetId) {
        ConfigData config = jsonRepository.readConfig();
        List<ConstraintPreset> presets = config.getConstraintPresets().get(teamId);
        if (presets == null || presets.stream().noneMatch(p -> presetId.equals(p.getId()))) {
            throw new NotFoundException("约束预设不存在");
        }
        presets.removeIf(p -> presetId.equals(p.getId()));
        jsonRepository.writeConfig(config);
        log.info("Deleted constraint preset {} for team {}", presetId, teamId);
    }
}

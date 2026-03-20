package com.tennis.service;

import com.tennis.exception.NotFoundException;
import com.tennis.model.ConfigData;
import com.tennis.model.ConstraintPreset;
import com.tennis.repository.JsonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConstraintPresetService Test")
class ConstraintPresetServiceTest {

    @Mock
    private JsonRepository jsonRepository;

    @InjectMocks
    private ConstraintPresetService constraintPresetService;

    private final String TEAM_ID = "team-1";

    @BeforeEach
    void setUp() {
        // Default: readConfig returns empty ConfigData
        when(jsonRepository.readConfig()).thenReturn(new ConfigData());
    }

    @Test
    @DisplayName("listPresets returns empty list when no presets exist")
    void listPresetsReturnsEmptyWhenNone() {
        List<ConstraintPreset> presets = constraintPresetService.listPresets(TEAM_ID);
        assertNotNull(presets);
        assertTrue(presets.isEmpty());
    }

    @Test
    @DisplayName("listPresets returns sorted presets newest first")
    void listPresetsSortedNewestFirst() {
        ConfigData config = new ConfigData();
        ConstraintPreset older = new ConstraintPreset();
        older.setId("preset-1");
        older.setName("Older");
        older.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));

        ConstraintPreset newer = new ConstraintPreset();
        newer.setId("preset-2");
        newer.setName("Newer");
        newer.setCreatedAt(Instant.parse("2025-06-01T00:00:00Z"));

        config.getConstraintPresets().put(TEAM_ID, new ArrayList<>(List.of(older, newer)));
        when(jsonRepository.readConfig()).thenReturn(config);

        List<ConstraintPreset> result = constraintPresetService.listPresets(TEAM_ID);

        assertEquals(2, result.size());
        assertEquals("preset-2", result.get(0).getId());
        assertEquals("preset-1", result.get(1).getId());
    }

    @Test
    @DisplayName("createPreset assigns id and createdAt, persists to ConfigData")
    void createPresetAssignsIdAndPersists() {
        ConstraintPreset preset = new ConstraintPreset();
        preset.setName("My Preset");
        doNothing().when(jsonRepository).writeConfig(any(ConfigData.class));

        ConstraintPreset created = constraintPresetService.createPreset(TEAM_ID, preset);

        assertNotNull(created.getId());
        assertTrue(created.getId().startsWith("preset-"));
        assertNotNull(created.getCreatedAt());
        assertEquals("My Preset", created.getName());
        verify(jsonRepository).writeConfig(any(ConfigData.class));
    }

    @Test
    @DisplayName("createPreset stores preset under correct teamId")
    void createPresetStoresUnderCorrectTeam() {
        ConstraintPreset preset = new ConstraintPreset();
        preset.setName("Team Preset");

        // Capture what gets written
        ConfigData[] written = new ConfigData[1];
        doAnswer(inv -> { written[0] = inv.getArgument(0); return null; })
                .when(jsonRepository).writeConfig(any(ConfigData.class));

        constraintPresetService.createPreset(TEAM_ID, preset);

        assertNotNull(written[0]);
        assertTrue(written[0].getConstraintPresets().containsKey(TEAM_ID));
        assertEquals(1, written[0].getConstraintPresets().get(TEAM_ID).size());
        assertEquals("Team Preset", written[0].getConstraintPresets().get(TEAM_ID).get(0).getName());
    }

    @Test
    @DisplayName("deletePreset removes preset and persists")
    void deletePresetRemovesAndPersists() {
        ConstraintPreset preset = new ConstraintPreset();
        preset.setId("preset-to-delete");
        preset.setName("Delete Me");
        preset.setCreatedAt(Instant.now());

        ConfigData config = new ConfigData();
        config.getConstraintPresets().put(TEAM_ID, new ArrayList<>(List.of(preset)));
        when(jsonRepository.readConfig()).thenReturn(config);
        doNothing().when(jsonRepository).writeConfig(any(ConfigData.class));

        assertDoesNotThrow(() -> constraintPresetService.deletePreset(TEAM_ID, "preset-to-delete"));
        verify(jsonRepository).writeConfig(any(ConfigData.class));
    }

    @Test
    @DisplayName("deletePreset throws NotFoundException when presetId not found")
    void deletePresetThrowsNotFoundWhenMissing() {
        assertThrows(NotFoundException.class,
                () -> constraintPresetService.deletePreset(TEAM_ID, "nonexistent-preset"));
    }

    @Test
    @DisplayName("deletePreset throws NotFoundException when team has no presets")
    void deletePresetThrowsWhenTeamHasNoPresets() {
        assertThrows(NotFoundException.class,
                () -> constraintPresetService.deletePreset("unknown-team", "any-preset"));
    }
}

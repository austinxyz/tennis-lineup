package com.tennis.service;

import com.tennis.model.PartnerNote;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
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
public class PartnerNoteService {

    private final JsonRepository jsonRepository;

    @Autowired
    public PartnerNoteService(JsonRepository jsonRepository) {
        this.jsonRepository = jsonRepository;
    }

    public List<PartnerNote> list(String teamId) {
        Team team = findTeam(teamId);
        List<PartnerNote> notes = team.getPartnerNotes();
        return notes == null ? List.of() : notes;
    }

    public PartnerNote upsert(String teamId, String player1Id, String player2Id, String player1Name, String player2Name, String note) {
        // Normalize pair order: use lexicographic min to ensure (A,B)==(B,A)
        String id1 = player1Id.compareTo(player2Id) <= 0 ? player1Id : player2Id;
        String id2 = player1Id.compareTo(player2Id) <= 0 ? player2Id : player1Id;
        String name1 = player1Id.compareTo(player2Id) <= 0 ? player1Name : player2Name;
        String name2 = player1Id.compareTo(player2Id) <= 0 ? player2Name : player1Name;

        TeamData teamData = jsonRepository.readData();
        Team team = findTeamInData(teamData, teamId);

        if (team.getPartnerNotes() == null) {
            team.setPartnerNotes(new ArrayList<>());
        }

        // Check if a note already exists for this pair
        PartnerNote existing = team.getPartnerNotes().stream()
                .filter(n -> n.getPlayer1Id().equals(id1) && n.getPlayer2Id().equals(id2))
                .findFirst()
                .orElse(null);

        Instant now = Instant.now();
        if (existing != null) {
            existing.setNote(note);
            existing.setPlayer1Name(name1);
            existing.setPlayer2Name(name2);
            existing.setUpdatedAt(now);
            jsonRepository.writeData(teamData);
            return existing;
        }

        PartnerNote newNote = new PartnerNote();
        newNote.setId("pn-" + System.nanoTime());
        newNote.setTeamId(teamId);
        newNote.setPlayer1Id(id1);
        newNote.setPlayer2Id(id2);
        newNote.setPlayer1Name(name1);
        newNote.setPlayer2Name(name2);
        newNote.setNote(note);
        newNote.setCreatedAt(now);
        newNote.setUpdatedAt(now);

        team.getPartnerNotes().add(newNote);
        jsonRepository.writeData(teamData);
        log.info("Created partner note {} for team {}", newNote.getId(), teamId);
        return newNote;
    }

    public PartnerNote update(String teamId, String noteId, String note) {
        TeamData teamData = jsonRepository.readData();
        Team team = findTeamInData(teamData, teamId);

        PartnerNote existing = team.getPartnerNotes().stream()
                .filter(n -> n.getId().equals(noteId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("搭档笔记不存在"));

        existing.setNote(note);
        existing.setUpdatedAt(Instant.now());
        jsonRepository.writeData(teamData);
        log.info("Updated partner note {} in team {}", noteId, teamId);
        return existing;
    }

    public void delete(String teamId, String noteId) {
        TeamData teamData = jsonRepository.readData();
        Team team = findTeamInData(teamData, teamId);

        boolean removed = team.getPartnerNotes().removeIf(n -> n.getId().equals(noteId));
        if (!removed) {
            throw new IllegalArgumentException("搭档笔记不存在");
        }
        jsonRepository.writeData(teamData);
        log.info("Deleted partner note {} from team {}", noteId, teamId);
    }

    /** Returns up to maxCount most-recently-updated notes for use in AI prompts. */
    public List<PartnerNote> listRecent(String teamId, int maxCount) {
        return list(teamId).stream()
                .filter(n -> n.getUpdatedAt() != null)
                .sorted(Comparator.comparing(PartnerNote::getUpdatedAt).reversed())
                .limit(maxCount)
                .toList();
    }

    private Team findTeam(String teamId) {
        TeamData teamData = jsonRepository.readData();
        return findTeamInData(teamData, teamId);
    }

    private Team findTeamInData(TeamData teamData, String teamId) {
        return teamData.getTeams().stream()
                .filter(t -> t.getId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("队伍不存在"));
    }
}

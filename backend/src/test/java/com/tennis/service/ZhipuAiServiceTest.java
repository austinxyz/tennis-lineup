package com.tennis.service;

import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("ZhipuAiService Test")
class ZhipuAiServiceTest {

    private ZhipuAiService service;

    @BeforeEach
    void setUp() {
        service = new ZhipuAiService();
    }

    private Lineup buildLineup(String strategy, double d1, double d2, double d3, double d4) {
        Lineup lineup = new Lineup();
        lineup.setStrategy(strategy);
        lineup.setTotalUtr(d1 + d2 + d3 + d4);
        lineup.setPairs(Arrays.asList(
            buildPair("D1", "Alice", "Bob", d1),
            buildPair("D2", "Carol", "Dave", d2),
            buildPair("D3", "Eve", "Frank", d3),
            buildPair("D4", "Grace", "Hank", d4)
        ));
        return lineup;
    }

    private Pair buildPair(String pos, String name1, String name2, double combined) {
        Pair pair = new Pair();
        pair.setPosition(pos);
        pair.setPlayer1Name(name1);
        pair.setPlayer2Name(name2);
        pair.setCombinedUtr(combined);
        return pair;
    }

    @Test
    @DisplayName("Returns -1 when API key is not configured")
    void testReturnsNegativeOneWhenNoApiKey() {
        ReflectionTestUtils.setField(service, "apiKey", "");
        List<Lineup> candidates = List.of(buildLineup("balanced", 14.0, 12.0, 10.0, 8.0));
        int result = service.selectBestLineup(candidates, "均衡");
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("Returns -1 for empty candidate list")
    void testReturnsNegativeOneForEmptyCandidates() {
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        int result = service.selectBestLineup(new ArrayList<>(), "balanced");
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("parseIndexFromContent returns correct 0-based index for valid content")
    void testParseValidContent() {
        int result = service.parseIndexFromContent("2", 3);
        assertEquals(1, result); // 1-based "2" → 0-based index 1
    }

    @Test
    @DisplayName("parseIndexFromContent returns -1 for out-of-range index")
    void testParseOutOfRangeContent() {
        int result = service.parseIndexFromContent("99", 3);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("parseIndexFromContent returns -1 for non-numeric content")
    void testParseNonNumericContent() {
        int result = service.parseIndexFromContent("invalid", 3);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("parseIndexFromContent returns -1 for null content")
    void testParseNullContent() {
        int result = service.parseIndexFromContent(null, 3);
        assertEquals(-1, result);
    }

    @Test
    @DisplayName("parseResult extracts index and explanation from tab-separated content")
    void testParseResultTabSeparated() {
        ZhipuAiService.AiResult result = service.parseResult("2\t己方D1组合UTR优势明显", 3);
        assertEquals(1, result.index());
        assertEquals("己方D1组合UTR优势明显", result.explanation());
    }

    @Test
    @DisplayName("parseResult handles index-only content (no tab)")
    void testParseResultIndexOnly() {
        ZhipuAiService.AiResult result = service.parseResult("1", 3);
        assertEquals(0, result.index());
        assertNull(result.explanation());
    }

    @Test
    @DisplayName("parseResult returns -1 index for out-of-range with tab")
    void testParseResultOutOfRangeWithTab() {
        ZhipuAiService.AiResult result = service.parseResult("9\t理由", 3);
        assertEquals(-1, result.index());
    }

    @Test
    @DisplayName("buildPrompt includes strategy and lineup positions")
    void testBuildPromptIncludesRequiredContent() {
        List<Lineup> candidates = List.of(buildLineup("balanced", 14.0, 12.0, 10.0, 8.0));
        String prompt = service.buildPrompt(candidates, "让前三线尽量强");
        assertTrue(prompt.contains("让前三线尽量强"));
        assertTrue(prompt.contains("D1"));
        assertTrue(prompt.contains("D4"));
        assertTrue(prompt.contains("总UTR"));
    }

    @Test
    @DisplayName("buildPrompt includes player notes when notes is non-empty")
    void testBuildPromptIncludesNotes() {
        Lineup lineup = new Lineup();
        lineup.setStrategy("balanced");
        lineup.setTotalUtr(12.0);
        Pair pair = new Pair();
        pair.setPosition("D1");
        pair.setPlayer1Name("张三");
        pair.setPlayer1Utr(6.0);
        pair.setPlayer1Notes("正手强");
        pair.setPlayer2Name("李四");
        pair.setPlayer2Utr(6.0);
        pair.setPlayer2Notes(null);
        pair.setCombinedUtr(12.0);
        lineup.setPairs(List.of(pair));

        String prompt = service.buildPrompt(List.of(lineup), "均衡");
        assertTrue(prompt.contains("备注:正手强"), "Prompt should contain player notes");
        assertTrue(prompt.contains("张三(UTR 6.0, 备注:正手强)"));
        assertTrue(prompt.contains("李四(UTR 6.0)"), "Player without notes should not have 备注 label");
    }

    @Test
    @DisplayName("buildPrompt format is unchanged when notes is null or blank")
    void testBuildPromptWithoutNotes() {
        List<Lineup> candidates = List.of(buildLineup("balanced", 14.0, 12.0, 10.0, 8.0));
        String prompt = service.buildPrompt(candidates, "均衡");
        assertFalse(prompt.contains("备注:"), "Prompt should not contain 备注 when notes are null");
        // buildPair doesn't set individual UTRs, so player appears without UTR parenthetical
        assertTrue(prompt.contains("Alice"));
        assertFalse(prompt.contains("Alice(UTR"));
    }

    @Test
    @DisplayName("parseCommentaryResult parses valid tab-separated content")
    void testParseCommentaryResultValid() {
        String content = "D1\t己方UTR优势，主动进攻\nD2\t势均力敌\nD3\t略占优势\nD4\t劣势，稳守";
        Map<String, String> result = service.parseCommentaryResult(content);
        assertEquals(4, result.size());
        assertEquals("己方UTR优势，主动进攻", result.get("D1"));
        assertEquals("势均力敌", result.get("D2"));
    }

    @Test
    @DisplayName("parseCommentaryResult handles partial lines gracefully")
    void testParseCommentaryResultPartial() {
        String content = "D1\t评析1\nD2\t评析2";
        Map<String, String> result = service.parseCommentaryResult(content);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("D1"));
        assertTrue(result.containsKey("D2"));
        assertFalse(result.containsKey("D3"));
    }

    @Test
    @DisplayName("parseCommentaryResult returns empty map for null content")
    void testParseCommentaryResultNull() {
        Map<String, String> result = service.parseCommentaryResult(null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("parseCommentaryResult returns empty map for blank content")
    void testParseCommentaryResultBlank() {
        Map<String, String> result = service.parseCommentaryResult("   ");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("buildCommentaryPrompt contains own and opponent players")
    void testBuildCommentaryPromptContent() {
        Lineup own = buildLineupWithUtr("balanced", 12.0, 10.0, 9.0, 8.0);
        Lineup opp = buildLineupWithUtr("balanced", 11.0, 10.0, 10.0, 9.0);

        String prompt = service.buildCommentaryPrompt(own, opp);
        assertTrue(prompt.contains("D1"));
        assertTrue(prompt.contains("己方"));
        assertTrue(prompt.contains("对手"));
        assertTrue(prompt.contains("delta="));
    }

    private Lineup buildLineupWithUtr(String strategy, double d1, double d2, double d3, double d4) {
        Lineup lineup = new Lineup();
        lineup.setStrategy(strategy);
        lineup.setTotalUtr(d1 + d2 + d3 + d4);
        double[] utrs = {d1, d2, d3, d4};
        String[] positions = {"D1", "D2", "D3", "D4"};
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Pair pair = new Pair();
            pair.setPosition(positions[i]);
            pair.setPlayer1Name("己方" + positions[i] + "甲");
            pair.setPlayer1Utr(utrs[i] / 2);
            pair.setPlayer2Name("己方" + positions[i] + "乙");
            pair.setPlayer2Utr(utrs[i] / 2);
            pair.setCombinedUtr(utrs[i]);
            pairs.add(pair);
        }
        lineup.setPairs(pairs);
        return lineup;
    }

    @Test
    @DisplayName("buildPromptWithOpponent includes opponent player notes")
    void testBuildPromptWithOpponentIncludesNotes() {
        Lineup ownLineup = buildLineup("balanced", 14.0, 12.0, 10.0, 8.0);

        Lineup oppLineup = new Lineup();
        oppLineup.setTotalUtr(10.0);
        Pair oppPair = new Pair();
        oppPair.setPosition("D1");
        oppPair.setPlayer1Name("对手甲");
        oppPair.setPlayer1Utr(5.0);
        oppPair.setPlayer1Notes("发球强");
        oppPair.setPlayer2Name("对手乙");
        oppPair.setPlayer2Utr(5.0);
        oppPair.setPlayer2Notes(null);
        oppPair.setCombinedUtr(10.0);
        oppLineup.setPairs(List.of(oppPair));

        String prompt = service.buildPromptWithOpponent(List.of(ownLineup), "均衡", oppLineup);
        assertTrue(prompt.contains("对手甲(UTR 5.0, 备注:发球强)"));
        assertTrue(prompt.contains("对手乙(UTR 5.0)"));
    }

    // ── Partner Notes in Prompt ──────────────────────────────────────────────

    @Test
    @DisplayName("Partner notes section included when notes are provided")
    void testPartnerNotesSectionIncluded() {
        Lineup lineup = buildLineup("balanced", 10.0, 9.0, 8.0, 7.0);

        com.tennis.controller.LineupMatchupRequest.PartnerNoteDto ownNote =
                new com.tennis.controller.LineupMatchupRequest.PartnerNoteDto();
        ownNote.setPlayer1Name("张三");
        ownNote.setPlayer2Name("李四");
        ownNote.setNote("默契好，配合稳定");

        com.tennis.controller.LineupMatchupRequest.PartnerNoteDto oppNote =
                new com.tennis.controller.LineupMatchupRequest.PartnerNoteDto();
        oppNote.setPlayer1Name("对手甲");
        oppNote.setPlayer2Name("对手乙");
        oppNote.setNote("发球强，正手攻击力强");

        String prompt = service.buildPromptWithOpponent(
                List.of(lineup), "均衡", null,
                List.of(ownNote), List.of(oppNote));

        assertTrue(prompt.contains("搭档笔记"), "prompt should contain 搭档笔记 section");
        assertTrue(prompt.contains("[张三 + 李四]: 默契好，配合稳定"), "own partner note present");
        assertTrue(prompt.contains("[对手甲 + 对手乙]: 发球强，正手攻击力强"), "opponent partner note present");
        assertTrue(prompt.contains("己方："), "own section label present");
        assertTrue(prompt.contains("对手："), "opponent section label present");
    }

    @Test
    @DisplayName("Partner notes section absent when lists are empty")
    void testPartnerNotesSectionAbsentWhenEmpty() {
        Lineup lineup = buildLineup("balanced", 10.0, 9.0, 8.0, 7.0);

        String prompt = service.buildPromptWithOpponent(
                List.of(lineup), "均衡", null,
                List.of(), null);

        assertFalse(prompt.contains("搭档笔记"), "prompt should not contain 搭档笔记 section when no notes");
    }

    @Test
    @DisplayName("Own partner notes only: no opponent section added")
    void testOnlyOwnPartnerNotes() {
        Lineup lineup = buildLineup("balanced", 10.0, 9.0, 8.0, 7.0);

        com.tennis.controller.LineupMatchupRequest.PartnerNoteDto ownNote =
                new com.tennis.controller.LineupMatchupRequest.PartnerNoteDto();
        ownNote.setPlayer1Name("甲");
        ownNote.setPlayer2Name("乙");
        ownNote.setNote("配合好");

        String prompt = service.buildPromptWithOpponent(
                List.of(lineup), "均衡", null, List.of(ownNote), null);

        assertTrue(prompt.contains("搭档笔记"));
        assertTrue(prompt.contains("己方："));
        assertFalse(prompt.contains("对手："), "should not have opponent section when no opponent notes");
    }

}

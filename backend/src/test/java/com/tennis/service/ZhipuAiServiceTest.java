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

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("buildPrompt includes strategy and lineup positions")
    void testBuildPromptIncludesRequiredContent() {
        List<Lineup> candidates = List.of(buildLineup("balanced", 14.0, 12.0, 10.0, 8.0));
        String prompt = service.buildPrompt(candidates, "让前三线尽量强");
        assertTrue(prompt.contains("让前三线尽量强"));
        assertTrue(prompt.contains("D1"));
        assertTrue(prompt.contains("D4"));
        assertTrue(prompt.contains("总UTR"));
    }

}

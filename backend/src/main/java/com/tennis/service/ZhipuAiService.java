package com.tennis.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tennis.controller.LineupMatchupRequest;
import com.tennis.model.Lineup;
import com.tennis.model.Pair;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class ZhipuAiService {

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String MODEL = "glm-4-air";
    private static final int TIMEOUT_SECONDS = 30;

    @Value("${zhipu.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /** Result holder: lineup index (0-based) + optional explanation */
    public record AiResult(int index, String explanation) {}

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ChatResponse {
        private List<Choice> choices;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Choice {
        private Message message;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Message {
        private String content;
    }

    // --- Public API ---

    /** Returns index (0-based) only — backward compat */
    public int selectBestLineupWithOpponent(List<Lineup> candidates, String strategy, Lineup opponentLineup) {
        return selectBestWithResult(candidates, strategy, opponentLineup).index();
    }

    /** Returns index (0-based) only — no opponent context */
    public int selectBestLineup(List<Lineup> candidates, String strategy) {
        if (candidates.isEmpty()) return -1;
        return callWithPrompt(buildPrompt(candidates, strategy), candidates.size()).index();
    }

    /** Returns index + explanation */
    public AiResult selectBestWithResult(List<Lineup> candidates, String strategy, Lineup opponentLineup) {
        return selectBestWithResult(candidates, strategy, opponentLineup, null, null);
    }

    /** Returns index + explanation, with optional partner notes context */
    public AiResult selectBestWithResult(List<Lineup> candidates, String strategy, Lineup opponentLineup,
            List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes,
            List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) {
        return callWithPrompt(
                buildPromptWithOpponent(candidates, strategy, opponentLineup, ownPartnerNotes, opponentPartnerNotes),
                candidates.size());
    }

    // --- Prompt builders ---

    String buildPrompt(List<Lineup> candidates, String strategy) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位网球双打教练。请根据以下策略选出最合适的排阵编号（从1开始）。\n\n");
        sb.append("策略：").append(strategy).append("\n\n");
        sb.append("重要说明：请主要参考实际UTR（actualUtr）进行分析。如果实际UTR与官方UTR不同，以实际UTR为准。同时结合个人备注和搭档笔记进行综合判断。\n\n");
        sb.append("候选排阵：\n");
        for (int i = 0; i < candidates.size(); i++) {
            appendLineup(sb, i + 1, candidates.get(i));
        }
        sb.append("\n请回复格式：排阵编号<TAB>一句话理由（中文）。");
        return sb.toString();
    }

    String buildPromptWithOpponent(List<Lineup> candidates, String strategy, Lineup opponentLineup) {
        return buildPromptWithOpponent(candidates, strategy, opponentLineup, null, null);
    }

    String buildPromptWithOpponent(List<Lineup> candidates, String strategy, Lineup opponentLineup,
            List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes,
            List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位网球双打教练。请根据对手排阵和策略，选出最合适的己方排阵编号（从1开始）。\n\n");
        sb.append("策略：").append(strategy).append("\n\n");
        sb.append("重要说明：请主要参考实际UTR（actualUtr）进行分析。如果实际UTR与官方UTR不同，以实际UTR为准。同时结合个人备注和搭档笔记进行综合判断。\n\n");
        if (opponentLineup != null) {
            sb.append("对手排阵：\n");
            for (Pair pair : opponentLineup.getPairs()) {
                sb.append("   ").append(pair.getPosition()).append(": ")
                  .append(describePlayer(pair.getPlayer1Name(), pair.getPlayer1Utr(), pair.getPlayer1ActualUtr(), pair.getPlayer1Notes()))
                  .append(" + ")
                  .append(describePlayer(pair.getPlayer2Name(), pair.getPlayer2Utr(), pair.getPlayer2ActualUtr(), pair.getPlayer2Notes()));
                if (pair.getCombinedActualUtr() != null && Math.abs(pair.getCombinedActualUtr() - pair.getCombinedUtr()) > 0.01) {
                    sb.append(" (组合UTR=").append(String.format("%.1f", pair.getCombinedUtr()))
                      .append("/实际=").append(String.format("%.1f", pair.getCombinedActualUtr())).append(")");
                } else {
                    sb.append(" (组合UTR=").append(String.format("%.1f", pair.getCombinedUtr())).append(")");
                }
                sb.append("\n");
            }
            sb.append("\n");
        }
        appendPartnerNotesSection(sb, ownPartnerNotes, opponentPartnerNotes);
        sb.append("己方候选排阵：\n");
        for (int i = 0; i < candidates.size(); i++) {
            appendLineup(sb, i + 1, candidates.get(i));
        }
        sb.append("\n请回复格式：排阵编号<TAB>一句话理由（中文）。例如：2\t己方D1组合UTR优势明显，整体胜率最高。");
        return sb.toString();
    }

    private void appendPartnerNotesSection(StringBuilder sb,
            List<LineupMatchupRequest.PartnerNoteDto> ownNotes,
            List<LineupMatchupRequest.PartnerNoteDto> oppNotes) {
        boolean hasOwn = ownNotes != null && !ownNotes.isEmpty();
        boolean hasOpp = oppNotes != null && !oppNotes.isEmpty();
        if (!hasOwn && !hasOpp) return;

        sb.append("搭档笔记：\n");
        if (hasOwn) {
            sb.append("  己方：\n");
            for (LineupMatchupRequest.PartnerNoteDto n : ownNotes) {
                sb.append("    [").append(n.getPlayer1Name()).append(" + ").append(n.getPlayer2Name())
                  .append("]: ").append(n.getNote()).append("\n");
            }
        }
        if (hasOpp) {
            sb.append("  对手：\n");
            for (LineupMatchupRequest.PartnerNoteDto n : oppNotes) {
                sb.append("    [").append(n.getPlayer1Name()).append(" + ").append(n.getPlayer2Name())
                  .append("]: ").append(n.getNote()).append("\n");
            }
        }
        sb.append("\n");
    }

    private void appendLineup(StringBuilder sb, int num, Lineup lineup) {
        sb.append(num).append(". 总UTR=").append(String.format("%.1f", lineup.getTotalUtr()));
        if (lineup.getActualUtrSum() != null && Math.abs(lineup.getActualUtrSum() - lineup.getTotalUtr()) > 0.01) {
            sb.append(" (实际UTR=").append(String.format("%.1f", lineup.getActualUtrSum())).append(")");
        }
        sb.append("\n");
        for (Pair pair : lineup.getPairs()) {
            sb.append("   ").append(pair.getPosition()).append(": ")
              .append(describePlayer(pair.getPlayer1Name(), pair.getPlayer1Utr(), pair.getPlayer1ActualUtr(), pair.getPlayer1Notes()))
              .append(" + ")
              .append(describePlayer(pair.getPlayer2Name(), pair.getPlayer2Utr(), pair.getPlayer2ActualUtr(), pair.getPlayer2Notes()))
              .append(" (组合UTR=").append(String.format("%.1f", pair.getCombinedUtr())).append(")\n");
        }
    }

    private String describePlayer(String name, Double utr, String notes) {
        StringBuilder sb = new StringBuilder(name);
        if (utr != null) {
            sb.append("(UTR ").append(String.format("%.1f", utr));
            if (notes != null && !notes.isBlank()) {
                sb.append(", 备注:").append(notes);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private String describePlayer(String name, Double utr, Double actualUtr, String notes) {
        StringBuilder sb = new StringBuilder(name);
        if (utr != null) {
            sb.append("(UTR ").append(String.format("%.1f", utr));
            if (actualUtr != null && Math.abs(actualUtr - utr) > 0.01) {
                sb.append("/实际").append(String.format("%.1f", actualUtr));
            }
            if (notes != null && !notes.isBlank()) {
                sb.append(", 备注:").append(notes);
            }
            sb.append(")");
        }
        return sb.toString();
    }

    // --- Internal ---

    private AiResult callWithPrompt(String prompt, int candidateCount) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Zhipu AI API key not configured, using fallback");
            return new AiResult(-1, null);
        }
        log.info("Zhipu AI calling, key prefix=[{}]", apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);
        try {
            CompletableFuture<AiResult> future = CompletableFuture.supplyAsync(
                    () -> callHttpApi(prompt, candidateCount));
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Zhipu AI call timed out after {}s, using fallback", TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Zhipu AI call failed: {}, using fallback", e.getMessage());
        }
        return new AiResult(-1, null);
    }

    private AiResult callHttpApi(String prompt, int candidateCount) {
        try {
            Map<String, Object> sysMsg = Map.of("role", "system", "content",
                    "你只能回复「数字<TAB>一句话理由」格式，不得包含其他内容。");
            Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);
            Map<String, Object> bodyMap = Map.of(
                    "model", MODEL,
                    "messages", List.of(sysMsg, userMsg),
                    "max_tokens", 100,
                    "temperature", 0.1
            );
            String bodyJson = mapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Zhipu AI HTTP status: {}, body: {}", response.statusCode(), response.body());

            if (response.statusCode() != 200) {
                log.warn("Zhipu AI non-200 response: {} {}", response.statusCode(), response.body());
                return new AiResult(-1, null);
            }

            ChatResponse chatResponse = mapper.readValue(response.body(), ChatResponse.class);
            if (chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
                return new AiResult(-1, null);
            }

            Choice choice = chatResponse.getChoices().get(0);
            String content = choice.getMessage().getContent();
            log.info("Zhipu AI response: [{}], finishReason: [{}]", content, choice.getFinishReason());
            return parseResult(content, candidateCount);
        } catch (Exception e) {
            throw new RuntimeException("AI HTTP call failed: " + e.getMessage(), e);
        }
    }

    AiResult parseResult(String content, int candidateCount) {
        if (content == null) return new AiResult(-1, null);
        String trimmed = content.trim();
        // Try tab-separated format: "2\t理由"
        String explanation = null;
        String indexPart = trimmed;
        int tabIdx = trimmed.indexOf('\t');
        if (tabIdx > 0) {
            indexPart = trimmed.substring(0, tabIdx).trim();
            explanation = trimmed.substring(tabIdx + 1).trim();
            if (explanation.isEmpty()) explanation = null;
        }
        // Extract leading digits
        String digits = indexPart.replaceAll("^[^0-9]*([0-9]+).*", "$1");
        try {
            int index = Integer.parseInt(digits) - 1;
            if (index < 0 || index >= candidateCount) return new AiResult(-1, null);
            return new AiResult(index, explanation);
        } catch (NumberFormatException e) {
            log.warn("Could not parse AI response as lineup index: {}", content);
            return new AiResult(-1, null);
        }
    }

    /** Backward-compat single-int parse (used in existing tests) */
    int parseIndexFromContent(String content, int candidateCount) {
        return parseResult(content, candidateCount).index();
    }

    // --- Commentary (逐线评析) ---

    /**
     * Builds a prompt asking AI for line-by-line commentary on a specific matchup.
     * Expected AI response format: "D1\t评析\nD2\t评析\n..."
     */
    String buildCommentaryPrompt(Lineup ownLineup, Lineup oppLineup) {
        return buildCommentaryPrompt(ownLineup, oppLineup, null, null);
    }

    String buildCommentaryPrompt(Lineup ownLineup, Lineup oppLineup,
            List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes,
            List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位网球双打教练，请对以下己方与对手的逐线对比给出简短评析（每线一句话）。\n\n");
        sb.append("重要说明：请主要参考实际UTR（actualUtr）进行分析，并结合搭档笔记综合判断。\n\n");

        Map<String, Pair> ownByPos = pairsByPosition(ownLineup);
        Map<String, Pair> oppByPos = pairsByPosition(oppLineup);

        for (String pos : List.of("D1", "D2", "D3", "D4")) {
            Pair own = ownByPos.get(pos);
            Pair opp = oppByPos.get(pos);
            if (own == null || opp == null) continue;

            // Use actual UTR for delta when available
            double ownActual = effectiveActualCombinedUtr(own);
            double oppActual = effectiveActualCombinedUtr(opp);
            double delta = ownActual - oppActual;

            sb.append(pos).append(": 己方 ")
              .append(describePlayer(own.getPlayer1Name(), own.getPlayer1Utr(), own.getPlayer1ActualUtr(), own.getPlayer1Notes()))
              .append("+")
              .append(describePlayer(own.getPlayer2Name(), own.getPlayer2Utr(), own.getPlayer2ActualUtr(), own.getPlayer2Notes()))
              .append(" vs 对手 ")
              .append(describePlayer(opp.getPlayer1Name(), opp.getPlayer1Utr(), opp.getPlayer1ActualUtr(), opp.getPlayer1Notes()))
              .append("+")
              .append(describePlayer(opp.getPlayer2Name(), opp.getPlayer2Utr(), opp.getPlayer2ActualUtr(), opp.getPlayer2Notes()))
              .append(String.format(" (实际delta=%+.1f)\n", delta));
        }

        appendPartnerNotesSection(sb, ownPartnerNotes, opponentPartnerNotes);
        sb.append("\n请按以下格式回复（不得包含其他内容）：\nD1\t评析\nD2\t评析\nD3\t评析\nD4\t评析");
        return sb.toString();
    }

    private double effectiveActualCombinedUtr(Pair pair) {
        if (pair.getCombinedActualUtr() != null) return pair.getCombinedActualUtr();
        double u1 = pair.getPlayer1ActualUtr() != null ? pair.getPlayer1ActualUtr()
                : (pair.getPlayer1Utr() != null ? pair.getPlayer1Utr() : 0);
        double u2 = pair.getPlayer2ActualUtr() != null ? pair.getPlayer2ActualUtr()
                : (pair.getPlayer2Utr() != null ? pair.getPlayer2Utr() : 0);
        return u1 + u2;
    }

    private Map<String, Pair> pairsByPosition(Lineup lineup) {
        Map<String, Pair> map = new HashMap<>();
        if (lineup != null && lineup.getPairs() != null) {
            for (Pair p : lineup.getPairs()) {
                map.put(p.getPosition(), p);
            }
        }
        return map;
    }

    /**
     * Parses the "D1\t评析\nD2\t评析\n..." format returned by the AI for commentary.
     * Returns empty Map on parse failure.
     */
    Map<String, String> parseCommentaryResult(String content) {
        Map<String, String> result = new HashMap<>();
        if (content == null || content.isBlank()) return result;
        for (String line : content.split("\n")) {
            int tab = line.indexOf('\t');
            if (tab > 0) {
                String pos = line.substring(0, tab).trim();
                String commentary = line.substring(tab + 1).trim();
                if (!pos.isEmpty() && !commentary.isEmpty()) {
                    result.put(pos, commentary);
                }
            }
        }
        return result;
    }

    /**
     * Calls AI to get line-by-line commentary. Returns empty Map if AI unavailable.
     */
    public Map<String, String> getCommentary(Lineup ownLineup, Lineup oppLineup) {
        return getCommentary(ownLineup, oppLineup, null, null);
    }

    /**
     * Calls AI to get line-by-line commentary with optional partner notes context.
     * Returns empty Map if AI unavailable.
     */
    public Map<String, String> getCommentary(Lineup ownLineup, Lineup oppLineup,
            List<LineupMatchupRequest.PartnerNoteDto> ownPartnerNotes,
            List<LineupMatchupRequest.PartnerNoteDto> opponentPartnerNotes) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Zhipu AI API key not configured, skipping commentary");
            return Map.of();
        }
        String prompt = buildCommentaryPrompt(ownLineup, oppLineup, ownPartnerNotes, opponentPartnerNotes);
        try {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(
                    () -> callHttpApiForCommentary(prompt));
            String content = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return parseCommentaryResult(content);
        } catch (TimeoutException e) {
            log.warn("Zhipu AI commentary timed out after {}s", TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.warn("Zhipu AI commentary failed: {}", e.getMessage());
        }
        return Map.of();
    }

    private String callHttpApiForCommentary(String prompt) {
        try {
            Map<String, Object> sysMsg = Map.of("role", "system", "content",
                    "你只能按「位置<TAB>一句话评析」格式回复4行，不得包含其他内容。");
            Map<String, Object> userMsg = Map.of("role", "user", "content", prompt);
            Map<String, Object> bodyMap = Map.of(
                    "model", MODEL,
                    "messages", List.of(sysMsg, userMsg),
                    "max_tokens", 300,
                    "temperature", 0.3
            );
            String bodyJson = mapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Zhipu AI commentary non-200: {} {}", response.statusCode(), response.body());
                return null;
            }
            ChatResponse chatResponse = mapper.readValue(response.body(), ChatResponse.class);
            if (chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) return null;
            return chatResponse.getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new RuntimeException("AI commentary HTTP call failed: " + e.getMessage(), e);
        }
    }
}

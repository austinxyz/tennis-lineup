package com.tennis.repository;

import com.tennis.model.ConfigData;
import com.tennis.model.ConstraintPreset;
import com.tennis.model.Team;
import com.tennis.model.TeamData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonRepository Test")
class JsonRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonRepository jsonRepository;
    private Path testDataPath;
    private Path testConfigPath;

    @BeforeEach
    void setUp() {
        testDataPath = tempDir.resolve("test-data.json");
        testConfigPath = tempDir.resolve("test-config.json");
        jsonRepository = new JsonRepository(testDataPath.toString(), testConfigPath.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(testDataPath);
        Files.deleteIfExists(testConfigPath);
    }

    // ======================== Data file tests ========================

    @Test
    @DisplayName("Should create data file if it doesn't exist")
    void shouldCreateDataFileIfItDoesntExist() {
        assertTrue(Files.exists(testDataPath));
        TeamData data = jsonRepository.readData();
        assertNotNull(data);
        assertNotNull(data.getTeams());
    }

    @Test
    @DisplayName("Should read data from existing file")
    void shouldReadDataFromExistingFile() {
        TeamData originalData = new TeamData();
        originalData.getTeams().add(createTestTeam("team-1"));
        jsonRepository.writeData(originalData);

        TeamData readData = jsonRepository.readData();

        assertNotNull(readData);
        assertEquals(1, readData.getTeams().size());
        assertEquals("team-1", readData.getTeams().get(0).getId());
    }

    @Test
    @DisplayName("Should write data to file")
    void shouldWriteDataToFile() throws Exception {
        TeamData data = new TeamData();
        data.getTeams().add(createTestTeam("team-1"));
        jsonRepository.writeData(data);

        assertTrue(Files.exists(testDataPath));
        assertTrue(Files.size(testDataPath) > 0);
    }

    @Test
    @DisplayName("Should handle missing data file gracefully")
    void shouldHandleFileNotFoundExceptionGracefully() throws Exception {
        Files.deleteIfExists(testDataPath);
        TeamData data = jsonRepository.readData();
        assertNotNull(data);
        assertNotNull(data.getTeams());
        assertEquals(0, data.getTeams().size());
    }

    @Test
    @DisplayName("Should preserve data when writing multiple times")
    void shouldPreserveDataWhenWritingMultipleTimes() {
        TeamData data1 = new TeamData();
        data1.getTeams().add(createTestTeam("team-1"));
        TeamData data2 = new TeamData();
        data2.getTeams().add(createTestTeam("team-2"));

        jsonRepository.writeData(data1);
        TeamData readData1 = jsonRepository.readData();
        jsonRepository.writeData(data2);
        TeamData readData2 = jsonRepository.readData();

        assertEquals(1, readData1.getTeams().size());
        assertEquals("team-1", readData1.getTeams().get(0).getId());
        assertEquals(1, readData2.getTeams().size());
        assertEquals("team-2", readData2.getTeams().get(0).getId());
    }

    @Test
    @DisplayName("Should handle concurrent reads safely")
    void shouldHandleConcurrentReadsSafely() throws InterruptedException {
        TeamData data = new TeamData();
        IntStream.range(0, 100).forEach(i -> data.getTeams().add(createTestTeam("team-" + i)));
        jsonRepository.writeData(data);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<TeamData>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.countDown();
                latch.await();
                return jsonRepository.readData();
            }));
        }

        for (Future<TeamData> future : futures) {
            try {
                TeamData readData = future.get(5, TimeUnit.SECONDS);
                assertNotNull(readData);
                assertEquals(100, readData.getTeams().size());
            } catch (Exception e) {
                fail("Concurrent read failed: " + e.getMessage());
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Should handle concurrent writes safely")
    void shouldHandleConcurrentWritesSafely() throws InterruptedException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                latch.countDown();
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < 10; j++) {
                    TeamData d = new TeamData();
                    d.getTeams().add(createTestTeam("thread-" + threadNum + "-team-" + j));
                    jsonRepository.writeData(d);
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertNotNull(jsonRepository.readData());
        assertTrue(jsonRepository.readData().getTeams().size() >= 1);
    }

    @Test
    @DisplayName("Should maintain data consistency during concurrent read-write")
    void shouldMaintainDataConsistencyDuringConcurrentReadWrite() throws InterruptedException {
        jsonRepository.writeData(new TeamData());

        int readerCount = 5;
        int writerCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);
        CountDownLatch startLatch = new CountDownLatch(readerCount + writerCount);

        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < 10; j++) {
                    assertNotNull(jsonRepository.readData());
                    try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            });
        }

        for (int i = 0; i < writerCount; i++) {
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < 5; j++) {
                    TeamData d = new TeamData();
                    d.getTeams().add(createTestTeam("concurrent-team-" + j));
                    jsonRepository.writeData(d);
                    try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));
        assertNotNull(jsonRepository.readData());
    }

    @Test
    @DisplayName("Should handle large data sets")
    void shouldHandleLargeDataSets() {
        TeamData data = new TeamData();
        for (int i = 0; i < 1000; i++) {
            data.getTeams().add(createTestTeam("team-" + i));
        }
        jsonRepository.writeData(data);
        TeamData readData = jsonRepository.readData();

        assertEquals(1000, readData.getTeams().size());
        assertEquals("team-0", readData.getTeams().get(0).getId());
        assertEquals("team-999", readData.getTeams().get(999).getId());
    }

    @Test
    @DisplayName("Should use atomic write (temp file + rename)")
    void shouldUseAtomicWrite() throws InterruptedException {
        TeamData data = new TeamData();
        data.getTeams().add(createTestTeam("team-1"));
        jsonRepository.writeData(data);
        Thread.sleep(100);

        assertTrue(Files.exists(testDataPath));
        long tempFiles = java.util.Arrays.stream(tempDir.toFile().listFiles())
                .filter(f -> f.getName().endsWith(".tmp"))
                .count();
        assertEquals(0, tempFiles);
    }

    @Test
    @DisplayName("Should handle concurrent mixed reads and writes without data corruption")
    void shouldHandleConcurrentMixedReadsAndWritesWithoutDataCorruption() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (threadNum % 2 == 0) {
                            TeamData writeData = new TeamData();
                            writeData.getTeams().add(createTestTeam("thread-" + threadNum + "-team-" + j));
                            jsonRepository.writeData(writeData);
                        } else {
                            TeamData readData = jsonRepository.readData();
                            assertNotNull(readData);
                            assertNotNull(readData.getTeams());
                        }
                    }
                } catch (Exception e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), "Concurrent operations produced errors: " + errors);
        assertNotNull(jsonRepository.readData());
    }

    // ======================== Config file tests ========================

    @Test
    @DisplayName("Should create config file if it doesn't exist")
    void shouldCreateConfigFileIfItDoesntExist() {
        assertTrue(Files.exists(testConfigPath));
        ConfigData config = jsonRepository.readConfig();
        assertNotNull(config);
        assertNotNull(config.getConstraintPresets());
    }

    @Test
    @DisplayName("Should read and write config data correctly")
    void shouldReadAndWriteConfigData() {
        ConfigData config = new ConfigData();
        ConstraintPreset preset = new ConstraintPreset();
        preset.setId("preset-1");
        preset.setName("Test Preset");
        config.getConstraintPresets().computeIfAbsent("team-1", k -> new ArrayList<>()).add(preset);

        jsonRepository.writeConfig(config);
        ConfigData readConfig = jsonRepository.readConfig();

        assertNotNull(readConfig);
        assertTrue(readConfig.getConstraintPresets().containsKey("team-1"));
        assertEquals(1, readConfig.getConstraintPresets().get("team-1").size());
        assertEquals("preset-1", readConfig.getConstraintPresets().get("team-1").get(0).getId());
        assertEquals("Test Preset", readConfig.getConstraintPresets().get("team-1").get(0).getName());
    }

    @Test
    @DisplayName("Should return empty config when config file missing")
    void shouldReturnEmptyConfigWhenFileMissing() throws IOException {
        Files.deleteIfExists(testConfigPath);
        ConfigData config = jsonRepository.readConfig();
        assertNotNull(config);
        assertNotNull(config.getConstraintPresets());
        assertTrue(config.getConstraintPresets().isEmpty());
    }

    @Test
    @DisplayName("Data and config locks are independent")
    void dataAndConfigLocksAreIndependent() throws InterruptedException {
        // Write config and data concurrently — should not deadlock
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount / 2; i++) {
            executor.submit(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < 10; j++) {
                        jsonRepository.writeData(new TeamData());
                        jsonRepository.readData();
                    }
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            executor.submit(() -> {
                latch.countDown();
                try {
                    latch.await();
                    for (int j = 0; j < 10; j++) {
                        jsonRepository.writeConfig(new ConfigData());
                        jsonRepository.readConfig();
                    }
                } catch (Exception e) {
                    errors.add(e);
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(), "Concurrent data+config operations produced errors: " + errors);
    }

    // ======================== Helpers ========================

    private Team createTestTeam(String id) {
        Team team = new Team();
        team.setId(id);
        team.setName("Test Team " + id);
        team.setCreatedAt(java.time.Instant.now());
        team.setPlayers(List.of());
        team.setLineups(List.of());
        return team;
    }
}

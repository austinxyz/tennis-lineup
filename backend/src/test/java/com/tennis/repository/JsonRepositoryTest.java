package com.tennis.repository;

import com.tennis.model.Team;
import com.tennis.model.TeamData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @BeforeEach
    void setUp() {
        testDataPath = tempDir.resolve("test-data.json");
        jsonRepository = new JsonRepository(testDataPath.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        if (Files.exists(testDataPath)) {
            Files.delete(testDataPath);
        }
    }

    @Test
    @DisplayName("Should create data file if it doesn't exist")
    void shouldCreateDataFileIfItDoesntExist() throws IOException {
        // File is created by the constructor (initializeDataFile)
        assertTrue(Files.exists(testDataPath));
        TeamData data = jsonRepository.readData();
        assertNotNull(data);
        assertTrue(data.getTeams() != null);
    }

    @Test
    @DisplayName("Should read data from existing file")
    void shouldReadDataFromExistingFile() throws IOException {
        // Arrange
        TeamData originalData = new TeamData();
        originalData.getTeams().add(createTestTeam("team-1"));

        // Write test data
        jsonRepository.writeData(originalData);

        // Act
        TeamData readData = jsonRepository.readData();

        // Assert
        assertNotNull(readData);
        assertEquals(1, readData.getTeams().size());
        assertEquals("team-1", readData.getTeams().get(0).getId());
    }

    @Test
    @DisplayName("Should write data to file")
    void shouldWriteDataToFile() throws IOException {
        // Arrange
        TeamData data = new TeamData();
        data.getTeams().add(createTestTeam("team-1"));

        // Act
        jsonRepository.writeData(data);

        // Assert
        assertTrue(Files.exists(testDataPath));
        assertTrue(Files.size(testDataPath) > 0);
    }

    @Test
    @DisplayName("Should handle file not found exception gracefully")
    void shouldHandleFileNotFoundExceptionGracefully() {
        // Arrange - delete the file if it exists
        try {
            Files.deleteIfExists(testDataPath);
        } catch (IOException e) {
            fail("Failed to delete test file");
        }

        // Act
        TeamData data = jsonRepository.readData();

        // Assert
        assertNotNull(data);
        assertTrue(data.getTeams() != null);
        assertEquals(0, data.getTeams().size());
    }

    @Test
    @DisplayName("Should preserve data when writing multiple times")
    void shouldPreserveDataWhenWritingMultipleTimes() throws IOException {
        // Arrange
        TeamData data1 = new TeamData();
        data1.getTeams().add(createTestTeam("team-1"));

        TeamData data2 = new TeamData();
        data2.getTeams().add(createTestTeam("team-2"));

        // Act
        jsonRepository.writeData(data1);
        TeamData readData1 = jsonRepository.readData();

        jsonRepository.writeData(data2);
        TeamData readData2 = jsonRepository.readData();

        // Assert
        assertEquals(1, readData1.getTeams().size());
        assertEquals("team-1", readData1.getTeams().get(0).getId());

        assertEquals(1, readData2.getTeams().size());
        assertEquals("team-2", readData2.getTeams().get(0).getId());
    }

    @Test
    @DisplayName("Should handle concurrent reads safely")
    void shouldHandleConcurrentReadsSafely() throws InterruptedException {
        // Arrange
        TeamData data = new TeamData();
        IntStream.range(0, 100).forEach(i -> {
            Team team = createTestTeam("team-" + i);
            data.getTeams().add(team);
        });
        jsonRepository.writeData(data);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Future<TeamData>> futures = new CopyOnWriteArrayList<>();

        // Act - create multiple concurrent readers
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                latch.countDown();
                latch.await();
                return jsonRepository.readData();
            }));
        }

        // Assert
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
        // Arrange
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        int writesPerThread = 10;

        // Act - create multiple concurrent writers
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                latch.countDown();
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < writesPerThread; j++) {
                    TeamData data = new TeamData();
                    Team team = createTestTeam("thread-" + threadNum + "-team-" + j);
                    data.getTeams().add(team);
                    jsonRepository.writeData(data);

                    // Small delay to increase chance of interleaving
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Wait for all writes to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Assert - final read should have at least one write
        TeamData finalData = jsonRepository.readData();
        assertNotNull(finalData);
        assertTrue(finalData.getTeams().size() >= 1);
    }

    @Test
    @DisplayName("Should maintain data consistency during concurrent read-write")
    void shouldMaintainDataConsistencyDuringConcurrentReadWrite() throws InterruptedException {
        // Arrange
        TeamData initialData = new TeamData();
        jsonRepository.writeData(initialData);

        int readerCount = 5;
        int writerCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(readerCount + writerCount);
        CountDownLatch startLatch = new CountDownLatch(readerCount + writerCount);

        // Create readers
        for (int i = 0; i < readerCount; i++) {
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < 10; j++) {
                    TeamData data = jsonRepository.readData();
                    assertNotNull(data);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Create writers
        for (int i = 0; i < writerCount; i++) {
            executor.submit(() -> {
                startLatch.countDown();
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                for (int j = 0; j < 5; j++) {
                    TeamData data = new TeamData();
                    Team team = createTestTeam("concurrent-team-" + j);
                    data.getTeams().add(team);
                    jsonRepository.writeData(data);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        // Wait for all operations
        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        // Assert - system should be in consistent state
        TeamData finalData = jsonRepository.readData();
        assertNotNull(finalData);
        assertTrue(finalData.getTeams() != null);
    }

    @Test
    @DisplayName("Should handle large data sets")
    void shouldHandleLargeDataSets() throws IOException {
        // Arrange
        TeamData data = new TeamData();
        for (int i = 0; i < 1000; i++) {
            Team team = createTestTeam("team-" + i);
            data.getTeams().add(team);
        }

        // Act
        jsonRepository.writeData(data);
        TeamData readData = jsonRepository.readData();

        // Assert
        assertEquals(1000, readData.getTeams().size());
        assertEquals("team-0", readData.getTeams().get(0).getId());
        assertEquals("team-999", readData.getTeams().get(999).getId());
    }

    @Test
    @DisplayName("Should use atomic write (temp file + rename)")
    void shouldUseAtomicWrite() throws IOException, InterruptedException {
        // Arrange
        TeamData data = new TeamData();
        data.getTeams().add(createTestTeam("team-1"));

        // Monitor file system activity
        Path parentDir = testDataPath.getParent();
        File[] filesBefore = parentDir.toFile().listFiles((dir, name) ->
            name.startsWith("test-data") || name.startsWith("test-data.tmp"));

        // Act
        jsonRepository.writeData(data);

        // Wait a bit for any async operations
        Thread.sleep(100);

        // Assert
        File[] filesAfter = parentDir.toFile().listFiles((dir, name) ->
            name.startsWith("test-data") || name.startsWith("test-data.tmp"));

        // Should have the target file but no orphaned temp files
        assertTrue(Files.exists(testDataPath));
        assertNotNull(filesAfter);

        // Count temp files
        long tempFiles = java.util.Arrays.stream(filesAfter)
            .filter(f -> f.getName().endsWith(".tmp"))
            .count();
        assertEquals(0, tempFiles);
    }

    @Test
    @DisplayName("Should handle concurrent mixed reads and writes without data corruption")
    void shouldHandleConcurrentMixedReadsAndWritesWithoutDataCorruption() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int operationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Exception> errors = new CopyOnWriteArrayList<>();

        // Act - half threads write, half threads read, all start simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (threadNum % 2 == 0) {
                            // Even threads write
                            TeamData writeData = new TeamData();
                            writeData.getTeams().add(createTestTeam("thread-" + threadNum + "-team-" + j));
                            jsonRepository.writeData(writeData);
                        } else {
                            // Odd threads read
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

        // Wait for all threads to finish
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Threads did not finish in time");
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Assert - no errors and final state is consistent
        assertTrue(errors.isEmpty(), "Concurrent operations produced errors: " + errors);
        TeamData finalData = jsonRepository.readData();
        assertNotNull(finalData);
        assertNotNull(finalData.getTeams());
    }

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

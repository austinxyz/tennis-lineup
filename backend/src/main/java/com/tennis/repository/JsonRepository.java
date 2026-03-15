package com.tennis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tennis.model.TeamData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Repository
@Slf4j
public class JsonRepository {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    private final Path dataFilePath;

    public JsonRepository(@Value("${storage.data-file:./data/tennis-data.json}") String dataFilePath) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.dataFilePath = Paths.get(dataFilePath);
        initializeDataFile();
    }

    private void initializeDataFile() {
        try {
            if (!Files.exists(dataFilePath)) {
                log.info("Data file not found, creating new file: {}", dataFilePath);
                TeamData emptyData = new TeamData();
                writeData(emptyData);
            }
        } catch (Exception e) {
            log.error("Failed to initialize data file", e);
            throw new RuntimeException("Failed to initialize data file", e);
        }
    }

    public TeamData readData() {
        lock.readLock().lock();
        try {
            log.debug("Reading data from file: {}", dataFilePath);
            if (!Files.exists(dataFilePath)) {
                log.warn("Data file not found, returning empty data");
                return new TeamData();
            }
            byte[] jsonData = Files.readAllBytes(dataFilePath);
            return objectMapper.readValue(jsonData, TeamData.class);
        } catch (IOException e) {
            log.error("Failed to read data file", e);
            throw new RuntimeException("Failed to read data file", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void writeData(TeamData data) {
        lock.writeLock().lock();
        try {
            log.debug("Writing data to file: {}", dataFilePath);
            Path tempFilePath = Paths.get(dataFilePath.toString() + ".tmp");

            // Write to temporary file first
            objectMapper.writeValue(tempFilePath.toFile(), data);

            // Atomically rename temp file to target file
            Files.move(tempFilePath, dataFilePath, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                     java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            log.error("Failed to write data file", e);
            throw new RuntimeException("Failed to write data file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
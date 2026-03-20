package com.tennis.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tennis.model.ConfigData;
import com.tennis.model.TeamData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

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
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();
    private final ObjectMapper objectMapper;
    private final Path dataFilePath;
    private final Path configFilePath;

    public JsonRepository(
            @Value("${storage.data-file:./data/tennis-data.json}") String dataFilePath,
            @Value("${storage.config-file:./data/tennis-config.json}") String configFilePath) {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.dataFilePath = Paths.get(dataFilePath);
        this.configFilePath = Paths.get(configFilePath);
        initializeDataFile();
        initializeConfigFile();
    }

    private void initializeDataFile() {
        try {
            if (!Files.exists(dataFilePath)) {
                log.info("Data file not found, creating new file: {}", dataFilePath);
                writeData(new TeamData());
            }
        } catch (Exception e) {
            log.error("Failed to initialize data file", e);
            throw new RuntimeException("Failed to initialize data file", e);
        }
    }

    private void initializeConfigFile() {
        try {
            if (!Files.exists(configFilePath)) {
                log.info("Config file not found, creating new file: {}", configFilePath);
                writeConfig(new ConfigData());
            }
        } catch (Exception e) {
            log.error("Failed to initialize config file", e);
            throw new RuntimeException("Failed to initialize config file", e);
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
            ensureParentDir(dataFilePath);
            Path tempFilePath = Paths.get(dataFilePath.toString() + ".tmp");
            objectMapper.writeValue(tempFilePath.toFile(), data);
            Files.move(tempFilePath, dataFilePath, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write data file", e);
            throw new RuntimeException("Failed to write data file", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ConfigData readConfig() {
        configLock.readLock().lock();
        try {
            log.debug("Reading config from file: {}", configFilePath);
            if (!Files.exists(configFilePath)) {
                log.warn("Config file not found, returning empty config");
                return new ConfigData();
            }
            byte[] jsonData = Files.readAllBytes(configFilePath);
            return objectMapper.readValue(jsonData, ConfigData.class);
        } catch (IOException e) {
            log.error("Failed to read config file", e);
            throw new RuntimeException("Failed to read config file", e);
        } finally {
            configLock.readLock().unlock();
        }
    }

    public void writeConfig(ConfigData config) {
        configLock.writeLock().lock();
        try {
            log.debug("Writing config to file: {}", configFilePath);
            ensureParentDir(configFilePath);
            Path tempFilePath = Paths.get(configFilePath.toString() + ".tmp");
            objectMapper.writeValue(tempFilePath.toFile(), config);
            Files.move(tempFilePath, configFilePath, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to write config file", e);
            throw new RuntimeException("Failed to write config file", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    private void ensureParentDir(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }
}

/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is provided "AS IS", without warranties
 * or conditions of any kind, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.atp.svp.repo.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.qubership.atp.svp.core.exceptions.CommonParametersStorageException;
import org.qubership.atp.svp.core.exceptions.FolderStorageException;
import org.qubership.atp.svp.core.exceptions.KeyParametersStorageException;
import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.core.exceptions.common.parameters.CommonParametersSaveException;
import org.qubership.atp.svp.core.exceptions.ei.SvpImportFileException;
import org.qubership.atp.svp.core.exceptions.folder.FolderDeleteException;
import org.qubership.atp.svp.core.exceptions.folder.FolderEditException;
import org.qubership.atp.svp.core.exceptions.folder.FolderExistException;
import org.qubership.atp.svp.core.exceptions.folder.FolderNotExistException;
import org.qubership.atp.svp.core.exceptions.folder.FolderSaveException;
import org.qubership.atp.svp.core.exceptions.key.parameters.KeyParameterSaveException;
import org.qubership.atp.svp.core.exceptions.page.PageCopyException;
import org.qubership.atp.svp.core.exceptions.page.PageDeleteException;
import org.qubership.atp.svp.core.exceptions.page.PageMoveException;
import org.qubership.atp.svp.core.exceptions.page.PageParseException;
import org.qubership.atp.svp.core.exceptions.page.PageSaveException;
import org.qubership.atp.svp.model.configuration.FolderConfiguration;
import org.qubership.atp.svp.model.configuration.KeyParameterConfiguration;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.impl.SutParameter;
import org.qubership.atp.svp.model.project.ProjectConfiguration;
import org.qubership.atp.svp.repo.PageConfigurationRepository;
import org.qubership.atp.svp.utils.Utils;
import org.springframework.stereotype.Repository;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class FilePageConfigurationRepository implements PageConfigurationRepository {

    public static final String PATH_TO_PAGES = "pages";
    public static final String FOLDER_DEFAULT = "Default";
    private static final String PATH_TO_KEY_PARAMETERS = "key_parameters.json";
    private static final String PATH_TO_COMMON_PARAMETERS = "common_parameters.json";
    private static final String PATH_TO_FOLDER = "folder_config.json";

    @Override
    public List<PageConfiguration> getPageConfigurations(ProjectConfiguration config, String folder)
            throws PageStorageException {
        try {
            String configPath = config.getConfigPath(folder);
            Path pageFolderPath = getPathToFile(configPath, PATH_TO_PAGES);
            if (!Files.exists(pageFolderPath)) {
                Files.createDirectories(pageFolderPath);
            }
            try (Stream<Path> streamPaths = Files.walk(pageFolderPath)) {
                List<Path> files = streamPaths
                        .filter(Files::isRegularFile)
                        .collect(Collectors.toList());
                List<PageConfiguration> pages = new ArrayList<>();
                for (Path file : files) {
                    try {
                        PageConfiguration pageConfiguration = readFileToPageConfiguration(file);
                        pages.add(pageConfiguration);
                    } catch (Exception ignored) {
                        log.error("File has some problem. Skip the page");
                    }
                }
                return pages;
            }
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - getPageConfigurations - IO Error!", e);
            throw new PageStorageException("Couldn't read configuration files!", e);
        }
    }

    @Override
    public void createOrUpdatePageConfiguration(ProjectConfigsEntity repositoryConfig,
                                                PageConfiguration pageConfiguration, String folder) {
        String configPath = repositoryConfig.getConfigPath(folder);
        try {
            saveFile(configPath, pageConfiguration);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - createOrUpdatePageConfiguration(). page: {}, folder: {}, "
                    + "config {} - IO Error!", pageConfiguration, folder, repositoryConfig, ex);
            throw new PageSaveException();
        }
    }

    private void saveFile(String configPath, PageConfiguration pageConfiguration) throws IOException {
        Path filePath = Paths.get(configPath).resolve(PATH_TO_PAGES).resolve(pageConfiguration.getName());
        File file = filePath.toFile();
        Files.createDirectories(filePath.getParent());
        Utils.mapper.writeValue(file, pageConfiguration);
    }

    @Override
    public void bulkCreateOrUpdatePageConfiguration(ProjectConfigsEntity repositoryConfig,
                                                    List<PageConfiguration> pageConfigurationList, String folder) {
        String configPath = repositoryConfig.getConfigPath(folder);
        try {
            saveFiles(configPath, pageConfigurationList);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - createOrUpdatePageConfiguration(). page: {}, folder: {}, "
                    + "config {} - IO Error!", pageConfigurationList, folder, repositoryConfig, ex);
            throw new PageSaveException();
        }
    }

    private void saveFiles(String configPath, List<PageConfiguration> pageConfigurationList) throws IOException {
        for (PageConfiguration pageConfiguration : pageConfigurationList) {
            Path path = Paths.get(configPath).resolve(PATH_TO_PAGES).resolve(pageConfiguration.getName());
            File file = path.toFile();
            Files.createDirectories(path.getParent());
            Utils.mapper.writeValue(file, pageConfiguration);
        }
    }

    @Override
    public void deletePageConfigurations(ProjectConfigsEntity config, String name, String folder) {
        String configPath = config.getConfigPath(folder);
        try {
            Files.deleteIfExists(getPathToFile(configPath, PATH_TO_PAGES, name));
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - deletePageConfigurations(), page: {}, folder: {} and config:"
                    + " {} - IO Exception!", name, folder, config, ex);
            throw new PageDeleteException();
        }
    }

    @Override
    public List<KeyParameterConfiguration> getKeyParameters(ProjectConfiguration config, String folder) {
        String configPath = config.getConfigPath(folder);
        Path configFile = getPathToFile(configPath, PATH_TO_KEY_PARAMETERS);
        if (Files.notExists(configFile)) {
            log.error("FilePageConfigurationRepository - getKeyParameters - File not found by path: {}!", configFile);
            return new ArrayList<>();
        }
        try {
            try {
                String fileString = readFile(configFile);
                return Utils.mapper.readValue(fileString, new TypeReference<List<KeyParameterConfiguration>>() {
                });
            } catch (JsonParseException | JsonMappingException ex) {
                String fileString = readFile(configFile);
                List<String> keyParameters = Utils.mapper.readValue(fileString, new TypeReference<List<String>>() {
                });
                return keyParameters.stream().map(KeyParameterConfiguration::new).collect(Collectors.toList());
            }
        } catch (JsonParseException | JsonMappingException ex) {
            log.error("FilePageConfigurationRepository - getKeyParameters - JSON Parsing Error!", ex);
            throw new KeyParametersStorageException("Couldn't parse key parameters!", ex);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - getKeyParameters - IO Error!", ex);
            throw new KeyParametersStorageException("Couldn't get key parameters!", ex);
        }
    }

    @Override
    public void updateKeyParameters(ProjectConfigsEntity config, List<KeyParameterConfiguration> keyParameters,
                                    String folder) {
        String configPath = config.getConfigPath(folder);
        try {
            Path path = Paths.get(configPath + "/" + PATH_TO_KEY_PARAMETERS);
            Files.createDirectories(path.getParent());
            Utils.mapper.writeValue(path.toFile(), keyParameters);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - updateKeyParameters for project {} in folder {} - IO Error!",
                    config, folder, ex);

            throw new KeyParameterSaveException();
        }
    }

    @Override
    public void updateCommonParameters(ProjectConfigsEntity config, List<SutParameter> commonParameters,
                                       String folder) {
        String configPath = config.getConfigPath(folder);
        try {
            Path path = Paths.get(configPath + "/" + PATH_TO_COMMON_PARAMETERS);
            Files.createDirectories(path.getParent());
            Utils.mapper.writeValue(path.toFile(), commonParameters);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - updateCommonParameters() for project {} in folder {} - IO "
                    + "Error!", config, folder, ex);
            throw new CommonParametersSaveException();
        }
    }

    @Override
    public List<SutParameter> getCommonParameters(ProjectConfiguration config, String folder) {
        String configPath = config.getConfigPath(folder);
        Path configFile = getPathToFile(configPath, PATH_TO_COMMON_PARAMETERS);
        if (Files.notExists(configFile)) {
            log.error("FilePageConfigurationRepository - getCommonParameters - File not found by path: {}!",
                    configFile);
            return new ArrayList<>();
        }
        try {
            String fileString = readFile(configFile);
            return Arrays.asList(Utils.mapper.readValue(fileString, SutParameter[].class));
        } catch (JsonParseException | JsonMappingException ex) {
            log.error("FilePageConfigurationRepository - getCommonParameters - JSON Parsing Error!", ex);
            throw new CommonParametersStorageException("Couldn't parse common parameters!", ex);
        } catch (IOException ex) {
            log.error("FilePageConfigurationRepository - getCommonParameters - IO Error!", ex);
            throw new CommonParametersStorageException("Couldn't get key parameters!", ex);
        }
    }

    @Override
    public void movePage(ProjectConfigsEntity config, String name, String newName, String folder,
                         String targetFolder, int newOrder) {
        String configPath = config.getConfigPath(folder);
        Path from = getPathToFile(configPath, PATH_TO_PAGES, name);
        try {
            PageConfiguration pageConfiguration = readFileToPageConfiguration(from);
            pageConfiguration.setName(newName);
            pageConfiguration.setOrder(newOrder);
            String newPath = config.getConfigPath(targetFolder);
            saveFile(newPath, pageConfiguration);
            Files.delete(from);
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - movePage(). page name '{}', folder {}, target folder {} and "
                    + "config {} - IO Error!", name, folder, targetFolder, config, e);
            throw new PageMoveException();
        }
    }

    @Override
    public void copyPage(ProjectConfigsEntity config, String name, String newName,
                         String folder, String targetFolder, int newOrder) {
        String configPath = config.getConfigPath(folder);
        Path file = getPathToFile(configPath, PATH_TO_PAGES, name);
        try {
            PageConfiguration pageConfiguration = readFileToPageConfiguration(file);
            pageConfiguration.setName(newName);
            pageConfiguration.setOrder(newOrder);
            String newPath = config.getConfigPath(targetFolder);
            saveFile(newPath, pageConfiguration);
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - copyPage(). page name '{}', folder {}, target folder {} and "
                    + "config {} - IO Error!", name, folder, targetFolder, config, e);
            throw new PageCopyException();
        }
    }

    @Override
    public void createFolder(ProjectConfigsEntity config, FolderEntity folder) {
        String path = config.getConfigPath(folder.getName());
        Path folderPath = Paths.get(path);
        if (Files.exists(folderPath)) {
            throw new FolderExistException();
        }
        try {
            Files.createDirectories(folderPath);
            Path folderFilePath = Paths.get(path).resolve(PATH_TO_FOLDER);
            File file = folderFilePath.toFile();
            Utils.mapper.writeValue(file, new FolderConfiguration(folder));
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - createFolder() folder '{}' and config {} - IO Error!", folder,
                    config, e);
            throw new FolderSaveException();
        }
    }

    @Override
    public void editFolder(ProjectConfigsEntity config, String oldName, FolderEntity folder) {
        String folderPath = config.getConfigPath(oldName);
        Path path = Paths.get(folderPath);
        if (!Files.exists(path)) {
            throw new FolderNotExistException();
        }
        try {
            Files.move(path, path.resolveSibling(folder.getName()));
            String newPath = config.getConfigPath(folder.getName());
            Path pathToPage = Paths.get(newPath).resolve(PATH_TO_FOLDER);
            File file = pathToPage.toFile();
            Utils.mapper.writeValue(file, new FolderConfiguration(folder));
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - editFolder folder '{}' and config {} - IO Error!", folder,
                    config, e);
            throw new FolderEditException();
        }
    }

    /**
     * Getting folders by project path.
     *
     * @param projectConfig ProjectConfiguration
     * @return List String
     */
    @Override
    public List<String> getFolders(ProjectConfiguration projectConfig) {
        Path projectFolder = Paths.get(projectConfig.getRepoConfig().getPath());
        try (Stream<Path> paths = Files.walk(projectFolder, 1)) {
            return Stream.concat(paths.filter(path -> isCorrectFile(path, projectFolder))
                            .map(path -> path.getFileName().toString()), Stream.of(FOLDER_DEFAULT))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("The folders can't be load by path: {}", projectFolder, e);
            throw new FolderStorageException("The folders can't be load by path: " + projectFolder);
        }
    }

    private String readFile(Path configFile) throws IOException {
        return new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
    }

    /**
     * Getting FolderConfiguration by projectConfig.
     *
     * @param projectConfig ProjectConfiguration
     */
    public List<FolderConfiguration> getFolderConfigurations(ProjectConfiguration projectConfig) {
        Path defaultProjectPath = Paths.get(projectConfig.getConfigPath("Default"));
        if (Files.exists(defaultProjectPath.resolve(PATH_TO_FOLDER))) {
            return getFolders(projectConfig).stream().map(folder -> {
                Path folderPath = Paths.get(projectConfig.getConfigPath(folder)).resolve(PATH_TO_FOLDER);
                try {
                    String fileString = readFile(folderPath);
                    return Utils.mapper.readValue(fileString, FolderConfiguration.class);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }).collect(Collectors.toList());
        } else {
            return getFolders(projectConfig).stream().map(folder -> {
                FolderConfiguration folderConfiguration = new FolderConfiguration();
                folderConfiguration.setId(null);
                folderConfiguration.setName(folder);
                return folderConfiguration;
            }).collect(Collectors.toList());
        }
    }

    private boolean isCorrectFile(Path path, Path projectFolder) {
        return !path.getFileName().toString().equals(projectFolder.getFileName().toString())
                && !path.getFileName().toString().equals(PATH_TO_PAGES)
                && !path.getFileName().toString().startsWith(".")
                && Files.isDirectory(path);
    }

    @Override
    public void deleteFolder(ProjectConfigsEntity config, String folder) {
        String folderPath = config.getConfigPath(folder);
        Path path = Paths.get(folderPath);
        if (!Files.exists(path)) {
            throw new FolderNotExistException();
        }
        try {
            FileUtils.deleteDirectory(path.toFile());
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - deleteFolder a folder '{}' and config {}", folder, config, e);
            throw new FolderDeleteException();
        }
    }

    @Override
    public void importProject(ProjectConfigsEntity config, List<FolderEntity> folders) {
        Path projectPath = Paths.get(config.getPathFolderLocalProject());
        if (Files.exists(projectPath)) {
            return;
        }
        try {
            for (FolderEntity folder : folders) {
                updateFolderConfig(config, folder);
                saveConfigFiles(config, folder);
            }
        } catch (IOException e) {
            log.error("FilePageConfigurationRepository - import project. Error import files in the project: {}",
                    config.getProjectId(), e);
            throw new SvpImportFileException();
        }
    }

    private void updateFolderConfig(ProjectConfigsEntity config, FolderEntity folder) throws IOException {
        Path folderPath = Paths.get(config.getConfigPath(folder.getName()));
        String folderName = folder.getName();
        if (folderName.equals("Default")) {
            Path folderFilePath = folderPath.resolve(PATH_TO_FOLDER);
            File file = folderFilePath.toFile();
            Utils.mapper.writeValue(file, new FolderConfiguration(folder));
        } else {
            if (Files.exists(folderPath)) {
                FileSystemUtils.deleteRecursively(folderPath);
            } else {
                deleteOldFolder(config, folder);
            }
            createFolder(config, folder);
        }
    }

    private void deleteOldFolder(ProjectConfigsEntity config, FolderEntity folder) throws IOException {
        ProjectConfiguration projectConf = new ProjectConfiguration(config);
        Optional<FolderConfiguration> folderConfigurationOptional = getFolderConfigurations(projectConf).stream()
                .filter(folderConfiguration -> folderConfiguration.getId().equals(folder.getFolderId()))
                .findFirst();
        if (folderConfigurationOptional.isPresent()) {
            String path = config.getConfigPath(folderConfigurationOptional.get().getName());
            Path folderPath = Paths.get(path);
            File file = folderPath.toFile();
            FileUtils.deleteDirectory(file);
        }
    }

    @Override
    public void initializationProjectConfigs(ProjectConfigsEntity config) {
        try {
            if (!Strings.isNullOrEmpty(config.getPathFolderLocalProject())) {
                Path projectPath = Paths.get(config.getPathFolderLocalProject());
                if (!Files.exists(projectPath)) {
                    Files.createDirectories(projectPath);
                    for (FolderEntity folder : config.getFolders()) {
                        String folderName = folder.getName();
                        if (folderName.equals("Default")) {
                            Path folderPath = Paths.get(config.getPathFolderLocalProject());
                            File file = new File(folderPath + "/" + PATH_TO_FOLDER);
                            Utils.mapper.writeValue(file, new FolderConfiguration(folder));
                        } else {
                            createFolder(config, folder);
                        }
                        saveConfigFiles(config, folder);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveConfigFiles(ProjectConfigsEntity config, FolderEntity folder) {
        String folderName = folder.getName();

        List<KeyParameterConfiguration> keyParameters = folder.getKeyParameterEntities().stream()
                .map(KeyParameterConfiguration::new).collect(Collectors.toList());
        updateKeyParameters(config, keyParameters, folderName);

        List<SutParameter> commonParameters = folder.getCommonParameters().stream()
                .map(SutParameter::new).collect(Collectors.toList());
        updateCommonParameters(config, commonParameters, folderName);

        folder.getPages().forEach(page -> {
            createOrUpdatePageConfiguration(config, new PageConfiguration(page), folderName);
        });
    }

    private Path getPathToFile(String config, String pathTo, String file) {
        return Paths.get(config, pathTo, file);
    }

    private Path getPathToFile(String config, String file) {
        return Paths.get(config, file);
    }

    private PageConfiguration readFileToPageConfiguration(Path file) throws IOException {
        try {
            String fileString = readFile(file);
            PageConfiguration pageConfiguration = Utils.mapper.readValue(fileString, PageConfiguration.class);
            pageConfiguration.setName(file.getFileName().toString());
            return pageConfiguration;
        } catch (JsonParseException | JsonMappingException ex) {
            log.error("FilePageConfigurationRepository - readFileToPageConfiguration(). File path {} "
                    + "- JSON Parsing Error!", file, ex);
            throw new PageParseException();
        }
    }
}

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

package org.qubership.atp.svp.ei;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import org.qubership.atp.ei.node.dto.ExportFormat;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.dto.ExportScope;
import org.qubership.atp.ei.node.dto.ValidationResult;
import org.qubership.atp.ei.node.services.FileService;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.ei.node.services.ObjectSaverToDiskService;
import org.qubership.atp.svp.core.enums.RepositoryType;
import org.qubership.atp.svp.core.enums.ServiceScopeEntities;
import org.qubership.atp.svp.core.exceptions.AtpSvpException;
import org.qubership.atp.svp.ei.component.ExportImportStrategiesRegistry;
import org.qubership.atp.svp.ei.executor.AtpSvpExportExecutor;
import org.qubership.atp.svp.ei.executor.AtpSvpImportExecutor;
import org.qubership.atp.svp.ei.service.AtpExportStrategy;
import org.qubership.atp.svp.ei.service.AtpImportStrategy;
import org.qubership.atp.svp.model.db.CommonParameterEntity;
import org.qubership.atp.svp.model.db.FolderEntity;
import org.qubership.atp.svp.model.db.KeyParameterEntity;
import org.qubership.atp.svp.model.db.PageConfigurationEntity;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.db.SutParameterEntity;
import org.qubership.atp.svp.model.ei.ExportImportCommonParameter;
import org.qubership.atp.svp.model.ei.ExportImportFolder;
import org.qubership.atp.svp.model.ei.ExportImportKeyParameter;
import org.qubership.atp.svp.model.ei.ExportImportPage;
import org.qubership.atp.svp.repo.impl.FilePageConfigurationRepository;
import org.qubership.atp.svp.service.jpa.FolderServiceJpa;
import org.qubership.atp.svp.service.jpa.PageConfigurationServiceJpa;
import org.qubership.atp.svp.service.jpa.ProjectConfigurationServiceJpa;
import org.qubership.atp.svp.tests.DbMockEntity;
import org.qubership.atp.svp.tests.TestWithTestData;
import org.qubership.atp.svp.utils.Utils;
import io.undertow.util.FileUtils;

public class ExportImportTest extends TestWithTestData {

    private static final String FOLDER_PATH = "FOLDER\\ExportImportFolder";
    private static final String PAGE_PATH = "PAGE\\ExportImportPage";
    private static final String COMMON_PATH = "COMMON_PARAMETER\\ExportImportCommonParameter";
    private static final String KEY_PATH = "KEY_PARAMETER\\ExportImportKeyParameter";

    private AtpSvpExportExecutor atpSvpExportExecutor;
    private AtpSvpImportExecutor atpSvpImportExecutor;
    @Mock
    ProjectConfigurationServiceJpa projectConfigurationServiceJpa;
    @Mock
    FilePageConfigurationRepository filePageConfigurationRepository;
    @Mock
    FolderServiceJpa folderServiceJpa;
    @Mock
    PageConfigurationServiceJpa pageConfigurationServiceJpa;
    private AtpImportStrategy atpImportStrategy;
    private UUID projectId;
    private ExportScope exportScope;
    private ExportImportData exportImportData;
    private ExportImportData importDataAnotherProject;
    private Path path;
    private List<FolderEntity> folderEntities;

    @AfterEach
    public void afterEach() throws IOException {
        FileUtils.deleteRecursive(path);
    }

    @BeforeEach
    public void beforeEach() throws IOException {
        folderEntities = new ArrayList<>();
        projectConfigurationServiceJpa = mock(ProjectConfigurationServiceJpa.class);
        filePageConfigurationRepository = mock(FilePageConfigurationRepository.class);
        folderServiceJpa = mock(FolderServiceJpa.class);
        pageConfigurationServiceJpa = mock(PageConfigurationServiceJpa.class);
        ExportImportStrategiesRegistry exportImportStrategiesRegistry = new ExportImportStrategiesRegistry(
                Arrays.asList(new AtpExportStrategy(folderServiceJpa, pageConfigurationServiceJpa,
                        new ObjectSaverToDiskService(new FileService(), true))),
                Arrays.asList(new AtpImportStrategy(projectConfigurationServiceJpa,
                        folderServiceJpa, new ObjectLoaderFromDiskService()))
        );
        atpSvpExportExecutor = new AtpSvpExportExecutor(exportImportStrategiesRegistry);
        atpImportStrategy =  new AtpImportStrategy(projectConfigurationServiceJpa,
                folderServiceJpa, new ObjectLoaderFromDiskService());
        atpImportStrategy.setFileRepo(filePageConfigurationRepository);
        atpSvpImportExecutor = new AtpSvpImportExecutor(atpImportStrategy);
        projectId = new UUID(0, 1);
        exportScope = new ExportScope();
        path = Paths.get("exportImportTest");
        initData();
        exportImportData = new ExportImportData(projectId, exportScope, ExportFormat.ATP);
        importDataAnotherProject = new ExportImportData(projectId, exportScope, ExportFormat.ATP, false,
                true, projectId, new HashMap<>(), null, null, false);

    }

    @Test
    public void exportImport_atp_sameProject() throws Exception {
        String expectedExportFoldersString = readFileToString("src/test/resources/test_data/ei"
                + "/expectedExportImportFolders.json");
        String expectedExportPagesString = readFileToString("src/test/resources/test_data/ei"
                + "/expectedExportImportPages.json");
        String expectedExportCommonsString = readFileToString("src/test/resources/test_data/ei"
                + "/expectedExportImportCommonParameters.json");
        String expectedExportKeysString = readFileToString("src/test/resources/test_data/ei"
                + "/expectedExportImportKeyParameters.json");

        List<ExportImportFolder> expectedExportFolders =
                Arrays.asList(Utils.mapper.readValue(expectedExportFoldersString, ExportImportFolder[].class));
        List<ExportImportPage> expectedExportPages =
                Arrays.asList(Utils.mapper.readValue(expectedExportPagesString, ExportImportPage[].class));
        List<ExportImportCommonParameter> expectedExportCommons =
                Arrays.asList(Utils.mapper.readValue(expectedExportCommonsString, ExportImportCommonParameter[].class));
        List<ExportImportKeyParameter> expectedExportKeys =
                Arrays.asList(Utils.mapper.readValue(expectedExportKeysString, ExportImportKeyParameter[].class));

        atpSvpExportExecutor.exportToFolder(exportImportData, path);

        List<ExportImportFolder> actualExportFolders = getActualObject(path, ExportImportFolder.class, FOLDER_PATH);
        List<ExportImportPage> actualExportPages = getActualObject(path, ExportImportPage.class, PAGE_PATH);
        List<ExportImportCommonParameter> actualExportCommons = getActualObject(path,
                ExportImportCommonParameter.class, COMMON_PATH);
        List<ExportImportKeyParameter> actualExportKeys = getActualObject(path, ExportImportKeyParameter.class,
                KEY_PATH);

        assertEquals(expectedExportFolders, actualExportFolders);
        assertEquals(expectedExportPages, actualExportPages);
        assertEquals(expectedExportCommons, actualExportCommons);
        assertEquals(expectedExportKeys, actualExportKeys);

        atpSvpImportExecutor.importData(exportImportData, path);
        ArgumentCaptor<List<FolderEntity>> saveAllArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(folderServiceJpa, times(1)).saveAll(saveAllArgumentCaptor.capture());
        List<FolderEntity> actualFolderEntities = saveAllArgumentCaptor.getValue();
        assertEquals(folderEntities, actualFolderEntities);
    }


    @Test
    public void import_allCasesForImport_successfulImport() throws Exception {
        String expectedFolders = readFileToString("src/test/resources/test_data/ei/expectedFolderEntities.json");
        List<FolderEntity> expectedFolderEntities = Arrays.asList(Utils.mapper.readValue(expectedFolders, FolderEntity[].class));
        Path exportPath = Paths.get("src/test/resources/test_data/ei/export/exportImportTest");

        atpSvpImportExecutor.importData(exportImportData, exportPath);

        ArgumentCaptor<List<FolderEntity>> saveAllArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(folderServiceJpa, times(1)).saveAll(saveAllArgumentCaptor.capture());
        List<FolderEntity> actualFolderEntities = saveAllArgumentCaptor.getValue();
        assertEquals(expectedFolderEntities,actualFolderEntities);
    }

    @Test
    public void exportValidate_atp_sameProject() throws Exception {
        atpSvpExportExecutor.exportToFolder(exportImportData, path);
        Path replacePagePath = Paths.get("exportImportTest/PAGE/ExportImportPage");
        Path replaceCommonPath = Paths.get("exportImportTest/COMMON_PARAMETER/ExportImportCommonParameter");
        Path pathMock = Paths.get("src/test/resources/test_data/ei/validation");
        Files.copy(pathMock.resolve("00000000-0000-0025-0000-00000000000b.json"),
                replacePagePath.resolve("00000000-0000-0025-0000-00000000000b.json"), REPLACE_EXISTING);
        Files.copy(pathMock.resolve("00000000-0000-0023-0000-00000000000b.json"),
                replaceCommonPath.resolve("00000000-0000-0023-0000-00000000000b.json"), REPLACE_EXISTING);

        ValidationResult result = atpSvpImportExecutor.validateData(exportImportData, path);

        String expectedMessage1 = "the CommonParameter: 'SUT parameter 1 (folder 1)' already exists, name wilt be with '_copy'";
        String expectedMessage2 = "the Page: 'Page 1 (folder 1) ConfigPageHideGroup' already exists, name wilt be with '_copy'";

        Assert.assertEquals(2, result.getDetails().size());
        Assert.assertEquals(expectedMessage1, result.getDetails().get(0).getMessage());
        Assert.assertEquals(expectedMessage2, result.getDetails().get(1).getMessage());
    }

    @Test
    public void exportValidate_atp_otherProject() throws Exception {
        atpSvpExportExecutor.exportToFolder(exportImportData, path);

        ValidationResult result = atpSvpImportExecutor.validateData(importDataAnotherProject, path);
        Assert.assertEquals(0, result.getDetails().size());
    }



    @Test
    public void export_notAtp() {
        AtpSvpException thrown = assertThrows(
                AtpSvpException.class,
                () -> {
                    exportImportData = new ExportImportData(projectId, exportScope, ExportFormat.NTT);
                    atpSvpExportExecutor.exportToFolder(exportImportData, path);
                },
                "Expected exportToFolder to throw, but it didn't"
        );
        assertEquals("SVP does not support export type '" + ExportFormat.NTT + "'", thrown.getMessage());
    }

    private void initData() throws IOException {
        List<PageConfigurationEntity> pageFolder1Entities = new ArrayList<>();
        List<PageConfigurationEntity> pageFolder2Entities = new ArrayList<>();
        List<CommonParameterEntity> commonParameterFolder1Entities = new ArrayList<>();
        List<CommonParameterEntity> commonParameterFolder2Entities = new ArrayList<>();
        List<KeyParameterEntity> keyParameterFolder1Entities = new ArrayList<>();
        List<KeyParameterEntity> keyParameterFolder2Entities = new ArrayList<>();
        //PROJECT
        ProjectConfigsEntity projectConfigsEntity = new ProjectConfigsEntity(
                projectId, "ProjectName", 10000, false, RepositoryType.LOCAL, null, "src/test/config/project/test", null, null, folderEntities
        );
        //FOLDERS
        folderEntities.add(new FolderEntity()
                .setFolderId(new UUID(1, 1))
                .setName("Folder1")
                .setProject(projectConfigsEntity)
                .setPages(pageFolder1Entities)
                .setCommonParameters(commonParameterFolder1Entities)
                .setKeyParameterEntities(keyParameterFolder1Entities)
        );
        folderEntities.add(new FolderEntity()
                .setFolderId(new UUID(1, 2))
                .setName("Default")
                .setProject(projectConfigsEntity)
                .setPages(pageFolder2Entities)
                .setCommonParameters(commonParameterFolder2Entities)
                .setKeyParameterEntities(keyParameterFolder2Entities)
        );
        //COMMON PARAMS FOLDER 1
        commonParameterFolder1Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 11))
                .setFolder(folderEntities.get(0))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 11))
                                .setName("SUT parameter 1 (folder 1)")
                        //todo other sets
                )
        );
        commonParameterFolder1Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 12))
                .setFolder(folderEntities.get(0))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 12))
                                .setName("SUT parameter 2 (folder 1)")
                        //todo other sets
                )
        );

        commonParameterFolder1Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 14))
                .setFolder(folderEntities.get(0))
                .setSourceId(new UUID(3, 13))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 12))
                                .setName("SUT parameter 3 (folder 1) sourceId existed")
                        //todo other sets
                )
        );

        commonParameterFolder1Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 16))
                .setFolder(folderEntities.get(0))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 16))
                                .setName("SUT parameter 4 (folder 1) existed Name")
                        //todo other sets
                )
        );

        //COMMON PARAMS FOLDER 2
        commonParameterFolder2Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 21))
                .setFolder(folderEntities.get(1))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 21))
                                .setName("SUT parameter 1 (folder 2)")
                        //todo other sets
                )
        );
        commonParameterFolder2Entities.add(new CommonParameterEntity()
                .setCommonParameterId(new UUID(3, 22))
                .setFolder(folderEntities.get(1))
                .setSutParameterEntity(new SutParameterEntity()
                                .setParameterId(new UUID(3, 22))
                                .setName("SUT parameter 2 (folder 2)")
                        //todo other sets
                )
        );
        //KEY PARAMS FOLDER 1
        keyParameterFolder1Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 11))
                .setName("KEY parameter 1 (folder 1)")
                .setFolder(folderEntities.get(0))
                .setKeyOrder(0)
        );
        keyParameterFolder1Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 12))
                .setName("KEY parameter 2 (folder 1)")
                .setFolder(folderEntities.get(0))
                .setKeyOrder(1)
        );
        keyParameterFolder1Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 13))
                .setName("KEY parameter 3 (folder 1) sourceId old")
                .setFolder(folderEntities.get(0))
                .setKeyOrder(2)
                .setSourceId(new UUID(4, 14))
        );
        keyParameterFolder1Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 16))
                .setName("KEY parameter 4 (folder 1) existed name")
                .setFolder(folderEntities.get(0))
                .setKeyOrder(3)
        );

        //KEY PARAMS FOLDER 2
        keyParameterFolder2Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 21))
                .setName("KEY parameter 1 (folder 2)")
                .setFolder(folderEntities.get(1))
                .setKeyOrder(0)
        );
        keyParameterFolder2Entities.add(new KeyParameterEntity()
                .setKeyParameterId(new UUID(4, 22))
                .setName("KEY parameter 2 (folder 2)")
                .setFolder(folderEntities.get(1))
                .setKeyOrder(1)
        );
        //PAGES FOLDER 1
        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageHideGroup")
                .setPageId(new UUID(5, 11))
                .setName("Page 1 (folder 1) ConfigPageHideGroup")
                .setFolder(folderEntities.get(0))
        );
        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageJsonTableLinks")
                .setPageId(new UUID(5, 12))
                .setName("Page 2 (folder 1) ConfigPageJsonTableLinks")
                .setFolder(folderEntities.get(0))
        );
        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageTableLinks")
                .setPageId(new UUID(5, 13))
                .setName("Page 3 (folder 1) ConfigPageTableLinks")
                .setFolder(folderEntities.get(0))
        );

        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageTableLinks")
                .setPageId(new UUID(5, 26))
                .setName("Page 4 (folder 1) existed SourceId")
                .setFolder(folderEntities.get(0))
                .setSourceId(new UUID(5, 25))
        );

        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageTableLinks")
                .setPageId(new UUID(5, 28))
                .setName("Page 5 (folder 1) existed name")
                .setFolder(folderEntities.get(0))
        );

//        pageFolder1Entities.add(DbMockEntity.getPageFromTestFile("ConfigPageTableLinks")
//                .setPageId(new UUID(5, 28))
//                .setName("Page 6 (folder 1) New")
//                .setFolder(folderEntities.get(0))
//        );


        //PAGES FOLDER 2
        pageFolder2Entities.add(DbMockEntity.getPageFromTestFile("ConfigWithoutPreconfiguredParams")
                .setPageId(new UUID(5, 21))
                .setName("Page 1 (folder 2) ConfigWithoutPreconfiguredParams")
                .setFolder(folderEntities.get(1))
        );
        pageFolder2Entities.add(DbMockEntity.getPageFromTestFile("ConfigWithPreconfiguredParams")
                .setPageId(new UUID(5, 22))
                .setName("Page 2 (folder 2) ConfigWithPreconfiguredParams")
                .setFolder(folderEntities.get(1))
        );
        pageFolder2Entities.add(DbMockEntity.getPageFromTestFile("ConfigWithSynchronousPreconfiguredParams")
                .setPageId(new UUID(5, 23))
                .setName("Page 3 (folder 2) ConfigWithSynchronousPreconfiguredParams")
                .setFolder(folderEntities.get(1))
        );
        pageFolder2Entities.add(DbMockEntity.getPageFromTestFile("ParamAndLinkTypedParametersConfig")
                .setPageId(new UUID(5, 24))
                .setName("Page 4 (folder 2) ParamAndLinkTypedParametersConfig")
                .setFolder(folderEntities.get(1))
        );
        List<PageConfigurationEntity> listAllPages = new ArrayList<>();
        listAllPages.addAll(pageFolder1Entities);
        listAllPages.addAll(pageFolder2Entities);
        Map<String, Set<String>> importExportEntities = new HashMap<>();
        Set<UUID> setFolders = folderEntities.stream().map(f -> f.getFolderId()).collect(Collectors.toSet());
        importExportEntities.put(ServiceScopeEntities.ENTITY_SVP_FOLDERS.getValue(),
                setFolders.stream().map(UUID::toString).collect(Collectors.toSet()));
        importExportEntities.put(ServiceScopeEntities.ENTITY_SVP_PAGES.getValue(),
                listAllPages.stream().map(p -> p.getPageId().toString()).collect(Collectors.toSet()));
        exportScope.setEntities(importExportEntities);
        when(folderServiceJpa.getFoldersByIds(setFolders)).thenReturn(folderEntities);
        when(pageConfigurationServiceJpa.getAllPagesByIds(
                listAllPages.stream().map(p -> p.getPageId()).collect(Collectors.toSet())))
                .thenReturn(listAllPages);
        when(projectConfigurationServiceJpa.findProjectConfigById(projectId)).thenReturn(projectConfigsEntity);
    }

    private <T> List<T> getActualObject(Path workDir, Class<T> tClass, String folderPath) {
        Path filesPath = Paths.get(workDir.toString(), folderPath);
        File folderFile = filesPath.toFile();
        File[] files = folderFile.listFiles();

        return Arrays.stream(files)
                .map(file -> convertFileToObject(file.toPath(), tClass))
                .collect(Collectors.toList());
    }

    private <T> T convertFileToObject(Path path, Class<T> to) {
        try {
            return Utils.mapper.readValue(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), to);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

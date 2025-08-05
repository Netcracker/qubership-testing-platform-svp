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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import org.qubership.atp.svp.core.exceptions.FolderStorageException;
import org.qubership.atp.svp.core.exceptions.PageStorageException;
import org.qubership.atp.svp.core.exceptions.page.PageCopyException;
import org.qubership.atp.svp.model.db.ProjectConfigsEntity;
import org.qubership.atp.svp.model.impl.PageConfiguration;
import org.qubership.atp.svp.model.project.ProjectConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = FilePageConfigurationRepository.class)
@TestPropertySource(locations = "classpath:application-IntegrationTest.properties")
public class FilePageConfigurationRepositoryTest {

    @Autowired
    private FilePageConfigurationRepository fileRepo;

    @Test
    public void getPageConfigurations_correctPath_returnsTwoConfigs() throws PageStorageException {
        ProjectConfiguration config = getDefaultConfig();
        int expectedNumberOfPages = 4;

        List<PageConfiguration> configs = fileRepo.getPageConfigurations(config, "Default");

        assertEquals(expectedNumberOfPages, configs.size());
    }

    @Test()
    public void getPageConfigurations_brokenPath_throwsException() throws PageStorageException {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/nonExistentDir");

        List<PageConfiguration> configs = fileRepo.getPageConfigurations(config, "Default");
        assertEquals(0, configs.size());
    }

    @Test
    public void deletePageConfigurations_correctPathFileDoesntExist_successfulIfNotExist() throws PageStorageException {
        ProjectConfigsEntity config = getDefaultConfigEntity();

        fileRepo.deletePageConfigurations(config, "Dell Test Configuration", null);

        File erFile = new File(config.getPathFolderLocalProject() + "/" + "Dell Test Configuration");
        Assert.assertFalse(erFile.exists());
    }

    @Test()
    public void deletePageConfigurations_correctPathFileExistInFolder_successfulDeleting()
            throws PageStorageException, FolderStorageException, IOException {
        ProjectConfigsEntity config = getDefaultConfigEntity();
        File erFile = new File(config.getPathFolderLocalProject() + "/newFolder/pages/" + "Deleting_page");
        Files.createDirectories(new File(config.getPathFolderLocalProject() + "/newFolder/pages/").toPath());
        erFile.createNewFile();

        fileRepo.deletePageConfigurations(config, "Deleting_page", "newFolder");

        Assert.assertFalse(erFile.exists());
        FileUtils.deleteDirectory(new File(config.getPathFolderLocalProject() + "/newFolder"));
    }

    @Test()
    public void deletePageConfigurations_correctPathAndNonExistentFile_NotThrowsPageStorageException()
            throws PageStorageException {
        ProjectConfigsEntity config = getDefaultConfigEntity();

        fileRepo.deletePageConfigurations(config, "DellTestConfiguration", "Default");
    }
//
//    @Test
//    public void createOrUpdatePageConfiguration_correctPath_newFileExist()
//            throws PageStorageException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        PageConfiguration erConfiguration = new PageConfiguration(UUID.randomUUID(),"Test Configuration",
//                Collections.emptyList(), false);
//
//        fileRepo.createOrUpdatePageConfiguration(config, erConfiguration, "Default");
//        File erFile = new File(config.getPathFolderLocalProject() + "/pages/" + "Test Configuration");
//
//        Assert.assertTrue(erFile.exists());
//        erFile.deleteOnExit();
//    }
//
//    @Test
//    public void createOrUpdatePageConfiguration_correctPathToFolder_newFileExist()
//            throws PageStorageException, FolderStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        PageConfiguration erConfiguration = new PageConfiguration(UUID.randomUUID(),"Test Configuration",
//                Collections.emptyList(), false);
//        fileRepo.createOrUpdatePageConfiguration(config, erConfiguration, "newFolder");
//        File erFile = new File(config.getPathFolderLocalProject() + "/newFolder/pages/" + "Test Configuration");
//
//        Assert.assertTrue(erFile.exists());
//        erFile.deleteOnExit();
//
//        FileUtils.deleteDirectory(new File(config.getPathFolderLocalProject() + "/newFolder"));
//    }
//
//    @Test()
//    public void createOrUpdatePageConfiguration_correctPathAndNonExistentFile_createDirectories()
//            throws PageStorageException {
//        ProjectConfigsEntity config = getNotExistConfigEntity();
//        PageConfiguration erConfiguration = new PageConfiguration(UUID.randomUUID(),"Test Configuration",
//                Collections.emptyList(), false);
//
//        fileRepo.createOrUpdatePageConfiguration(config, erConfiguration, "Default");
//
//        File erFile = new File(config.getPathFolderLocalProject() + "/pages/" + "Test Configuration");
//        Assert.assertTrue(erFile.exists());
//        erFile.deleteOnExit();
//    }
//
//    @Test()
//    public void getKeyParameters_brokenPath_returnEmptyList() {
//        ProjectConfiguration config = getDefaultConfig();
//
//        List<String> actualKeyParam = fileRepo.getKeyParameters(config, "testFolder");
//
//        Assert.assertTrue(actualKeyParam.isEmpty());
//    }
//
//    @Test(expected = KeyParametersStorageException.class)
//    public void getKeyParameters_JsonParseBad_throwsJsonParseException() throws KeyParametersStorageException{
//        ProjectConfiguration config = getConfigForBrokenFiles();
//
//        fileRepo.getKeyParameters(config, "Default");
//
//    }
//
//    @Test()
//    public void updateKeyParameters_correctPath_addNewKeyParameter_noExistConfig_throwsPageStorageException()
//            throws KeyParametersStorageException {
//        ProjectConfigsEntity config = getNotExistConfigEntity();
//        fileRepo.updateKeyParameters(config, Arrays.asList("Customer_ID", "Sales_Order_ID", "Test_key_parameter"),
//                "Default");
//
//        File erFile = new File(config.getPathFolderLocalProject()  + "/key_parameters.json");
//        Assert.assertTrue(erFile.exists());
//        erFile.deleteOnExit();
//    }
//
//    @Test
//    public void updateKeyParameters_correctPathAndAddNewKeyParameter_returnsUpdatedKeyParameters()
//            throws KeyParametersStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        List<String> erKeyParameters = Arrays.asList("Customer_ID", "Sales_Order_ID", "Test_key_parameter");
//
//        fileRepo.updateKeyParameters(config, erKeyParameters, "Default");
//        String pathToFile = config.getPathFolderLocalProject() + "/key_parameters.json";
//        List<String> arKeyParameters =
//                Arrays.asList(mapper.readValue(new String(Files.readAllBytes(new File(pathToFile).toPath())),
//                String[].class));
//
//        fileRepo.getKeyParameters(getDefaultConfig(), "Default");
//
//        Assert.assertEquals(erKeyParameters, arKeyParameters);
//        File erFile = new File(config.getPathFolderLocalProject()  + "/key_parameters.json");
//        erFile.deleteOnExit();
//    }
//
//    @Test()
//    public void getCommonParameters_brokenPath_returnEmptyList() throws CommonParametersStorageException {
//        ProjectConfiguration config = getConfigForBrokenFiles();
//
//        List<SutParameter> commonParameters = fileRepo.getCommonParameters(config, "testFolder");
//
//        Assert.assertTrue(commonParameters.isEmpty());
//    }
//
//    @Test()
//    public void updateCommonParameters_brokenPath_createFile() {
//        ProjectConfigsEntity config = getConfigForBrokenFilesEntity();
//        SutParameter sutParameter = new SutParameter();
//        sutParameter.setName("TestName");
//        List<SutParameter> erSutParameters = Collections.singletonList(sutParameter);
//
//        File erFile = new File(config.getPathFolderLocalProject()  + "/common_parameters.json");
//        Assert.assertFalse(erFile.exists());
//
//        fileRepo.updateCommonParameters(config, erSutParameters, "Default");
//
//        Assert.assertTrue(erFile.exists());
//        erFile.deleteOnExit();
//    }
//
//    @Test()
//    public void createFolder_newPath_SuccessfulCreating()
//            throws FolderStorageException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//
//        fileRepo.createFolder(config, "folder");
//
//        File erFile = new File(config.getPathFolderLocalProject() + "/folder");
//        Assert.assertTrue(erFile.exists());
//
//        erFile.deleteOnExit();
//    }
//
//    @Test(expected = FolderExistException.class)
//    public void createFolder_folderAlreadyExists_FolderStorageException()
//            throws FolderStorageException {
//        ProjectConfigsEntity config = getFolderProjectEntity();
//        fileRepo.createFolder(config, "folder");
//    }
//
//    @Test()
//    public void deleteFolder_folderExists_SuccessfulDeleting()
//            throws FolderStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        File erFile = new File(config.getPathFolderLocalProject() + "/folder");
//        Files.createDirectories(erFile.toPath());
//
//        fileRepo.deleteFolder(config, "folder");
//
//        Assert.assertFalse(erFile.exists());
//    }
//
//    @Test(expected = FolderNotExistException.class)
//    public void deleteFolder_folderDoesntExists_FolderStorageException()
//            throws FolderStorageException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//
//        fileRepo.deleteFolder(config, "testFolder");
//    }
//
//    @Test()
//    public void getFolder_getListWithEmpty_SuccessfulGettingFolders()
//            throws FolderStorageException {
//        ProjectConfiguration config = getTestDefaultConfig();
//
//        List<String> folders = fileRepo.getFolders(config);
//
//        Assert.assertEquals(1, folders.size());
//        Assert.assertEquals("Default", folders.get(0));
//    }
//
//    @Test()
//    public void getFolder_getListWithOnePage_SuccessfulGettingFolders()
//            throws FolderStorageException {
//        ProjectConfiguration config = getFolderProject();
//
//        List<String> folders = fileRepo.getFolders(config);
//
//        Assert.assertEquals(2, folders.size());
//        Assert.assertEquals("folder", folders.get(0));
//        Assert.assertEquals("Default", folders.get(1));
//    }
//
//    @Test()
//    public void editFolder_Exists_SuccessfulEdit()
//            throws FolderStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        File fileOld = new File(config.getPathFolderLocalProject() + "/folderOld");
//        Files.createDirectories(fileOld.toPath());
//
//        fileRepo.editFolder(config, "folderOld", "folderNew");
//
//        File fileNew = new File(config.getPathFolderLocalProject() + "/folderNew");
//        Assert.assertFalse(fileOld.exists());
//        Assert.assertTrue(fileNew.exists());
//        fileNew.deleteOnExit();
//    }
//
//    @Test(expected = FolderNotExistException.class)
//    public void editFolder_folderDoesntExists_FolderStorageException()
//            throws FolderStorageException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//
//        fileRepo.editFolder(config, "folderOld", "folderNew");
//    }
//
//    @Test(expected = PageMoveException.class)
//    public void movePage_PageDoesntExists_PageStorageException()
//            throws PageStorageException {
//        ProjectConfigsEntity config = getNotExistConfigEntity();
//
//        fileRepo.movePage(config, "name",  "newName", "Default", "targetFolder");
//    }
//
//    @Test(expected = PageMoveException.class)
//    public void movePage_FolderDoesntExists_PageStorageException()
//            throws PageStorageException {
//        ProjectConfigsEntity config = getNotExistConfigEntity();
//
//        fileRepo.movePage(config, "CustomerInfo", "newName","doesntExistsFolder", "newFolder");
//    }
//
//    @Test()
//    public void movePage_defaultFolderToNewFolder_SuccessfulMovingPage()
//            throws PageStorageException, FolderStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        PageConfiguration erConfiguration = new PageConfiguration(UUID.randomUUID(),"Moving_page",
//                Collections.emptyList(), false);
//        File oldPathToPage = new File(config.getPathFolderLocalProject() + "/pages/" + "Moving_page");
//        oldPathToPage.createNewFile();
//        mapper.writeValue(oldPathToPage, erConfiguration);
//
//
//        fileRepo.movePage(config, "Moving_page", "Moving_page", "Default", "newFolder");
//
//
//        File newPathToPage = new File(config.getPathFolderLocalProject() + "/newFolder" + "/pages/" + "Moving_page");
//        Assert.assertFalse(oldPathToPage.exists());
//        Assert.assertTrue(newPathToPage.exists());
//        FileUtils.deleteDirectory(new File(config.getPathFolderLocalProject() + "/newFolder"));
//    }
//
//    @Test()
//    public void copyPage_defaultFolderToNewFolder_SuccessfulCopyPage()
//            throws PageStorageException, FolderStorageException, IOException {
//        ProjectConfigsEntity config = getDefaultConfigEntity();
//        PageConfiguration erConfiguration = new PageConfiguration(UUID.randomUUID(),"Coping_page",
//                Collections.emptyList(), false);
//        File oldPathToPage = new File(config.getPathFolderLocalProject() + "/pages/" + "Coping_page");
//        oldPathToPage.createNewFile();
//        mapper.writeValue(oldPathToPage, erConfiguration);
//
//        fileRepo.copyPage(config, "Coping_page", "Coping_page", "Default", "newFolder");;
//        File newPathToPage = new File(config.getPathFolderLocalProject() + "/newFolder" + "/pages/" + "Coping_page");
//
//        Assert.assertTrue(oldPathToPage.exists());
//        Assert.assertTrue(newPathToPage.exists());
//        oldPathToPage.deleteOnExit();
//        FileUtils.deleteDirectory(new File(config.getPathFolderLocalProject() + "/newFolder"));
//    }

    @Test(expected = PageCopyException.class)
    @Ignore
    public void copyPage_WrongPath_PageStorageException()
            throws PageStorageException {
        ProjectConfigsEntity config = getNotExistConfigEntity();
        fileRepo.copyPage(config, "name", "newName", "folder", "newFolder", 1);
    }

    private ProjectConfiguration getFolderProject() {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/folder_project");
        return config;
    }

    private ProjectConfiguration getDefaultConfig() {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/test");
        return config;
    }

    private ProjectConfiguration getTestDefaultConfig() {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/testDefault");
        return config;
    }

    private ProjectConfiguration getConfigForBrokenFiles() {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/BROKEN_FILES");
        return config;
    }

    private ProjectConfiguration getNotExistConfig() {
        ProjectConfiguration config = new ProjectConfiguration();
        config.getRepoConfig().setPath("src/test/config/project/pageNotExist");
        return config;
    }

    private ProjectConfigsEntity getFolderProjectEntity() {
        ProjectConfigsEntity config = new ProjectConfigsEntity();
        config.setPathFolderLocalProject("src/test/config/project/folder_project");
        return config;
    }

    private ProjectConfigsEntity getDefaultConfigEntity() {
        ProjectConfigsEntity config = new ProjectConfigsEntity();
        config.setPathFolderLocalProject("src/test/config/project/test");
        return config;
    }

    private ProjectConfigsEntity getTestDefaultConfigEntity() {
        ProjectConfigsEntity config = new ProjectConfigsEntity();
        config.setPathFolderLocalProject("src/test/config/project/testDefault");
        return config;
    }

    private ProjectConfigsEntity getConfigForBrokenFilesEntity() {
        ProjectConfigsEntity config = new ProjectConfigsEntity();
        config.setPathFolderLocalProject("src/test/config/project/BROKEN_FILES");
        return config;
    }

    private ProjectConfigsEntity getNotExistConfigEntity() {
        ProjectConfigsEntity config = new ProjectConfigsEntity();
        config.setPathFolderLocalProject("src/test/config/project/pageNotExist");
        return config;
    }
}

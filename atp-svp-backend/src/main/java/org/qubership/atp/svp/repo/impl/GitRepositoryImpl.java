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
import java.util.Objects;

import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.svp.core.exceptions.git.GitCloneException;
import org.qubership.atp.svp.core.exceptions.git.GitCommitAndPushException;
import org.qubership.atp.svp.core.exceptions.git.GitGetException;
import org.qubership.atp.svp.core.exceptions.git.GitPullException;
import org.qubership.atp.svp.core.exceptions.git.GitRemoveCommitAndPushException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class GitRepositoryImpl {

    private final Provider<UserInfo> userProvider;
    private final AuthTokenProvider authTokenProvider;

    @Value("${reposUser:user}")
    private String reposUser;

    @Value("${reposPass:pass}")
    private String reposPass;

    @Value("${reposEmail:example@example.com}")
    private String reposEmail;

    @Value("${reposEmail:@example.com}")
    private String reposDomainEmail;

    @Autowired
    public GitRepositoryImpl(Provider<UserInfo> userProvider, AuthTokenProvider authTokenProvider) {
        this.userProvider = userProvider;
        this.authTokenProvider = authTokenProvider;
    }

    private UsernamePasswordCredentialsProvider getGitCred() {
        return new UsernamePasswordCredentialsProvider(reposUser, reposPass);
    }

    /**
     * Git clone.
     *
     * @param path path to GIT.
     * @param url to GIT repository.
     */
    public void gitClone(String path, String url) {
        File file = new File(path);
        try (Git git = Git.cloneRepository().setURI(url).setDirectory(file).setCredentialsProvider(getGitCred())
                .call()) {
            git.getRepository().close();
        } catch (Exception e) {
            log.error("Failed to execute git clone.", e);
            throw new GitCloneException();
        }
    }

    /**
     * Git commit.
     *
     * @param pathToGit path to GIT.
     */
    public void gitCommitAndPush(String pathToGit, String commentMessage) {
        try (Git git = getGitRepo(pathToGit)) {
            git.add().addFilepattern(".").call();
            executeGitCommitAndPush(git, commentMessage);
        } catch (Exception e) {
            String massage = "Failed to execute git commit and push.";
            log.error(massage, e);
            throw new GitCommitAndPushException();
        }
    }

    /**
     * Git pull.
     *
     * @param pathToGit path to GIT.
     */
    public void gitPull(String pathToGit) {
        try (Git git = getGitRepo(pathToGit)) {
            git.pull().setStrategy(MergeStrategy.THEIRS).setRemote("origin").setRemoteBranchName("main")
                    .setCredentialsProvider(getGitCred()).call();
        } catch (GitAPIException e) {
            String massage = "Failed to execute git pull.";
            log.error(massage, e);
            throw new GitPullException();
        }
    }

    /**
     * Git remove and commit.
     *
     * @param pathToGit path to GIT.
     */
    public void gitRemoveCommitAndPush(String pathToGit, String commentMessage, String fileName) {
        try (Git git = getGitRepo(pathToGit)) {
            git.rm().addFilepattern(fileName).call();
            executeGitCommitAndPush(git, commentMessage);
        } catch (Exception e) {
            log.error("Failed to execute git remove commit and push.", e);
            throw new GitRemoveCommitAndPushException();
        }
    }


    /**
     * Add all files to git.
     * @Deprecated.
     */
    @Deprecated
    public void gitAddAllFiles(String pathToGit) {
        try (Git git = getGitRepo(pathToGit)) {
            git.add().addFilepattern(".").call();
        } catch (Exception e) {
            log.error("Failed add all files to git.", e);
            throw new RuntimeException("Failed add all files to git.", e);
        }
    }

    private void executeGitCommitAndPush(Git git, String commitMessage) throws IOException,
            GitAPIException {
        String endCommitMessage = " (commit from SVP tool)";
        try {
            CommitCommand commit = git.commit().setMessage(commitMessage + endCommitMessage)
                    .setAuthor(getAuthorCred());
            commit.setCredentialsProvider(getGitCred());
            commit.call();
            git.push().setRemote("origin").setRefSpecs(new RefSpec("refs/heads/main:refs/heads/main"))
                    .setCredentialsProvider(getGitCred()).call();
        } catch (Exception e) {
            if (Objects.nonNull(git)) {
                ObjectId lastCommitId = git.getRepository().resolve("HEAD^");
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(lastCommitId.getName()).call();
            }
            String massage = "Failed - executeGitCommitAndPush.";
            log.error(massage, e);
            throw e;
        }
    }

    /**
     * Get GIT repository.
     *
     * @param pathToGit path to GIT
     */
    private Git getGitRepo(String pathToGit) {
        try {
            File file = new File(pathToGit);
            return Git.open(file);
        } catch (IOException e) {
            log.error("Failed to get GIT repository.", e);
            throw new GitGetException();
        }
    }

    /**
     * Get the user credential for author .
     *
     * @return PersonIdent contains username and email.
     */
    private PersonIdent getAuthorCred() {
        try {
            PersonIdent credential;
            if (authTokenProvider.isAuthentication()) {
                String username = userProvider.get().getUsername();
                username = Strings.isNullOrEmpty(username) ? reposUser : username;
                credential = new PersonIdent(username, reposDomainEmail);
            } else {
                credential = new PersonIdent(reposUser, reposEmail);
            }
            return credential;
        } catch (NullPointerException ex) {
            return new PersonIdent(reposUser, reposEmail);
        }
    }
}

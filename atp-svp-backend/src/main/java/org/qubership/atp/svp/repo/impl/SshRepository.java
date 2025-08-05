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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.svp.core.exceptions.SshTimeOutException;
import org.qubership.atp.svp.model.environments.Server;
import org.slf4j.MDC;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class SshRepository {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(20);
    private static final String INTERRUPT_SIGNAL = "INT";

    /**
     * Execute command SSH.
     *
     * @param connectionProperties Settings for connection ssh
     * @param command String command line with ssh
     * @return String sshCommandResult
     * @throws JSchException Exception
     */
    public String executeCommandSsh(Server connectionProperties, String command)
            throws JSchException {
        Session session = createSession(connectionProperties);
        String host = connectionProperties.getHost();
        try {
            log.info("Connecting with SSH host. Host: [{}]. Command: [{}].", host, command);
            session.connect();
            log.debug("Start execute SSH command on the host. Host: [{}]. Command: [{}].", host, command);
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            String sshCommandResult = runCommand(session, connectionProperties, command);
            stopWatch.stop();
            log.debug("Executed SSH command on the host. Host: [{}]. Command: [{}]. Time in seconds: [{}].",
                    host, command, stopWatch.getTotalTimeSeconds());
            return sshCommandResult;
        } catch (JSchException e) {
            log.error("Error while connecting with SSH host. Host: [{}]. Command: [{}].", host, command, e);
            throw e;
        } finally {
            session.disconnect();
            log.info("Disconnected with SSH host. Host: [{}]. Command: [{}].", host, command);
        }
    }

    private String runCommand(Session session, Server connectionProperties, String command) {
        SshChannelType channelType = useChannel(connectionProperties);
        try {
            switch (channelType) {
                case EXEC: {
                    return runExecCommand(session, connectionProperties, command, channelType);
                }
                case SHELL: {
                    return runShellCommand(session, connectionProperties, command, channelType);
                }
                default: {
                    String messageTemplate = "Unknown channel. Channel: [%s].";
                    String message = String.format(messageTemplate, channelType);
                    throw new IllegalArgumentException(message);
                }
            }
        } catch (JSchException | IOException e) {
            String messageTemplate = "Cannot run command. Command: [%s]. Cause: [%s].";
            String message = String.format(messageTemplate, command, e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    private String runExecCommand(Session session, Server connectionProperties, String command,
                                  SshChannelType channelType)
            throws JSchException, IOException {
        final ChannelExec channel = (ChannelExec) session.openChannel(channelType.toString());
        final AtomicBoolean isExecution = new AtomicBoolean(true);
        try {
            channel.setPty(connectionProperties.isPty());
            channel.setCommand(command);
            channel.setInputStream(null);
            InputStream errorIn;
            StringBuilder stdOutBuilder = new StringBuilder();
            OffsetDateTime started = OffsetDateTime.now();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
                errorIn = channel.getErrStream();
                channel.connect(connectionProperties.getTimeoutConnect());

                interruptExecutionOnTimeout(connectionProperties.getTimeoutExecute(), channel, command, isExecution);
                readingResponse(command, channelType, reader, stdOutBuilder);
            }
            String stdErr = IOUtils.toString(errorIn, StandardCharsets.UTF_8);
            if (StringUtils.isNotBlank(stdErr)) {
                String messageTemplate = "There was a problem running the command. "
                        + "Command: [{}], STDERR: [{}].";
                log.warn(messageTemplate, command, stdErr);
                log.debug("STDOUTBuilder: [{}]", stdOutBuilder);
            }
            checkTimeout(started, connectionProperties.getTimeoutExecute(), stdOutBuilder.toString());
            return stdOutBuilder.toString();
        } finally {
            isExecution.set(false);
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void checkTimeout(OffsetDateTime started, int timout, String result) {
        OffsetDateTime finished = OffsetDateTime.now();
        long startedMilli = started.toInstant().toEpochMilli();
        long finishedMilli = finished.toInstant().toEpochMilli();
        long duration = finishedMilli - startedMilli;
        if (result.isEmpty() && duration > timout) {
            throw new SshTimeOutException();
        }
    }

    private String runShellCommand(Session session, Server connectionProperties, String command,
                                   SshChannelType channelType)
            throws JSchException, IOException {
        if (command == null || command.trim().isEmpty()) {
            throw new IllegalArgumentException("Command cannot be empty");
        }
        final ChannelShell channel = (ChannelShell) session.openChannel(channelType.toString());
        final AtomicBoolean isExecution = new AtomicBoolean(true);
        try {
            channel.setPty(connectionProperties.isPty());
            if (connectionProperties.isPtyTypeDumb()) {
                channel.setPtyType("dumb");
            }
            final OffsetDateTime started = OffsetDateTime.now();
            channel.setExtOutputStream(new PipedOutputStream());
            channel.connect(connectionProperties.getTimeoutConnect());
            StringBuilder stdOutBuilder = new StringBuilder();
            try (PrintStream input = new PrintStream(channel.getOutputStream())) {
                input.print(command);
                input.print("\n");
                input.print("exit\n");
                input.flush();
                interruptExecutionOnTimeout(connectionProperties.getTimeoutExecute(), channel, command, isExecution);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()))) {
                    readingResponse(command, channelType, reader, stdOutBuilder);
                }
            }
            checkTimeout(started, connectionProperties.getTimeoutExecute(), stdOutBuilder.toString());
            return stdOutBuilder.toString();
        } finally {
            isExecution.set(false);
            if (channel != null) {
                channel.disconnect();
            }
        }
    }


    private void readingResponse(String command, SshChannelType channelType, BufferedReader reader,
                                 StringBuilder stdOutBuilder) throws IOException {
        log.debug("Start reader lines with Command: [{}], Channel type: [{}].", command, channelType);
        String line;
        while ((line = reader.readLine()) != null) {
            log.debug("Reader line: [{}],\n", line);
            stdOutBuilder.append(line).append("\n");
        }
        if (stdOutBuilder.length() > 0) {
            stdOutBuilder.setLength(stdOutBuilder.length() - 1);
        }
    }

    private void interruptExecutionOnTimeout(int timeout, Channel channel, String command, AtomicBoolean isExecution) {
        Map<String, String> mdcMap = MDC.getCopyOfContextMap();
        executorService.schedule(() -> {
            MdcUtils.setContextMap(mdcMap);
            if (isExecution.get()) {
                try {
                    channel.sendSignal(INTERRUPT_SIGNAL);
                } catch (Exception e) {
                    log.error("Unexpected error in interruptExecutionOnTimeout", e);
                }
                channel.disconnect();
                log.warn("SSH command interruption and channel disconnect by timeout: [{}], "
                        + "send interrupt signal: [{}], " + ", command SSH: [{}] ", timeout, INTERRUPT_SIGNAL, command);
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    private SshChannelType useChannel(Server connectionSettings) {
        return connectionSettings.isShell() ? SshChannelType.SHELL : SshChannelType.EXEC;
    }

    private Session createSession(Server connectionProperties) {
        String host = connectionProperties.getHost();
        int port = connectionProperties.getPort();
        String login = connectionProperties.getUser();
        String password = connectionProperties.getPass();
        String key = connectionProperties.getKey();
        String passphrase = connectionProperties.getPassPhrase();
        try {
            JSch jsch = new JSch();
            if (StringUtils.isNotBlank(key)) {
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                byte[] passphraseBytes = null;
                if (passphrase != null && !passphrase.isEmpty()) {
                    passphraseBytes = passphrase.getBytes(StandardCharsets.UTF_8);
                }
                jsch.addIdentity("id_rsa", keyBytes, null, passphraseBytes);
            }
            Session session = jsch.getSession(login, host, port);
            session.setConfig(createSessionProperties(session));
            session.setServerAliveInterval(connectionProperties.getTimeoutServAliveInterval());
            session.setServerAliveCountMax(Integer.MAX_VALUE);
            session.setTimeout(connectionProperties.getTimeoutConnect());
            session.setPassword(password);
            return session;
        } catch (JSchException e) {
            String messageTemplate = "Can't open ssh connection. Host: [%s], Port: [%s], Login: [%s].";
            String message = String.format(messageTemplate, host, port, login);
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private Properties createSessionProperties(Session session) {
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        config.put("cipher.c2s", session.getConfig("cipher.c2s") + ",ssh-rsa,signature.dss");
        config.put("cipher.s2c", session.getConfig("cipher.s2c") + ",ssh-rsa,signature.dss");
        config.put("server_host_key", session.getConfig("server_host_key") + ",ssh-rsa,signature.dss");
        config.put("PubkeyAcceptedAlgorithms", session.getConfig("PubkeyAcceptedAlgorithms") + ",ssh-rsa,signature"
                + ".dss");
        return config;
    }
}

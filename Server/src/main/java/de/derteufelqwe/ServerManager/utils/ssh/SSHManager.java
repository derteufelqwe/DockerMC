package de.derteufelqwe.ServerManager.utils.ssh;

import com.jcraft.jsch.*;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class to connect to a server via SSH and execute commands.
 */
public class SSHManager {

    private UserInfo user;
    private String username;
    private String host;
    private int timeout = 30;

    private JSch jSch = new JSch();
    private Session session;

    public SSHManager(String username, String password, String host) {
        this.user = new SSHUser(username, password);
        this.username = username;
        this.host = host;
    }

    public SSHManager(String username, String password, String host, int timeout) {
        this(username, password, host);
        this.timeout = timeout;
    }

    public void connect() throws JSchException {
        this.session = jSch.getSession(this.username, this.host, 22);
        this.session.setUserInfo(this.user);
        this.session.connect(timeout);
    }

    public void close() {
        this.session.disconnect();
    }

    public String sendCommand(String command) {
        StringBuilder outputBuffer = new StringBuilder();

        try {
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            InputStream commandOutput = channel.getInputStream();
            channel.connect();
            int readByte = commandOutput.read();

            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = commandOutput.read();
            }

            channel.disconnect();
        } catch (IOException | JSchException ioX) {
            return null;
        }

        return outputBuffer.toString().trim();
    }

}

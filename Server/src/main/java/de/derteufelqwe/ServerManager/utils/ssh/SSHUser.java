package de.derteufelqwe.ServerManager.utils.ssh;

import com.jcraft.jsch.UserInfo;

/**
 * Simple implementation of {@link UserInfo} for the {@link SSHManager}
 */
public class SSHUser implements UserInfo {

    private String username;
    private String password;

    public SSHUser(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getPassphrase() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean promptPassword(String s) {
        return true;
    }

    @Override
    public boolean promptPassphrase(String s) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean promptYesNo(String s) {
        return true;
    }

    @Override
    public void showMessage(String s) {
        System.out.println("Message: " + s);
    }

}

package de.derteufelqwe.commons;

import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.List;

/**
 * Confirms that a Minecraft / BC server is working as expected
 */
public class PingServer {


    public static void main(String[] args) {
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            PingServer pingServer = new PingServer();
            pingServer.pingServer(host, port);
            pingServer.pingPlugin(host, port);

        } catch (IndexOutOfBoundsException | NumberFormatException e1) {
            System.exit(100);
        }

    }

    /**
     * Checks that the MC / BC server accepts connections
     * @param host
     * @param port
     */
    public void pingServer(String host, int port) {
        try {
            ServerListPing ping = new ServerListPing();
            ping.setAddress(new InetSocketAddress(host, port));
            ServerListPing.StatusResponse response = ping.fetchData();

            if (response == null) {
                System.out.println("No response from MC server.");
                System.exit(102);
            }

        } catch (Exception e1) {
            System.out.println("Exception: " + e1.getMessage());
            System.exit(101);
        }
    }

    /**
     * Checks that the small webserver in the DockerMC plugins are running
     * @param host
     * @param port
     */
    public void pingPlugin(String host, int port) {
        try {
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpGet get = new HttpGet("http://" + host + ":8001" + "/health");

                try (CloseableHttpResponse response = httpClient.execute(get)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        return;     // Success

                    } else {
                        System.out.println("Invalid HTTP status code: " + statusCode);
                        System.exit(500);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            System.exit(110);
        }
    }


    /**
     * Pings a minecraft server
     *
     * @author zh32 <zh32 at zh32.de>
     * @source https://gist.github.com/zh32/7190955
     */
    public static class ServerListPing {

        private InetSocketAddress host;
        private int timeout = 7000;
        private Gson gson = new Gson();

        public void setAddress(InetSocketAddress host) {
            this.host = host;
        }

        public InetSocketAddress getAddress() {
            return this.host;
        }

        void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        int getTimeout() {
            return this.timeout;
        }

        public int readVarInt(DataInputStream in) throws IOException {
            int i = 0;
            int j = 0;
            while (true) {
                int k = in.readByte();
                i |= (k & 0x7F) << j++ * 7;
                if (j > 5) throw new RuntimeException("VarInt too big");
                if ((k & 0x80) != 128) break;
            }
            return i;
        }

        public void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
            while (true) {
                if ((paramInt & 0xFFFFFF80) == 0) {
                    out.writeByte(paramInt);
                    return;
                }

                out.writeByte(paramInt & 0x7F | 0x80);
                paramInt >>>= 7;
            }
        }

        public StatusResponse fetchData() throws IOException {

            Socket socket = new Socket();
            OutputStream outputStream;
            DataOutputStream dataOutputStream;
            InputStream inputStream;
            InputStreamReader inputStreamReader;

            socket.setSoTimeout(this.timeout);

            socket.connect(host, timeout);

            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);

            inputStream = socket.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);

            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream handshake = new DataOutputStream(b);
            handshake.writeByte(0x00); //packet id for handshake
            writeVarInt(handshake, 4); //protocol version
            writeVarInt(handshake, this.host.getHostString().length()); //host length
            handshake.writeBytes(this.host.getHostString()); //host string
            handshake.writeShort(host.getPort()); //port
            writeVarInt(handshake, 1); //state (1 for handshake)

            writeVarInt(dataOutputStream, b.size()); //prepend size
            dataOutputStream.write(b.toByteArray()); //write handshake packet


            dataOutputStream.writeByte(0x01); //size is only 1
            dataOutputStream.writeByte(0x00); //packet id for ping
            DataInputStream dataInputStream = new DataInputStream(inputStream);
            int size = readVarInt(dataInputStream); //size of packet
            int id = readVarInt(dataInputStream); //packet id

            if (id == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (id != 0x00) { //we want a status response
                throw new IOException("Invalid packetID");
            }
            int length = readVarInt(dataInputStream); //length of json string

            if (length == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (length == 0) {
                throw new IOException("Invalid string length.");
            }

            byte[] in = new byte[length];
            dataInputStream.readFully(in);  //read json string
            String json = new String(in);


            long now = System.currentTimeMillis();
            dataOutputStream.writeByte(0x09); //size of packet
            dataOutputStream.writeByte(0x01); //0x01 for ping
            dataOutputStream.writeLong(now); //time!?

            readVarInt(dataInputStream);
            id = readVarInt(dataInputStream);
            if (id == -1) {
                throw new IOException("Premature end of stream.");
            }

            if (id != 0x01) {
                throw new IOException("Invalid packetID");
            }
            long pingtime = dataInputStream.readLong(); //read response

            StatusResponse response = gson.fromJson(json, StatusResponse.class);

            dataOutputStream.close();
            outputStream.close();
            inputStreamReader.close();
            inputStream.close();
            socket.close();

            return response;
        }


        public class StatusResponse {
            private Description description;
            private Players players;
            private Version version;
            private String favicon;
            private int time;

            public Description getDescription() {
                return description;
            }

            public Players getPlayers() {
                return players;
            }

            public Version getVersion() {
                return version;
            }

            public String getFavicon() {
                return favicon;
            }

            public int getTime() {
                return time;
            }

            public void setTime(int time) {
                this.time = time;
            }

        }

        public class Players {
            private int max;
            private int online;
            private List<Player> sample;

            public int getMax() {
                return max;
            }

            public int getOnline() {
                return online;
            }

            public List<Player> getSample() {
                return sample;
            }
        }

        public class Player {
            private String name;
            private String id;

            public String getName() {
                return name;
            }

            public String getId() {
                return id;
            }

        }

        public class Version {
            private String name;
            private int protocol;

            public String getName() {
                return name;
            }

            public int getProtocol() {
                return protocol;
            }
        }

        public class Description {
            private String text;

            public String getText() {
                return text;
            }

        }

    }

}

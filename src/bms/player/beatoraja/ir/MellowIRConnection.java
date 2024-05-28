package bms.player.beatoraja.ir;


import bms.player.beatoraja.battle.BattleConnection;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bms.player.beatoraja.battle.BattleRoom;
import bms.player.beatoraja.song.SongData;
import org.zeromq.*;
import zmq.Msg;

public class MellowIRConnection implements IRConnection, BattleConnection {
    // IR Connection
    public static final String NAME = "Mell0wIR";
    public static final String HOME = "https://ir.mell0w-5phere.net";
    private static final String baseURL = "http://localhost:8080/api";
    private String playerId;
    private String session;
    // ZMQ
    private static final String MQReqURL = "tcp://localhost:5555";
    private static final String MQSubURL = "tcp://localhost:5563";
    private ZContext zctx;
    private ZMQ.Socket reqSock;
    private ZMQ.Socket subSock;
    private ZMQ.Poller poller;
    private List<String> recvBuf;
    private List<Message> sendBuf;
    // battle matching
    private BattleRoom room;
    private Thread oppPoller;
    private Thread listPoller;

    public void log(String msg, Level level) {
        Logger.getGlobal().log(level, String.format("[Mell0wIR] %s", msg));
    }

    public String readAll(InputStream s) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(s));
        char[] buf = new char[256];
        StringBuilder res = new StringBuilder();
        int n = br.read(buf);
        while (n > 0) {
            res.append(String.copyValueOf(buf, 0, n));
            n = br.read(buf);
        }
        return res.toString();
    }

    public String request(String endpoint, String auth, String data) throws IOException {
        URL url = new URL(baseURL + endpoint);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Authorization", auth);
        if (data != null) {
            conn.setDoOutput(true);
            PrintStream ps = new PrintStream(conn.getOutputStream());
            ps.print(data);
        }
        return readAll(conn.getInputStream());
    }

    // IRConnection
    public IRResponse<IRPlayerData> register(IRAccount account) {
        return null;
    }

    public IRResponse<IRPlayerData> login(IRAccount account) {
        try {
            playerId = account.id;
            String signature = "Basic " + Base64.getEncoder().encodeToString(String.format("%s:%s", account.id, account.password).getBytes());
            String data = request("/me", signature, null);
            String[] parts = data.split(":");
            if (parts.length != 2) {
                log("invalid response from server", Level.WARNING);
                throw new IOException();
            }
            log("login Succeeded. username: " + parts[0], Level.INFO);
            IRPlayerData irpd = new IRPlayerData(account.id, parts[0], null);
            session = parts[1];
            return new MellowIRResponse<>(true, null, irpd);
        } catch (IOException e) {
            e.printStackTrace();
            return new MellowIRResponse<>(false, null, null);
        }
    }

    public IRResponse<IRPlayerData[]> getRivals() {
        return new MellowIRResponse<>(false, null, null);
    }

    public IRResponse<IRTableData[]> getTableDatas() {
        return new MellowIRResponse<>(false, null, null);
    }

    public IRResponse<IRScoreData[]> getPlayData(IRPlayerData player, IRChartData chart) {
        return null;
    }

    public IRResponse<IRScoreData[]> getCoursePlayData(IRPlayerData player, IRCourseData course) {
        return null;
    }

    public IRResponse<Object> sendPlayData(IRChartData model, IRScoreData score) {
        return null;
    }

    public IRResponse<Object> sendCoursePlayData(IRCourseData course, IRScoreData score) {
        return null;
    }

    public String getSongURL(IRChartData chart) {
        return null;
    }

    public String getCourseURL(IRCourseData course) {
        return null;
    }

    public String getPlayerURL(IRPlayerData player) {
        return null;
    }

    // ZMQ
    private static class Message {
        public String cmd;
        public String content;

        public Message(String cmd, String content) {
            this.cmd = cmd;
            this.content = content;
        }

        public String toString() {
            return String.format("Message<cmd: %s, content: %s>", cmd, content);
        }

        public static Message fromZMsgReq(ZMsg m) {
            String cmd = new String(m.pop().getData());
            String content = new String(m.pop().getData());
            return new Message(cmd, content);
        }

        public static Message fromZMsgSub(ZMsg m) {
            m.pop(); // ignore topic
            String cmd = new String(m.pop().getData());
            String content = new String(m.pop().getData());
            return new Message(cmd, content);
        }
    }

    private boolean sendReqMsg(Message msg) {
        if (room == null) {
            log("not connected to room yet", Level.WARNING);
            return false;
        }
        reqSock.sendMore(room.roomId.getBytes(StandardCharsets.UTF_8));
        reqSock.sendMore(msg.cmd.getBytes(StandardCharsets.UTF_8));
        if (msg.content == null) {
            return reqSock.send(new byte[0]);
        } else {
            return reqSock.send(msg.content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void initSocket(String topic) {
        if (zctx != null && !zctx.isClosed()) {
            zctx.close();
        }

        zctx = new ZContext();
        reqSock = zctx.createSocket(SocketType.REQ);
        reqSock.setIdentity(playerId.getBytes(StandardCharsets.UTF_8));
        subSock = zctx.createSocket(SocketType.SUB);

        if (!reqSock.connect(MQReqURL)) {
            log("cannot connect req socket", Level.WARNING);
            reqSock.close();
        }
        if (!subSock.connect(MQSubURL)) {
            log("cannot connect sub socket", Level.WARNING);
            subSock.close();
        }
        if (!subSock.subscribe(topic)) {
            log("cannot subscribe topic:" + topic, Level.WARNING);
            subSock.close();
        }
        recvBuf = Collections.synchronizedList(new ArrayList<>());
        sendBuf = Collections.synchronizedList(new ArrayList<>());
        poller = zctx.createPoller(2);
        int reqIdx = poller.register(reqSock);
        int subIdx = poller.register(subSock);

        if (!sendReqMsg(new Message("JOIN_REQ", null))) {
            log("cannot send join room request", Level.WARNING);
            return;
        }

        Message rep = Message.fromZMsgReq(ZMsg.recvMsg(reqSock));
        if (!rep.cmd.equals("JOIN_ACK")) {
            log("cannot join room:" + rep, Level.WARNING);
            return;
        }
        Thread pollThread = new Thread(() -> pollSockets(reqIdx, subIdx));
        pollThread.start();
        Thread thumpThread = new Thread(() -> {
            while (!zctx.isClosed()) {
                try {
                    putSendBuffer("THUMP_REQ", null);
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log("zmq thump thread quit", Level.INFO);
        });
        thumpThread.start();
    }

    public String getRecvBuffer() {
        synchronized (recvBuf) {
            if (recvBuf.isEmpty()) {
                return null;
            }
            return recvBuf.remove(0);
        }
    }

    public void putSendBuffer(String cmd, String content) {
        synchronized (sendBuf) {
            sendBuf.add(new Message(cmd, content));
        }
    }
    public boolean createRoom() {
        String signature = String.format("Bearer %s", session);
        try {
            String res = request("/room/create", signature, null);
            if (res.startsWith("OK:")) {
                String[] roomInfo = res.split(":")[1].split(",");
                room = new BattleRoom(roomInfo[0], roomInfo[1]);
                log("room name acquired:" + room.roomId, Level.INFO);
                initSocket(room.roomId);
                startPollOpponent();
                return true;
            } else {
                throw new IOException("WOOOOOOOOOOO:" + res);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("failed room create request", Level.WARNING);
            return false;
        }
    }

    public boolean joinRoom(String roomKeyword) {
        String signature = String.format("Bearer %s", session);
        try {
            String res = request("/room/join", signature, "keyword=" + roomKeyword);
            if (res.startsWith("OK:")) {
                room = new BattleRoom(res.split(":")[1], roomKeyword);
                log("room joined:" + room.roomId, Level.INFO);
                initSocket(room.roomId);
                startPollOpponent();
                return true;
            } else {
                throw new IOException("WOOOOOOOOOOO:" + res);
            }
        } catch (IOException e) {
            e.printStackTrace();
            log("failed room join request", Level.WARNING);
            return false;
        }
    }

    public String getRoomKey() {
        return room.roomKey;
    }

    public IRPlayerData getOpponent() {
        return room.opponent;
    }

    private void startPollOpponent() {
        if (oppPoller != null && oppPoller.isAlive()) {
            return;
        }
        oppPoller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                putSendBuffer("POLL_OPPONENT", null);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log("interrupted opponent poll thread", Level.INFO);
                    break;
                }
            }
        });
        oppPoller.start();
    }

    public void sendSongList(SongData[] songs) {
        String[] hashes = new String[songs.length];
        for(int i = 0; i < songs.length; i++) {
            hashes[i] = songs[i].getSha256();
        }
        String body = String.join(",", hashes);
        putSendBuffer("SEND_LIST", body);
        startPollMusicList();
    }

    private void startPollMusicList() {
        if (listPoller != null && listPoller.isAlive()) {
            return;
        }
        listPoller = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                putSendBuffer("POLL_LIST", null);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log("interrupted song list poll thread", Level.INFO);
                    break;
                }
            }
        });
        listPoller.start();
    }


    // async threads
    private void pollSockets(int reqIdx, int subIdx) {
        long lastThump = System.currentTimeMillis();
        while (!zctx.isClosed()) {
            // 1 minutes of silence
            if (System.currentTimeMillis() - lastThump > 60000) {
                log("the room may have been closed!", Level.WARNING);
                // TODO
                return;
            }
            poller.poll();
            synchronized (recvBuf) {
                if (poller.pollin(reqIdx)) {
                    Message repMsg = Message.fromZMsgReq(ZMsg.recvMsg(reqSock));
                    switch (repMsg.cmd) {
                        case "ERR_INVALID_ROOM":
                            log("the room may have been closed!", Level.WARNING);
                            log(repMsg.content, Level.INFO);
                            return;
                        case "ERR_NOT_REGISTERED":
                            log("something going wrong", Level.WARNING);
                            return;
                        case "THUMP_ACK":
                        case "LIST_ACK":
                            log("got ack", Level.INFO);
                            break;
                        case "OPPONENT_NOT_FOUND":
                            log("opponent not found", Level.INFO);
                            break;
                        case "OPPONENT_FOUND":
                            oppPoller.interrupt();
                            String[] parts = repMsg.content.split(":", 2);
                            room.opponent = new IRPlayerData(parts[0], parts[1], "");
                            break;
                        case "LIST_NOT_READY":
                            log("list not ready", Level.INFO);
                            break;
                        case "LIST_READY":
                            listPoller.interrupt();
                            log("list acquired", Level.INFO);
                            room.availSongs = repMsg.content.split(",");
                            break;
                    }
                }
                if (poller.pollin(subIdx)) {
                    Message subMsg = Message.fromZMsgSub(ZMsg.recvMsg(subSock));
                    log("sub receiced: " + subMsg, Level.INFO);
                    lastThump = System.currentTimeMillis();
                }
            }
            synchronized (sendBuf) {
                if (!sendBuf.isEmpty()) {
                    if (poller.pollout(reqIdx)) {
                        Message msg = sendBuf.remove(0);
                        log("send req: " + msg, Level.INFO);

                        boolean res = sendReqMsg(msg);
                        if (!res) {
                            log("failed to send req", Level.WARNING);
                        }
                    }
                }
            }
        }
        log("zmq polling thread quit", Level.INFO);
    }
}


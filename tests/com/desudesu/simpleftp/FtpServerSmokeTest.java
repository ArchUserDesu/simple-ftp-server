package com.desudesu.simpleftp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public final class FtpServerSmokeTest {
    public static void main(String[] args) throws Exception {
        FtpServer server = new FtpServer(23212, "ftp", "secret");
        server.start();
        Socket socket = null;
        for (int i=0; i<20 && socket==null; i++) {
            try { socket = new Socket("127.0.0.1", 23212); }
            catch (ConnectException e) { Thread.sleep(25); }
        }
        if (socket == null) throw new AssertionError("server did not listen");
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        expect(in, 220);
        send(out, "USER wrong"); expect(in, 530);
        send(out, "PASS secret"); expect(in, 530);
        send(out, "USER ftp"); expect(in, 331);
        send(out, "PASS secret"); expect(in, 230);
        send(out, "PWD"); expect(in, 257);
        send(out, "SYST"); expect(in, 215);
        send(out, "QUIT"); expect(in, 221);
        socket.close(); server.stop();

        FtpServer anonymous = new FtpServer(23213, "", "", true);
        anonymous.start();
        Socket anonSocket = connect(23213);
        BufferedReader anonIn = new BufferedReader(new InputStreamReader(anonSocket.getInputStream(), StandardCharsets.UTF_8));
        PrintWriter anonOut = new PrintWriter(new OutputStreamWriter(anonSocket.getOutputStream(), StandardCharsets.UTF_8), true);
        expect(anonIn, 220);
        send(anonOut, "PWD"); expect(anonIn, 257);
        send(anonOut, "USER anonymous"); expect(anonIn, 331);
        send(anonOut, "PASS guest@example.com"); expect(anonIn, 230);
        send(anonOut, "QUIT"); expect(anonIn, 221);
        anonSocket.close(); anonymous.stop();
        System.out.println("FTP authenticated and anonymous smoke tests passed");
    }
    private static Socket connect(int port) throws Exception {
        for(int i=0;i<20;i++){try{return new Socket("127.0.0.1",port);}catch(ConnectException e){Thread.sleep(25);}}
        throw new AssertionError("server did not listen on "+port);
    }
    private static void send(PrintWriter out, String line) { out.print(line+"\r\n"); out.flush(); }
    private static void expect(BufferedReader in, int code) throws Exception {
        String line=in.readLine();
        if(line==null || !line.startsWith(String.valueOf(code))) throw new AssertionError("expected "+code+", got "+line);
    }
}

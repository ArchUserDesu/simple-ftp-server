package com.desudesu.simpleftp;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

final class FtpServer {
    private final int port; private final String user,pass; private final boolean anonymous;
    private volatile boolean live; private ServerSocket listener;
    private final ExecutorService pool=Executors.newCachedThreadPool();
    FtpServer(int p,String u,String pw){this(p,u,pw,false);}
    FtpServer(int p,String u,String pw,boolean anon){port=p;user=u;pass=pw;anonymous=anon;}
    void start(){live=true;pool.execute(()->{try{listener=new ServerSocket();listener.setReuseAddress(true);listener.bind(new InetSocketAddress(port));while(live)pool.execute(new Session(listener.accept()));}catch(IOException ignored){stop();}});}
    void stop(){live=false;try{if(listener!=null)listener.close();}catch(IOException ignored){}pool.shutdownNow();}

    private final class Session implements Runnable {
        private final Socket control; private BufferedReader in; private PrintWriter out;
        private boolean authed=anonymous,userAccepted=false,binary=true; private File cwd=root(), renameFrom; private ServerSocket passive; private InetSocketAddress active;
        Session(Socket s){control=s;}
        public void run(){
            try{control.setSoTimeout(300000);in=new BufferedReader(new InputStreamReader(control.getInputStream(),StandardCharsets.UTF_8));out=new PrintWriter(new OutputStreamWriter(control.getOutputStream(),StandardCharsets.UTF_8),true);say(220,"Simple FTP Server ready");
                String line;while((line=in.readLine())!=null){String cmd,arg="";int sp=line.indexOf(' ');if(sp<0)cmd=line.toUpperCase(Locale.US);else{cmd=line.substring(0,sp).toUpperCase(Locale.US);arg=line.substring(sp+1);}if(!handle(cmd,arg))break;}
            }catch(Exception ignored){}finally{closePassive();try{control.close();}catch(Exception ignored){}}
        }
        private boolean handle(String c,String a)throws IOException{
            if(c.equals("USER")){if(anonymous){authed=true;userAccepted=true;say(331,"Anonymous access; any password accepted");}else{authed=false;userAccepted=a.equals(user);if(userAccepted)say(331,"Password required");else say(530,"Invalid username");}return true;}
            if(c.equals("PASS")){authed=anonymous||(userAccepted&&a.equals(pass));say(authed?230:530,authed?"Logged in":"Invalid credentials");return true;}
            if(c.equals("QUIT")){say(221,"Goodbye");return false;}
            if(c.equals("NOOP")){say(200,"OK");return true;}
            if(c.equals("SYST")){say(215,"UNIX Type: L8");return true;}
            if(c.equals("FEAT")){out.print("211-Features\r\n UTF8\r\n SIZE\r\n MDTM\r\n MLST type*;size*;modify*;\r\n211 End\r\n");out.flush();return true;}
            if(c.equals("OPTS")){say(200,"UTF8 enabled");return true;}
            if(!authed){say(530,"Log in first");return true;}
            switch(c){
                case "PWD": say(257,"\""+virtual(cwd)+"\"");break;
                case "CWD": {File f=resolve(a);if(f!=null&&f.isDirectory()){cwd=f;say(250,"Directory changed");}else say(550,"Directory unavailable");break;}
                case "CDUP": {File f=resolve("..");if(f!=null){cwd=f;say(250,"Directory changed");}else say(550,"At root");break;}
                case "TYPE": binary=!a.toUpperCase(Locale.US).startsWith("A");say(200,"Type set");break;
                case "PASV": enterPassive(false);break;
                case "EPSV": enterPassive(true);break;
                case "PORT": setPort(a);break;
                case "LIST": list(a,false);break;
                case "NLST": list(a,true);break;
                case "MLSD": mlsd(a);break;
                case "RETR": retrieve(a);break;
                case "STOR": store(a);break;
                case "SIZE": {File f=resolve(a);if(f!=null&&f.isFile())say(213,""+f.length());else say(550,"Not a file");break;}
                case "MDTM": {File f=resolve(a);if(f!=null&&f.exists())say(213,new SimpleDateFormat("yyyyMMddHHmmss",Locale.US).format(new Date(f.lastModified())));else say(550,"Not found");break;}
                case "DELE": {File f=resolve(a);say(f!=null&&f.isFile()&&f.delete()?250:550,"Delete "+(f!=null&&!f.exists()?"successful":"failed"));break;}
                case "MKD": {File f=resolveNew(a);if(f!=null&&f.mkdirs())say(257,"Directory created");else say(550,"Create failed");break;}
                case "RMD": {File f=resolve(a);say(f!=null&&!f.equals(root())&&f.isDirectory()&&f.delete()?250:550,"Remove directory result");break;}
                case "RNFR": {File f=resolve(a);if(f!=null&&f.exists()){renameFrom=f;say(350,"Ready for destination");}else say(550,"Not found");break;}
                case "RNTO": {File f=resolveNew(a);if(renameFrom!=null&&f!=null&&renameFrom.renameTo(f))say(250,"Renamed");else say(550,"Rename failed");renameFrom=null;break;}
                case "STAT": say(211,"Server running");break;
                default:say(502,"Command not implemented");
            }return true;
        }
        private void enterPassive(boolean epsv)throws IOException{closePassive();passive=new ServerSocket(0,1);passive.setSoTimeout(30000);int p=passive.getLocalPort();if(epsv)say(229,"Entering Extended Passive Mode (|||"+p+"|)");else{byte[] b=control.getLocalAddress().getAddress();say(227,"Entering Passive Mode ("+(b[0]&255)+","+(b[1]&255)+","+(b[2]&255)+","+(b[3]&255)+","+(p/256)+","+(p%256)+")");}}
        private void setPort(String a){try{String[] x=a.split(",");if(x.length!=6)throw new Exception();String h=x[0]+"."+x[1]+"."+x[2]+"."+x[3];InetAddress host=InetAddress.getByName(h);if(!host.equals(control.getInetAddress()))throw new Exception();int p=Integer.parseInt(x[4])*256+Integer.parseInt(x[5]);if(p<1024||p>65535)throw new Exception();active=new InetSocketAddress(host,p);closePassive();say(200,"Active mode set");}catch(Exception e){say(501,"Bad PORT");}}
        private Socket data()throws IOException{if(passive!=null){Socket s=passive.accept();closePassive();return s;}if(active!=null){Socket s=new Socket();s.connect(active,10000);active=null;return s;}throw new IOException("Use PASV or PORT first");}
        private void list(String a,boolean names){File f=a.isEmpty()?cwd:resolve(stripListFlags(a));if(f==null||!f.exists()){say(550,"Not found");return;}say(150,"Opening data connection");try(Socket d=data();PrintWriter w=new PrintWriter(new OutputStreamWriter(d.getOutputStream(),StandardCharsets.UTF_8))){File[] fs=f.isDirectory()?f.listFiles():new File[]{f};if(fs!=null)for(File q:fs){if(names)w.print(q.getName()+"\r\n");else w.print(listLine(q)+"\r\n");}w.flush();say(226,"Transfer complete");}catch(Exception e){say(425,"Data connection failed");}}
        private void mlsd(String a){File f=a.isEmpty()?cwd:resolve(a);if(f==null||!f.isDirectory()){say(550,"Not found");return;}say(150,"Opening data connection");try(Socket d=data();PrintWriter w=new PrintWriter(new OutputStreamWriter(d.getOutputStream(),StandardCharsets.UTF_8))){File[] fs=f.listFiles();if(fs!=null)for(File q:fs)w.printf(Locale.US,"type=%s;size=%d;modify=%s; %s\r\n",q.isDirectory()?"dir":"file",q.length(),new SimpleDateFormat("yyyyMMddHHmmss",Locale.US).format(new Date(q.lastModified())),q.getName());w.flush();say(226,"Transfer complete");}catch(Exception e){say(425,"Data connection failed");}}
        private void retrieve(String a){File f=resolve(a);if(f==null||!f.isFile()){say(550,"Not found");return;}say(150,"Opening data connection");try(Socket d=data();InputStream src=new FileInputStream(f);OutputStream dst=d.getOutputStream()){copy(src,dst);say(226,"Transfer complete");}catch(Exception e){say(425,"Transfer failed");}}
        private void store(String a){File f=resolveNew(a);if(f==null){say(550,"Invalid path");return;}File parent=f.getParentFile();if(parent==null||!parent.exists()){say(550,"Parent missing");return;}say(150,"Opening data connection");try(Socket d=data();InputStream src=d.getInputStream();OutputStream dst=new FileOutputStream(f)){copy(src,dst);say(226,"Transfer complete");}catch(Exception e){say(425,"Transfer failed");}}
        private File resolve(String a){try{File f=a.startsWith("/")?new File(root(),a.substring(1)):new File(cwd,a);f=f.getCanonicalFile();return inside(f)?f:null;}catch(Exception e){return null;}}
        private File resolveNew(String a){return resolve(a);}
        private boolean inside(File f)throws IOException{String r=root().getCanonicalPath(),p=f.getCanonicalPath();return p.equals(r)||p.startsWith(r+File.separator);}
        private String virtual(File f)throws IOException{String r=root().getCanonicalPath(),p=f.getCanonicalPath();return p.equals(r)?"/":"/"+p.substring(r.length()+1);}
        private String stripListFlags(String a){String s=a.trim();return s.startsWith("-")&&s.contains(" ")?s.substring(s.indexOf(' ')+1):s.startsWith("-")?"":s;}
        private String listLine(File f){String perms=f.isDirectory()?"drwxr-xr-x":"-rw-r--r--";String date=new SimpleDateFormat("MMM dd HH:mm",Locale.US).format(new Date(f.lastModified()));return String.format(Locale.US,"%s 1 ftp ftp %d %s %s",perms,f.length(),date,f.getName());}
        private void copy(InputStream a,OutputStream b)throws IOException{byte[] buf=new byte[65536];int n;while((n=a.read(buf))>=0)b.write(buf,0,n);b.flush();}
        private void closePassive(){try{if(passive!=null)passive.close();}catch(Exception ignored){}passive=null;}
        private void say(int code,String msg){out.print(code+" "+msg+"\r\n");out.flush();}
    }
    private static File root(){return new File("/storage/emulated/0");}
}

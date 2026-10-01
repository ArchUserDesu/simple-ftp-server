package com.desudesu.simpleftp;

import android.app.*;
import android.content.*;
import android.os.*;

public class FtpService extends Service {
    static volatile boolean running=false;
    private FtpServer server;
    private PowerManager.WakeLock lock;
    @Override public void onCreate(){
        super.onCreate();
        if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel("ftp","FTP server",NotificationManager.IMPORTANCE_LOW); getSystemService(NotificationManager.class).createNotificationChannel(c); }
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i==null)return START_NOT_STICKY;
        int port=i.getIntExtra("port",2121); String user=i.getStringExtra("user"), pass=i.getStringExtra("pass"); boolean anonymous=i.getBooleanExtra("anonymous",false);
        Intent open=new Intent(this,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
        Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"ftp"):new Notification.Builder(this);
        Notification n=nb.setSmallIcon(com.desudesu.simpleftp.R.drawable.ic_ftp).setContentTitle("FTP server running")
            .setContentText("ftp://"+MainActivity.localIp()+":"+port).setOngoing(true).setContentIntent(pi).build();
        startForeground(7,n);
        if(server!=null)server.stop();
        server=new FtpServer(port,user,pass,anonymous); server.start(); running=true;
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE); lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"simpleftp:server");lock.acquire();
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){running=false;if(server!=null)server.stop();if(lock!=null&&lock.isHeld())lock.release();stopForeground(true);super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}

package com.matteo.HTTPServer.server;

import java.io.File;

import com.matteo.HTTPServer.utility.DynamicQueue;

public class ResourceDeletionThread implements Runnable {
    private DynamicQueue<String> deleteQueue;
    
    public ResourceDeletionThread(DynamicQueue<String> deleteQueue) {
        this.deleteQueue = deleteQueue;
        Thread thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            File toDelete = new File(deleteQueue.remove());
            while(!toDelete.delete()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}

package com.matteo.HTTPServer.server;

import java.util.Vector;

public class SessionCleanerThread implements Runnable {
	private Thread t;
	private Vector<Session> sessions;
	private final int CLEAN_INTERVAL = 5 * 1000;
	private final int MAX_SESSIONS = 100000;
	public SessionCleanerThread(Vector<Session> sessions) {
		this.sessions = sessions;
		t = new Thread(this);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	@Override
	public void run() {
		while(!Thread.interrupted()) {
			if(sessions.size() > 0) {
				if(sessions.size() > MAX_SESSIONS) {
					sessions.remove(0); // rimuovo quella più vecchia
				} else {
					synchronized(sessions) {
						for(int i = 0; i < sessions.size(); i++) {
							Session session = sessions.get(i);
							if(session.isIsExpiredMethodUnlocked() && session.isExpired()) {
								session.destroy();
								sessions.remove(i);
								break;
							}
						}
					}
				}
			}
			try {
				Thread.sleep(CLEAN_INTERVAL);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
	
	public void interrupt() {
		t.interrupt();
	}
}

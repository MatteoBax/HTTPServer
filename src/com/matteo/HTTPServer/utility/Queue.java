package com.matteo.HTTPServer.utility;

public class Queue <T> {
	private T[] queue;
	private int elements = 0;
	private int testa = 0;
	private int coda = 0;
	
	public Queue(int dim) {
		queue = (T[]) new Object[500];
		for(int i = 0; i < dim; i++) {
			queue[i] = null;
		}
	}
	public synchronized void add(T element) {
		while(elements == queue.length) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		elements++;
		queue[coda] = element;
		coda = (coda + 1) % queue.length;
		if(elements < queue.length) {
			notifyAll();
		}
	}
	
	public synchronized T remove() {
		while(elements == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		elements--;
		T app = queue[testa];
		queue[testa] = null;
		testa = (testa + 1) % queue.length;
		
		if(elements != 0) {
			notifyAll();
		}
		return app;
	}
	
	public synchronized T getFirst() {
		while(elements == 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return queue[testa];
	}
	
	public synchronized void wipe() {
		for(int i = 0; i < queue.length; i++) {
			queue[i] = null;
		}
		
		elements = 0;
		testa = 0;
		coda = 0;
	}
}

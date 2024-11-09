package com.matteo.HTTPServer.server;

import java.util.function.BiConsumer;

import com.matteo.HTTPServer.utility.Queue;

/**
 * Thread che gestisce le richieste delle API
 * 
 * @author Matteo Basso
 */
public class RequestHandlerThread implements Runnable {
	private Queue<DataFromSocketHandler> queue;
	private String path;
	private String method;
	private BiConsumer<Request, Response> handler;
	
	/**
	 * Costruttore del thread (avvia automaticamente il thread)
	 * 
	 * @param queue la coda dei dati provenienti dal gestore del socket da gestire
	 * @param path la path della richiesta (url senza hostname)
	 * @param method il metodo della richiesta (GET, POST ...)
	 * @param handler il gestore della callback
	 */
	public RequestHandlerThread(Queue<DataFromSocketHandler> queue, String path, String method, BiConsumer<Request, Response> handler) {
		this.queue = queue;
		this.path = path;
		this.method = method;
		this.handler = handler;
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		DataFromSocketHandler dt;
		while(true) {
			dt = queue.getFirst();
			if(dt.getMethod().equals(method)) {
				if(dt.getRequestPath().equals(path)) {
					dt = queue.remove();
					new LamdaExpressionHandlerThread(handler, dt);
				}
			}
		}
	}
	
	public class LamdaExpressionHandlerThread implements Runnable {
		private BiConsumer<Request, Response> handler;
		private DataFromSocketHandler dataFromSocketHandler;
		
		public LamdaExpressionHandlerThread(BiConsumer<Request, Response> handler, DataFromSocketHandler dataFromSocketHandler) {
			this.handler = handler;
			this.dataFromSocketHandler = dataFromSocketHandler;
			new Thread(this).start();
		}
		
		@Override
		public void run() {
			try {
				handler.accept(dataFromSocketHandler.getRequest(), dataFromSocketHandler.getResponse()); // faccio la callback con la request e la response
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			dataFromSocketHandler.getResponse().close();
		}
	}
}

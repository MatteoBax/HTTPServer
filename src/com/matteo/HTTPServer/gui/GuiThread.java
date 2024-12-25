package com.matteo.HTTPServer.gui;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import com.matteo.HTTPServer.enums.LogType;
import com.matteo.HTTPServer.server.Server;
import com.matteo.HTTPServer.server.Session;
import com.matteo.HTTPServer.server.SessionVariable;

/**
 * Classe del thread che gestisce la GUI
 * 
 * @author Matteo Basso
 */
public class GuiThread implements Runnable {
	private GuiVars guiVars = new GuiVars(); // variabili condivise tra la gui
	
	/**
	 * Avvia il thread della GUI
	 */
	public GuiThread() {
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		Server server = new Server();
        guiVars.setServer(server);
        
		HandleLogThread normalLogThread = null;
		HandleLogThread errorLogThread = null;
		Gui gui = new Gui(guiVars);
		
		/*
		ConfigJPanel configJPanel = new ConfigJPanel(guiVars);
		
		gui.add(configJPanel);
		gui.setSize(configJPanel.getSize());
		
		gui.center();
        String port = guiVars.getPort();
        String documentRoot = guiVars.getDocumentRoot();
        
        gui.remove(configJPanel);
        */
                
		LogJPanel logJPanel = new LogJPanel(guiVars);
		gui.add(logJPanel);
		gui.pack();
		gui.center();
		
		// STDOUT
		PipedInputStream inForSTDOUT = new PipedInputStream();
		PipedOutputStream outForSTDOUT = null;
		try {
			outForSTDOUT = new PipedOutputStream(inForSTDOUT); // fa il pipe dell'output stream nell'input stream
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(outForSTDOUT != null) {
			PrintStream writer = new PrintStream(outForSTDOUT);
			System.setOut(writer);
			normalLogThread = new HandleLogThread(logJPanel, inForSTDOUT, LogType.NORMAL);
		}
		
		// STDERR
		PipedInputStream inForSTDERR = new PipedInputStream();
		PipedOutputStream outForSTDERR = null;
		try {
			outForSTDERR = new PipedOutputStream(inForSTDERR);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(outForSTDERR != null) {
			PrintStream writer = new PrintStream(outForSTDERR);
			System.setErr(writer);
			errorLogThread = new HandleLogThread(logJPanel, inForSTDERR, LogType.ERROR);
		}
		
		//Server server = new Server(Integer.parseInt(port), documentRoot, "D:\\xampp\\php\\php.exe");
		
		server.post("/api/login", (req, res) -> {
			Session session = req.getSession();
			session.start();
			session.addSessionVariable(new SessionVariable("username", req.getRequestParamValue("username")));
			session.addSessionVariable(new SessionVariable("password", req.getRequestParamValue("password")));
			res.send("Username: " + req.getRequestParamValue("username") + "\nPassword: " + req.getRequestParamValue("password"));
			res.close();
		});
		
		server.get("/api/showData", (req, res) -> {
			Session session = req.getSession();
			res.send("Username: " + session.getSessionVariable("username").getValue() + "\nPassword: " + session.getSessionVariable("password").getValue());
			res.close();
		});

		server.get("/api/ciao", (req, res) -> {
			res.send("CIAO").close();
		});
		
		//gui.setSize(logJPanel.getSize());
		
        try {
			server.startServer();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

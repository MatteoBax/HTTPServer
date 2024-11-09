package com.matteo.HTTPServer;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.matteo.HTTPServer.gui.GuiThread;

public class App {
	public static void main(String[] args) {
		new GuiThread();
	}
}

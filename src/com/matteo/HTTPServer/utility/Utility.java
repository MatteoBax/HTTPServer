package com.matteo.HTTPServer.utility;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.overviewproject.mime_types.MimeTypeDetector;

import com.matteo.HTTPServer.server.HTTPVersion;

/**
 * Questa classe contiene delle utility
 * 
 * @author Matteo Basso
 */
public class Utility {
	
	private static Tika tika = new Tika();
	private static MimeTypeDetector detector = new MimeTypeDetector();
	
	/**
	 * Questo methodo fa una scansione di una directory in maniera ricorsiva e restituisce tutti i file all'interno della directory
	 * 
	 * @param path il percorso della directory
	 * @return un array di stringhe contenente i percorsi dei file
	 */
	public static String[] listAllFiles(File path) {
		ArrayList<String> filesAndDirs = new ArrayList<String>();
		ArrayList<String> normalizedPath = new ArrayList<String>();
		
		if(path.list() != null) {
			for(String s : path.list()) {
				String p = path.getAbsolutePath() + File.separator + s;
				normalizedPath.add(p);
			}
		}
		
		
		filesAndDirs.addAll(normalizedPath);
		
		for(String s : normalizedPath) {
			File f = new File(s);
			if(f.exists() && f.isDirectory()) {
				filesAndDirs.addAll(Arrays.asList(listAllFiles(f)));
			}
		}
		ArrayList<String> finalArr = new ArrayList<String>();
		for(String s : filesAndDirs) {
			if(!new File(s).isDirectory()) {
				finalArr.add(s);
			}
		}
		return finalArr.toArray(new String[finalArr.size()]);
	}
	
	/**
	 * Questo metodo verifica se una porta è libera o occupata
	 * 
	 * @param port la porta
	 * @return <b>true</b> se la porta è libera, <b>false</b> se la porta è occupata
	 */
	public static boolean isPortAvailable(int port) {
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			return false;
		}
		
		try {
			server.close();
		} catch (IOException e) {}
		
		return true;
		
	}
	
	public static String generateSecureRandomString(int length) {
		if(length > 0) {
			SecureRandom secureRandom = new SecureRandom();
			StringBuilder sb = new StringBuilder(length);
			for(int i = 0; i < length; i++) {
				sb.append((char)((secureRandom.nextDouble() * (127 - 33)) + 33));
			}
			return sb.toString();
		} else {
			return "";
		}
		
	}
	
	public static String generateSecureRandomString(int length, char[] toExclude) {
		if(length > 0) {
			SecureRandom secureRandom = new SecureRandom();
			StringBuilder sb = new StringBuilder(length);
			for(int i = 0; i < length; i++) {
				char generatedChar;
				boolean found;
				do {
					generatedChar = (char)((secureRandom.nextDouble() * (127 - 33)) + 33); 
					found = false;
					for(char c : toExclude) {
						if(generatedChar == c) {
							found = true;
						}
					}
				} while (found);
				
				sb.append(generatedChar);
			}
			return sb.toString();
		} else {
			return "";
		}
		
	}
	
	/**
	 * Questo metodo restituisce una stringa contenente la data formattata in UTC
	 * @param date la data
	 * @return una stringa contenente la data formattata in UTC
	 */
	public static String formatDateAsUTCDateString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
	}
	
	public static String getFileExtensionFromMimeType(String mimeType) {
	    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
	    MimeType type;
		try {
			type = allTypes.forName(mimeType);
			return type.getExtension();
		} catch (MimeTypeException e) {
			
		}
	    return null;
	
	}
	
	public static String readLineFromBufferedInputStream(BufferedInputStream bi) throws IOException {
		StringBuilder sb = new StringBuilder();
        int c;
        boolean isNewLine = false;
        while ((c = bi.read()) != -1) {
        	
            if (isNewLine && c == '\n') {
                break;
            } else if (c == '\r') {
            	isNewLine = true;
            } else {
            	sb.append((char)c);
            }
            
        }
        return sb.toString();
	}
	
	// TODO sti metodi ci mettono troppo tempo
	public static String guessMimeTypeOfFile(File file) throws IOException {
	    //Tika tika = new Tika();
	    //return tika.detect(file);
		try {
			return detector.detectMimeType(file);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}
	
	public static String guessMimeTypeOfFile(InputStream inputStream) throws IOException {
	    //Tika tika = new Tika();
	    return tika.detect(inputStream);
	}
	
	public static long getInputStreamLength(InputStream inputStream) throws IOException {
        long length = 0;
        int readBytes = 0;
        byte[] buffer = new byte[8192];
        while ((readBytes = inputStream.read(buffer)) != -1) {
            length+=readBytes;
        }
        return length;
	}
	
	public static boolean deleteDirectory(File directoryToDelete) {
		File[] all = directoryToDelete.listFiles();
		if(all != null) {
			for(File f : all) {
				deleteDirectory(f);
			}
		}
		return directoryToDelete.delete();
	}
	
	/**
	 * Questo metodo verifica se il traffico in ingresso è una connessione HTTPS grezza (non gestita da {@link SSLServerSocket})
	 * 
	 * @param inputStream l'InputStream del socket
	 * @see <a href="https://en.wikipedia.org/wiki/Transport_Layer_Security#TLS_record">Record TLS</a>
	 * @return {@code true} se il traffico è HTTPS grezzo, {@code false} altrimenti
	 * @throws IOException nel caso si siano verificari errori di I/O di rete
	 */
	public static boolean isRawHTTPSTraffic(InputStream inputStream) throws IOException {
		/*
		 * Il byte 0 nel record TLS contiene il ContentType (Handshake, ChangeCipherSpec, ...)
		 * Il byte 1 nel record TLS contiene la versione minore del protocollo
		 */
		inputStream.mark(2); // marco 2 byte
		byte[] buffer = new byte[2];
        int bytesRead = inputStream.read(buffer, 0, 2);
        // il bitwise AND converte il byte in un unsigned int (in rete viaggiano unsigned)
        if (bytesRead == 2 && (buffer[0] & 0xFF) == 0x16 && (buffer[1] & 0xFF) >= 0x03) { // 0x16 = HandShake, 0x03 = versione major del protocollo (progressiva)
        	inputStream.reset(); // ritorno allo stato iniziale
            return true;
        } else {
        	inputStream.reset(); // ritorno allo stato iniziale
            return false;
        }
	}
}

package com.matteo.HTTPServer.gui;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JScrollPane;

/**
 * Classe che implementa una JTextPane con la JScrollPane
 * 
 * @author Matteo Basso
 */
public class JScrollTextPane extends JScrollPane {
	private JTextPane textPane;
	
	/**
	 * Costruttore
	 * 
	 * @param width la larghezza dalla JTextPane
	 * @param height l'altezza della JTextPane
	 * @param editable indica se il contenuto della JTextPane puè essere modificato o no
	 */
	public JScrollTextPane(int width, int height, boolean editable) {
		super();
		textPane = new JTextPane();
		textPane.setEditable(editable);
		textPane.setPreferredSize(new Dimension(width, height));
        
        // permette di scrollare automaticamente verso il basso
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
		setViewportView(textPane);
	}
	
	/**
	 * Costruttore (il contenuto della JTextPane è modificabile)
	 * 
	 * @param width la larghezza della JTextPane
	 * @param height l'altezza della JTextPane
	 */
	public JScrollTextPane(int width, int height) {
		this(width, height, true);
	}
	
	/**
	 * Cambia il testo della JTextPane
	 * 
	 * @param text la stringa contenente il nuovo testo
	 */
	public void setText(String text) {
		textPane.setText(text);
	}
	
	/**
	 * Restituisce il contenuto della JTextPane
	 * 
	 * @return una stringa contemente il contenuto della JTextPane
	 */
	public String getText() {
		return textPane.getText();
	}
	
	/**
	 * Aggiunge del testo con un colore specifico in append alla JTextPane
	 * 
	 * @param text la stringa contenente il testo
	 * @param color il colore che deve avere il testo
	 */
	public void append(String text, Color color) {
		StyledDocument doc = textPane.getStyledDocument();

        Style style = textPane.addStyle("Color Style", null);
        StyleConstants.setForeground(style, color);
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * Aggiunge del testo in append alla JTextPane. Il colore del testo è quello di default, cioè il nero.
	 * 
	 * @param text la stringa contenente il testo
	 */
	public void append(String text) {
		append(text, Color.BLACK);
	}
}

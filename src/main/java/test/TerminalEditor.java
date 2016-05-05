package test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.fusesource.jansi.Ansi.Color;

import static org.fusesource.jansi.Ansi.ansi;

import jline.console.ConsoleReader;
import jline.console.CursorBuffer;
import jline.console.KeyMap;

/**
 * An Editor in a terminal.
 * @author skip
 */
public class TerminalEditor implements Editor {
	ConsoleReader reader;
	public static final String KEY_LF = "\r";
	public static final String KEY_ENTER = "\n";
	public static final String KEY_UP = "\033[A";
	public static final String KEY_DOWN = "\033[B";
	ArrayList<String> lines;
	int currentLineIndex;

	private TerminalEditor() throws IOException {
		this(System.in, System.out);
	}

	public TerminalEditor(InputStream in, OutputStream out) throws IOException {
		reader = new ConsoleReader();
		reader.setExpandEvents(false);
		reader.setHandleUserInterrupt(true);
		reader.setBellEnabled(true);
		setPrompt("> ");
		registerKeyBindings();
		lines = new ArrayList<>();
		currentLineIndex = 0;
	}

	private void registerKeyBindings() {
		bind(KEY_LF, this::commandBreakLine);
		bind(KEY_ENTER, this::commandBreakLine);
		bind(KEY_UP, this::commandPreviousLine);
	}
	
	private void saveEditedLine() {
		CursorBuffer buff = reader.getCursorBuffer();

	}

	private void commandBreakLine(ActionEvent evt) {
		try {
			if (reader.getCursorBuffer().buffer.length() == 0) {
					reader.accept();
					return;
			}
			reader.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void commandPreviousLine(ActionEvent evt) {
		try {
			reader.println(ansi().cursorUp(1).reset().toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void bind(String keySeq, ActionListener action) {
		KeyMap keys = reader.getKeys();
		keys.bind(keySeq, action);
	}

	@Override
	public void setPrompt(String promptString) {
		reader.setPrompt(promptString);
	}

	@Override
	public String getInput() throws IOException {
		return reader.readLine();
	}

	public static void main(String[] args) throws IOException {
		System.out.println(ansi().fg(Color.CYAN).a("Hi").reset().a(" everyone!"));
		Editor ed = new TerminalEditor(); 
		ed.setPrompt("Balletie $ ");
		String input = ed.getInput();
		System.out.println("User typed in \"" + input + '"');
	}
}

package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import static org.fusesource.jansi.Ansi.ansi;

import jline.console.ConsoleReader;

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
	String prompt;
	String continuationPrompt;
	ArrayList<String> lines;

	public TerminalEditor() throws IOException {
		this(System.in, System.out);
	}

	public TerminalEditor(InputStream in, OutputStream out) throws IOException {
		reader = new ConsoleReader(in, out);
		reader.setExpandEvents(false);
		reader.setHandleUserInterrupt(true);
		reader.setBellEnabled(true);
		setPrompt(">>> ");
		setContinuationPrompt("... ");
		lines = new ArrayList<>();
	}
	
	private void saveLine(String lastLine) {
		lines.add(lastLine);
	}

	@Override
	public void setPrompt(String promptString) {
		prompt = promptString;
	}

	@Override
	public void setContinuationPrompt(String promptString) {
		continuationPrompt = promptString;
	}

	@Override
	public String getInput() throws IOException {
		String input;
		String lastLine;
		reader.setPrompt(prompt);
		// While the input is not empty, keep asking.
		while ((lastLine = reader.readLine()).trim().length() > 0) {
			reader.flush();
			reader.setPrompt(continuationPrompt);
			saveLine(lastLine);
		}
		// Concat the strings with newlines inbetween
		input = lines.stream().reduce("", (left, right) -> left + right + "\n");
		// Clear the lines for next input.
		lines.clear();
		return input;
	}

	public static void main(String[] args) throws IOException {
		System.out.println(ansi().a("Welcome to the ").bold().a("Spoofax").reset().a(" REPL"));
		Editor ed = new TerminalEditor(); 
		String input = ed.getInput();
		System.out.println("User typed in \"" + input + '"');
	}
}

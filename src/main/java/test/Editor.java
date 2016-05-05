package test;

import java.io.IOException;

/**
 * An Editor is where expressions in some language can be typed.
 * It takes care of the prompt, keybindings, the history and multiline editing capabilities.
 * @author skip
 */
public interface Editor {

	/**
	 * Set the REPL prompt.
	 * TODO: Should this just be in a {@link TerminalEditor}?
	 * @param promptString
	 */
	public void setPrompt(String promptString);

	/**
	 * Set the REPL prompt for a continued input on a new line.
	 * TODO: Should this just be in a {@link TerminalEditor}?
	 * @param promptString
	 */
	public void setContinuationPrompt(String promptString);

	/**
	 * Get the input from the user, optionally spanning multiple lines.
	 * @return The input typed in by the user.
	 * @throws IOException 
	 */
	public String getInput() throws IOException;

}

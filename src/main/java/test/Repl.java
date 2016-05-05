package test;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Test stdin to Spoofax.
 */
public final class Repl {
	private SpoofaxTest spoofax;
	private BufferedWriter out;
	private BufferedWriter err;
	private Editor editor;

	private Repl(SpoofaxTest spoofax, InputStream in, OutputStream out, OutputStream err) throws IOException {
		this.spoofax = spoofax;
		this.out = new BufferedWriter(new OutputStreamWriter(out));
		this.err = new BufferedWriter(new OutputStreamWriter(err));
		this.editor = new TerminalEditor(in, out);
		editor.setPrompt(coloredFg(Color.RED, "[In ]: "));
		editor.setContinuationPrompt("[...]: ");
	}

	private String coloredFg(Color c, String s) {
		return Ansi.ansi().fg(c).a(s).reset().toString();
	}

	private void write(BufferedWriter w, String output) {
		try {
			w.write(output);
			w.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void run() throws IOException {
		System.out.println(Ansi.ansi().a("Welcome to the ").bold().a("Spoofax").reset().a(" REPL"));
		String input;
		while (!(input = editor.getInput()).trim().equals("exit")) {
			if (input.length() == 0) {
				continue;
			}
			try {
				this.write(this.out, coloredFg(Color.GREEN, "[Out]: ") + this.spoofax.run(input) + "\n");
			} catch (IOException | MetaborgException e) {
				this.write(this.err, e.getMessage());
			}
		}
	}

	/**
	 * A simple test REPL.
	 * @param args <languagepath> <projectpath>.
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Usage: <languagepath> <projectpath>\n");
			return;
		}

		try (Spoofax spoofax = new Spoofax()) {
			SpoofaxTest test = new SpoofaxTest(spoofax, args[0], args[1]);
			new Repl(test, System.in, System.out, System.err).run();
		} catch (MetaborgException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

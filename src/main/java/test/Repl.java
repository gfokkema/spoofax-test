package test;

import org.metaborg.core.MetaborgException;
import org.metaborg.spoofax.core.Spoofax;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

/**
 * Test stdin to Spoofax.
 */
public final class Repl {
    private SpoofaxTest spoofax;
    private BufferedReader in;
    private BufferedWriter out;
    private BufferedWriter err;
    private String prompt;

    private Repl(SpoofaxTest spoofax, InputStream in, OutputStream out, OutputStream err) {
        this.spoofax = spoofax;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        this.err = new BufferedWriter(new OutputStreamWriter(err));
        this.prompt = "> ";
    }

    private String read() {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void write(BufferedWriter w, String output) {
        try {
            w.write(output);
            w.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        String input;

        while (true) {
            this.write(this.out, prompt);
            input = this.read();
            if (input == null) {
                return;
            } else if (input.length() == 0) {
				continue;
			}

            this.write(this.out, "I read: " + input + ". Handing to Spoofax...\n");
            try {
                this.spoofax.run(input);
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
        }
    }
}

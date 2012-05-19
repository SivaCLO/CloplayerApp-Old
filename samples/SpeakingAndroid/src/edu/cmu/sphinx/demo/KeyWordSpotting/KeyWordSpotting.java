/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.demo.KeyWordSpotting;

import java.io.IOException;
import java.net.URL;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.linguist.language.grammar.NoSkipGrammar;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class KeyWordSpotting {

	public static void main(String Args[]) throws IOException {
		ConfigurationManager cm = new ConfigurationManager("./src/config.xml");
		Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
		NoSkipGrammar grammar = (NoSkipGrammar) cm.lookup("NoSkipGrammar");
		//System.out.println("Key Word : quantum number");
		grammar.addKeyword("quantum number");
        grammar.addKeyword("orbital");
		// System.out.println(grammar);
		AudioFileDataSource dataSource = (AudioFileDataSource) cm
				.lookup("audioFileDataSource");
		dataSource.setAudioFile(new URL("file:./resource/wav/azimuthal.wav"),
				null);
		recognizer.allocate();
		Result result = recognizer.recognize();
		System.out.println(result.getTimedBestResult(false, true));

	}
}

import com.kotcrab.vis.usl.USL;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * The AUTHORITY half of USL's conformance oracle. Its twin is {@code Oracle.scala} beside it, and
 * {@code just usl-oracle-measure} runs both and diffs the transcripts line for line.
 *
 * <h2>Why this is a ZERO-AUTHORING gate</h2>
 * Every other behavioural gate in this corpus is either a ported upstream suite or a hand-written
 * probe somebody had to compose. This one is neither: upstream ships BOTH sides of the answer and
 * checks them into the repository.
 *
 * <ul>
 *   <li>the INPUTS are {@code usl/styles/*.usl} — the 19 skin templates VisUI's own root
 *       {@code build.gradle} compiles the shipped skin from;</li>
 *   <li>the EXPECTED OUTPUT is {@code ui/src/main/resources/com/kotcrab/vis/ui/skin/x1/uiskin.json},
 *       which is the artifact that build TASK wrote. {@code compileUsl} is four lines:
 *       {@code Lexer.addIncludeSource(styles)}, {@code USL.parse(null, "include <visui-" + version + ">")},
 *       and the result assigned to BOTH the {@code x1} and {@code x2} files.</li>
 * </ul>
 *
 * <h2>TWO facts about that oracle that a reading of the build file gets wrong</h2>
 * Both were measured by running this class against the upstream java before any Scala existed,
 * which is the only honest way to establish an authority:
 *
 * <ol>
 *   <li><b>there is ONE known-good output, not two.</b> {@code x1/uiskin.json} and
 *       {@code x2/uiskin.json} are BYTE-IDENTICAL, because {@code compileUsl} assigns one string to
 *       both — the x1/x2 split is about the texture ATLAS resolution, not about the JSON. Counting
 *       them as two independent oracles would double a piece of evidence that exists once.</li>
 *   <li><b>SEVEN of the 19 fixtures reproduce it, not one.</b> The checked-in skin was compiled
 *       from some version's template, and the templates from 1.4.5 onward are output-identical
 *       (200 lines / 15,340 bytes); the twelve older ones legitimately produce OLDER SKINS and are
 *       not failures. WHICH seven is DERIVED by this program on every run and never listed
 *       anywhere: the set follows upstream, so a template edit moves the number instead of
 *       silently invalidating a hard-coded list.</li>
 * </ol>
 *
 * <h2>Why no include source is registered</h2>
 * {@code Lexer.addIncludeSource} exists for {@code include <name>} directives, and NONE of the 19
 * fixtures uses one — re-derived by the lane, which greps the tree and fails if that stops being
 * true. That is what makes this gate offline and deterministic: upstream's own {@code RemoteTest}
 * is {@code @Ignore}d precisely because the include path downloads over HTTP.
 */
public class OracleJava {
	public static void main (String[] args) throws Exception {
		File stylesDir = new File(args[0]);
		String knownGood = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);

		File[] fixtures = stylesDir.listFiles((d, n) -> n.endsWith(".usl"));
		if (fixtures == null || fixtures.length == 0) {
			throw new IllegalStateException("no .usl fixtures under " + stylesDir);
		}
		// Sorted, so the transcript is a stable diff target: a directory listing's order is the
		// filesystem's and two machines need not agree about it.
		Arrays.sort(fixtures);

		System.out.println("fixtures: " + fixtures.length);
		for (File f : fixtures) {
			System.out.println("=== " + f.getName() + " ===");
			String out;
			try {
				out = USL.parse(f);
			} catch (Throwable t) {
				// A THROW is transcript content, not a harness failure: whether the port throws
				// where java throws is exactly what this gate is asking. The message is included
				// because USL's own exceptions carry the source position they failed at.
				//
				// THE SIMPLE NAME, NOT THE FQN, and that is CLAUDE.md §4.56 rather than brevity.
				// This transcript is diffed against one produced by the PORT, whose types live in
				// `sge.visui.usl` because the port renames them on purpose. Printing the qualified
				// name would make every throw a guaranteed diff — reporting the rename, which is
				// policy working, as a behavioural divergence. The simple name is the part the two
				// namespaces share, and it is the part that carries the behaviour.
				System.out.println("THREW " + t.getClass().getSimpleName() + ": "
					+ String.valueOf(t.getMessage()).replace('\n', ' '));
				continue;
			}
			System.out.println("known-good: " + (out.equals(knownGood) ? "EXACT" : "DIFFERS"));
			System.out.println(out);
		}
	}
}

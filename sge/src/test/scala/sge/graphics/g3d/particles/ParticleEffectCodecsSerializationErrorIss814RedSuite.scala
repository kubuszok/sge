// SGE — RED reproducer for ISS-814 (ParticleEffectCodecs raw UnsupportedOperationException).
//
// ParticleEffectCodecs signals JSON deserialization failures — a missing or unknown polymorphic
// "class" tag — with a raw java.lang.UnsupportedOperationException at 9 sites (:807-1711), e.g. the
// polymorphic Influencer decoder (ParticleEffectCodecs.scala:1262 "Influencer object missing 'class'
// field", :1267 "Unknown Influencer type: ..."). The original LibGDX g3d/particles code has NO such
// UnsupportedOperationException: its Json path raises com.badlogic.gdx.utils.SerializationException
// for malformed / unresolvable serialized data (a data/serialization error, not an "unsupported
// operation"). SGE's own error hierarchy already models this as sge.utils.SgeError.SerializationError.
//
// This suite pins TWO representative sites through the PUBLIC polymorphic Influencer codec
// (ParticleEffectCodecs.influencerCodec, wired through sge.utils.readFromString): decoding a JSON
// object with no "class" tag, and one with an unknown "class" tag, must signal the malformed data via
// sge.utils.SgeError (the project's own hierarchy — NOT a raw JDK exception), and specifically NOT via
// java.lang.UnsupportedOperationException (which means "operation not supported", a different
// semantic). It FAILS today because both sites throw a raw UnsupportedOperationException. Migrating the
// 9 sites to SgeError.SerializationError flips this green.
//
// Written by the reproducer agent; MUST NOT be modified by the fixer — it encodes the exception-type
// contract for the particle codec's serialization-error sites.

package sge
package graphics
package g3d
package particles

import munit.FunSuite
import sge.graphics.g3d.particles.influencers.Influencer
import sge.utils.{ SgeError, readFromString }
import ParticleEffectCodecs.given

class ParticleEffectCodecsSerializationErrorIss814RedSuite extends FunSuite {

  private def assertSerializationError(thrown: Throwable, site: String): Unit = {
    assert(
      !thrown.isInstanceOf[UnsupportedOperationException],
      s"$site: a malformed-serialized-data error must not be a raw java.lang.UnsupportedOperationException " +
        s"(the original g3d/particles Json path raises SerializationException, not UOE); got ${thrown.getClass.getName}: ${thrown.getMessage}"
    )
    assert(
      thrown.isInstanceOf[SgeError],
      s"$site: the error must be signaled through the project error hierarchy sge.utils.SgeError " +
        s"(target: SgeError.SerializationError); got ${thrown.getClass.getName}: ${thrown.getMessage}"
    )
  }

  test(
    "ISS-814 (RED): decoding an Influencer object with no 'class' tag signals SgeError, not raw UnsupportedOperationException (ParticleEffectCodecs.scala:1262)"
  ) {
    // A JSON object with no polymorphic "class" field — the decoder cannot resolve a concrete type.
    val thrown = intercept[Throwable](readFromString[Influencer]("""{"someField":1}"""))
    assertSerializationError(thrown, "Influencer missing 'class' (ParticleEffectCodecs.scala:1262)")
  }

  test(
    "ISS-814 (RED): decoding an Influencer object with an unknown 'class' tag signals SgeError, not raw UnsupportedOperationException (ParticleEffectCodecs.scala:1267)"
  ) {
    // A JSON object naming a class that is not in influencerTypes — unresolvable serialized data.
    val thrown = intercept[Throwable](readFromString[Influencer]("""{"class":"com.example.NoSuchInfluencer"}"""))
    assertSerializationError(thrown, "Influencer unknown class (ParticleEffectCodecs.scala:1267)")
  }
}

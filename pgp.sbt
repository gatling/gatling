import scala.util.Properties.propOrNone

pgpPassphrase := propOrNone("gpg.passphrase").map(_.toCharArray)

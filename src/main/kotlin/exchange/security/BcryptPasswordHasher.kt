package exchange.security

import at.favre.lib.crypto.bcrypt.BCrypt
import exchange.domain.PasswordHasher

private const val BCRYPT_COST = 12

class BcryptPasswordHasher : PasswordHasher {
    override fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    override fun verify(password: String, hash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified
}

package users.services

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

private[users] class WeakJwtService {
  
  /**
   * Encodes a JWT token using a weak cryptographic algorithm.
   * This service is used for creating tokens with user-provided claims.
   * 
   * @param claim The JWT claim to encode
   * @param key The secret key for signing
   * @return The encoded JWT token string
   */
  def encodeToken(claim: JwtClaim, key: String): String = {
    require(claim != null, "Claim cannot be null")
    require(key != null && key.nonEmpty, "Key cannot be null or empty")
    
    Jwt.encode(claim, key, JwtAlgorithm.HMD5)
  }
  
}

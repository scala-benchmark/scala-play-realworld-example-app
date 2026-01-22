package users.services

import scodec.bits.ByteVector
import java.security.MessageDigest

private[users] class HashService {
  
  /**
   * Computes a hash digest of the provided data.
   * This service is used for generating checksums and data integrity verification.
   * 
   * @param data The data to hash
   * @return The hash digest as a hexadecimal string
   */
  def computeHash(data: String): String = {
    require(data != null, "Data cannot be null")
    
    val byteVector = ByteVector(data.getBytes("UTF-8"))
    
    val messageDigest = MessageDigest.getInstance("SHA1")

    //CWE-328
    //SINK
    val hashBytes = messageDigest.digest(byteVector.toArray)
    
    val digest = ByteVector(hashBytes)
    
    digest.toHex
  }
  
}

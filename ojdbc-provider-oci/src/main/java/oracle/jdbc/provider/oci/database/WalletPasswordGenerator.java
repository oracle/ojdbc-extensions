/*
 ** Copyright (c) 2023 Oracle and/or its affiliates.
 **
 ** The Universal Permissive License (UPL), Version 1.0
 **
 ** Subject to the condition set forth below, permission is hereby granted to any
 ** person obtaining a copy of this software, associated documentation and/or data
 ** (collectively the "Software"), free of charge and under any and all copyright
 ** rights in the Software, and any and all patent rights owned or freely
 ** licensable by each licensor hereunder covering either (i) the unmodified
 ** Software as contributed to or provided by such licensor, or (ii) the Larger
 ** Works (as defined below), to deal in both
 **
 ** (a) the Software, and
 ** (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 ** one is included with the Software (each a "Larger Work" to which the Software
 ** is contributed by such licensors),
 **
 ** without restriction, including without limitation the rights to copy, create
 ** derivative works of, display, perform, and distribute the Software and make,
 ** use, sell, offer for sale, import, export, have made, and have sold the
 ** Software and the Larger Work(s), and to sublicense the foregoing rights on
 ** either these or other terms.
 **
 ** This license is subject to the following condition:
 ** The above copyright notice and either this complete permission notice or at
 ** a minimum a reference to the UPL must be included in all copies or
 ** substantial portions of the Software.
 **
 ** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 ** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 ** FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 ** AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 ** LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 ** OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 ** SOFTWARE.
 */

package oracle.jdbc.provider.oci.database;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A generator for passwords of Autonomous Database (ADB) wallets. The password
 * strength requirements are specified by
 * <a href="https://docs.oracle.com/en-us/iaas/api/#/en/database/20160918/datatypes/GenerateAutonomousDatabaseWalletDetails">
 * OCI documentation
 * </a>.
 */
final class WalletPasswordGenerator {

  private WalletPasswordGenerator(){}

  /** Lower case characters that may appear in a wallet password */
  private static final char[] LOWER_CASE_CHARACTERS =
    "abcdefghijklmnopqrstuvwxyz".toCharArray();

  /** Upper case characters that may appear in a wallet password */
  private static final char[] UPPER_CASE_CHARACTERS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  /** Numeric characters that may appear in a wallet password */
  private static final char[] NUMERIC_CHARACTERS =
    "0123456789".toCharArray();

  /** Special characters that may appear in a wallet password */
  private static final char[] SPECIAL_CHARACTERS =
    "!\"#$%&'*+,./:;=?@\\^`|~".toCharArray();

  /** All characters that may appear in a wallet password */
  private static final char[] ALL_CHARACTERS = Stream.of(
      LOWER_CASE_CHARACTERS,
      UPPER_CASE_CHARACTERS,
      NUMERIC_CHARACTERS,
      SPECIAL_CHARACTERS)
    .map(String::new)
    .collect(Collectors.joining())
    .toCharArray();

  /**
   * Generates a password to protect the contents of a wallet.
   */
  static char[] generatePassword() {
    final SecureRandom secureRandom;
    try {
      secureRandom = SecureRandom.getInstanceStrong();
    }
    catch (NoSuchAlgorithmException noSuchAlgorithmException) {
      throw new IllegalStateException(
        "Failed to create random number generator",
        noSuchAlgorithmException);
    }

    // Generating random letters at all indexes
    char[] password = new char[128];
    for (int i = 0; i < password.length; i++) {
      password[i] =
        ALL_CHARACTERS[secureRandom.nextInt(ALL_CHARACTERS.length)];
    }

    // Ensure there is at least one upper case character at a random index
    password[secureRandom.nextInt(password.length)] =
      UPPER_CASE_CHARACTERS[secureRandom.nextInt(UPPER_CASE_CHARACTERS.length)];

    // Ensure there is at least one lower case character at a random index
    password[secureRandom.nextInt(password.length)] =
      LOWER_CASE_CHARACTERS[secureRandom.nextInt(LOWER_CASE_CHARACTERS.length)];

    // Ensure there is at least one number character at a random index
    password[secureRandom.nextInt(password.length)] =
      NUMERIC_CHARACTERS[secureRandom.nextInt(NUMERIC_CHARACTERS.length)];

    // Ensure there is at least one special character at a random index
    password[secureRandom.nextInt(password.length)] =
      SPECIAL_CHARACTERS[secureRandom.nextInt(SPECIAL_CHARACTERS.length)];

    return password;
  }
}

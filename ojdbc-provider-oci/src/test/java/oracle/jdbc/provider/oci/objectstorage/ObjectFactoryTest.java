/*
 ** Copyright (c) 2026 Oracle and/or its affiliates.
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

package oracle.jdbc.provider.oci.objectstorage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies accepted and rejected URL formats for Object Storage URL parsing.
 */
public class ObjectFactoryTest {

  /**
   * Accepts a standard Object Storage URL.
   */
  @Test
  public void testClassicUrlAcceptedByParser() {
    assertDoesNotThrow(() -> new ObjectFactory.ObjectUrl(
      "https://objectstorage.us-ashburn-1.oraclecloud.com/n/namespace/b/bucket_name/o/object_name"));
  }

  /**
   *  Accepts a classic Object Storage URL that includes a PAR token segment.
   */
  @Test
  public void testClassicParUrlAcceptedByParser() {
    assertDoesNotThrow(() -> new ObjectFactory.ObjectUrl(
      "https://objectstorage.us-ashburn-1.oraclecloud.com/p/parToken1234567890/n/namespace/b/bucket_name/o/object_name"));
  }

  /**
   * Accepts the customer-OCI host format.
   */
  @Test
  public void testCustomerOciUrlAcceptedByParser() {
    assertDoesNotThrow(() -> new ObjectFactory.ObjectUrl(
      "https://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/n/namespace/b/bucket_name/o/object_name"));
  }

  /**
   * Accepts the customer-OCI host format with a PAR token segment.
   */
  @Test
  public void testCustomerOciParUrlAcceptedByParser() {
    assertDoesNotThrow(() -> new ObjectFactory.ObjectUrl(
      "https://namespace.objectstorage.us-ashburn-1.oci.customer-oci.com/p/parToken1234567890/n/namespace/b/bucket_name/o/object_name"));
  }

  /**
   * Rejects URLs that do not match supported Object Storage path formats.
   */
  @Test
  public void testInvalidUrlRejectedByParser() {
    IllegalArgumentException exception = assertThrows(
      IllegalArgumentException.class,
      () -> new ObjectFactory.ObjectUrl(
        "https://example.com/not/object/storage/path"));

    assertTrue(exception.getMessage().contains("Fail to parse Object URL"));
  }
}

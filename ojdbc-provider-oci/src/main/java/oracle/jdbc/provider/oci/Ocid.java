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

package oracle.jdbc.provider.oci;

import com.oracle.bmc.Region;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a general Oracle Cloud ID (OCID) class, which contains an
 * ocid string as its content. It parses out a region from the provided
 * ocid string during construction.
 **/
public final class Ocid {

  /** The ocid string */
  private final String content;

  /** The parsed region extracted from the ocid string */
  private final Region region;

  /** The regular expression of ocid */
  private static final String REGEX = "ocid1\\.[^.]+\\.[^.]+\\.([^.]*)\\..+";

  /** The pattern of ocid */
  private static final Pattern PATTERN = Pattern.compile(REGEX);

  public Ocid(String content) {
    this.content = content;
    this.region = parseRegion(content);
  }

  /**
   * Returns a {@link Region} which is parsed from the ocid string. If
   * the region part is empty. The method will return null instead of
   * throwing an exception.
   * The format of Oracle Cloud ID (OCID) is documented as follows:
   * <pre>
   * ocid1.<RESOURCE TYPE>.<REALM>.[REGION][.FUTURE USE].<UNIQUE ID>
   * </pre>
   * @see <a href="https://docs.oracle.com/en-us/iaas/Content/General/Concepts/identifiers.htm">Resource Identifiers</a>
   * @return an {@code Region} which is extracted from the ocid string
   **/
  private static Region parseRegion(String content) {
    Matcher matcher = PATTERN.matcher(content);
    if (matcher.matches()) {
      String regionCode = matcher.group(1);
      if (regionCode.equals("")) {
        return null;
      }
      return Region.fromRegionCode(matcher.group(1));
    }
    throw new IllegalStateException(
      "Fail to parse region from the OCID: " + content);
  }

  public Region getRegion() {
    return region;
  }

  public String getContent() {
    return content;
  }
}

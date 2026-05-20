/*
 *  Copyright (c) 2026 Oracle and/or its affiliates.
 *
 *  The Universal Permissive License (UPL), Version 1.0
 *
 *  Subject to the condition set forth below, permission is hereby granted to any
 *  person obtaining a copy of this software, associated documentation and/or data
 *  (collectively the "Software"), free of charge and under any and all copyright
 *  rights in the Software, and any and all patent rights owned or freely
 *  licensable by each licensor hereunder covering either (i) the unmodified
 *  Software as contributed to or provided by such licensor, or (ii) the Larger
 *  Works (as defined below), to deal in both
 *
 *  (a) the Software, and
 *  (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 *  one is included with the Software (each a "Larger Work" to which the Software
 *  is contributed by such licensors),
 *
 *  without restriction, including without limitation the rights to copy, create
 *  derivative works of, display, perform, and distribute the Software and make,
 *  use, sell, offer for sale, import, export, have made, and have sold the
 *  Software and the Larger Work(s), and to sublicense the foregoing rights on
 *  either these or other terms.
 *
 *  This license is subject to the following condition:
 *  The above copyright notice and either this complete permission notice or at
 *  a minimum a reference to the UPL must be included in all copies or
 *  substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package oracle.jdbc.provider.spring.resource;

import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonFactory;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContextAttributesUtilTest {

  @Test
  void parseJson_validNestedObjects_returnsMapOfOracleJsonObjects() {

    String json = "{\"ns1\":{\"k1\":\"v1\",\"k2\":\"v2\"},\"ns2\":{\"k3\":\"v3\"}}";

    Map<String, OracleJsonObject> result = ContextAttributesUtil.parseJson(json);

    assertNotNull(result, "result map should not be null");
    assertEquals(2, result.size(), "should contain two namespaces");

    assertTrue(result.containsKey("ns1"));
    OracleJsonObject ns1 = result.get("ns1");
    assertNotNull(ns1);
    assertEquals("v1", ns1.getString("k1"));
    assertEquals("v2", ns1.getString("k2"));

    assertTrue(result.containsKey("ns2"));
    OracleJsonObject ns2 = result.get("ns2");
    assertNotNull(ns2);
    assertEquals("v3", ns2.getString("k3"));
  }

  @Test
  void parseJson_nullOrBlank_returnsEmptyMap() {
    assertTrue(ContextAttributesUtil.parseJson(null).isEmpty(), "null should return empty map");
    assertTrue(ContextAttributesUtil.parseJson("  ").isEmpty(), "blank should return empty map");
    assertTrue(ContextAttributesUtil.parseJson("{}").isEmpty(), "{} should return empty map");
  }

  @Test
  void parseJson_nonObjectNamespace_isSkipped() {
    String json = "{\"ns1\":\"not-an-object\",\"ns2\":{\"k\":\"v\"}}";

    Map<String, OracleJsonObject> result = ContextAttributesUtil.parseJson(json);

    assertNotNull(result);
    assertEquals(1, result.size(), "non-object namespace should be skipped");
    assertFalse(result.containsKey("ns1"));
    assertTrue(result.containsKey("ns2"));
    assertEquals("v", result.get("ns2").getString("k"));
  }

  @Test
  void parseJson_invalidJson_throwsIllegalArgumentException() {
    String json = "not json";
    IllegalArgumentException ex = assertThrows(
      IllegalArgumentException.class,
      () -> ContextAttributesUtil.parseJson(json),
      "invalid JSON should throw IllegalArgumentException"
    );
    assertTrue(ex.getMessage().contains("Failed to parse"), ex.getMessage());
  }

  private static OracleJsonObject jsonObj(String json) {
    return new OracleJsonFactory()
        .createJsonTextValue(new StringReader(json))
        .asJsonObject();
  }

  @Test
  void merge_unionAndOverride_holderWinsPerKey() {
    Map<String, OracleJsonObject> paramAttrs = Map.of(
        "ns1", jsonObj("{\"p\":\"pv\"}"),
        "ns2", jsonObj("{\"fromParam\":\"pv\"}")
    );
    Map<String, OracleJsonObject> holderAttrs = Map.of(
        "ns2", jsonObj("{\"fromHolder\":\"hv\",\"fromParam\":\"pv-new\"}"),

        "ns3", jsonObj("{\"h\":\"hv3\"}")
    );

    Map<String, OracleJsonObject> merged = ContextAttributesUtil.merge(paramAttrs, holderAttrs);

    assertEquals(3, merged.size());
    assertEquals("pv", merged.get("ns1").getString("p"));
    assertTrue(merged.containsKey("ns2"));
    // union of keys for ns2
    assertEquals("pv-new", merged.get("ns2").getString("fromParam"));
    assertEquals("hv", merged.get("ns2").getString("fromHolder"));
    assertEquals("hv3", merged.get("ns3").getString("h"));
  }

  @Test
  void merge_sameKey_holderOverridesValue() {
    Map<String, OracleJsonObject> paramAttrs = Map.of(
        "ns2", jsonObj("{\"fromParam\":\"pv\"}")
    );
    Map<String, OracleJsonObject> holderAttrs = Map.of(
        "ns2", jsonObj("{\"fromParam\":\"hv\"}")
    );

    Map<String, OracleJsonObject> merged = ContextAttributesUtil.merge(paramAttrs, holderAttrs);

    assertEquals(1, merged.size());
    assertEquals("hv", merged.get("ns2").getString("fromParam"));
  }
}

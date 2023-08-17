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

package  oracle.jdbc.provider.parameter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static oracle.jdbc.provider.parameter.Parameter.CommonAttribute.SENSITIVE;
import static org.junit.jupiter.api.Assertions.*;

public class ParameterSetParserTest {

  private static final Parameter<String> SENSITIVE_PARAMETER =
    Parameter.create(SENSITIVE);

  private static final Parameter<Integer> INTEGER_PARAMETER =
    Parameter.create();

  private static final Parameter<Duration> DURATION_PARAMETER =
    Parameter.create();

  @Test
  public void testGet() {

    Map<String, String> namedValues = new HashMap<>();
    namedValues.put("x", "secret");
    namedValues.put("Y", "9");
    namedValues.put("z-duration", "99");

    ParameterSet parameterSet = parseTestParameters(namedValues);

    assertEquals("secret", parameterSet.getOptional(SENSITIVE_PARAMETER));
    assertEquals(9, parameterSet.getOptional(INTEGER_PARAMETER));
    assertEquals(Duration.ofMillis(99), parameterSet.getOptional(DURATION_PARAMETER));
  }

  @Test
  public void testGetRequired() {

    Map<String, String> namedValues = new HashMap<>();
    namedValues.put("x", "secret");
    // Expect default value, 0, of "Y" parameter
    // Expect IllegalStateException for missing "z-duration" parameter

    ParameterSet parameterSet = parseTestParameters(namedValues);

    assertEquals("secret", parameterSet.getRequired(SENSITIVE_PARAMETER));
    assertEquals(0, parameterSet.getRequired(INTEGER_PARAMETER));
    assertThrows(
      IllegalStateException.class,
      () -> parameterSet.getRequired(DURATION_PARAMETER));
  }

  @Test
  public void testToString() {
    Map<String, String> namedValues = new HashMap<>();
    namedValues.put("x", "secret");
    namedValues.put("Y", "9");
    namedValues.put("z-duration", "99");

    ParameterSet parameterSet = parseTestParameters(namedValues);

    // Verify that only non-sensitive values are not exposed by toString
    String parameterSetString = parameterSet.toString();
    assertNotNull(parameterSetString);
    assertFalse(parameterSetString.contains("secret"), parameterSetString);
    assertTrue(parameterSetString.contains("y=9"), parameterSetString);
    assertTrue(
      parameterSetString.contains("z-duration=PT0.099S"), parameterSetString);
  }

  private static ParameterSet parseTestParameters(
    Map<String, String> namedValues) {

    return ParameterSetParser.builder()
      .addParameter("x", SENSITIVE_PARAMETER)
      .addParameter("y", INTEGER_PARAMETER, 0, Integer::parseInt)
      .addParameter("z-duration", DURATION_PARAMETER,
        null,
        value -> Duration.ofMillis(Long.parseLong(value)))
      .build()
      .parseNamedValues(namedValues);
  }
}

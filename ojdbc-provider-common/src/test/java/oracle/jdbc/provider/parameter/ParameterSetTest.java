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

package oracle.jdbc.provider.parameter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies {@link ParameterSet} and {@link ParameterSetBuilder} as implementing
 * the behavior specified by their JavaDocs.
 */
public class ParameterSetTest {

  /**
   * Verifies behavior of a {@code ParameterSet} with a single parameter value
   */
  @Test
  public void testSingle() {
    Parameter<String> testParameter = Parameter.create();
    ParameterSet parameterSet =
      ParameterSet.builder()
        .add("test", testParameter, "a")
        .build();

    assertTrue(parameterSet.contains(testParameter));
    assertEquals("test", parameterSet.getName(testParameter));
    assertEquals("a", parameterSet.getOptional(testParameter));
    assertEquals("a", parameterSet.getRequired(testParameter));
  }

  /**
   * Verifies behavior of a {@code ParameterSet} with multiple parameter values
   */
  @Test
  public void testMultiple() {
    Parameter<String> testParameterA = Parameter.create();
    Parameter<String> testParameterB = Parameter.create();

    ParameterSet parameterSet =
      ParameterSet.builder()
        .add("testA", testParameterA, "a")
        .add("testB", testParameterB, "b")
        .build();

    assertTrue(parameterSet.contains(testParameterA));
    assertEquals("testA", parameterSet.getName(testParameterA));
    assertEquals("a", parameterSet.getOptional(testParameterA));
    assertEquals("a", parameterSet.getRequired(testParameterA));

    assertEquals("testB", parameterSet.getName(testParameterB));
    assertEquals("b", parameterSet.getOptional(testParameterB));
    assertEquals("b", parameterSet.getRequired(testParameterB));
    assertTrue(parameterSet.contains(testParameterB));
  }

  /**
   * Verifies behavior of {@link ParameterSet#getRequired(Parameter)}
   */
  @Test
  public void testGetRequired() {
    Parameter<String> testParameterB = Parameter.create();

    ParameterSet parameterSet =
      ParameterSet.builder()
        .add("testB", testParameterB, "b")
        .build();

    assertEquals("testB", parameterSet.getName(testParameterB));
    assertEquals("b", parameterSet.getOptional(testParameterB));
    assertEquals("b", parameterSet.getRequired(testParameterB));
    assertTrue(parameterSet.contains(testParameterB));

    Parameter<String> testParameterA = Parameter.create();
    assertFalse(parameterSet.contains(testParameterA));
    assertNull(parameterSet.getName(testParameterA));
    assertNull(parameterSet.getOptional(testParameterA));
    assertThrows(
      IllegalStateException.class,
      () -> parameterSet.getRequired(testParameterA));
  }

  /**
   * Verifies behavior of {@link ParameterSet#copyBuilder()}
   */
  @Test
  public void testCopyBuilder() {
    Parameter<String> testParameterB = Parameter.create();

    ParameterSet parameterSet =
      ParameterSet.builder()
        .add("testB", testParameterB, "b")
        .build();

    assertEquals("testB", parameterSet.getName(testParameterB));
    assertEquals("b", parameterSet.getOptional(testParameterB));
    assertEquals("b", parameterSet.getRequired(testParameterB));
    assertTrue(parameterSet.contains(testParameterB));

    Parameter<String> testParameterA = Parameter.create();
    assertFalse(parameterSet.contains(testParameterA));
    assertNull(parameterSet.getName(testParameterA));
    assertNull(parameterSet.getOptional(testParameterA));
    assertThrows(
      IllegalStateException.class,
      () -> parameterSet.getRequired(testParameterA));

    ParameterSet copiedParameterSet =
      parameterSet.copyBuilder()
        .add("testA", testParameterA, "a")
        .build();

    assertTrue(copiedParameterSet.contains(testParameterA));
    assertEquals("testA", copiedParameterSet.getName(testParameterA));
    assertEquals("a", copiedParameterSet.getOptional(testParameterA));
    assertEquals("a", copiedParameterSet.getRequired(testParameterA));

    assertEquals("testB", copiedParameterSet.getName(testParameterB));
    assertEquals("b", copiedParameterSet.getOptional(testParameterB));
    assertEquals("b", copiedParameterSet.getRequired(testParameterB));
    assertTrue(copiedParameterSet.contains(testParameterB));

    // Verify that the original ParameterSet is not modified
    assertEquals("testB", parameterSet.getName(testParameterB));
    assertEquals("b", parameterSet.getOptional(testParameterB));
    assertEquals("b", parameterSet.getRequired(testParameterB));
    assertTrue(parameterSet.contains(testParameterB));

    assertFalse(parameterSet.contains(testParameterA));
    assertNull(parameterSet.getName(testParameterA));
    assertNull(parameterSet.getOptional(testParameterA));
    assertThrows(
      IllegalStateException.class,
      () -> parameterSet.getRequired(testParameterA));
  }

}

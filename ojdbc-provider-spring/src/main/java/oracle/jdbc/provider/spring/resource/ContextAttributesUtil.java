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

import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonObject;
import oracle.sql.json.OracleJsonValue;

import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility functions for parsing and merging namespace attributes represented as:
 * {@code Map<String, OracleJsonObject>}.
 */
public final class ContextAttributesUtil {

    private ContextAttributesUtil() {}

    /**
     * Parse JSON using oracle.sql.json.
     * This is used to set the namespace attributes from configuration.
     * Expected shape:
     * {"ns1":{"k1":"v1","k2":"v2"},"ns2":{"k3":"v3"}}
     *
     * - Keys at the top level are namespace names.
     * - Values are JSON objects; the raw OracleJsonObject is preserved.
     * - Returns empty map on invalid input or parse errors.
     *
     * @param json a JSON object string describing namespace attributes; may be null or blank
     * @return a map of namespace -> OracleJsonObject; empty map if input is null/blank/invalid
     */
    public static Map<String, OracleJsonObject> parseJson(String json) {
        if (json == null) return Collections.emptyMap();
        String trimmed = json.trim();
        if (trimmed.isEmpty() || "{}".equals(trimmed)) return Collections.emptyMap();

        try {
            OracleJsonFactory factory = new OracleJsonFactory();
            OracleJsonValue rootValue = factory.createJsonTextValue(new StringReader(trimmed));
            OracleJsonObject rootObject = rootValue.asJsonObject();
            if (rootObject == null || rootObject.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<String, OracleJsonObject> result = new HashMap<>();

            for (String namespace : rootObject.keySet()) {
                OracleJsonValue namespaceValue = rootObject.get(namespace);
                if (namespaceValue == null) {
                    continue;
                }
                OracleJsonObject namespaceObject;
                try {
                    namespaceObject = namespaceValue.asJsonObject();
                } catch (ClassCastException cce) {
                    // Non-object value: skip this namespace
                    continue;
                }
                if (namespaceObject == null || namespaceObject.isEmpty()) {
                    continue;
                }
                // Deep copy the inner object so callers can safely mutate their own copies
                OracleJsonObject copy =
                    factory.createJsonTextValue(new StringReader(namespaceObject.toString())).asJsonObject();
                result.put(namespace, copy);
            }
            return result;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Failed to parse the namespace values from configuration", e);
        }
    }

     /**
      * Merge parameter-provided attributes with holder-provided attributes.
      * Per-namespace merge: combine keys; if a key exists in both, the holder value overrides (key-level right-wins).
      * Returns a deep-copied merged map (mutable).
     *
     * @param paramAttrs attributes provided via parameters/configuration (may be null or empty)
     * @param holderAttrs attributes from the holder (may be null or empty); wins per namespace
     * @return merged attributes where holder namespaces replace parameter namespaces; never null
     */
    public static Map<String, OracleJsonObject> merge(
            Map<String, OracleJsonObject> paramAttrs,
            Map<String, OracleJsonObject> holderAttrs) {

        // Strategy requested:
        // 1) Map-level: copy param namespaces then overwrite with holder namespaces (holder wins per namespace)
        // 2) For namespaces present in BOTH, merge JSON objects: param putAll then holder putAll (holder wins per key)
        Map<String, OracleJsonObject> merged = new HashMap<>();
        if (paramAttrs != null && !paramAttrs.isEmpty()) {
            merged.putAll(paramAttrs);
        }
        if (holderAttrs != null && !holderAttrs.isEmpty()) {
            merged.putAll(holderAttrs);
        }

        if (paramAttrs != null && holderAttrs != null && !paramAttrs.isEmpty() && !holderAttrs.isEmpty()) {
            for (String ns : paramAttrs.keySet()) {
                if (holderAttrs.containsKey(ns)) {
                    OracleJsonObject p = paramAttrs.get(ns);
                    OracleJsonObject h = holderAttrs.get(ns);
                    OracleJsonObject combined = new OracleJsonFactory().createObject();
                    if (p != null) {
                        combined.putAll(p);
                    }
                    if (h != null) {
                        combined.putAll(h); // holder wins for duplicate keys
                    }
                    merged.put(ns, combined);
                }
            }
        }
        return merged;
    }
}

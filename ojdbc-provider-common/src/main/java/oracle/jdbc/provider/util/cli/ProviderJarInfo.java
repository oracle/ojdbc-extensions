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
 ** The above copyright notice and either this complete permission notice or
 ** at a minimum a reference to the UPL must be included in all copies or
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

package oracle.jdbc.provider.util.cli;

/**
 * Gives every provider jar a friendly {@code java -jar <jar-file>} entry
 * point. Run with no arguments, it just introduces the provider name,
 * version, a short description, and a link to the README and exits
 * right away, so nothing ever sits there waiting on input that may
 * never come. Pass {@value #SETUP_FLAG} to ask for more; a subclass can
 * override {@link #onSetupRequested()} to launch an interactive setup
 * wizard there instead of just repeating the introduction.
 */
public abstract class ProviderJarInfo {

  /**
   * Command-line flag recognized by {@link #start(String[])}.
   */
  protected static final String SETUP_FLAG = "--setup";

  /**
   * The name of the provider, printed in the banner when the jar is run.
   */
  protected abstract String displayName();

  /**
   * A short description of the provider, printed in the banner.
   */
  protected abstract String description();

  /**
   * README URL printed in the banner.
   */
  protected abstract String readmeUrl();

  /**
   * Entry point called from a subclass's own {@code main(String[])}
   * method. Prints a banner if {@value #SETUP_FLAG} is absent from
   * {@code args}; otherwise calls {@link #onSetupRequested()}.
   *
   * @param args Arguments passed to {@code main(String[])}.
   */
  protected final void start(String[] args) {
    if (containsSetupFlag(args)) {
      onSetupRequested();
    }
    else {
      printInfo();
    }
  }

  /**
   * Called by {@link #start(String[])} when {@value #SETUP_FLAG} is
   * present. The default implementation just prints the banner. A
   * subclass can override this to do something else instead, in which
   * case it takes responsibility for printing its own banner, if any.
   */
  protected void onSetupRequested() {
    printInfo();
    System.out.println();
    System.out.println(
      "This module has no interactive setup helper. See the README "
        + "above for configuration examples.");
  }

  /**
   * Prints a short banner (name, version, description, and URL), without
   * reading any input. A subclass with more to print here should override
   * this and call {@code super.printInfo()} first.
   */
  protected void printInfo() {
    System.out.println();
    System.out.println(displayName() + " " + version());
    System.out.println(description());
    System.out.println();
    System.out.println("More information: " + readmeUrl());
  }

  private static boolean containsSetupFlag(String[] args) {
    for (String arg : args) {
      if (SETUP_FLAG.equals(arg)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return The jar's version from its manifest (eg: "1.1.0"), or
   * "unknown" when not running from a built jar (eg: from an IDE).
   */
  protected final String version() {
    String version = getClass().getPackage().getImplementationVersion();
    return version != null ? version : "unknown";
  }
}
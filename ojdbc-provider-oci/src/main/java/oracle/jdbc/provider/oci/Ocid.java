package oracle.jdbc.provider.oci;

import com.oracle.bmc.Region;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a general OCID class, which contains an ocid string as its
 * content. It parses out a region from the provided ocid string during
 * construction.
 *
 * Fields:
 * - String content: The full ocid as a string.
 * - Region region: The parsed region extracted from the ocid string.
 **/
public class Ocid {
  private String content;
  private Region region;

  public Ocid(String content) {
    this.content = content;
    this.region = parseRegion();
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
  private Region parseRegion(){
    String regex = "ocid1\\.[^.]+\\.[^.]+\\.([^.]*)\\..+";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(content);
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

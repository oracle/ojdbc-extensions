package oracle.jdbc.provider.oson.sample.model;

import java.util.Objects;

public class Image {
  public String location;
  public String description;

  public Image() {}
  public Image (String location, String description) {
    this.description = description;
    this.location = location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Image image = (Image) o;
    return location.equals(image.location) && description.equals(image.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(location, description);
  }
}
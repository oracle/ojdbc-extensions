package oracle.jdbc.provider.oson.sample.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Movie {
  @JsonProperty("_id")
  public int id;
  public String title;
  public String genre;
  public BigDecimal gross;
  public OffsetDateTime released;
  public List<Image> image;

  public Movie() {}
  public Movie(int id, String title, String genre, BigDecimal gross, OffsetDateTime released, List<Image> image) {
    this.id = id;
    this.title = title;
    this.genre = genre;
    this.gross = gross;
    this.released = released;
    this.image = image;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Movie movie = (Movie) o;
    return id == movie.id
        && title.equals(movie.title)
        && genre.equals(movie.genre)
        && gross.compareTo(movie.gross) == 0
        && released.isEqual(movie.released)
        && image.equals(movie.image);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, title, genre, gross, released, image);
  }
}



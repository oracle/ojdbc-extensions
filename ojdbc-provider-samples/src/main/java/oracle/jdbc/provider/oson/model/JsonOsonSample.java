package oracle.jdbc.provider.oson.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "JsonOsonSample")
@Table(name = "jackson_oson_sample")
public class JsonOsonSample {

  @Id
  private int id;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name="json_value")
  private Emp emp;

  public int getId() {
    return id;
  }

  public Emp getEmp() {
    return emp;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setEmp(Emp emp) {
    this.emp = emp;
  }
}

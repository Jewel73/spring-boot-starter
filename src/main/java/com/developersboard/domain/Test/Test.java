package com.developersboard.domain.Test;

import com.developersboard.constant.user.UserConstants;
import com.developersboard.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;

@Entity
@Getter
@Setter
@Audited
@Table(name = "Test")
@ToString(callSuper = true)
public class Test extends BaseEntity<Long> implements Serializable {

  @Serial private static final long serialVersionUID = 7538542321562810251L;

  @Column(unique = true, nullable = false)
  @NotBlank(message = UserConstants.BLANK_USERNAME)
  @Size(min = 3, max = 50, message = UserConstants.USERNAME_SIZE)
  private String username;

  @Column(unique = true, nullable = false)
  @NotBlank(message = UserConstants.BLANK_EMAIL)
  @Email(message = UserConstants.INVALID_EMAIL)
  private String email;
}

package com.developersboard.rest.v1;

import com.developersboard.annotation.Loggable;
import com.developersboard.constant.AdminConstants;
import com.developersboard.constant.ErrorConstants;
import com.developersboard.constant.user.ProfileConstants;
import com.developersboard.constant.user.SignUpConstants;
import com.developersboard.constant.user.UserConstants;
import com.developersboard.dto.UserDto;
import com.developersboard.enums.OperationStatus;
import com.developersboard.enums.UserHistoryType;
import com.developersboard.service.mail.EmailService;
import com.developersboard.service.security.EncryptionService;
import com.developersboard.service.security.JwtService;
import com.developersboard.service.user.UserService;
import com.developersboard.util.UserUtils;
import com.developersboard.web.payload.request.SignUpRequest;
import com.developersboard.web.payload.response.UserResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * This class handles all rest calls for users.
 *
 * @author Eric Opoku
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(AdminConstants.API_V1_USERS_ROOT_URL)
public class UserRestApi {

  private final UserService userService;
  private final JwtService jwtService;
  private final EmailService emailService;
  private final EncryptionService encryptionService;

  private static final String AUTHORIZE =
      "isFullyAuthenticated() && hasRole(T(com.developersboard.enums.RoleType).ROLE_ADMIN)";

  /**
   * Performs a search for users based on the provided search criteria.
   *
   * @param page Allows for pagination of the search results.
   * @return The ResponseEntity containing the search results as a Page of users
   */
  @PreAuthorize(AUTHORIZE)
  @Loggable(ignoreResponseData = true)
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Page<UserResponse>> getUsers(final Pageable page) {

    Page<UserResponse> vehicles = userService.findAll(page);
    return ResponseEntity.ok(vehicles);
  }

  /**
   * Enables the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @PreAuthorize(AUTHORIZE)
  @PutMapping(value = "/{publicId}/enable")
  public ResponseEntity<OperationStatus> enableUser(@PathVariable String publicId) {
    var userDto = userService.enableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Disables the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @PreAuthorize(AUTHORIZE)
  @PutMapping(value = "/{publicId}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> disableUser(@PathVariable String publicId) {
    var userDto = userService.disableUser(publicId);

    return ResponseEntity.ok(
        Objects.isNull(userDto) ? OperationStatus.FAILURE : OperationStatus.SUCCESS);
  }

  /**
   * Deletes the user associated with the publicId.
   *
   * @param publicId the publicId
   * @return if the operation is success
   */
  @PreAuthorize(AUTHORIZE)
  @DeleteMapping(value = "/{publicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<OperationStatus> deleteUser(@PathVariable String publicId) {
    userService.deleteUser(publicId);

    return ResponseEntity.ok(OperationStatus.SUCCESS);
  }

  @Loggable
  @PostMapping
  @SecurityRequirements
  public ResponseEntity<String> createUser(@Valid @RequestBody SignUpRequest signUpRequest) {
    var userDto = UserUtils.convertToUserDto(signUpRequest);

    if (userService.existsByUsernameOrEmailAndEnabled(userDto.getUsername(), userDto.getEmail())) {
      LOG.warn(UserConstants.USERNAME_OR_EMAIL_EXISTS);
      return ResponseEntity.badRequest().body(UserConstants.USERNAME_OR_EMAIL_EXISTS);
    }

    var verificationToken = jwtService.generateJwtToken(userDto.getUsername());
    userDto.setVerificationToken(verificationToken);

    var savedUserDto = userService.createUser(userDto);

    var encryptedToken = encryptionService.encrypt(verificationToken);
    LOG.debug("Encrypted JWT token: {}", encryptedToken);
    var encodedToken = encryptionService.encode(encryptedToken);

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setFrom("jewelranar420@gmail.com");
    msg.setTo("md.jewel.message@gmail.com");
    msg.setText("Hello from Jewel, how are you");
    msg.setSubject("Hello World");

    emailService.sendMail(msg);

    emailService.sendAccountVerificationEmail(savedUserDto, encodedToken);
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{publicId}")
            .buildAndExpand(savedUserDto.getPublicId())
            .toUriString();

    return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.LOCATION, location).build();
  }

  /**
   * This mapping handles the continuation of sign up.
   *
   * @param token the token.
   * @param redirectAttributes the redirectAttributes
   * @return the view mapping for login.
   */
  @Loggable
  @SecurityRequirements
  @PostMapping(SignUpConstants.SIGN_UP_VERIFY_MAPPING)
  public String completeSignUp(@RequestParam String token, RedirectAttributes redirectAttributes) {
    var decodedToken = encryptionService.decode(token);
    var verificationToken = encryptionService.decrypt(decodedToken);

    var userDto = validateTokenAndUpdateUser(verificationToken, redirectAttributes);
    if (Objects.nonNull(userDto) && !redirectAttributes.containsAttribute(ErrorConstants.ERROR)) {

      // send an account confirmation to the user.
      emailService.sendAccountConfirmationEmail(userDto);

      return ProfileConstants.REDIRECT_TO_PROFILE;
    }

    return SignUpConstants.SIGN_UP_VIEW_NAME;
  }

  /**
   * Update the user at this point then send an email after an update if the token is valid.
   *
   * @param token the token
   * @return the user dto
   */
  private UserDto validateTokenAndUpdateUser(final String token, final Model model) {
    if (!jwtService.isValidJwtToken(token)) {
      LOG.debug(ErrorConstants.INVALID_TOKEN);
      model.addAttribute(ErrorConstants.ERROR, ErrorConstants.INVALID_TOKEN);
      return null;
    }

    var username = jwtService.getUsernameFromToken(token);
    var userDto = userService.findByUsername(username);

    if (Objects.isNull(userDto) || !token.equals(userDto.getVerificationToken())) {
      LOG.debug(ErrorConstants.INVALID_TOKEN);
      model.addAttribute(ErrorConstants.ERROR, ErrorConstants.INVALID_TOKEN);
      return null;
    }

    if (userDto.getUsername().equals(username) && userDto.isEnabled()) {
      LOG.debug(SignUpConstants.ACCOUNT_EXISTS);
      model.addAttribute(ErrorConstants.ERROR, SignUpConstants.ACCOUNT_EXISTS);
      return null;
    }

    if (userDto.getUsername().equals(username)) {
      UserUtils.enableUser(userDto);
      return userService.updateUser(userDto, UserHistoryType.VERIFIED);
    }

    return null;
  }
}

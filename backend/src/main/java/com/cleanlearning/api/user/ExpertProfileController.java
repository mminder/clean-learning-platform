package com.cleanlearning.api.user;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.user.request.ExpertProfileRequest;
import com.cleanlearning.api.user.response.ExpertProfileResponse;
import com.cleanlearning.application.user.GetExpertProfileUseCase;
import com.cleanlearning.application.user.RemoveExpertPhotoUseCase;
import com.cleanlearning.application.user.SaveExpertProfileUseCase;
import com.cleanlearning.application.user.UploadExpertPhotoUseCase;
import com.cleanlearning.domain.user.CurrentUser;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** UC-14. The routes have no id: the profile is always the current user's. */
@RestController
@RequestMapping(ExpertProfileController.PATH)
@PreAuthorize("hasRole('EXPERT')")
class ExpertProfileController {

  static final String PATH = "/api/expert/profile";

  private final GetExpertProfileUseCase getExpertProfile;

  private final SaveExpertProfileUseCase saveExpertProfile;

  private final UploadExpertPhotoUseCase uploadExpertPhoto;

  private final RemoveExpertPhotoUseCase removeExpertPhoto;

  ExpertProfileController(
      GetExpertProfileUseCase getExpertProfile,
      SaveExpertProfileUseCase saveExpertProfile,
      UploadExpertPhotoUseCase uploadExpertPhoto,
      RemoveExpertPhotoUseCase removeExpertPhoto) {
    this.getExpertProfile = getExpertProfile;
    this.saveExpertProfile = saveExpertProfile;
    this.uploadExpertPhoto = uploadExpertPhoto;
    this.removeExpertPhoto = removeExpertPhoto;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  ExpertProfileResponse getExpertProfile(@AuthenticationPrincipal CurrentUser currentUser) {
    return ExpertProfileResponse.from(getExpertProfile.invoke(currentUser));
  }

  @PutMapping
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status415
  ExpertProfileResponse saveExpertProfile(
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody ExpertProfileRequest request) {
    return ExpertProfileResponse.from(saveExpertProfile.invoke(currentUser, request.toCommand()));
  }

  /** A file over 2 MB stops at the multipart limit and answers 400, like a file of another type. */
  @PutMapping(path = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status415
  ExpertProfileResponse uploadExpertPhoto(
      @AuthenticationPrincipal CurrentUser currentUser, @RequestPart("file") MultipartFile file)
      throws IOException {
    return ExpertProfileResponse.from(uploadExpertPhoto.invoke(currentUser, file.getBytes()));
  }

  @DeleteMapping("/photo")
  @ResponseStatus(HttpStatus.OK)
  ExpertProfileResponse removeExpertPhoto(@AuthenticationPrincipal CurrentUser currentUser) {
    return ExpertProfileResponse.from(removeExpertPhoto.invoke(currentUser));
  }
}

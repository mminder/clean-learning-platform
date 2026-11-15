package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestClock;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * UC-14: an expert uploads, replaces and removes their own photo (D-21), and the shop, the expert
 * picker and the customer's case show it. {@link SeedData#DELETE_TEST_DATA} sets the seeded
 * profiles back to no photo and deletes the images afterwards.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ExpertPhotoTest {

  private static final String PATH = "/api/expert/profile/photo";

  private static final String CUSTOMER_CASES = "/api/customer/consultation-cases";

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @Autowired JdbcClient jdbc;

  @Autowired DataSource dataSource;

  private MvcTestResult upload(UUID expert, String fileName, String contentType, byte[] bytes) {
    return mockMvc
        .put()
        .uri(PATH)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .multipart()
        .file(new MockMultipartFile("file", fileName, contentType, bytes))
        .exchange();
  }

  private MvcTestResult uploadJpeg(UUID expert) {
    return upload(expert, "photo.jpg", "image/jpeg", TestImages.jpeg(100));
  }

  private MvcTestResult remove(UUID expert) {
    return mockMvc
        .delete()
        .uri(PATH)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .exchange();
  }

  private MvcTestResult profile(UUID expert) {
    return mockMvc
        .get()
        .uri("/api/expert/profile")
        .with(LoggedIn.as(expert, Role.EXPERT))
        .exchange();
  }

  private MvcTestResult image(String imageId) {
    return mockMvc.get().uri("/api/images/{id}", imageId).exchange();
  }

  private static @Nullable String photoId(MvcTestResult result) throws Exception {
    return JsonPath.read(result.getResponse().getContentAsString(), "$.photoId");
  }

  private @Nullable String storedPhotoId(UUID expert) {
    return jdbc.sql("select photo_id::text from expert_profile where user_id = ?")
        .param(expert)
        .query(String.class)
        .optional()
        .orElse(null);
  }

  private long images() {
    return jdbc.sql("select count(*) from image").query(Long.class).single();
  }

  /** The images that neither an offering nor a profile refers to. */
  private long imagesLeftBehind() {
    return jdbc.sql(
            """
            select count(*) from image i
            where not exists (select 1 from offering o where o.image_id = i.id)
              and not exists (select 1 from expert_profile p where p.photo_id = i.id)
            """)
        .query(Long.class)
        .single();
  }

  /** A 400 with this detail and no {@code errors}. */
  private static void assertBadRequest(MvcTestResult result, String detail) {
    assertThat(result)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "%s",
              "instance": "%s"
            }
            """
                .formatted(detail, PATH));
  }

  @Test
  void uploadsAPhotoThatTheShopShows() throws Exception {
    byte[] jpeg = TestImages.jpeg(100);

    MvcTestResult uploaded = upload(SeedData.EXPERT_2, "photo.jpg", "image/jpeg", jpeg);

    assertThat(uploaded).hasStatusOk();
    String photoId = photoId(uploaded);
    assertThat(image(photoId)).hasStatusOk().hasContentType("image/jpeg").body().isEqualTo(jpeg);
    MvcTestResult detail =
        mockMvc.get().uri("/api/shop/offerings/{id}", SeedData.CAREER_COACHING).exchange();
    assertThat(detail)
        .bodyJson()
        .extractingPath("$.consultation.eligibleExperts[1].photoId")
        .isEqualTo(photoId);
    assertThat(detail)
        .bodyJson()
        .extractingPath("$.consultation.eligibleExperts[0].photoId")
        .isNull();
  }

  @Test
  void theUploadAnswersTheProfileWithItsPhotoId() throws Exception {
    MvcTestResult uploaded = uploadJpeg(SeedData.EXPERT_2);

    String photoId = photoId(uploaded);
    assertThat(UUID.fromString(photoId)).isNotNull();
    assertThat(uploaded)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "displayName": "Expert 2",
              "bio": "Leadership coach and former engineering manager. Works with new team leads on their first year in the role.",
              "teamsLink": null,
              "photoId": "%s"
            }
            """
                .formatted(photoId));
    assertThat(profile(SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.photoId")
        .isEqualTo(photoId);
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isEqualTo(photoId);
  }

  @Test
  void savingTheProfileKeepsThePhoto() throws Exception {
    String photoId = photoId(uploadJpeg(SeedData.EXPERT_2));

    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/profile")
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "displayName": "Expert Two", "bio": "New bio", "teamsLink": "" }
                    """))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.photoId")
        .isEqualTo(photoId);
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isEqualTo(photoId);
  }

  @Test
  void acceptsPngAndWebp() throws Exception {
    assertThat(
            image(
                photoId(upload(SeedData.EXPERT_2, "photo.png", "image/png", TestImages.png(100)))))
        .hasContentType("image/png");
    assertThat(
            image(
                photoId(
                    upload(SeedData.EXPERT_2, "photo.webp", "image/webp", TestImages.webp(100)))))
        .hasContentType("image/webp");
  }

  @Test
  void aReplacedPhotoGetsANewIdAndTheOldOneIsDeleted() throws Exception {
    String oldPhotoId = photoId(uploadJpeg(SeedData.EXPERT_2));
    byte[] png = TestImages.png(200);

    MvcTestResult replaced = upload(SeedData.EXPERT_2, "photo.png", "image/png", png);

    assertThat(replaced).hasStatusOk();
    String newPhotoId = photoId(replaced);
    assertThat(newPhotoId).isNotEqualTo(oldPhotoId);
    assertThat(image(newPhotoId)).hasStatusOk().hasContentType("image/png").body().isEqualTo(png);
    assertThat(image(oldPhotoId)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(images()).isOne();
    assertThat(imagesLeftBehind()).isZero();
  }

  @Test
  void removesThePhoto() throws Exception {
    String photoId = photoId(uploadJpeg(SeedData.EXPERT_2));

    MvcTestResult removed = remove(SeedData.EXPERT_2);

    assertThat(removed).hasStatusOk().bodyJson().extractingPath("$.photoId").isNull();
    assertThat(removed).bodyJson().extractingPath("$.displayName").isEqualTo("Expert 2");
    assertThat(profile(SeedData.EXPERT_2)).bodyJson().extractingPath("$.photoId").isNull();
    assertThat(image(photoId)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", SeedData.CAREER_COACHING))
        .bodyJson()
        .extractingPath("$.consultation.eligibleExperts[1].photoId")
        .isNull();
    assertThat(images()).isZero();
  }

  @Test
  void removingWithoutAPhotoChangesNothing() {
    assertThat(remove(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.photoId")
        .isNull();
    assertThat(images()).isZero();
  }

  @Test
  void answersAnInvalidFileWithBadRequestAndKeepsThePhoto() throws Exception {
    String photoId = photoId(uploadJpeg(SeedData.EXPERT_2));
    String notAnImage = "An image is a JPEG, PNG or WebP file.";

    assertBadRequest(
        upload(SeedData.EXPERT_2, "photo.gif", "image/gif", TestImages.gif(100)), notAnImage);
    assertBadRequest(
        upload(
            SeedData.EXPERT_2,
            "photo.png",
            "image/png",
            "Not a picture.".getBytes(StandardCharsets.UTF_8)),
        notAnImage);
    assertBadRequest(
        upload(
            SeedData.EXPERT_2,
            "photo.jpg",
            "image/jpeg",
            TestImages.jpeg(TestImages.MAX_BYTES + 1)),
        "An image is at most 2 MB.");
    assertBadRequest(
        upload(SeedData.EXPERT_2, "photo.png", "image/png", new byte[0]), "An image is not empty.");

    assertThat(profile(SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.photoId")
        .isEqualTo(photoId);
    assertThat(image(photoId)).hasStatusOk().body().isEqualTo(TestImages.jpeg(100));
    assertThat(images()).isOne();
  }

  @Test
  void answersARequestWithoutAFileWithBadRequestAndABodyThatIsNotMultipartWith415() {
    assertThat(
            mockMvc
                .put()
                .uri(PATH)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .multipart()
                .file(
                    new MockMultipartFile("photo", "photo.png", "image/png", TestImages.png(100))))
        .hasStatus(HttpStatus.BAD_REQUEST);
    assertThat(
            mockMvc
                .put()
                .uri(PATH)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isNull();
    assertThat(images()).isZero();
  }

  @Test
  void eachExpertChangesOnlyTheirOwnPhoto() throws Exception {
    String expert2Photo = photoId(uploadJpeg(SeedData.EXPERT_2));

    String expert1Photo = photoId(uploadJpeg(SeedData.EXPERT_1));

    assertThat(expert1Photo).isNotEqualTo(expert2Photo);
    assertThat(storedPhotoId(SeedData.EXPERT_1)).isEqualTo(expert1Photo);
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isEqualTo(expert2Photo);

    assertThat(remove(SeedData.EXPERT_1)).hasStatusOk();

    assertThat(storedPhotoId(SeedData.EXPERT_1)).isNull();
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isEqualTo(expert2Photo);
    assertThat(image(expert2Photo)).hasStatusOk();
    assertThat(images()).isOne();
  }

  /**
   * Customer 1 bought Career Coaching and booked Expert 2: the expert picker and the case show the
   * photo, and none after its removal.
   */
  @Test
  void theExpertPickerAndTheCustomersCaseShowThePhoto() throws Exception {
    clock.set(NOW);
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "dates": ["2026-11-17"], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                    """))
        .hasStatusOk();
    MvcTestResult bought =
        mockMvc
            .post()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                { "offeringId": "%s", "packageId": "%s", "price": "450.00", "sessionCount": 3 }
                """
                    .formatted(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS))
            .exchange();
    assertThat(bought).hasStatus(HttpStatus.CREATED);
    String caseId = JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/appointments", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "expertId": "%s", "start": "2026-11-17T09:00:00Z" }
                    """
                        .formatted(SeedData.EXPERT_2)))
        .hasStatus(HttpStatus.CREATED);

    String photoId = photoId(uploadJpeg(SeedData.EXPERT_2));

    assertThat(eligibleExperts(caseId))
        .bodyJson()
        .extractingPath("$[1].photoId")
        .isEqualTo(photoId);
    assertThat(eligibleExperts(caseId)).bodyJson().extractingPath("$[0].photoId").isNull();
    assertThat(customerCase(caseId))
        .bodyJson()
        .extractingPath("$.expert.photoId")
        .isEqualTo(photoId);

    assertThat(remove(SeedData.EXPERT_2)).hasStatusOk();

    assertThat(eligibleExperts(caseId)).bodyJson().extractingPath("$[1].photoId").isNull();
    assertThat(customerCase(caseId)).bodyJson().extractingPath("$.expert.photoId").isNull();
  }

  private MvcTestResult eligibleExperts(String caseId) {
    return mockMvc
        .get()
        .uri(CUSTOMER_CASES + "/{id}/eligible-experts", caseId)
        .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
        .exchange();
  }

  private MvcTestResult customerCase(String caseId) {
    return mockMvc
        .get()
        .uri(CUSTOMER_CASES + "/{id}", caseId)
        .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
        .exchange();
  }

  /** The test cleanup sets both profiles back to no photo and deletes their images. */
  @Test
  void theTestCleanupRemovesThePhotos() {
    assertThat(uploadJpeg(SeedData.EXPERT_1)).hasStatusOk();
    assertThat(uploadJpeg(SeedData.EXPERT_2)).hasStatusOk();

    new ResourceDatabasePopulator(new ClassPathResource(SeedData.DELETE_TEST_DATA))
        .execute(dataSource);

    assertThat(storedPhotoId(SeedData.EXPERT_1)).isNull();
    assertThat(storedPhotoId(SeedData.EXPERT_2)).isNull();
    assertThat(images()).isZero();
  }
}

package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * UC-16 step 3: the admin uploads, replaces and removes an offering's image (D-21). A test creates its own offerings,
 * and {@link SeedData#DELETE_TEST_DATA} removes them with their images afterwards.
 */
@IntegrationTest
class ConsultationOfferingImageTest {

    private static final String PATH = "/api/admin/consultation-offerings";

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    JdbcClient jdbc;

    private String createDraft() throws Exception {
        MvcTestResult created = mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Agile Coaching",
                          "description": "Coaching for agile teams.",
                          "sessionDurationMinutes": 45,
                          "cancellationDeadlineHours": 48,
                          "packages": [{ "sessionCount": 1, "price": "160" }],
                          "eligibleExpertIds": ["%s"]
                        }
                        """.formatted(SeedData.EXPERT_2))
                .exchange();
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult upload(String id, String fileName, String contentType, byte[] bytes) {
        return mockMvc.put().uri(PATH + "/{id}/image", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .multipart()
                .file(new MockMultipartFile("file", fileName, contentType, bytes))
                .exchange();
    }

    private MvcTestResult remove(String id) {
        return mockMvc.delete().uri(PATH + "/{id}/image", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .exchange();
    }

    private MvcTestResult change(String id, String action) {
        return mockMvc.post().uri(PATH + "/{id}/" + action, id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .exchange();
    }

    private MvcTestResult get(String id) {
        return mockMvc.get().uri(PATH + "/{id}", id).with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)).exchange();
    }

    private MvcTestResult image(String imageId) {
        return mockMvc.get().uri("/api/images/{id}", imageId).exchange();
    }

    /** A draft with a PNG of 100 bytes; returns the offering's id. */
    private String createDraftWithImage() throws Exception {
        String id = createDraft();
        assertThat(upload(id, "agile.png", "image/png", TestImages.png(100))).hasStatusOk();
        return id;
    }

    private static String imageId(MvcTestResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.imageId");
    }

    private long images() {
        return jdbc.sql("select count(*) from image").query(Long.class).single();
    }

    /** A 400 with this detail and no {@code errors}. */
    private static void assertBadRequest(MvcTestResult result, String detail, String instance) {
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "%s",
                          "instance": "%s"
                        }
                        """.formatted(detail, instance));
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void uploadsAnImageThatIsServedWithItsContentType() throws Exception {
        String id = createDraft();
        byte[] png = TestImages.png(100);

        MvcTestResult uploaded = upload(id, "agile.png", "image/png", png);

        assertThat(uploaded).hasStatusOk();
        String imageId = imageId(uploaded);
        assertThat(mockMvc.get().uri("/api/images/{id}", imageId))
                .hasStatusOk()
                .hasContentType("image/png")
                .body().isEqualTo(png);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void theUploadAnswersTheOfferingWithItsImageId() throws Exception {
        String id = createDraft();

        MvcTestResult uploaded = upload(id, "agile.png", "image/png", TestImages.png(100));

        String imageId = imageId(uploaded);
        assertThat(UUID.fromString(imageId)).isNotNull();
        assertThat(uploaded).bodyJson().extractingPath("$.name").isEqualTo("Agile Coaching");
        assertThat(get(id)).bodyJson().extractingPath("$.imageId").isEqualTo(imageId);
        assertThat(get(id)).bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void theTypeComesFromTheFirstBytesNotFromTheNameOrTheDeclaredType() throws Exception {
        String id = createDraft();
        byte[] png = TestImages.png(100);

        String imageId = imageId(upload(id, "photo.jpg", "image/jpeg", png));

        assertThat(image(imageId)).hasContentType("image/png").body().isEqualTo(png);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void acceptsJpegAndWebp() throws Exception {
        String id = createDraft();

        assertThat(image(imageId(upload(id, "photo.jpg", "image/jpeg", TestImages.jpeg(100)))))
                .hasContentType("image/jpeg");
        assertThat(image(imageId(upload(id, "photo.webp", "image/webp", TestImages.webp(100)))))
                .hasContentType("image/webp");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void aReplacedImageGetsANewIdAndTheOldOneIsDeleted() throws Exception {
        String id = createDraft();
        String oldImageId = imageId(upload(id, "first.png", "image/png", TestImages.png(100)));
        long images = images();
        byte[] newImage = TestImages.jpeg(200);

        MvcTestResult replaced = upload(id, "second.jpg", "image/jpeg", newImage);

        assertThat(replaced).hasStatusOk();
        String newImageId = imageId(replaced);
        assertThat(newImageId).isNotEqualTo(oldImageId);
        assertThat(image(newImageId)).hasStatusOk().hasContentType("image/jpeg").body().isEqualTo(newImage);
        assertThat(image(oldImageId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(images()).as("the old image's row is gone").isEqualTo(images);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void removesTheImage() throws Exception {
        String id = createDraftWithImage();
        String imageId = imageId(get(id));

        MvcTestResult removed = remove(id);

        assertThat(removed).hasStatusOk().bodyJson().extractingPath("$.imageId").isNull();
        assertThat(get(id)).bodyJson().extractingPath("$.imageId").isNull();
        assertThat(image(imageId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(images()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void removingWithoutAnImageChangesNothing() throws Exception {
        String id = createDraft();

        assertThat(remove(id)).hasStatusOk().bodyJson().extractingPath("$.imageId").isNull();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void answersAnInvalidFileWithBadRequestAndKeepsTheImage() throws Exception {
        String id = createDraftWithImage();
        String imageId = imageId(get(id));
        String instance = PATH + "/" + id + "/image";
        String notAnImage = "An image is a JPEG, PNG or WebP file.";

        assertBadRequest(upload(id, "photo.gif", "image/gif", TestImages.gif(100)), notAnImage, instance);
        assertBadRequest(upload(id, "photo.png", "image/png", "Not a picture.".getBytes(StandardCharsets.UTF_8)),
                notAnImage, instance);
        assertBadRequest(upload(id, "photo.jpg", "image/jpeg", TestImages.jpeg(TestImages.MAX_BYTES + 1)),
                "An image is at most 2 MB.", instance);
        assertBadRequest(upload(id, "photo.png", "image/png", new byte[0]), "An image is not empty.", instance);

        assertThat(get(id)).bodyJson().extractingPath("$.imageId").isEqualTo(imageId);
        assertThat(image(imageId)).hasStatusOk().body().isEqualTo(TestImages.png(100));
        assertThat(images()).isOne();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void acceptsAJpegOfExactlyTwoMegabytes() throws Exception {
        String id = createDraft();
        byte[] jpeg = TestImages.jpeg(TestImages.MAX_BYTES);

        MvcTestResult uploaded = upload(id, "photo.jpg", "image/jpeg", jpeg);

        assertThat(uploaded).hasStatusOk();
        assertThat(image(imageId(uploaded))).hasStatusOk().body().isEqualTo(jpeg);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void answersARequestWithoutAFileWithBadRequest() throws Exception {
        String id = createDraft();

        assertThat(mockMvc.put().uri(PATH + "/{id}/image", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .multipart()
                .file(new MockMultipartFile("photo", "photo.png", "image/png", TestImages.png(100))))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mockMvc.put().uri(PATH + "/{id}/image", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(get(id)).bodyJson().extractingPath("$.imageId").isNull();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void anArchivedOfferingsImageCantBeChanged() throws Exception {
        String id = createDraftWithImage();
        String imageId = imageId(get(id));
        assertThat(change(id, "publish")).hasStatusOk();
        assertThat(change(id, "archive")).hasStatusOk();
        String archived = "The offering is archived and can't be changed any more.";

        for (MvcTestResult refused : new MvcTestResult[] {
                upload(id, "new.png", "image/png", TestImages.png(200)), remove(id)}) {
            assertThat(refused)
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "%s",
                              "instance": "%s",
                              "code": "WRONG_OFFERING_STATUS"
                            }
                            """.formatted(archived, PATH + "/" + id + "/image"));
        }

        assertThat(get(id)).bodyJson().extractingPath("$.imageId").isEqualTo(imageId);
        assertThat(image(imageId)).hasStatusOk();
        assertThat(images()).isOne();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void deletingADraftDeletesItsImage() throws Exception {
        String id = createDraftWithImage();
        String imageId = imageId(get(id));

        assertThat(mockMvc.delete().uri(PATH + "/{id}", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token()))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(image(imageId)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(images()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void theShopShowsAPublishedOfferingsImage() throws Exception {
        String id = createDraftWithImage();
        String imageId = imageId(get(id));
        assertThat(change(id, "publish")).hasStatusOk();

        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].imageId").asArray().containsExactly(imageId, null);
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", id))
                .bodyJson().extractingPath("$.imageId").isEqualTo(imageId);

        assertThat(remove(id)).hasStatusOk();

        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].imageId").asArray().containsExactly(null, null);
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", id))
                .bodyJson().extractingPath("$.imageId").isNull();
    }

    @Test
    void answersAnUnknownOfferingWithNotFound() {
        assertThat(upload(UNKNOWN_ID, "agile.png", "image/png", TestImages.png(100)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.detail").isEqualTo("Offering " + UNKNOWN_ID + " not found.");
        assertThat(remove(UNKNOWN_ID)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(images()).isZero();
    }
}

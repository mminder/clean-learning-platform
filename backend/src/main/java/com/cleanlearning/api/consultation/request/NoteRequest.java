package com.cleanlearning.api.consultation.request;

import com.cleanlearning.domain.consultation.value.NoteVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The annotations repeat UC-15's rules so a 400 names every invalid field; the value objects stay
 * the authority.
 */
public record NoteRequest(
    @NotBlank String title, @NotBlank String text, @NotNull NoteVisibility visibility) {}

/**
 * REST controllers, one sub-package per feature; they call use cases directly. Each feature keeps its response records
 * in {@code response} and its request records in {@code request}. Responses that several features return live in
 * {@code api.response}.
 */
package com.cleanlearning.api;

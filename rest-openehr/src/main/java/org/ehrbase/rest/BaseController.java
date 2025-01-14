/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest;

import com.google.common.annotations.VisibleForTesting;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.rest.openehr.format.CompositionRepresentation;
import org.ehrbase.rest.openehr.format.OpenEHRMediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

/**
 * This base controller implements the basic functionality for all specific controllers. This
 * includes error handling and utils.
 */
public abstract class BaseController {

    // HTTP Headers

    public static final String OPENEHR_AUDIT_DETAILS = "openEHR-AUDIT_DETAILS";

    public static final String OPENEHR_VERSION = "openEHR-VERSION";

    public static final String EHRBASE_TEMPLATE_ID = "EHRBase-Template-ID";

    public static final String PREFER = "Prefer";

    public static final String RETURN_MINIMAL = "return=minimal";

    public static final String RETURN_REPRESENTATION = "return=representation";

    // Fixed header identifiers
    public static final String CONTENT_TYPE = HttpHeaders.CONTENT_TYPE;
    public static final String ACCEPT = HttpHeaders.ACCEPT;
    public static final String REQ_CONTENT_TYPE = "Client may request content format";
    public static final String REQ_CONTENT_TYPE_BODY = "Format of transferred body";
    public static final String REQ_ACCEPT = "Client should specify expected format";
    // response headers
    public static final String RESP_CONTENT_TYPE_DESC = "Format of response";
    // Audit
    public static final String REST_OPERATION = "RestOperation";

    public static final String LOCATION = HttpHeaders.LOCATION;
    public static final String ETAG = HttpHeaders.ETAG;
    public static final String LAST_MODIFIED = HttpHeaders.LAST_MODIFIED;

    public static final String IF_MATCH = HttpHeaders.IF_MATCH;
    public static final String IF_NONE_MATCH = HttpHeaders.IF_NONE_MATCH;
    // Configuration of swagger-ui description fields
    // request headers
    public static final String REQ_OPENEHR_VERSION = "Optional custom request header for versioning";
    public static final String REQ_OPENEHR_AUDIT = "Optional custom request header for auditing";
    public static final String REQ_PREFER = "May be used by clients for resource representation negotiation";
    public static final String RESP_LOCATION_DESC = "Location of resource";
    public static final String RESP_ETAG_DESC = "Entity tag for resource";
    public static final String RESP_LAST_MODIFIED_DESC = "Time of last modification of resource";
    // common response description fields
    public static final String RESP_NOT_ACCEPTABLE_DESC =
            "Not Acceptable - Service can not fulfill requested format via accept header.";
    public static final String RESP_UNSUPPORTED_MEDIA_DESC =
            "Unsupported Media Type - request's content-type not supported.";

    // constants of all API resources
    public static final String EHR = "ehr";
    public static final String EHR_STATUS = "ehr_status";
    public static final String VERSIONED_EHR_STATUS = "versioned_ehr_status";
    public static final String VERSIONED_COMPOSITION = "versioned_composition";
    public static final String COMPOSITION = "composition";
    public static final String DIRECTORY = "directory";
    public static final String CONTRIBUTION = "contribution";
    public static final String QUERY = "query";
    public static final String DEFINITION = "definition";
    public static final String TEMPLATE = "template";
    public static final String API_CONTEXT_PATH = "${openehr-api.context-path:/rest/openehr}";
    public static final String API_CONTEXT_PATH_WITH_VERSION = API_CONTEXT_PATH + "/v1";
    public static final String ADMIN_API_CONTEXT_PATH = "${admin-api.context-path:/rest/admin}";

    public Map<String, Map<String, String>> add2MetaMap(
            Map<String, Map<String, String>> metaMap, String key, String value) {
        Map<String, String> contentMap;

        if (metaMap == null) {
            metaMap = new HashMap<>();
            contentMap = new HashMap<>();
            metaMap.put("meta", contentMap);
        } else {
            contentMap = metaMap.get("meta");
        }

        contentMap.put(key, value);
        return metaMap;
    }

    @VisibleForTesting
    public String getContextPath() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    @Value(API_CONTEXT_PATH_WITH_VERSION)
    protected String apiContextPathWithVersion;

    /**
     * Returns a URI for list of segments.
     * The segments are appended to the base path and encoded to ensure safe usage in a URI.
     *
     * @param pathSegments List of segments to append to the base URL
     * @return URI for the given base URL and segments
     */
    protected URI createLocationUri(String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(getContextPath())
                .path(this.encodePath(apiContextPathWithVersion))
                .pathSegment(pathSegments)
                .build()
                .toUri();
    }

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
     * when no UUID representation can be created. This case is equal to no matching object.
     *
     * @param ehrIdString Input String representation of the ehrId
     * @return UUID representation of the ehrId
     * @throws ObjectNotFoundException when no UUID can't be created from input
     */
    protected UUID getEhrUuid(String ehrIdString) {
        return extractUUIDFromStringWithError(
                ehrIdString, "ehr", "EHR not found, in fact, only UUID-type IDs are supported");
    }

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
     * when no UUID representation can be created. This case is equal to no matching object.
     *
     * @param compositionVersionedObjectUidString Input String representation
     * @return UUID representation
     * @throws ObjectNotFoundException when no UUID can't be created from input
     */
    protected UUID getCompositionVersionedObjectUidString(String compositionVersionedObjectUidString) {
        return extractUUIDFromStringWithError(
                compositionVersionedObjectUidString,
                COMPOSITION,
                "Composition not found, in fact, only UUID-type versionedObjectUids are supported");
    }

    /**
     * Helper to allow string UUID input from controllers, which throws an ObjectNotFound exception
     * when no UUID representation can be created. This case is equal to no matching object.
     *
     * @param compositionVersionedObjectUidString Input String representation
     * @return UUID representation
     * @throws ObjectNotFoundException when no UUID can't be created from input
     */
    protected UUID getContributionVersionedObjectUidString(String compositionVersionedObjectUidString) {
        return extractUUIDFromStringWithError(
                compositionVersionedObjectUidString,
                CONTRIBUTION,
                "Contribution not found, in fact, only UUID-type versionedObjectUids are supported");
    }

    // Internal abstraction layer helper, so calling methods above can invoke with meaningful error
    // messages depending on context.
    private UUID extractUUIDFromStringWithError(String uuidString, String type, String error) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new ObjectNotFoundException(type, error);
        }
        return uuid;
    }

    /**
     * Extracts the {@link CompositionRepresentation} from the REST request's input {@link MediaType} style
     * content type header string and validates them against the given format as {@link CompositionFormat}.
     *
     * @param contentType String representation of REST request's input {@link MediaType} style content type header
     * @param format      String representation of the REST request <code>format</code> parameter that is
     *                    interpreted as an {@link CompositionFormat}
     * @return {@link CompositionFormat} expressing the content type
     * @throws NotAcceptableException when content type or composition format is not supported or the input is invalid
     */
    protected CompositionRepresentation extractCompositionRepresentation(String contentType, String format) {

        final CompositionRepresentation representation;
        try {
            final Optional<CompositionFormat> parsedFormat =
                    Optional.ofNullable(format).filter(s -> !s.isEmpty()).map(CompositionFormat::valueOf);
            final MediaType mediaType = resolveContentType(
                    contentType,
                    MediaType.APPLICATION_JSON,
                    mediaType1 -> mediaType1.isCompatibleWith(MediaType.APPLICATION_JSON)
                            || mediaType1.isCompatibleWith(MediaType.APPLICATION_XML)
                            || mediaType1.isCompatibleWith(OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON)
                            || mediaType1.isCompatibleWith(OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON));

            representation =
                    CompositionRepresentation.selectFromMediaTypeWithFormat(mediaType, parsedFormat.orElse(null));
        } catch (IllegalArgumentException e) {
            throw new NotAcceptableException(
                    "Invalid compositions format [%s] only [XML, JSON, FLAT, STRUCTURED] are supported at the moment"
                            .formatted(format));
        }
        return representation;
    }

    /**
     * Convenience helper to encode path strings to URI-safe strings
     *
     * @param path input
     * @return URI-safe escaped string
     * @throws InternalServerException when encoding failed
     */
    public String encodePath(String path) {

        path = UriUtils.encodePath(path, "UTF-8");

        return path;
    }

    /**
     * Extracts the UUID base from a versioned UID. Or, if
     *
     * @param versionUid
     * @return
     */
    protected UUID extractVersionedObjectUidFromVersionUid(String versionUid) {
        if (!versionUid.contains("::")) {
            return UUID.fromString(versionUid);
        }
        return UUID.fromString(versionUid.substring(0, versionUid.indexOf("::")));
    }

    protected Optional<Integer> extractVersionFromVersionUid(String versionUid) {
        // extract the version from string of format "$UUID::$SYSTEM::$VERSION"
        // via making a substring starting at last occurrence of "::" + 2
        int lastOccourence = versionUid.lastIndexOf("::");
        if (lastOccourence > 0 && versionUid.indexOf("::") != lastOccourence) {
            return Optional.of(Integer.parseInt(versionUid.substring(lastOccourence + 2)));
        }

        return Optional.empty();
    }

    /**
     * Add attribute to the current request.
     *
     * @param attributeName
     * @param value
     */
    protected void enrichRequestAttribute(String attributeName, Object value) {
        RequestContextHolder.currentRequestAttributes()
                .setAttribute(attributeName, value, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * Resolves the Content-Type based on Accept header.
     *
     * @param acceptHeader Accept header value
     * @return Content-Type of the response
     */
    protected MediaType resolveContentType(String acceptHeader) {
        return resolveContentType(acceptHeader, MediaType.APPLICATION_JSON);
    }

    /**
     * Resolves the Content-Type based on Accept header.
     *
     * @param acceptHeader     Accept header value
     * @param defaultMediaType Default Content-Type
     * @return Content-Type of the response
     */
    protected MediaType resolveContentType(String acceptHeader, MediaType defaultMediaType) {
        return resolveContentType(
                acceptHeader,
                defaultMediaType,
                mediaType -> mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)
                        || mediaType.isCompatibleWith(MediaType.APPLICATION_XML));
    }

    /**
     * Resolves the Content-Type based on Accept header using the supported predicate.
     *
     * @param acceptHeader     Accept header value
     * @param defaultMediaType Default Content-Type
     * @param isSupported      Filter the supported Content-Types
     * @return Content-Type of the response
     */
    protected MediaType resolveContentType(
            String acceptHeader, MediaType defaultMediaType, Predicate<MediaType> isSupported) {

        List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
        if (mediaTypes.isEmpty()) {
            return defaultMediaType;
        }

        MediaType.sortBySpecificityAndQuality(mediaTypes);
        MediaType contentType = mediaTypes.stream()
                .filter(isSupported)
                .findFirst()
                .orElseThrow(() -> new InvalidApiParameterException("Wrong Content-Type header in request"));

        if (contentType.equals(MediaType.ALL)) {
            return defaultMediaType;
        }

        return contentType;
    }

    protected static Optional<OffsetDateTime> decodeVersionAtTime(String versionAtTimeParam) {
        return Optional.ofNullable(versionAtTimeParam)
                .filter(StringUtils::isNotBlank)
                // revert application/x-www-form-urlencoded
                .map(s -> s.replace(' ', '+'))
                .map(s -> {
                    try {
                        DvDateTime dvDateTime = new DvDateTime(s);

                        if (dvDateTime.getValue() instanceof OffsetDateTime offsetDateTime) {
                            return offsetDateTime;
                        } else if (dvDateTime.getValue() instanceof LocalDateTime localDateTime) {

                            return localDateTime.atOffset(ZoneOffset.UTC);
                        } else {
                            throw new IllegalArgumentException(
                                    "Value '%s' is not valid for version_at_time parameter. Value must be in the extended ISO 8601 format."
                                            .formatted(versionAtTimeParam));
                        }
                    } catch (DateTimeParseException e) {
                        throw new IllegalArgumentException(
                                "Value '%s' is not valid for version_at_time parameter. Value must be in the extended ISO 8601 format."
                                        .formatted(versionAtTimeParam),
                                e);
                    }
                });
    }
}

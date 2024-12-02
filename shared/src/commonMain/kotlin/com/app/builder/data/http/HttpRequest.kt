package com.app.builder.data.http

/**
 * Represents an HTTP request with associated metadata.
 *
 * @property url The URL of the request.
 * @property headerMap A map of headers associated with the request.
 * @property queryMap A map of query parameters associated with the request.
 */
data class HttpRequest(
    val url: URL,
    val headerMap: Map<Header, String> = emptyMap(),
    val queryMap: Map<Query, String> = emptyMap()
)
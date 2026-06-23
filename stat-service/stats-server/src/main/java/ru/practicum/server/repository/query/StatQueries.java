package ru.practicum.server.repository.query;

public enum StatQueries {

    ;

    public static final String STATS = """
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.ip) DESC
            """;

    public static final String UNIQUE_STATS = """
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip))
            FROM EndpointHit h
            WHERE h.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR h.uri IN :uris)
            GROUP BY h.app, h.uri
            ORDER BY COUNT(DISTINCT h.ip) DESC
            """;

    public static final String ALL_STATS = """
            SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri,
                CASE WHEN :unique = true THEN COUNT(h.ip) ELSE COUNT(DISTINCT h.ip) END AS unique)
            FROM EndpointHit h
            WHERE :uris IS NULL OR h.uri IN :uris
            GROUP BY h.app, h.uri
            ORDER BY COUNT(h.ip) DESC
            """;
}
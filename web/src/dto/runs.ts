export type RunSortField = "name" | "author" | "highlights" | "lastModified"
export type SortOrder = "asc" | "desc"

export interface RunSummary {
    name: string
    author: string
    highlightCount: number
    lastModified: string
}

export interface RunListResponse {
    runs: RunSummary[]
    totalPages: number
    currentPage: number
}

export type ArtifactKey = "book" | "calibration" | "output" | "headingsOutput" | "debugRun"

export interface ArtifactSummary {
    name: string
    format: string
    location: string
}

export interface RunDetail {
    name: string
    author: string
    highlightCount: number
    lastModified: string
    artifacts: Partial<Record<ArtifactKey, ArtifactSummary>>
}

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

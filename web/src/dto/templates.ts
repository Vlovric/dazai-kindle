export type TemplateType = "output" | "heading"
export type TemplateFilter = "all" | TemplateType
export type TemplateSortField = "name" | "lastModified"
export type SortOrder = "asc" | "desc"

export interface TemplateSummary {
    name: string
    type: TemplateType
    lastModified: string
}

export interface TemplateListResponse {
    templates: TemplateSummary[]
    totalPages: number
    currentPage: number
}

export interface TemplateContent {
    name: string
    content: string
}

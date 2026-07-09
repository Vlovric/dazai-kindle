import { apiDelete, apiGet, apiGetBlob, apiPost, apiPostForm } from "./client"
import type { TemplateContent, TemplateFilter, TemplateListResponse, TemplateSortField, TemplateSummary, SortOrder } from "../dto/templates"

export function listTemplates(
    type: TemplateFilter,
    sort: TemplateSortField,
    order: SortOrder,
    page: number
): Promise<TemplateListResponse> {
    const params = new URLSearchParams({ sort, order, page: String(page) })
    if (type !== "all") {
        params.set("type", type)
    }
    return apiGet<TemplateListResponse>(`/templates?${params.toString()}`)
}

export function uploadTemplate(file: File): Promise<TemplateSummary> {
    const form = new FormData()
    form.append("file", file)
    return apiPostForm<TemplateSummary>("/templates", form)
}

// Sent as repeated ?names=&names=... params rather than a single comma-joined
// value - a template name is a user-chosen filename that can itself contain a
// comma, same reasoning as run names in api/runs.ts.
function namesParams(names: string[]): string {
    const params = new URLSearchParams()
    names.forEach((name) => params.append("names", name))
    return params.toString()
}

export function deleteTemplates(names: string[]): Promise<void> {
    return apiDelete(`/templates?${namesParams(names)}`)
}

export function exportTemplates(names: string[]): Promise<{ blob: Blob, filename: string }> {
    return apiGetBlob(`/templates/export?${namesParams(names)}`, "templates.zip")
}

export function getTemplate(name: string): Promise<TemplateContent> {
    return apiGet<TemplateContent>(`/templates/${encodeURIComponent(name)}`)
}

export function previewTemplate(content: string): Promise<{ rendered: string }> {
    return apiPost<{ rendered: string }>("/templates/preview", { content })
}

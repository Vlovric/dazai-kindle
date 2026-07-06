export type ArtifactType = "book" | "calibration" | "template" | "headingsTemplate"

export interface UploadedFileResponse {
    name: string
    lastModified: string
    draftId: string | null
}

export interface FileListResponse {
    files: UploadedFileResponse[]
    totalPages: number
    currentPage: number
}
